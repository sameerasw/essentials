package com.sameerasw.essentials.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.view.Display
import com.sameerasw.essentials.R
import java.util.Locale
import kotlin.math.roundToInt

object RefreshRateUtils {
    const val MODE_FIXED = "fixed"
    const val MODE_RANGE = "range"

    private const val KEY_PEAK_REFRESH_RATE = "peak_refresh_rate"
    private const val KEY_MIN_REFRESH_RATE = "min_refresh_rate"
    private const val DEFAULT_SYSTEM_REFRESH_RATE = 60f

    val PRESET_RATES = listOf(10, 30, 60, 90, 120)

    data class RefreshRateState(
        val min: Float,
        val peak: Float,
        val isSystemManaged: Boolean,
        val usesInfinityDefaultPeak: Boolean
    )

    fun getPeakRefreshRate(context: Context): Float {
        return getCurrentState(context).peak
    }

    fun getMinRefreshRate(context: Context): Float {
        return getCurrentState(context).min
    }

    fun hasCustomRefreshRate(context: Context): Boolean {
        val state = getCurrentState(context)
        return !state.isSystemManaged && (state.peak > 0f || state.min > 0f)
    }

    fun getDisplayValue(context: Context): Float {
        val state = getCurrentState(context)
        if (state.isSystemManaged) return 0f

        val min = state.min
        val peak = state.peak
        return when {
            peak > 0f -> peak
            min > 0f -> min
            else -> 0f
        }
    }

    fun getDisplaySubtitle(context: Context): String {
        val state = getCurrentState(context)
        if (state.isSystemManaged) {
            return context.getString(R.string.refresh_rate_system_default)
        }

        val min = state.min
        val peak = state.peak
        return when {
            min > 0f && peak > 0f && min.roundToInt() != peak.roundToInt() ->
                "${min.roundToInt()}-${peak.roundToInt()} Hz"

            else -> "${getDisplayValue(context).roundToInt()} Hz"
        }
    }

    fun applyFixedRefreshRate(value: Float): Boolean {
        if (!ShizukuUtils.hasPermission()) return false

        val clamped = normalizeRate(value)
        val formatted = formatRate(clamped)
        ShizukuUtils.runCommand("settings put system $KEY_PEAK_REFRESH_RATE $formatted")
        ShizukuUtils.runCommand("settings put system $KEY_MIN_REFRESH_RATE $formatted")
        return true
    }

    fun applyRangeRefreshRate(minValue: Float, peakValue: Float): Boolean {
        if (!ShizukuUtils.hasPermission()) return false

        val safeMin = normalizeRate(minValue)
        val safePeak = normalizeRate(maxOf(minValue, peakValue))
        ShizukuUtils.runCommand("settings put system $KEY_MIN_REFRESH_RATE ${formatRate(safeMin)}")
        ShizukuUtils.runCommand("settings put system $KEY_PEAK_REFRESH_RATE ${formatRate(safePeak)}")
        return true
    }

    fun resetRefreshRate(restoreInfinityPeak: Boolean = false): Boolean {
        if (!ShizukuUtils.hasPermission()) return false

        // Clear both namespaces first, then restore the original system-managed peak behavior.
        ShizukuUtils.runCommand("settings delete system $KEY_MIN_REFRESH_RATE")
        ShizukuUtils.runCommand("settings delete system $KEY_PEAK_REFRESH_RATE")
        ShizukuUtils.runCommand("settings delete global $KEY_PEAK_REFRESH_RATE")
        ShizukuUtils.runCommand("settings delete global $KEY_MIN_REFRESH_RATE")
        if (restoreInfinityPeak) {
            ShizukuUtils.runCommand("settings put system $KEY_PEAK_REFRESH_RATE Infinity")
        }
        return true
    }

    fun getNextPreset(context: Context): Int {
        val currentValue = getDisplayValue(context).roundToInt()
        val currentIndex = PRESET_RATES.indexOf(currentValue)
        return when {
            currentIndex != -1 -> PRESET_RATES.getOrElse(currentIndex + 1) { 0 }
            currentValue <= 0 -> PRESET_RATES.first()
            else -> PRESET_RATES.firstOrNull { it > currentValue } ?: 0
        }
    }

    fun normalizeRate(value: Float): Float {
        val rounded = value.roundToInt()
        return rounded.coerceIn(10, 120).toFloat()
    }

    fun getCurrentState(context: Context): RefreshRateState {
        val maxRefreshRate = getHighestSupportedRefreshRate(context)
        val rawMin = getSystemString(context, KEY_MIN_REFRESH_RATE)
        val rawPeak = getSystemString(context, KEY_PEAK_REFRESH_RATE)

        val min = parseRefreshRate(rawMin, maxRefreshRate)
        val peak = parseRefreshRate(rawPeak, maxRefreshRate)
        val isSystemManaged = isSystemManagedState(rawMin, min, rawPeak, peak)

        return if (isSystemManaged) {
            RefreshRateState(
                min = 0f,
                peak = 0f,
                isSystemManaged = true,
                usesInfinityDefaultPeak = isInfinityValue(rawPeak)
            )
        } else {
            RefreshRateState(
                min = min,
                peak = peak,
                isSystemManaged = false,
                usesInfinityDefaultPeak = false
            )
        }
    }

    private fun getSystemString(context: Context, key: String): String? {
        return try {
            Settings.System.getString(context.contentResolver, key)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRefreshRate(rawValue: String?, fallbackForInfinity: Float): Float {
        val trimmed = rawValue?.trim().orEmpty()
        if (trimmed.isEmpty()) return 0f
        if (trimmed.equals("Infinity", ignoreCase = true)) return fallbackForInfinity

        val parsed = trimmed.toFloatOrNull() ?: return 0f
        return when {
            !parsed.isFinite() -> fallbackForInfinity
            parsed <= 0f -> 0f
            else -> parsed
        }
    }

    private fun isSystemManagedState(
        rawMin: String?,
        min: Float,
        rawPeak: String?,
        peak: Float
    ): Boolean {
        val isMinUnset = min <= 0f && isUnsetValue(rawMin)
        if (!isMinUnset) return false

        return isUnsetValue(rawPeak) ||
                isInfinityValue(rawPeak) ||
                peak <= 0f ||
                peak.roundToInt() == DEFAULT_SYSTEM_REFRESH_RATE.roundToInt()
    }

    private fun isUnsetValue(rawValue: String?): Boolean {
        val trimmed = rawValue?.trim().orEmpty()
        return trimmed.isEmpty() || trimmed == "0" || trimmed == "0.0"
    }

    private fun isInfinityValue(rawValue: String?): Boolean {
        val trimmed = rawValue?.trim().orEmpty()
        return trimmed.equals("Infinity", ignoreCase = true) ||
                trimmed.equals("inf", ignoreCase = true)
    }

    private fun getHighestSupportedRefreshRate(context: Context): Float {
        return try {
            val displayManager = context.getSystemService(DisplayManager::class.java)
            val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
            display?.supportedModes
                ?.maxOfOrNull { it.refreshRate }
                ?.takeIf { it.isFinite() && it > 0f }
                ?: DEFAULT_SYSTEM_REFRESH_RATE
        } catch (_: Exception) {
            DEFAULT_SYSTEM_REFRESH_RATE
        }
    }

    private fun formatRate(value: Float): String {
        return String.format(Locale.US, "%.0f", value)
    }
}
