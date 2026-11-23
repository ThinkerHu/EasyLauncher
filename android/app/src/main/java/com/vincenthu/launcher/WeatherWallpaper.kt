package com.vincenthu.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Two bounded drawing layers; animation state is read only during drawing. */
@Composable
internal fun WeatherWallpaper(
    colors: List<Color>,
    scene: WeatherWallpaperScene?,
    visible: Boolean,
    pagePosition: () -> Float
) {
    val motion = LocalLauncherMotionEnabled.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val powerSave = rememberWallpaperPowerSave()
    val running = motion && visible && !powerSave && lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    val phase: State<Float> = if (running && scene != null) {
        val transition = rememberInfiniteTransition(label = "wallpaper-atmosphere")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "atmosphere-cycle")
    } else remember { mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxSize().graphicsLayer {
        scaleX = if (motion) 1.045f else 1f
        scaleY = if (motion) 1.045f else 1f
        translationX = if (motion) -pagePosition() * 5.dp.toPx() else 0f
    }) {
        Crossfade(targetState = scene, animationSpec = tween(if (running) 1100 else 0), label = "weather-wallpaper") { current ->
            MountainScene(colors, current, phase)
        }
    }
}

@Composable
internal fun rememberWallpaperPowerSave(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(PowerManager::class.java) }
    var powerSave by remember(manager) { mutableStateOf(manager?.isPowerSaveMode == true) }
    DisposableEffect(context, manager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                powerSave = manager?.isPowerSaveMode == true
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        powerSave = manager?.isPowerSaveMode == true
        onDispose { context.unregisterReceiver(receiver) }
    }
    return powerSave
}

@Composable
private fun MountainScene(colors: List<Color>, scene: WeatherWallpaperScene?, phase: State<Float>) {
    val night = scene?.isDay == false
    val kind = scene?.atmosphere
    val sky = when {
        night -> Color(0xFF202F49) to Color(0xFF526A80)
        kind == WeatherAtmosphere.CLEAR || kind == WeatherAtmosphere.PARTLY_CLOUDY -> Color(0xFF729BAF) to Color(0xFFB4CDD0)
        kind == WeatherAtmosphere.STORM -> Color(0xFF4D657B) to Color(0xFF879FAB)
        kind == WeatherAtmosphere.RAIN -> Color(0xFF657E92) to Color(0xFF9BAFB9)
        kind == WeatherAtmosphere.SNOW -> Color(0xFF8A9FB2) to Color(0xFFC1D1D9)
        kind == WeatherAtmosphere.FOG -> Color(0xFF8BA1A9) to Color(0xFFB5C6CA)
        kind == WeatherAtmosphere.CLOUDY -> Color(0xFF7C96A6) to Color(0xFFACBFC6)
        else -> colors[0] to Color(0xFFB9CFD2)
    }
    val far = if (night) lerp(colors[1], Color(0xFF36485F), .72f) else colors[1]
    val near = if (night) lerp(colors[2], Color(0xFF182A40), .7f) else colors[2]
    val bottom = if (night) Color(0xFF1C3044) else Color(0xFF355A6A)
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().drawWithCache {
            val skyBrush = Brush.verticalGradient(listOf(lerp(colors[0], sky.first, .84f), sky.second, near.copy(alpha = .78f)))
            val farBrush = Brush.verticalGradient(listOf(far.copy(alpha = .76f), far), size.height * .55f, size.height)
            val nearBrush = Brush.verticalGradient(listOf(near, bottom), size.height * .68f, size.height)
            val farRidge = Path().apply {
                moveTo(0f, size.height * .69f)
                cubicTo(size.width * .13f, size.height * .55f, size.width * .19f, size.height * .58f, size.width * .31f, size.height * .68f)
                cubicTo(size.width * .46f, size.height * .79f, size.width * .55f, size.height * .55f, size.width * .69f, size.height * .57f)
                cubicTo(size.width * .82f, size.height * .59f, size.width * .91f, size.height * .71f, size.width, size.height * .65f)
                lineTo(size.width, size.height); lineTo(0f, size.height); close()
            }
            val nearRidge = Path().apply {
                moveTo(0f, size.height * .83f)
                cubicTo(size.width * .16f, size.height * .71f, size.width * .27f, size.height * .78f, size.width * .40f, size.height * .88f)
                cubicTo(size.width * .55f, size.height * .98f, size.width * .70f, size.height * .68f, size.width * .83f, size.height * .73f)
                cubicTo(size.width * .92f, size.height * .76f, size.width * .96f, size.height * .82f, size.width, size.height * .78f)
                lineTo(size.width, size.height); lineTo(0f, size.height); close()
            }
            onDrawBehind {
                drawRect(skyBrush)
                drawPath(farRidge, farBrush)
                drawPath(nearRidge, nearBrush)
            }
        })
        if (scene != null) AtmosphereLayer(scene, phase)
    }
}

