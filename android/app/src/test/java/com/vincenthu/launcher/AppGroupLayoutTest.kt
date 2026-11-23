package com.vincenthu.launcher

import org.junit.Assert.*
import org.junit.Test

class AppGroupLayoutTest {
    @Test fun tabletPagesHaveRoomForIconsAndLabelsInBothOrientations() {
        val portrait = AppGroupLayout.calculate(512f, 550f, false, 1.15f)
        val landscape = AppGroupLayout.calculate(820f, 300f, true, 1.15f)
        assertEquals(4, portrait.columns)
        assertEquals(3, portrait.rows)
        assertEquals(6, landscape.columns)
        assertEquals(2, landscape.rows)
    }

    @Test fun everyApplicationRemainsReachableAfterRotationAndInSplitScreen() {
        val count = 57
        for (layout in listOf(
            AppGroupLayout.calculate(512f, 550f, false, 1.15f),
            AppGroupLayout.calculate(820f, 300f, true, 1.15f),
            AppGroupLayout.calculate(240f, 160f, false, 1.5f)
        )) {
            val indices = (0 until layout.pageCount(count)).flatMap { page ->
                (layout.pageStart(page) until (layout.pageStart(page) + layout.capacity).coerceAtMost(count)).toList()
            }
            assertEquals((0 until count).toList(), indices)
        }
    }

    @Test fun emptyAndExactPagesHaveNoUnreachableOrPhantomPage() {
        val layout = AppGroupLayout(4, 3)
        assertEquals(1, layout.pageCount(0))
        assertEquals(1, layout.pageCount(12))
        assertEquals(2, layout.pageCount(13))
        assertEquals(2, layout.pageCount(24))
    }

    @Test fun largeTextAndShortWindowsReduceRowsBeforeLabelsOverlap() {
        val standard = AppGroupLayout.calculate(512f, 230f, false, 1f)
        val largeText = AppGroupLayout.calculate(512f, 230f, false, 1.8f)
        assertEquals(2, standard.rows)
        assertEquals(1, largeText.rows)
    }
}
