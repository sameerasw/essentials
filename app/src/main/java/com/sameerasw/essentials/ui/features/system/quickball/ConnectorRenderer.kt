/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Radial Menus
 * File: ConnectorRenderer.kt
 * Description: Renders visual connecting lines, dots, waves, or curves between the Quick Ball and radial shortcuts.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

class ConnectorRenderer(
    val style: MenuConnectorStyle,
    color: Int,
    private val strokeWidth: Float,
) {
    companion object {
        const val INSET_DP = 8f
        private const val DOT_SIZE_RATIO = 1.75f
        private const val DOT_SPACING_RATIO = 5f
        private const val DASH_LENGTH_RATIO = 3f
        private const val DASH_GAP_RATIO = 4f
        private const val CURVE_BEND = 0.22f
        private const val TAPER_START_RATIO = 3f
        private const val TAPER_END_RATIO = 0.4f
        private const val WAVE_AMPLITUDE_RATIO = 1f
        private const val WAVE_LENGTH_RATIO = WAVE_AMPLITUDE_RATIO * 8f
        private const val WAVE_SEGMENTS_PER_HALF = 8
        private const val ZIGZAG_AMPLITUDE_RATIO = 1.3f
        private const val ZIGZAG_LENGTH_RATIO = ZIGZAG_AMPLITUDE_RATIO * 3.4f
        private const val DOUBLE_RAIL_RATIO = 0.7f
        private const val DOUBLE_OFFSET_RATIO = 1.1f
        private val GLOW_LAYERS = listOf(8f to 0.08f, 5f to 0.14f, 2.6f to 0.3f)
        private const val FADE_END_ALPHA = 0.2f
        private const val BUBBLE_START_RATIO = 0.5f
        private const val BUBBLE_END_RATIO = 1.6f
    }

    val hasRing = style == MenuConnectorStyle.LINE_RING || style == MenuConnectorStyle.DOT_RING
    private val isDotted = style == MenuConnectorStyle.DOTTED || style == MenuConnectorStyle.DOT_RING
    private val baseAlpha = Color.alpha(color)
    private val opaqueColor = color or 0xFF000000.toInt()
    private val fadeEndColor = (color and 0x00FFFFFF) or ((FADE_END_ALPHA * 255).toInt() shl 24)
    private val dotRadius = strokeWidth * DOT_SIZE_RATIO / 2f
    private val dotSpacing = strokeWidth * DOT_SPACING_RATIO

    private val path = Path()
    private val bounds = RectF()
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.strokeWidth = this@ConnectorRenderer.strokeWidth
            this.strokeCap = Paint.Cap.ROUND
            this.strokeJoin = Paint.Join.ROUND
        }

    fun drawRing(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Double,
        sweepAngle: Double,
        opacity: Float,
    ) {
        if (opacity <= 0f) return
        setOpacity(opacity)

        if (isDotted) {
            paint.style = Paint.Style.FILL
            val steps = max(1, (radius * sweepAngle / dotSpacing).roundToInt())
            for (step in 0..steps) {
                val angle = startAngle + sweepAngle * step / steps
                canvas.drawCircle(
                    centerX + (cos(angle) * radius).toFloat(),
                    centerY + (sin(angle) * radius).toFloat(),
                    dotRadius,
                    paint,
                )
            }
            return
        }

        paint.style = Paint.Style.STROKE
        bounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        canvas.drawArc(
            bounds,
            Math.toDegrees(startAngle).toFloat(),
            Math.toDegrees(sweepAngle).toFloat(),
            false,
            paint,
        )
    }

    fun drawConnector(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        directionX: Float,
        directionY: Float,
        start: Float,
        end: Float,
        opacity: Float,
        bend: Float = 0f,
    ) {
        if (end <= start || opacity <= 0f) return
        setOpacity(opacity)

        val line = Line(centerX, centerY, directionX, directionY, start, end)

        when (style) {
            MenuConnectorStyle.NONE -> Unit
            MenuConnectorStyle.DOTTED,
            MenuConnectorStyle.DOT_RING,
            -> drawDots(canvas, line)
            MenuConnectorStyle.DASH -> drawDashes(canvas, line)
            MenuConnectorStyle.FADE -> drawFade(canvas, line)
            MenuConnectorStyle.CURVE -> drawCurve(canvas, line, bend)
            MenuConnectorStyle.TAPER -> drawTaper(canvas, line)
            MenuConnectorStyle.WAVE -> drawPolyline(canvas, wavePoints(line), Paint.Join.ROUND)
            MenuConnectorStyle.ZIGZAG -> drawPolyline(canvas, zigzagPoints(line), Paint.Join.MITER)
            MenuConnectorStyle.DOUBLE -> drawDouble(canvas, line)
            MenuConnectorStyle.GLOW -> drawGlow(canvas, line)
            MenuConnectorStyle.BUBBLE -> drawBubbles(canvas, line)
            MenuConnectorStyle.LINE,
            MenuConnectorStyle.LINE_RING,
            MenuConnectorStyle.ARC,
            -> drawStraight(canvas, line)
        }
    }

    private fun setOpacity(opacity: Float) {
        paint.alpha = (baseAlpha * opacity.coerceIn(0f, 1f)).toInt()
    }

    private fun drawStraight(
        canvas: Canvas,
        line: Line,
    ) {
        paint.style = Paint.Style.STROKE
        canvas.drawLine(line.startX, line.startY, line.endX, line.endY, paint)
    }

    private fun drawDots(
        canvas: Canvas,
        line: Line,
    ) {
        paint.style = Paint.Style.FILL
        var position = if (hasRing) line.start + dotSpacing else line.start
        while (position <= line.end - dotRadius) {
            canvas.drawCircle(line.x(position), line.y(position), dotRadius, paint)
            position += dotSpacing
        }
    }

    private fun drawDashes(
        canvas: Canvas,
        line: Line,
    ) {
        paint.style = Paint.Style.STROKE
        val dashLength = strokeWidth * DASH_LENGTH_RATIO
        val dashGap = strokeWidth * DASH_GAP_RATIO

        val lastEnd = line.end - strokeWidth / 2f
        var position = line.start
        while (position < lastEnd) {
            val dashEnd = min(position + dashLength, lastEnd)
            if (dashEnd - position < strokeWidth) break
            canvas.drawLine(line.x(position), line.y(position), line.x(dashEnd), line.y(dashEnd), paint)
            position += dashLength + dashGap
        }
    }

    private fun drawFade(
        canvas: Canvas,
        line: Line,
    ) {
        paint.style = Paint.Style.STROKE
        paint.shader =
            LinearGradient(
                line.startX,
                line.startY,
                line.endX,
                line.endY,
                opaqueColor,
                fadeEndColor,
                Shader.TileMode.CLAMP,
            )
        canvas.drawLine(line.startX, line.startY, line.endX, line.endY, paint)
        paint.shader = null
    }

    private fun drawCurve(
        canvas: Canvas,
        line: Line,
        bend: Float,
    ) {
        val offset = (line.end - line.start) * CURVE_BEND * bend
        val middle = (line.start + line.end) / 2f

        paint.style = Paint.Style.STROKE
        path.reset()
        path.moveTo(line.startX, line.startY)
        path.quadTo(line.x(middle, offset), line.y(middle, offset), line.endX, line.endY)
        canvas.drawPath(path, paint)
    }

    private fun drawTaper(
        canvas: Canvas,
        line: Line,
    ) {
        val startHalf = strokeWidth * TAPER_START_RATIO / 2f
        val endHalf = strokeWidth * TAPER_END_RATIO / 2f

        paint.style = Paint.Style.FILL
        path.reset()
        path.moveTo(line.x(line.start, startHalf), line.y(line.start, startHalf))
        path.lineTo(line.x(line.end, endHalf), line.y(line.end, endHalf))
        path.lineTo(line.x(line.end, -endHalf), line.y(line.end, -endHalf))
        path.lineTo(line.x(line.start, -startHalf), line.y(line.start, -startHalf))
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawPolyline(
        canvas: Canvas,
        points: FloatArray,
        join: Paint.Join,
    ) {
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = join
        path.reset()
        path.moveTo(points[0], points[1])
        for (i in 2 until points.size step 2) path.lineTo(points[i], points[i + 1])
        canvas.drawPath(path, paint)
        paint.strokeJoin = Paint.Join.ROUND
    }

    private fun drawDouble(
        canvas: Canvas,
        line: Line,
    ) {
        val offset = strokeWidth * DOUBLE_OFFSET_RATIO

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth * DOUBLE_RAIL_RATIO
        for (side in floatArrayOf(-offset, offset)) {
            canvas.drawLine(
                line.x(line.start, side),
                line.y(line.start, side),
                line.x(line.end, side),
                line.y(line.end, side),
                paint,
            )
        }
        paint.strokeWidth = strokeWidth
    }

    private fun drawGlow(
        canvas: Canvas,
        line: Line,
    ) {
        val lineAlpha = paint.alpha
        paint.style = Paint.Style.STROKE
        for ((widthRatio, opacity) in GLOW_LAYERS) {
            paint.strokeWidth = strokeWidth * widthRatio
            paint.alpha = (lineAlpha * opacity).toInt()
            canvas.drawLine(line.startX, line.startY, line.endX, line.endY, paint)
        }
        paint.strokeWidth = strokeWidth
        paint.alpha = lineAlpha
        canvas.drawLine(line.startX, line.startY, line.endX, line.endY, paint)
    }

    private fun drawBubbles(
        canvas: Canvas,
        line: Line,
    ) {
        paint.style = Paint.Style.FILL
        var position = line.start
        while (position < line.end) {
            val fraction = (position - line.start) / (line.end - line.start)
            val scale = BUBBLE_START_RATIO + (BUBBLE_END_RATIO - BUBBLE_START_RATIO) * fraction
            if (position + dotRadius * scale > line.end) break
            canvas.drawCircle(line.x(position), line.y(position), dotRadius * scale, paint)
            position += dotSpacing * (0.6f + scale * 0.5f)
        }
    }

    private fun wavePoints(line: Line): FloatArray {
        val length = line.end - line.start
        val halfWave = strokeWidth * WAVE_LENGTH_RATIO / 2f
        val halves = max(1, (length / halfWave).roundToInt())
        val amplitude = strokeWidth * WAVE_AMPLITUDE_RATIO
        val steps = halves * WAVE_SEGMENTS_PER_HALF

        return FloatArray((steps + 1) * 2) { i ->
            val fraction = (i / 2).toFloat() / steps
            val along = line.start + length * fraction
            val across = sin(fraction * halves * PI).toFloat() * amplitude
            if (i % 2 == 0) line.x(along, across) else line.y(along, across)
        }
    }

    private fun zigzagPoints(line: Line): FloatArray {
        val length = line.end - line.start
        val halfWave = strokeWidth * ZIGZAG_LENGTH_RATIO / 2f
        val halves = max(1, (length / halfWave).roundToInt())
        val amplitude = strokeWidth * ZIGZAG_AMPLITUDE_RATIO

        val pointCount = halves + 2
        return FloatArray(pointCount * 2) { i ->
            val point = i / 2
            val (fraction, across) =
                when (point) {
                    0 -> 0f to 0f
                    pointCount - 1 -> 1f to 0f
                    else -> {
                        val peak = point - 1
                        (peak + 0.5f) / halves to if (peak % 2 == 0) amplitude else -amplitude
                    }
                }
            val along = line.start + length * fraction
            if (i % 2 == 0) line.x(along, across) else line.y(along, across)
        }
    }

    private class Line(
        val centerX: Float,
        val centerY: Float,
        val directionX: Float,
        val directionY: Float,
        val start: Float,
        val end: Float,
    ) {
        val startX get() = x(start)
        val startY get() = y(start)
        val endX get() = x(end)
        val endY get() = y(end)

        fun x(
            along: Float,
            across: Float = 0f,
        ) = centerX + directionX * along - directionY * across

        fun y(
            along: Float,
            across: Float = 0f,
        ) = centerY + directionY * along + directionX * across
    }
}
