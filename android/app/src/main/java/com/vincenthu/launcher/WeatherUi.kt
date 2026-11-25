package com.vincenthu.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.stateDescription
import kotlinx.coroutines.flow.drop
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val WeatherInk = Color(0xFF29465A)
private val WeatherMuted = Color(0xFF738B98)
private val WeatherAccent = Color(0xFF477C98)
private val WeatherCanvas = Color(0xFFF5F7F7)
private val WeatherLine = Color(0xFFE5ECEF)
private fun Double.degrees() = "${roundToInt()}°"

/** Draw-only motion: no layout animation, bitmaps, permissions or per-particle coroutines. */
@Composable
internal fun WeatherArtwork(code: Int, isDay: Boolean, modifier: Modifier = Modifier, animated: Boolean = true) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val running = animated && code >= 0 && LocalLauncherMotionEnabled.current && lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    val phaseState: State<Float> = if (running) {
        val transition = rememberInfiniteTransition(label = "weather-scene")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "weather-cycle")
    } else remember { mutableFloatStateOf(0f) }
    Canvas(modifier) {
        val phase = phaseState.value
        val unit = size.minDimension
        val cx = size.width * .5f
        val cy = size.height * .44f
        val rainy = code in 51..67 || code in 80..82 || code in 95..99
        val snowy = code in 71..77 || code in 85..86
        val foggy = code == 45 || code == 48
        val cloudy = code in 1..3 || rainy || snowy || foggy || code < 0
        if (code == 0 || code == 1 || code == 2) {
            val sun = Offset(if (cloudy) cx - unit * .12f else cx, if (cloudy) cy - unit * .09f else cy)
            if (isDay) {
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD993).copy(alpha = .5f), Color.Transparent), sun, unit * .47f), unit * .47f, sun)
                rotate(phase * 45f, sun) {
                    repeat(8) { i ->
                        val angle = i * PI / 4
                        drawLine(Color(0xFFE5B565), sun + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * (unit * .28f), sun + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * (unit * .35f), unit * .035f, StrokeCap.Round)
                    }
                }
                drawCircle(Brush.linearGradient(listOf(Color(0xFFFFE8A5), Color(0xFFEABF70))), unit * .2f, sun)
            } else {
                val moon = Path().apply {
                    moveTo(sun.x + unit * .12f, sun.y - unit * .21f)
                    cubicTo(sun.x - unit * .3f, sun.y - unit * .19f, sun.x - unit * .27f, sun.y + unit * .28f, sun.x + unit * .1f, sun.y + unit * .23f)
                    cubicTo(sun.x + unit * .28f, sun.y + unit * .2f, sun.x + unit * .28f, sun.y + unit * .08f, sun.x + unit * .28f, sun.y + unit * .03f)
                    cubicTo(sun.x + unit * .03f, sun.y + unit * .12f, sun.x - unit * .01f, sun.y - unit * .1f, sun.x + unit * .12f, sun.y - unit * .21f)
                }
                drawPath(moon, Color(0xFFE8EDCC))
                repeat(4) { i ->
                    drawCircle(Color(0xFFE3EBEA).copy(alpha = .4f + .4f * ((sin((phase + i * .2f) * PI * 2) + 1) / 2).toFloat()), unit * .014f, Offset(size.width * (.2f + i * .2f), size.height * (if (i % 2 == 0) .17f else .3f)))
                }
            }
        }
        if (cloudy) {
            val drift = sin(phase * PI * 2).toFloat() * unit * .035f
            translate(drift, 0f) {
                val cloudColor = if (rainy) Color(0xFF91AEC2) else Color(0xFFC4DDE7)
                drawCircle(cloudColor, unit * .19f, Offset(cx - unit * .15f, cy + unit * .03f))
                drawCircle(cloudColor, unit * .24f, Offset(cx + unit * .03f, cy - unit * .05f))
                drawCircle(cloudColor, unit * .16f, Offset(cx + unit * .23f, cy + unit * .05f))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFDFEDF1), cloudColor)), Offset(cx - unit * .32f, cy + unit * .02f), Size(unit * .7f, unit * .25f), CornerRadius(unit * .13f))
            }
        }
        if (rainy || snowy) repeat(if (snowy) 7 else 9) { i ->
            val fall = (phase * (if (snowy) 3 else 7) + i * .173f) % 1f
            val x = cx + ((i % 4) - 1.5f) * unit * .16f - fall * unit * .06f
            val y = cy + unit * .25f + fall * unit * .35f
            val alpha = sin(fall * PI).toFloat() * .8f
            if (snowy) drawCircle(Color.White.copy(alpha = alpha), unit * .018f, Offset(x, y))
            else drawLine(Color(0xFF659FC4).copy(alpha = alpha), Offset(x, y), Offset(x - unit * .025f, y + unit * .07f), unit * .023f, StrokeCap.Round)
        }
        if (foggy) repeat(3) { i ->
            val shift = sin((phase + i * .15f) * PI * 2).toFloat() * unit * .035f
            drawLine(Color(0xFFABC5CE).copy(alpha = .75f), Offset(cx - unit * .32f + shift, cy + unit * (.29f + i * .1f)), Offset(cx + unit * .3f + shift, cy + unit * (.29f + i * .1f)), unit * .035f, StrokeCap.Round)
        }
        if (code in 95..99) {
            val bolt = Path().apply {
                moveTo(cx + unit * .04f, cy + unit * .17f); lineTo(cx - unit * .08f, cy + unit * .4f)
                lineTo(cx + unit * .04f, cy + unit * .37f); lineTo(cx - unit * .01f, cy + unit * .57f)
                lineTo(cx + unit * .2f, cy + unit * .28f); lineTo(cx + unit * .07f, cy + unit * .31f); close()
            }
            drawPath(bolt, Color(0xFFF4CE7F))
        }
    }
}

