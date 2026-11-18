package com.vincenthu.launcher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity

/** Owns the system widget host and the bind/configure activity flow. */
class WidgetHostController(
    private val activity: ComponentActivity,
    private val model: LauncherModel
) {
    private val manager = AppWidgetManager.getInstance(activity)
    private val host = AppWidgetHost(activity, HOST_ID)
    private var pending: PendingWidget? = null

    fun start() = host.startListening()
    fun stop() = host.stopListening()

    fun providers(): List<AppWidgetProviderInfo> = manager.installedProviders
        .distinctBy { it.provider.flattenToString() }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.loadLabel(activity.packageManager).toString() })

    fun beginBind(provider: AppWidgetProviderInfo, launch: (Intent) -> Unit) {
        if (pending != null) return
        val appWidgetId = host.allocateAppWidgetId()
        pending = PendingWidget(appWidgetId, provider, Phase.BIND)
        val bound = runCatching {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, provider.provider)
        }.getOrDefault(false)
        if (bound) beginConfiguration(launch) else launch(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
        })
    }

    fun handleActivityResult(resultCode: Int, data: Intent?, launch: (Intent) -> Unit) {
        val current = pending ?: return
        if (resultCode != Activity.RESULT_OK) {
            discardPending()
            return
        }
        when (current.phase) {
            Phase.BIND -> beginConfiguration(launch)
            Phase.CONFIGURE -> finishPending()
        }
    }

    fun createView(widget: HomeWidget): View {
        val info = manager.getAppWidgetInfo(widget.appWidgetId)
            ?: providers().firstOrNull { it.provider.flattenToString() == widget.provider }
        if (info == null) return unavailableView("小组件不可用")
        return runCatching { host.createView(activity, widget.appWidgetId, info) }
            .getOrElse { unavailableView("小组件暂时无法显示") }
    }

    fun remove(widget: HomeWidget) {
        runCatching { host.deleteAppWidgetId(widget.appWidgetId) }
        model.removeWidget(widget.appWidgetId)
    }

    fun cancelPending() = discardPending()

    private fun beginConfiguration(launch: (Intent) -> Unit) {
        val current = pending ?: return
        val configure = current.provider.configure
        if (configure == null) {
            finishPending()
            return
        }
        pending = current.copy(phase = Phase.CONFIGURE)
        launch(Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, current.appWidgetId)
        })
    }

    private fun finishPending() {
        val current = pending ?: return
        model.addWidget(current.appWidgetId, current.provider.provider.flattenToString())
        pending = null
    }

    private fun discardPending() {
        pending?.let { runCatching { host.deleteAppWidgetId(it.appWidgetId) } }
        pending = null
    }

    private fun unavailableView(message: String): View = TextView(activity).apply {
        text = message
        setTextColor(Color.rgb(41, 70, 90))
        textSize = 14f
        gravity = android.view.Gravity.CENTER
        setPadding(24, 24, 24, 24)
    }

    private data class PendingWidget(
        val appWidgetId: Int,
        val provider: AppWidgetProviderInfo,
        val phase: Phase
    )

    private enum class Phase { BIND, CONFIGURE }

    companion object {
        private const val HOST_ID = 0x454c
    }
}
