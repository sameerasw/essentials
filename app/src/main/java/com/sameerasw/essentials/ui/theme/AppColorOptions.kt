/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: AppColorOptions.kt
 * Description: Seed and palette style presets for the app color setting.
 */

package com.sameerasw.essentials.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle

class AppColorOption(
    val seedColor: Color,
    val style: PaletteStyle,
    val isMix: Boolean = false,
) {
    val seed: Int = seedColor.toArgb()
}

val AppColorOptions =
    listOf(
        AppColorOption(Color(0xFF7C4DFF), PaletteStyle.Vibrant),
        AppColorOption(Color(0xFFFF1744), PaletteStyle.Vibrant),
        AppColorOption(Color(0xFFFF6D00), PaletteStyle.Vibrant),
        AppColorOption(Color(0xFF00C853), PaletteStyle.Vibrant),
        AppColorOption(Color(0xFF00BFA5), PaletteStyle.Vibrant),
        AppColorOption(Color(0xFF2979FF), PaletteStyle.Vibrant),
        AppColorOption(Color(0xFFF50057), PaletteStyle.Vibrant),
        AppColorOption(Color(0xFFFF6D00), PaletteStyle.Expressive, isMix = true),
        AppColorOption(Color(0xFF2979FF), PaletteStyle.FruitSalad, isMix = true),
        AppColorOption(Color(0xFF7C4DFF), PaletteStyle.Rainbow, isMix = true),
    )

fun paletteStyleOrDefault(name: String?): PaletteStyle =
    PaletteStyle.entries.firstOrNull { it.name == name } ?: PaletteStyle.TonalSpot
