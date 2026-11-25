package com.vincenthu.launcher

import org.junit.Assert.*
import org.junit.Test

class WidgetLayoutTest {
    @Test fun squareWidgetFitsLandscapeWithoutExpandingAcrossDesktop() {
        val size = WidgetLayout.fit(920f, 180f, 220f, 220f)
        assertEquals(WidgetSize(180, 180), size)
    }

    @Test fun portraitAndLandscapeRespectBothAvailableDimensions() {
        for ((width, height) in listOf(920f to 180f, 520f to 480f, 160f to 75f)) {
            val size = WidgetLayout.fit(width, height, 300f, 110f)
            assertTrue(size.width in 1..width.toInt())
            assertTrue(size.height in 1..height.toInt())
            assertEquals(300f / 110f, size.width.toFloat() / size.height, .06f)
        }
    }

    @Test fun largeWidgetWidthRemainsBounded() {
        assertEquals(WidgetSize(620, 310), WidgetLayout.fit(1500f, 900f, 1200f, 600f))
    }

    @Test fun tallAndWideProvidersKeepTheirAspectRatio() {
        assertEquals(WidgetSize(100, 400), WidgetLayout.fit(700f, 400f, 100f, 400f))
        assertEquals(WidgetSize(600, 50), WidgetLayout.fit(700f, 400f, 600f, 50f))
    }

    @Test fun missingProviderDimensionsAndCollapsedWindowsRemainSafe() {
        assertEquals(WidgetSize(240, 120), WidgetLayout.fit(700f, 400f, 0f, 0f))
        assertEquals(WidgetSize(1, 1), WidgetLayout.fit(0f, -20f, 0f, 0f))
        assertEquals(WidgetSize(240, 120), WidgetLayout.fit(700f, 400f, Float.NaN, Float.NaN))
    }
}
