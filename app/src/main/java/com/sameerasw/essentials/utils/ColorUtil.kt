package com.sameerasw.essentials.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs

object ColorUtil {
    private val pastelColors = listOf(
        Color(0xFFF48FB1), Color(0xFFCE93D8), Color(0xFFB39DDB),
        Color(0xFF9FA8DA), Color(0xFF90CAF9), Color(0xFF81D4FA),
        Color(0xFF80DEEA), Color(0xFF80CBC4), Color(0xFFA5D6A7),
        Color(0xFFC5E1A5), Color(0xFFE6EE9C), Color(0xFFFFF59D),
        Color(0xFFFFE082), Color(0xFFFFCC80), Color(0xFFFFAB91),
        Color(0xFFBCAAA4), Color(0xFFB0BEC5)
    )

    fun getPastelColorFor(key: Any): Color {
        val hash = abs(key.hashCode())
        val index = hash % pastelColors.size
        return pastelColors[index]
    }

    /**
     * Takes a pastel color and returns a more saturated,
     * vibrant version suitable for icons/text.
     */
    fun getVibrantColorFor(key: Any): Color {
        val baseColor = getPastelColorFor(key)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)

        // Increase Saturation: Pastels are usually ~0.3, we want ~0.7 to 0.8
        hsv[1] = (hsv[1] * 5f).coerceIn(0f, 5f)

        // Lower Value slightly: Makes the color "richer" and better for contrast
        hsv[2] = (hsv[2] * 0.7f).coerceIn(0f, 1f)

        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}