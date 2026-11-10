/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Radial Menus
 * File: QuickBallTheme.kt
 * Description: Theming models, preset themes (57 palettes), ball visual styles, and color resolvers for Quick Ball.
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
    PRESET(R.string.quick_ball_color_mode_preset),
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

@Keep
enum class QuickBallPillStyle(
    @field:StringRes val titleRes: Int,
    @field:StringRes val descRes: Int,
) {
    MIUI_ARC(R.string.quick_ball_pill_style_miui, R.string.quick_ball_pill_style_miui_desc),
    IOS_CAPSULE(R.string.quick_ball_pill_style_ios, R.string.quick_ball_pill_style_ios_desc),
    ANDROID_BAR(R.string.quick_ball_pill_style_android, R.string.quick_ball_pill_style_android_desc),
    SAMSUNG_EDGE(R.string.quick_ball_pill_style_samsung, R.string.quick_ball_pill_style_samsung_desc),
    ONEPLUS_SLIDER(R.string.quick_ball_pill_style_oneplus, R.string.quick_ball_pill_style_oneplus_desc),
    NOTHING_GLYPH(R.string.quick_ball_pill_style_nothing, R.string.quick_ball_pill_style_nothing_desc),
    MINIMAL_DOT(R.string.quick_ball_pill_style_dot, R.string.quick_ball_pill_style_dot_desc);

    companion object {
        fun fromName(name: String?): QuickBallPillStyle {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MIUI_ARC
        }
    }
}

private const val FILL_ALPHA = 0xBF
private fun fill(rgb: Long): Int = (FILL_ALPHA shl 24) or (rgb.toInt() and 0xFFFFFF)
private fun opaque(rgb: Long): Int = (0xFF shl 24) or (rgb.toInt() and 0xFFFFFF)
private const val PILL_ALPHA = 0xCC
private fun pill(rgb: Long): Int = (PILL_ALPHA shl 24) or (rgb.toInt() and 0xFFFFFF)

