/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: External
 * File: SettingsExternalHandler.kt
 * Description: Component file for SettingsExternalHandler.kt.
 */

package com.sameerasw.essentials.external

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import com.sameerasw.essentials.data.repository.SettingsRepository

class SettingsExternalHandler : ExternalHandler {
    override val path: String = "settings"

    private val blockedKeys =
        setOf(
            SettingsRepository.KEY_GITHUB_ACCESS_TOKEN,
            SettingsRepository.KEY_SHIZUKU_AUTH_TOKEN,
            SettingsRepository.KEY_EXTERNAL_CONTROL_TOKEN,
            SettingsRepository.KEY_UNSPLASH_ACCESS_KEY,
        )

    // Only primitive feature flags and numbers are exposed; strings may hold tokens or action JSON
    private fun isExposed(
        key: String,
        value: Any?,
    ): Boolean = key !in blockedKeys && (value is Boolean || value is Int || value is Float || value is Long)

    override fun onQuery(
        context: Context,
        remainingPath: String,
        extras: Bundle?,
    ): Cursor? {
        val key = remainingPath
        val prefs =
            context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(key)) return null

        val value = prefs.all[key] ?: return null
        if (!isExposed(key, value)) return null
        val cursor = MatrixCursor(arrayOf("key", "value", "type"))
        cursor.addRow(arrayOf(key, value, value.javaClass.simpleName))
        return cursor
    }

    override fun onUpdate(
        context: Context,
        remainingPath: String,
        value: String?,
        extras: Bundle?,
    ): Boolean {
        val key = remainingPath
        val prefs =
            context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(key)) return false

        val currentValue = prefs.all[key] ?: return false
        if (!isExposed(key, currentValue)) return false
        val repository = SettingsRepository(context)

        return try {
            when (currentValue) {
                is Boolean -> repository.putBoolean(key, value?.toBoolean() ?: false)
                is Int -> repository.putInt(key, value?.toInt() ?: 0)
                is Float -> repository.putFloat(key, value?.toFloat() ?: 0f)
                is Long -> repository.putLong(key, value?.toLong() ?: 0L)
                else -> false
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun onAction(
        context: Context,
        remainingPath: String,
        action: String?,
        extras: Bundle?,
    ): Bundle? {
        if (action == "toggle") {
            val key = remainingPath
            val prefs =
                context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
            val currentValue = prefs.all[key]
            if (currentValue is Boolean && key !in blockedKeys) {
                val repository = SettingsRepository(context)
                val newValue = !currentValue
                repository.putBoolean(key, newValue)
                return Bundle().apply { putBoolean("value", newValue) }
            }
        }
        return null
    }
}
