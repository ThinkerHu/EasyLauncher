package com.vincenthu.launcher

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.View
import android.view.MotionEvent
import android.view.GestureDetector
import android.view.ViewGroup
import kotlin.math.min

/** Fit provider content to grid bounds without distorting text or hit targets. */
internal class DesktopAppWidgetHostView(context: Context) : AppWidgetHostView(context) {
    var editing = false
    var onEdit: (() -> Unit)? = null
    private var longPressConsumed = false
    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent) = true
        override fun onLongPress(event: MotionEvent) {
            if (editing) return
            longPressConsumed = true
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
            super@DesktopAppWidgetHostView.dispatchTouchEvent(cancel)
            cancel.recycle()
            onEdit?.invoke()
        }
    })

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) longPressConsumed = false
        if (!editing) gestures.onTouchEvent(event)
        if (editing || longPressConsumed) return true
        return super.dispatchTouchEvent(event)
    }

    override fun prepareView(view: View) {
        super.prepareView(view)
        val mode = appWidgetInfo?.resizeMode ?: AppWidgetProviderInfo.RESIZE_NONE
        val params = view.layoutParams ?: return
        if (mode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0) params.width = ViewGroup.LayoutParams.MATCH_PARENT
        if (mode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0) params.height = ViewGroup.LayoutParams.MATCH_PARENT
        view.layoutParams = params
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val child = getChildAt(0) ?: return
        if (child.width <= 0 || child.height <= 0) return
        val availableWidth = (width - paddingLeft - paddingRight).coerceAtLeast(1)
        val availableHeight = (height - paddingTop - paddingBottom).coerceAtLeast(1)
        val scale = min(availableWidth.toFloat() / child.width, availableHeight.toFloat() / child.height)
        child.pivotX = 0f
        child.pivotY = 0f
        child.scaleX = scale
        child.scaleY = scale
        child.translationX = paddingLeft + (availableWidth - child.width * scale) / 2 - child.left
        child.translationY = paddingTop + (availableHeight - child.height * scale) / 2 - child.top
    }
}
