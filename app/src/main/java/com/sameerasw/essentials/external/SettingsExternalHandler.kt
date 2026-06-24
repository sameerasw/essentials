package com.sameerasw.essentials.external

import android.content.Context
import android.os.Bundle
import com.sameerasw.essentials.data.repository.SettingsRepository

class SettingsExternalHandler : ExternalHandler {
    override val path: String = "settings"

    override fun onQuery(context: Context, remainingPath: String, extras: Bundle?): Bundle? {
        val key = remainingPath
        val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(key)) return null

        val value = prefs.all[key] ?: return null
        val bundle = Bundle()
        when (value) {
            is Boolean -> bundle.putBoolean("value", value)
            is String -> bundle.putString("value", value)
            is Int -> bundle.putInt("value", value)
            is Float -> bundle.putFloat("value", value)
            is Long -> bundle.putLong("value", value)
            else -> bundle.putString("value", value.toString())
        }
        bundle.putString("type", value.javaClass.simpleName)
        return bundle
    }

    override fun onUpdate(context: Context, remainingPath: String, value: String?, extras: Bundle?): Boolean {
        val key = remainingPath
        val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(key)) return false

        val currentValue = prefs.all[key] ?: return false
        val repository = SettingsRepository(context)

        return try {
            when (currentValue) {
                is Boolean -> repository.putBoolean(key, value?.toBoolean() ?: false)
                is String -> repository.putString(key, value)
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

    override fun onAction(context: Context, remainingPath: String, action: String?, extras: Bundle?): Bundle? {
        if (action == "toggle") {
            val key = remainingPath
            val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
            val currentValue = prefs.all[key]
            if (currentValue is Boolean) {
                val repository = SettingsRepository(context)
                val newValue = !currentValue
                repository.putBoolean(key, newValue)
                return Bundle().apply { putBoolean("value", newValue) }
            }
        }
        return null
    }
}
