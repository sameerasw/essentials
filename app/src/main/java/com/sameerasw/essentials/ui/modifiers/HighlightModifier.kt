package com.sameerasw.essentials.ui.modifiers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline

fun Modifier.highlight(
    enabled: Boolean,
    color: Color = Color.Unspecified,
    shape: Shape = RectangleShape
): Modifier = composed {
    if (!enabled) return@composed Modifier

    val highlightColor = if (color == Color.Unspecified) {
        MaterialTheme.colorScheme.primaryContainer
    } else color

    val alpha = remember { Animatable(0f) }

    LaunchedEffect(enabled) {
        if (enabled) {
            alpha.animateTo(
                targetValue = 0.7f,
                animationSpec = repeatable(
                    iterations = 3,
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            alpha.animateTo(0f) // Reset to 0 after pulses
        }
    }

    this.drawWithContent {
        drawContent()
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(
            outline = outline,
            color = highlightColor.copy(alpha = alpha.value)
        )
    }
}
