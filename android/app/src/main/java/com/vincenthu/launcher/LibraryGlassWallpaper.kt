package com.vincenthu.launcher

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas as BitmapCanvas
import android.graphics.Color as BitmapColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path as BitmapPath
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.LruCache
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The refracted wallpaper is rendered once per aspect ratio on a worker thread.
 * Frames only translate two cached textures; no screen capture, live blur or API 33 shader.
 */
@Composable
internal fun LibraryGlassWallpaper(modifier: Modifier) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val powerSave = rememberWallpaperPowerSave()
    val running = LocalLauncherMotionEnabled.current && !powerSave &&
        lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val textures = produceState<GlassTextures?>(null, viewport) {
        if (viewport.width > 0 && viewport.height > 0) {
            value = withContext(Dispatchers.Default) { GlassWallpaperRenderer.render(viewport) }
        }
    }
    val phase: State<Float> = if (running) {
        rememberInfiniteTransition(label = "library-liquid-glass").animateFloat(
            initialValue = 0f, targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(32_000, easing = LinearEasing)),
            label = "glass-drift"
        )
    } else remember { mutableFloatStateOf(0f) }

    Canvas(modifier.clipToBounds().onSizeChanged { viewport = it }) {
        val texture = textures.value
        if (texture == null) {
            drawRect(Brush.linearGradient(
                listOf(Color(0xFF287F63), Color(0xFF718A4A), Color(0xFF164759)),
                Offset.Zero, Offset(size.width, size.height)
            ))
        } else {
            val angle = phase.value
            // Overscan keeps every edge covered during the entire drift cycle.
            val destination = IntSize((size.width * 1.06f).roundToInt(), (size.height * 1.06f).roundToInt())
            val origin = IntOffset((-size.width * .03f).roundToInt(), (-size.height * .03f).roundToInt())
            drawImage(texture.backdrop, dstSize = destination,
                dstOffset = origin + IntOffset(
                    (sin(angle) * size.width * .009f).roundToInt(),
                    (cos(angle) * size.height * .006f).roundToInt()
                ), filterQuality = FilterQuality.Low)
            drawImage(texture.lens, dstSize = destination,
                dstOffset = origin + IntOffset(
                    (sin(angle + .7f) * size.width * .012f).roundToInt(),
                    (cos(angle + .4f) * size.height * .009f).roundToInt()
                ), filterQuality = FilterQuality.Low)
        }
        // Stable contrast across the search bar, category labels and the gesture area.
        drawRect(Color(0xFF082F35).copy(alpha = .17f))
    }
}

private data class GlassTextures(val backdrop: ImageBitmap, val lens: ImageBitmap)

private object GlassWallpaperRenderer {
    private val cache = LruCache<String, GlassTextures>(2)

