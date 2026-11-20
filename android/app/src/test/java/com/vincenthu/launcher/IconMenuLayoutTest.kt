package com.vincenthu.launcher

import org.junit.Assert.*
import org.junit.Test

class IconMenuLayoutTest {
    @Test fun firstRowOpensBelowWithoutCoveringIcon() {
        val result = placeIconMenu(1000, 600, 80, 140, 148, 208, 216, 260, 14, 16, 24, 24)
        assertTrue(result.below)
        assertEquals(222, result.top)
        assertEquals(80, result.left)
    }
    @Test fun dockOpensAboveAndRemainsOnScreen() {
        val result = placeIconMenu(1000, 600, 80, 480, 140, 540, 216, 260, 14, 16, 24, 24)
        assertFalse(result.below)
        assertEquals(206, result.top)
        assertTrue(result.top + 260 < 480)
    }
    @Test fun rightEdgeAlignsWithIconsRightEdge() {
        val result = placeIconMenu(1000, 600, 910, 140, 978, 208, 216, 200, 14, 16)
        assertEquals(762, result.left)
        assertTrue(result.left + 216 <= 984)
    }
    @Test fun tallMenuUsesLargerSideAndScrolls() {
        val result = placeIconMenu(1000, 600, 80, 260, 148, 328, 216, 700, 14, 16, 24, 24)
        assertTrue(result.below)
        assertEquals(218, result.maxHeight)
        assertEquals(342, result.top)
    }
    @Test fun portraitKeepsMenuInsideSafeEdges() {
        val result = placeIconMenu(600, 1000, 512, 600, 580, 668, 216, 400, 14, 16, 24, 40)
        assertFalse(result.below)
        assertEquals(364, result.left)
        assertEquals(186, result.top)
    }
    @Test fun topLeftClampsHorizontalMargin() {
        val result = placeIconMenu(320, 560, 5, 70, 73, 138, 216, 200, 14, 16, 24, 24)
        assertEquals(16, result.left)
        assertTrue(result.below)
    }
}
