package com.vincenthu.launcher

import kotlin.math.roundToInt

/** Searchable presentation metadata. Browsing never allocates a widget id. */
internal data class WidgetCatalogItem(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val label: String,
    val widthDp: Int,
    val heightDp: Int,
    val resizable: Boolean,
    val configurable: Boolean,
    val compatibilityHint: String?
) {
    val sizeLabel: String get() {
        if (widthDp <= 1 || heightDp <= 1) return "应用指定尺寸"
        val ratio = widthDp.toFloat() / heightDp
        return when {
            ratio >= 3f -> "横条样式"
            ratio >= 1.4f -> "宽幅样式"
            ratio <= .7f -> "纵向样式"
            maxOf(widthDp, heightDp) >= 280 -> "大方块样式"
            else -> "方形样式"
        }
    }
}

internal object WidgetCatalog {
    fun filter(items: List<WidgetCatalogItem>, query: String, packageName: String? = null): List<WidgetCatalogItem> {
        val terms = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return items.filter { item ->
            (packageName == null || item.packageName == packageName) && terms.all { term ->
                item.label.contains(term, ignoreCase = true) || item.appLabel.contains(term, ignoreCase = true)
            }
        }
    }

    /** Bound image decode/render cost without stretching the supplied artwork. */
    fun previewSize(width: Int, height: Int, maxSide: Int = 1280): WidgetSize {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val scale = minOf(1f, maxSide.coerceAtLeast(1).toFloat() / maxOf(w, h))
        return WidgetSize((w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1))
    }
}
