package com.vincenthu.launcher

import android.content.res.Configuration
import android.graphics.Rect
import android.provider.Settings

/** Controls only a HOME return from an app launched by this launcher. */
internal class HomeReturnAnimation(private val activity: MainActivity) {
    private data class Destination(val appId: String, val source: AppLaunchAnchor?, val orientation: Int)
    private data class Icon(val appIds: List<String>, val priority: Int)
    private val icons = mutableMapOf<AppLaunchAnchor, Icon>()
    private var destination: Destination? = null
    private var background = false
    private var pendingExit: Int? = null
    // Ready before HOME is dispatched: its window transition can start before
    // synchronous layout queries in onNewIntent have finished.
    private var preparedExit: Int? = null
    private var preparedAnchor: AppLaunchAnchor? = null
    private var pendingAnchor: AppLaunchAnchor? = null
    private var systemMotion = true
    var preservePageOnHome = false
        private set

    fun register(anchor: AppLaunchAnchor, appIds: List<String>, priority: Int) {
        icons[anchor] = Icon(appIds, priority)
    }

    fun unregister(anchor: AppLaunchAnchor) { icons.remove(anchor) }

    fun launched(appId: String, source: AppLaunchSource?) {
        destination = Destination(appId, source?.anchor, activity.resources.configuration.orientation)
        prepare()
    }

    fun onPause() {
        background = true
        prepare()
    }

    // The drawer closes and recent apps reorder after startActivity. Read their
    // final layout once the outgoing launch transition has completed.
    fun onStop() {
        prepare()
        // Require a fresh launcher draw on return. Otherwise the retained HOME
        // surface can let WMS start its transition before onNewIntent is run.
        if (destination != null && !activity.isInMultiWindowMode) activity.setVisible(false)
    }

    private fun prepare() {
        systemMotion = Settings.Global.getFloat(
            activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) > 0f
        preparedAnchor = null
        preparedExit = destination?.let(::resolveExit)
    }

    private fun resolveExit(target: Destination): Int? {
        if (activity.isInMultiWindowMode || activity.isInPictureInPictureMode ||
            target.orientation == Configuration.ORIENTATION_UNDEFINED ||
            target.orientation != activity.resources.configuration.orientation) return null
        val view = activity.window.decorView
        val origin = IntArray(2).also(view::getLocationOnScreen)
        val frame = Rect(origin[0], origin[1], origin[0] + view.width, origin[1] + view.height)
        fun bounds(anchor: AppLaunchAnchor): Rect? = anchor.source()?.screenBounds()?.takeIf {
            it.width() > 0 && it.height() > 0 && frame.contains(it)
        }
        // Prefer the same live icon. Drawer/folder children fall back to the
        // desktop, containing folder, then Dock, in that order.
        val same = target.source?.takeIf { icons.containsKey(it) }?.let { anchor ->
            bounds(anchor)?.let { anchor to it }
        }
        val icon = same ?: icons.entries.sortedBy { it.value.priority }
            .firstNotNullOfOrNull { (anchor, icon) ->
                if (target.appId in icon.appIds) bounds(anchor)?.let { anchor to it } else null
            }
        return icon?.let { (anchor, it) ->
            homeReturnAnimationIndex(it.exactCenterX(), it.exactCenterY(),
                frame.left.toFloat(), frame.top.toFloat(), frame.width().toFloat(), frame.height().toFloat())
                ?.let { index ->
                    preparedAnchor = anchor
                    HomeReturnAnimationResources.exitFor(index)
                }
        }
    }

    fun onHome(reduceMotion: Boolean) {
        val target = destination.takeIf { background }
        destination = null
        preservePageOnHome = target != null
        pendingExit = null
        pendingAnchor = null
        if (target == null) return
        pendingExit = if (reduceMotion || !systemMotion) 0 else preparedExit.takeIf {
            target.orientation == activity.resources.configuration.orientation &&
                !activity.isInMultiWindowMode && !activity.isInPictureInPictureMode
        }
        if (pendingExit != null && pendingExit != 0) pendingAnchor = preparedAnchor
        // HOME can become ready in system_server before layout queries finish.
        // Submit the already prepared resource before doing any UI work.
        applyPending()
        activity.setVisible(true)
        pendingAnchor?.let { it.landingSequence++ }
        pendingAnchor = null
    }

    fun onResume() {
        applyPending()
        activity.setVisible(true)
        pendingExit = null
        preparedExit = null
        preparedAnchor = null
        destination = null // BACK, recents and unlock must not reuse an old launch.
        background = false
    }

    @Suppress("DEPRECATION")
    private fun applyPending() {
        val exit = pendingExit ?: return
        activity.overridePendingTransition(if (exit == 0) 0 else R.anim.home_return_reveal, exit)
    }
}
