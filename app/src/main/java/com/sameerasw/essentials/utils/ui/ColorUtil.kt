/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Utilities
 * File: ColorUtil.kt
 * Description: Generates pastel background and vibrant icon tint pairs.
 */

package com.sameerasw.essentials.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs

object ColorUtil {
    private val pastelColors =
        listOf(
            Color(0xFFF8BBD0),
            Color(0xFFE1BEE7),
            Color(0xFFD1C4E9),
            Color(0xFFC5CAE9),
            Color(0xFFBBDEFB),
            Color(0xFFB3E5FC),
            Color(0xFFB2EBF2),
            Color(0xFFB2DFDB),
            Color(0xFFC8E6C9),
            Color(0xFFDCEDC8),
            Color(0xFFF0F4C3),
            Color(0xFFFFF9C4),
            Color(0xFFFFECB3),
            Color(0xFFFFE0B2),
            Color(0xFFFFCCBC),
            Color(0xFFD7CCC8),
            Color(0xFFCFD8DC),
        )

    fun getPastelColorFor(key: Any): Color {
        val hash = abs(key.hashCode())
        val index = hash % pastelColors.size
        return pastelColors[index]
    }

    /**
     * Takes a pastel color and returns a richer, darker, highly saturated
     * version suitable for text, icons, and filter chips in all themes.
     */
    fun toRichColor(color: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)

        // Boost saturation for vividness
        hsv[1] = (hsv[1] * 2.5f).coerceIn(0.6f, 1f)

        // Darken tone for contrast
        hsv[2] = (hsv[2] * 0.65f).coerceIn(0.2f, 0.75f)

        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    /**
     * Takes a key and returns a more saturated,
     * vibrant version suitable for icons/text.
     */
    fun getVibrantColorFor(key: Any): Color {
        val baseColor = getPastelColorFor(key)
        return toRichColor(baseColor)
    }
}
