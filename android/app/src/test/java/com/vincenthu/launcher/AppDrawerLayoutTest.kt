package com.vincenthu.launcher

import org.junit.Assert.*
import org.junit.Test

class AppDrawerLayoutTest {
    @Test fun tabletGridPreservesTheApprovedLandscapeAndPortraitDensity() {
        assertEquals(8, AppDrawerLayout.columns(900f, landscape = true))
        assertEquals(4, AppDrawerLayout.columns(540f, landscape = false))
    }

    @Test fun splitScreenStillLeavesRoomForLabelsAndTouchTargets() {
        assertEquals(2, AppDrawerLayout.columns(280f, landscape = false))
        assertEquals(3, AppDrawerLayout.columns(360f, landscape = true))
        assertEquals(10, AppDrawerLayout.columns(1600f, landscape = true))
    }

    @Test fun drawerHistoryUsesRealRecencyWithoutChangingDockLimit() {
        val history = (1..8).map { "app$it/.Home" }
        val installed = history.filterNot { it == "app2/.Home" }
        val landscape = DockHistory.visible(history, installed, emptyList(), AppDrawerLayout.recentLimit(true))
        val portrait = DockHistory.visible(history, installed, emptyList(), AppDrawerLayout.recentLimit(false))
        assertEquals(listOf("app1/.Home", "app3/.Home", "app4/.Home", "app5/.Home", "app6/.Home", "app7/.Home"), landscape)
        assertEquals(landscape.take(4), portrait)
        assertEquals(landscape.take(3), DockHistory.visible(history, installed, emptyList()))
    }

    @Test fun drawerHistoryAllowsPinnedAppsButDeduplicatesActivities() {
        val history = listOf("a/.Home", "a/.Other", "b/.Home")
        assertEquals(listOf("a/.Home", "b/.Home"), DockHistory.visible(history, history, emptyList(), 6))
        assertEquals(listOf("b/.Home"), DockHistory.visible(history, history, listOf("a/.Home")))
    }
}