@Keep
enum class QuickBallPresetTheme(
    val title: String,
    val ballColor: Int,
    val ballIconColor: Int,
    val ballBorderColor: Int,
    val menuColor: Int,
    val menuIconColor: Int,
    val menuBorderColor: Int,
    val connectorColor: Int,
    val pillColor: Int,
) {
    DEFAULT(
        title = "Default",
        ballColor = 0xBF2C2C2C.toInt(),
        ballIconColor = 0xFFFFFFFF.toInt(),
        ballBorderColor = 0x66FFFFFF,
        menuColor = 0xBF2C2C2C.toInt(),
        menuIconColor = 0xFFFFFFFF.toInt(),
        menuBorderColor = 0x66FFFFFF,
        connectorColor = 0x802C2C2C.toInt(),
        pillColor = 0xCC777777.toInt(),
    ),
    ONYX("Onyx", ring = 0xE9ECF0, itemRing = 0x363C45, base = 0x0B0D10, icon = 0xB4BAC4, connector = 0x4ADE80),
    QUARTZ("Quartz", ring = 0x14171C, itemRing = 0x8B929C, base = 0xECEEF1, icon = 0x3E444D, connector = 0x16A34A),
    BLACK("Black", ring = 0xFFFFFF, itemRing = 0xA0A0A0, base = 0x000000, icon = 0xFFFFFF, connector = 0x404040),
    WHITE("White", ring = 0x000000, itemRing = 0x606060, base = 0xFFFFFF, icon = 0x000000, connector = 0xC0C0C0),
    PULSE("Pulse", ring = 0x86F7FA, itemRing = 0x1A1A3A, base = 0x0D0D1E, icon = 0xC5A3FF, connector = 0x4A4A6F),
    NIGHT_CITY("Night City", ring = 0x00F0FF, itemRing = 0x036970, base = 0x030D1A, icon = 0xFAFF66, connector = 0x1BD677),
    NOTHING_DARK("Nothing Dark", ring = 0xFFFFFF, itemRing = 0xD71921, base = 0x0B0B10, icon = 0xFFFFFF, connector = 0x3A3A40),
    NOTHING_LIGHT("Nothing Light", ring = 0xD71921, itemRing = 0x1F2022, base = 0xF5F5F5, icon = 0x1B1B1D, connector = 0xC0C0C5),
    GAME_BOY("Game Boy", ring = 0x8BAC0F, itemRing = 0x306230, base = 0x080F05, icon = 0x9BBC0F, connector = 0x5A8420),
    SUNRISE("Sunrise", ring = 0xFF6B35, itemRing = 0xFFF0DC, base = 0xFFF8EE, icon = 0x8B3A00, connector = 0xCFA370),
    DUNE_HARVEST("Dune Harvest", ring = 0xF2E0C7, itemRing = 0xEE8A33, base = 0x0C1C2B, icon = 0xF2E0C7, connector = 0x3A434A),
    COTTON_CANDY("Cotton Candy", ring = 0xFC9E94, itemRing = 0x9ABFD3, base = 0xE9FEDB, icon = 0x141414, connector = 0xC2FC9B),
    CLASSIC_95("Classic 95", ring = 0x000080, itemRing = 0x808080, base = 0xC0C0C0, icon = 0x000000, connector = 0x008080),
    MUSTARD("Mustard", ring = 0xFF0000, itemRing = 0x808000, base = 0xFFFF00, icon = 0x000000, connector = 0x000080),
    PLUM("Plum", ring = 0xC9A3C9, itemRing = 0x7E5F7E, base = 0x4A374A, icon = 0xF0E6F0, connector = 0x6B8E23),
    WINDOWS_2000("Windows 2000", ring = 0x0A246A, itemRing = 0x808080, base = 0xD4D0C8, icon = 0x000000, connector = 0x3A6EA5),
    TACTICAL("Tactical", ring = 0x9DC183, itemRing = 0x4B5320, base = 0x1B1B1B, icon = 0xAFA79D, connector = 0x6D7A58),
    ICE_CAVE("Ice Cave", ring = 0xBBEEFF, itemRing = 0x1A3A5C, base = 0x010508, icon = 0x88CCEE, connector = 0x3B7095),
    TEAL("Teal", ring = 0x1DE9B6, itemRing = 0x042828, base = 0x021211, icon = 0x80CBC4, connector = 0x2B786D),
    NORDIC("Nordic", ring = 0x38BDF8, itemRing = 0x1E3A8A, base = 0x0F172A, icon = 0xCBD5E1, connector = 0x475569),
    SAGE("Sage", ring = 0x15803D, itemRing = 0xDCFCE7, base = 0xF1F5F0, icon = 0x0F361E, connector = 0xA3C2A6),
    GOLD("Gold", ring = 0xB8860B, itemRing = 0xF5E6AA, base = 0xFDFCF9, icon = 0x2B1F00, connector = 0xCFA350),
    DESERT_SAGE("Desert Sage", ring = 0xEACF98, itemRing = 0xC2BD97, base = 0x2C4545, icon = 0xEACF98, connector = 0x526055),
    TEAL_HORIZON("Teal Horizon", ring = 0xC7DDE3, itemRing = 0xEDC35D, base = 0x254245, icon = 0xC7DDE3, connector = 0x456164),
    NORDIC_SPRUCE("Nordic Spruce", ring = 0xFDFAF3, itemRing = 0xF8BA76, base = 0x0C181E, icon = 0xFDFAF3, connector = 0x3C4548),
    ORANGE_ZEST("Orange Zest", ring = 0xFDE7CF, itemRing = 0xDCBB9E, base = 0x323232, icon = 0xFDE7CF, connector = 0x5A5651),
    VINTAGE_VELVET("Vintage Velvet", ring = 0xF4FAAD, itemRing = 0xE4D78C, base = 0x28112B, icon = 0xF4FAAD, connector = 0x503F45),
    SEA_BREEZE("Sea Breeze", ring = 0xDAF9F2, itemRing = 0x89ECF4, base = 0x070C07, icon = 0xDAF9F2, connector = 0x313B36),
    FOREST_FERN("Forest Fern", ring = 0xE7E6DF, itemRing = 0xCEDC71, base = 0x1C282D, icon = 0xE7E6DF, connector = 0x444E50),
    MEADOW_BLOOM("Meadow Bloom", ring = 0x7CAD17, itemRing = 0x27C5B1, base = 0xF3F9E5, icon = 0x141414, connector = 0xD8EBAC),
    LEMON_HONEY("Lemon Honey", ring = 0xFCB468, itemRing = 0xFDE0C1, base = 0xF7FBE1, icon = 0x141414, connector = 0xE7F3A5),
    SPICED_PLUM("Spiced Plum", ring = 0xCEB895, itemRing = 0xDF9712, base = 0x140714, icon = 0xCEB895, connector = 0x381338),
    SANDSTONE_SLATE("Sandstone Slate", ring = 0xE8DFD2, itemRing = 0xCFC3AB, base = 0x121212, icon = 0xE8DFD2, connector = 0x3C3B38),
    MULBERRY_WINE("Mulberry Wine", ring = 0xFDFCFC, itemRing = 0xBFBCBD, base = 0x141010, icon = 0xFDFCFC, connector = 0x423F3F),
    HIVE("Hive", ring = 0xF8D152, itemRing = 0xAF6301, base = 0x1C0F05, icon = 0xF7E9C9, connector = 0x8FBF3F),
    SENSU("Sensu", ring = 0xE24E2B, itemRing = 0xB98A4E, base = 0x1A1310, icon = 0xF3E7CE, connector = 0x4C74C4),
    TRINE("Trine", ring = 0xFF7A59, itemRing = 0x5E7186, base = 0x10141A, icon = 0xEAF0F6, connector = 0x24A597),
    JARVIS("Jarvis", ring = 0x35D6FF, itemRing = 0x2C5A70, base = 0x06121A, icon = 0xCDEFFF, connector = 0xFFB74D),
    ASCII("Ascii", ring = 0x33FF66, itemRing = 0x2E6B3E, base = 0x0A0F0A, icon = 0xB6FFC8, connector = 0xFFCC33),
    NEBULA("Nebula", ring = 0xE85D9C, itemRing = 0x4FC3F7, base = 0x0A0714, icon = 0xEDE7F6, connector = 0x26C6DA),
    CONSOLE("Console", ring = 0xFFB000, itemRing = 0x6B4A12, base = 0x120C03, icon = 0xFFD98A, connector = 0xFFE9B0),
    AURA("Aura", ring = 0xFF5FA2, itemRing = 0x9B6BFF, base = 0x0A0614, icon = 0xF2E6FF, connector = 0x6E4AA8),
    ANIMUS("Animus", ring = 0x7FD4F5, itemRing = 0x2E5C7A, base = 0x04070C, icon = 0xDCF3FF, connector = 0x46B3A8),
    HORIZON("Horizon", ring = 0xFFA94D, itemRing = 0x2A3550, base = 0x0A0F1E, icon = 0xF0E4D2, connector = 0xFFD08A),
    POCKET("Pocket", ring = 0x1F3D33, itemRing = 0x5E7A55, base = 0xC4D4A4, icon = 0x1F3D33, connector = 0x2F5D45),
    WHITEROOM("Whiteroom", ring = 0x2C6E8F, itemRing = 0xA8B6C0, base = 0xF2F5F7, icon = 0x1C3A4A, connector = 0x2E7D74),
    LIME_ORCHID("Lime Orchid", ring = 0xCFDE67, itemRing = 0x6E7A44, base = 0x161819, icon = 0xEDE4D1, connector = 0xD8A24A),
    PIXEL_COSMOS("Pixel Cosmos", ring = 0x00EEFF, itemRing = 0x8800FF, base = 0x020008, icon = 0xE0D0FF, connector = 0xFFB300),
    ABYSS("Abyss", ring = 0x6B4FA0, itemRing = 0x3B2A6B, base = 0x05050A, icon = 0xF5F5F5, connector = 0xC9A227),
    CIRCUIT("Circuit", ring = 0xFF4FA3, itemRing = 0xCFD3D8, base = 0x1B1C1E, icon = 0xF1F3E8, connector = 0x4FB25A),
    OSH_PURPLE("OSH Purple", ring = 0x7CFFCB, itemRing = 0xD2D5DB, base = 0x17151C, icon = 0xF2EEFA, connector = 0x9070CC),
    BLUEPRINT("Blueprint", ring = 0x7FE3FF, itemRing = 0xD6DAE0, base = 0x15181D, icon = 0xF4F8FC, connector = 0x3A86D0),
    REDLINE("Redline", ring = 0x7FDFFF, itemRing = 0xD3D6DB, base = 0x1A1718, icon = 0xFFF1F4, connector = 0xE04A3C),
    UNO("Uno", ring = 0xFFB000, itemRing = 0xD0D4D9, base = 0x16191B, icon = 0xF2FBFB, connector = 0x1FA2AA),
    DEEP_SEA("Deep Sea", ring = 0x00F0D0, itemRing = 0x007B8A, base = 0x00060F, icon = 0x66DDCC, connector = 0x33A39A),
    SYNTHWAVE("Synthwave", ring = 0x00F5D4, itemRing = 0xAA2266, base = 0x1A0030, icon = 0xFF2079, connector = 0x00BFA5),
    SUNSET_GLOW("Sunset Glow", ring = 0xFF8800, itemRing = 0xCC0066, base = 0x080005, icon = 0xFF88AA, connector = 0xAA3377),
    WARPED_FOREST("Warped Forest", ring = 0x5AF2F2, itemRing = 0x163836, base = 0x063230, icon = 0xEDBD8C, connector = 0x368E8B),
    OLD_MONEY("Old Money", ring = 0x6F4226, itemRing = 0xE8CDA0, base = 0xFAF0E4, icon = 0x3E1F0A, connector = 0xA88C74),
    CORAL_REEF("Coral Reef", ring = 0xF35156, itemRing = 0x006C5E, base = 0x04332C, icon = 0xBDF2EF, connector = 0xE8A33D),
    TWILIGHT_MOSS("Twilight Moss", ring = 0x9B71B2, itemRing = 0x6C6D11, base = 0x24121F, icon = 0xE3D0EA, connector = 0x4FB8A8),
    RADIUM("Radium", ring = 0xCCFF00, itemRing = 0x6E6E00, base = 0x000000, icon = 0xDFDFA8, connector = 0x00C2FF),
    CYBER_LIME("Cyber Lime", ring = 0x22C55E, itemRing = 0x1E293B, base = 0x0A0F1D, icon = 0xD4E6D9, connector = 0x475569),
    TOKYO("Tokyo", ring = 0x06B6D4, itemRing = 0x172554, base = 0x090B18, icon = 0xE0F7FA, connector = 0x3B4D75),
    BLUSH("Blush", ring = 0xBE123C, itemRing = 0xFFE4E6, base = 0xFFF0F1, icon = 0x5E0B25, connector = 0xE0A2A6),
    PASTEL_ROSE("Pastel Rose", ring = 0xF0C1D3, itemRing = 0xF8ACA3, base = 0x0C0907, icon = 0xF0C1D3, connector = 0x392D2F),
    ORCHID_MIST("Orchid Mist", ring = 0xF6E8DD, itemRing = 0xC3B5CD, base = 0x543E54, icon = 0xF6E8DD, connector = 0x74606F),
    STEEL_BLUE("Steel Blue", ring = 0xF2E5D4, itemRing = 0xD6C8C3, base = 0x0B0809, icon = 0xF2E5D4, connector = 0x393431),
    RUST_COPPER("Rust Copper", ring = 0xE97618, itemRing = 0x9C9089, base = 0x150D08, icon = 0xE97618, connector = 0x372215),
    PINK_SHERBET("Pink Sherbet", ring = 0x64695E, itemRing = 0xF47198, base = 0xF9F9F8, icon = 0x64695E, connector = 0xCFCFC8),
    EVERGREEN("Evergreen", ring = 0xE5D697, itemRing = 0xD3902C, base = 0x0C180D, icon = 0xE5D697, connector = 0x373E28),
    CLAY_CANYON("Clay Canyon", ring = 0xD3C9C0, itemRing = 0xE59E4F, base = 0x161E1F, icon = 0xD3C9C0, connector = 0x3B403F),
    CRIMSON_TIDE("Crimson Tide", ring = 0xEDA793, itemRing = 0xF27A62, base = 0x2A2C3D, icon = 0xEDA793, connector = 0x1F202D),
    LAGOON_SPLASH("Lagoon Splash", ring = 0xE3EEEB, itemRing = 0xBBA4AD, base = 0x1F3846, icon = 0xE3EEEB, connector = 0x465C67),
    BUBBLEGUM_SKY("Bubblegum Sky", ring = 0x28546A, itemRing = 0xF2A4BA, base = 0xEBF5E9, icon = 0x28546A, connector = 0xBFDFB8),
    MINECRAFT("Minecraft", ring = 0x4AEDD9, itemRing = 0x6E6E6E, base = 0x3B3B3B, icon = 0xFFFFFF, connector = 0x80FF20),
    CONSTELLATION("Constellation", ring = 0xEAF2FF, itemRing = 0x5C7CA6, base = 0x05070F, icon = 0xDCE6F5, connector = 0xFFB74D),
    DOT_MATRIX("Dot Matrix", ring = 0xFF7A2E, itemRing = 0xA85A22, base = 0x0B0A08, icon = 0xFFE6CC, connector = 0x4ED8C0),
    IRON("Iron", ring = 0x5EE7FF, itemRing = 0x2E6E8C, base = 0x08111A, icon = 0xE8B84B, connector = 0xF2FBFF),
    SPYDER("Spyder", ring = 0xFFFFFF, itemRing = 0xE52521, base = 0x2A75D3, icon = 0xFFFFFF, connector = 0x1A1A1A),
    SPECTRAL_SILK("Spectral Silk", ring = 0xF9FCFE, itemRing = 0xE177F0, base = 0x6ACADF, icon = 0x000000, connector = 0xFF007F),
    FOUNDER("Founder", ring = 0xF0D68C, itemRing = 0xB4903C, base = 0x0C0A06, icon = 0xF5EAD0, connector = 0x8A6A1F),
    GOLD_EDITION("Gold Edition", ring = 0xFFE3A0, itemRing = 0x8A7A55, base = 0x0E0E0F, icon = 0xD4AF37, connector = 0xA88A3A),
    NEON_TRACE("Neon Trace", ring = 0x00F0FF, itemRing = 0x8A95AA, base = 0x0E1220, icon = 0x9AA4B8, connector = 0x3478D0);

    constructor(
        title: String,
        ring: Long,
        itemRing: Long,
        base: Long,
        icon: Long,
        connector: Long
    ) : this(
        title = title,
        ballColor = fill(base),
        ballIconColor = opaque(icon),
        ballBorderColor = opaque(ring),
        menuColor = fill(base),
        menuIconColor = opaque(icon),
        menuBorderColor = opaque(itemRing),
        connectorColor = opaque(connector),
        pillColor = pill(ring)
    )

    companion object {
        fun fromName(name: String?): QuickBallPresetTheme {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
        }
    }
}

