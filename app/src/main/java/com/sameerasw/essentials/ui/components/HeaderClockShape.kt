package com.sameerasw.essentials.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HeaderClockShape(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    visibleFraction: Float = 0.65f,
) {
    val alpha = progress.coerceIn(0f, 1f)
    if (alpha == 0f) return

    val spin by rememberInfiniteTransition(label = "clock_shape_spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart),
        label = "clock_shape_rotation",
    )
    val shape = MaterialShapes.Cookie12Sided.toShape()
    val color = MaterialTheme.colorScheme.primary
    val offset = -size * (1f - visibleFraction)

    Canvas(
        modifier =
            modifier
                .layout { measurable, _ ->
                    val sizePx = size.roundToPx()
                    val placeable = measurable.measure(Constraints.fixed(sizePx, sizePx))
                    layout(0, 0) {
                        placeable.place(offset.roundToPx(), offset.roundToPx())
                    }
                }
                .graphicsLayer {
                    this.alpha = alpha * 0.6f
                    rotationZ = -spin + (1f - alpha) * 140f
                    scaleX = 0.85f + 0.15f * alpha
                    scaleY = 0.85f + 0.15f * alpha
                },
    ) {
        drawOutline(
            outline = shape.createOutline(this.size, layoutDirection, this),
            color = color,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