@Composable
internal fun WeatherHomeCard(
    modifier: Modifier, state: WeatherState, city: WeatherCity, cities: List<WeatherCity>,
    weatherForCity: (WeatherCity) -> WeatherState, onCitySelected: (WeatherCity) -> Unit,
    animate: Boolean, onClick: () -> Unit, interactionEnabled: Boolean = true
) {
    val deck = cities.ifEmpty { listOf(city) }
    val pager = rememberPagerState(initialPage = deck.indexOfFirst { it.id == city.id }.coerceAtLeast(0), pageCount = { deck.size })
    val latestCity by rememberUpdatedState(city)
    val latestDeck by rememberUpdatedState(deck)
    val latestSelect by rememberUpdatedState(onCitySelected)
    val motion = LocalLauncherMotionEnabled.current
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect { index ->
            latestDeck.getOrNull(index)?.takeIf { it.id != latestCity.id }?.let(latestSelect)
        }
    }
    LaunchedEffect(city.id, deck.map { it.id }) {
        val index = deck.indexOfFirst { it.id == city.id }
        if (index >= 0 && index != pager.settledPage && !pager.isScrollInProgress) pager.scrollToPage(index)
    }
    Box(modifier.clip(RoundedCornerShape(20.dp)).semantics {
        stateDescription = "${city.name}天气，第 ${pager.settledPage + 1} 个城市，共 ${deck.size} 个，上下滑动切换城市"
    }) {
        VerticalPager(
            state = pager, modifier = Modifier.fillMaxSize(), userScrollEnabled = interactionEnabled && deck.size > 1,
            beyondViewportPageCount = 1, key = { deck[it].id },
            flingBehavior = PagerDefaults.flingBehavior(state = pager, snapAnimationSpec = LauncherMotion.page(motion))
        ) { index ->
            val pageCity = deck[index]
            val pageState = if (pageCity.id == city.id) state else remember(pageCity.id, state) { weatherForCity(pageCity) }
            WeatherCardContent(Modifier.fillMaxSize().graphicsLayer {
                val offset = kotlin.math.abs((pager.currentPage - index) + pager.currentPageOffsetFraction).coerceIn(0f, 1f)
                alpha = if (motion) 1f - .25f * offset else 1f
                scaleX = if (motion) 1f - .025f * offset else 1f
            }, pageState, pageCity, animate && index == pager.settledPage && !pager.isScrollInProgress, onClick)
        }
        if (deck.size > 1) Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(deck.size) { index ->
                Box(Modifier.width(3.dp).height(if (index == pager.currentPage) 9.dp else 3.dp)
                    .background(WeatherAccent.copy(alpha = if (index == pager.currentPage) .7f else .22f), CircleShape))
            }
        }
    }
}

