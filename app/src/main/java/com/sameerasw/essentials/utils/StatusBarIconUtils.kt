package com.sameerasw.essentials.utils

import android.content.Context
import android.provider.Settings
import androidx.core.content.edit

/**
 * Extensions and utilities for managing statusbar icon visibility
 */

/**
 * Update the icon blacklist setting in secure settings
 */
fun updateIconBlacklistSetting(
    context: Context,
    blacklistNames: Set<String>
) {
    if (blacklistNames.isEmpty()) {
        try {
            Settings.Secure.putString(context.contentResolver, "icon_blacklist", null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } else {
        val blacklistString = blacklistNames.joinToString(",")
        try {
            Settings.Secure.putString(context.contentResolver, "icon_blacklist", blacklistString)
        } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Save icon visibility to shared preferences
 */
fun saveIconVisibilities(
    context: Context,
    visibilities: Map<String, Boolean>
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
    defaultVisibilities: Map<String, Boolean>
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
    defaultVisibilities: Map<String, Boolean>
) {
    val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
    prefs.edit {
        for ((iconId, default) in defaultVisibilities) {
            putBoolean("icon_${iconId}_visible", default)
        }
    }
}

