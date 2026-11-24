/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: SystemFontState.kt
 * Description: Process-wide observable state for the system font setting.
 */

package com.sameerasw.essentials.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import com.sameerasw.essentials.data.repository.SettingsRepository

object SystemFontState {
    var enabled by mutableStateOf(false)
        private set

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun init(context: Context) {
        if (listener != null) return
        val prefs = context.applicationContext.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        enabled = prefs.getBoolean(SettingsRepository.KEY_USE_SYSTEM_FONT, false)
        val l =
            SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                if (key == SettingsRepository.KEY_USE_SYSTEM_FONT) {
                    enabled = p.getBoolean(key, false)
                }
            }
        listener = l
        prefs.registerOnSharedPreferenceChangeListener(l)
    }
}

val AppFontFamily: FontFamily
    get() = if (SystemFontState.enabled) FontFamily.Default else GoogleSansFlexRounded