data class QuickBallColors(
    val surfaceColor: Int,
    val accentColor: Int,
    val borderColor: Int,
    val contentColor: Int,
    val ballColor: Int,
    val ballIconColor: Int,
    val ballBorderColor: Int,
    val connectorColor: Int,
    val pillColor: Int,
)

object QuickBallTheme {

    fun resolveColors(context: Context, settings: SettingsRepository): QuickBallColors {
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val mode = QuickBallColorMode.fromName(settings.getQuickBallColorMode())

        val isBorderEnabled = settings.isQuickBallBorderEnabled()

        // 1. Resolve dynamic base scheme
        val dynamicScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (isDark) darkColorScheme() else lightColorScheme()
        }

        if (mode == QuickBallColorMode.DYNAMIC) {
            val surface = dynamicScheme.surfaceContainerHighest.toArgb()
            val accent = dynamicScheme.primary.toArgb()
            val border = if (isBorderEnabled) dynamicScheme.outlineVariant.toArgb() else AndroidColor.TRANSPARENT
            return QuickBallColors(
                surfaceColor = surface,
                accentColor = accent,
                borderColor = border,
                contentColor = accent,
                ballColor = surface,
                ballIconColor = accent,
                ballBorderColor = border,
                connectorColor = accent,
                pillColor = accent,
            )
        }

