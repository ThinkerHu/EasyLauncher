package com.vincenthu.launcher

import android.graphics.Bitmap
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal data class IconMenuAction(
    val label: String,
    val onClick: () -> Unit = {},
    val destructive: Boolean = false,
    val children: List<IconMenuAction> = emptyList()
)
internal data class IconMenuContent(val title: String, val actions: List<IconMenuAction>)
internal val LocalIconMenus = staticCompositionLocalOf<(String) -> IconMenuContent> {
    { IconMenuContent("", emptyList()) }
}
internal val LocalShortcutLaunched = staticCompositionLocalOf<(String) -> Unit> { {} }
internal val LocalIconMenuHomeRequest = staticCompositionLocalOf { 0 }
private val menuIconSize = 28.dp
private val menuLabelFontSize = 14.sp

/** Full-window focus layer; anchors remain in the same coordinates in home and dialogs. */
@Composable
internal fun IconMenuAnchor(
    id: String,
    preview: @Composable () -> Unit,
    launchAnchor: AppLaunchAnchor? = null,
    content: @Composable (openMenu: () -> Unit, iconModifier: Modifier) -> Unit
) {
    var expanded by remember(id) { mutableStateOf(false) }
    var opening by remember(id) { mutableStateOf(false) }
    var backdrop by remember(id) { mutableStateOf<Bitmap?>(null) }
    var anchor by remember(id) { mutableStateOf(Rect.Zero) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val configuration = LocalConfiguration.current
    val homeRequest = LocalIconMenuHomeRequest.current
    val currentView by rememberUpdatedState(view)
    val hostWindow = (view.parent as? DialogWindowProvider)?.window ?: view.context.findActivity()?.window
    val hostWidth = hostWindow?.decorView?.width ?: view.width
    val hostHeight = hostWindow?.decorView?.height ?: view.height
    val density = LocalDensity.current
    val shortcutService = remember(LocalContext.current) { LauncherShortcuts(view.context) }
    val menuFor = LocalIconMenus.current
    val launched = LocalShortcutLaunched.current
    val visibility = rememberEntranceState(visible = expanded)
    val motion = LocalLauncherMotionEnabled.current
    fun launchSource(): AppLaunchSource? =
        launchAnchor?.source()
            ?: if (view.isAttachedToWindow && anchor.width > 0f && anchor.height > 0f) AppLaunchSource(view, anchor) else null

    // HOME, app launches, rotation and backgrounding must never leave a stale focus layer.
    LaunchedEffect(homeRequest, configuration.screenWidthDp, configuration.screenHeightDp) { expanded = false }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) expanded = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    content({
        if (!expanded && !opening && anchor.width > 0f) {
            opening = true
            scope.launch {
                try {
                    val window: Window? = (currentView.parent as? DialogWindowProvider)?.window
                        ?: currentView.context.findActivity()?.window
                    backdrop = window?.let { captureIconMenuBackdrop(it) }
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) expanded = true
                } finally { opening = false }
            }
        }
    }, Modifier.onGloballyPositioned { anchor = it.boundsInWindow() })

    if (expanded || visibility.currentState || !visibility.isIdle) {
        var submenu by remember(id) { mutableStateOf<IconMenuContent?>(null) }
        var shortcutResult by remember(id) { mutableStateOf<AppShortcutResult?>(null) }
        val context = LocalContext.current
        LaunchedEffect(id) { shortcutResult = shortcutService.query(id) }
        val menu = submenu ?: menuFor(id)
        val transition = rememberTransition(visibility, label = "icon-menu-lifetime")
        val progress by transition.animateFloat(
            transitionSpec = { tween(if (motion) if (targetState) 190 else 130 else 0) },
            label = "icon-menu-opacity"
        ) { if (it) 1f else 0f }
        val pop by transition.animateFloat(
            transitionSpec = { if (motion) spring(dampingRatio = .78f, stiffness = 530f) else tween(0) },
            label = "icon-menu-pop"
        ) { if (it) 1f else 0f }
        Dialog(
            onDismissRequest = { expanded = false },
            // Explicitly size the window below. The default-width measurement path avoids
            // Compose 1.7's screenHeightDp cap, which excludes the status bar on EMUI.
            properties = DialogProperties(usePlatformDefaultWidth = true, decorFitsSystemWindows = false)
        ) {
            val window = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect {
                window?.let {
                    WindowCompat.setDecorFitsSystemWindows(it, false)
                    it.setLayout(hostWidth, hostHeight)
                    it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    it.setWindowAnimations(0)
                    @Suppress("DEPRECATION")
                    run {
                        it.statusBarColor = android.graphics.Color.TRANSPARENT
                        it.navigationBarColor = android.graphics.Color.TRANSPARENT
                    }
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        it.isStatusBarContrastEnforced = false
                        it.isNavigationBarContrastEnforced = false
                    }
                }
            }
            BoxWithConstraints(Modifier.requiredSize(
                with(density) { hostWidth.toDp() }, with(density) { hostHeight.toDp() }
            ).semantics { paneTitle = "应用快捷菜单" }) {
                val screenWidth = constraints.maxWidth
                val screenHeight = constraints.maxHeight
                val gap = with(density) { 10.dp.roundToPx() }
                val margin = with(density) { 16.dp.roundToPx() }
                val insets = WindowInsets.safeDrawing
                val menuWidth = minOf(200.dp, maxWidth - 24.dp)
                val menuWidthPx = with(density) { menuWidth.roundToPx() }
                val shortcuts = shortcutResult?.shortcuts.orEmpty()
                val appFooter = submenu == null && menu.actions.size == 2 &&
                    menu.actions.map { it.label }.toSet() == setOf("移除", "应用信息")
                val estimatedRows = if (appFooter) shortcuts.size else menu.actions.size + if (submenu != null) 1 else shortcuts.size
                val naturalHeight = with(density) {
                    (49.dp * estimatedRows + (if (appFooter) 2.dp else 10.dp) +
                        (if (shortcuts.isNotEmpty()) 9.dp else 0.dp) +
                        (if (appFooter) 70.dp else 0.dp)).roundToPx()
                }
                val placement = placeIconMenu(
                    screenWidth, screenHeight, anchor.left.roundToInt(), anchor.top.roundToInt(),
                    anchor.right.roundToInt(), anchor.bottom.roundToInt(), menuWidthPx, naturalHeight, gap, margin,
                    insets.getTop(density), insets.getBottom(density)
                )
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress }
                    .pointerInput(Unit) { detectTapGestures { expanded = false } }
                    .semantics { contentDescription = "关闭快捷菜单" }) {
                    backdrop?.let {
                        Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    }
                    Box(Modifier.fillMaxSize().background(Color(0xFF122638).copy(alpha = if (backdrop != null) .23f else .40f)))
                }
                Box(Modifier.offset { IntOffset(anchor.left.roundToInt(), anchor.top.roundToInt()) }
                    .size(with(density) { anchor.width.toDp() }, with(density) { anchor.height.toDp() })
                    .graphicsLayer {
                        val scale = 1f + .075f * pop
                        scaleX = scale; scaleY = scale
                        shadowElevation = 14.dp.toPx() * progress
                        shape = RoundedCornerShape(22.dp)
                    }.clickable { expanded = false }) { preview() }
                Surface(
                    modifier = Modifier.offset { IntOffset(placement.left, placement.top) }
                        .width(menuWidth).heightIn(max = with(density) { placement.maxHeight.toDp() })
                        .graphicsLayer {
                            alpha = progress
                            scaleX = .88f + .12f * pop; scaleY = .88f + .12f * pop
                            transformOrigin = TransformOrigin(
                                ((anchor.center.x - placement.left) / menuWidthPx).coerceIn(0f, 1f),
                                if (placement.below) 0f else 1f
                            )
                            translationY = (if (placement.below) -8.dp.toPx() else 8.dp.toPx()) * (1f - pop)
                        },
                    shape = RoundedCornerShape(20.dp), color = Color(0xFFF8FAFC).copy(alpha = .98f),
                    shadowElevation = 18.dp, tonalElevation = 0.dp
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = if (appFooter) 1.dp else 5.dp)) {
                        if (submenu != null) {
                            MenuRow("返回", symbol = "‹", onClick = { submenu = null })
                            HorizontalDivider(color = Color(0xFF29465A).copy(alpha = .10f))
                        } else {
                            shortcuts.forEachIndexed { index, shortcut ->
                                MenuRow(shortcut.label, shortcutIcon = shortcut.icon, symbol = "↗", onClick = {
                                    expanded = false
                                    if (shortcutService.start(shortcut, launchSource(), motion)) launched(id)
                                    else android.widget.Toast.makeText(context, "快捷功能暂不可用，请确认默认桌面和应用状态", android.widget.Toast.LENGTH_SHORT).show()
                                })
                                if (index < shortcuts.lastIndex) HorizontalDivider(
                                    Modifier.padding(start = 54.dp, end = 14.dp), color = Color(0xFF29465A).copy(alpha = .09f)
                                )
                            }
                            if (shortcuts.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(thickness = 1.dp, color = Color(0xFF29465A).copy(alpha = .17f))
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                        if (appFooter) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                menu.actions.forEach { action ->
                                    FooterAction(action, Modifier.weight(1f)) {
                                        expanded = false
                                        action.onClick()
                                    }
                                }
                            }
                        } else {
                            menu.actions.forEach { action ->
                                MenuRow(action.label, destructive = action.destructive,
                                    trailing = if (action.children.isNotEmpty()) "›" else null,
                                    symbol = if (action.label == "解散文件夹") "−" else "⋯", onClick = {
                                        if (action.children.isNotEmpty()) submenu = IconMenuContent(action.label, action.children)
                                        else { expanded = false; action.onClick() }
                                    })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterAction(action: IconMenuAction, modifier: Modifier, onClick: () -> Unit) {
    val color = Color(0xFF626A70)
    Column(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .padding(top = 6.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Image(
            painterResource(if (action.label == "应用信息") R.drawable.ic_menu_info else R.drawable.ic_menu_remove),
            contentDescription = null, modifier = Modifier.size(menuIconSize).padding(3.dp)
        )
        Text(action.label, color = color, fontSize = menuLabelFontSize, lineHeight = 20.sp,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MenuRow(
    label: String, destructive: Boolean = false, trailing: String? = null,
    shortcutIcon: Bitmap? = null, symbol: String = "⋯", onClick: () -> Unit
) {
    val color = if (destructive) Color(0xFFB4464F) else Color(0xFF29465A)
    Row(Modifier.fillMaxWidth().heightIn(min = 49.dp).clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(menuIconSize), contentAlignment = Alignment.Center) {
            if (shortcutIcon != null) Image(shortcutIcon.asImageBitmap(), null, Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
            else Text(symbol, color = color.copy(alpha = .8f), fontSize = 21.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), fontSize = menuLabelFontSize, color = color, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (trailing != null) Text(trailing, fontSize = 18.sp, color = Color(0xFF66818F))
    }
}
