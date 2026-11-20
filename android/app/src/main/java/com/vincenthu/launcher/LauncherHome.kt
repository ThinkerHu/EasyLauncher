@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.vincenthu.launcher

import android.app.role.RoleManager
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.repeatOnLifecycle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
private fun CloudHeader(
    now: LocalDateTime,
    landscape: Boolean,
    tight: Boolean,
    weatherVisible: Boolean,
    weatherState: WeatherState,
    weatherCity: WeatherCity,
    weatherAnimating: Boolean,
    weatherCities: List<WeatherCity>,
    weatherForCity: (WeatherCity) -> WeatherState,
    onWeatherCitySelected: (WeatherCity) -> Unit,
    calendarEvents: List<CalendarEntry>,
    calendarPermission: Boolean,
    onSearch: () -> Unit,
    onWeather: () -> Unit,
    onCalendar: () -> Unit
) {
    val motion = LocalLauncherMotionEnabled.current
    val compactCards = tight || !landscape
    val cardEntrance = rememberEntranceState()
    val weatherEntrance = rememberEntranceState(visible = weatherVisible)
    val date = now.format(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.CHINA))
    if (landscape) {
        Row(Modifier.fillMaxWidth().height(if (tight) 124.dp else 164.dp), horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(date, color = Color.White.copy(alpha = .88f), fontSize = 14.sp)
                Text("每一天，都有新风景。", color = Color.White, fontSize = if (tight) 22.sp else 24.sp, fontWeight = FontWeight.Medium)
                Surface(onClick = onSearch, shape = RoundedCornerShape(24.dp), color = Color.White.copy(alpha = .18f)) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⌕", color = Color.White, fontSize = 19.sp); Spacer(Modifier.width(9.dp))
                        Text("搜索应用", color = Color.White.copy(alpha = .92f), fontSize = 12.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(.45f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AnimatedVisibility(
                    visibleState = weatherEntrance,
                    modifier = Modifier.weight(1.05f),
                    enter = fadeIn(tween(if (motion) 320 else 0)) + slideInHorizontally(LauncherMotion.layer(motion)) { it / 10 },
                    exit = fadeOut(tween(if (motion) 160 else 0)) + scaleOut(tween(if (motion) 160 else 0), targetScale = if (motion) .98f else 1f)
                ) { WeatherHomeCard(Modifier.fillMaxSize(), weatherState, weatherCity, weatherCities, weatherForCity, onWeatherCitySelected, weatherAnimating, onWeather) }
                AnimatedVisibility(
                    visibleState = cardEntrance,
                    modifier = Modifier.weight(1.1f),
                    enter = fadeIn(tween(if (motion) 320 else 0)) + slideInHorizontally(LauncherMotion.layer(motion)) { it / 12 }
                ) { CloudCalendarCard(Modifier.fillMaxSize(), compactCards, now, calendarEvents, calendarPermission, onCalendar) }
            }
        }
    } else {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(date, color = Color.White.copy(alpha = .84f), fontSize = 14.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("每一天，都有新风景。", Modifier.weight(1f), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Medium)
                TextButton(onClick = onSearch) { Text("⌕", color = Color.White, fontSize = 22.sp) }
            }
            Row(Modifier.fillMaxWidth().height(if (tight) 96.dp else 132.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AnimatedVisibility(
                    visibleState = weatherEntrance,
                    modifier = Modifier.weight(1f),
                    enter = fadeIn(tween(if (motion) 320 else 0)) + slideInVertically(LauncherMotion.layer(motion)) { -it / 10 },
                    exit = fadeOut(tween(if (motion) 160 else 0)) + scaleOut(tween(if (motion) 160 else 0), targetScale = if (motion) .98f else 1f)
                ) { WeatherHomeCard(Modifier.fillMaxSize(), weatherState, weatherCity, weatherCities, weatherForCity, onWeatherCitySelected, weatherAnimating, onWeather) }
                AnimatedVisibility(
                    visibleState = cardEntrance,
                    modifier = Modifier.weight(1.05f),
                    enter = fadeIn(tween(if (motion) 320 else 0)) + slideInVertically(LauncherMotion.layer(motion)) { -it / 12 }
                ) { CloudCalendarCard(Modifier.fillMaxSize(), compactCards, now, calendarEvents, calendarPermission, onCalendar) }
            }
        }
    }
}

