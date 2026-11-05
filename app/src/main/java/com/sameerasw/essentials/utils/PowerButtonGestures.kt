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
            if (!repo.getBoolean(SettingsRepository.KEY_BUTTON_REMAP_POWER_OVERRIDDEN, false)) {
                val original = Settings.Global.getString(cr, SETTING_POWER_BUTTON_LONG_PRESS)
                repo.putString(SettingsRepository.KEY_BUTTON_REMAP_POWER_ORIGINAL, original ?: ORIGINAL_UNSET)
            }
            if (Settings.Global.getString(cr, SETTING_POWER_BUTTON_LONG_PRESS) != holdVal.toString()) {
                Settings.Global.putInt(cr, SETTING_POWER_BUTTON_LONG_PRESS, holdVal)
            }
            repo.putBoolean(SettingsRepository.KEY_BUTTON_REMAP_POWER_OVERRIDDEN, true)
        } catch (_: SecurityException) {
        }
    }

    fun restoreDefaults(
        context: Context,
        repo: SettingsRepository,
    ) {
        if (!repo.getBoolean(SettingsRepository.KEY_BUTTON_REMAP_POWER_OVERRIDDEN, false)) return
        if (!writeOriginal(context, repo)) return
        repo.putBoolean(SettingsRepository.KEY_BUTTON_REMAP_POWER_OVERRIDDEN, false)
        repo.remove(SettingsRepository.KEY_BUTTON_REMAP_POWER_ORIGINAL)
    }

    fun onListenerAvailable(
        context: Context,
        repo: SettingsRepository,
    ) {
        if (repo.getBoolean(SettingsRepository.KEY_BUTTON_REMAP_POWER_OVERRIDDEN, false)) sync(context, repo)
    }

    fun onListenerUnavailable(
        context: Context,
        repo: SettingsRepository,
    ) {
        if (!repo.getBoolean(SettingsRepository.KEY_BUTTON_REMAP_POWER_OVERRIDDEN, false)) return
        val current =
            try {
                Settings.Global.getString(context.contentResolver, SETTING_POWER_BUTTON_LONG_PRESS)
            } catch (_: Exception) {
                null
            }
        if (current == HANDLED_BY_ESSENTIALS.toString()) writeOriginal(context, repo)
    }

    private fun writeOriginal(
        context: Context,
        repo: SettingsRepository,
    ): Boolean {
        if (!PermissionUtils.canWriteSecureSettings(context)) return false
        val original = repo.getString(SettingsRepository.KEY_BUTTON_REMAP_POWER_ORIGINAL)
        return try {
            val cr = context.contentResolver
            when {
                original == null -> Settings.Global.putInt(cr, SETTING_POWER_BUTTON_LONG_PRESS, DEFAULT_ASSISTANT)
                original == ORIGINAL_UNSET || original.toIntOrNull() == HANDLED_BY_ESSENTIALS ->
                    Settings.Global.putString(cr, SETTING_POWER_BUTTON_LONG_PRESS, null)
                else -> Settings.Global.putString(cr, SETTING_POWER_BUTTON_LONG_PRESS, original)
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private const val ORIGINAL_UNSET = "unset"
    private const val HANDLED_BY_ESSENTIALS = 0
    private const val DEFAULT_ASSISTANT = 5
}
