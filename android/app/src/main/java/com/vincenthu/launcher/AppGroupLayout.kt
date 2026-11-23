package com.vincenthu.launcher

import kotlin.math.ceil

/** Stable row-major pages; orientation changes capacity, never the app ordering. */
internal data class AppGroupLayout(val columns: Int, val rows: Int) {
    val capacity: Int get() = columns * rows
    fun pageCount(appCount: Int): Int = ceil(appCount.coerceAtLeast(0).toDouble() / capacity).toInt().coerceAtLeast(1)
    fun pageStart(page: Int): Int = page.coerceAtLeast(0) * capacity

    companion object {
        fun calculate(width: Float, gridHeight: Float, landscape: Boolean, fontScale: Float): AppGroupLayout {
            val labelExtra = ((fontScale - 1f).coerceAtLeast(0f) * 20f)
            val columns = (width / 104f).toInt().coerceIn(2, if (landscape) 6 else 4)
            val rows = (gridHeight / (112f + labelExtra)).toInt().coerceIn(1, if (landscape) 2 else 3)
            return AppGroupLayout(columns, rows)
        }
    }
}