        if (mode == QuickBallColorMode.PRESET) {
            val themeName = settings.getQuickBallPresetTheme()
            val preset = QuickBallPresetTheme.fromName(themeName)
            val border = if (isBorderEnabled) preset.menuBorderColor else AndroidColor.TRANSPARENT
            val ballBorder = if (isBorderEnabled) preset.ballBorderColor else AndroidColor.TRANSPARENT
            return QuickBallColors(
                surfaceColor = preset.menuColor,
                accentColor = preset.ballBorderColor,
                borderColor = border,
                contentColor = preset.menuIconColor,
                ballColor = preset.ballColor,
                ballIconColor = preset.ballIconColor,
                ballBorderColor = ballBorder,
                connectorColor = preset.connectorColor,
                pillColor = preset.pillColor,
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

        val r = AndroidColor.red(accent)
        val g = AndroidColor.green(accent)
        val b = AndroidColor.blue(accent)

        val surface = if (isDark) {
            AndroidColor.rgb(
                (r * 0.18f + 24 * 0.82f).roundToInt(),
                (g * 0.18f + 24 * 0.82f).roundToInt(),
                (b * 0.18f + 24 * 0.82f).roundToInt(),
            )
        } else {
            AndroidColor.rgb(
                (r * 0.12f + 245 * 0.88f).roundToInt(),
                (g * 0.12f + 245 * 0.88f).roundToInt(),
                (b * 0.12f + 245 * 0.88f).roundToInt(),
            )
        }

        val border = if (!isBorderEnabled) {
            AndroidColor.TRANSPARENT
        } else if (isDark) {
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
            ballColor = accent,
            ballIconColor = if (isDark) AndroidColor.BLACK else AndroidColor.WHITE,
            ballBorderColor = border,
            connectorColor = accent,
            pillColor = accent,
        )
    }
}

@Keep
enum class QuickBallHapticIntensity(@field:StringRes val titleRes: Int) {
    LIGHT(R.string.haptic_light),
    MEDIUM(R.string.haptic_medium),
    STRONG(R.string.haptic_strong);

    companion object {
        fun fromName(name: String?): QuickBallHapticIntensity {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MEDIUM
        }
    }
}

object QuickBallHaptics {
    fun perform(context: Context, settings: SettingsRepository) {
        if (!settings.isQuickBallHaptic()) return
        val intensity = QuickBallHapticIntensity.fromName(settings.getQuickBallHapticIntensity())
        when (intensity) {
            QuickBallHapticIntensity.LIGHT -> com.sameerasw.essentials.island.ui.IslandHaptics.touchDown(context)
            QuickBallHapticIntensity.MEDIUM -> com.sameerasw.essentials.island.ui.IslandHaptics.commit(context)
            QuickBallHapticIntensity.STRONG -> com.sameerasw.essentials.island.ui.IslandHaptics.thresholdReached(context)
        }
    }
}

