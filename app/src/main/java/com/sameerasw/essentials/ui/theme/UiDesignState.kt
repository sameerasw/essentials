/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: UiDesignState.kt
 * Description: OS specific UI design preference state.
 */

package com.sameerasw.essentials.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.sameerasw.essentials.data.repository.SettingsRepository

enum class UiDesign {
    AUTO,
    EXPRESSIVE,
    ONE_UI,
}

val LocalUiDesign = staticCompositionLocalOf { UiDesign.EXPRESSIVE }

object UiDesignState {
    var selected by mutableStateOf(UiDesign.AUTO)
        private set

    val resolved: UiDesign
        get() = resolve(selected)

    private val isSamsung: Boolean by lazy { Build.MANUFACTURER.equals("samsung", ignoreCase = true) }

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun resolve(design: UiDesign): UiDesign =
        when (design) {
            UiDesign.AUTO -> if (isSamsung) UiDesign.ONE_UI else UiDesign.EXPRESSIVE
            else -> design
        }

    fun init(context: Context) {
        if (listener != null) return
        val prefs = prefs(context)
        selected = read(prefs)
        val l =
            SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                if (key == SettingsRepository.KEY_UI_DESIGN) {
                    selected = read(p)
                }
            }
        listener = l
        prefs.registerOnSharedPreferenceChangeListener(l)
    }

    fun applyFirstRunDefaults(context: Context) {
        val prefs = prefs(context)
        if (prefs.getBoolean(SettingsRepository.KEY_UI_DESIGN_DEFAULTS_APPLIED, false)) return
        val editor = prefs.edit().putBoolean(SettingsRepository.KEY_UI_DESIGN_DEFAULTS_APPLIED, true)
        if (resolve(read(prefs)) == UiDesign.ONE_UI) {
            if (!prefs.contains(SettingsRepository.KEY_PITCH_BLACK_THEME_ENABLED)) {
                editor.putBoolean(SettingsRepository.KEY_PITCH_BLACK_THEME_ENABLED, true)
            }
            if (!prefs.contains(SettingsRepository.KEY_USE_SYSTEM_FONT)) {
                editor.putBoolean(SettingsRepository.KEY_USE_SYSTEM_FONT, true)
            }
        }
        editor.apply()
    }

    fun save(context: Context, design: UiDesign) {
        selected = design
        prefs(context).edit().putString(SettingsRepository.KEY_UI_DESIGN, design.name).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)

    private fun read(prefs: SharedPreferences): UiDesign =
        prefs.getString(SettingsRepository.KEY_UI_DESIGN, null)
            ?.let { name -> UiDesign.entries.firstOrNull { it.name == name } }
            ?: UiDesign.AUTO
}