@Composable
private fun AtmosphereLayer(scene: WeatherWallpaperScene, phase: State<Float>) {
    Box(Modifier.fillMaxSize().drawWithCache {
        val unit = size.minDimension
        val kind = scene.atmosphere
        val night = !scene.isDay
        val rainy = kind == WeatherAtmosphere.RAIN || kind == WeatherAtmosphere.STORM
        val snowy = kind == WeatherAtmosphere.SNOW
        val clear = kind == WeatherAtmosphere.CLEAR || kind == WeatherAtmosphere.PARTLY_CLOUDY
        // Keep the light source in open sky instead of behind the information cards.
        val sun = if (size.width > size.height) Offset(size.width * .43f, size.height * .13f)
            else Offset(size.width * .72f, size.height * .09f)
        val glowRadius = unit * (if (night) .24f else .48f)
        val glow = Brush.radialGradient(listOf(
            (if (night) Color(0xFFD6E5EF) else Color(0xFFFFE3AF)).copy(alpha = if (night) .16f else .28f), Color.Transparent
        ), sun, glowRadius)
        val cloudRadius = unit * .32f
        val cloud = Brush.radialGradient(listOf(
            Color.White.copy(alpha = if (night) .08f else .21f), Color.White.copy(alpha = if (night) .03f else .075f), Color.Transparent
        ), Offset.Zero, cloudRadius)
        val haze = Brush.radialGradient(listOf(Color(0xFFE0EBEF).copy(alpha = if (night) .07f else .13f), Color.Transparent), Offset.Zero, unit * .45f)
        // Deterministic seeds keep positions stable through recomposition and rotation.
        val particleRandom = java.util.Random(941L)
        val particles = List(if (snowy) 32 else 46) { Offset(particleRandom.nextFloat(), particleRandom.nextFloat()) }
        val starRandom = java.util.Random(173L)
        val stars = List(20) { Offset(starRandom.nextFloat(), .055f + starRandom.nextFloat() * .39f) }
        val veil = Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF1D3447).copy(alpha = .045f)), endY = size.height)
        onDrawBehind {
            val cycle = phase.value
            val angle = cycle * 2f * PI.toFloat()
            if (clear) {
                drawCircle(glow, glowRadius, sun, alpha = .91f + sin(angle) * .09f)
                if (!night) drawCircle(Color(0xFFFFE6B7).copy(alpha = .28f), unit * .028f, sun)
                if (night) {
                    // A quiet moon and distant stars, without flashing or sharp rays.
                    drawCircle(Color(0xFFDCE8E9).copy(alpha = .55f), unit * .019f, sun)
                    stars.forEachIndexed { index, point ->
                        drawCircle(Color.White.copy(alpha = .16f + .12f * (.5f + .5f * sin(angle + index))),
                            (.65f + index % 3 * .3f).dp.toPx(), Offset(point.x * size.width, point.y * size.height))
                    }
                }
            }
            if (kind != WeatherAtmosphere.CLEAR) {
                val count = if (kind == WeatherAtmosphere.PARTLY_CLOUDY) 3 else 5
                repeat(count) { index ->
                    val drift = sin(angle + index * 1.3f) * unit * .055f
                    val x = size.width * (.08f + index * .23f) + drift
                    val y = size.height * (.11f + (index % 3) * .12f)
                    softAtmosphere(cloud, cloudRadius, Offset(x, y), 1.65f, .46f)
                }
            }
            if (kind == WeatherAtmosphere.FOG || kind == WeatherAtmosphere.CLOUDY || rainy || snowy) {
                repeat(if (kind == WeatherAtmosphere.FOG) 4 else 2) { index ->
                    softAtmosphere(haze, unit * .45f,
                        Offset(size.width * (.22f + index * .29f) + cos(angle + index) * unit * .05f, size.height * (.4f + index * .115f)),
                        2f, if (kind == WeatherAtmosphere.FOG) .34f else .23f)
                }
            }
            if (rainy || snowy) {
                particles.forEachIndexed { index, point ->
                    val depth = .55f + index % 3 * .23f
                    val travel = (point.y + cycle * (if (snowy) 2f else 18f)) % 1f
                    val x = if (snowy) point.x * size.width + sin(angle * 2f + index) * unit * .022f
                        else ((point.x + (1f - travel) * .065f) % 1f) * size.width
                    val y = travel * (size.height + unit * .08f) - unit * .04f
                    if (snowy) {
                        drawCircle(Color.White.copy(alpha = .22f + depth * .16f), (1.1f + depth).dp.toPx(), Offset(x, y))
                    } else {
                        val length = (12f + depth * 13f).dp.toPx()
                        drawLine(Color(0xFFDDECF4).copy(alpha = .10f + depth * .13f), Offset(x, y), Offset(x - length * .23f, y + length),
                            (.65f + depth * .25f).dp.toPx(), StrokeCap.Round)
                    }
                }
            }
            drawRect(veil)
        }
    })
}

private fun DrawScope.softAtmosphere(brush: Brush, radius: Float, center: Offset, width: Float, height: Float) {
    translate(center.x, center.y) {
        scale(width, height, Offset.Zero) { drawCircle(brush, radius, Offset.Zero) }
    }
}
