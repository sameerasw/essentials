/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlay
 * File: RippleConfigIo.kt
 * Description: Utility helper for RippleConfigIo.kt.
 */

package com.sameerasw.essentials.utils.overlay

import android.content.Intent
import android.content.SharedPreferences
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.NotificationLightingRipplePosition
import com.sameerasw.essentials.domain.model.RippleConfig

private const val EXTRA_SPEED = "ripple_speed"
private const val EXTRA_REPEAT_COUNT = "ripple_repeat_count"
private const val EXTRA_WAVE_SIZE = "ripple_wave_size"
private const val EXTRA_OPACITY = "ripple_opacity"
private const val EXTRA_SPARKLES_ENABLED = "ripple_sparkles_enabled"
private const val EXTRA_SPARKLE_COUNT = "ripple_sparkle_count"
private const val EXTRA_SPARKLE_SIZE = "ripple_sparkle_size"
private const val EXTRA_POSITION = "ripple_position"
private const val EXTRA_POSITION_X = "ripple_position_x"
private const val EXTRA_POSITION_Y = "ripple_position_y"

fun RippleConfig.writeTo(intent: Intent) {
    intent.putExtra(EXTRA_SPEED, speed)
    intent.putExtra(EXTRA_REPEAT_COUNT, repeatCount)
    intent.putExtra(EXTRA_WAVE_SIZE, waveSize)
    intent.putExtra(EXTRA_OPACITY, opacity)
    intent.putExtra(EXTRA_SPARKLES_ENABLED, sparklesEnabled)
    intent.putExtra(EXTRA_SPARKLE_COUNT, sparkleCount)
    intent.putExtra(EXTRA_SPARKLE_SIZE, sparkleSize)
    intent.putExtra(EXTRA_POSITION, position.name)
    intent.putExtra(EXTRA_POSITION_X, positionX)
    intent.putExtra(EXTRA_POSITION_Y, positionY)
}

fun RippleConfig.Companion.fromIntent(intent: Intent): RippleConfig =
    RippleConfig(
        speed = intent.getFloatExtra(EXTRA_SPEED, DEFAULT_SPEED),
        repeatCount = intent.getIntExtra(EXTRA_REPEAT_COUNT, DEFAULT_REPEAT_COUNT),
        waveSize = intent.getFloatExtra(EXTRA_WAVE_SIZE, DEFAULT_WAVE_SIZE),
        opacity = intent.getFloatExtra(EXTRA_OPACITY, DEFAULT_OPACITY),
        sparklesEnabled = intent.getBooleanExtra(EXTRA_SPARKLES_ENABLED, true),
        sparkleCount = intent.getIntExtra(EXTRA_SPARKLE_COUNT, DEFAULT_SPARKLE_COUNT),
        sparkleSize = intent.getFloatExtra(EXTRA_SPARKLE_SIZE, DEFAULT_SPARKLE_SIZE),
        position = parsePosition(intent.getStringExtra(EXTRA_POSITION)),
        positionX = intent.getFloatExtra(EXTRA_POSITION_X, DEFAULT_POSITION_PERCENT),
        positionY = intent.getFloatExtra(EXTRA_POSITION_Y, DEFAULT_POSITION_PERCENT),
    )

fun RippleConfig.Companion.fromPrefs(prefs: SharedPreferences): RippleConfig =
    RippleConfig(
        speed = prefs.float(SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_SPEED, DEFAULT_SPEED),
        repeatCount =
            prefs
                .float(
                    SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_REPEAT_COUNT,
                    DEFAULT_REPEAT_COUNT.toFloat(),
                ).toInt(),
        waveSize = prefs.float(SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_WAVE_SIZE, DEFAULT_WAVE_SIZE),
        opacity = prefs.float(SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_OPACITY, DEFAULT_OPACITY),
        sparklesEnabled =
            prefs.getBoolean(SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_SPARKLES_ENABLED, true),
        sparkleCount =
            prefs
                .float(
                    SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_SPARKLE_COUNT,
                    DEFAULT_SPARKLE_COUNT.toFloat(),
                ).toInt(),
        sparkleSize =
            prefs.float(SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_SPARKLE_SIZE, DEFAULT_SPARKLE_SIZE),
        position =
            parsePosition(
                prefs.getString(
                    SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_POSITION,
                    NotificationLightingRipplePosition.AUTO.name,
                ),
            ),
        positionX =
            prefs.float(
                SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_POSITION_X,
                DEFAULT_POSITION_PERCENT,
            ),
        positionY =
            prefs.float(
                SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_POSITION_Y,
                DEFAULT_POSITION_PERCENT,
            ),
    )

fun parseRipplePosition(name: String?): NotificationLightingRipplePosition = parsePosition(name)

private fun parsePosition(name: String?): NotificationLightingRipplePosition =
    try {
        NotificationLightingRipplePosition.valueOf(
            name ?: NotificationLightingRipplePosition.AUTO.name,
        )
    } catch (_: Exception) {
        NotificationLightingRipplePosition.AUTO
    }

private fun SharedPreferences.float(
    key: String,
    default: Float,
): Float =
    try {
        getFloat(key, default)
    } catch (_: ClassCastException) {
        getInt(key, default.toInt()).toFloat()
    }
