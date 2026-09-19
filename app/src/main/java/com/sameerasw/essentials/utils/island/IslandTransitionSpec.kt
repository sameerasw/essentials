/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays - Island
 * File: IslandTransitionSpec.kt
 * Description: Named duration/interpolator presets for island overlay animations.
 */

package com.sameerasw.essentials.utils.island

import android.animation.TimeInterpolator
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

data class IslandTransitionSpec(
    val durationMs: Long,
    val interpolator: TimeInterpolator,
) {
    companion object {
        fun spring(dampingRatio: Float, responseTimeSec: Float, durationMs: Long) =
            IslandTransitionSpec(durationMs, AppleSpringInterpolator(dampingRatio, responseTimeSec))

        fun dismiss(responseTimeSec: Float, durationMs: Long) =
            IslandTransitionSpec(durationMs, AppleDismissInterpolator(responseTimeSec))

        val SwipeDismiss = dismiss(responseTimeSec = 0.24f, durationMs = 240L)
        val DragCollapse = dismiss(responseTimeSec = 0.20f, durationMs = 200L)
        val DragSnapBack = spring(dampingRatio = 0.72f, responseTimeSec = 0.35f, durationMs = 320L)
        val ActionExecution = spring(dampingRatio = 0.85f, responseTimeSec = 0.28f, durationMs = 260L)
        val ContentShow = spring(dampingRatio = 0.70f, responseTimeSec = 0.50f, durationMs = 480L)
        val ModeChange = spring(dampingRatio = 0.66f, responseTimeSec = 0.46f, durationMs = 420L)
        val MediaDismiss = dismiss(responseTimeSec = 0.28f, durationMs = 280L)
        val BubbleIn = spring(dampingRatio = 0.70f, responseTimeSec = 0.44f, durationMs = 420L)
        val Merge = spring(dampingRatio = 0.72f, responseTimeSec = 0.48f, durationMs = 480L)
        val ExpandOpen = spring(dampingRatio = 0.65f, responseTimeSec = 0.50f, durationMs = 460L)

        fun notificationDismiss(wasExpanded: Boolean) = if (wasExpanded) {
            dismiss(responseTimeSec = 0.32f, durationMs = 320L)
        } else {
            dismiss(responseTimeSec = 0.28f, durationMs = 280L)
        }
    }
}

internal class AppleSpringInterpolator(
    private val dampingRatio: Float = 0.70f,
    private val responseTimeSec: Float = 0.50f,
) : TimeInterpolator {
    private val omegaN = (2.0 * Math.PI / responseTimeSec).toFloat()
    private val omegaD = (omegaN * sqrt((1.0 - dampingRatio * dampingRatio))).toFloat()
    private val beta = (dampingRatio / sqrt((1.0 - dampingRatio * dampingRatio))).toFloat()

    override fun getInterpolation(input: Float): Float {
        if (input <= 0f) return 0f
        if (input >= 1f) return 1f
        val t = input * responseTimeSec
        val envelope = exp((-dampingRatio * omegaN * t).toDouble()).toFloat()
        val osc = cos((omegaD * t).toDouble()).toFloat() + beta * sin((omegaD * t).toDouble()).toFloat()
        return 1.0f - envelope * osc
    }
}

internal class AppleDismissInterpolator(
    private val responseTimeSec: Float = 0.28f,
) : TimeInterpolator {
    private val omegaN = (2.0 * Math.PI / (responseTimeSec * 1.15f)).toFloat()

    override fun getInterpolation(input: Float): Float {
        if (input <= 0f) return 0f
        if (input >= 1f) return 1f
        val t = input * responseTimeSec
        val envelope = exp((-omegaN * t).toDouble()).toFloat()
        return 1.0f - (envelope * (1.0f + omegaN * t))
    }
}
