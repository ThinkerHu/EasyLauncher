package com.vincenthu.launcher

import org.junit.Assert.*
import org.junit.Test

class HomeLayoutTest {
    @Test fun firstPageReservesHeaderAndOtherPagesUseFullCapacity() {
        assertEquals(14, HomeLayout.capacity(0))
        assertEquals(28, HomeLayout.capacity(1))
        assertEquals(0, HomeLayout.pageStart(0))
        assertEquals(14, HomeLayout.pageStart(1))
        assertEquals(42, HomeLayout.pageStart(2))
        assertEquals(70, HomeLayout.pageStart(3))
        assertEquals(1, HomeLayout.pageCount(0))
        assertEquals(1, HomeLayout.pageCount(14))
        assertEquals(2, HomeLayout.pageCount(15))
        assertEquals(2, HomeLayout.pageCount(42))
        assertEquals(3, HomeLayout.pageCount(43))
    }

    @Test fun allDesktopItemsStayReachableExactlyOnceInTheirOriginalOrder() {
        for (count in 0..200) {
            val home = (0 until count).toList()
            val pages = (0 until HomeLayout.pageCount(count)).map { HomeLayout.members(home, it) }
            assertEquals(home, pages.flatten())
            assertEquals(home.size, pages.flatten().distinct().size)
            assertTrue(pages.drop(1).all { it.size <= HomeLayout.FULL_PAGE_SIZE })
        }
    }

    @Test fun rotationsKeepPageMembershipAndHaveRoomForEveryIcon() {
        val home = (0 until 103).toList()
        for (landscape in listOf(true, false)) {
            for (page in 0 until HomeLayout.pageCount(home.size)) {
                val members = HomeLayout.members(home, page)
                assertTrue(members.size <= HomeLayout.columns(landscape) * HomeLayout.rows(landscape, page))
            }
        }
        assertEquals(4, HomeLayout.rows(true, 1))
        assertEquals(7, HomeLayout.rows(false, 1))
        assertEquals((14..41).toList(), HomeLayout.members(home, 1))
    }

    @Test fun invalidAndWidgetPagesNeverExposeAppTargets() {
        val home = (0 until 42).toList()
        assertTrue(HomeLayout.members(home, -1).isEmpty())
        assertTrue(HomeLayout.members(home, 2).isEmpty())
    }

    @Test fun crossPageReorderUsesTheFullPageBoundary() {
        val home = (0 until 71).toMutableList()
        val source = HomeLayout.members(home, 1).last()
        val target = HomeLayout.members(home, 2).first()
        home[home.indexOf(source)] = target
        home[42] = source
        assertEquals(target, HomeLayout.members(home, 1).last())
        assertEquals(source, HomeLayout.members(home, 2).first())
        assertEquals((0 until 71).toSet(), home.toSet())
    }
}
