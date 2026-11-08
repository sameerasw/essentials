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

// Holds device-local runtime state and is deliberately left out of settings export.
object DIYRuntimeStore {
    private const val PREFS_NAME = "diy_runtime_prefs"
    private const val KEY_OPEN_STATES_PREFIX = "open_states_"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
