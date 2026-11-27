/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: WhatsNewState.kt
 * Description: Process-wide observable state for the developer "Show what's new" option.
 */

package com.sameerasw.essentials.ui.state

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.registry.WhatsNewRegistry

object WhatsNewState {
    var enabled by mutableStateOf(false)
        private set

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun init(context: Context) {
        if (listener != null) return
        val prefs = context.applicationContext.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        enabled = prefs.getBoolean(SettingsRepository.KEY_SHOW_WHATS_NEW, false)
        val l =
            SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                if (key == SettingsRepository.KEY_SHOW_WHATS_NEW) {
                    enabled = p.getBoolean(key, false)
                }
            }
        listener = l
        prefs.registerOnSharedPreferenceChangeListener(l)
    }

    fun isNewFeature(featureId: String?): Boolean = enabled && featureId != null && featureId in WhatsNewRegistry.newFeatures

    fun isNewSetting(
        featureId: String?,
        key: String,
    ): Boolean = enabled && featureId != null && (featureId to key) in WhatsNewRegistry.newSettings
}
