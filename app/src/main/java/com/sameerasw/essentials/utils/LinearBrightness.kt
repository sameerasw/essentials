package com.sameerasw.essentials.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.view.Display
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

class LinearBrightness(private val context: Context) {
    private var baseGamma = 0f

    fun begin() {
        baseGamma = gammaOf(read())
    }

    private fun range(): Pair<Float, Float> {
        val info = runCatching {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = dm.getDisplay(Display.DEFAULT_DISPLAY)
            Display::class.java.getMethod("getBrightnessInfo").invoke(display)
        }.getOrNull()
        val min = info?.let { runCatching { it.javaClass.getField("brightnessMinimum").getFloat(it) }.getOrNull() } ?: 0f
        val max = info?.let { runCatching { it.javaClass.getField("brightnessMaximum").getFloat(it) }.getOrNull() } ?: 1f
        return if (max > min) min to max else 0f to 1f
    }

    fun slide(dx: Float, rangePx: Float): Int {
        val gamma = (baseGamma + dx / rangePx.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val (min, max) = range()
        val target = (min + linearOf(gamma) * (max - min)).coerceIn(MIN_LINEAR, 1f)
        if (kotlin.math.abs(target - readRaw()) > 1e-5f) write(target)
        return (gamma * 100f).roundToInt()
    }

    fun percent(): Int = (gammaOf(read()) * 100f).roundToInt()

    private fun read(): Float {
        val (min, max) = range()
        return ((readRaw().coerceIn(min, max) - min) / (max - min)).coerceIn(0f, 1f)
    }

    private fun readRaw(): Float {
        val resolver = context.contentResolver
        val float = runCatching { Settings.System.getFloat(resolver, FLOAT_KEY, -1f) }.getOrDefault(-1f)
        if (float >= 0f) return float.coerceIn(0f, 1f)
        val int = runCatching { Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128) }.getOrDefault(128)
        return (int / 255f).coerceIn(0f, 1f)
    }

    private fun write(linear: Float) {
        val resolver = context.contentResolver
        val hasFloat = runCatching { Settings.System.getFloat(resolver, FLOAT_KEY, -1f) >= 0f }.getOrDefault(false)
        runCatching {
            if (hasFloat) {
                Settings.System.putFloat(resolver, FLOAT_KEY, linear)
            } else {
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, (linear * 255f).roundToInt().coerceIn(1, 255))
            }
        }
    }

    private fun gammaOf(linear: Float): Float {
        val norm = linear * 12f
        val ret = if (norm <= 1f) sqrt(norm) * R else A * ln(norm - B) + C
        return ret.coerceIn(0f, 1f)
    }

    private fun linearOf(gamma: Float): Float {
        val ret = if (gamma <= R) (gamma / R) * (gamma / R) else exp((gamma - C) / A) + B
        return (ret / 12f).coerceIn(0f, 1f)
    }

    companion object {
        private const val FLOAT_KEY = "screen_brightness_float"
        private const val MIN_LINEAR = 0.0015f
        private const val R = 0.5f
        private const val A = 0.17883277f
        private const val B = 0.28466892f
        private const val C = 0.55991073f
    }
}
