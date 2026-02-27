package com.sameerasw.essentials.utils

import android.graphics.Color
import androidx.core.graphics.ColorUtils as AndroidColorUtils

object ColorFormatUtils {

    enum class ColorFormat {
        HEX, RGB, HSL, HSV
    }

    fun formatColor(color: Int, format: ColorFormat): String {
        return when (format) {
            ColorFormat.HEX -> String.format("#%06X", 0xFFFFFF and color)
            ColorFormat.RGB -> {
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                "rgb($r, $g, $b)"
            }
            ColorFormat.HSL -> {
                val hsl = FloatArray(3)
                AndroidColorUtils.colorToHSL(color, hsl)
                String.format("hsl(%.0f, %.0f%%, %.0f%%)", hsl[0], hsl[1] * 100, hsl[2] * 100)
            }
            ColorFormat.HSV -> {
                val hsv = FloatArray(3)
                Color.colorToHSV(color, hsv)
                String.format("hsv(%.0f, %.0f%%, %.0f%%)", hsv[0], hsv[1] * 100, hsv[2] * 100)
            }
        }
    }
}
