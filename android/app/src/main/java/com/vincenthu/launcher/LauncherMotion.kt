package com.vincenthu.launcher

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/** One motion policy for paging, overlays and accessibility. No frame timers. */
internal object LauncherMotion {
    val easeOut = CubicBezierEasing(.16f, 1f, .3f, 1f)
    val easeIn = CubicBezierEasing(.4f, 0f, 1f, 1f)

    fun <T> page(enabled: Boolean): FiniteAnimationSpec<T> =
        if (enabled) spring(dampingRatio = 1f, stiffness = 240f) else snap()

    fun <T> layer(enabled: Boolean): FiniteAnimationSpec<T> =
        if (enabled) spring(dampingRatio = .9f, stiffness = 380f) else snap()
}

internal val LocalLauncherMotionEnabled = staticCompositionLocalOf { true }

@Composable
internal fun LauncherMotionTheme(reduceMotion: Boolean, content: @Composable () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    fun systemEnabled() = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    var systemMotion by remember(resolver) { mutableStateOf(systemEnabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { systemMotion = systemEnabled() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    CompositionLocalProvider(LocalLauncherMotionEnabled provides (systemMotion && !reduceMotion), content = content)
}

/** Boolean `visible = true` starts at the end state and never runs an entrance. */
@Composable
internal fun rememberEntranceState(animate: Boolean = true, visible: Boolean = true): MutableTransitionState<Boolean> {
    val enabled = LocalLauncherMotionEnabled.current && animate
    return remember { MutableTransitionState(!enabled && visible) }.also { it.targetState = visible }
}