@Composable
private fun WeatherCardContent(modifier: Modifier, state: WeatherState, city: WeatherCity, animate: Boolean, onClick: () -> Unit) {
    val snapshot = (state as? WeatherState.Ready)?.snapshot
    val motion = LocalLauncherMotionEnabled.current
    val background by animateColorAsState(if (snapshot?.isDay == false) Color(0xFFD0DFEB) else Color(0xFFDDECF0), tween(if (LocalLauncherMotionEnabled.current) 450 else 0), label = "weather-card-sky")
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp), color = background,
        border = BorderStroke(1.dp, Color.White.copy(alpha = .38f))) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val tiny = maxHeight < 118.dp
            val compact = maxHeight < 145.dp
            val small = maxWidth < 175.dp
            Column(Modifier.fillMaxSize().padding(horizontal = if (small) 12.dp else 15.dp, vertical = if (tiny) 6.dp else if (compact) 9.dp else 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(city.name, color = WeatherInk, fontSize = if (tiny) 10.sp else 12.sp, lineHeight = if (tiny) 14.sp else 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(snapshot?.description ?: "天气", color = WeatherMuted, fontSize = if (tiny) 8.sp else 10.sp, lineHeight = if (tiny) 11.sp else 14.sp, maxLines = 1)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    AnimatedContent(snapshot?.temperature?.degrees() ?: "—°", transitionSpec = { fadeIn(tween(if (motion) 220 else 0)).togetherWith(fadeOut(tween(if (motion) 130 else 0))) }, label = "weather-temperature") { value ->
                        Text(value, color = WeatherInk, fontSize = if (tiny) 26.sp else if (compact) 32.sp else 40.sp, lineHeight = if (tiny) 30.sp else if (compact) 36.sp else 44.sp, fontWeight = FontWeight.Light, letterSpacing = (-1).sp)
                    }
                    WeatherArtwork(snapshot?.code ?: -1, snapshot?.isDay ?: true, Modifier.size(if (tiny) 30.dp else if (compact) 38.dp else 52.dp), animate)
                }
                Text(snapshot?.let { "${it.low.degrees()} — ${it.high.degrees()} · 和风天气" } ?: if (state is WeatherState.Error) "连接失败 · 点击重试" else if (state is WeatherState.Idle) "点击连接和风天气" else "正在连接天气服务", color = WeatherMuted, fontSize = if (tiny) 8.sp else 10.sp, lineHeight = if (tiny) 11.sp else 14.sp, maxLines = 1)
                if (snapshot != null) {
                    val stale = System.currentTimeMillis() - snapshot.fetchedAt > 3_600_000
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = .34f)).padding(horizontal = 7.dp, vertical = if (tiny) 2.dp else if (compact) 3.dp else 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (stale) "缓存天气" else "体感 ${snapshot.feelsLike?.degrees() ?: "—"}", fontSize = if (tiny) 8.sp else 9.sp, lineHeight = if (tiny) 11.sp else 13.sp, color = WeatherInk, maxLines = 1)
                        Text("湿度 ${snapshot.humidity?.let { "$it%" } ?: "—"}", fontSize = if (tiny) 8.sp else 9.sp, lineHeight = if (tiny) 11.sp else 13.sp, color = WeatherInk, maxLines = 1)
                    }
                } else if (compact) Text(if (state is WeatherState.Error) "点击查看并重试" else if (state is WeatherState.Idle) "点击配置天气" else "正在更新…", color = WeatherMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
internal fun LauncherWeatherPage(model: LauncherModel, onBack: () -> Unit) {
    var citiesVisible by rememberSaveable { mutableStateOf(false) }
    var configurationVisible by rememberSaveable { mutableStateOf(!model.weatherConfigured) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val snapshot = (model.weatherState as? WeatherState.Ready)?.snapshot
    BoxWithConstraints(Modifier.fillMaxSize().background(WeatherCanvas)) {
        val wide = maxWidth >= 820.dp
        BackHandler(configurationVisible || (!wide && citiesVisible)) { focus.clearFocus(); configurationVisible = false; citiesVisible = false }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = if (wide) 28.dp else 18.dp), verticalAlignment = Alignment.CenterVertically) {
                WeatherIconButton(if (configurationVisible) "返回天气" else "返回桌面", "back", onClick = { if (configurationVisible) { focus.clearFocus(); configurationVisible = false } else onBack() })
                Column(Modifier.weight(1f).padding(start = 13.dp)) {
                    Text(if (configurationVisible) "天气连接" else "天气", color = WeatherInk, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("${model.weatherCity.name} · ${snapshot?.let { weatherTime(it.observedAt, it.zone()) + " 实况" } ?: "城市天气"}", color = WeatherMuted, fontSize = 11.sp, maxLines = 1)
                }
                WeatherIconButton("和风天气连接设置", "settings", selected = configurationVisible, onClick = { focus.clearFocus(); configurationVisible = !configurationVisible })
                Spacer(Modifier.width(8.dp))
                if (!configurationVisible) WeatherIconButton(if (model.weatherRefreshing) "正在刷新" else "刷新天气", "refresh", enabled = !model.weatherRefreshing, loading = model.weatherRefreshing, onClick = model::refreshWeather)
                if (!wide && !configurationVisible) {
                    Spacer(Modifier.width(8.dp))
                    WeatherIconButton(if (citiesVisible) "查看天气预报" else "选择城市", if (citiesVisible) "sun" else "city", selected = citiesVisible, onClick = { focus.clearFocus(); citiesVisible = !citiesVisible })
                }
            }
            HorizontalDivider(color = WeatherLine)
            if (configurationVisible) Box(Modifier.weight(1f).fillMaxWidth()) { WeatherConnectionPage(model) { configurationVisible = false } }
            else if (wide) Row(Modifier.weight(1f).fillMaxWidth()) {
                WeatherForecast(model, Modifier.weight(1f).fillMaxHeight().padding(horizontal = 28.dp), onConfigure = { configurationVisible = true })
                VerticalDivider(color = WeatherLine)
                WeatherCityPanel(model, Modifier.width(282.dp).fillMaxHeight(), onSelect = { model.chooseWeatherCity(it); focus.clearFocus(); keyboard?.hide() })
            } else {
                if (citiesVisible) WeatherCityPanel(model, Modifier.weight(1f).fillMaxWidth(), onSelect = { model.chooseWeatherCity(it); focus.clearFocus(); keyboard?.hide(); citiesVisible = false })
                else WeatherForecast(model, Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp), onConfigure = { configurationVisible = true })
            }
        }
    }
}

