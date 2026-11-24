package com.vincenthu.launcher

import org.junit.Assert.*
import org.junit.Test

class HomeFolderDropTest {
    private val work = HomeFolder("folder:work", "工作", listOf("notes/.Main", "mail/.Main"))
    private val other = HomeFolder("folder:other", "其他", listOf("camera/.Main"))

    @Test fun moveRemovesDesktopAppAndAppendsToFolderWithoutMovingFolder() {
        val home = listOf("a/.Main", work.id, "b/.Main", other.id)
        val result = HomeFolderDrop.move(home, listOf(work, other), "a/.Main", work.id)!!
        assertEquals(listOf(work.id, "b/.Main", other.id), result.home)
        assertEquals(work.apps + "a/.Main", result.folders[0].apps)
        assertEquals(work.name, result.folders[0].name)
        assertEquals(other, result.folders[1])
        assertEquals(4, home.size)
        assertEquals(2, work.apps.size)
    }

    @Test fun repeatedDropCannotDuplicateMembersOrRemoveUnrelatedApps() {
        val home = listOf(work.id, "notes/.Main", "b/.Main")
        val result = HomeFolderDrop.move(home, listOf(work), "notes/.Main", work.id)!!
        assertEquals(work.apps, result.folders.single().apps)
        assertEquals(listOf(work.id, "b/.Main"), result.home)
        assertNull(HomeFolderDrop.move(result.home, result.folders, "notes/.Main", work.id))
    }

    @Test fun foldersCannotBeNestedOrDroppedIntoThemselves() {
        val home = listOf(work.id, other.id)
        assertNull(HomeFolderDrop.move(home, listOf(work, other), work.id, other.id))
        assertNull(HomeFolderDrop.move(home, listOf(work), work.id, work.id))
    }

    @Test fun removedOrMissingSourceAndTargetDoNotChangeSavedLayout() {
        assertNull(HomeFolderDrop.move(listOf(work.id), listOf(work), "a/.Main", work.id))
        assertNull(HomeFolderDrop.move(listOf("a/.Main"), listOf(work), "a/.Main", work.id))
        assertNull(HomeFolderDrop.move(listOf("a/.Main", "folder:missing"), listOf(work), "a/.Main", "folder:missing"))
        assertNull(HomeFolderDrop.move(listOf("a/.Main", "b/.Main"), listOf(work), "a/.Main", "b/.Main"))
    }

    @Test fun crossPageMovePreservesEveryOtherDesktopItemInOrder() {
        val home = listOf(work.id) + (1..HomeLayout.PAGE_SIZE).map { "app$it/.Main" }
        val appId = home.last()
        assertEquals(2, HomeLayout.pageCount(home.size))
        val result = HomeFolderDrop.move(home, listOf(work), appId, work.id)!!
        assertEquals(home.dropLast(1), result.home)
        assertEquals(1, HomeLayout.pageCount(result.home.size))
        assertTrue(appId in result.folders.single().apps)
    }
}
