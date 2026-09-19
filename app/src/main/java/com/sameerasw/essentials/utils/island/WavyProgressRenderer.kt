/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays - Island
 * File: WavyProgressRenderer.kt
 * Description: Animated wavy track-progress line drawn on canvas for the media full player.
 */

package com.sameerasw.essentials.utils.island

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.animation.LinearInterpolator
import kotlin.math.sin

class WavyProgressRenderer(
    private val density: Float,
    private val onFrame: () -> Unit,
) {
    private var phase = 0f
    private var animator: ValueAnimator? = null
    private val path = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // Phase advances every display frame so the wave stays smooth regardless of position poll rate.
    fun setRunning(running: Boolean) {
        if (!running) {
            animator?.cancel()
            animator = null
            return
        }
        if (animator != null) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200L
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                phase += 0.045f
                onFrame()
            }
            start()
        }
    }

    fun draw(canvas: Canvas, left: Float, centerY: Float, right: Float, progress: Float, baseAlpha: Int, accent: Int) {
        val width = right - left
        if (width <= 0f) return
        val amplitude = 4.5f * density
        val wavelength = 22f * density
        val gap = 6f * density
        val progressX = left + width * progress.coerceIn(0f, 1f)

        if (progressX > left) {
            paint.strokeWidth = 5f * density
            paint.color = accent
            paint.alpha = baseAlpha
            path.reset()
            var x = left
            val step = 1.5f * density
            while (x <= progressX) {
                val y = centerY + sin((x - left) / wavelength * 2f * Math.PI.toFloat() + phase) * amplitude
                if (x == left) path.moveTo(x, y) else path.lineTo(x, y)
                x += step
            }
            canvas.drawPath(path, paint)
        }

        val trackStart = progressX + gap
        if (trackStart < right) {
            paint.strokeWidth = 4f * density
            paint.color = Color.WHITE
            paint.alpha = (baseAlpha * 0.3f).toInt().coerceIn(0, 255)
            canvas.drawLine(trackStart, centerY, right, centerY, paint)
        }
    }
}
