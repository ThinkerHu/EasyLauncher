package com.vincenthu.launcher

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class WeatherRepositoryTest {
    private val city = WeatherRepository.cities.first()
    private fun current() = JSONObject("""{
      "metadata":{"attributions":["https://developer.qweather.com/attribution.html"]},
      "validTime":"2026-11-18T08:00Z","condition":{"code":"305","text":"小雨"},
      "temperature":{"value":23.5,"unit":"°C"},"feelsLike":{"value":24.8,"unit":"°C"},"humidity":0.86,
      "wind":{"speed":{"value":5,"unit":"m/s"},"direction":{"degree":90}}
    }""")
    private fun daily() = JSONObject("""{"metadata":{"attributions":["https://developer.qweather.com/attribution.html"]},"days":[
      {"temperatureMax":{"value":25},"temperatureMin":{"value":18},"uvIndexMax":3.2,
       "astro":{"sunrise":"2026-11-17T22:30Z","sunset":"2026-11-18T10:00Z"},
       "daytime":{"forecastStartTime":"2026-11-17T23:00Z","condition":{"code":"305"},"precipitation":{"probability":0.6}},
       "nighttime":{"precipitation":{"probability":0.8}}},
      {"temperatureMax":{"value":27},"temperatureMin":{"value":19},
       "astro":{"sunrise":"2026-11-18T22:30Z","sunset":"2026-11-19T10:00Z"},
       "daytime":{"forecastStartTime":"2026-11-18T23:00Z","condition":{"code":"100"}}}
    ]}""")
    private fun hourly() = JSONObject("""{"metadata":{"attributions":["https://developer.qweather.com/attribution.html"]},"hours":[
      {"forecastTime":"2026-11-18T07:00Z","temperature":{"value":21},"condition":{"code":"104"}},
      {"forecastTime":"2026-11-18T08:00Z","temperature":{"value":22},"condition":{"code":"305"},"precipitation":{"probability":0.31}},
      {"forecastTime":"2026-11-18T09:00Z","temperature":{"value":23},"condition":{"code":"101"}},
      {"forecastTime":"2026-11-18T11:00Z","temperature":{"value":20},"condition":{"code":"100"}}
    ]}""")

    @Test fun parsesQWeatherV1AndConvertsUnits() {
        val w = WeatherRepository.parse(city, current(), daily(), hourly(), 1000)
        assertEquals(23.5, w.temperature, 0.01)
        assertEquals(86, w.humidity)
        assertEquals(18.0, w.windSpeed!!, 0.01)
        assertEquals(24.8, w.feelsLike!!, 0.01)
        assertEquals("小雨", w.description)
        assertTrue(w.isDay)
        assertEquals(2, w.days.size)
        assertEquals(80, w.days.first().rainChance)
        assertEquals(3, w.hours.size)
        assertEquals(31, w.hours.first().rainChance)
        assertNull(w.hours[1].rainChance)
        assertFalse(w.hours.last().isDay)
        assertEquals(1000, w.fetchedAt)
    }

    @Test fun missingOptionalMetricsRemainUnknown() {
        val current = current().put("humidity", JSONObject.NULL)
        current.remove("feelsLike"); current.remove("wind")
        val daily = daily(); daily.getJSONArray("days").getJSONObject(0).remove("uvIndexMax")
        val w = WeatherRepository.parse(city, current, daily, null)
        assertNull(w.humidity); assertNull(w.feelsLike); assertNull(w.windSpeed); assertNull(w.uv)
        assertTrue(w.hours.isEmpty())
    }

    @Test fun cacheRoundTripPreservesForecastAttributionAndTimezone() {
        val w = WeatherRepository.parse(city, current(), daily(), hourly(), 1000)
        assertEquals(w, WeatherSnapshot.fromJson(w.toJson()))
        assertEquals(1, w.sourceLinks.size)
        assertEquals(ZoneId.of("Asia/Shanghai"), w.zone())
    }

    @Test fun previousCacheSchemaDoesNotInventExtraMetrics() {
        val old = JSONObject("""{"city":"深圳","temperature":25,"high":27,"low":20,"description":"少云","fetchedAt":1000}""")
        val w = WeatherSnapshot.fromJson(old)
        assertNull(w.humidity); assertNull(w.feelsLike); assertTrue(w.hours.isEmpty()); assertEquals(-1, w.code)
    }

    @Test(expected = IllegalStateException::class)
    fun refusesEmptyDailyForecast() { WeatherRepository.parse(city, current(), JSONObject("""{"days":[]}"""), null) }

    @Test fun citySelectionPersistsCoordinatesAndTimezone() {
        val city = WeatherCity("qw_1", "纽约", 40.71, -74.01, "美国", "America/New_York")
        assertEquals(city, WeatherCity.fromJson(city.toJson()))
        assertEquals("08:00", weatherTime(0L, ZoneId.of("Asia/Shanghai")))
        assertEquals("00:00", weatherTime(0L, ZoneId.of("UTC")))
    }

    @Test fun mapsConditionsIncludingNightSnowAndThunderstorm() {
        assertEquals(0, WeatherRepository.condition(150))
        assertEquals(95, WeatherRepository.condition(302))
        assertEquals(71, WeatherRepository.condition(400))
        assertEquals(-1, WeatherRepository.condition(999))
    }

    @Test fun acceptsRegionalHostAndRedactsCredentialInDiagnostics() {
        val c = WeatherConnection.validated("https://example.re.qweatherapi.com/", "test-key-for-unit-tests")
        assertEquals("example.re.qweatherapi.com", c.host)
        assertFalse(c.toString().contains(c.apiKey))
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesSendingCredentialToOtherHosts() { WeatherConnection.validated("qweatherapi.com.attacker.test", "test-key-for-unit-tests") }

    @Test fun preservesKmhWithoutDoubleConversion() {
        val current = current()
        current.getJSONObject("wind").put("speed", JSONObject().put("value", 18).put("unit", "km/h"))
        assertEquals(18.0, WeatherRepository.parse(city, current, daily(), null).windSpeed!!, 0.01)
    }
    @Test fun selectingCitiesKeepsSwipeOrderStable() {
        val cities = WeatherRepository.cities.take(3)
        assertEquals(cities.map { it.id }, cities.withWeatherCity(cities[1]).map { it.id })
        val extra = WeatherRepository.cities[3]
        assertEquals(cities + extra, cities.withWeatherCity(extra))
    }

    @Test fun cityDeckHasNoDuplicatesAndRetainsNewCityAtCapacity() {
        val cities = WeatherRepository.cities.take(6)
        val extra = WeatherRepository.cities[6]
        val deck = cities.withWeatherCity(extra)
        assertEquals(6, deck.size)
        assertEquals(extra, deck.last())
        assertEquals(6, deck.map { it.id }.toSet().size)
        assertFalse(deck.any { it.id == cities.first().id })
    }

}
