package com.vincenthu.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.os.Process
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class AppShortcut(val info: ShortcutInfo, val label: String, val icon: Bitmap?)
internal data class AppShortcutResult(
    val shortcuts: List<AppShortcut> = emptyList(),
    val needsHomePermission: Boolean = false,
    val unavailable: Boolean = false
)

/** Query afresh on every long press: dynamic shortcuts and the HOME role can change. */
internal class LauncherShortcuts(context: Context) {
    private val launcher = context.applicationContext.getSystemService(LauncherApps::class.java)
    private val iconDensity = context.resources.displayMetrics.densityDpi

    suspend fun query(componentId: String): AppShortcutResult = withContext(Dispatchers.IO) {
        val component = ComponentName.unflattenFromString(componentId)
            ?: return@withContext AppShortcutResult()
        val service = launcher ?: return@withContext AppShortcutResult(unavailable = true)
        try {
            if (!service.hasShortcutHostPermission()) {
                return@withContext AppShortcutResult(needsHomePermission = true)
            }
            val query = LauncherApps.ShortcutQuery().setPackage(component.packageName)
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC)
            val shortcuts = service.getShortcuts(query, Process.myUserHandle()).orEmpty()
                .filter { it.isEnabled && (it.activity == null || it.activity == component) }
                .distinctBy { it.id }
                .sortedWith(compareBy<ShortcutInfo> { if (it.isDeclaredInManifest) 0 else 1 }.thenBy { it.rank }.thenBy { it.id })
                .mapNotNull { info ->
                    val label = (info.shortLabel ?: info.longLabel)?.toString()?.trim()
                        ?.takeIf(String::isNotEmpty) ?: return@mapNotNull null
                    val icon = runCatching { service.getShortcutIconDrawable(info, iconDensity)?.toBitmap(96, 96) }.getOrNull()
                    AppShortcut(info, label, icon)
                }
            AppShortcutResult(shortcuts = shortcuts)
        } catch (_: SecurityException) {
            AppShortcutResult(needsHomePermission = true)
        } catch (_: RuntimeException) {
            AppShortcutResult(unavailable = true)
        }
    }

    fun start(shortcut: AppShortcut): Boolean = try {
        val service = launcher
        if (service == null || !service.hasShortcutHostPermission()) false
        else { service.startShortcut(shortcut.info, null, null); true }
    } catch (_: RuntimeException) {
        false
    }
}
