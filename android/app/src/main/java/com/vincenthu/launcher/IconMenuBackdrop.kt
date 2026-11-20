package com.vincenthu.launcher

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.roundToInt

/** Capture only the launcher's own window, never another app or the display. */
internal suspend fun captureIconMenuBackdrop(window: Window): Bitmap? {
    val view = window.decorView
    if (!view.isAttachedToWindow || view.width == 0 || view.height == 0) return null
    val scale = (420f / max(view.width, view.height)).coerceAtMost(1f)
    val bitmap = createBitmap(max(1, (view.width * scale).roundToInt()), max(1, (view.height * scale).roundToInt()))
    val copied = suspendCancellableCoroutine { continuation ->
        try {
            PixelCopy.request(window, bitmap, { result ->
                if (continuation.isActive) continuation.resume(result == PixelCopy.SUCCESS)
                else bitmap.recycle()
            }, Handler(Looper.getMainLooper()))
        } catch (_: IllegalArgumentException) {
            if (continuation.isActive) continuation.resume(false)
        }
    }
    if (!copied) { bitmap.recycle(); return null }
    // A small, bounded CPU blur also works on API 26–30 where RenderEffect does not.
    return withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        var input = IntArray(width * height)
        var output = IntArray(input.size)
        bitmap.getPixels(input, 0, width, 0, 0, width, height)
        val radius = 6
        val diameter = radius * 2 + 1
        repeat(3) {
            for (y in 0 until height) {
                var red = 0; var green = 0; var blue = 0
                for (dx in -radius..radius) {
                    val pixel = input[y * width + dx.coerceIn(0, width - 1)]
                    red += pixel shr 16 and 255; green += pixel shr 8 and 255; blue += pixel and 255
                }
                for (x in 0 until width) {
                    output[y * width + x] = (255 shl 24) or ((red / diameter) shl 16) or ((green / diameter) shl 8) or (blue / diameter)
                    val remove = input[y * width + (x - radius).coerceIn(0, width - 1)]
                    val add = input[y * width + (x + radius + 1).coerceIn(0, width - 1)]
                    red += (add shr 16 and 255) - (remove shr 16 and 255)
                    green += (add shr 8 and 255) - (remove shr 8 and 255)
                    blue += (add and 255) - (remove and 255)
                }
            }
            val swap = input; input = output; output = swap
            for (x in 0 until width) {
                var red = 0; var green = 0; var blue = 0
                for (dy in -radius..radius) {
                    val pixel = input[dy.coerceIn(0, height - 1) * width + x]
                    red += pixel shr 16 and 255; green += pixel shr 8 and 255; blue += pixel and 255
                }
                for (y in 0 until height) {
                    output[y * width + x] = (255 shl 24) or ((red / diameter) shl 16) or ((green / diameter) shl 8) or (blue / diameter)
                    val remove = input[(y - radius).coerceIn(0, height - 1) * width + x]
                    val add = input[(y + radius + 1).coerceIn(0, height - 1) * width + x]
                    red += (add shr 16 and 255) - (remove shr 16 and 255)
                    green += (add shr 8 and 255) - (remove shr 8 and 255)
                    blue += (add and 255) - (remove and 255)
                }
            }
            val swapped = input; input = output; output = swapped
        }
        bitmap.setPixels(input, 0, width, 0, 0, width, height)
        bitmap
    }
}
