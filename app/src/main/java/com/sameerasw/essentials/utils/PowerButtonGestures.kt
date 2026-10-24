/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core Utilities
 * File: PowerButtonGestures.kt
 * Description: Helpers for synchronizing power button gestures with Android system settings.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.provider.Settings
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.PowerChoice
import com.sameerasw.essentials.domain.model.RemapInput
import com.sameerasw.essentials.domain.model.RemapScreenState
import com.sameerasw.essentials.domain.model.RemapSlot

object PowerButtonGestures {
    private const val SETTING_POWER_BUTTON_LONG_PRESS = "power_button_long_press"

    fun longPressValue(
        off: PowerChoice,
        on: PowerChoice,
    ): Int =
        if (off == PowerChoice.ASSISTANT && on == PowerChoice.ASSISTANT) {
            5
        } else if (off == PowerChoice.POWER_MENU && on == PowerChoice.POWER_MENU) {
            1
        } else {
            0
        }

    fun isHandledByEssentials(
        off: PowerChoice,
        on: PowerChoice,
    ): Boolean = off != on || off == PowerChoice.ACTIONS || on == PowerChoice.ACTIONS

    fun isHoldHandled(repo: SettingsRepository): Boolean =
        isHandledByEssentials(
            repo.getPowerChoice(RemapSlot(RemapInput.POWER_HOLD, RemapScreenState.OFF)),
            repo.getPowerChoice(RemapSlot(RemapInput.POWER_HOLD, RemapScreenState.ON)),
        )

    fun isListenerNeeded(repo: SettingsRepository): Boolean =
        isHoldHandled(repo) || repo.getBoolean(SettingsRepository.KEY_BUTTON_REMAP_POWER_CAMERA_FLIP, false)

    fun sync(
        context: Context,
        repo: SettingsRepository,
    ) {
        if (!PermissionUtils.canWriteSecureSettings(context)) return

        val holdOff = repo.getPowerChoice(RemapSlot(RemapInput.POWER_HOLD, RemapScreenState.OFF))
        val holdOn = repo.getPowerChoice(RemapSlot(RemapInput.POWER_HOLD, RemapScreenState.ON))
        val holdVal = longPressValue(holdOff, holdOn)

        try {
            val cr = context.contentResolver
            Settings.Global.putInt(cr, SETTING_POWER_BUTTON_LONG_PRESS, holdVal)
            repo.putBoolean(SettingsRepository.KEY_BUTTON_REMAP_POWER_OVERRIDDEN, true)
        } catch (_: SecurityException) {
        }
    }

    fun restoreDefaults(
        context: Context,
        repo: SettingsRepository,
    ) {
        if (!repo.getBoolean(SettingsRepository.KEY_BUTTON_REMAP_POWER_OVERRIDDEN, false)) return
        if (!PermissionUtils.canWriteSecureSettings(context)) return

        try {
            val cr = context.contentResolver
            Settings.Global.putInt(cr, SETTING_POWER_BUTTON_LONG_PRESS, 5)
            repo.putBoolean(SettingsRepository.KEY_BUTTON_REMAP_POWER_OVERRIDDEN, false)
        } catch (_: SecurityException) {
        }
    }
}
