/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: ViewModel State Holders
 * File: RippleSettings.kt
 * Description: Observable settings holder for the notification lighting ripple effect.
 */

package com.sameerasw.essentials.viewmodels.state

import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.NotificationLightingRipplePosition
import com.sameerasw.essentials.domain.model.RippleConfig
import com.sameerasw.essentials.utils.overlay.parseRipplePosition

class RippleSettings(
    private val repository: () -> SettingsRepository,
) {
    val speed = mutableFloatStateOf(RippleConfig.DEFAULT_SPEED)
    val repeatCount = mutableFloatStateOf(RippleConfig.DEFAULT_REPEAT_COUNT.toFloat())
    val waveSize = mutableFloatStateOf(RippleConfig.DEFAULT_WAVE_SIZE)
    val opacity = mutableFloatStateOf(RippleConfig.DEFAULT_OPACITY)
    val sparklesEnabled = mutableStateOf(true)
    val sparkleCount = mutableFloatStateOf(RippleConfig.DEFAULT_SPARKLE_COUNT.toFloat())
    val sparkleSize = mutableFloatStateOf(RippleConfig.DEFAULT_SPARKLE_SIZE)
    val position = mutableStateOf(NotificationLightingRipplePosition.AUTO)
    val positionX = mutableFloatStateOf(RippleConfig.DEFAULT_POSITION_PERCENT)
    val positionY = mutableFloatStateOf(RippleConfig.DEFAULT_POSITION_PERCENT)

    fun load() {
        val repo = repository()
        speed.floatValue = repo.getFloat(KEY_SPEED, RippleConfig.DEFAULT_SPEED)
        repeatCount.floatValue =
            repo.getFloat(KEY_REPEAT_COUNT, RippleConfig.DEFAULT_REPEAT_COUNT.toFloat())
        waveSize.floatValue = repo.getFloat(KEY_WAVE_SIZE, RippleConfig.DEFAULT_WAVE_SIZE)
        opacity.floatValue = repo.getFloat(KEY_OPACITY, RippleConfig.DEFAULT_OPACITY)
        sparklesEnabled.value = repo.getBoolean(KEY_SPARKLES_ENABLED, true)
        sparkleCount.floatValue =
            repo.getFloat(KEY_SPARKLE_COUNT, RippleConfig.DEFAULT_SPARKLE_COUNT.toFloat())
        sparkleSize.floatValue = repo.getFloat(KEY_SPARKLE_SIZE, RippleConfig.DEFAULT_SPARKLE_SIZE)
        position.value =
            parseRipplePosition(
                repo.getString(KEY_POSITION, NotificationLightingRipplePosition.AUTO.name),
            )
        positionX.floatValue = repo.getFloat(KEY_POSITION_X, RippleConfig.DEFAULT_POSITION_PERCENT)
        positionY.floatValue = repo.getFloat(KEY_POSITION_Y, RippleConfig.DEFAULT_POSITION_PERCENT)
    }

    fun toConfig(): RippleConfig =
        RippleConfig(
            speed = speed.floatValue,
            repeatCount = repeatCount.floatValue.toInt(),
            waveSize = waveSize.floatValue,
            opacity = opacity.floatValue,
            sparklesEnabled = sparklesEnabled.value,
            sparkleCount = sparkleCount.floatValue.toInt(),
            sparkleSize = sparkleSize.floatValue,
            position = position.value,
            positionX = positionX.floatValue,
            positionY = positionY.floatValue,
        )

    fun saveSpeed(value: Float) = putFloat(speed, KEY_SPEED, value)

    fun saveRepeatCount(value: Float) = putFloat(repeatCount, KEY_REPEAT_COUNT, value)

    fun saveWaveSize(value: Float) = putFloat(waveSize, KEY_WAVE_SIZE, value)

    fun saveOpacity(value: Float) = putFloat(opacity, KEY_OPACITY, value)

    fun saveSparkleCount(value: Float) = putFloat(sparkleCount, KEY_SPARKLE_COUNT, value)

    fun saveSparkleSize(value: Float) = putFloat(sparkleSize, KEY_SPARKLE_SIZE, value)

    fun saveSparklesEnabled(enabled: Boolean) {
        sparklesEnabled.value = enabled
        repository().putBoolean(KEY_SPARKLES_ENABLED, enabled)
    }

    fun savePosition(value: NotificationLightingRipplePosition) {
        position.value = value
        repository().putString(KEY_POSITION, value.name)
    }

    fun savePositionXY(
        x: Float,
        y: Float,
    ) {
        positionX.floatValue = x
        positionY.floatValue = y
        repository().apply {
            putFloat(KEY_POSITION_X, x)
            putFloat(KEY_POSITION_Y, y)
        }
    }

    private fun putFloat(
        state: MutableFloatState,
        key: String,
        value: Float,
    ) {
        state.floatValue = value
        repository().putFloat(key, value)
    }

    private companion object {
        const val KEY_SPEED = SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_SPEED
        const val KEY_REPEAT_COUNT = SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_REPEAT_COUNT
        const val KEY_WAVE_SIZE = SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_WAVE_SIZE
        const val KEY_OPACITY = SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_OPACITY
        const val KEY_SPARKLES_ENABLED = SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_SPARKLES_ENABLED
        const val KEY_SPARKLE_COUNT = SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_SPARKLE_COUNT
        const val KEY_SPARKLE_SIZE = SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_SPARKLE_SIZE
        const val KEY_POSITION = SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_POSITION
        const val KEY_POSITION_X = SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_POSITION_X
        const val KEY_POSITION_Y = SettingsRepository.KEY_EDGE_LIGHTING_RIPPLE_POSITION_Y
    }
}
