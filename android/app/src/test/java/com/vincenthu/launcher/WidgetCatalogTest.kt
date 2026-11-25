package com.vincenthu.launcher

import org.junit.Assert.*
import org.junit.Test

class WidgetCatalogTest {
    private val music = WidgetCatalogItem("music/compact", "music", "华为音乐", "播放控制", 320, 80, true, false, null)
    private val calendar = WidgetCatalogItem("calendar/month", "calendar", "日历", "月视图", 300, 240, true, false, null)
    private val english = WidgetCatalogItem("news/top", "news", "Daily News", "Headlines", 300, 180, false, true, null)
    private val entries = listOf(music, calendar, english)

    @Test fun searchesAppAndWidgetLabelsWithCaseInsensitiveTerms() {
        assertEquals(listOf(music), WidgetCatalog.filter(entries, " 音乐  播放 "))
        assertEquals(listOf(english), WidgetCatalog.filter(entries, "DAILY\tHEADLINES"))
        assertEquals(listOf(calendar), WidgetCatalog.filter(entries, "月视图"))
    }

    @Test fun packageFilterCombinesWithSearchWithoutChangingOrder() {
        assertEquals(listOf(music), WidgetCatalog.filter(entries, "", "music"))
        assertTrue(WidgetCatalog.filter(entries, "日历", "music").isEmpty())
        assertEquals(entries, WidgetCatalog.filter(entries, "   "))
    }

    @Test fun largeArtworkIsBoundedWithoutStretching() {
        assertEquals(WidgetSize(1280, 640), WidgetCatalog.previewSize(4000, 2000))
        assertEquals(WidgetSize(320, 1280), WidgetCatalog.previewSize(1000, 4000))
        assertEquals(WidgetSize(200, 100), WidgetCatalog.previewSize(200, 100))
    }

    @Test fun missingArtworkDimensionsDoNotAllocateAnInvalidBitmap() {
        assertEquals(WidgetSize(1, 1), WidgetCatalog.previewSize(-1, 0))
    }
}
