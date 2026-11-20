package com.vincenthu.launcher

import kotlin.math.roundToInt

/** Index into the generated destination grid, relative to the fullscreen window. */
internal fun homeReturnAnimationIndex(
    centerX: Float, centerY: Float, left: Float, top: Float, width: Float, height: Float
): Int? {
    if (listOf(centerX, centerY, left, top, width, height).any { !it.isFinite() } || width <= 0 || height <= 0) return null
    val x = (centerX - left) / width
    val y = (centerY - top) / height
    if (x !in 0f..1f || y !in 0f..1f) return null
    val steps = HomeReturnAnimationResources.STEPS
    return (y * steps).roundToInt() * (steps + 1) + (x * steps).roundToInt()
}
