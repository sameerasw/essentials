/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: RemapSlot.kt
 * Description: Identifies a Button Remap slot (physical input + screen state) and its preference key.
 */

package com.sameerasw.essentials.domain.model

import android.view.KeyEvent
import com.sameerasw.essentials.data.repository.SettingsRepository

/**
 * Physical inputs that can be remapped. New inputs (e.g. power button, back tap) are added here
 * along with their preference keys in [RemapSlot.prefKey].
 */
enum class RemapInput {
    VOLUME_UP,
    VOLUME_DOWN,
}

enum class RemapScreenState {
    OFF,
    ON,
}

/**
 * A single remappable slot. Each slot stores an ordered list of actions that run on long press.
 */
data class RemapSlot(
    val input: RemapInput,
    val screenState: RemapScreenState,
) {
    val prefKey: String
        get() =
            when (input) {
                RemapInput.VOLUME_UP ->
                    if (screenState == RemapScreenState.ON) {
                        SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_ON
                    } else {
                        SettingsRepository.KEY_BUTTON_REMAP_VOL_UP_ACTION_OFF
                    }

                RemapInput.VOLUME_DOWN ->
                    if (screenState == RemapScreenState.ON) {
                        SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_ON
                    } else {
                        SettingsRepository.KEY_BUTTON_REMAP_VOL_DOWN_ACTION_OFF
                    }
            }

    companion object {
        val ALL: List<RemapSlot> =
            RemapInput.entries.flatMap { input ->
                RemapScreenState.entries.map { RemapSlot(input, it) }
            }

        fun forVolume(
            isUp: Boolean,
            isScreenOn: Boolean,
        ) = RemapSlot(
            input = if (isUp) RemapInput.VOLUME_UP else RemapInput.VOLUME_DOWN,
            screenState = if (isScreenOn) RemapScreenState.ON else RemapScreenState.OFF,
        )

        fun forKeyCode(
            keyCode: Int,
            isScreenOn: Boolean,
        ): RemapSlot? =
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> forVolume(isUp = true, isScreenOn = isScreenOn)
                KeyEvent.KEYCODE_VOLUME_DOWN -> forVolume(isUp = false, isScreenOn = isScreenOn)
                else -> null
            }
    }
}
