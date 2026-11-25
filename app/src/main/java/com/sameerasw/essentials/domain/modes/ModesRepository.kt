/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModesRepository.kt
 * Description: Persists Essentials modes.
 */

package com.sameerasw.essentials.domain.modes

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.domain.diy.ActionGsonAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ModesRepository {
    const val PREFS_NAME = "modes_prefs"
    private const val KEY_MODES = "saved_modes"

    private val _modes = MutableStateFlow<List<Mode>>(emptyList())
    val modes = _modes.asStateFlow()

    private var prefs: SharedPreferences? = null
    private val gson =
        ActionGsonAdapter.gson
            .newBuilder()
            .registerTypeAdapter(ModeCondition::class.java, ModeCondition.GsonAdapter)
            .create()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        reload()
    }

    fun reload() {
        val json = prefs?.getString(KEY_MODES, null)
        _modes.value =
            json?.let {
                runCatching {
                    gson.fromJson<List<Mode>>(it, object : TypeToken<List<Mode>>() {}.type)
                }.getOrNull()
            } ?: emptyList()
    }

    fun getMode(id: String): Mode? = _modes.value.find { it.id == id }

    fun upsert(mode: Mode) {
        val current = _modes.value.toMutableList()
        val index = current.indexOfFirst { it.id == mode.id }
        if (index == -1) current.add(mode) else current[index] = mode
        save(current)
    }

    fun setEnabled(
        id: String,
        enabled: Boolean,
    ) {
        val mode = getMode(id) ?: return
        if (mode.isEnabled != enabled) upsert(mode.copy(isEnabled = enabled))
    }

    fun remove(id: String) {
        save(_modes.value.filterNot { it.id == id })
    }

    private fun save(modes: List<Mode>) {
        _modes.value = modes
        prefs?.edit()?.putString(KEY_MODES, gson.toJson(modes))?.apply()
    }
}
