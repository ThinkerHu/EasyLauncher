package com.vincenthu.launcher

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.core.view.WindowInsetsControllerCompat

internal data class AppGroupPresentation(
    val id: String,
    val title: String,
    val apps: List<LauncherApp>,
    val backdrop: Bitmap?,
    val origin: Offset? = null
)

/** One full-screen folder surface for desktop folders and library categories. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun ApplicationGroupOverlay(
    group: AppGroupPresentation?,
    onClose: () -> Unit,
    onRename: ((String) -> Unit)? = null,
    startEditing: Boolean = false,
    appTile: @Composable (LauncherApp, Dp, (AppLaunchSource?) -> Unit) -> Unit,
    onLaunch: (LauncherApp, AppLaunchSource?) -> Unit
) {
    val motion = LocalLauncherMotionEnabled.current
    val visibility = remember { MutableTransitionState(false) }
    var retained by remember { mutableStateOf<AppGroupPresentation?>(null) }
    LaunchedEffect(group) {
        if (group != null) retained = group
        visibility.targetState = group != null
    }
    val shown = group ?: retained
    if (shown == null || (group == null && !visibility.currentState && visibility.isIdle)) return
    val window = LocalView.current.context.findActivity()?.window
    DisposableEffect(window) {
        val bars = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        val status = bars?.isAppearanceLightStatusBars
        val navigation = bars?.isAppearanceLightNavigationBars
        bars?.isAppearanceLightStatusBars = false
        bars?.isAppearanceLightNavigationBars = false
        onDispose {
            if (status != null) bars?.isAppearanceLightStatusBars = status
            if (navigation != null) bars?.isAppearanceLightNavigationBars = navigation
        }
    }
    var renaming by rememberSaveable(shown.id) { mutableStateOf(false) }
    var draft by rememberSaveable(shown.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(shown.title))
    }
    val focusRequester = remember(shown.id) { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun beginRename() {
        draft = TextFieldValue(shown.title, TextRange(0, shown.title.length))
        renaming = true
    }
    fun finishRename() {
        val name = draft.text.trim()
        if (renaming && name.isNotEmpty() && name != shown.title) onRename?.invoke(name)
        renaming = false
        keyboard?.hide()
        focusManager.clearFocus(force = true)
    }
    LaunchedEffect(group != null, shown.id, startEditing) {
        if (group != null && startEditing && onRename != null) beginRename()
        if (group == null) { renaming = false; keyboard?.hide(); focusManager.clearFocus(force = true) }
    }
    LaunchedEffect(renaming) {
        if (renaming) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    val dismissOrFinish by rememberUpdatedState(newValue = {
        if (renaming) finishRename() else onClose()
    })
    BackHandler(enabled = group != null) { dismissOrFinish() }
    val transition = rememberTransition(visibility, label = "group-lifetime")
    val progress by transition.animateFloat(
        transitionSpec = { tween(if (motion) if (targetState) 360 else 240 else 0, easing = LauncherMotion.easeOut) },
        label = "group-focus"
    ) { if (it) 1f else 0f }
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .semantics { paneTitle = shown.title }
            .pointerInput(Unit) { detectTapGestures { dismissOrFinish() } }
            .then(if (group == null) Modifier.clearAndSetSemantics { } else Modifier)
    ) {
        Box(Modifier.matchParentSize().graphicsLayer { alpha = progress }) {
            // This texture was captured and blurred once before opening. API 26–30
            // use the same bounded CPU path as the launcher's icon menus.
            val backdrop = shown.backdrop
            if (backdrop != null) {
                val bitmap = backdrop
                Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize().graphicsLayer {
                    scaleX = 1.035f; scaleY = 1.035f
                }, contentScale = ContentScale.Crop)
            } else Box(Modifier.fillMaxSize().background(Color(0xFF314C65)))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color(0xB010223F), Color(0x82445B79), Color(0xB01E3C59))
            )))
        }
        val landscape = maxWidth > maxHeight
        val density = LocalDensity.current
        val origin = shown.origin?.let {
            TransformOrigin((it.x / with(density) { maxWidth.toPx() }).coerceIn(0f, 1f),
                (it.y / with(density) { maxHeight.toPx() }).coerceIn(0f, 1f))
        } ?: TransformOrigin(.5f, .38f)
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val side = if (maxWidth < 420.dp) 24.dp else 44.dp
            val contentWidth = minOf(maxWidth - side * 2, if (landscape) 820.dp else 600.dp)
            val top = (maxHeight * if (landscape) .12f else .19f).coerceAtMost(if (landscape) 70.dp else 180.dp)
            val titleHeight = 60.dp
            val titleGap = if (landscape) 18.dp else 32.dp
            val bottom = maxHeight * if (landscape) .12f else .18f
            val gridHeight = (maxHeight - top - titleHeight - titleGap - bottom - 30.dp).coerceAtLeast(100.dp)
            val layout = AppGroupLayout.calculate(contentWidth.value, gridHeight.value, landscape, density.fontScale)
            val pageCount = layout.pageCount(shown.apps.size)
            key(shown.id) {
                val pager = rememberPagerState(pageCount = { pageCount })
                val scope = rememberCoroutineScope()
                LaunchedEffect(pageCount) {
                    if (pager.currentPage >= pageCount) pager.scrollToPage(pageCount - 1)
                }
                Column(
                    Modifier.width(contentWidth).fillMaxHeight().align(Alignment.TopCenter)
                        .padding(top = top, bottom = bottom)
                        .graphicsLayer {
                            alpha = progress
                            transformOrigin = origin
                            scaleX = if (motion) .91f + .09f * progress else 1f
                            scaleY = scaleX
                            translationY = if (motion) 14.dp.toPx() * (1f - progress) else 0f
                        }
                ) {
                    Row(Modifier.fillMaxWidth().height(titleHeight), verticalAlignment = Alignment.CenterVertically) {
                        val titleSize = if (landscape) 32.sp else 36.sp
                        if (renaming) {
                            BasicTextField(
                                value = draft,
                                onValueChange = { value ->
                                    val text = value.text.take(16)
                                    draft = if (text == value.text) value else value.copy(text = text,
                                        selection = TextRange(value.selection.start.coerceAtMost(text.length), value.selection.end.coerceAtMost(text.length)),
                                        composition = null)
                                },
                                modifier = Modifier.weight(1f).focusRequester(focusRequester)
                                    .semantics { contentDescription = "分组名称" },
                                textStyle = TextStyle(color = Color.White, fontSize = titleSize, fontWeight = FontWeight.Light),
                                cursorBrush = SolidColor(Color.White), singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { finishRename() }),
                                decorationBox = { field ->
                                    Column {
                                        Box { field() }
                                        Spacer(Modifier.height(3.dp))
                                        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = .55f)))
                                    }
                                }
                            )
                        } else {
                            Text(shown.title, Modifier.weight(1f)
                                .then(if (onRename != null) Modifier.combinedClickable(onClick = { beginRename() }, onLongClick = { beginRename() }) else Modifier),
                                color = Color.White, fontSize = titleSize,
                                fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (onRename != null) IconButton(onClick = { if (renaming) finishRename() else beginRename() },
                            modifier = Modifier.semantics { contentDescription = if (renaming) "保存分组名称" else "重命名分组" }) {
                            if (renaming) Canvas(Modifier.size(18.dp)) {
                                val stroke = 1.8.dp.toPx()
                                drawLine(Color.White, Offset(size.width * .1f, size.height * .5f), Offset(size.width * .38f, size.height * .78f), stroke, StrokeCap.Round)
                                drawLine(Color.White, Offset(size.width * .38f, size.height * .78f), Offset(size.width * .9f, size.height * .22f), stroke, StrokeCap.Round)
                            } else Text("⋯", color = Color.White.copy(alpha = .8f), fontSize = 24.sp)
                        }
                    }
                    Spacer(Modifier.height(titleGap))
                    HorizontalPager(
                        state = pager, modifier = Modifier.fillMaxWidth().weight(1f),
                        pageSpacing = 24.dp,
                        flingBehavior = PagerDefaults.flingBehavior(pager, snapAnimationSpec = LauncherMotion.page(motion)),
                        key = { it }
                    ) { page ->
                        val start = layout.pageStart(page)
                        val iconSize = minOf(if (landscape) 64.dp else 72.dp,
                            (contentWidth / layout.columns - 28.dp).coerceAtLeast(36.dp),
                            (gridHeight / layout.rows - 46.dp).coerceAtLeast(36.dp))
                        Column(Modifier.fillMaxSize()) {
                            repeat(layout.rows) { row ->
                                Row(Modifier.fillMaxWidth().height(minOf(if (landscape) 126.dp else 148.dp, gridHeight / layout.rows))) {
                                    repeat(layout.columns) { col ->
                                        val index = row * layout.columns + col
                                        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                                            shown.apps.getOrNull(start + index)?.let { app ->
                                                val reveal by transition.animateFloat(
                                                    transitionSpec = {
                                                        tween(if (motion) if (targetState) 320 else 170 else 0,
                                                            delayMillis = if (motion && targetState) (index * 18).coerceAtMost(126) else 0,
                                                            easing = LauncherMotion.easeOut)
                                                    }, label = "group-icon-$index"
                                                ) { if (it) 1f else 0f }
                                                Box(Modifier.graphicsLayer {
                                                    alpha = reveal
                                                    scaleX = if (motion) .82f + .18f * reveal else 1f
                                                    scaleY = scaleX
                                                    translationY = if (motion) 12.dp.toPx() * (1f - reveal) else 0f
                                                }) {
                                                    appTile(app, iconSize) { source ->
                                                        if (renaming) finishRename()
                                                        onLaunch(app, source)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (shown.apps.isEmpty()) Text("分组中暂无应用", color = Color.White.copy(alpha = .75f), fontSize = 14.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(30.dp)
                        .semantics { contentDescription = "分组第 ${pager.settledPage + 1} 页，共 $pageCount 页" },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        repeat(pageCount) { page ->
                            Box(Modifier.size(28.dp).clickable(onClickLabel = "转到第 ${page + 1} 页") {
                                scope.launch { pager.animateScrollToPage(page, animationSpec = LauncherMotion.page(motion)) }
                            }, contentAlignment = Alignment.Center) {
                                Box(Modifier.size(if (page == pager.settledPage) 6.dp else 5.dp)
                                    .background(Color.White.copy(alpha = if (page == pager.settledPage) .95f else .36f), CircleShape))
                            }
                        }
                    }
                }
            }
            IconButton(onClick = { if (renaming) finishRename(); onClose() }, modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 12.dp)
                .graphicsLayer { alpha = progress }
                .semantics { contentDescription = "关闭分组" }) {
                Canvas(Modifier.size(15.dp)) {
                    drawLine(Color.White.copy(alpha = .75f), Offset.Zero, Offset(size.width, size.height), 1.6.dp.toPx(), StrokeCap.Round)
                    drawLine(Color.White.copy(alpha = .75f), Offset(size.width, 0f), Offset(0f, size.height), 1.6.dp.toPx(), StrokeCap.Round)
                }
            }
        }
        GroupBackEdges(onBack = { dismissOrFinish() })
    }
}
