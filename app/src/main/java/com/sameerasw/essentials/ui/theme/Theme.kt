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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.sameerasw.essentials.data.repository.SettingsRepository
import androidx.compose.ui.platform.LocalHapticFeedback
import com.sameerasw.essentials.utils.DeviceUtils
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

private fun Color.monochrome(): Color {
    val gray = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return Color(gray, gray, gray, alpha)
}

private fun ColorScheme.asMonochromeSurfaces(darkTheme: Boolean): ColorScheme =
    copy(
        background = if (darkTheme) background.monochrome() else surfaceContainer,
        surface = if (darkTheme) surface.monochrome() else surfaceContainer,
        surfaceDim = surfaceDim.monochrome(),
        surfaceBright =
            if (darkTheme) {
                lerp(surfaceBright.monochrome(), primary, 0.08f)
            } else {
                lerp(surfaceContainerLowest.monochrome(), primary, 0.02f)
            },
        surfaceContainerLowest = surfaceContainerLowest.monochrome(),
        surfaceContainerLow = surfaceContainerLow.monochrome(),
        surfaceContainer = surfaceContainer.monochrome(),
        surfaceContainerHigh = surfaceContainerHigh.monochrome(),
        surfaceContainerHighest = surfaceContainerHighest.monochrome(),
        surfaceVariant = surfaceVariant.monochrome(),
        surfaceTint = surfaceTint.monochrome(),
        inverseSurface = inverseSurface.monochrome(),
        outline = outline.monochrome(),
        outlineVariant = outlineVariant.monochrome(),
        onBackground = onBackground.monochrome(),
        onSurface = onSurface.monochrome(),
        onSurfaceVariant = onSurfaceVariant.monochrome(),
        inverseOnSurface = inverseOnSurface.monochrome(),
    )

val LocalBlurEnabled = staticCompositionLocalOf { false }

@Composable
private fun rememberBlurEnabled(): Boolean {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE) }
    fun read() =
        prefs.getBoolean(SettingsRepository.KEY_USE_BLUR, true) &&
            !DeviceUtils.isBlurProblematicDevice() &&
            !DeviceUtils.isPowerSaveMode(context)
    var enabled by remember { mutableStateOf(read()) }
    DisposableEffect(prefs) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == SettingsRepository.KEY_USE_BLUR) enabled = read()
            }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return enabled
}

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
    SystemFontState.init(LocalContext.current)
    UiDesignState.init(LocalContext.current)
    val useSystemFont = SystemFontState.enabled
    val uiDesign = UiDesignState.resolved
    val blurEnabled = rememberBlurEnabled()
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
    val designScheme = if (uiDesign == UiDesign.ONE_UI) baseScheme.asMonochromeSurfaces(darkTheme) else baseScheme
    val colorScheme =
        if (darkTheme && pitchBlackTheme) designScheme.asPitchBlack() else designScheme

    val gatedHaptics = HapticUtil.gate(LocalContext.current, LocalHapticFeedback.current)
    CompositionLocalProvider(
        LocalHapticFeedback provides gatedHaptics,
        LocalUiDesign provides uiDesign,
        LocalBlurEnabled provides blurEnabled,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = if (useSystemFont) Typography.withFontFamily(FontFamily.Default) else Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
