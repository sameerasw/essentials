/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Radial Menus
 * File: QuickBallTheme.kt
 * Description: Theming models, ball visual styles, and color resolvers for Quick Ball components.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.toArgb
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import kotlin.math.roundToInt

@Keep
enum class QuickBallColorMode(@field:StringRes val titleRes: Int) {
    DYNAMIC(R.string.quick_ball_color_mode_dynamic),
    CUSTOM(R.string.quick_ball_color_mode_custom);

    companion object {
        fun fromName(name: String?): QuickBallColorMode {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DYNAMIC
        }
    }
}

@Keep
enum class QuickBallBallStyle(@field:StringRes val titleRes: Int) {
    CLASSIC(R.string.quick_ball_style_classic),
    OUTLINE(R.string.quick_ball_style_outline),
    SQUIRCLE(R.string.quick_ball_style_squircle),
    NEON(R.string.quick_ball_style_neon),
    MINIMAL(R.string.quick_ball_style_minimal);

    companion object {
        fun fromName(name: String?): QuickBallBallStyle {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CLASSIC
        }
    }
}

data class QuickBallColors(
    val surfaceColor: Int,
    val accentColor: Int,
    val borderColor: Int,
    val contentColor: Int,
)

object QuickBallTheme {

    fun resolveColors(context: Context, settings: SettingsRepository): QuickBallColors {
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val mode = QuickBallColorMode.fromName(settings.getQuickBallColorMode())

        // 1. Resolve dynamic base scheme
        val dynamicScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (isDark) darkColorScheme() else lightColorScheme()
        }

        if (mode == QuickBallColorMode.DYNAMIC) {
            return QuickBallColors(
                surfaceColor = dynamicScheme.surfaceContainerHighest.toArgb(),
                accentColor = dynamicScheme.primary.toArgb(),
                borderColor = dynamicScheme.outlineVariant.toArgb(),
                contentColor = dynamicScheme.primary.toArgb(),
            )
        }

        // 2. Resolve custom color
        val customHex = settings.getQuickBallCustomColor()
        val accent = try {
            if (customHex.equals("auto", ignoreCase = true)) {
                dynamicScheme.primary.toArgb()
            } else {
                AndroidColor.parseColor(customHex)
            }
        } catch (_: Exception) {
            dynamicScheme.primary.toArgb()
        }

        // Derive harmonious surface and border from custom accent
        val r = AndroidColor.red(accent)
        val g = AndroidColor.green(accent)
        val b = AndroidColor.blue(accent)

        val surface = if (isDark) {
            // Dark tinted surface (18% accent blended with dark surface)
            AndroidColor.rgb(
                (r * 0.18f + 24 * 0.82f).roundToInt(),
                (g * 0.18f + 24 * 0.82f).roundToInt(),
                (b * 0.18f + 24 * 0.82f).roundToInt(),
            )
        } else {
            // Light tinted surface (12% accent blended with white surface)
            AndroidColor.rgb(
                (r * 0.12f + 245 * 0.88f).roundToInt(),
                (g * 0.12f + 245 * 0.88f).roundToInt(),
                (b * 0.12f + 245 * 0.88f).roundToInt(),
            )
        }

        val border = if (isDark) {
            AndroidColor.argb(
                120,
                (r * 0.6f + 200 * 0.4f).roundToInt(),
                (g * 0.6f + 200 * 0.4f).roundToInt(),
                (b * 0.6f + 200 * 0.4f).roundToInt(),
            )
        } else {
            AndroidColor.argb(
                120,
                (r * 0.5f + 80 * 0.5f).roundToInt(),
                (g * 0.5f + 80 * 0.5f).roundToInt(),
                (b * 0.5f + 80 * 0.5f).roundToInt(),
            )
        }

        return QuickBallColors(
            surfaceColor = surface,
            accentColor = accent,
            borderColor = border,
            contentColor = accent,
        )
    }
}
