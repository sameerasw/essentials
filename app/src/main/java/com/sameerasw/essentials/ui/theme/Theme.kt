/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: Theme.kt
 * Description: UI layout element for Theme.kt.
 */

package com.sameerasw.essentials.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.sameerasw.essentials.data.repository.SettingsRepository
import androidx.compose.ui.platform.LocalHapticFeedback
import com.sameerasw.essentials.utils.HapticUtil
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40,
    )

private fun ColorScheme.asPitchBlack(): ColorScheme =
    copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceContainer = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color.Black,
    )

@Composable
private fun rememberAppColor(): Pair<Int, PaletteStyle> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE) }
    fun read() =
        prefs.getInt(SettingsRepository.KEY_APP_COLOR_SEED, 0) to
            paletteStyleOrDefault(prefs.getString(SettingsRepository.KEY_APP_COLOR_STYLE, null))
    var color by remember { mutableStateOf(read()) }
    DisposableEffect(prefs) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == SettingsRepository.KEY_APP_COLOR_SEED || key == SettingsRepository.KEY_APP_COLOR_STYLE) {
                    color = read()
                }
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return color
}

@Composable
fun EssentialsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pitchBlackTheme: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val (seed, style) = rememberAppColor()
    val baseScheme =
        when {
            dynamicColor && seed != 0 ->
                remember(seed, style, darkTheme) {
                    dynamicColorScheme(
                        seedColor = Color(seed),
                        isDark = darkTheme,
                        style = style,
                    )
                }

            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    val colorScheme = if (darkTheme && pitchBlackTheme) baseScheme.asPitchBlack() else baseScheme

    val gatedHaptics = HapticUtil.gate(LocalContext.current, LocalHapticFeedback.current)
    CompositionLocalProvider(LocalHapticFeedback provides gatedHaptics) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
