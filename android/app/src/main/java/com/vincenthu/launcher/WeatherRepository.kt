package com.vincenthu.launcher

import kotlinx.coroutines.*
import java.time.OffsetDateTime
import java.util.zip.GZIPInputStream
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

data class WeatherCity(val id: String, val name: String, val latitude: Double, val longitude: Double, val region: String = "", val timezone: String = "Asia/Shanghai") {
    fun toJson() = JSONObject().put("id", id).put("name", name).put("latitude", latitude).put("longitude", longitude).put("region", region).put("timezone", timezone)
    companion object {
        fun fromJson(j: JSONObject) = WeatherCity(j.getString("id"), j.getString("name"), j.getDouble("latitude"), j.getDouble("longitude"), j.optString("region"), j.optString("timezone", "Asia/Shanghai"))
    }
}

data class WeatherHour(val time: Long, val temperature: Double, val code: Int, val rainChance: Int?, val isDay: Boolean)
internal fun List<WeatherCity>.withWeatherCity(city: WeatherCity): List<WeatherCity> =
    (if (any { it.id == city.id }) map { if (it.id == city.id) city else it } else this + city).distinctBy { it.id }.takeLast(6)
data class WeatherDay(val time: Long, val high: Double, val low: Double, val code: Int, val rainChance: Int?)

data class WeatherSnapshot(
    val city: String, val temperature: Double, val high: Double, val low: Double,
    val description: String, val fetchedAt: Long,
    val code: Int = -1, val isDay: Boolean = true, val feelsLike: Double? = null,
    val humidity: Int? = null, val windSpeed: Double? = null, val windDirection: Int? = null,
    val uv: Double? = null, val sunrise: Long? = null, val sunset: Long? = null,
    val timezone: String = "Asia/Shanghai", val observedAt: Long = fetchedAt,
    val hours: List<WeatherHour> = emptyList(), val days: List<WeatherDay> = emptyList(),
    val notice: String? = null, val sourceLinks: List<String> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("city", city); put("temperature", temperature); put("high", high); put("low", low)
        put("description", description); put("fetchedAt", fetchedAt); put("code", code); put("isDay", isDay)
        put("feelsLike", feelsLike); put("humidity", humidity); put("windSpeed", windSpeed); put("windDirection", windDirection)
        put("uv", uv); put("sunrise", sunrise); put("sunset", sunset); put("timezone", timezone); put("observedAt", observedAt)
        put("notice", notice); put("sourceLinks", JSONArray(sourceLinks))
        put("hours", JSONArray().apply { hours.forEach { put(JSONObject().put("time", it.time).put("temperature", it.temperature).put("code", it.code).put("rain", it.rainChance).put("day", it.isDay)) } })
        put("days", JSONArray().apply { days.forEach { put(JSONObject().put("time", it.time).put("high", it.high).put("low", it.low).put("code", it.code).put("rain", it.rainChance)) } })
    }
    companion object {
        fun fromJson(j: JSONObject): WeatherSnapshot = WeatherSnapshot(
            city = j.getString("city"), temperature = j.getDouble("temperature"), high = j.getDouble("high"), low = j.getDouble("low"),
            description = j.getString("description"), fetchedAt = j.getLong("fetchedAt"), code = j.optInt("code", -1), isDay = j.optBoolean("isDay", true),
            feelsLike = j.numberOrNull("feelsLike"), humidity = j.numberOrNull("humidity")?.toInt(), windSpeed = j.numberOrNull("windSpeed"),
            windDirection = j.numberOrNull("windDirection")?.toInt(), uv = j.numberOrNull("uv"), sunrise = j.numberOrNull("sunrise")?.toLong(),
            sunset = j.numberOrNull("sunset")?.toLong(), timezone = j.optString("timezone", "Asia/Shanghai"), observedAt = j.optLong("observedAt", j.getLong("fetchedAt")),
            hours = j.optJSONArray("hours").objects().map { WeatherHour(it.getLong("time"), it.getDouble("temperature"), it.optInt("code", -1), it.numberOrNull("rain")?.toInt(), it.optBoolean("day", true)) },
            days = j.optJSONArray("days").objects().map { WeatherDay(it.getLong("time"), it.getDouble("high"), it.getDouble("low"), it.optInt("code", -1), it.numberOrNull("rain")?.toInt()) },
            notice = if (j.isNull("notice")) null else j.optString("notice"), sourceLinks = j.optJSONArray("sourceLinks").strings()
        )
    }
}

sealed interface WeatherState {
    data object Idle : WeatherState
    data object Loading : WeatherState
    data class Ready(val snapshot: WeatherSnapshot) : WeatherState
    data class Error(val message: String) : WeatherState
}

internal fun JSONObject.numberOrNull(key: String): Double? = if (isNull(key)) null else optDouble(key).takeIf { it.isFinite() }
private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
internal class WeatherServiceException(val status: Int, message: String) : java.io.IOException(message)

