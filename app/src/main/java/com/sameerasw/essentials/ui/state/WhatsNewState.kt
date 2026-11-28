/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: WhatsNewState.kt
 * Description: Process-wide observable state for the day-limited "what's new" indicators.
 */

package com.sameerasw.essentials.ui.state

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.registry.WhatsNewRegistry

object WhatsNewState {
    private const val DURATION_MS = 24L * 60 * 60 * 1000

    private var until by mutableLongStateOf(0L)

    val enabled: Boolean
        get() = until > System.currentTimeMillis()

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)

    fun init(context: Context) {
        if (listener != null) return
        val prefs = prefs(context)
        val release = WhatsNewRegistry.currentRelease
        if (prefs.getString(SettingsRepository.KEY_WHATS_NEW_DEFAULTED_RELEASE, null) != release) {
            prefs
                .edit()
                .putLong(SettingsRepository.KEY_SHOW_WHATS_NEW_UNTIL, System.currentTimeMillis() + DURATION_MS)
                .putString(SettingsRepository.KEY_WHATS_NEW_DEFAULTED_RELEASE, release)
                .apply()
        }
        until = prefs.getLong(SettingsRepository.KEY_SHOW_WHATS_NEW_UNTIL, 0L)
        val l =
            SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                if (key == SettingsRepository.KEY_SHOW_WHATS_NEW_UNTIL) {
                    until = p.getLong(key, 0L)
                }
            }
        listener = l
        prefs.registerOnSharedPreferenceChangeListener(l)
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        prefs(context)
            .edit()
            .putLong(SettingsRepository.KEY_SHOW_WHATS_NEW_UNTIL, if (enabled) System.currentTimeMillis() + DURATION_MS else 0L)
            .apply()
    }

    fun isNewFeature(featureId: String?): Boolean = enabled && featureId != null && featureId in WhatsNewRegistry.newFeatures

    fun isNewSetting(
        featureId: String?,
        key: String,
    ): Boolean = enabled && featureId != null && (featureId to key) in WhatsNewRegistry.newSettings
}
