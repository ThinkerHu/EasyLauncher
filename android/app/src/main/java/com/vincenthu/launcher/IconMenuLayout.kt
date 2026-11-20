package com.vincenthu.launcher

internal data class IconMenuPlacement(val left: Int, val top: Int, val maxHeight: Int, val below: Boolean)

/** All coordinates use the icon's window; never place a menu over its source icon. */
internal fun placeIconMenu(
    windowWidth: Int, windowHeight: Int,
    iconLeft: Int, iconTop: Int, iconRight: Int, iconBottom: Int,
    menuWidth: Int, menuHeight: Int, gap: Int, margin: Int,
    insetTop: Int = 0, insetBottom: Int = 0
): IconMenuPlacement {
    val topEdge = insetTop + margin
    val bottomEdge = (windowHeight - insetBottom - margin).coerceAtLeast(topEdge)
    val above = (iconTop - gap - topEdge).coerceAtLeast(0)
    val below = (bottomEdge - iconBottom - gap).coerceAtLeast(0)
    val opensBelow = below >= menuHeight || (above < menuHeight && below >= above)
    val available = if (opensBelow) below else above
    val height = menuHeight.coerceAtMost(available)
    val preferredLeft = if (iconLeft + menuWidth <= windowWidth - margin) iconLeft else iconRight - menuWidth
    val left = preferredLeft.coerceIn(margin, (windowWidth - margin - menuWidth).coerceAtLeast(margin))
    val top = if (opensBelow) iconBottom + gap else iconTop - gap - height
    return IconMenuPlacement(left, top.coerceIn(topEdge, bottomEdge), available, opensBelow)
}
