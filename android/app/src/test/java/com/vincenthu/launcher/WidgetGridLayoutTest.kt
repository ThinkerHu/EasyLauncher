package com.vincenthu.launcher

import org.junit.Assert.*
import org.junit.Test

class WidgetGridLayoutTest {
    @Test fun multipleSmallWidgetsShareOnePage() {
        val pages = WidgetGridLayout.pages((1..4).map { WidgetGridItem(it, 1) })
        assertEquals(1, pages.size)
        assertEquals(listOf(0, 1, 2, 3), pages.single().items.map { it.slot })
    }

    @Test fun wideWidgetsShareAPageWithoutCrossingPortraitRows() {
        val pages = WidgetGridLayout.pages(listOf(WidgetGridItem(1, 2), WidgetGridItem(2, 2)))
        assertEquals(1, pages.size)
        assertEquals(listOf(0, 2), pages.single().items.map { it.slot })
    }

    @Test fun mixedCardsFillEarlierGapsBeforeCreatingAnotherPage() {
        val pages = WidgetGridLayout.pages(listOf(WidgetGridItem(1, 1), WidgetGridItem(2, 2), WidgetGridItem(3, 1), WidgetGridItem(4, 1)))
        assertEquals(2, pages.size)
        assertEquals(listOf(1, 3, 2), pages.first().items.map { it.appWidgetId })
        assertEquals(0, WidgetGridLayout.pageFor(pages, 3))
        assertEquals(1, WidgetGridLayout.pageFor(pages, 4))
    }

    @Test fun everyWidgetRemainsReachableAndSlotsNeverOverlap() {
        val ids = (1..19).map { WidgetGridItem(it, if (it % 3 == 0) 2 else 1) }
        val pages = WidgetGridLayout.pages(ids)
        assertEquals(ids.map { it.appWidgetId }.toSet(), pages.flatMap { it.items }.map { it.appWidgetId }.toSet())
        for (page in pages) {
            val occupied = page.items.flatMap { it.slot until it.slot + it.span }
            assertEquals(occupied.size, occupied.distinct().size)
            assertTrue(occupied.all { it in 0..3 })
            for (item in page.items.filter { it.span == 2 }) assertEquals(0, item.slot % 2)
        }
    }

    @Test fun removingAWidgetRepacksWithoutDuplicatingOrLosingOthers() {
        val pages = WidgetGridLayout.pages(listOf(WidgetGridItem(10, 1), WidgetGridItem(15, 1)))
        assertEquals(1, pages.size)
        assertEquals(listOf(10, 15), pages.single().items.map { it.appWidgetId })
        assertEquals(listOf(15), WidgetGridLayout.pages(listOf(WidgetGridItem(15, 1))).single().items.map { it.appWidgetId })
        assertTrue(WidgetGridLayout.pages(emptyList()).isEmpty())
    }

    @Test fun partiallyFilledLandscapeGridIsCenteredAsAGroup() {
        val page = WidgetGridPage(listOf(
            WidgetGridPlacement(10, 0, 1),
            WidgetGridPlacement(15, 1, 1)
        ))
        val bounds = WidgetGridLayout.bounds(page, width = 920f, height = 240f, landscape = true)
        assertEquals(2, bounds.size)
        assertEquals(bounds.first().x, (920f - bounds.last().x - bounds.last().width), .01f)
        assertEquals(bounds.first().y, (240f - bounds.first().height) / 2, .01f)
        assertTrue(bounds.last().x > bounds.first().x + bounds.first().width)
    }

    @Test fun portraitWideWidgetFillsTheRowAndSmallWidgetsShareEvenRows() {
        val page = WidgetGridPage(listOf(
            WidgetGridPlacement(10, 0, 1),
            WidgetGridPlacement(15, 1, 1),
            WidgetGridPlacement(17, 2, 2)
        ))
        val bounds = WidgetGridLayout.bounds(page, width = 560f, height = 580f, landscape = false)
        assertEquals(536f, bounds.last().width, .05f)
        assertEquals(bounds[0].y, bounds[1].y, .01f)
        assertEquals(bounds[0].x, (560f - bounds[1].x - bounds[1].width), .05f)
        assertTrue(bounds[2].y > bounds[1].y + bounds[1].height)
    }

    @Test fun geometryKeepsAllCardsInsideThePageWithConsistentSpacing() {
        val placements = WidgetGridPage((1..4).map { WidgetGridPlacement(it, it - 1, 1) })
        for ((dimensions, landscape) in listOf((920f to 240f) to true, (560f to 580f) to false)) {
            val (width, height) = dimensions
            val bounds = WidgetGridLayout.bounds(placements, width, height, landscape)
            assertEquals(4, bounds.size)
            assertTrue(bounds.all { it.x >= 0f && it.y >= 0f && it.x + it.width <= width + .01f && it.y + it.height <= height + .01f })
        }
    }

    @Test fun splitScreenAndCollapsedWindowsReduceSpacingBeforeOverflowing() {
        val page = WidgetGridPage((0..3).map { WidgetGridPlacement(it, it, 1) })
        for (width in listOf(1f, 12f, 160f, 320f, 680f, 920f)) {
            for (height in listOf(1f, 12f, 80f, 160f, 240f, 580f)) {
                val bounds = WidgetGridLayout.bounds(page, width, height, landscape = width > height)
                assertEquals(4, bounds.size)
                for (card in bounds) {
                    assertTrue(card.width > 0 && card.height > 0)
                    assertTrue(card.x >= 0 && card.y >= 0)
                    assertTrue(card.x + card.width <= width + .01f)
                    assertTrue(card.y + card.height <= height + .01f)
                    assertEquals(card.width, card.height, .01f)
                }
                bounds.forEachIndexed { index, a ->
                    bounds.drop(index + 1).forEach { b ->
                        assertTrue(a.x + a.width <= b.x + .01f || b.x + b.width <= a.x + .01f ||
                            a.y + a.height <= b.y + .01f || b.y + b.height <= a.y + .01f)
                    }
                }
            }
        }
    }

    @Test fun repeatedLayoutIsStableAndInvalidWindowHasNoPlacements() {
        val page = WidgetGridPage(listOf(WidgetGridPlacement(15, 0, 1), WidgetGridPlacement(17, 2, 2)))
        assertEquals(WidgetGridLayout.bounds(page, 920f, 240f, true), WidgetGridLayout.bounds(page, 920f, 240f, true))
        assertTrue(WidgetGridLayout.bounds(page, Float.NaN, 240f, true).isEmpty())
        assertTrue(WidgetGridLayout.bounds(page, 920f, 0f, true).isEmpty())
    }
}
