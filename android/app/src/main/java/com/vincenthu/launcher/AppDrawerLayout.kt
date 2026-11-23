package com.vincenthu.launcher

internal object AppDrawerLayout {
    fun columns(availableWidth: Float, landscape: Boolean): Int =
        if (landscape) (availableWidth / 108f).toInt().coerceIn(3, 10)
        else (availableWidth / 120f).toInt().coerceIn(2, 4)

    fun recentLimit(landscape: Boolean) = if (landscape) 6 else 4
}
