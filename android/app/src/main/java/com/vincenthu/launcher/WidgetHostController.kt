package com.vincenthu.launcher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity

internal enum class WidgetBindResult { Pending, Added, Cancelled }

/** Owns the system widget host and the bind/configure activity flow. */
class WidgetHostController(
    private val activity: ComponentActivity,
    private val model: LauncherModel,
    savedState: Bundle? = null
) {
    private val manager = AppWidgetManager.getInstance(activity)
    private val host = object : AppWidgetHost(activity, HOST_ID) {
        override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo): AppWidgetHostView =
            DesktopAppWidgetHostView(context)
    }
    private var pending: PendingWidget? = savedState?.let { state ->
        @Suppress("DEPRECATION")
        val provider = state.getParcelable<AppWidgetProviderInfo>("provider")
        val phase = runCatching { Phase.valueOf(state.getString("phase").orEmpty()) }.getOrNull()
        val id = state.getInt("id", -1)
        if (provider != null && phase != null && id > 0) PendingWidget(id, provider, phase) else null
    }

    fun start() = host.startListening()
    fun stop() = host.stopListening()

    fun providers(): List<AppWidgetProviderInfo> = manager.installedProviders
        .filter { it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 }
        .distinctBy { it.provider.flattenToString() }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.loadLabel(activity.packageManager).toString() })

    internal fun beginBind(provider: AppWidgetProviderInfo, launch: (Intent) -> Unit): WidgetBindResult {
        if (pending != null) return WidgetBindResult.Pending
        val appWidgetId = host.allocateAppWidgetId()
        pending = PendingWidget(appWidgetId, provider, Phase.BIND)
        val bound = runCatching {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, provider.provider, homeOptions())
        }.getOrDefault(false)
        if (bound) return beginConfiguration(launch)
        launchPending(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, homeOptions())
        }, launch)
        return WidgetBindResult.Pending
    }

    internal fun handleActivityResult(resultCode: Int, data: Intent?, launch: (Intent) -> Unit): WidgetBindResult {
        val current = pending ?: return WidgetBindResult.Cancelled
        if (resultCode != Activity.RESULT_OK) {
            discardPending()
            return WidgetBindResult.Cancelled
        }
        if (manager.getAppWidgetInfo(current.appWidgetId) == null) {
            discardPending()
            return WidgetBindResult.Cancelled
        }
        return when (current.phase) {
            Phase.BIND -> beginConfiguration(launch)
            Phase.CONFIGURE -> finishPending()
        }
    }

    fun createView(widget: HomeWidget): View {
        val info = manager.getAppWidgetInfo(widget.appWidgetId)
        if (info == null) return unavailableView("小组件不可用")
        return runCatching { host.createView(activity, widget.appWidgetId, info) }
            .getOrElse { unavailableView("小组件暂时无法显示") }
    }

    fun info(widget: HomeWidget): AppWidgetProviderInfo? = manager.getAppWidgetInfo(widget.appWidgetId)

    // These theme containers read Huawei launcher's private item metadata;
    // their initial layout can render even though no selected theme is loaded.
    fun compatibilityHint(provider: ComponentName?): String? =
        if (provider?.packageName == "com.huawei.android.thememanager" &&
            provider.className in setOf(
                "com.huawei.android.thememanager.widget.provider.HuaweiWidgetCustomProvider",
                "com.huawei.android.thememanager.widget.provider.HuaweiPetWidgetCustomProvider"
            )) "华为主题专属组件，在第三方桌面可能仅显示占位内容" else null

    fun updateSize(view: View, widthDp: Int, heightDp: Int) {
        if (view !is AppWidgetHostView) return
        val info = view.appWidgetInfo ?: return
        val padding = AppWidgetHostView.getDefaultPaddingForWidget(activity, info.provider, null)
        val density = activity.resources.displayMetrics.density
        val contentWidth = (widthDp - (padding.left + padding.right) / density).toInt().coerceAtLeast(1)
        val contentHeight = (heightDp - (padding.top + padding.bottom) / density).toInt().coerceAtLeast(1)
        val options = manager.getAppWidgetOptions(view.appWidgetId)
        if (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, -1) == contentWidth &&
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, -1) == contentWidth &&
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, -1) == contentHeight &&
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, -1) == contentHeight) return
        view.updateAppWidgetSize(homeOptions(), widthDp, heightDp, widthDp, heightDp)
    }

    fun remove(widget: HomeWidget) {
        runCatching { host.deleteAppWidgetId(widget.appWidgetId) }
        model.removeWidget(widget.appWidgetId)
    }

    fun cancelPending() = discardPending()

    fun saveState(): Bundle? = pending?.let { current -> Bundle().apply {
        putInt("id", current.appWidgetId)
        putParcelable("provider", current.provider)
        putString("phase", current.phase.name)
    } }

    private fun beginConfiguration(launch: (Intent) -> Unit): WidgetBindResult {
        val current = pending ?: return WidgetBindResult.Cancelled
        val configure = current.provider.configure
        if (configure == null) {
            return finishPending()
        }
        pending = current.copy(phase = Phase.CONFIGURE)
        launchPending(Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            component = configure
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, current.appWidgetId)
        }, launch)
        return WidgetBindResult.Pending
    }

    private fun launchPending(intent: Intent, launch: (Intent) -> Unit) {
        try {
            launch(intent)
        } catch (error: RuntimeException) {
            discardPending()
            throw error
        }
    }

    private fun homeOptions() = Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
    }

    private fun finishPending(): WidgetBindResult {
        val current = pending ?: return WidgetBindResult.Cancelled
        model.addWidget(current.appWidgetId, current.provider.provider.flattenToString())
        pending = null
        return WidgetBindResult.Added
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
