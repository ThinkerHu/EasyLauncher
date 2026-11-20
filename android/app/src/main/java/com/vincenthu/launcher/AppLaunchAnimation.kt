package com.vincenthu.launcher

import android.app.ActivityOptions
import android.content.Context
import android.graphics.Rect as ScreenRect
import android.os.Bundle
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalView
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** The icon's bounds are in its own window, including when it lives in a dialog. */
internal class AppLaunchSource(private val view: View, bounds: Rect, val anchor: AppLaunchAnchor? = null) {
    private val windowLocation = IntArray(2).also(view::getLocationInWindow)
    private val x = bounds.left.roundToInt() - windowLocation[0]
    private val y = bounds.top.roundToInt() - windowLocation[1]
    private val width = bounds.width.roundToInt().coerceAtLeast(1)
    private val height = bounds.height.roundToInt().coerceAtLeast(1)

    fun screenBounds(): ScreenRect {
        val location = IntArray(2).also(view::getLocationOnScreen)
        val left = location[0] + x
        val top = location[1] + y
        return ScreenRect(left, top, left + width, top + height)
    }

    fun scaleUpOptions(): Bundle? = if (view.isAttachedToWindow) {
        ActivityOptions.makeScaleUpAnimation(view, x, y, width, height).toBundle()
    } else null
}

internal class AppLaunchAnchor(private val view: View) {
    var coordinates: LayoutCoordinates? = null
    var landingSequence by mutableIntStateOf(0)
    val landingScale = Animatable(1f)

    fun source(): AppLaunchSource? {
        val current = coordinates?.takeIf { it.isAttached } ?: return null
        val bounds = current.boundsInWindow()
        return if (bounds.width > 0f && bounds.height > 0f) AppLaunchSource(view, bounds, this) else null
    }
}

@Composable
internal fun rememberAppLaunchAnchor(returnAppIds: List<String> = emptyList(), priority: Int = 0): AppLaunchAnchor {
    val view = LocalView.current
    val anchor = remember(view) { AppLaunchAnchor(view) }
    LaunchedEffect(anchor, anchor.landingSequence) {
        anchor.landingScale.snapTo(1f)
        if (anchor.landingSequence != 0) {
            // Meet the real outgoing window during its final fade, then settle.
            // A new return cancels the previous pulse instead of queuing one.
            delay(340)
            anchor.landingScale.animateTo(1.10f, tween(90))
            anchor.landingScale.animateTo(1f, spring(dampingRatio = .8f, stiffness = 500f))
        }
    }
    val controller = (view.context.findActivity() as? MainActivity)?.homeReturnAnimation
    DisposableEffect(anchor, controller, returnAppIds, priority) {
        if (returnAppIds.isNotEmpty()) controller?.register(anchor, returnAppIds, priority)
        onDispose { controller?.unregister(anchor) }
    }
    return anchor
}

internal fun Modifier.homeReturnLanding(anchor: AppLaunchAnchor): Modifier = graphicsLayer {
    scaleX = anchor.landingScale.value
    scaleY = anchor.landingScale.value
}

internal fun appLaunchOptions(context: Context, source: AppLaunchSource?, motion: Boolean): Bundle? {
    if (!motion) return ActivityOptions.makeCustomAnimation(context, 0, 0).toBundle()
    return source?.scaleUpOptions()
        ?: ActivityOptions.makeCustomAnimation(context, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
}
