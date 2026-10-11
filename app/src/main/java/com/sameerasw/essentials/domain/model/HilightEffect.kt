/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: HilightEffect.kt
 * Description: Hilight (Pixel rear LED array) effects and the per-frame colours they produce.
 */

package com.sameerasw.essentials.domain.model

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.sameerasw.essentials.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

@Keep
enum class HilightPattern(
    @StringRes val title: Int,
) {
    @SerializedName("SOLID")
    SOLID(R.string.hilight_pattern_solid),

    @SerializedName("BLINK")
    BLINK(R.string.hilight_pattern_blink),

    @SerializedName("GLOW")
    GLOW(R.string.hilight_pattern_glow),

    @SerializedName("FLASH")
    FLASH(R.string.hilight_pattern_flash),

    @SerializedName("SWEEP")
    SWEEP(R.string.hilight_pattern_sweep),

    @SerializedName("RAINBOW")
    RAINBOW(R.string.hilight_pattern_rainbow),
}

@Keep
data class HilightEffect(
    @SerializedName("pattern") val pattern: HilightPattern = HilightPattern.GLOW,
    @SerializedName("color") val color: Int = DEFAULT_COLOR,
    @SerializedName("durationMs") val durationMs: Long = DEFAULT_DURATION_MS,
) {
    // Gson sets an unknown pattern name to null despite the non-null type
    @Suppress("USELESS_ELVIS")
    fun withValidPattern() = copy(pattern = pattern ?: HilightPattern.GLOW)

    companion object {
        const val DEFAULT_COLOR = 0xFF4285F4.toInt()
        const val DEFAULT_DURATION_MS = 3_000L
        const val MIN_DURATION_MS = 1_000L

        // No published guidance exists for sustained use of the array, so keep effects short
        const val MAX_DURATION_MS = 30_000L
    }
}

// Pure frame maths, kept free of Android framework calls so it can be unit tested
object HilightFrames {
    const val OFF = 0x00000000

    fun frame(
        effect: HilightEffect,
        elapsedMs: Long,
        ledCount: Int,
    ): IntArray {
        val t = elapsedMs.coerceAtLeast(0)
        return when (effect.pattern) {
            HilightPattern.SOLID -> IntArray(ledCount) { opaque(effect.color) }
            HilightPattern.BLINK -> uniform(ledCount, effect.color, if ((t / BLINK_HALF_MS) % 2 == 0L) 1f else 0f)
            HilightPattern.GLOW -> {
                val phase = (t % GLOW_PERIOD_MS).toDouble() / GLOW_PERIOD_MS
                uniform(ledCount, effect.color, ((1 - cos(2 * PI * phase)) / 2).toFloat())
            }
            HilightPattern.FLASH -> {
                val inCycle = t % FLASH_PERIOD_MS
                val lit = inCycle < FLASH_ON_MS || inCycle in (2 * FLASH_ON_MS) until (3 * FLASH_ON_MS)
                uniform(ledCount, effect.color, if (lit) 1f else 0f)
            }
            HilightPattern.SWEEP -> {
                val head = ((t / SWEEP_STEP_MS) % ledCount.coerceAtLeast(1)).toInt()
                IntArray(ledCount) { i ->
                    when ((head - i + ledCount) % ledCount.coerceAtLeast(1)) {
                        0 -> scale(effect.color, 1f)
                        1 -> scale(effect.color, SWEEP_TAIL_LEVEL)
                        else -> OFF
                    }
                }
            }
            HilightPattern.RAINBOW ->
                IntArray(ledCount) { i ->
                    val hue = ((t % RAINBOW_PERIOD_MS).toFloat() / RAINBOW_PERIOD_MS * 360f + i * 360f / ledCount.coerceAtLeast(1)) % 360f
                    hueToColor(hue)
                }
        }
    }

    fun scale(
        color: Int,
        level: Float,
    ): Int {
        val f = level.coerceIn(0f, 1f)
        if (f == 0f) return OFF
        val r = (((color shr 16) and 0xFF) * f).toInt()
        val g = (((color shr 8) and 0xFF) * f).toInt()
        val b = ((color and 0xFF) * f).toInt()
        return opaque((r shl 16) or (g shl 8) or b)
    }

    private fun opaque(color: Int) = color or (0xFF shl 24)

    fun hueToColor(hue: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f / 60f
        val x = 1f - abs(h % 2f - 1f)
        val (r, g, b) =
            when (h.toInt()) {
                0 -> Triple(1f, x, 0f)
                1 -> Triple(x, 1f, 0f)
                2 -> Triple(0f, 1f, x)
                3 -> Triple(0f, x, 1f)
                4 -> Triple(x, 0f, 1f)
                else -> Triple(1f, 0f, x)
            }
        return opaque(((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt())
    }

    private fun uniform(
        ledCount: Int,
        color: Int,
        level: Float,
    ): IntArray {
        val value = scale(color, level)
        return IntArray(ledCount) { value }
    }

    private const val BLINK_HALF_MS = 500L
    private const val GLOW_PERIOD_MS = 2_000L
    private const val FLASH_PERIOD_MS = 1_200L
    private const val FLASH_ON_MS = 120L
    private const val SWEEP_STEP_MS = 90L
    private const val SWEEP_TAIL_LEVEL = 0.3f
    private const val RAINBOW_PERIOD_MS = 3_000L
}
