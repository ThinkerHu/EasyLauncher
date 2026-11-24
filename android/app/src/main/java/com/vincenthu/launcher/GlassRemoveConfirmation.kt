package com.vincenthu.launcher

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.core.view.WindowCompat
import android.view.WindowManager

/** Full-window glass prompt for removing a home-screen shortcut. */
@Composable
internal fun GlassRemoveConfirmation(
    app: LauncherApp,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val motion = LocalLauncherMotionEnabled.current
    val visibility = remember { MutableTransitionState(false) }
    var backdrop by remember { mutableStateOf<Bitmap?>(null) }
    val view = LocalView.current
    val density = LocalDensity.current
    val host = view.context.findActivity()?.window
    val hostWidth = host?.decorView?.width ?: view.width
    val hostHeight = host?.decorView?.height ?: view.height
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val cancel by rememberUpdatedState(onCancel)
    val confirm by rememberUpdatedState(onConfirm)
    var closing by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }
    fun dismiss(accept: Boolean = false) {
        if (closing) return
        confirmed = accept
        closing = true
        visibility.targetState = false
    }

    LaunchedEffect(app.id) {
        backdrop = host?.let { captureIconMenuBackdrop(it) }
        if (!closing) visibility.targetState = true
    }
    DisposableEffect(backdrop) {
        val texture = backdrop
        onDispose { texture?.takeUnless { it.isRecycled }?.recycle() }
    }
    LaunchedEffect(closing, visibility.isIdle, visibility.currentState) {
        if (closing && visibility.isIdle && !visibility.currentState) {
            if (confirmed) confirm() else cancel()
        }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) cancel()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val transition = rememberTransition(visibility, label = "remove-confirmation")
    val progress by transition.animateFloat(
        transitionSpec = { tween(if (motion) if (targetState) 240 else 170 else 0, easing = LauncherMotion.easeOut) },
        label = "glass-confirm-progress"
    ) { if (it) 1f else 0f }
    Dialog(onDismissRequest = { dismiss() }, properties = DialogProperties(usePlatformDefaultWidth = true, decorFitsSystemWindows = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                it.setLayout(hostWidth, hostHeight)
                it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                it.setWindowAnimations(0)
                @Suppress("DEPRECATION")
                run {
                    it.statusBarColor = android.graphics.Color.TRANSPARENT
                    it.navigationBarColor = android.graphics.Color.TRANSPARENT
                }
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    it.isStatusBarContrastEnforced = false
                    it.isNavigationBarContrastEnforced = false
                }
            }
        }
        BackHandler { dismiss() }
        BoxWithConstraints(
            Modifier.requiredSize(with(density) { hostWidth.toDp() }, with(density) { hostHeight.toDp() })
                .semantics { dialog(); paneTitle = "确认从桌面移除" }
                ,
            contentAlignment = Alignment.Center
        ) {
            backdrop?.let { bitmap ->
                Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize().graphicsLayer { alpha = progress }, contentScale = ContentScale.FillBounds)
            }
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = progress }
                .background(Color(0x77081B2B)).clickable(onClick = { dismiss() }))

            val width = minOf(maxWidth - 40.dp, 360.dp)
            Column(
                Modifier.width(width).safeDrawingPadding().heightIn(max = maxHeight - 40.dp).graphicsLayer {
                    alpha = progress
                    val scale = if (motion) .92f + .08f * progress else 1f
                    scaleX = scale
                    scaleY = scale
                    translationY = if (motion) 14.dp.toPx() * (1f - progress) else 0f
                }.shadow(30.dp, RoundedCornerShape(30.dp), ambientColor = Color.Black.copy(alpha = .24f))
                    .clip(RoundedCornerShape(30.dp))
                    .background(Brush.linearGradient(listOf(
                        Color.White.copy(alpha = .78f),
                        Color(0xFFE9F4FA).copy(alpha = .72f),
                        Color(0xFFD9E9F2).copy(alpha = .78f)
                    )))
                    .border(1.dp, Brush.linearGradient(listOf(
                        Color.White.copy(alpha = .96f), Color.White.copy(alpha = .42f), Color.White.copy(alpha = .78f)
                    )), RoundedCornerShape(30.dp))
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(horizontal = 26.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(54.dp).background(Color(0xFF526E80).copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                    Image(app.icon.asImageBitmap(), null, Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)))
                }
                Text(
                    "从桌面移除“${app.label}”？",
                    Modifier.fillMaxWidth().padding(top = 18.dp),
                    color = Color(0xFF203A4A), fontSize = 19.sp, lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
                )
                Text(
                    "应用仍保留在设备上，可从应用库再次添加到桌面。",
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    color = Color(0xFF526B79), fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center
                )
                Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassActionButton("取消", destructive = false, modifier = Modifier.weight(1f), enabled = !closing, onClick = { dismiss() })
                    GlassActionButton("确认", destructive = true, modifier = Modifier.weight(1f), enabled = !closing, onClick = { dismiss(true) })
                }
            }
        }
    }
}

@Composable
private fun GlassActionButton(label: String, destructive: Boolean, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val tint = if (destructive) Color(0xFFB3404D) else Color(0xFF294B5E)
    Box(
        modifier.pressFeedback(interaction).heightIn(min = 52.dp).clip(shape)
            .background(if (destructive) Color(0xFFB54552).copy(alpha = .88f) else Color.White.copy(alpha = .48f))
            .border(1.dp, if (destructive) Color.White.copy(alpha = .30f) else Color.White.copy(alpha = .78f), shape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (destructive) Color.White else tint, fontSize = 15.sp,
            fontWeight = if (destructive) FontWeight.SemiBold else FontWeight.Medium)
    }
}
