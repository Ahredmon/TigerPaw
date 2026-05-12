package com.tigerpaw.launcher.feature.ai

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tigerpaw.launcher.feature.ai.R

/**
 * Animated tiger-orb widget.
 *
 * - [isThinking]  → a spinning arc orbits the orb
 * - [isMicActive] → the orb pulses (scale breathes)
 * - Tap to toggle mic
 */
@Composable
fun TigerOrb(
    isThinking: Boolean,
    isMicActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    // ── Spinning ring (visible while thinking) ────────────────────────────
    val spinAngle by infiniteTransition.animateThinkingRotation(isThinking)

    // ── Pulse scale (breathing while mic is active) ───────────────────────
    val pulseScale by infiniteTransition.animatePulseScale(isMicActive)

    // ── Ring colour intensity (alpha 0 when idle) ─────────────────────────
    val ringAlpha = if (isThinking) 1f else 0f

    val ringColor = Color(0xFFFF8C40) // amber-orange accent

    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale)
            .drawBehind {
                // Outer glow ring
                drawSpinningArc(
                    angle = spinAngle,
                    alpha = ringAlpha,
                    color = ringColor,
                    strokeWidth = 5.dp.toPx(),
                    radiusFraction = 0.52f,
                )
                // Softer secondary arc offset 120°
                drawSpinningArc(
                    angle = spinAngle + 120f,
                    alpha = ringAlpha * 0.4f,
                    color = ringColor,
                    strokeWidth = 3.dp.toPx(),
                    radiusFraction = 0.52f,
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_tiger_claw),
            contentDescription = if (isMicActive) "Mute microphone" else "Unmute microphone",
            modifier = Modifier.size(size),
        )
    }
}

// ── Drawing helpers ───────────────────────────────────────────────────────────

private fun DrawScope.drawSpinningArc(
    angle: Float,
    alpha: Float,
    color: Color,
    strokeWidth: Float,
    radiusFraction: Float,
) {
    if (alpha <= 0f) return
    val radius = size.minDimension * radiusFraction
    val diameter = radius * 2f
    val topLeft = androidx.compose.ui.geometry.Offset(
        x = center.x - radius,
        y = center.y - radius,
    )
    rotate(angle, center) {
        drawArc(
            color = color.copy(alpha = alpha),
            startAngle = -30f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = topLeft,
            size = androidx.compose.ui.geometry.Size(diameter, diameter),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

// ── InfiniteTransition extensions ────────────────────────────────────────────

@Composable
private fun InfiniteTransition.animateThinkingRotation(active: Boolean): androidx.compose.runtime.State<Float> =
    animateFloat(
        initialValue = 0f,
        targetValue = if (active) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin",
    )

@Composable
private fun InfiniteTransition.animatePulseScale(active: Boolean): androidx.compose.runtime.State<Float> =
    animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                1f at 0 using FastOutSlowInEasing
                1.08f at 450 using FastOutSlowInEasing
                1f at 900
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse",
    )
