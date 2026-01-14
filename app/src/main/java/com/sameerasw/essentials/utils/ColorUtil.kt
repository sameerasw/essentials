package com.sameerasw.essentials.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

object ColorUtil {
    private val pastelColors = listOf(
        Color(0xFFF48FB1), // Pink
        Color(0xFFCE93D8), // Purple
        Color(0xFFB39DDB), // Deep Purple
        Color(0xFF9FA8DA), // Indigo
        Color(0xFF90CAF9), // Blue
        Color(0xFF81D4FA), // Light Blue
        Color(0xFF80DEEA), // Cyan
        Color(0xFF80CBC4), // Teal
        Color(0xFFA5D6A7), // Green
        Color(0xFFC5E1A5), // Light Green
        Color(0xFFE6EE9C), // Lime
        Color(0xFFFFF59D), // Yellow
        Color(0xFFFFE082), // Amber
        Color(0xFFFFCC80), // Orange
        Color(0xFFFFAB91), // Deep Orange
        Color(0xFFBCAAA4), // Brown
        Color(0xFFB0BEC5)  // Blue Grey
    )

    fun getPastelColorFor(key: Any): Color {
        val hash = abs(key.hashCode())
        val index = hash % pastelColors.size
        return pastelColors[index]
    }
}
