/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Radial Menus
 * File: ConnectorPreview.kt
 * Description: Renders visual mini previews of connector line styles for settings and picker sheets.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ConnectorStylePreview(
    style: MenuConnectorStyle,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier.size(width = 46.dp, height = 26.dp),
    ) {
        val w = size.width
        val h = size.height
        val cy = h / 2f
        val startX = 6.dp.toPx()
        val endX = w - 6.dp.toPx()
        val strokeWidth = 2.dp.toPx()

        // Draw start dot (representing ball edge)
        drawCircle(
            color = color.copy(alpha = 0.5f),
            radius = 3.dp.toPx(),
            center = Offset(startX, cy),
        )

        // Draw end dot (representing shortcut item)
        drawCircle(
            color = color,
            radius = 3.5.dp.toPx(),
            center = Offset(endX, cy),
        )

        val spanX = endX - startX

        when (style) {
            MenuConnectorStyle.NONE -> {
                // Dim dashed strike through
                drawLine(
                    color = color.copy(alpha = 0.25f),
                    start = Offset(startX + 4.dp.toPx(), cy),
                    end = Offset(endX - 4.dp.toPx(), cy),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
                )
            }

            MenuConnectorStyle.LINE -> {
                drawLine(
                    color = color,
                    start = Offset(startX + 4.dp.toPx(), cy),
                    end = Offset(endX - 4.dp.toPx(), cy),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }

            MenuConnectorStyle.DOTTED -> {
                val dots = 5
                val step = (spanX - 8.dp.toPx()) / (dots - 1)
                for (i in 1 until dots - 1) {
                    drawCircle(
                        color = color,
                        radius = 1.5.dp.toPx(),
                        center = Offset(startX + 4.dp.toPx() + i * step, cy),
                    )
                }
            }

            MenuConnectorStyle.ARC -> {
                val path = Path().apply {
                    moveTo(startX + 3.dp.toPx(), cy)
                    quadraticTo(
                        startX + spanX / 2f,
                        cy - 9.dp.toPx(),
                        endX - 3.dp.toPx(),
                        cy,
                    )
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            MenuConnectorStyle.LINE_RING -> {
                // Line + mini vertical ring cap
                drawLine(
                    color = color,
                    start = Offset(startX + 4.dp.toPx(), cy),
                    end = Offset(endX - 4.dp.toPx(), cy),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color.copy(alpha = 0.6f),
                    start = Offset(endX - 4.dp.toPx(), cy - 7.dp.toPx()),
                    end = Offset(endX - 4.dp.toPx(), cy + 7.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            MenuConnectorStyle.DOT_RING -> {
                val dots = 5
                val step = (spanX - 8.dp.toPx()) / (dots - 1)
                for (i in 1 until dots - 1) {
                    drawCircle(
                        color = color,
                        radius = 1.5.dp.toPx(),
                        center = Offset(startX + 4.dp.toPx() + i * step, cy),
                    )
                }
                drawLine(
                    color = color.copy(alpha = 0.6f),
                    start = Offset(endX - 4.dp.toPx(), cy - 7.dp.toPx()),
                    end = Offset(endX - 4.dp.toPx(), cy + 7.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            MenuConnectorStyle.CURVE -> {
                val path = Path().apply {
                    moveTo(startX + 3.dp.toPx(), cy)
                    cubicTo(
                        startX + spanX * 0.35f,
                        cy - 8.dp.toPx(),
                        startX + spanX * 0.65f,
                        cy + 8.dp.toPx(),
                        endX - 3.dp.toPx(),
                        cy,
                    )
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            MenuConnectorStyle.FADE -> {
                val brush = Brush.horizontalGradient(
                    colors = listOf(color, color.copy(alpha = 0.15f)),
                    startX = startX + 4.dp.toPx(),
                    endX = endX - 4.dp.toPx(),
                )
                drawLine(
                    brush = brush,
                    start = Offset(startX + 4.dp.toPx(), cy),
                    end = Offset(endX - 4.dp.toPx(), cy),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }

            MenuConnectorStyle.DASH -> {
                drawLine(
                    color = color,
                    start = Offset(startX + 4.dp.toPx(), cy),
                    end = Offset(endX - 4.dp.toPx(), cy),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                )
            }

            MenuConnectorStyle.TAPER -> {
                val path = Path().apply {
                    moveTo(startX + 4.dp.toPx(), cy - 2.5.dp.toPx())
                    lineTo(endX - 4.dp.toPx(), cy - 0.6.dp.toPx())
                    lineTo(endX - 4.dp.toPx(), cy + 0.6.dp.toPx())
                    lineTo(startX + 4.dp.toPx(), cy + 2.5.dp.toPx())
                    close()
                }
                drawPath(path = path, color = color)
            }

            MenuConnectorStyle.WAVE -> {
                val path = Path().apply {
                    moveTo(startX + 4.dp.toPx(), cy)
                    val segments = 24
                    val startP = startX + 4.dp.toPx()
                    val totalW = (endX - 4.dp.toPx()) - startP
                    for (i in 1..segments) {
                        val px = startP + (i.toFloat() / segments) * totalW
                        val py = cy + sin((i.toFloat() / segments) * 4 * PI).toFloat() * 4.dp.toPx()
                        lineTo(px, py)
                    }
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            MenuConnectorStyle.BUBBLE -> {
                val count = 4
                val step = (spanX - 8.dp.toPx()) / (count + 1)
                for (i in 1..count) {
                    val r = (1f + i * 0.7f).dp.toPx()
                    drawCircle(
                        color = color,
                        radius = r,
                        center = Offset(startX + 4.dp.toPx() + i * step, cy),
                    )
                }
            }

            MenuConnectorStyle.ZIGZAG -> {
                val path = Path().apply {
                    moveTo(startX + 4.dp.toPx(), cy)
                    val points = listOf(
                        0.2f to -5.dp.toPx(),
                        0.4f to 5.dp.toPx(),
                        0.6f to -5.dp.toPx(),
                        0.8f to 5.dp.toPx(),
                    )
                    points.forEach { (pct, dy) ->
                        lineTo(startX + 4.dp.toPx() + pct * (spanX - 8.dp.toPx()), cy + dy)
                    }
                    lineTo(endX - 4.dp.toPx(), cy)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            MenuConnectorStyle.DOUBLE -> {
                drawLine(
                    color = color,
                    start = Offset(startX + 4.dp.toPx(), cy - 2.dp.toPx()),
                    end = Offset(endX - 4.dp.toPx(), cy - 2.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(startX + 4.dp.toPx(), cy + 2.dp.toPx()),
                    end = Offset(endX - 4.dp.toPx(), cy + 2.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            MenuConnectorStyle.GLOW -> {
                // Multi-layered glow
                drawLine(
                    color = color.copy(alpha = 0.2f),
                    start = Offset(startX + 4.dp.toPx(), cy),
                    end = Offset(endX - 4.dp.toPx(), cy),
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color.copy(alpha = 0.45f),
                    start = Offset(startX + 4.dp.toPx(), cy),
                    end = Offset(endX - 4.dp.toPx(), cy),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(startX + 4.dp.toPx(), cy),
                    end = Offset(endX - 4.dp.toPx(), cy),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
