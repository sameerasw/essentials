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
import kotlin.math.max
import kotlin.math.round

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

    @SerializedName("PULSE")
    PULSE(R.string.hilight_pattern_pulse),

    @SerializedName("RIPPLE")
    RIPPLE(R.string.hilight_pattern_ripple),

    @SerializedName("SPARKLE")
    SPARKLE(R.string.hilight_pattern_sparkle),

    @SerializedName("RAINBOW")
    RAINBOW(R.string.hilight_pattern_rainbow),

    @SerializedName("BATTERY")
    BATTERY(R.string.hilight_pattern_battery),
}

@Keep
data class HilightEffect(
    @SerializedName("pattern") val pattern: HilightPattern = HilightPattern.GLOW,
    @SerializedName("color") val color: Int = DEFAULT_COLOR,
    @SerializedName("durationMs") val durationMs: Long = DEFAULT_DURATION_MS,
    @SerializedName("brightness") val brightness: Int = 100,
    @SerializedName("repeats") val repeats: Int = 0,
    @SerializedName("autoColor") val autoColor: Boolean = false,
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

        const val MIN_BRIGHTNESS = 10
        const val MAX_REPEATS = 10
        const val DEFAULT_REPEATS = 3
        const val DEFAULT_LED_COUNT = 8

        fun appColorOr(
            appColor: Int,
            fallback: Int,
        ): Int = if (appColor ushr 24 == 0) fallback else appColor
    }
}

// Pure frame maths, kept free of Android framework calls so it can be unit tested
object HilightFrames {
    const val OFF = 0x00000000

    fun frame(
        effect: HilightEffect,
        elapsedMs: Long,
        ledCount: Int,
        batteryLevel: Int = 100,
    ): IntArray {
        val t = elapsedMs.coerceAtLeast(0)
        val base = patternFrame(effect, t, ledCount, batteryLevel)
        return if (effect.brightness < 100) {
            val factor = effect.brightness / 100f
            IntArray(ledCount) { i -> scale(base[i], factor) }
        } else {
            base
        }
    }

    private fun patternFrame(
        effect: HilightEffect,
        t: Long,
        ledCount: Int,
        batteryLevel: Int,
    ): IntArray =
        when (effect.pattern) {
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
            HilightPattern.PULSE -> {
                fun beat(x: Long): Float = if (x in 0 until 200) 1f - x / 200f else 0f
                val c = t % PULSE_PERIOD_MS
                val level = max(beat(c), beat(c - 280))
                uniform(ledCount, effect.color, level)
            }
            HilightPattern.RIPPLE -> {
                val centre = (ledCount - 1) / 2f
                val maxD = centre + 1f
                val front = (t % RIPPLE_PERIOD_MS).toFloat() / RIPPLE_PERIOD_MS * maxD
                IntArray(ledCount) { i ->
                    val d = abs(i - centre)
                    val level = (1f - abs(d - front)).coerceIn(0f, 1f)
                    scale(effect.color, level)
                }
            }
            HilightPattern.SPARKLE -> {
                val step = t / SPARKLE_STEP_MS
                IntArray(ledCount) { i ->
                    var h = (i * 73856093) xor (step * 19349663L).toInt()
                    h = h xor (h ushr 13)
                    if ((h and 3) == 0) {
                        val level = 0.4f + ((h ushr 2) and 0xFF) / 255f * 0.6f
                        scale(effect.color, level)
                    } else {
                        OFF
                    }
                }
            }
            HilightPattern.RAINBOW ->
                IntArray(ledCount) { i ->
                    val hue =
                        ((t % RAINBOW_PERIOD_MS).toFloat() / RAINBOW_PERIOD_MS * 360f + i * 360f / ledCount.coerceAtLeast(1)) % 360f
                    hueToColor(hue)
                }
            HilightPattern.BATTERY -> {
                val maxLit = HilightProgressFrames.litCount(batteryLevel.toFloat(), ledCount)
                val filled = ((t / BATTERY_STEP_MS) + 1).toInt().coerceAtMost(maxLit)
                IntArray(ledCount) { i -> if (i < filled) scale(effect.color, 1f) else OFF }
            }
        }

    fun cycleMs(
        pattern: HilightPattern,
        ledCount: Int,
    ): Long? =
        when (pattern) {
            HilightPattern.BLINK -> 2 * BLINK_HALF_MS
            HilightPattern.GLOW -> GLOW_PERIOD_MS
            HilightPattern.FLASH -> FLASH_PERIOD_MS
            HilightPattern.SWEEP -> SWEEP_STEP_MS * ledCount
            HilightPattern.PULSE -> PULSE_PERIOD_MS
            HilightPattern.RIPPLE -> RIPPLE_PERIOD_MS
            HilightPattern.RAINBOW -> RAINBOW_PERIOD_MS
            HilightPattern.SOLID,
            HilightPattern.SPARKLE,
            HilightPattern.BATTERY,
            -> null
        }

    fun durationMs(
        effect: HilightEffect,
        ledCount: Int,
    ): Long {
        val cycle = cycleMs(effect.pattern, ledCount)
        val duration =
            if (effect.repeats > 0 && cycle != null) {
                effect.repeats * cycle
            } else {
                effect.durationMs.coerceAtLeast(HilightEffect.MIN_DURATION_MS)
            }
        return duration.coerceAtMost(HilightEffect.MAX_DURATION_MS)
    }

    fun repeatsFor(effect: HilightEffect): Int {
        if (effect.repeats > 0) return effect.repeats
        val cycle = cycleMs(effect.pattern, HilightEffect.DEFAULT_LED_COUNT) ?: return HilightEffect.DEFAULT_REPEATS
        val count = round(effect.durationMs.toFloat() / cycle).toInt()
        return count.coerceIn(1, HilightEffect.MAX_REPEATS)
    }

    // The repeat count the editor shows, so an effect saved without touching the slider plays what it showed
    fun withShownRepeats(effect: HilightEffect): HilightEffect =
        if (effect.repeats == 0 && cycleMs(effect.pattern, HilightEffect.DEFAULT_LED_COUNT) != null) {
            effect.copy(repeats = repeatsFor(effect))
        } else {
            effect
        }

    fun scale(
        color: Int,
        level: Float,
    ): Int {
        val f = level.coerceIn(0f, 1f)
        if (color == OFF || f == 0f) return OFF
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
    private const val PULSE_PERIOD_MS = 1_200L
    private const val RIPPLE_PERIOD_MS = 1_200L
    private const val SPARKLE_STEP_MS = 120L
    private const val BATTERY_STEP_MS = 150L
}
