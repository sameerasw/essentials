package com.sameerasw.essentials.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import com.sameerasw.essentials.utils.ShizukuUtils
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import java.lang.reflect.Method

object FreezeManager {
    private const val TAG = "FreezeManager"

    // Hidden API constants
    private const val COMPONENT_ENABLED_STATE_DEFAULT = 0
    private const val COMPONENT_ENABLED_STATE_ENABLED = 1
    private const val COMPONENT_ENABLED_STATE_DISABLED = 2
    private const val COMPONENT_ENABLED_STATE_DISABLED_USER = 3
    private const val COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED = 4

    /**
     * Freeze an application using Shizuku.
     * Sets state to COMPONENT_ENABLED_STATE_DISABLED_USER (3).
     */
    fun freezeApp(context: Context, packageName: String): Boolean {
        return setApplicationEnabledSetting(context, packageName, COMPONENT_ENABLED_STATE_DISABLED_USER)
    }

    /**
     * Unfreeze an application using Shizuku.
     * Sets state to COMPONENT_ENABLED_STATE_ENABLED (1).
     */
    fun unfreezeApp(context: Context, packageName: String): Boolean {
        return setApplicationEnabledSetting(context, packageName, COMPONENT_ENABLED_STATE_ENABLED)
    }

    /**
     * Freeze all selected apps from settings that have auto-freeze ENABLED.
     */
    fun freezeAll(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("freeze_selected_apps", null)
        val excludedJson = prefs.getString("freeze_auto_excluded_apps", null)
        
        if (json != null) {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<com.sameerasw.essentials.domain.model.AppSelection>>() {}.type
            val excludedType = object : com.google.gson.reflect.TypeToken<Set<String>>() {}.type
            
            try {
                val apps: List<com.sameerasw.essentials.domain.model.AppSelection> = gson.fromJson(json, type)
                val excludedSet: Set<String> = if (excludedJson != null) {
                    gson.fromJson(excludedJson, excludedType) ?: emptySet()
                } else emptySet()
                
                apps.forEach { app ->
                    // Freezing happens if it's in the list AND not excluded
                    if (!excludedSet.contains(app.packageName)) {
                        freezeApp(context, app.packageName)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Freeze EVERY app in the selection list immediately.
     */
    fun freezeAllManual(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("freeze_selected_apps", null)
        if (json != null) {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<com.sameerasw.essentials.domain.model.AppSelection>>() {}.type
            try {
                val apps: List<com.sameerasw.essentials.domain.model.AppSelection> = gson.fromJson(json, type)
                apps.forEach { app ->
                    freezeApp(context, app.packageName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Unfreeze all picked apps from settings that have auto-freeze ENABLED.
     */
    fun unfreezeAll(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("freeze_selected_apps", null)
        val excludedJson = prefs.getString("freeze_auto_excluded_apps", null)
        
        if (json != null) {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<com.sameerasw.essentials.domain.model.AppSelection>>() {}.type
            val excludedType = object : com.google.gson.reflect.TypeToken<Set<String>>() {}.type
            
            try {
                val apps: List<com.sameerasw.essentials.domain.model.AppSelection> = gson.fromJson(json, type)
                val excludedSet: Set<String> = if (excludedJson != null) {
                    gson.fromJson(excludedJson, excludedType) ?: emptySet()
                } else emptySet()
                
                apps.forEach { app ->
                    if (!excludedSet.contains(app.packageName)) {
                        unfreezeApp(context, app.packageName)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Unfreeze all picked apps in settings, ignoring the auto-freeze toggle.
     */
    fun unfreezeAllManual(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("freeze_selected_apps", null)
        if (json != null) {
            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<com.sameerasw.essentials.domain.model.AppSelection>>() {}.type
            try {
                val apps: List<com.sameerasw.essentials.domain.model.AppSelection> = gson.fromJson(json, type)
                apps.forEach { app ->
                    unfreezeApp(context, app.packageName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Check if an application is currently frozen/disabled.
     */
    fun isAppFrozen(context: Context, packageName: String): Boolean {
        return try {
            val state = context.packageManager.getApplicationEnabledSetting(packageName)
            state == COMPONENT_ENABLED_STATE_DISABLED_USER || state == COMPONENT_ENABLED_STATE_DISABLED
        } catch (e: Exception) {
            false
        }
    }

    private fun setApplicationEnabledSetting(context: Context, packageName: String, newState: Int): Boolean {
        if (!ShellUtils.hasPermission(context)) {
            return false
        }

        val cmd = when (newState) {
            COMPONENT_ENABLED_STATE_DISABLED_USER -> "pm disable-user --user 0 $packageName"
            COMPONENT_ENABLED_STATE_ENABLED -> "pm enable $packageName"
            else -> return false
        }

        return try {
            ShellUtils.runCommand(context, cmd)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