@Composable
private fun WeatherForecast(model: LauncherModel, modifier: Modifier, onConfigure: () -> Unit) {
    val context = LocalContext.current
    val snapshot = (model.weatherState as? WeatherState.Ready)?.snapshot
    Column(modifier.verticalScroll(rememberScrollState()).padding(top = 22.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!model.weatherConfigured) Button(onClick = onConfigure, modifier = Modifier.fillMaxWidth()) { Text("连接和风天气，获取完整预报") }
        WeatherHero(model.weatherCity, snapshot, model.weatherState, model.weatherRefreshing, model::refreshWeather)
        if (model.weatherRefreshError != null && snapshot != null) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF2DC)) {
                Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(model.weatherRefreshError ?: "更新失败，显示缓存天气", Modifier.weight(1f), color = Color(0xFF876B42), fontSize = 12.sp)
                    TextButton(onClick = model::refreshWeather, enabled = !model.weatherRefreshing) { Text("重试", fontSize = 12.sp) }
                }
            }
        }
        if (snapshot?.notice != null) Text(snapshot.notice, color = WeatherMuted, fontSize = 11.sp)
        WeatherPanel("逐小时预报", "温度 / 降水概率") {
            if (snapshot?.hours.isNullOrEmpty()) WeatherEmptyLine(if (model.weatherRefreshing) "正在获取逐小时预报…" else "暂无逐小时数据")
            else LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 2.dp)) {
                items(snapshot!!.hours, key = { it.time }) { hour ->
                    Column(Modifier.width(54.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(weatherTime(hour.time, snapshot.zone(), "HH时"), color = WeatherMuted, fontSize = 11.sp)
                        WeatherArtwork(hour.code, hour.isDay, Modifier.size(32.dp), animated = false)
                        Text(hour.temperature.degrees(), color = WeatherInk, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Text(hour.rainChance?.let { "$it%" } ?: "—", color = WeatherAccent, fontSize = 10.sp)
                    }
                }
            }
        }
        WeatherPanel("未来五天", "低温 — 高温") {
            if (snapshot?.days.isNullOrEmpty()) WeatherEmptyLine(if (model.weatherRefreshing) "正在获取多日预报…" else "暂无多日预报")
            else {
                val days = snapshot!!.days
                val min = days.minOf { it.low }.toFloat()
                val range = (days.maxOf { it.high }.toFloat() - min).coerceAtLeast(1f)
                days.forEachIndexed { index, day ->
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(if (index == 0) "今天" else weatherTime(day.time, snapshot.zone(), "EEE"), Modifier.width(40.dp), color = WeatherInk, fontSize = 12.sp)
                        WeatherArtwork(day.code, true, Modifier.size(29.dp), animated = false)
                        Text(day.rainChance?.let { "$it%" } ?: "—", Modifier.width(32.dp), color = WeatherAccent, fontSize = 10.sp)
                        Text(day.low.degrees(), Modifier.width(28.dp), color = WeatherMuted, fontSize = 13.sp)
                        Canvas(Modifier.weight(1f).height(6.dp)) {
                            drawRoundRect(Color(0xFFF0F3F4), cornerRadius = CornerRadius(size.height))
                            val start = (day.low.toFloat() - min) / range * size.width
                            val end = (day.high.toFloat() - min) / range * size.width
                            drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF8CC6D5), Color(0xFFE8C686))), Offset(start, 0f), Size((end - start).coerceAtLeast(size.height), size.height), CornerRadius(size.height))
                        }
                        Text(day.high.degrees(), Modifier.width(30.dp), color = WeatherInk, fontSize = 13.sp)
                    }
                }
            }
        }
        Text("天气详情", color = WeatherInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeatherMetric("humidity", "相对湿度", snapshot?.humidity?.let { "$it%" } ?: "—", "当前空气湿度", Modifier.weight(1f))
            WeatherMetric("wind", "风速", snapshot?.windSpeed?.let { "${it.roundToInt()} km/h" } ?: "—", snapshot?.windDirection?.let(::windName) ?: "风向暂无数据", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeatherMetric("sun", "今日最高紫外线", snapshot?.uv?.let { String.format(java.util.Locale.ROOT, "%.1f", it) } ?: "—", snapshot?.uv?.let { if (it >= 6) "较强，外出注意防晒" else if (it >= 3) "中等，适当防护" else "较弱" } ?: "暂无紫外线数据", Modifier.weight(1f))
            WeatherMetric("sunrise", "日出 / 日落", snapshot?.let { weatherTime(it.sunrise, it.zone()) + " / " + weatherTime(it.sunset, it.zone()) } ?: "—", "所选城市当地时间", Modifier.weight(1f))
        }
        Text("天气数据：和风天气 · QWeather · 预报仅供参考\n${snapshot?.let { "数据获取于 " + weatherTime(it.fetchedAt, it.zone(), "M月d日 HH:mm") + " · 城市当地时间" } ?: "配置连接后更新天气，不申请设备定位权限"}", color = WeatherMuted, fontSize = 10.sp, lineHeight = 17.sp,
            modifier = Modifier.clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, "https://developer.qweather.com/attribution.html".toUri())) } })
        snapshot?.sourceLinks?.filter { it != "https://developer.qweather.com/attribution.html" }?.forEach { link ->
            if (link.startsWith("https://")) Text("来源说明：$link", color = WeatherAccent, fontSize = 10.sp,
                modifier = Modifier.clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, link.toUri())) } })
        }
    }
}

