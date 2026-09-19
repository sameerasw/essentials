/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlay
 * File: DashConfigIo.kt
 * Description: Utility helper for DashConfigIo.kt.
 */

package com.sameerasw.essentials.utils.overlay

import android.content.Intent
import android.content.SharedPreferences
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.DashConfig

private const val EXTRA_THICKNESS = "dash_thickness"
private const val EXTRA_LENGTH = "dash_length"
private const val EXTRA_GLOW = "dash_glow"
private const val EXTRA_GLOW_LENGTH = "dash_glow_length"
private const val EXTRA_OVERLAP = "dash_overlap"

fun DashConfig.writeTo(intent: Intent) {
    intent.putExtra(EXTRA_THICKNESS, thickness)
    intent.putExtra(EXTRA_LENGTH, lengthPercent)
    intent.putExtra(EXTRA_GLOW, glow)
    intent.putExtra(EXTRA_GLOW_LENGTH, glowLengthRatio)
    intent.putExtra(EXTRA_OVERLAP, overlapPercent)
}

fun DashConfig.Companion.fromIntent(intent: Intent): DashConfig =
    DashConfig(
        thickness = intent.getFloatExtra(EXTRA_THICKNESS, DEFAULT_THICKNESS),
        lengthPercent = intent.getFloatExtra(EXTRA_LENGTH, DEFAULT_LENGTH_PERCENT),
        glow = intent.getFloatExtra(EXTRA_GLOW, DEFAULT_GLOW),
        glowLengthRatio = intent.getFloatExtra(EXTRA_GLOW_LENGTH, DEFAULT_GLOW_LENGTH_RATIO),
        overlapPercent = intent.getFloatExtra(EXTRA_OVERLAP, DEFAULT_OVERLAP_PERCENT),
    )

fun DashConfig.Companion.fromPrefs(prefs: SharedPreferences): DashConfig =
    DashConfig(
        thickness =
            prefs.dashFloat(SettingsRepository.KEY_EDGE_LIGHTING_DASH_THICKNESS, DEFAULT_THICKNESS),
        lengthPercent =
            prefs.dashFloat(SettingsRepository.KEY_EDGE_LIGHTING_DASH_LENGTH, DEFAULT_LENGTH_PERCENT),
        glow = prefs.dashFloat(SettingsRepository.KEY_EDGE_LIGHTING_DASH_GLOW, DEFAULT_GLOW),
        glowLengthRatio =
            prefs.dashFloat(
                SettingsRepository.KEY_EDGE_LIGHTING_DASH_GLOW_LENGTH,
                DEFAULT_GLOW_LENGTH_RATIO,
            ),
        overlapPercent =
            prefs.dashFloat(
                SettingsRepository.KEY_EDGE_LIGHTING_DASH_OVERLAP,
                DEFAULT_OVERLAP_PERCENT,
            ),
    )

private fun SharedPreferences.dashFloat(
    key: String,
    default: Float,
): Float =
    try {
        getFloat(key, default)
    } catch (_: ClassCastException) {
        getInt(key, default.toInt()).toFloat()
    }
