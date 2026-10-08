/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: HilightProgress.kt
 * Description: Colour modes and frame maths for showing live notification progress on the Hilight LEDs.
 */

package com.sameerasw.essentials.domain.model

import androidx.annotation.StringRes
import com.sameerasw.essentials.R
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class HilightProgressColorMode(
    @StringRes val title: Int,
) {
    MONOCHROME(R.string.hilight_progress_mode_monochrome),
    SEQUENTIAL(R.string.hilight_progress_mode_sequential),
    VIBGYOR(R.string.hilight_progress_mode_vibgyor),
}

// Pure frame maths, kept free of Android framework calls so it can be unit tested
object HilightProgressFrames {
    private const val COMPLETION_PHASE_MS = 200L
    private const val SWEEP_STEP_MS = 120L
    private const val SEQUENTIAL_MIN_LEVEL = 0.2f

    // Violet, indigo, blue, teal, green, yellow, orange, red; even hue steps would skip yellow and indigo
    private val VIBGYOR_HUES = floatArrayOf(280f, 250f, 220f, 180f, 120f, 60f, 30f, 0f)

    const val COMPLETION_DURATION_MS = 4 * COMPLETION_PHASE_MS
    const val DEMO_STEP_MS = 400L

    // Each LED is either fully on or off so neighbouring LEDs never blend into a partial one
    fun litCount(
        progress: Float,
        ledCount: Int,
    ): Int = ceil(progress.coerceIn(0f, 100f) / 100f * ledCount).toInt().coerceIn(1, ledCount)

    fun ledColor(
        mode: HilightProgressColorMode,
        color: Int,
        index: Int,
        ledCount: Int,
    ): Int {
        val position = if (ledCount > 1) index.toFloat() / (ledCount - 1) else 1f
        return when (mode) {
            HilightProgressColorMode.MONOCHROME -> HilightFrames.scale(color, 1f)
            // Evenly spaced steps from dim to full, so each LED reads as its own shade
            HilightProgressColorMode.SEQUENTIAL ->
                HilightFrames.scale(color, SEQUENTIAL_MIN_LEVEL + (1f - SEQUENTIAL_MIN_LEVEL) * position)
            HilightProgressColorMode.VIBGYOR ->
                HilightFrames.hueToColor(VIBGYOR_HUES[(position * (VIBGYOR_HUES.size - 1)).roundToInt()])
        }
    }

    fun progress(
        progress: Float,
        mode: HilightProgressColorMode,
        color: Int,
        ledCount: Int,
    ): IntArray {
        val lit = litCount(progress, ledCount)
        return IntArray(ledCount) { i -> if (i < lit) ledColor(mode, color, i, ledCount) else HilightFrames.OFF }
    }

    // All LEDs blink twice
    fun completion(
        elapsedMs: Long,
        mode: HilightProgressColorMode,
        color: Int,
        ledCount: Int,
    ): IntArray {
        val phase = (elapsedMs.coerceAtLeast(0) / COMPLETION_PHASE_MS).toInt()
        val lit = phase % 2 == 0 && phase < 4
        return IntArray(ledCount) { i -> if (lit) ledColor(mode, color, i, ledCount) else HilightFrames.OFF }
    }

    // One LED sweeping back and forth when the notification has no percentage
    fun indeterminate(
        elapsedMs: Long,
        mode: HilightProgressColorMode,
        color: Int,
        ledCount: Int,
    ): IntArray {
        val period = (2 * (ledCount - 1)).coerceAtLeast(1)
        val step = ((elapsedMs.coerceAtLeast(0) / SWEEP_STEP_MS) % period).toInt()
        val position = if (step < ledCount) step else period - step
        return IntArray(ledCount) { i -> if (i == position) ledColor(mode, color, i, ledCount) else HilightFrames.OFF }
    }

    // Fills the LEDs one at a time, then plays the completion blink
    fun demo(
        elapsedMs: Long,
        mode: HilightProgressColorMode,
        color: Int,
        ledCount: Int,
    ): IntArray {
        val fillMs = ledCount * DEMO_STEP_MS
        if (elapsedMs >= fillMs) return completion(elapsedMs - fillMs, mode, color, ledCount)
        val lit = (elapsedMs / DEMO_STEP_MS).toInt() + 1
        return IntArray(ledCount) { i -> if (i < lit) ledColor(mode, color, i, ledCount) else HilightFrames.OFF }
    }

    fun demoDurationMs(ledCount: Int) = ledCount * DEMO_STEP_MS + COMPLETION_DURATION_MS
}
