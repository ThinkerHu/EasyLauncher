package com.vincenthu.launcher

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** EMUI intercepts HOME's outermost edge. Reserve the adjacent strip for in-app BACK. */
@Composable
internal fun BoxScope.GroupBackEdges(onBack: () -> Unit) {
    val currentBack by rememberUpdatedState(onBack)
    val halfHeight = with(LocalDensity.current) { 100.dp.toPx() }
    listOf(1 to Alignment.CenterStart, -1 to Alignment.CenterEnd).forEach { (direction, alignment) ->
        Box(Modifier.align(alignment).width(28.dp).fillMaxHeight()
            .systemGestureExclusion { coordinates ->
                val half = coordinates.size.height / 2f
                Rect(0f, half - halfHeight, coordinates.size.width.toFloat(), half + halfHeight)
            }.pointerInput(direction) {
                var travel = 0f
                var handled = false
                detectHorizontalDragGestures(
                    onDragStart = { travel = 0f; handled = false },
                    onHorizontalDrag = { change, amount ->
                        travel += amount * direction
                        change.consume()
                        if (!handled && travel >= 64.dp.toPx()) {
                            handled = true
                            currentBack()
                        }
                    }
                )
            })
    }
}
