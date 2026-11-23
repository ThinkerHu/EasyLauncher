package com.vincenthu.launcher

/** Keep enough history to refill the recent section after pinning an app. */
internal object DockHistory {
    const val VISIBLE_LIMIT = 3
    const val HISTORY_LIMIT = 24

    fun record(history: List<String>, id: String): List<String> =
        (listOf(id) + history).filter(String::isNotBlank).distinctBy(::packageOf).take(HISTORY_LIMIT)

    fun visible(
        history: List<String>,
        installed: Collection<String>,
        pinned: Collection<String>,
        limit: Int = VISIBLE_LIMIT
    ): List<String> {
        val available = installed.toSet()
        val pinnedPackages = pinned.map(::packageOf).toSet()
        return history.asSequence().filter { it in available && packageOf(it) !in pinnedPackages }
            .distinctBy(::packageOf).take(limit.coerceIn(0, HISTORY_LIMIT)).toList()
    }

    private fun packageOf(id: String) = id.substringBefore('/')
}

internal data class DockMetrics(
    val iconSize: Float,
    val gap: Float,
    val padding: Float,
    val width: Float,
    val scrollable: Boolean
)

internal object DockLayout {
    /** Size to contents; reduce spacing first, preserving 48 dp targets on small screens. */
    fun metrics(pinned: Int, recent: Int, availableWidth: Float): DockMetrics {
        val pins = pinned.coerceIn(0, 4)
        val recents = recent.coerceIn(0, DockHistory.VISIBLE_LIMIT)
        val icons = 1 + pins + recents
        val groups = 1 + (if (pins > 0) 1 else 0) + (if (recents > 0) 1 else 0)
        val dividers = groups - 1
        val gapUnits = icons - groups + dividers * 2
        val padding = 12f
        val available = availableWidth.coerceAtLeast(1f)
        val gap = if (icons * 56f + gapUnits * 12f + dividers + padding * 2 <= available) 12f else 8f
        val iconSize = ((available - padding * 2 - dividers - gapUnits * gap) / icons).coerceIn(48f, 56f)
        val contentWidth = icons * iconSize + gapUnits * gap + dividers + padding * 2
        return DockMetrics(iconSize, gap, padding, minOf(contentWidth, available), contentWidth > available + .5f)
    }
}
