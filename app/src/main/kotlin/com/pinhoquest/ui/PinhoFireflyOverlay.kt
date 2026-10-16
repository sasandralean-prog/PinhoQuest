package com.pinhoquest.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Low-cost decorative fireflies shared by Home, Garden and Profile.
 *
 * It is only composed on those visible destinations, so the animation stops when
 * the player opens Settings or leaves the main navigation. The overlay contains
 * no interactive semantics and intentionally uses a small fixed particle count.
 */
@Composable
fun PinhoFireflyOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pinho-fireflies")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "firefly-flight",
    )

    Canvas(modifier = modifier.clearAndSetSemantics {}) {
        val count = 7
        val warmGold = Color(0xFFFFD86B)
        val glowRadius = 7.dp.toPx()
        val coreRadius = 1.65.dp.toPx()
        repeat(count) { index ->
            val offset = index.toFloat() / count
            val t = (phase + offset) % 1f
            val seed = index * 1.71f
            val xBase = size.width * ((index * 37 % 97) / 97f)
            val sway = sin((t * 2.0 * PI + seed).toFloat()).toFloat() * (10.dp.toPx() + index % 3 * 3.dp.toPx())
            val y = size.height * (1f - t)
            val pulse = (0.28f + 0.72f * sin((t * PI).toFloat()).toFloat()).coerceIn(0.16f, 1f)
            val center = Offset((xBase + sway).coerceIn(2f, size.width - 2f), y)

            drawCircle(
                color = warmGold.copy(alpha = 0.10f * pulse),
                radius = glowRadius,
                center = center,
            )
            drawCircle(
                color = warmGold.copy(alpha = 0.28f * pulse),
                radius = glowRadius * 0.48f,
                center = center,
            )
            drawCircle(
                color = Color(0xFFFFF5C7).copy(alpha = 0.92f * pulse),
                radius = coreRadius,
                center = center,
            )
        }
    }
}
