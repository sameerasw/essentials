/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays - Island
 * File: MarqueeController.kt
 * Description: Self-contained horizontal text-scroll animation for a single label.
 */

package com.sameerasw.essentials.utils.island

import android.animation.ValueAnimator
import android.graphics.Paint
import android.view.animation.LinearInterpolator
import kotlin.math.abs

class MarqueeController {
    var offset: Float = 0f
        private set
    var isNeeded: Boolean = false
        private set
    private var lastText: String = ""
    private var lastWidth: Float = 0f
    private var animator: ValueAnimator? = null

    fun update(text: String, maxTextWidth: Float, textPaint: Paint, density: Float, onInvalidate: () -> Unit) {
        val textWidth = textPaint.measureText(text)
        val needed = (textWidth - maxTextWidth) > 1.5f * density && maxTextWidth > 0f

        if (needed) {
            if (!isNeeded || text != lastText || abs(maxTextWidth - lastWidth) > 1f) {
                isNeeded = true
                lastText = text
                lastWidth = maxTextWidth
                animator?.cancel()
                offset = 0f

                val marqueeGap = 28f * density
                val totalDistance = textWidth + marqueeGap
                val speedDpPerSec = 30f
                val durationMs = ((totalDistance / density) / speedDpPerSec * 1000L).toLong().coerceAtLeast(2000L)

                animator = ValueAnimator.ofFloat(0f, totalDistance).apply {
                    duration = durationMs
                    interpolator = LinearInterpolator()
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.RESTART
                    startDelay = 1200L
                    addUpdateListener {
                        offset = it.animatedValue as Float
                        onInvalidate()
                    }
                    start()
                }
            }
        } else {
            if (isNeeded || text != lastText) {
                stop()
                lastText = text
                lastWidth = maxTextWidth
            }
        }
    }

    fun stop() {
        isNeeded = false
        lastText = ""
        lastWidth = 0f
        animator?.cancel()
        animator = null
        offset = 0f
    }
}
