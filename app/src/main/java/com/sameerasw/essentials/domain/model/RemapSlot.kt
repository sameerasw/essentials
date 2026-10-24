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

enum class RemapInput {
    VOLUME_UP,
    VOLUME_DOWN,
    POWER_HOLD,
}

enum class RemapScreenState {
    OFF,
    ON,
}

enum class PowerChoice {
    ASSISTANT,
    POWER_MENU,
    ACTIONS,
}

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

                RemapInput.POWER_HOLD ->
                    if (screenState == RemapScreenState.ON) {
                        SettingsRepository.KEY_BUTTON_REMAP_POWER_HOLD_ACTION_ON
                    } else {
                        SettingsRepository.KEY_BUTTON_REMAP_POWER_HOLD_ACTION_OFF
                    }
            }

    val choicePrefKey: String?
        get() =
            when (input) {
                RemapInput.POWER_HOLD ->
                    if (screenState == RemapScreenState.ON) {
                        SettingsRepository.KEY_BUTTON_REMAP_POWER_HOLD_CHOICE_ON
                    } else {
                        SettingsRepository.KEY_BUTTON_REMAP_POWER_HOLD_CHOICE_OFF
                    }

                RemapInput.VOLUME_UP,
                RemapInput.VOLUME_DOWN,
                -> null
            }

    val defaultChoice: PowerChoice?
        get() =
            when (input) {
                RemapInput.POWER_HOLD -> PowerChoice.ASSISTANT
                RemapInput.VOLUME_UP,
                RemapInput.VOLUME_DOWN,
                -> null
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
