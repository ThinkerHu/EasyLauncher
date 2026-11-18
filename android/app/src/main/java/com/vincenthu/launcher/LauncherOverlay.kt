package com.vincenthu.launcher

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Keep a single window and its content alive until the closing spring settles. */
@Composable
internal fun LauncherOverlay(
    overlay: String,
    onDismiss: () -> Unit,
    content: @Composable (String) -> Unit
) {
    val motion = LocalLauncherMotionEnabled.current
    val hostWindow = LocalView.current.context.findActivity()?.window
    // Full-screen pages share the activity's edge-to-edge window. This avoids
    // vendor dialog windows overriding icon contrast or exposing the wallpaper
    // below the gesture area, and lets IME insets resize the city results.
    if (overlay == "weather" || overlay == "settings") {
        DisposableEffect(hostWindow) {
            val bars = hostWindow?.let { WindowInsetsControllerCompat(it, it.decorView) }
            val oldStatus = bars?.isAppearanceLightStatusBars
            val oldNavigation = bars?.isAppearanceLightNavigationBars
            bars?.isAppearanceLightStatusBars = true
            bars?.isAppearanceLightNavigationBars = true
            onDispose {
                if (oldStatus != null) bars.isAppearanceLightStatusBars = oldStatus
                if (oldNavigation != null) bars.isAppearanceLightNavigationBars = oldNavigation
            }
        }
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFF5F7F7))
            .safeDrawingPadding().imePadding().pointerInput(Unit) { detectTapGestures { } }) {
            content(overlay)
        }
        return
    }
    val settingsPage = overlay == "settings"
    DisposableEffect(settingsPage, hostWindow) {
        if (settingsPage && hostWindow != null) {
            val oldStatusBarColor = hostWindow.statusBarColor
            val oldNavigationBarColor = hostWindow.navigationBarColor
            val bars = WindowInsetsControllerCompat(hostWindow, hostWindow.decorView)
            val oldLightStatusIcons = bars.isAppearanceLightStatusBars
            val oldLightNavigationIcons = bars.isAppearanceLightNavigationBars
            // The launcher draws its wallpaper edge to edge. While the full-screen
            // settings dialog is open, stop that wallpaper from showing through
            // the transparent gesture-navigation area underneath the dialog.
            WindowCompat.setDecorFitsSystemWindows(hostWindow, true)
            hostWindow.statusBarColor = android.graphics.Color.rgb(245, 247, 247)
            hostWindow.navigationBarColor = android.graphics.Color.rgb(245, 247, 247)
            bars.isAppearanceLightStatusBars = true
            bars.isAppearanceLightNavigationBars = true
            onDispose {
                WindowCompat.setDecorFitsSystemWindows(hostWindow, false)
                hostWindow.statusBarColor = oldStatusBarColor
                hostWindow.navigationBarColor = oldNavigationBarColor
                bars.isAppearanceLightStatusBars = oldLightStatusIcons
                bars.isAppearanceLightNavigationBars = oldLightNavigationIcons
            }
        } else onDispose { }
    }
    val visibility = remember { MutableTransitionState(false) }
    var displayedOverlay by remember { mutableStateOf(overlay) }
    LaunchedEffect(overlay) {
        if (overlay.isNotEmpty()) displayedOverlay = overlay
        visibility.targetState = overlay.isNotEmpty()
    }
    val transition = rememberTransition(visibility, label = "overlay-lifetime")
    val progress by transition.animateFloat(
        transitionSpec = { LauncherMotion.layer(motion) }, label = "overlay-progress"
    ) { if (it) 1f else 0f }

    if (overlay.isNotEmpty() || visibility.currentState || visibility.targetState || !visibility.isIdle) {
        val isSettingsPage = (overlay.ifEmpty { displayedOverlay }) == "settings"
        Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = isSettingsPage)) {
            val window = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect {
                // Drive the platform dim across system bars and app content together.
                // Keep the dialog's runtime inset mode in sync with DialogProperties.
                // A mismatch here relayouts the whole window one frame after opening
                // settings, which appears as a colored flash in the top bar.
                window?.let { WindowCompat.setDecorFitsSystemWindows(it, isSettingsPage) }
                window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window?.setDimAmount(if (isSettingsPage) 0f else .25f * progress.coerceIn(0f, 1f))
                window?.setWindowAnimations(0)
                val systemBarColor = if (isSettingsPage) android.graphics.Color.rgb(245, 247, 247) else android.graphics.Color.TRANSPARENT
                window?.statusBarColor = systemBarColor
                window?.navigationBarColor = systemBarColor
                if (window != null) {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = isSettingsPage
                        isAppearanceLightNavigationBars = isSettingsPage
                    }
                    if (isSettingsPage) {
                        window.decorView.systemUiVisibility =
                            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                                android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    }
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window?.isStatusBarContrastEnforced = false
                    window?.isNavigationBarContrastEnforced = false
                }
            }
            Box(
                Modifier.fillMaxSize()
                    .then(if (isSettingsPage) Modifier.background(androidx.compose.ui.graphics.Color(0xFFF5F7F7)) else Modifier)
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } },
                contentAlignment = Alignment.Center
            ) {
            Box(Modifier.fillMaxSize().then(if (isSettingsPage) Modifier.imePadding() else Modifier.safeDrawingPadding().imePadding())
                    .then(if (isSettingsPage) Modifier else Modifier.padding(18.dp)), contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = overlay.ifEmpty { displayedOverlay },
                        modifier = (if (isSettingsPage) Modifier.fillMaxSize() else Modifier.widthIn(max = 760.dp).fillMaxWidth()).graphicsLayer {
                            val p = progress.coerceIn(0f, 1.02f)
                            // A settings surface covers the whole screen and reaches
                            // the system bars. Fading the desktop through it produces
                            // a brief wallpaper-colored flash across its title area.
                            alpha = if (isSettingsPage) 1f else p.coerceAtMost(1f)
                            scaleX = if (motion && !isSettingsPage) .92f + .08f * p else 1f
                            scaleY = if (motion && !isSettingsPage) .92f + .08f * p else 1f
                            translationY = if (motion && !isSettingsPage) 32.dp.toPx() * (1f - p) else 0f
                        },
                        transitionSpec = {
                            val fullScreenSettings = targetState in setOf("settings", "weather") || initialState in setOf("settings", "weather")
                            val duration = if (motion && !fullScreenSettings) 240 else 0
                            (fadeIn(tween(duration, easing = LauncherMotion.easeOut)) +
                                slideInHorizontally(tween(duration, easing = LauncherMotion.easeOut)) { if (motion) it / 24 else 0 })
                                .togetherWith(fadeOut(tween(if (motion && !fullScreenSettings) 130 else 0)) +
                                    slideOutHorizontally(tween(duration, easing = LauncherMotion.easeOut)) { if (motion) -it / 32 else 0 })
                        },
                        contentAlignment = Alignment.Center,
                        label = "overlay-navigation"
                    ) { shown ->
                        // Only taps outside the card dismiss it. Children still own
                        // their buttons, scrolling and text-selection gestures.
                        Box(Modifier.pointerInput(Unit) { detectTapGestures { } }) { content(shown) }
                    }
                }
            }
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
