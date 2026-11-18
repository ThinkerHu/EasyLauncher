package com.vincenthu.launcher

import java.time.Instant

internal enum class WeatherAtmosphere { CLEAR, PARTLY_CLOUDY, CLOUDY, RAIN, SNOW, FOG, STORM }

internal data class WeatherWallpaperScene(val atmosphere: WeatherAtmosphere, val isDay: Boolean)

/** The wallpaper only uses the selected city's real, reasonably recent observation. */
internal fun weatherWallpaperScene(state: WeatherState, enabled: Boolean, now: Long): WeatherWallpaperScene? {
    if (!enabled) return null
    val weather = (state as? WeatherState.Ready)?.snapshot ?: return null
    val age = now - weather.observedAt
    if (age !in -15 * 60_000L..3 * 60 * 60_000L) return null
    val atmosphere = when (weather.code) {
        0 -> WeatherAtmosphere.CLEAR
        1 -> WeatherAtmosphere.PARTLY_CLOUDY
        2, 3 -> WeatherAtmosphere.CLOUDY
        45, 48 -> WeatherAtmosphere.FOG
        in 51..67, in 80..82 -> WeatherAtmosphere.RAIN
        in 71..77, in 85..86 -> WeatherAtmosphere.SNOW
        in 95..99 -> WeatherAtmosphere.STORM
        else -> return null
    }
    val localNow = Instant.ofEpochMilli(now).atZone(weather.zone())
    val sunrise = weather.sunrise
    val sunset = weather.sunset
    val hasTodaysSunTimes = sunrise != null && sunset != null && sunrise < sunset &&
        Instant.ofEpochMilli(sunrise).atZone(weather.zone()).toLocalDate() == localNow.toLocalDate() &&
        Instant.ofEpochMilli(sunset).atZone(weather.zone()).toLocalDate() == localNow.toLocalDate()
    val isDay = when {
        hasTodaysSunTimes -> now >= sunrise!! && now < sunset!!
        age <= 90 * 60_000L -> weather.isDay
        else -> localNow.hour in 6..17
    }
    return WeatherWallpaperScene(atmosphere, isDay)
}
