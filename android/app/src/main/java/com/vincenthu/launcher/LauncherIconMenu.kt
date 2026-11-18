package com.vincenthu.launcher

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.draw.shadow

internal data class IconMenuAction(
    val label: String,
    val onClick: () -> Unit = {},
    val destructive: Boolean = false,
    val children: List<IconMenuAction> = emptyList()
)
internal data class IconMenuContent(val title: String, val actions: List<IconMenuAction>)

internal val LocalIconMenus = staticCompositionLocalOf<(String) -> IconMenuContent> {
    { IconMenuContent("", emptyList()) }
}

/** The popup belongs to the icon's window, including icons inside a dialog. */
@Composable
internal fun IconMenuAnchor(
    id: String,
    content: @Composable (openMenu: () -> Unit) -> Unit
) {
    var expanded by remember(id) { mutableStateOf(false) }
    var submenu by remember(id) { mutableStateOf<IconMenuContent?>(null) }
    val menuFor = LocalIconMenus.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val gapPx = with(density) { 10.dp.roundToPx() }
    var anchorBounds by remember(id) { mutableStateOf<Rect?>(null) }
    val windowHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val availableSpacePx = anchorBounds?.let { bounds ->
        maxOf(bounds.top, windowHeightPx - bounds.bottom) - gapPx
    }?.coerceAtLeast(0f)
    val maxMenuHeight = with(density) { (availableSpacePx?.toDp() ?: 360.dp).coerceAtMost(360.dp) }
    val positionProvider = remember(gapPx) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val left = anchorBounds.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
                val above = anchorBounds.top - popupContentSize.height - gapPx
                val below = anchorBounds.bottom + gapPx
                val top = when {
                    above >= 0 -> above
                    below + popupContentSize.height <= windowSize.height -> below
                    windowSize.height - anchorBounds.bottom >= anchorBounds.top -> below.coerceAtMost(windowSize.height - popupContentSize.height)
                    else -> above.coerceAtLeast(0)
                }
                return IntOffset(left, top)
            }
        }
    }
    Box {
        Box(Modifier.onGloballyPositioned { anchorBounds = it.boundsInWindow() }) {
            content { submenu = null; expanded = true }
        }
        if (expanded) MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(18.dp))) {
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
                content = {
                    Surface(
                        modifier = Modifier.width(228.dp).heightIn(max = maxMenuHeight)
                            .shadow(12.dp, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFF4F8FA),
                        tonalElevation = 0.dp
                    ) {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            val menu = submenu ?: menuFor(id)
                            Text(
                                menu.title, color = Color(0xFF66818F), fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                            HorizontalDivider(color = Color(0xFF29465A).copy(alpha = .08f))
                            if (submenu != null) MenuRow("‹ 返回", onClick = { submenu = null })
                            menu.actions.forEach { action ->
                                MenuRow(
                                    label = action.label,
                                    destructive = action.destructive,
                                    trailing = if (action.children.isNotEmpty()) "›" else null,
                                    onClick = {
                                        if (action.children.isNotEmpty()) submenu = IconMenuContent(action.label, action.children)
                                        else { expanded = false; action.onClick() }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun MenuRow(
    label: String,
    destructive: Boolean = false,
    trailing: String? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp,
            color = if (destructive) Color(0xFFB4464F) else Color(0xFF29465A))
        if (trailing != null) Text(trailing, fontSize = 18.sp, color = Color(0xFF66818F))
    }
}