@Composable
private fun CloudCalendarCard(modifier: Modifier, tight: Boolean, now: LocalDateTime, events: List<CalendarEntry>, permissionGranted: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.fillMaxHeight(), shape = RoundedCornerShape(18.dp), color = Color(0xFFF0F0E8).copy(alpha = .93f)) {
        Column(Modifier.fillMaxSize().padding(if (tight) 12.dp else 17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(now.format(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.CHINA)), Modifier.weight(1f), color = Color(0xFF667E88), fontSize = 10.sp, maxLines = 1)
                Text("${now.dayOfMonth}", color = Color(0xFF536E79), fontSize = if (tight) 18.sp else 22.sp, fontWeight = FontWeight.Light)
            }
            if (events.isEmpty()) {
                Text(if (permissionGranted) "今天暂无安排" else "把今天安排好", color = Ink, fontSize = if (tight) 14.sp else 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(if (permissionGranted) "点击添加或查看日程" else "授权后显示今天的安排", color = Muted, fontSize = 10.sp, maxLines = 1)
            } else {
                events.take(if (tight) 2 else 3).forEachIndexed { index, event ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                        Box(Modifier.width(3.dp).height(if (tight) 24.dp else 28.dp).clip(CircleShape).background(if (index == 0) Color(0xFF829FBE) else Color(0xFF9CAA92)))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(if (event.allDay) "全天" else java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(event.begin)), color = Muted, fontSize = 9.sp)
                            Text(event.title, color = Ink, fontSize = if (tight) 11.sp else 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private val Ink = Color(0xFF29465A)
private val Muted = Color(0xFF66818F)
private val palettes = listOf(
    listOf(Color(0xFF8BA9BB), Color(0xFF6392AE), Color(0xFF335C7A)),
    listOf(Color(0xFF93B3AB), Color(0xFF628D89), Color(0xFF345E5A)),
    listOf(Color(0xFFADB0C8), Color(0xFF77829F), Color(0xFF495573))
)

@Composable
fun LauncherHome(model: LauncherModel, homeRequest: Int, widgetHost: WidgetHostController) {
    LauncherMotionTheme(model.reduceMotion) { LauncherHomeContent(model, homeRequest, widgetHost) }
}

@Composable
private fun LauncherHomeContent(model: LauncherModel, homeRequest: Int, widgetHost: WidgetHostController) {
    val context = LocalContext.current
    val motion = LocalLauncherMotionEnabled.current
    val scope = rememberCoroutineScope()
    var overlay by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var folderName by rememberSaveable { mutableStateOf("") }
    var folderMembers by rememberSaveable { mutableStateOf(listOf<String>()) }
    var deleteTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragPoint by remember { mutableStateOf<Offset?>(null) }
    var lastDropTarget by remember { mutableStateOf<String?>(null) }
    var pagerBounds by remember { mutableStateOf<Rect?>(null) }
    var dockBounds by remember { mutableStateOf<Rect?>(null) }
    var dragOverDock by remember { mutableStateOf(false) }
    var pagerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var autoPageTarget by remember { mutableIntStateOf(-1) }
    var autoPageJob by remember { mutableStateOf<Job?>(null) }
    var calendarEvents by remember { mutableStateOf<List<CalendarEntry>>(emptyList()) }
    var calendarPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) }
    fun message(text: String) { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
    val requestCalendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        calendarPermission = granted
        if (granted) scope.launch { calendarEvents = withContext(Dispatchers.IO) { CalendarRepository.today(context) } }
    }
    val defaultHomeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        model.refreshDefaultHome()
        if (!model.isDefaultHome) message("系统未将 EasyLauncher 设为默认桌面，请在桌面设置中确认")
    }
    val uninstallLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        model.refresh()
    }
    lateinit var widgetActivityLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    widgetActivityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        widgetHost.handleActivityResult(result.resultCode, result.data) { intent ->
            runCatching { widgetActivityLauncher.launch(intent) }.onFailure { message("无法打开系统小组件设置") }
        }
    }
    val tileBounds = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    val appMap = remember(model.apps) { model.apps.associateBy { it.id } }
    val folderMap = remember(model.folders) { model.folders.associateBy { it.id } }
    val pages = HomeLayout.pageCount(model.home.size)
    val pager = rememberPagerState(pageCount = { pages })
    val edgeThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 92.dp.toPx() }
    val colors = palettes[model.palette.coerceIn(palettes.indices)].mapIndexed { index, color ->
        animateColorAsState(color, tween(if (motion) 360 else 0, easing = LauncherMotion.easeOut), label = "wallpaper-color-$index").value
    }
    val backdropDepth by animateFloatAsState(
        if (overlay.isNotEmpty() && motion) 1f else 0f,
        LauncherMotion.layer(motion), label = "desktop-depth"
    )
    suspend fun goToPage(page: Int) {
        pager.animateScrollToPage(page.coerceIn(0, pages - 1), animationSpec = LauncherMotion.page(motion))
    }
    fun endDrag(commit: Boolean = true) {
        val id = draggingId
        if (commit && dragOverDock && id != null) {
            if (model.moveHomeToDock(id)) message("已移至常驻栏")
            else if (id.startsWith("folder:")) message("文件夹不能加入常驻栏")
            else message("常驻栏已满，请先移出一个应用")
        }
        autoPageJob?.cancel()
        autoPageJob = null
        autoPageTarget = -1
        draggingId = null
        dragPoint = null
        lastDropTarget = null
        dragOverDock = false
    }
    fun handleDrag(rootPoint: Offset) {
        val id = draggingId ?: return
        dragPoint = rootPoint
        dragOverDock = dockBounds?.contains(rootPoint) == true
        if (dragOverDock) {
            autoPageJob?.cancel()
            autoPageJob = null
            autoPageTarget = -1
            lastDropTarget = null
            return
        }
        val visibleMembers = model.home.drop(pager.currentPage * HomeLayout.PAGE_SIZE).take(HomeLayout.PAGE_SIZE).toSet()
        val over = tileBounds.entries.firstOrNull { it.key != id && it.key in visibleMembers && it.value.contains(rootPoint) }?.key
        if (over != null && over != lastDropTarget) {
            model.swap(id, over)
            lastDropTarget = over
        }
        val bounds = pagerBounds
        if (bounds != null) {
            val nextPage = when {
                rootPoint.x < bounds.left + edgeThresholdPx && pager.currentPage > 0 -> pager.currentPage - 1
                rootPoint.x > bounds.right - edgeThresholdPx && pager.currentPage < pages - 1 -> pager.currentPage + 1
                else -> -1
            }
            if (nextPage >= 0 && nextPage != autoPageTarget) {
                autoPageTarget = nextPage
                autoPageJob?.cancel()
                autoPageJob = scope.launch {
                    goToPage(nextPage)
                    delay(180)
                    if (autoPageTarget == nextPage) autoPageTarget = -1
                }
            } else if (nextPage < 0) {
                autoPageTarget = -1
            }
        }
    }
    fun launch(app: LauncherApp) {
        runCatching { context.startActivity(app.intent()); model.markRecentlyUsed(app.id) }
            .onFailure { message("暂时无法打开${app.label}，请检查应用状态"); model.refresh() }
    }
    fun renameFolder(id: String) { target = id; folderName = model.folders.firstOrNull { it.id == id }?.name.orEmpty(); overlay = "rename" }
    fun menuFor(id: String): IconMenuContent {
        val app = model.apps.firstOrNull { it.id == id }
        val folder = model.folders.firstOrNull { it.id == id }
        val entries = buildList {
            if (app != null) {
                add(IconMenuAction("移除", { deleteTarget = id }, true))
            }
            if (app != null) {
                add(IconMenuAction("应用信息", {
                    runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ("package:" + app.packageName).toUri())) }
                        .onFailure { message("无法打开应用信息") }
                }))
            }
            if (folder != null) {
                add(IconMenuAction("重命名文件夹", { renameFolder(id) }))
                add(IconMenuAction("解散文件夹", { model.dissolveFolder(id); overlay = "" }, true))
            }
        }
        return IconMenuContent(app?.label ?: folder?.name ?: "整理应用", entries)
    }
    fun beginFolder() { folderName = ""; folderMembers = emptyList(); overlay = "create" }

    LaunchedEffect(Unit) {
        while (true) { now = LocalDateTime.now(); delay(15_000) }
    }
    val weatherLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(weatherLifecycleOwner, model) {
        weatherLifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                model.refreshWeatherIfNeeded()
                delay(30 * 60 * 1000L)
            }
        }
    }
    LaunchedEffect(calendarPermission) {
        if (calendarPermission) calendarEvents = withContext(Dispatchers.IO) { CalendarRepository.today(context) }
    }
    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) { overlay = ""; query = ""; editing = false; goToPage(0) }
    }
    BackHandler {
        if (overlay.isNotEmpty()) overlay = ""
        else if (editing) editing = false
        else scope.launch { goToPage(0) }
    }
    val wallpaperScene = remember(model.weatherState, model.weatherWallpaper, now) {
        weatherWallpaperScene(model.weatherState, model.weatherWallpaper, System.currentTimeMillis())
    }
    LaunchedEffect(wallpaperScene?.isDay, overlay) {
        if (overlay.isEmpty()) (context.findActivity() as? MainActivity)?.updateDesktopSystemBars(night = wallpaperScene?.isDay == false)
    }

    CompositionLocalProvider(
        LocalIconMenus provides ::menuFor,
        LocalShortcutLaunched provides { id -> model.markRecentlyUsed(id) },
        LocalIconMenuHomeRequest provides homeRequest
    ) {
    Box(Modifier.fillMaxSize().onGloballyPositioned { rootCoordinates = it }) {
        WeatherWallpaper(
            colors = colors,
            scene = wallpaperScene,
            visible = overlay.isEmpty()
        ) { (pager.currentPage + pager.currentPageOffsetFraction) / (pages - 1).coerceAtLeast(1) * 2f - 1f }
        BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer {
            scaleX = 1f - backdropDepth * .025f
            scaleY = 1f - backdropDepth * .025f
        }.safeDrawingPadding()) {
            val landscape = maxWidth > maxHeight
            val tight = maxHeight < 500.dp
            val side = if (maxWidth < 480.dp) 16.dp else if (landscape) 40.dp else 28.dp
            Column(Modifier.fillMaxSize().padding(horizontal = side)) {
                Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (editing) {
                        Text("编辑桌面 · 长按拖动图标", color = Color.White.copy(alpha = .84f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    } else Spacer(Modifier.weight(1f))
                    if (editing) {
                        HomeToolbarAction("新建文件夹", R.drawable.ic_toolbar_folder_add) { beginFolder() }
                        HomeToolbarAction("小组件", R.drawable.ic_toolbar_widgets) { overlay = "widgets" }
                    }
                    HomeToolbarAction(if (editing) "完成编辑" else "编辑桌面",
                        if (editing) R.drawable.ic_toolbar_done else R.drawable.ic_toolbar_edit) { editing = !editing }
                    HomeToolbarAction("桌面设置", R.drawable.ic_toolbar_settings) { overlay = "settings" }
                }
                CloudHeader(
                    now = now,
                    landscape = landscape,
                    tight = tight,
                    weatherVisible = model.weatherVisible,
                    weatherState = model.weatherState,
                    weatherCity = model.weatherCity,
                    weatherAnimating = overlay.isEmpty(),
                    weatherCities = model.weatherCities,
                    weatherForCity = model::weatherForCity,
                    onWeatherCitySelected = model::chooseWeatherCity,
                    calendarEvents = calendarEvents,
                    calendarPermission = calendarPermission,
                    onSearch = { overlay = "drawer" },
                    onWeather = { overlay = "weather" },
                    onCalendar = {
                        if (!calendarPermission) requestCalendarPermission.launch(Manifest.permission.READ_CALENDAR)
                        else {
                            val app = model.apps.firstOrNull { it.label.contains("日历") || it.packageName.contains("calendar") }
                            if (app != null) launch(app) else message("未找到可打开的日历应用")
                        }
                    }
                )
                Spacer(Modifier.height(if (tight) 8.dp else 16.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.fillMaxSize()) {
                        model.widgets.forEach { widget ->
                            key(widget.appWidgetId) {
                                AnimatedVisibility(visibleState = rememberEntranceState(), enter = fadeIn(tween(360)) + slideInVertically(tween(360)) { -it / 4 }) {
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color.White.copy(alpha = .72f),
                                        modifier = Modifier.fillMaxWidth().height(if (landscape) 144.dp else 184.dp).padding(bottom = 10.dp)
                                    ) {
                                        Box(Modifier.fillMaxSize()) {
                                            AndroidView(factory = { widgetHost.createView(widget) }, modifier = Modifier.fillMaxSize().padding(8.dp))
                                            if (editing) TextButton(onClick = { widgetHost.remove(widget) }, modifier = Modifier.align(Alignment.TopEnd)) { Text("移除") }
                                        }
                                    }
                                }
                            }
                        }
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            when {
                                model.loading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(Modifier.size(28.dp)); Spacer(Modifier.height(12.dp)); Text("正在整理应用…", color = Muted)
                                }
                                model.loadFailed -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("暂时无法读取应用", color = Ink); TextButton(onClick = model::refresh) { Text("重新加载") }
                                }
                                model.home.isEmpty() -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("留一点空白，也很好。", color = Muted)
                                    TextButton(onClick = { overlay = "drawer" }) { Text("从应用库添加应用") }
                                }
                                else -> Box(
                                    Modifier.fillMaxSize()
                                        .onGloballyPositioned {
                                            pagerCoordinates = it
                                            pagerBounds = it.boundsInRoot()
                                        }
                                        .then(if (editing) Modifier.pointerInput(editing, pages) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { localPoint ->
                                                    val rootPoint = pagerCoordinates?.localToRoot(localPoint) ?: localPoint
                                                    tileBounds.entries.firstOrNull { it.value.contains(rootPoint) }?.key?.let { id ->
                                                        autoPageJob?.cancel()
                                                        draggingId = id
                                                        dragPoint = rootPoint
                                                        lastDropTarget = null
                                                        dragOverDock = dockBounds?.contains(rootPoint) == true
                                                    }
                                                },
                                                onDrag = { change, _ ->
                                                    val rootPoint = pagerCoordinates?.localToRoot(change.position) ?: change.position
                                                    if (draggingId != null) handleDrag(rootPoint)
                                                    change.consume()
                                                },
                                                onDragEnd = { endDrag(commit = true) },
                                                onDragCancel = { endDrag(commit = false) }
                                            )
                                        } else Modifier)
                                ) {
                                    HorizontalPager(
                                        state = pager, userScrollEnabled = !editing,
                                        beyondViewportPageCount = 1,
                                        flingBehavior = PagerDefaults.flingBehavior(state = pager, snapAnimationSpec = LauncherMotion.page(motion)),
                                        modifier = Modifier.fillMaxSize(), key = { it }
                                    ) { page ->
                                    BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer {
                                        val pageOffset = if (motion) ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f) else 0f
                                        scaleX = 1f - pageOffset * .012f
                                        scaleY = 1f - pageOffset * .012f
                                    }) {
                                        val cols = HomeLayout.columns(landscape)
                                        val rows = HomeLayout.rows(landscape)
                                        val iconSize = minOf(if (landscape) 62.dp else 68.dp, (maxHeight / rows - 38.dp).coerceAtLeast(32.dp), (maxWidth / cols - 16.dp).coerceAtLeast(32.dp))
                                        val members = model.home.drop(page * HomeLayout.PAGE_SIZE).take(HomeLayout.PAGE_SIZE)
                                        val cellHeight = maxHeight / rows
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(cols),
                                            userScrollEnabled = false,
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalArrangement = Arrangement.Top
                                        ) {
                                            items(members, key = { it }) { id ->
                                                Box(
                                                    Modifier.height(cellHeight).animateItem(
                                                        placementSpec = spring(stiffness = 650f, dampingRatio = 0.82f)
                                                    ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    HomeTile(id, appMap, folderMap, iconSize, editing, dragging = draggingId == id,
                                                        onDelete = { deleteTarget = id },
                                                        onClick = {
                                                            if (id.startsWith("folder:")) { target = id; overlay = "folder" }
                                                            else appMap[id]?.let(::launch)
                                                        },
                                                        onDragStart = {},
                                                        onDrag = {},
                                                        onDragEnd = {},
                                                        onPositioned = { bounds -> tileBounds[id] = bounds })
                                                }
                                            }
                                        }
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().height(42.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.width((pages * 14 + 10).dp).height(20.dp)
                        .semantics { contentDescription = "第 ${pager.settledPage + 1} 页，共 $pages 页" }) {
                        val spacing = 14.dp.toPx()
                        val start = 12.dp.toPx()
                        repeat(pages) { index -> drawCircle(Color.White.copy(alpha = .36f), 2.5.dp.toPx(), Offset(start + index * spacing, size.height / 2)) }
                        val position = (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, (pages - 1).toFloat())
                        drawRoundRect(Color.White.copy(alpha = .96f),
                            topLeft = Offset(start + position * spacing - 7.5.dp.toPx(), size.height / 2 - 2.5.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(15.dp.toPx(), 5.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                    }
                }
                val recentApps = DockHistory.visible(model.recentAppIds, appMap.keys, model.dock)
                    .mapNotNull(appMap::get)
                val dockApps = model.dock.mapNotNull(appMap::get)
                GlassDock(
                    motion = motion && overlay.isEmpty(),
                    dropHint = if (dragOverDock) {
                        when {
                            draggingId?.startsWith("folder:") == true -> "仅支持应用图标"
                            draggingId in model.dock || model.dock.size < 4 -> "松开放入常驻栏"
                            else -> "常驻栏已满"
                        }
                    } else null,
                    modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 620.dp).padding(bottom = 12.dp)
                        .onGloballyPositioned { dockBounds = it.boundsInRoot() }
                ) {
                    BoxWithConstraints {
                        val metrics = DockLayout.metrics(dockApps.size, recentApps.size, maxWidth.value)
                        val iconSize = metrics.iconSize.dp
                        Row(
                            Modifier.width(metrics.width.dp).horizontalScroll(rememberScrollState(), enabled = metrics.scrollable)
                                .padding(horizontal = metrics.padding.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(metrics.gap.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DockAction("打开全部应用", size = iconSize, onClick = { overlay = "drawer" }) {
                                AllAppsMark(Modifier.size(iconSize * .48f))
                            }
                            if (dockApps.isNotEmpty()) DockDivider()
                            dockApps.forEach { app ->
                                key(app.id) { AppIcon(app, iconSize, onClick = { launch(app) }) }
                            }
                            if (recentApps.isNotEmpty()) {
                                DockDivider()
                                recentApps.forEach { app ->
                                    key(app.id) {
                                        AppIcon(app, iconSize, onClick = { launch(app) }, description = "最近使用：${app.label}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val previewPoint: Offset? = dragPoint
    if (draggingId != null && previewPoint != null) {
        DragPreview(
            id = draggingId!!,
            app = appMap[draggingId!!],
            folder = folderMap[draggingId!!],
            folderApps = appMap,
            point = previewPoint,
            size = 68.dp
        )
    }

    LauncherOverlay(overlay = overlay, onDismiss = { overlay = "" }) { displayedOverlay ->
    when (displayedOverlay) {
        "drawer" -> HomeModal("全部应用", { overlay = "" }) {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, placeholder = { Text("搜索应用名称") }, shape = RoundedCornerShape(18.dp))
            val results = model.apps.filter { it.label.contains(query.trim(), true) || it.packageName.contains(query.trim(), true) }
            Text("${results.size} 个应用 · 长按可添加到桌面", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
            if (results.isEmpty()) Text("没有找到应用，换个名称试试。", color = Muted, modifier = Modifier.padding(20.dp))
            LazyVerticalGrid(GridCells.Adaptive(106.dp), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(results, key = { it.id }) { app -> AppTile(app, 64.dp, { launch(app) }, labelColor = Ink) }
            }
        }
        "rename" -> HomeModal("重命名文件夹", { overlay = "" }, compact = true) {
            OutlinedTextField(
                value = folderName, onValueChange = { folderName = it.take(16) },
                singleLine = true, label = { Text("文件夹名称") }, modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { model.renameFolder(target, folderName); overlay = "" },
                enabled = folderName.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) { Text("保存") }
        }
        "folder" -> {
            val folder = folderMap[target]
            HomeModal(folder?.name ?: "文件夹", { overlay = "" }) {
                LazyVerticalGrid(GridCells.Adaptive(106.dp), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(22.dp), contentPadding = PaddingValues(vertical = 20.dp)) {
                    items(folder?.apps.orEmpty().mapNotNull(appMap::get), key = { it.id }) { app -> AppTile(app, 66.dp, { launch(app) }, labelColor = Ink) }
                }
                TextButton(onClick = { renameFolder(target) }) { Text("重命名文件夹") }
            }
        }
        "create" -> HomeModal("新建文件夹", { overlay = "" }) {
            OutlinedTextField(value = folderName, onValueChange = { folderName = it.take(16) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("文件夹名称") }, placeholder = { Text("例如：工作、影音") })
            Text("选择至少 2 个应用 · 已选 ${folderMembers.size} 个", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 14.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(model.apps, key = { it.id }) { app ->
                    Row(Modifier.fillMaxWidth().clickable { folderMembers = if (app.id in folderMembers) folderMembers - app.id else folderMembers + app.id }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(app.icon.asImageBitmap(), null, Modifier.size(38.dp)); Text(app.label, Modifier.weight(1f).padding(start = 12.dp), color = Ink)
                        Checkbox(checked = app.id in folderMembers, onCheckedChange = { folderMembers = if (it) (folderMembers + app.id).distinct() else folderMembers - app.id })
                    }
                }
            }
            Button(onClick = { model.createFolder(folderName, folderMembers); overlay = ""; scope.launch { goToPage(0) } }, enabled = folderMembers.size >= 2, modifier = Modifier.fillMaxWidth()) { Text("创建文件夹") }
        }
        "settings" -> LauncherSettingsPage(
            model = model,
            onBack = { overlay = "" },
            onCreateFolder = { beginFolder() },
            onOpenWidgets = { overlay = "widgets" },
            onSetDefaultHome = {
                runCatching {
                    val roleManager = if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java) else null
                    val intent = if (Build.VERSION.SDK_INT >= 29 && roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    } else Intent(Settings.ACTION_HOME_SETTINGS)
                    defaultHomeLauncher.launch(intent)
                }.onFailure { message("请在系统设置中选择默认桌面") }
            }
        )
        "widgets" -> HomeModal("添加系统小组件", { overlay = "" }) {
            val providers = remember(widgetHost) { widgetHost.providers() }
            Text("选择系统提供的小组件，绑定过程会由系统确认。", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            if (providers.isEmpty()) {
                Text("当前设备没有可用的小组件。", color = Muted, modifier = Modifier.padding(20.dp))
            } else {
                LazyColumn(Modifier.weight(1f)) {
                    items(providers, key = { it.provider.flattenToString() }) { provider ->
                        val label = provider.loadLabel(context.packageManager).toString()
                        Row(Modifier.fillMaxWidth().clickable {
                            overlay = ""
                            widgetHost.beginBind(provider) { intent ->
                                runCatching { widgetActivityLauncher.launch(intent) }.onFailure { message("无法打开系统小组件设置") }
                            }
                        }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(label, color = Ink, fontSize = 16.sp)
                                Text(if (provider.configure != null) "需要完成一次配置" else "可直接添加", color = Muted, fontSize = 12.sp)
                            }
                            Text("添加 ›", color = Color(0xFF426D87), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        "weather" -> LauncherWeatherPage(model, onBack = { overlay = "" })
    }
    }
    }

    // Keep this dialog outside LauncherOverlay: that host is only composed while
    // another full-screen overlay is visible, while edit badges are always usable.
    deleteTarget?.let { id ->
        val app = appMap[id]
        val folder = folderMap[id]
        if (app != null || folder != null) {
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text(if (app != null) "删除“${app.label}”？" else "删除文件夹“${folder?.name}”？") },
                text = {
                    Text(if (app != null) "可以只从桌面移除，或继续卸载设备上的应用。卸载将由系统确认。"
                    else "解散文件夹会将其中的应用保留在桌面。")
                },
                confirmButton = {
                    if (app != null) {
                        TextButton(onClick = {
                            deleteTarget = null
                            runCatching {
                                uninstallLauncher.launch(Intent(Intent.ACTION_DELETE, ("package:" + app.packageName).toUri()))
                            }.onFailure { message("无法启动系统卸载流程") }
                        }) { Text("卸载应用", color = MaterialTheme.colorScheme.error) }
                    } else {
                        TextButton(onClick = {
                            model.dissolveFolder(id)
                            deleteTarget = null
                        }) { Text("解散文件夹") }
                    }
                },
                dismissButton = {
                    Row {
                        if (app != null) {
                            TextButton(onClick = {
                                model.removeHome(id)
                                deleteTarget = null
                                message("已从桌面移除")
                            }) { Text("从桌面移除") }
                        }
                        TextButton(onClick = { deleteTarget = null }) { Text("取消") }
                    }
                }
            )
        }
    }
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeToolbarAction(label: String, icon: Int, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(icon), contentDescription = label,
                tint = Color.White.copy(alpha = .94f), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun HomeTile(
    id: String,
    apps: Map<String, LauncherApp>,
    folders: Map<String, HomeFolder>,
    size: Dp,
    editing: Boolean,
    dragging: Boolean = false,
    onDelete: () -> Unit,
    entranceDelayMs: Int = 0,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onPositioned: (androidx.compose.ui.geometry.Rect) -> Unit
) {
    val app = apps[id]
    if (app != null) AppTile(app, size, onClick, editing, dragging, onDelete, onPositioned, entranceDelayMs, animateEntrance = false)
    else folders[id]?.let { folder ->
        var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
        val interaction = remember(id) { MutableInteractionSource() }
        val haptic = LocalHapticFeedback.current
        val dragScale by animateFloatAsState(if (dragging) 1.08f else 1f, spring(stiffness = 650f, dampingRatio = 0.82f), label = "folder-drag-scale")
        val dragAlpha by animateFloatAsState(if (dragging) .04f else 1f, tween(if (dragging) 90 else 180), label = "folder-drag-alpha")
        Box(Modifier.widthIn(max = 150.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconMenuAnchor(id, preview = { FolderIcon(folder, apps, Modifier.fillMaxSize()) }) { openMenu, iconModifier ->
                Column(
                    Modifier.pressFeedback(interaction, enabled = !editing)
                        .graphicsLayer { scaleX = dragScale; scaleY = dragScale; alpha = dragAlpha }
                        .onGloballyPositioned { coordinates = it; onPositioned(it.boundsInRoot()) }
                        .then(if (editing) Modifier
                            else Modifier.combinedClickable(
                                interactionSource = interaction, indication = null, onClick = onClick,
                                onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); openMenu() }
                            ))
                        .semantics { stateDescription = if (editing) "可编辑，拖动可排序" else "" }
                        .padding(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(size).then(iconModifier)) {
                        FolderIcon(folder, apps, Modifier.fillMaxSize())
                        if (editing) EditBadge(Modifier.align(Alignment.TopStart), onDelete)
                    }
                    Text(folder.name, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun FolderIcon(folder: HomeFolder, apps: Map<String, LauncherApp>, modifier: Modifier) {
    Surface(shape = RoundedCornerShape(23.dp), color = Color.White.copy(alpha = .58f), modifier = modifier) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(2) { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(2) { col ->
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            folder.apps.getOrNull(row * 2 + col)?.let(apps::get)?.let {
                                Image(it.icon.asImageBitmap(), null, Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DragPreview(
    id: String,
    app: LauncherApp?,
    folder: HomeFolder?,
    folderApps: Map<String, LauncherApp>,
    point: Offset,
    size: Dp
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val scale by animateFloatAsState(1.08f, spring(stiffness = 520f, dampingRatio = 0.74f), label = "drag-preview-scale")
    val alpha by animateFloatAsState(1f, tween(120), label = "drag-preview-alpha")
    Box(
        Modifier
            .offset {
                IntOffset(
                    (point.x - with(density) { size.toPx() } / 2f).roundToInt(),
                    (point.y - with(density) { size.toPx() } / 2f).roundToInt()
                )
            }
            .size(size)
            .zIndex(20f)
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .shadow(22.dp, RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = .18f))
            .semantics { contentDescription = "正在移动：${app?.label ?: folder?.name ?: id}" }
    ) {
        if (app != null) {
            Image(app.icon.asImageBitmap(), null, Modifier.fillMaxSize())
        } else if (folder != null) {
            Surface(shape = RoundedCornerShape(22.dp), color = Color.White.copy(alpha = .58f), modifier = Modifier.fillMaxSize()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(2) { row ->
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(2) { col ->
                                Box(Modifier.weight(1f).fillMaxHeight()) {
                                    folder.apps.getOrNull(row * 2 + col)?.let { memberId ->
                                        folderApps[memberId]?.let { Image(it.icon.asImageBitmap(), null, Modifier.fillMaxSize()) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTile(
    app: LauncherApp,
    size: Dp,
    onClick: () -> Unit,
    editing: Boolean = false,
    dragging: Boolean = false,
    onDelete: () -> Unit = {},
    onPositioned: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    entranceDelayMs: Int = 0,
    animateEntrance: Boolean = true,
    labelColor: Color = Color.White
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val interaction = remember(app.id) { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val dragScale by animateFloatAsState(if (dragging) 1.08f else 1f, spring(stiffness = 650f, dampingRatio = 0.82f), label = "app-drag-scale")
    val dragAlpha by animateFloatAsState(if (dragging) .04f else 1f, tween(if (dragging) 90 else 180), label = "app-drag-alpha")
    AnimatedVisibility(
        visibleState = rememberEntranceState(animateEntrance),
        modifier = Modifier.widthIn(max = 150.dp).fillMaxWidth(),
        enter = fadeIn(tween(180, delayMillis = entranceDelayMs)) + scaleIn(tween(280, delayMillis = entranceDelayMs, easing = LauncherMotion.easeOut), initialScale = .96f)
    ) {
        IconMenuAnchor(app.id, preview = {
            Image(app.icon.asImageBitmap(), null, Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)))
        }) { openMenu, iconModifier ->
            Column(
            Modifier.pressFeedback(interaction, enabled = !editing)
                .graphicsLayer {
                    scaleX = dragScale
                    scaleY = dragScale
                    alpha = dragAlpha
                }
                    .onGloballyPositioned { coordinates = it; onPositioned(it.boundsInRoot()) }
                    .then(if (editing) Modifier
                        else Modifier.combinedClickable(
                            interactionSource = interaction, indication = null, onClick = onClick,
                            onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); openMenu() }
                        ))
                    .semantics { stateDescription = if (editing) "可编辑，拖动可排序" else "" }
                    .padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(size).then(iconModifier)) {
                    Image(app.icon.asImageBitmap(), "应用图标：" + app.label, Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)))
                    if (editing) EditBadge(Modifier.align(Alignment.TopStart), onDelete)
                }
                Text(
                    app.label, color = labelColor, fontSize = 13.sp, textAlign = TextAlign.Center,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun EditBadge(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.offset(x = (-5).dp, y = (-5).dp).size(28.dp)
            .clip(CircleShape).background(Color(0xFFE6535B))
            .border(2.dp, Color.White.copy(alpha = .96f), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "删除" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(12.dp)) {
            drawLine(Color.White, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 2.dp.toPx())
        }
    }
}

@Composable
private fun AppIcon(app: LauncherApp, size: Dp, onClick: () -> Unit, description: String = "打开" + app.label) {
    val interaction = remember(app.id) { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    AnimatedVisibility(visibleState = rememberEntranceState(), enter = fadeIn(tween(200)) + scaleIn(tween(300, easing = LauncherMotion.easeOut), initialScale = .94f)) {
        IconMenuAnchor(app.id, preview = {
            Image(app.icon.asImageBitmap(), null, Modifier.fillMaxSize().clip(RoundedCornerShape(17.dp)))
        }) { openMenu, iconModifier ->
            Box(
                Modifier.size(size).then(iconModifier).pressFeedback(interaction).clip(RoundedCornerShape(17.dp))
                    .combinedClickable(
                        interactionSource = interaction, indication = null, onClick = onClick,
                        onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); openMenu() }
                    )
                    .semantics { contentDescription = description }
            ) { Image(app.icon.asImageBitmap(), null, Modifier.fillMaxSize()) }
        }
    }
}

@Composable
private fun GlassDock(
    motion: Boolean,
    modifier: Modifier = Modifier,
    dropHint: String? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(27.dp)
    val borderColor by animateColorAsState(
        targetValue = when {
            dropHint == null -> Color.White.copy(alpha = .42f)
            dropHint == "常驻栏已满" -> Color(0xFFE78383)
            else -> Color.White.copy(alpha = .98f)
        },
        animationSpec = tween(160), label = "dock-drop-border"
    )
    val lifecycle by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val running = motion && lifecycle.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
    val sheen = if (running) {
        val transition = rememberInfiniteTransition(label = "dock-glass-sheen")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Restart
            ),
            label = "dock-glass-sheen-position"
        )
    } else remember { mutableFloatStateOf(.5f) }
    Box(
        modifier.shadow(8.dp, shape, clip = false).animateContentSize(LauncherMotion.layer(motion)).clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .32f), Color.White.copy(alpha = .20f), Color.White.copy(alpha = .26f))))
            .border(androidx.compose.foundation.BorderStroke(if (dropHint == null) 1.dp else 2.dp,
                if (dropHint == null) Brush.verticalGradient(listOf(Color.White.copy(alpha = .48f), Color.White.copy(alpha = .18f))) else Brush.verticalGradient(listOf(borderColor, borderColor.copy(alpha = .76f)))), shape)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val x = sheen.value * size.width
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = .42f), Color.Transparent),
                    startX = x - size.width * .24f,
                    endX = x + size.width * .24f
                ),
                start = Offset(18.dp.toPx(), 1.5.dp.toPx()),
                end = Offset(size.width - 18.dp.toPx(), 1.5.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
        content()
        if (dropHint != null) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
                color = if (dropHint == "常驻栏已满") Color(0xFFE78383) else Color(0xFF426D87),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp
            ) {
                Text(dropHint, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
            }
        }
    }
}

@Composable
private fun DockAction(description: String, size: Dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.size(size).pressFeedback(interaction).clip(RoundedCornerShape(size * .32f))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .52f), Color.White.copy(alpha = .29f))))
            .border(1.dp, Color.White.copy(alpha = .52f), RoundedCornerShape(size * .32f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun DockDivider() {
    Box(Modifier.width(1.dp).height(32.dp).background(Color.White.copy(alpha = .40f)))
}

@Composable
private fun AllAppsMark(modifier: Modifier = Modifier) {
    val colors = listOf(Color.White, Color(0xFFF2FAFF), Color(0xFFE9F3FA), Color.White.copy(alpha = .93f))
    Canvas(modifier) {
        val gap = size.minDimension * .09f
        val tile = (size.minDimension - gap) / 2f
        repeat(4) { index ->
            val x = (index % 2) * (tile + gap)
            val y = (index / 2) * (tile + gap)
            drawRoundRect(colors[index], Offset(x, y), androidx.compose.ui.geometry.Size(tile, tile),
                androidx.compose.ui.geometry.CornerRadius(tile * .24f))
        }
    }
}

private val settingsSections = listOf("外观", "桌面", "功能", "关于")
private val settingsSectionMarks = mapOf("外观" to "appearance", "桌面" to "home", "功能" to "features", "关于" to "about")

private val SettingsCanvas = Color(0xFFF5F7F7)
private val SettingsAccent = Color(0xFF426D87)
private val SettingsLine = Color(0xFFE6ECEF)

@Composable
private fun SettingsGlyph(name: String, modifier: Modifier = Modifier, color: Color = SettingsAccent) {
    Canvas(modifier) {
        val stroke = size.minDimension * .09f
        val left = size.width * .18f
        val right = size.width * .82f
        val top = size.height * .18f
        val bottom = size.height * .82f
        val center = Offset(size.width / 2f, size.height / 2f)
        when (name) {
            "appearance", "palette" -> {
                drawCircle(color, radius = size.minDimension * .32f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawCircle(color, radius = size.minDimension * .10f, center = center)
                drawCircle(color.copy(alpha = .55f), radius = size.minDimension * .055f, center = Offset(size.width * .68f, size.height * .34f))
            }
            "home" -> {
                val path = Path().apply {
                    moveTo(left, size.height * .48f); lineTo(center.x, top); lineTo(right, size.height * .48f)
                    lineTo(right * .91f, size.height * .48f); lineTo(right * .91f, bottom)
                    lineTo(left * 1.09f, bottom); lineTo(left * 1.09f, size.height * .48f); close()
                }
                drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            "about" -> {
                drawCircle(color, radius = size.minDimension * .34f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawCircle(color, radius = stroke * 1.25f, center = Offset(center.x, size.height * .34f))
                drawLine(color, Offset(center.x, size.height * .48f), Offset(center.x, size.height * .69f), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            "motion" -> {
                val path = Path().apply {
                    moveTo(center.x, top); lineTo(center.x + size.width * .09f, center.y - size.height * .09f)
                    lineTo(right, center.y); lineTo(center.x + size.width * .09f, center.y + size.height * .09f)
                    lineTo(center.x, bottom); lineTo(center.x - size.width * .09f, center.y + size.height * .09f)
                    lineTo(left, center.y); lineTo(center.x - size.width * .09f, center.y - size.height * .09f); close()
                }
                drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            "layout", "widgets", "cards" -> {
                val gap = size.minDimension * .10f
                val cell = (size.minDimension - gap * 3f) / 2f
                for (row in 0..1) for (column in 0..1) {
                    val x = left + column * (cell + gap)
                    val y = top + row * (cell + gap)
                    drawRoundRect(color, Offset(x, y), androidx.compose.ui.geometry.Size(cell, cell), androidx.compose.ui.geometry.CornerRadius(stroke * 1.6f))
                }
            }
            else -> {
                drawLine(color, Offset(left, center.y), Offset(right, center.y), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color, Offset(center.x, top), Offset(center.x, bottom), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun LauncherSettingsPage(
    model: LauncherModel,
    onBack: () -> Unit,
    onCreateFolder: () -> Unit,
    onOpenWidgets: () -> Unit,
    onSetDefaultHome: () -> Unit
) {
    var section by rememberSaveable { mutableStateOf(settingsSections.first()) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 760.dp
        Surface(Modifier.fillMaxSize(), color = SettingsCanvas) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().height(if (wide) 76.dp else 68.dp).padding(horizontal = if (wide) 32.dp else 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(onClick = onBack, shape = CircleShape, color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, SettingsLine)) {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Text("‹", color = Ink, fontSize = 27.sp, modifier = Modifier.offset(y = (-1).dp))
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text("设置", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Text("桌面与偏好", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = androidx.compose.foundation.BorderStroke(1.dp, SettingsLine)) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).background(Color(0xFF6D9A87), CircleShape))
                            Text("EasyLauncher", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 7.dp))
                            Text("0.2.0", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                HorizontalDivider(color = SettingsLine)
                if (wide) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        Column(Modifier.width(218.dp).fillMaxHeight().padding(start = 22.dp, top = 24.dp, end = 12.dp)) {
                            Text("桌面偏好", color = Muted, fontSize = 10.sp, letterSpacing = .7.sp,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp, bottom = 14.dp))
                            settingsSections.forEach { item ->
                                SettingsNavigationItem(item, selected = section == item, modifier = Modifier.fillMaxWidth()) { section = item }
                            }
                            Spacer(Modifier.weight(1f))
                            Text("设置会自动保存在此设备", color = Muted, fontSize = 10.sp,
                                modifier = Modifier.padding(start = 12.dp, bottom = 18.dp))
                        }
                        Box(Modifier.weight(1f).fillMaxHeight().padding(start = 14.dp, end = 32.dp, top = 22.dp, bottom = 18.dp), contentAlignment = Alignment.TopCenter) {
                            AnimatedContent(
                                targetState = section,
                                transitionSpec = {
                                    (fadeIn(tween(180, easing = FastOutSlowInEasing)) + slideInVertically(tween(180, easing = FastOutSlowInEasing)) { it / 40 })
                                        .togetherWith(fadeOut(tween(100)))
                                },
                                label = "settings-section"
                            ) { target ->
                                Box(Modifier.widthIn(max = 1100.dp).fillMaxWidth().fillMaxHeight()) {
                                    SettingsSectionContent(target, model, onCreateFolder, onOpenWidgets, onSetDefaultHome)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        settingsSections.forEach { item ->
                            SettingsNavigationItem(item, selected = section == item, compact = true) { section = item }
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.TopCenter) {
                        AnimatedContent(
                            targetState = section,
                            transitionSpec = {
                                (fadeIn(tween(180, easing = FastOutSlowInEasing)) + slideInVertically(tween(180, easing = FastOutSlowInEasing)) { it / 40 })
                                    .togetherWith(fadeOut(tween(100)))
                            },
                            label = "settings-section"
                        ) { target ->
                            Box(Modifier.widthIn(max = 900.dp).fillMaxWidth().fillMaxHeight()) {
                                SettingsSectionContent(target, model, onCreateFolder, onOpenWidgets, onSetDefaultHome)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(label: String, selected: Boolean, modifier: Modifier = Modifier, compact: Boolean = false, onClick: () -> Unit) {
    val background = if (selected) Color(0xFFE5EEF2) else Color.Transparent
    Surface(onClick = onClick, modifier = modifier.padding(vertical = if (compact) 0.dp else 3.dp),
        shape = RoundedCornerShape(if (compact) 15.dp else 16.dp), color = background) {
        Row(Modifier.height(if (compact) 42.dp else 48.dp).padding(horizontal = if (compact) 14.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(if (compact) 28.dp else 34.dp).background(
                if (selected) Color.White else Color(0xFFE9EEF0), RoundedCornerShape(if (compact) 9.dp else 11.dp)),
                contentAlignment = Alignment.Center) {
                SettingsGlyph(settingsSectionMarks[label].orEmpty(), Modifier.size(if (compact) 15.dp else 18.dp),
                    if (selected) SettingsAccent else Muted)
            }
            Text(label, color = Ink, fontSize = if (compact) 12.sp else 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier.padding(start = 10.dp).weight(1f))
            if (selected && !compact) Box(Modifier.size(6.dp).background(SettingsAccent, CircleShape))
        }
    }
}

@Composable
private fun SettingsSectionContent(
    section: String,
    model: LauncherModel,
    onCreateFolder: () -> Unit,
    onOpenWidgets: () -> Unit,
    onSetDefaultHome: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        when (section) {
            "外观" -> {
                SettingsPageHeading("外观", "调整桌面背景与视觉反馈。更改会立即应用并自动保存。")
                SettingsGroup("壁纸色调", "选择一组适合桌面图标的柔和配色。") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("云山", "青岚", "暮色").forEachIndexed { index, label ->
                            val selected = model.palette == index
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    onClick = { model.choosePalette(index) },
                                    modifier = Modifier.fillMaxWidth().height(84.dp).border(
                                        if (selected) 2.dp else 1.dp,
                                        if (selected) SettingsAccent else SettingsLine, RoundedCornerShape(15.dp)
                                    ), shape = RoundedCornerShape(16.dp), color = palettes[index][1]
                                ) {
                                    Box {
                                        Canvas(Modifier.fillMaxSize()) {
                                            drawRect(Brush.verticalGradient(palettes[index], endY = size.height))
                                            val y = size.height * .72f
                                            val path = Path().apply { moveTo(0f, y); cubicTo(size.width * .25f, y - 28f, size.width * .55f, y + 18f, size.width, y - 8f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                                            drawPath(path, palettes[index][2].copy(alpha = .62f))
                                        }
                                        if (selected) Surface(Modifier.align(Alignment.TopEnd).padding(7.dp), shape = CircleShape,
                                            color = SettingsAccent) {
                                            Text("✓", color = Color.White, fontSize = 9.sp, lineHeight = 13.sp,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text(label, color = if (selected) SettingsAccent else Ink, fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    modifier = Modifier.padding(top = 7.dp))
                            }
                        }
                    }
                }
                SettingsGroup("天气背景", "沿用当前壁纸色调，跟随天气卡片所选城市和当地昼夜变化。") {
                    SettingsToggleRow("动态天气背景", "晴光、云雾、雨雪随实况变化；省电模式下自动静止。", model.weatherWallpaper, model::updateWeatherWallpaper)
                }
                SettingsGroup("动态效果", "减少动态效果会关闭桌面视差、缩放及天气背景动画，并继续遵循系统动画设置。") {
                    SettingsToggleRow("减少动态效果", "适合偏好静态界面或希望减少视觉动态的情况。", model.reduceMotion, model::updateReduceMotion)
                }
            }
            "桌面" -> {
                val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                SettingsPageHeading("桌面", "管理布局和默认桌面行为。图标、文件夹与常驻栏会自动保存。")
                SettingsGroup("图标布局", "当前方向自动适配，旋转设备后会即时调整。") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsValueTile("每行图标", "${HomeLayout.columns(landscape)} 列", Modifier.weight(1f))
                        SettingsValueTile("图标行数", "${HomeLayout.rows(landscape)} 行", Modifier.weight(1f))
                        SettingsValueTile("每屏容量", "${HomeLayout.PAGE_SIZE} 个", Modifier.weight(1f))
                    }
                    Text("横屏 7 列 × 2 行 · 竖屏 4 列 × 4 行", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                }
                SettingsGroup("桌面整理", "快速整理主屏上的应用与内容。") {
                    SettingsActionRow("新建文件夹", "将常用应用归类到一起。", "›", onCreateFolder)
                    HorizontalDivider(color = Color(0xFFE7ECEF))
                    SettingsActionRow("添加系统小组件", "浏览设备上可用的小组件。", "›", onOpenWidgets)
                }
                SettingsGroup("默认桌面", "使用 Home 键时打开的桌面应用。") {
                    SettingsStatusRow(if (model.isDefaultHome) "EasyLauncher 已设为默认" else "当前不是默认桌面", model.isDefaultHome)
                    if (!model.isDefaultHome) {
                        SettingsActionRow("设为默认桌面", "将由 Android 系统确认并完成设置。", "›", onSetDefaultHome)
                    }
                }
            }
            "功能" -> {
                SettingsPageHeading("功能", "管理桌面信息卡片与快捷入口。")
                SettingsGroup("信息卡片", "选择显示在桌面顶部的信息。") {
                    SettingsToggleRow("天气卡片", "显示所选城市的天气信息。", model.weatherVisible, { model.toggleWeather() })
                    Text("当前城市：${model.weatherCity.name}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 2.dp, top = 8.dp))
                }
                SettingsGroup("系统组件", "使用 Android 提供的桌面小组件。") {
                    SettingsActionRow("管理小组件", "添加或移除设备中的小组件。", "›", onOpenWidgets)
                }
            }
            else -> {
                SettingsPageHeading("关于", "EasyLauncher 为平板使用场景设计。")
                SettingsGroup("应用信息", "") {
                    SettingsValueRow("应用名称", "EasyLauncher")
                    HorizontalDivider(color = Color(0xFFE7ECEF))
                    SettingsValueRow("版本", "0.2.0")
                    HorizontalDivider(color = Color(0xFFE7ECEF))
                    SettingsValueRow("天气数据", "和风天气 · QWeather")
                    HorizontalDivider(color = Color(0xFFE7ECEF))
                    SettingsValueRow("布局偏好", "保存在此设备")
                }
                Text("天气数据需要网络连接；桌面布局与偏好设置保存在本机。", color = Muted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SettingsPageHeading(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(start = 2.dp, bottom = 2.dp)) {
        Text("桌面偏好  ·  ${settingsSections.indexOf(title).plus(1).toString().padStart(2, '0')}",
            color = SettingsAccent, fontSize = 9.sp, letterSpacing = .7.sp, fontWeight = FontWeight.SemiBold)
        Text(title, color = Ink, fontSize = 25.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 3.dp))
        Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun SettingsGroup(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    val mark = when (title) {
        "壁纸色调" -> "palette"
        "动态效果" -> "motion"
        "图标布局" -> "layout"
        "桌面整理" -> "organize"
        "默认桌面" -> "home"
        "信息卡片" -> "cards"
        "系统组件" -> "widgets"
        "应用信息" -> "about"
        else -> "features"
    }
    Surface(shape = RoundedCornerShape(21.dp), color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, SettingsLine)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).background(Color(0xFFEAF1F3), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                    SettingsGlyph(mark, Modifier.size(17.dp), SettingsAccent)
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(title, color = Ink, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)
                    if (description.isNotBlank()) Text(description, color = Muted, fontSize = 10.sp, lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
            Spacer(Modifier.height(11.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 14.dp)) {
            Text(title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(description, color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingsValueTile(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = Color(0xFFF4F7F8),
        border = androidx.compose.foundation.BorderStroke(1.dp, SettingsLine)) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 10.dp)) {
            Text(title, color = Muted, fontSize = 9.sp)
            Text(value, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun SettingsValueRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 38.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = Muted, fontSize = 11.sp)
        Text(value, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsStatusRow(title: String, active: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(if (active) Color(0xFF4D9B7C) else Color(0xFFC3A36A), CircleShape))
        Text(title, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 9.dp))
    }
}

@Composable
private fun SettingsActionRow(title: String, description: String, trailing: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 2.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(description, color = Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Box(Modifier.size(25.dp).background(Color(0xFFF0F4F5), CircleShape), contentAlignment = Alignment.Center) {
                Text(if (trailing == "›") "→" else trailing, color = SettingsAccent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HomeModal(title: String, onClose: () -> Unit, compact: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(28.dp), color = Color(0xFFF4F8FA),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .7f)),
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth().then(if (compact) Modifier.heightIn(max = 650.dp) else Modifier.fillMaxHeight(.94f))
    ) {
        Column(Modifier.padding(22.dp).then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Medium)
                TextButton(onClick = onClose) { Text("关闭") }
            }
            content()
        }
    }
}

@Composable
private fun Modifier.pressFeedback(interaction: MutableInteractionSource, enabled: Boolean = true): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val motion = LocalLauncherMotionEnabled.current
    val scale by animateFloatAsState(if (pressed && motion && enabled) .94f else 1f, LauncherMotion.layer(motion), label = "icon-press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
        Text(label, Modifier.weight(1f), textAlign = TextAlign.Start, fontSize = 15.sp)
        Text("›", color = Muted)
    }
}
