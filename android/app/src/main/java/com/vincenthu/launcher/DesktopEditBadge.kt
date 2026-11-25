package com.vincenthu.launcher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** A quiet visual badge with a separate, generous touch target. */
@Composable
internal fun DesktopEditBadge(
    visible: Boolean,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val motion = LocalLauncherMotionEnabled.current
    val interaction = remember { MutableInteractionSource() }
    AnimatedVisibility(
        visible = visible, modifier = modifier,
        enter = fadeIn(tween(if (motion) 160 else 0)) + scaleIn(tween(if (motion) 220 else 0, easing = LauncherMotion.easeOut), initialScale = .65f),
        exit = fadeOut(tween(if (motion) 100 else 0)) + scaleOut(tween(if (motion) 100 else 0), targetScale = .8f)
    ) {
        Box(
            Modifier.size(44.dp).semantics { contentDescription = description }
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(24.dp).pressFeedback(interaction)
                    .shadow(4.dp, CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFF7FAFC), Color(0xFFDCE5EB))), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = .85f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(10.dp)) {
                    drawLine(Color(0xFF425362), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2.dp.toPx(), StrokeCap.Round)
                }
            }
        }
    }
}