    @Synchronized
    fun render(viewport: IntSize): GlassTextures {
        val ratio = minOf(1f, 960f / maxOf(viewport.width, viewport.height))
        val width = (viewport.width * ratio).roundToInt().coerceAtLeast(1)
        val height = (viewport.height * ratio).roundToInt().coerceAtLeast(1)
        val key = "$width:$height"
        cache.get(key)?.let { return it }
        val w = width.toFloat()
        val h = height.toFloat()
        val unit = minOf(w, h)
        val backdrop = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = BitmapCanvas(backdrop)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val clamp = Shader.TileMode.CLAMP
        fun color(value: String) = BitmapColor.parseColor(value)
        fun field(x: Float, y: Float, radius: Float, start: String) {
            paint.shader = RadialGradient(x, y, radius, color(start), BitmapColor.TRANSPARENT, clamp)
            canvas.drawPaint(paint)
        }
        paint.shader = LinearGradient(0f, 0f, w * .7f, h,
            intArrayOf(color("#168267"), color("#487B48"), color("#133E54")), null, clamp)
        canvas.drawPaint(paint)
        field(w * 1.02f, -h * .05f, maxOf(w, h) * .88f, "#C0B867")
        field(-w * .08f, h * .27f, maxOf(w, h) * .63f, "#30AA78")
        field(w * .55f, h * .96f, unit * .72f, "#547F8D")
        // A broad blurred fold gives the glass something visible to refract.
        val fold = BitmapPath().apply {
            moveTo(w * .65f, -h * .3f)
            cubicTo(w * .51f, h * .13f, w * .10f, h * .43f, w * .34f, h * .74f)
            cubicTo(w * .55f, h * 1.04f, w * .92f, h * .70f, w * 1.3f, h * .96f)
        }
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = unit * .26f
        paint.color = color("#69305F54")
        paint.maskFilter = BlurMaskFilter(unit * .055f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(fold, paint)
        paint.strokeWidth = unit * .095f
        paint.color = color("#A6DEE1BA")
        paint.maskFilter = BlurMaskFilter(unit * .043f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawPath(fold, paint)
        paint.maskFilter = null
        paint.style = Paint.Style.FILL

        val lens = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val glass = BitmapCanvas(lens)
        // A continuously curved sheet, with its ends extending beyond the screen.
        fun center(y: Float) = w * (.52f + .15f * cos(y * PI).toFloat() - .16f * sin(y * PI).toFloat())
        fun halfWidth(y: Float) = unit * (.14f + .025f * sin(y * PI).toFloat())
        fun edge(direction: Float): BitmapPath = BitmapPath().apply {
            for (step in 0..72) {
                val t = -.15f + step / 72f * 1.3f
                val x = center(t) + direction * halfWidth(t)
                if (step == 0) moveTo(x, t * h) else lineTo(x, t * h)
            }
        }
        val sheet = edge(-1f).apply {
            for (step in 72 downTo 0) {
                val t = -.15f + step / 72f * 1.3f
                lineTo(center(t) + halfWidth(t), t * h)
            }
            close()
        }
        // Displace wallpaper samples through the curved sheet. This bitmap mesh
        // is rasterized once and also works on the tablet's Android 10 renderer.
        val steps = 48
        val mesh = FloatArray((steps + 1) * (steps + 1) * 2)
        var index = 0
        for (row in 0..steps) for (col in 0..steps) {
            val y = row.toFloat() / steps
            val x = col.toFloat() / steps * w
            val distance = ((x - center(y)) / halfWidth(y)).coerceIn(-1f, 1f)
            val inside = abs(x - center(y)) < halfWidth(y)
            val distortion = if (inside) sin(distance * PI).toFloat() * unit * .052f else 0f
            mesh[index++] = x + distortion
            mesh[index++] = y * h + if (inside) cos(distance * PI / 2).toFloat() * unit * .018f else 0f
        }
        glass.save()
        glass.clipPath(sheet)
        glass.drawBitmapMesh(backdrop, steps, steps, mesh, 0, null, 0, paint)
        paint.shader = LinearGradient(w * .25f, 0f, w * .76f, h,
            intArrayOf(color("#24ECF9F0"), color("#08FFFFFF"), color("#2522A6A3")), null, clamp)
        glass.drawPath(sheet, paint)
        // Fresnel highlights: a luminous outer rim and a dark inner seam make
        // the broad transparent sheet read as glass rather than a painted stripe.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = unit * .018f
        paint.shader = null
        paint.color = color("#1013444C")
        paint.maskFilter = BlurMaskFilter(unit * .018f, BlurMaskFilter.Blur.NORMAL)
        glass.drawPath(edge(1f), paint)
        paint.maskFilter = null
        glass.restore()
        paint.strokeWidth = unit * .012f
        paint.color = color("#5AECFFE6")
        paint.maskFilter = BlurMaskFilter(unit * .016f, BlurMaskFilter.Blur.NORMAL)
        glass.drawPath(edge(-1f), paint)
        paint.maskFilter = null
        paint.strokeWidth = maxOf(.8f, unit * .002f)
        paint.shader = LinearGradient(0f, 0f, w * .8f, h,
            intArrayOf(color("#A8FCFFE8"), color("#548BE9DC"), color("#8CDCF3FF")), null, clamp)
        glass.drawPath(edge(-1f), paint)
        paint.shader = LinearGradient(0f, 0f, w, h,
            intArrayOf(color("#55FBF7C7"), color("#199BE9DC"), color("#70E8F9FF")), null, clamp)
        glass.drawPath(edge(1f), paint)

        return GlassTextures(backdrop.asImageBitmap(), lens.asImageBitmap()).also { cache.put(key, it) }
    }
}
