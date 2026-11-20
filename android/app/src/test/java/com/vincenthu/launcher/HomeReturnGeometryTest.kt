package com.vincenthu.launcher

import org.junit.Assert.*
import org.junit.Test

class HomeReturnGeometryTest {
    @Test fun landscapeAndPortraitUseIndependentAxes() {
        assertEquals(33 * 51 + 25, homeReturnAnimationIndex(1000f, 800f, 0f, 0f, 2000f, 1200f))
        assertEquals(33 * 51 + 31, homeReturnAnimationIndex(736f, 1320f, 0f, 0f, 1200f, 2000f))
    }

    @Test fun windowOffsetIsRemovedBeforeChoosingPivot() {
        assertEquals(25 * 51 + 25, homeReturnAnimationIndex(1100f, 700f, 100f, 100f, 2000f, 1200f))
    }

    @Test fun everyGeneratedPivotIsReachableWithoutOutOfBoundsIndices() {
        for (y in 0..50) for (x in 0..50) {
            assertEquals(y * 51 + x, homeReturnAnimationIndex(x * 40f, y * 24f, 0f, 0f, 2000f, 1200f))
        }
    }

    @Test fun pivotErrorIsAtMostOnePercentOfEachDimension() {
        for ((width, height) in listOf(2000f to 1200f, 1200f to 2000f, 3840f to 2160f)) {
            for (i in 0..1000) {
                val x = width * i / 1000f
                val y = height * (1000 - i) / 1000f
                val index = homeReturnAnimationIndex(x, y, 0f, 0f, width, height)!!
                assertTrue(kotlin.math.abs(index % 51 * width / 50f - x) <= width * .01001f)
                assertTrue(kotlin.math.abs(index / 51 * height / 50f - y) <= height * .01001f)
            }
        }
    }

    @Test fun invisibleOrInvalidDestinationsNeverProduceAnAnimation() {
        assertNull(homeReturnAnimationIndex(-1f, 20f, 0f, 0f, 2000f, 1200f))
        assertNull(homeReturnAnimationIndex(20f, 1201f, 0f, 0f, 2000f, 1200f))
        assertNull(homeReturnAnimationIndex(20f, 20f, 0f, 0f, 0f, 1200f))
        assertNull(homeReturnAnimationIndex(Float.NaN, 20f, 0f, 0f, 2000f, 1200f))
        assertNull(homeReturnAnimationIndex(20f, 20f, 0f, 0f, Float.POSITIVE_INFINITY, 1200f))
    }
}
