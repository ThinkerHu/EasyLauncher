package com.vincenthu.launcher

import android.app.Application
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.createBitmap
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.Collator
import java.util.Locale

data class LauncherApp(
    val id: String,
    val label: String,
    val packageName: String,
    val icon: Bitmap,
    val systemCategory: Int = android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED
) {
    fun intent(): Intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(ComponentName.unflattenFromString(id))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
}

data class HomeFolder(val id: String, val name: String, val apps: List<String>)
data class HomeWidget(val appWidgetId: Int, val provider: String)

class LauncherModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("light_home", 0)
    private val weatherConnectionStore = WeatherConnectionStore(application)
    private var weatherConnection = weatherConnectionStore.read()
    var weatherApiHost by mutableStateOf(weatherConnection?.host.orEmpty()); private set
    var weatherConfigured by mutableStateOf(weatherConnection != null); private set
    var weatherConnectionVersion by mutableIntStateOf(0); private set
    var apps by mutableStateOf<List<LauncherApp>>(emptyList()); private set
    var home by mutableStateOf(readList("home")); private set
    var dock by mutableStateOf(readList("dock")); private set
    var folders by mutableStateOf(readFolders()); private set
    var widgets by mutableStateOf(readWidgets()); private set
    var palette by mutableIntStateOf(preferences.getInt("palette", 0)); private set
    var weatherVisible by mutableStateOf(preferences.getBoolean("weatherVisible", true)); private set
    var reduceMotion by mutableStateOf(preferences.getBoolean("reduceMotion", false)); private set
    var weatherWallpaper by mutableStateOf(preferences.getBoolean("weatherWallpaper", true)); private set
    var recentAppIds by mutableStateOf(readRecentAppIds()); private set
    var isDefaultHome by mutableStateOf(false); private set
    var loading by mutableStateOf(true); private set
    var loadFailed by mutableStateOf(false); private set
    var weatherCity by mutableStateOf(readWeatherCity()); private set
    var weatherState by mutableStateOf<WeatherState>(readWeatherCache()); private set
    var weatherRefreshing by mutableStateOf(false); private set
    var weatherRefreshError by mutableStateOf<String?>(null); private set
    var recentWeatherCities by mutableStateOf(readRecentWeatherCities()); private set
    var weatherCities by mutableStateOf(readWeatherCities()); private set
    private var weatherJob: Job? = null
    private var weatherRequestVersion = 0L
    private var refreshing = false

    fun refresh() {
        if (refreshing) return
        refreshing = true
        viewModelScope.launch {
            try {
                val found = withContext(Dispatchers.IO) {
                    val pm = getApplication<Application>().packageManager
                    @Suppress("DEPRECATION")
                    val resolved = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), PackageManager.MATCH_ALL)
                    val collator = Collator.getInstance(Locale.CHINA)
                    resolved.filter { it.activityInfo.packageName != getApplication<Application>().packageName }
                        .map { info ->
                            val component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                            val bitmap = createBitmap(144, 144, Bitmap.Config.ARGB_8888)
                            info.loadIcon(pm).apply { setBounds(0, 0, 144, 144); draw(Canvas(bitmap)) }
                            LauncherApp(
                                component.flattenToString(), info.loadLabel(pm).toString(),
                                info.activityInfo.packageName, bitmap, info.activityInfo.applicationInfo.category
                            )
                        }.distinctBy { it.id }.sortedWith { a, b -> collator.compare(a.label, b.label) }
                }
                apps = found
                val available = found.map { it.id }.toSet()
                recentAppIds = recentAppIds.filter(available::contains)
                folders = folders.map { it.copy(apps = it.apps.filter(available::contains)) }.filter { it.apps.isNotEmpty() }
                if (!preferences.contains("home")) {
                    val preferred = listOf("浏览器", "微信", "图库", "相机", "备忘录", "日历", "文件管理", "设置", "音乐", "视频")
                    val first = preferred.mapNotNull { label -> found.firstOrNull { it.label == label }?.id }
                    home = first + found.map { it.id }.filterNot(first::contains)
                    dock = first.take(4).ifEmpty { found.take(4).map { it.id } }
                } else {
                    home = home.filter { available.contains(it) || folders.any { folder -> folder.id == it } }
                    dock = dock.filter(available::contains).take(4)
                }
                save()
                loadFailed = false
            } catch (_: Exception) {
                loadFailed = true
            } finally {
                loading = false
                refreshing = false
            }
        }
    }

    fun refreshDefaultHome() {
        val app = getApplication<Application>()
        val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                app.getSystemService(RoleManager::class.java)
                    ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_HOME) }
                    ?.isRoleHeld(RoleManager.ROLE_HOME)
            }.getOrNull()
        } else null
        val resolvedHome = runCatching {
            @Suppress("DEPRECATION")
            app.packageManager.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY
            )?.activityInfo?.packageName == app.packageName
        }.getOrDefault(false)
        // Some vendor builds expose the HOME role but report it conservatively;
        // the resolver is the final source of truth for the activity handling HOME.
        isDefaultHome = roleHeld == true || resolvedHome
    }

    fun markRecentlyUsed(id: String) {
        recentAppIds = DockHistory.record(recentAppIds, id)
        preferences.edit {
            putString("recentAppIds", JSONArray(recentAppIds).toString())
            remove("recentAppId")
        }
    }

    private fun readRecentAppIds(): List<String> {
        val stored = if (preferences.contains("recentAppIds")) readList("recentAppIds")
            else listOfNotNull(preferences.getString("recentAppId", null))
        return stored.filter(String::isNotBlank).distinctBy { it.substringBefore('/') }.take(DockHistory.HISTORY_LIMIT)
    }

    fun addHome(id: String) { if (id !in home) { home = home + id; save() } }
    fun removeHome(id: String) { home = home - id; folders = folders.filterNot { it.id == id }; save() }
    fun moveFirst(id: String) { home = listOf(id) + (home - id); save() }
    fun move(id: String, offset: Int) {
        val from = home.indexOf(id)
        if (from < 0) return
        val to = (from + offset).coerceIn(0, home.lastIndex)
        home = home.toMutableList().apply { add(to, removeAt(from)) }; save()
    }
    fun moveToIndex(id: String, index: Int) {
        val from = home.indexOf(id)
        if (from < 0 || home.isEmpty()) return
        val to = index.coerceIn(0, home.lastIndex)
        if (from == to) return
        home = home.toMutableList().apply { add(to, removeAt(from)) }
        save()
    }
    fun swap(first: String, second: String) {
        val a = home.indexOf(first)
        val b = home.indexOf(second)
        if (a < 0 || b < 0 || a == b) return
        home = home.toMutableList().apply { set(a, second); set(b, first) }
        save()
    }
    fun toggleDock(id: String): Boolean {
        if (id in dock) dock = dock - id
        else if (dock.size < 4) dock = dock + id
        else return false
        save(); return true
    }
    fun moveHomeToDock(id: String): Boolean {
        if (id !in home || id.startsWith("folder:")) return false
        if (id !in dock && dock.size >= 4) return false
        home = home - id
        if (id !in dock) dock = dock + id
        save()
        return true
    }
    fun createFolder(name: String, selected: List<String>) {
        val id = "folder:" + java.util.UUID.randomUUID()
        folders = folders + HomeFolder(id, name.trim().ifEmpty { "文件夹" }.take(16), selected)
        home = listOf(id) + home.filterNot(selected::contains)
        save()
    }
    fun dissolveFolder(id: String) {
        val folder = folders.firstOrNull { it.id == id } ?: return
        home = home.flatMap { if (it == id) folder.apps.filterNot(home::contains) else listOf(it) }.distinct()
        folders = folders.filterNot { it.id == id }; save()
    }
    fun renameFolder(id: String, name: String) {
        val clean = name.trim().take(16)
        if (clean.isEmpty()) return
        folders = folders.map { if (it.id == id) it.copy(name = clean) else it }
        save()
    }
    fun addWidget(appWidgetId: Int, provider: String) {
        if (widgets.any { it.appWidgetId == appWidgetId }) return
        widgets = widgets + HomeWidget(appWidgetId, provider)
        save()
    }
    fun removeWidget(appWidgetId: Int) {
        widgets = widgets.filterNot { it.appWidgetId == appWidgetId }
        save()
    }
    fun choosePalette(value: Int) { palette = value; save() }
    fun toggleWeather() { weatherVisible = !weatherVisible; save() }
    fun updateReduceMotion(value: Boolean) { reduceMotion = value; save() }
    fun updateWeatherWallpaper(value: Boolean) { weatherWallpaper = value; save() }
    fun saveWeatherConnection(host: String, apiKey: String) {
        val connection = WeatherConnection.validated(host, apiKey.ifBlank { weatherConnection?.apiKey.orEmpty() })
        weatherConnectionStore.save(connection)
        weatherJob?.cancel()
        weatherRequestVersion++
        weatherConnection = connection
        weatherApiHost = connection.host
        weatherConfigured = true
        weatherConnectionVersion++
        weatherState = readWeatherCache()
        refreshWeather()
    }

    suspend fun searchWeatherCities(query: String): List<WeatherCity> {
        val connection = weatherConnection ?: error("请先配置和风天气连接")
        return WeatherRepository.searchCities(query, connection)
    }

    fun chooseWeatherCity(city: WeatherCity) {
        if (city.id == weatherCity.id) return
        weatherJob?.cancel()
        weatherRequestVersion++
        weatherRefreshing = false
        weatherRefreshError = null
        weatherCity = city
        // Preserve the swipe order; choosing a city must not reshuffle the deck.
        weatherCities = weatherCities.withWeatherCity(city)
        recentWeatherCities = (listOf(city) + recentWeatherCities).distinctBy { it.id }.take(6)
        preferences.edit {
            putString("weatherCity", city.id)
            putString("weatherCityDetails", city.toJson().toString())
            putString("weatherRecentCities", JSONArray().apply { recentWeatherCities.forEach { put(it.toJson()) } }.toString())
            putString("weatherCities", JSONArray().apply { weatherCities.forEach { put(it.toJson()) } }.toString())
        }
        weatherState = readWeatherCache()
        refreshWeatherIfNeeded()
    }
    fun weatherForCity(city: WeatherCity): WeatherState = if (city.id == weatherCity.id) weatherState else readWeatherCache(city)
    fun refreshWeatherIfNeeded() {
        val snapshot = (weatherState as? WeatherState.Ready)?.snapshot
        if (snapshot != null && snapshot.notice == null && System.currentTimeMillis() - snapshot.fetchedAt < 30 * 60 * 1000L) return
        refreshWeather()
    }
    fun refreshWeather() {
        val connection = weatherConnection ?: return
        if (weatherJob?.isActive == true) return
        val requestedCity = weatherCity
        val version = ++weatherRequestVersion
        weatherRefreshing = true
        weatherRefreshError = null
        if (weatherState !is WeatherState.Ready) weatherState = WeatherState.Loading
        weatherJob = viewModelScope.launch {
            try {
                val snapshot = WeatherRepository.fetch(requestedCity, connection)
                if (version == weatherRequestVersion) {
                    preferences.edit { putString("qweatherCache_${requestedCity.id}", snapshot.toJson().toString()) }
                    weatherState = WeatherState.Ready(snapshot)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (version == weatherRequestVersion) {
                    android.util.Log.w("EasyWeather", "QWeather refresh failed: ${failure.javaClass.simpleName}")
                    weatherRefreshError = if (failure is WeatherServiceException) failure.message else "暂时无法更新，请检查网络后重试"
                    if (weatherState !is WeatherState.Ready) weatherState = WeatherState.Error(weatherRefreshError!!)
                }
            } finally {
                if (version == weatherRequestVersion) weatherRefreshing = false
            }
        }
    }

    private fun readList(key: String): List<String> = runCatching {
        val array = JSONArray(preferences.getString(key, "[]"))
        List(array.length()) { array.getString(it) }
    }.getOrDefault(emptyList())

    private fun readFolders(): List<HomeFolder> = runCatching {
        val array = JSONArray(preferences.getString("folders", "[]"))
        List(array.length()) { index ->
            val obj = array.getJSONObject(index)
            val members = obj.getJSONArray("apps")
            HomeFolder(obj.getString("id"), obj.getString("name"), List(members.length()) { members.getString(it) })
        }
    }.getOrDefault(emptyList())

    private fun readWidgets(): List<HomeWidget> = runCatching {
        val array = JSONArray(preferences.getString("widgets", "[]") ?: "[]")
        List(array.length()) { index ->
            val obj = array.getJSONObject(index)
            HomeWidget(obj.getInt("appWidgetId"), obj.getString("provider"))
        }
    }.getOrDefault(emptyList())

    private fun save() {
        val jsonFolders = JSONArray().apply {
            folders.forEach { folder -> put(JSONObject().put("id", folder.id).put("name", folder.name).put("apps", JSONArray(folder.apps))) }
        }
        preferences.edit {
            putString("home", JSONArray(home).toString())
            putString("dock", JSONArray(dock).toString()).putString("folders", jsonFolders.toString())
            putString("widgets", JSONArray().apply {
                widgets.forEach { widget -> put(JSONObject().put("appWidgetId", widget.appWidgetId).put("provider", widget.provider)) }
            }.toString())
            putInt("palette", palette).putBoolean("weatherVisible", weatherVisible)
            putBoolean("reduceMotion", reduceMotion)
            putBoolean("weatherWallpaper", weatherWallpaper)
            putString("recentAppIds", JSONArray(recentAppIds).toString())
            remove("recentAppId")
        }
    }

    private fun readWeatherCity(): WeatherCity = runCatching {
        WeatherCity.fromJson(JSONObject(preferences.getString("weatherCityDetails", "") ?: ""))
    }.getOrElse { WeatherRepository.cities.firstOrNull { it.id == preferences.getString("weatherCity", "shenzhen") } ?: WeatherRepository.cities.first() }

    private fun readRecentWeatherCities(): List<WeatherCity> = runCatching {
        val array = JSONArray(preferences.getString("weatherRecentCities", "[]"))
        (0 until array.length()).map { WeatherCity.fromJson(array.getJSONObject(it)) }.take(6)
    }.getOrDefault(emptyList())

    private fun readWeatherCities(): List<WeatherCity> = runCatching {
        val array = JSONArray(preferences.getString("weatherCities", "[]"))
        val saved = (0 until array.length()).map { WeatherCity.fromJson(array.getJSONObject(it)) }
        (saved.ifEmpty { listOf(weatherCity) + recentWeatherCities }).withWeatherCity(weatherCity)
    }.getOrElse { (listOf(weatherCity) + recentWeatherCities).distinctBy { it.id }.take(6) }

    private fun readWeatherCache(city: WeatherCity = weatherCity): WeatherState = runCatching {
        if (!weatherConfigured) return WeatherState.Idle
        val raw = preferences.getString("qweatherCache_${city.id}", "")
        val snapshot = WeatherSnapshot.fromJson(JSONObject(raw ?: ""))
        require(snapshot.city == city.name)
        WeatherState.Ready(snapshot)
    }.getOrDefault(WeatherState.Idle)
}
