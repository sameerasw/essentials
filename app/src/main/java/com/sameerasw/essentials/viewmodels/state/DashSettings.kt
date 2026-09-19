/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: ViewModel State Holders
 * File: DashSettings.kt
 * Description: Observable settings holder for the notification lighting edge dash effect.
 */

package com.sameerasw.essentials.viewmodels.state

import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.DashConfig

class DashSettings(
    private val repository: () -> SettingsRepository,
) {
    val thickness = mutableFloatStateOf(DashConfig.DEFAULT_THICKNESS)
    val length = mutableFloatStateOf(DashConfig.DEFAULT_LENGTH_PERCENT)
    val glow = mutableFloatStateOf(DashConfig.DEFAULT_GLOW)
    val glowLength = mutableFloatStateOf(DashConfig.DEFAULT_GLOW_LENGTH_RATIO)

    fun load() {
        val repo = repository()
        thickness.floatValue = repo.getFloat(KEY_THICKNESS, DashConfig.DEFAULT_THICKNESS)
        length.floatValue = repo.getFloat(KEY_LENGTH, DashConfig.DEFAULT_LENGTH_PERCENT)
        glow.floatValue = repo.getFloat(KEY_GLOW, DashConfig.DEFAULT_GLOW)
        glowLength.floatValue =
            repo.getFloat(KEY_GLOW_LENGTH, DashConfig.DEFAULT_GLOW_LENGTH_RATIO)
    }

    fun toConfig(): DashConfig =
        DashConfig(
            thickness = thickness.floatValue,
            lengthPercent = length.floatValue,
            glow = glow.floatValue,
            glowLengthRatio = glowLength.floatValue,
        )

    fun saveThickness(value: Float) = putFloat(thickness, KEY_THICKNESS, value)

    fun saveLength(value: Float) = putFloat(length, KEY_LENGTH, value)

    fun saveGlow(value: Float) = putFloat(glow, KEY_GLOW, value)

    fun saveGlowLength(value: Float) = putFloat(glowLength, KEY_GLOW_LENGTH, value)

    private fun putFloat(
        state: MutableFloatState,
        key: String,
        value: Float,
    ) {
        state.floatValue = value
        repository().putFloat(key, value)
    }

    private companion object {
        const val KEY_THICKNESS = SettingsRepository.KEY_EDGE_LIGHTING_DASH_THICKNESS
        const val KEY_LENGTH = SettingsRepository.KEY_EDGE_LIGHTING_DASH_LENGTH
        const val KEY_GLOW = SettingsRepository.KEY_EDGE_LIGHTING_DASH_GLOW
        const val KEY_GLOW_LENGTH = SettingsRepository.KEY_EDGE_LIGHTING_DASH_GLOW_LENGTH
    }
}