object WeatherRepository {
    val cities = listOf(
        WeatherCity("shenzhen", "深圳", 22.5431, 114.0579, "广东"), WeatherCity("beijing", "北京", 39.9042, 116.4074, "中国"),
        WeatherCity("shanghai", "上海", 31.2304, 121.4737, "中国"), WeatherCity("guangzhou", "广州", 23.1291, 113.2644, "广东"),
        WeatherCity("hangzhou", "杭州", 30.2741, 120.1551, "浙江"), WeatherCity("chengdu", "成都", 30.5728, 104.0668, "四川"),
        WeatherCity("chongqing", "重庆", 29.5630, 106.5516, "中国"), WeatherCity("wuhan", "武汉", 30.5928, 114.3055, "湖北"),
        WeatherCity("nanjing", "南京", 32.0603, 118.7969, "江苏"), WeatherCity("suzhou", "苏州", 31.2989, 120.5853, "江苏"),
        WeatherCity("xian", "西安", 34.3416, 108.9398, "陕西"), WeatherCity("changsha", "长沙", 28.2282, 112.9388, "湖南"),
        WeatherCity("xiamen", "厦门", 24.4798, 118.0894, "福建"), WeatherCity("qingdao", "青岛", 36.0671, 120.3826, "山东"),
        WeatherCity("kunming", "昆明", 25.0389, 102.7183, "云南"), WeatherCity("hongkong", "香港", 22.3193, 114.1694, "中国")
    )

    suspend fun fetch(city: WeatherCity, connection: WeatherConnection): WeatherSnapshot = supervisorScope {
        val position = String.format(Locale.ROOT, "%.2f/%.2f", city.latitude, city.longitude)
        val query = "/$position?lang=zh"
        val current = async(Dispatchers.IO) { request(connection, "/weather/v1/current$query") }
        val daily = async(Dispatchers.IO) { request(connection, "/weather/v1/daily$query&days=7") }
        val hourly = async(Dispatchers.IO) { request(connection, "/weather/v1/hourly$query&hours=24") }
        val currentJson = current.await()
        val dailyJson = daily.await()
        var notice: String? = null
        val hourlyJson = try { hourly.await() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            notice = if (failure is WeatherServiceException) "逐小时预报：${failure.message}" else "逐小时预报暂不可用，可稍后刷新"
            null
        }
        parse(city, currentJson, dailyJson, hourlyJson).copy(notice = notice)
    }

    /** QWeather's v1 APIs return ISO-8601 instants and explicit SI units. */
    internal fun parse(city: WeatherCity, currentJson: JSONObject, dailyJson: JSONObject, hourlyJson: JSONObject?, fetchedAt: Long = System.currentTimeMillis()): WeatherSnapshot {
        val current = currentJson
        val conditions = current.getJSONObject("condition")
        val conditionCode = conditions.optInt("code", -1)
        val observed = instant(current.getString("validTime"))
        val daily = dailyJson.getJSONArray("days").objects()
        val firstDay = daily.firstOrNull() ?: error("和风暂未返回多日预报")
        val days = daily.map { day ->
            val daytime = day.getJSONObject("daytime")
            WeatherDay(instant(daytime.getString("forecastStartTime")), day.getJSONObject("temperatureMax").getDouble("value"), day.getJSONObject("temperatureMin").getDouble("value"),
                condition(daytime.getJSONObject("condition").optInt("code", -1)), listOfNotNull(daytime.optJSONObject("precipitation")?.numberOrNull("probability"), day.optJSONObject("nighttime")?.optJSONObject("precipitation")?.numberOrNull("probability")).maxOrNull()?.let { (it * 100).roundToInt() })
        }.take(5)
        val hours = hourlyJson?.optJSONArray("hours").objects().mapNotNull { hour ->
            val time = instant(hour.getString("forecastTime"))
            if (time < observed / 3_600_000 * 3_600_000) return@mapNotNull null
            val condition = hour.getJSONObject("condition").optInt("code", -1)
            WeatherHour(time, hour.getJSONObject("temperature").getDouble("value"), condition(condition), hour.optJSONObject("precipitation")?.numberOrNull("probability")?.let { (it * 100).roundToInt() }, isDay(condition, time, daily))
        }.take(24)
        val astro = firstDay.optJSONObject("astro")
        val speed = current.optJSONObject("wind")?.optJSONObject("speed")?.numberOrNull("value")
        val windUnit = current.optJSONObject("wind")?.optJSONObject("speed")?.optString("unit", "m/s") ?: "m/s"
        val sourceLinks = (currentJson.optJSONObject("metadata")?.optJSONArray("attributions").strings() +
            dailyJson.optJSONObject("metadata")?.optJSONArray("attributions").strings() +
            hourlyJson?.optJSONObject("metadata")?.optJSONArray("attributions").strings()).distinct()
        return WeatherSnapshot(
            city = city.name, temperature = current.getJSONObject("temperature").getDouble("value").also { require(it.isFinite()) },
            high = days.first().high, low = days.first().low, description = conditions.optString("text", description(condition(conditionCode))),
            fetchedAt = fetchedAt, code = condition(conditionCode), isDay = isDay(conditionCode, observed, daily),
            feelsLike = current.optJSONObject("feelsLike")?.numberOrNull("value"), humidity = current.numberOrNull("humidity")?.let { (it * 100).roundToInt() },
            windSpeed = speed?.let { if (windUnit == "m/s") it * 3.6 else it }, windDirection = current.optJSONObject("wind")?.optJSONObject("direction")?.numberOrNull("degree")?.roundToInt(),
            uv = firstDay.numberOrNull("uvIndexMax"), sunrise = astro?.optString("sunrise")?.let(::optionalInstant), sunset = astro?.optString("sunset")?.let(::optionalInstant),
            timezone = city.timezone, observedAt = observed, hours = hours, days = days, sourceLinks = sourceLinks
        )
    }

