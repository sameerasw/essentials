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
        return try {
            val type = object : TypeToken<List<WidgetStackConfig>>() {}.type
            gson.fromJson<List<WidgetStackConfig>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun get(stackWidgetId: Int): WidgetStackConfig? = getAll().firstOrNull { it.stackWidgetId == stackWidgetId }

    fun save(config: WidgetStackConfig) {
        val updated = getAll().filterNot { it.stackWidgetId == config.stackWidgetId } + config
        write(updated)
    }

    fun delete(stackWidgetId: Int) {
        write(getAll().filterNot { it.stackWidgetId == stackWidgetId })
    }

    private fun write(stacks: List<WidgetStackConfig>) {
        prefs.edit().putString(KEY_STACKS, gson.toJson(stacks)).apply()
    }

    companion object {
        private const val PREFS_NAME = "widget_stack_prefs"
        private const val KEY_STACKS = "stacks"
    }
}
