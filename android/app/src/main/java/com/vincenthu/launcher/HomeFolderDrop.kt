package com.vincenthu.launcher

internal data class HomeFolderDropResult(val home: List<String>, val folders: List<HomeFolder>)

/** Move a desktop app into an existing desktop folder without changing other placements. */
internal object HomeFolderDrop {
    fun move(home: List<String>, folders: List<HomeFolder>, appId: String, folderId: String): HomeFolderDropResult? {
        if (appId == folderId || appId.startsWith("folder:") || folders.any { it.id == appId }) return null
        if (appId !in home || folderId !in home) return null
        val folder = folders.firstOrNull { it.id == folderId } ?: return null
        return HomeFolderDropResult(
            home = home.filterNot { it == appId },
            folders = folders.map { if (it.id == folderId) folder.copy(apps = (folder.apps + appId).distinct()) else it }
        )
    }
}
