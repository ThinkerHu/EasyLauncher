package com.vincenthu.launcher

import kotlin.math.roundToInt

internal data class WidgetSize(val width: Int, val height: Int)

internal data class WidgetGridItem(val appWidgetId: Int, val span: Int)
internal data class WidgetGridPlacement(val appWidgetId: Int, val slot: Int, val span: Int)
internal data class WidgetGridPage(val items: List<WidgetGridPlacement>)
internal data class WidgetGridBounds(val appWidgetId: Int, val x: Float, val y: Float, val width: Float, val height: Float)

/** Four stable slots: 4×1 in landscape, 2×2 in portrait. Wide cards use two. */
internal object WidgetGridLayout {
    fun span(width: Int, height: Int): Int = if (width > 0 && height > 0 && width.toFloat() / height >= 1.7f) 2 else 1

    fun pages(items: List<WidgetGridItem>): List<WidgetGridPage> {
        val pages = mutableListOf<MutableList<WidgetGridPlacement>>()
        for (item in items.distinctBy { it.appWidgetId }) {
            val span = item.span.coerceIn(1, 2)
            var placed = false
            for (page in pages) {
                val slot = freeSlot(page, span)
                if (slot != null) {
                    page += WidgetGridPlacement(item.appWidgetId, slot, span)
                    placed = true
                    break
                }
            }
            if (!placed) pages += mutableListOf(WidgetGridPlacement(item.appWidgetId, 0, span))
        }
        return pages.map { WidgetGridPage(it.sortedBy { placement -> placement.slot }) }
    }

    private fun freeSlot(items: List<WidgetGridPlacement>, span: Int): Int? {
        val occupied = BooleanArray(4)
        for (item in items) repeat(item.span) { occupied[item.slot + it] = true }
        return (0..3).firstOrNull { slot ->
            (span == 1 || slot % 2 == 0) && slot + span <= 4 && (slot until slot + span).none { occupied[it] }
        }
    }

    fun pageFor(pages: List<WidgetGridPage>, appWidgetId: Int): Int =
        pages.indexOfFirst { page -> page.items.any { it.appWidgetId == appWidgetId } }.coerceAtLeast(0)

    /** Center the occupied grid as a group, then give each widget a stable cell. */
    fun bounds(
        page: WidgetGridPage,
        width: Float,
        height: Float,
        landscape: Boolean,
        inset: Float = 12f,
        gap: Float = 18f
    ): List<WidgetGridBounds> {
        if (page.items.isEmpty() || !width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) return emptyList()
        val columns = if (landscape && width >= 680f) 4 else 2
        val maxRows = 4 / columns
        // Reduce decoration first in split-screen or while the window resizes.
        val safeInset = inset.takeIf { it.isFinite() }?.coerceIn(0f, minOf(width, height) / 8f) ?: 0f
        val safeGap = minOf(
            gap.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f,
            (width - safeInset * 2) / (columns * 4f),
            (height - safeInset * 2) / (maxRows * 4f)
        )
        val cellSize = minOf(
            (width - safeInset * 2 - safeGap * (columns - 1)) / columns,
            (height - safeInset * 2 - safeGap * (maxRows - 1)) / maxRows
        )
        val cellWidth = cellSize
        val cellHeight = cellSize
        val usedColumns = page.items.maxOf { it.slot % columns + it.span.coerceIn(1, columns) }.coerceAtMost(columns)
        val usedRows = page.items.maxOf { it.slot / columns + 1 }.coerceAtMost(maxRows)
        val occupiedWidth = cellWidth * usedColumns + safeGap * (usedColumns - 1)
        val occupiedHeight = cellHeight * usedRows + safeGap * (usedRows - 1)
        val originX = ((width - occupiedWidth) / 2).coerceAtLeast(0f)
        val originY = ((height - occupiedHeight) / 2).coerceAtLeast(0f)
        return page.items.map { item ->
            val span = item.span.coerceIn(1, columns)
            val column = item.slot % columns
            val row = item.slot / columns
            WidgetGridBounds(
                appWidgetId = item.appWidgetId,
                x = originX + column * (cellWidth + safeGap),
                y = originY + row * (cellHeight + safeGap),
                width = cellWidth * span + safeGap * (span - 1),
                height = cellHeight
            )
        }
    }
}

/** Widget dimensions are dp; preserve provider proportions inside the page. */
internal object WidgetLayout {
    fun fit(availableWidth: Float, availableHeight: Float, preferredWidth: Float, preferredHeight: Float): WidgetSize {
        val widthLimit = availableWidth.takeIf { it.isFinite() }?.coerceAtLeast(1f) ?: 1f
        val heightLimit = availableHeight.takeIf { it.isFinite() }?.coerceAtLeast(1f) ?: 1f
        val ratio = if (preferredWidth.isFinite() && preferredHeight.isFinite() && preferredWidth > 0f && preferredHeight > 0f)
            preferredWidth / preferredHeight else 2f
        val width = minOf(widthLimit, 620f, maxOf(240f, preferredWidth.takeIf { it.isFinite() } ?: 240f), heightLimit * ratio)
        return WidgetSize(
            width.roundToInt().coerceIn(1, widthLimit.toInt().coerceAtLeast(1)),
            (width / ratio).roundToInt().coerceIn(1, heightLimit.toInt().coerceAtLeast(1))
        )
    }
}