    suspend fun searchCities(query: String, connection: WeatherConnection): List<WeatherCity> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val json = request(connection, "/geo/v2/city/lookup?location=$encoded&number=12&lang=zh")
        json.optJSONArray("location").objects().map { j ->
            WeatherCity("qw_${j.getString("id")}", j.getString("name"), j.getDouble("lat"), j.getDouble("lon"),
                listOf(j.optString("adm1"), j.optString("country")).filter { it.isNotBlank() }.distinct().joinToString(" · "), j.optString("tz", "Asia/Shanghai"))
        }.distinctBy { it.id }
    }

    private fun instant(value: String): Long = OffsetDateTime.parse(value).toInstant().toEpochMilli()
    private fun optionalInstant(value: String): Long? = runCatching { instant(value) }.getOrNull()
    private fun isDay(code: Int, time: Long, days: List<JSONObject>): Boolean {
        if (code in listOf(150, 151, 152, 153, 350, 351, 456, 457)) return false
        val riseSet = days.mapNotNull { it.optJSONObject("astro") }.mapNotNull { astro ->
            val rise = optionalInstant(astro.optString("sunrise")) ?: return@mapNotNull null
            val set = optionalInstant(astro.optString("sunset")) ?: return@mapNotNull null
            rise to set
        }
        return if (riseSet.isNotEmpty()) riseSet.any { time in it.first..it.second } else code !in 150..199
    }

    internal fun condition(code: Int): Int = when (code) {
        100, 150 -> 0; 101, 151 -> 2; 102, 103, 152, 153 -> 1; 104 -> 3
        302, 303, 304 -> 95; 300, 301, 350, 351 -> 80
        305, 309 -> 61; 306, 314 -> 63; 307, 308, in 310..312, in 315..318 -> 65; 313 -> 66
        400 -> 71; 401 -> 73; 402, 403, in 408..410 -> 75; 404, 405, 406, 407, 456, 457 -> 85
        in 500..515 -> 45; else -> -1
    }

    private fun request(connection: WeatherConnection, path: String): JSONObject {
        val request = (URL("https://${connection.host}$path").openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000; readTimeout = 10000; requestMethod = "GET"; instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Encoding", "gzip")
            setRequestProperty("X-QW-Api-Key", connection.apiKey)
        }
        return try {
            val status = request.responseCode
            if (status !in 200..299) throw WeatherServiceException(status, when (status) {
                400 -> "请求参数不可用，请检查所选城市"
                401 -> "认证失败，请检查 API Host 和 API Key"
                402 -> "账号额度不足，请检查和风控制台"
                403 -> "接口未授权，请检查订阅与 Key 的访问限制"
                404 -> "接口不可用，请检查 API Host 与服务权限"
                429 -> "请求过于频繁或已达额度，请稍后再试"
                else -> "和风服务暂不可用（$status）"
            })
            val stream = request.inputStream.let { if (request.contentEncoding?.contains("gzip", true) == true) GZIPInputStream(it) else it }
            val json = stream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
            val code = json.optString("code", "200")
            if (code != "200") throw WeatherServiceException(code.toIntOrNull() ?: 500, if (code == "404") "未找到对应城市" else "城市查询暂不可用（$code）")
            json
        } finally { request.disconnect() }
    }

    fun description(code: Int): String = when (code) {
        0 -> "晴"; 1 -> "晴间多云"; 2 -> "多云"; 3 -> "阴"; 45, 48 -> "雾"
        51, 53, 55 -> "毛毛雨"; 56, 57, 66, 67 -> "冻雨"; 61 -> "小雨"; 63 -> "中雨"; 65 -> "大雨"
        71 -> "小雪"; 73 -> "中雪"; 75, 77 -> "大雪"; 80, 81, 82 -> "阵雨"; 85, 86 -> "阵雪"
        95, 96, 99 -> "雷雨"; else -> "天气未知"
    }
}

internal fun WeatherSnapshot.zone(): ZoneId = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("Asia/Shanghai"))
internal fun weatherTime(time: Long?, zone: ZoneId, pattern: String = "HH:mm"): String = time?.let {
    java.time.format.DateTimeFormatter.ofPattern(pattern, Locale.CHINA).format(Instant.ofEpochMilli(it).atZone(zone))
} ?: "—"