@Composable
private fun WeatherHero(city: WeatherCity, snapshot: WeatherSnapshot?, state: WeatherState, refreshing: Boolean, onRetry: () -> Unit) {
    val context = LocalContext.current
    val night = snapshot?.isDay == false
    val foreground = if (night) Color(0xFFF2F7FA) else WeatherInk
    val secondary = if (night) Color(0xFFB7CCD9) else Color(0xFF617F91)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(if (night) listOf(Color(0xFF314D68), Color(0xFF20374F)) else listOf(Color(0xFFD9ECF3), Color(0xFFECF4ED))))) {
        WeatherArtwork(snapshot?.code ?: -1, snapshot?.isDay ?: true, Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 10.dp).size(156.dp))
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                WeatherSymbol("location", Modifier.size(13.dp), secondary)
                Text(city.name, color = foreground, fontSize = 19.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(.6f))
            }
            Text(snapshot?.temperature?.degrees() ?: "—°", color = foreground, fontSize = 56.sp, fontWeight = FontWeight.Light, letterSpacing = (-3).sp, modifier = Modifier.padding(top = 4.dp))
            Text(snapshot?.let { "${it.description}   体感 ${it.feelsLike?.degrees() ?: "—"}" } ?: if (refreshing) "正在获取最新天气…" else "天气暂不可用", color = foreground, fontSize = 14.sp)
            Text(snapshot?.let { "最高 ${it.high.degrees()}   最低 ${it.low.degrees()}" } ?: (state as? WeatherState.Error)?.message ?: "选择城市，查看此刻天气", color = secondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = foreground.copy(alpha = .1f))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(if (refreshing) Color(0xFFD6B574) else if (snapshot == null) Color(0xFFB8C4CB) else Color(0xFF79A48F), CircleShape))
                Text(if (refreshing) "正在更新，稍候即可查看" else if (snapshot == null) "连接网络后可获取完整预报" else if (System.currentTimeMillis() - snapshot.fetchedAt > 3_600_000) "缓存数据 · 可刷新获取最新天气" else "${weatherTime(snapshot.observedAt, snapshot.zone())} 实况 · 前台每 30 分钟更新", color = secondary, fontSize = 10.sp, modifier = Modifier.weight(1f).padding(start = 7.dp), maxLines = 2)
                if (snapshot != null) Text("和风天气", color = secondary, fontSize = 10.sp, modifier = Modifier.padding(start = 7.dp).clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, "https://developer.qweather.com/attribution.html".toUri())) } })
                if (snapshot == null && !refreshing) TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("重试", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun WeatherPanel(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, WeatherLine)) {
        Column(Modifier.fillMaxWidth().padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = WeatherInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(subtitle, color = WeatherMuted, fontSize = 10.sp)
            }
            content()
        }
    }
}

