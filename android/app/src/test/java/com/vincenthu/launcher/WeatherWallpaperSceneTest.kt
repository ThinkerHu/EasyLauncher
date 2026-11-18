package com.vincenthu.launcher

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class WeatherWallpaperSceneTest {
    private val now = Instant.parse("2026-11-18T08:00:00Z").toEpochMilli()
    private fun weather(code: Int, isDay: Boolean = true) = WeatherSnapshot(
        "深圳", 25.0, 27.0, 20.0, "实况", now, code = code, isDay = isDay,
        sunrise = Instant.parse("2026-11-17T22:30:00Z").toEpochMilli(),
        sunset = Instant.parse("2026-11-18T10:00:00Z").toEpochMilli()
    )
    private fun scene(weather: WeatherSnapshot, time: Long = now) = weatherWallpaperScene(WeatherState.Ready(weather), true, time)

    @Test fun mapsEverySupportedWeatherFamily() {
        val families = mapOf(
            WeatherAtmosphere.CLEAR to listOf(0),
            WeatherAtmosphere.PARTLY_CLOUDY to listOf(1),
            WeatherAtmosphere.CLOUDY to listOf(2, 3),
            WeatherAtmosphere.RAIN to listOf(51, 55, 61, 67, 80, 82),
            WeatherAtmosphere.SNOW to listOf(71, 77, 85, 86),
            WeatherAtmosphere.FOG to listOf(45, 48),
            WeatherAtmosphere.STORM to listOf(95, 99)
        )
        families.forEach { (family, codes) -> codes.forEach { assertEquals("code=$it", family, scene(weather(it))?.atmosphere) } }
    }

    @Test fun unavailableDisabledOrUnknownWeatherKeepsOriginalWallpaper() {
        listOf(WeatherState.Idle, WeatherState.Loading, WeatherState.Error("offline")).forEach {
            assertNull(weatherWallpaperScene(it, true, now))
        }
        assertNull(scene(weather(-1)))
        assertNull(scene(weather(999)))
        assertNull(weatherWallpaperScene(WeatherState.Ready(weather(0)), false, now))
    }

    @Test fun staleObservationsAndLargeClockSkewDoNotDriveWallpaper() {
        assertNotNull(scene(weather(61).copy(observedAt = now - 3 * 60 * 60_000L)))
        assertNull(scene(weather(61).copy(observedAt = now - 3 * 60 * 60_000L - 1)))
        assertNull(scene(weather(61).copy(observedAt = now + 16 * 60_000L)))
        // A freshly downloaded response can still contain an old observation.
        assertNull(scene(weather(61).copy(fetchedAt = now, observedAt = now - 4 * 60 * 60_000L)))
    }

    @Test fun sunsetChangesBackgroundWithoutAnotherNetworkRequest() {
        assertTrue(scene(weather(0), now + 119 * 60_000L)!!.isDay)
        assertFalse(scene(weather(0), now + 120 * 60_000L)!!.isDay)
    }

    @Test fun sunriseIsDayAndSunsetIsNight() {
        val sunrise = weather(0).sunrise!!
        assertTrue(scene(weather(0).copy(observedAt = sunrise), sunrise)!!.isDay)
        assertFalse(scene(weather(0).copy(observedAt = sunrise), sunrise - 1)!!.isDay)
    }

    @Test fun absentSolarTimesUseRecentProviderDayNightThenCityLocalTime() {
        val localNight = now + 2 * 60 * 60_000L // 18:00 in Shenzhen, 10:00 in London.
        val withoutSun = weather(0).copy(sunrise = null, sunset = null)
        assertFalse(scene(withoutSun.copy(isDay = false))!!.isDay)
        assertFalse(scene(withoutSun, localNight)!!.isDay)
        assertTrue(scene(withoutSun.copy(timezone = "Europe/London"), localNight)!!.isDay)
    }

    @Test fun previousDaySolarTimesAreNotReused() {
        val previous = weather(0).copy(sunrise = weather(0).sunrise!! - 86_400_000L, sunset = weather(0).sunset!! - 86_400_000L)
        assertTrue(scene(previous)!!.isDay)
    }
}
