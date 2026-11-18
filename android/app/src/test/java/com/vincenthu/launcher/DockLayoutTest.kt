package com.vincenthu.launcher

import org.junit.Assert.*
import org.junit.Test

class DockLayoutTest {
    @Test fun recentHistoryMovesNewestToFrontAndDeduplicatesByPackage() {
        val initial = listOf("camera/.Main", "music/.Main")
        assertEquals(listOf("browser/.Home", "camera/.Main", "music/.Main"), DockHistory.record(initial, "browser/.Home"))
        assertEquals(listOf("camera/.New", "music/.Main"), DockHistory.record(initial, "camera/.New"))
    }

    @Test fun recentsExcludePinnedAppsAndKeepMostRecentThree() {
        val history = listOf("a/.Home", "b/.Home", "c/.Home", "d/.Home", "e/.Home")
        assertEquals(listOf("c/.Home", "d/.Home", "e/.Home"), DockHistory.visible(history, history, listOf("a/.Pinned", "b/.Pinned")))
    }

    @Test fun unavailableAppsAndDuplicateActivitiesDoNotShowInHistory() {
        val history = listOf("a/.Home", "a/.Settings", "missing/.Home", "b/.Home")
        assertEquals(listOf("a/.Home", "b/.Home"), DockHistory.visible(history, listOf("a/.Home", "b/.Home"), emptyList()))
    }

    @Test fun dockWidthGrowsWithContentsAndCollapsesWhenEmpty() {
        val empty = DockLayout.metrics(0, 0, 500f)
        val pinned = DockLayout.metrics(4, 0, 500f)
        val full = DockLayout.metrics(4, 3, 500f)
        assertTrue(empty.width < pinned.width)
        assertTrue(pinned.width < full.width)
        assertFalse(full.scrollable)
    }

    @Test fun narrowScreensPreserveIconTouchSizeAndEnableHorizontalScroll() {
        val compact = DockLayout.metrics(4, 3, 340f)
        assertTrue(compact.scrollable)
        assertTrue(compact.iconSize >= 48f)
        assertTrue(compact.width <= 340f)
    }

    @Test fun storedHistoryIsBounded() {
        val history = (0..DockHistory.HISTORY_LIMIT).map { "app$it/.Home" }
        assertEquals(DockHistory.HISTORY_LIMIT, DockHistory.record(history, "new/.Home").size)
    }
}