@Composable
private fun WeatherMetric(icon: String, title: String, value: String, detail: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = Color.White, border = BorderStroke(1.dp, WeatherLine)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                WeatherSymbol(icon, Modifier.size(16.dp), WeatherAccent)
                Text(title, color = WeatherMuted, fontSize = 11.sp)
            }
            Text(value, color = WeatherInk, fontSize = 21.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(detail, color = WeatherMuted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun WeatherEmptyLine(message: String) {
    Text(message, color = WeatherMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 20.dp))
}

private fun windName(degrees: Int): String = listOf("北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风")[((degrees + 22) % 360) / 45]

@Composable
private fun WeatherCityPanel(model: LauncherModel, modifier: Modifier, onSelect: (WeatherCity) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var remote by remember { mutableStateOf<List<WeatherCity>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val term = query.trim()
    val local = remember(term, model.recentWeatherCities) { (listOf(model.weatherCity) + model.recentWeatherCities + WeatherRepository.cities).distinctBy { it.id }.filter { term.isNotEmpty() && (it.name.contains(term, true) || it.id.contains(term, true) || it.region.contains(term, true)) } }
    LaunchedEffect(term, retry, model.weatherConnectionVersion) {
        remote = emptyList(); searchError = false; searching = false
        if (term.length < 2 || !model.weatherConfigured) return@LaunchedEffect
        searching = true
        delay(450)
        try { remote = model.searchWeatherCities(term) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { searchError = true }
        finally { searching = false }
    }
    Column(modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
        Text("城市管理", color = WeatherInk, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text("选过的城市可在桌面上下滑动切换", color = WeatherMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp, bottom = 19.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it.take(60) }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            placeholder = { Text("搜索城市 / 拼音", fontSize = 12.sp) },
            leadingIcon = { WeatherSymbol("search", Modifier.size(18.dp), WeatherMuted) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { WeatherSymbol("close", Modifier.size(15.dp).semantics { contentDescription = "清空搜索" }, WeatherMuted) } },
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = WeatherLine, focusedBorderColor = WeatherAccent, unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { retry++ })
        )
        Spacer(Modifier.height(18.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            if (term.isEmpty()) {
                item { WeatherCityCaption("桌面天气") }
                item { WeatherCityRow(model.weatherCity, true, onClick = { onSelect(model.weatherCity) }) }
                val recent = model.recentWeatherCities.filter { it.id != model.weatherCity.id }
                if (recent.isNotEmpty()) {
                    item { WeatherCityCaption("最近选择") }
                    items(recent, key = { "recent_${it.id}" }) { city -> WeatherCityRow(city, false) { onSelect(city) } }
                }
                item { WeatherCityCaption("热门城市") }
                items(WeatherRepository.cities.filter { it.id != model.weatherCity.id && recent.none { recentCity -> recentCity.id == it.id } }, key = { it.id }) { city -> WeatherCityRow(city, false) { onSelect(city) } }
            } else {
                val results = (local + remote).distinctBy { "${it.name}_${(it.latitude * 10).roundToInt()}_${(it.longitude * 10).roundToInt()}" }
                item { WeatherCityCaption(if (searching) "正在查找城市…" else "搜索结果 · ${results.size}") }
                items(results, key = { it.id }) { city -> WeatherCityRow(city, city.id == model.weatherCity.id) { onSelect(city) } }
                if (searching) item { Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp, color = WeatherAccent) } }
                if (searchError) item {
                    Column(Modifier.padding(vertical = 12.dp)) {
                        Text("在线搜索暂不可用，仍可选择本地城市。", color = WeatherMuted, fontSize = 12.sp)
                        TextButton(onClick = { retry++ }) { Text("重新搜索", fontSize = 12.sp) }
                    }
                } else if (!searching && results.isEmpty()) item { WeatherEmptyLine(if (term.length < 2) "输入至少两个字符搜索更多城市" else "没有找到该城市，试试完整名称或拼音") }
            }
        }
        HorizontalDivider(color = WeatherLine)
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            WeatherSymbol("location", Modifier.size(13.dp), WeatherMuted)
            Text("手动选择城市，无需定位权限", color = WeatherMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun WeatherCityCaption(text: String) {
    Text(text, color = WeatherMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp, bottom = 5.dp))
}

@Composable
private fun WeatherCityRow(city: WeatherCity, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp), color = if (selected) Color(0xFFE2EDF2) else Color.White,
        border = BorderStroke(1.dp, if (selected) Color(0xFFC3D9E3) else WeatherLine)) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(city.name, color = WeatherInk, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (city.region.isNotBlank()) Text(city.region, color = WeatherMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
            }
            if (selected) WeatherSymbol("check", Modifier.size(16.dp).semantics { contentDescription = "当前城市" }, WeatherAccent)
            else WeatherSymbol("forward", Modifier.size(13.dp), WeatherMuted)
        }
    }
}

