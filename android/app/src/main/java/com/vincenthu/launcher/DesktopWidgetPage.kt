package com.vincenthu.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** Content-only desktop cards. Labels and explanations belong in the picker. */
@Composable
internal fun DesktopWidgetPage(
    page: WidgetGridPage,
    widgets: List<HomeWidget>,
    host: WidgetHostController,
    editing: Boolean,
    onEdit: () -> Unit,
    onRemove: (HomeWidget) -> Unit
) {
    val widgetMap = remember(widgets) { widgets.associateBy { it.appWidgetId } }
    BoxWithConstraints(Modifier.fillMaxSize().semantics { contentDescription = "桌面小组件" }) {
        val placements = remember(page, maxWidth, maxHeight) {
            WidgetGridLayout.bounds(
                page, maxWidth.value, maxHeight.value,
                landscape = maxWidth > maxHeight
            )
        }
        page.items.forEach { placement ->
            val widget = widgetMap[placement.appWidgetId] ?: return@forEach
            val bounds = placements.firstOrNull { it.appWidgetId == widget.appWidgetId } ?: return@forEach
            key(widget.appWidgetId) {
                val slotWidth = bounds.width.dp
                val slotHeight = bounds.height.dp
                Box(
                    Modifier.offset(
                        x = bounds.x.dp,
                        y = bounds.y.dp
                    ).size(slotWidth, slotHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { host.createView(widget) },
                            update = {
                                host.updateSize(it, bounds.width.toInt(), bounds.height.toInt())
                                (it as? DesktopAppWidgetHostView)?.let { view ->
                                    view.editing = editing
                                    view.onEdit = onEdit
                                }
                            },
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp))
                        )
                        if (editing) {
                            // Keep provider taps inert while launcher edit controls are visible.
                            Box(Modifier.matchParentSize().background(Color.White.copy(alpha = .04f), RoundedCornerShape(22.dp))
                                .pointerInput(Unit) { detectTapGestures { } })
                        }
                        DesktopEditBadge(
                            visible = editing,
                            description = "移除桌面小组件",
                            modifier = Modifier.align(Alignment.TopStart).offset((-12).dp, (-12).dp),
                            onClick = { onRemove(widget) }
                        )
                    }
                }
            }
        }
    }
}
