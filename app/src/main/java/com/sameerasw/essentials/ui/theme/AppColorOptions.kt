/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: AppColorOptions.kt
 * Description: Seed and palette style presets for the app color setting.
 */

package com.sameerasw.essentials.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import com.sameerasw.essentials.R

enum class AppColorVariant(
    val style: PaletteStyle,
    @StringRes val labelRes: Int,
) {
    Soft(PaletteStyle.TonalSpot, R.string.app_color_variant_soft),
    Bright(PaletteStyle.Vibrant, R.string.app_color_variant_bright),
    Neutral(PaletteStyle.Neutral, R.string.app_color_variant_neutral),
    ;

    companion object {
        fun from(name: String?): AppColorVariant = entries.firstOrNull { it.name == name } ?: Bright
    }
}

class AppColorOption(
    val seedColor: Color,
    val mixStyle: PaletteStyle? = null,
) {
    val seed: Int = seedColor.toArgb()
    val isMix: Boolean get() = mixStyle != null

    fun styleFor(variant: AppColorVariant): PaletteStyle = mixStyle ?: variant.style
}

val AppColorOptions =
    listOf(
        AppColorOption(Color(0xFF7C4DFF)),
        AppColorOption(Color(0xFFD500F9)),
        AppColorOption(Color(0xFFF50057)),
        AppColorOption(Color(0xFFFF1744)),
        AppColorOption(Color(0xFFFF5722)),
        AppColorOption(Color(0xFFFF6D00)),
        AppColorOption(Color(0xFFFFAB00)),
        AppColorOption(Color(0xFFFFD600)),
        AppColorOption(Color(0xFFAEEA00)),
        AppColorOption(Color(0xFF64DD17)),
        AppColorOption(Color(0xFF00C853)),
        AppColorOption(Color(0xFF00BFA5)),
        AppColorOption(Color(0xFF00B8D4)),
        AppColorOption(Color(0xFF00B0FF)),
        AppColorOption(Color(0xFF2979FF)),
        AppColorOption(Color(0xFF536DFE)),
        AppColorOption(Color(0xFF8D6E63)),
        AppColorOption(Color(0xFF78909C)),
        AppColorOption(Color(0xFFFF8A80)),
        AppColorOption(Color(0xFFF48FB1)),
        AppColorOption(Color(0xFFCE93D8)),
        AppColorOption(Color(0xFF9FA8DA)),
        AppColorOption(Color(0xFF80DEEA)),
        AppColorOption(Color(0xFFA5D6A7)),
        AppColorOption(Color(0xFFFF6D00), PaletteStyle.Expressive),
        AppColorOption(Color(0xFF2979FF), PaletteStyle.Expressive),
        AppColorOption(Color(0xFFF50057), PaletteStyle.Expressive),
        AppColorOption(Color(0xFF00C853), PaletteStyle.Expressive),
        AppColorOption(Color(0xFF7C4DFF), PaletteStyle.Expressive),
        AppColorOption(Color(0xFFFFD600), PaletteStyle.Expressive),
        AppColorOption(Color(0xFF00B8D4), PaletteStyle.Expressive),
        AppColorOption(Color(0xFFFF1744), PaletteStyle.Expressive),
        AppColorOption(Color(0xFF2979FF), PaletteStyle.FruitSalad),
        AppColorOption(Color(0xFF00C853), PaletteStyle.FruitSalad),
        AppColorOption(Color(0xFFF50057), PaletteStyle.FruitSalad),
        AppColorOption(Color(0xFFFF6D00), PaletteStyle.FruitSalad),
        AppColorOption(Color(0xFF7C4DFF), PaletteStyle.FruitSalad),
        AppColorOption(Color(0xFF00B8D4), PaletteStyle.FruitSalad),
        AppColorOption(Color(0xFFFFAB00), PaletteStyle.FruitSalad),
        AppColorOption(Color(0xFFD500F9), PaletteStyle.FruitSalad),
        AppColorOption(Color(0xFF7C4DFF), PaletteStyle.Rainbow),
        AppColorOption(Color(0xFFFF1744), PaletteStyle.Rainbow),
        AppColorOption(Color(0xFF00B8D4), PaletteStyle.Rainbow),
        AppColorOption(Color(0xFFFFAB00), PaletteStyle.Rainbow),
        AppColorOption(Color(0xFF00C853), PaletteStyle.Rainbow),
        AppColorOption(Color(0xFFF50057), PaletteStyle.Rainbow),
        AppColorOption(Color(0xFF2979FF), PaletteStyle.Rainbow),
        AppColorOption(Color(0xFFFF6D00), PaletteStyle.Rainbow),
    )

fun paletteStyleOrDefault(name: String?): PaletteStyle =
    PaletteStyle.entries.firstOrNull { it.name == name } ?: PaletteStyle.TonalSpot