@Composable
private fun WeatherIconButton(label: String, symbol: String, enabled: Boolean = true, selected: Boolean = false, loading: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, shape = CircleShape, color = if (selected) Color(0xFFDFECF2) else Color.White, border = BorderStroke(1.dp, WeatherLine), modifier = Modifier.semantics { contentDescription = label }) {
        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 1.8.dp, color = WeatherAccent)
            else WeatherSymbol(symbol, Modifier.size(19.dp), WeatherAccent)
        }
    }
}

@Composable
private fun WeatherSymbol(name: String, modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val u = size.minDimension
        val stroke = Stroke(u * .075f, cap = StrokeCap.Round)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, Offset(size.width * x1, size.height * y1), Offset(size.width * x2, size.height * y2), stroke.width, StrokeCap.Round)
        when (name) {
            "settings" -> { repeat(3) { i -> val y = .25f + i * .25f; line(.15f, y, .85f, y); drawCircle(Color.White, u * .1f, Offset(u * (if (i % 2 == 0) .36f else .65f), u * y)); drawCircle(color, u * .1f, Offset(u * (if (i % 2 == 0) .36f else .65f), u * y), style = stroke) } }
            "back", "forward" -> { val a = if (name == "back") .62f else .38f; val b = 1f - a; line(a, .2f, b, .5f); line(b, .5f, a, .8f) }
            "close" -> { line(.25f, .25f, .75f, .75f); line(.25f, .75f, .75f, .25f) }
            "check" -> { line(.17f, .5f, .4f, .72f); line(.4f, .72f, .83f, .28f) }
            "search" -> { drawCircle(color, u * .27f, Offset(u * .43f, u * .43f), style = stroke); line(.64f, .64f, .85f, .85f) }
            "refresh" -> { drawArc(color, 35f, 285f, false, Offset(u * .18f, u * .18f), Size(u * .64f, u * .64f), style = stroke); line(.78f, .17f, .78f, .4f); line(.55f, .4f, .78f, .4f) }
            "location" -> {
                val path = Path().apply { moveTo(u * .5f, u * .91f); cubicTo(u * .04f, u * .49f, u * .19f, u * .12f, u * .5f, u * .12f); cubicTo(u * .81f, u * .12f, u * .96f, u * .49f, u * .5f, u * .91f) }
                drawPath(path, color, style = stroke); drawCircle(color, u * .1f, Offset(u * .5f, u * .4f), style = stroke)
            }
            "humidity" -> {
                val path = Path().apply { moveTo(u * .5f, u * .1f); cubicTo(u * .35f, u * .36f, u * .14f, u * .53f, u * .24f, u * .73f); cubicTo(u * .36f, u, u * .86f, u * .88f, u * .77f, u * .57f); cubicTo(u * .73f, u * .42f, u * .59f, u * .25f, u * .5f, u * .1f) }
                drawPath(path, color, style = stroke)
            }
            "wind" -> { line(.13f, .35f, .7f, .35f); line(.2f, .55f, .87f, .55f); line(.13f, .75f, .6f, .75f); drawArc(color, 180f, 220f, false, Offset(u * .57f, u * .12f), Size(u * .23f, u * .23f), style = stroke) }
            "city" -> { drawRoundRect(color, Offset(u * .15f, u * .32f), Size(u * .28f, u * .54f), CornerRadius(u * .04f), style = stroke); drawRoundRect(color, Offset(u * .5f, u * .12f), Size(u * .34f, u * .74f), CornerRadius(u * .04f), style = stroke); line(.64f, .3f, .71f, .3f); line(.64f, .48f, .71f, .48f) }
            else -> { drawCircle(color, u * .2f, Offset(u * .5f, u * .5f), style = stroke); repeat(8) { i -> val a = i * PI / 4; drawLine(color, Offset(u * (.5f + cos(a).toFloat() * .32f), u * (.5f + sin(a).toFloat() * .32f)), Offset(u * (.5f + cos(a).toFloat() * .43f), u * (.5f + sin(a).toFloat() * .43f)), stroke.width, StrokeCap.Round) }; if (name == "sunrise") line(.1f, .83f, .9f, .83f) }
        }
    }
}
