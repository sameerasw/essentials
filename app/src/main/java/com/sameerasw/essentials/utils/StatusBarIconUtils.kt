/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - General
 * File: StatusBarIconUtils.kt
 * Description: Utility helper for StatusBarIconUtils.kt.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.provider.Settings
import androidx.core.content.edit

/**
 * Extensions and utilities for managing statusbar icon visibility
 */

private const val ICON_BLACKLIST_KEY = "icon_blacklist"

fun writeSecureSetting(
    context: Context,
    key: String,
    value: String?,
): Boolean {
    if (PermissionUtils.canWriteSecureSettings(context)) {
        try {
            if (Settings.Secure.putString(context.contentResolver, key, value)) return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    if (!ShellUtils.hasPermission(context)) return false
    val command =
        if (value == null) {
            "settings delete secure $key"
        } else {
            "settings put secure $key $value"
        }
    ShellUtils.runCommand(context, command, notifyOnError = false)
    return true
}

/**
 * Update the icon blacklist setting in secure settings
 */
fun updateIconBlacklistSetting(
    context: Context,
    blacklistNames: Set<String>,
) {
    val repository =
        com.sameerasw.essentials.data.repository
            .SettingsRepository(context)
    val isEnabled =
        repository.getBoolean(
            com.sameerasw.essentials.data.repository.SettingsRepository.KEY_STATUS_BAR_ICON_CONTROL_ENABLED,
            false,
        )
    if (!isEnabled) return

    val position =
        context
            .getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            .getInt(com.sameerasw.essentials.viewmodels.StatusBarIconViewModel.PREF_CLOCK_POSITION, 0)
    val names = blacklistNames + clockPositionBlacklist(position)

    writeSecureSetting(
        context,
        ICON_BLACKLIST_KEY,
        names.takeIf { it.isNotEmpty() }?.joinToString(","),
    )
}

private fun clockPositionBlacklist(position: Int): Set<String> =
    when (position) {
        1 -> setOf("right_clock_position", "middle_clock_position")
        2 -> setOf("left_clock_position", "right_clock_position")
        3 -> setOf("left_clock_position", "middle_clock_position")
        else -> emptySet()
    }

/**
 * Save icon visibility to shared preferences
 */
fun saveIconVisibilities(
    context: Context,
    visibilities: Map<String, Boolean>,
) {
    val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
    prefs.edit {
        for ((iconId, isVisible) in visibilities) {
            putBoolean("icon_${iconId}_visible", isVisible)
        }
    }
}

/**
 * Load icon visibility from shared preferences
 */
fun loadIconVisibilities(
    context: Context,
    defaultVisibilities: Map<String, Boolean>,
): Map<String, Boolean> {
    val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
    return defaultVisibilities.mapValues { (iconId, default) ->
        prefs.getBoolean("icon_${iconId}_visible", default)
    }
}

/**
 * Reset all icon visibility settings to defaults
 */
fun resetAllIconVisibilities(
    context: Context,
    defaultVisibilities: Map<String, Boolean>,
) {
    val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
    prefs.edit {
        for ((iconId, default) in defaultVisibilities) {
            putBoolean("icon_${iconId}_visible", default)
        }
    }
}
