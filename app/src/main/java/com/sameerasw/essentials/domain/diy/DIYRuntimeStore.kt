/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: DIYRuntimeStore.kt
 * Description: Storage for device-local DIY runtime state.
 */

package com.sameerasw.essentials.domain.diy

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Holds device-local runtime state and is deliberately left out of settings export.
object DIYRuntimeStore {
    private const val PREFS_NAME = "diy_runtime_prefs"
    private const val KEY_PAUSED_IDS = "loop_paused_ids"
    private const val KEY_OPEN_STATES_PREFIX = "open_states_"

    private var prefs: SharedPreferences? = null
    private val _pausedIds = MutableStateFlow<Set<String>>(emptySet())
    val pausedIds: StateFlow<Set<String>> = _pausedIds.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sharedPreferences
        _pausedIds.value = sharedPreferences.getStringSet(KEY_PAUSED_IDS, emptySet())?.toSet() ?: emptySet()
    }

    fun setPaused(
        id: String,
        paused: Boolean,
    ) {
        val current = _pausedIds.value.toMutableSet()
        val changed = if (paused) current.add(id) else current.remove(id)
        if (changed) {
            val newSet = current.toSet()
            _pausedIds.value = newSet
            prefs?.edit()?.putStringSet(KEY_PAUSED_IDS, newSet)?.apply()
        }
    }

    fun openStateIds(moduleId: String): Set<String> =
        prefs?.getStringSet(KEY_OPEN_STATES_PREFIX + moduleId, emptySet())?.toSet() ?: emptySet()

    fun setOpenStateIds(
        moduleId: String,
        ids: Set<String>,
    ) {
        prefs?.edit()?.putStringSet(KEY_OPEN_STATES_PREFIX + moduleId, ids.toSet())?.apply()
    }
}
