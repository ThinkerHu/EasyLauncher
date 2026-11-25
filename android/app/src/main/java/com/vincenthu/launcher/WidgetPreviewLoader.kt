package com.vincenthu.launcher

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache

internal data class WidgetCatalogSnapshot(
    val items: List<WidgetCatalogItem>,
    val providers: Map<String, AppWidgetProviderInfo>
)

internal sealed interface WidgetPreview {
    data class Artwork(val bitmap: Bitmap) : WidgetPreview
    data class Layout(val packageName: String, val layoutId: Int) : WidgetPreview
    data object Unavailable : WidgetPreview
}

/** Provider resources only: no binding, updates or configuration during preview. */
internal class WidgetPreviewLoader(context: Context) {
    private val context = context.applicationContext
    private val density = context.resources.displayMetrics
    private val previews = object : LruCache<String, WidgetPreview>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: WidgetPreview): Int =
            (value as? WidgetPreview.Artwork)?.bitmap?.allocationByteCount ?: 1
    }
    private val icons = object : LruCache<String, Bitmap>(2 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    fun catalog(host: WidgetHostController): WidgetCatalogSnapshot {
        val pm = context.packageManager
        val providers = host.providers()
        val appNames = providers.map { it.provider.packageName }.distinct().associateWith { pkg ->
            runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
        }
        val entries = providers.map { info ->
            WidgetCatalogItem(
                key = info.provider.flattenToString(), packageName = info.provider.packageName,
                appLabel = appNames.getValue(info.provider.packageName),
                label = runCatching { info.loadLabel(pm)?.toString() }.getOrNull().orEmpty().ifBlank { appNames.getValue(info.provider.packageName) },
                widthDp = (info.minWidth / density.density).toInt().coerceAtLeast(1),
                heightDp = (info.minHeight / density.density).toInt().coerceAtLeast(1),
                resizable = info.resizeMode != AppWidgetProviderInfo.RESIZE_NONE,
                configurable = info.configure != null,
                compatibilityHint = host.compatibilityHint(info.provider)
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appLabel + it.label })
        return WidgetCatalogSnapshot(entries, providers.associateBy { it.provider.flattenToString() })
    }

    fun icon(packageName: String): Bitmap? {
        icons.get(packageName)?.let { return it }
        return runCatching { context.packageManager.getApplicationIcon(packageName).rasterize(128) }
            .getOrNull()?.also { icons.put(packageName, it) }
    }

    fun preview(info: AppWidgetProviderInfo): WidgetPreview {
        val key = info.provider.flattenToString()
        previews.get(key)?.let { return it }
        val image = runCatching { info.loadPreviewImage(context, density.densityDpi)?.rasterize(1280) }.getOrNull()
        val result = when {
            image != null -> WidgetPreview.Artwork(image)
            Build.VERSION.SDK_INT >= 31 && info.previewLayout != 0 ->
                WidgetPreview.Layout(info.provider.packageName, info.previewLayout)
            else -> WidgetPreview.Unavailable
        }
        previews.put(key, result)
        return result
    }

    private fun Drawable.rasterize(maxSide: Int): Bitmap {
        val size = WidgetCatalog.previewSize(intrinsicWidth, intrinsicHeight, maxSide)
        return Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888).also { bitmap ->
            setBounds(0, 0, size.width, size.height)
            draw(Canvas(bitmap))
        }
    }
}
