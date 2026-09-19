/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays - Island
 * File: EqualizerIcon.kt
 * Description: Lightweight animated 3-bar equalizer glyph drawn directly on canvas.
 */

package com.sameerasw.essentials.utils.island

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.animation.LinearInterpolator
import kotlin.random.Random

private const val BAR_COUNT = 3
private const val FLAT_LEVEL = 0.16f
private const val TICK_MS = 80L
private const val EASE_PER_TICK = 0.35f

class EqualizerAnimator {
    val barLevels = FloatArray(BAR_COUNT) { FLAT_LEVEL }
    private val targetLevels = FloatArray(BAR_COUNT) { FLAT_LEVEL }
    private val nextRetargetAtMs = LongArray(BAR_COUNT)
    private val random = Random(System.nanoTime())
    private var animator: ValueAnimator? = null
    private var isFlat = true

    fun start(onInvalidate: () -> Unit) {
        isFlat = false
        if (animator != null) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = TICK_MS
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                tick()
                onInvalidate()
            }
            start()
        }
    }

    private fun tick() {
        val now = SystemClock.uptimeMillis()
        for (i in 0 until BAR_COUNT) {
            if (now >= nextRetargetAtMs[i]) {
                targetLevels[i] = 0.15f + random.nextFloat() * 0.85f
                nextRetargetAtMs[i] = now + 90L + random.nextInt(260)
            }
            barLevels[i] += (targetLevels[i] - barLevels[i]) * EASE_PER_TICK
        }
    }

    fun stop() {
        animator?.cancel()
        animator = null
    }

    fun freezeFlat(onInvalidate: () -> Unit) {
        stop()
        isFlat = true
        for (i in 0 until BAR_COUNT) {
            barLevels[i] = FLAT_LEVEL
            targetLevels[i] = FLAT_LEVEL
            nextRetargetAtMs[i] = 0L
        }
        onInvalidate()
    }

    fun isFrozenFlat(): Boolean = isFlat
}

// Bars are drawn centered on [centerY] and grow symmetrically up and down (mirrored), not from a baseline.
fun drawEqualizerIcon(canvas: Canvas, bounds: RectF, levels: FloatArray, paint: Paint) {
    val size = minOf(bounds.width(), bounds.height())
    val barWidth = size * 0.18f
    val gap = size * 0.08f
    val totalWidth = BAR_COUNT * barWidth + (BAR_COUNT - 1) * gap
    var left = bounds.centerX() - totalWidth / 2f
    val minHeight = size * 0.22f
    val maxHeight = size * 0.9f
    val radius = barWidth / 2f

    for (i in 0 until BAR_COUNT) {
        val height = minHeight + (maxHeight - minHeight) * levels[i]
        val halfHeight = height / 2f
        canvas.drawRoundRect(
            left,
            bounds.centerY() - halfHeight,
            left + barWidth,
            bounds.centerY() + halfHeight,
            radius,
            radius,
            paint,
        )
        left += barWidth + gap
    }
}
