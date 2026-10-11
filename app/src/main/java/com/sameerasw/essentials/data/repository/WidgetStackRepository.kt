/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Data & Repository Layer
 * File: WidgetStackRepository.kt
 * Description: Stores the configuration of every widget stack on the homescreen.
 */

package com.sameerasw.essentials.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.domain.model.WidgetStackConfig

class WidgetStackRepository(
    context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAll(): List<WidgetStackConfig> {
        val json = prefs.getString(KEY_STACKS, null) ?: return emptyList()
        synchronized(LOCK) {
            if (json == cachedJson) return cachedStacks
        }
        val parsed =
            try {
                val type = object : TypeToken<List<WidgetStackConfig>>() {}.type
                gson.fromJson<List<WidgetStackConfig>>(json, type) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        synchronized(LOCK) {
            cachedJson = json
            cachedStacks = parsed
        }
        return parsed
    }

    fun get(stackWidgetId: Int): WidgetStackConfig? = getAll().firstOrNull { it.stackWidgetId == stackWidgetId }

    fun save(config: WidgetStackConfig) =
        synchronized(LOCK) {
            write(getAll().filterNot { it.stackWidgetId == config.stackWidgetId } + config)
        }

    fun delete(stackWidgetId: Int) =
        synchronized(LOCK) {
            write(getAll().filterNot { it.stackWidgetId == stackWidgetId })
        }

    private fun write(stacks: List<WidgetStackConfig>) {
        val json = gson.toJson(stacks)
        prefs.edit().putString(KEY_STACKS, json).apply()
        cachedJson = json
        cachedStacks = stacks
    }

    companion object {
        private const val PREFS_NAME = "widget_stack_prefs"
        private const val KEY_STACKS = "stacks"
        private val LOCK = Any()
        private var cachedJson: String? = null
        private var cachedStacks: List<WidgetStackConfig> = emptyList()
    }
}
