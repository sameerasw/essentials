package com.sameerasw.essentials.utils

import android.content.Context

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
        val dontFreezeActive = prefs.getBoolean("freeze_dont_freeze_active_apps", false)
        
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
                        if (!dontFreezeActive || !isAppActive(context, app.packageName)) {
                            freezeApp(context, app.packageName)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isAppActive(context: Context, packageName: String): Boolean {
        // 1. Check Foreground
        if (isAppInForeground(context, packageName)) return true

        // 2. Check Media Sessions
        if (isAppStreamingMedia(context, packageName)) return true

        // 3. Check Ongoing Notifications
        if (hasOngoingNotification(context, packageName)) return true

        return false
    }

    private fun isAppInForeground(context: Context, packageName: String): Boolean {
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val time = System.currentTimeMillis()
            val events = usageStatsManager.queryEvents(time - 10 * 1000, time)
            val event = android.app.usage.UsageEvents.Event()
            var lastEvent = -1
            
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.packageName == packageName) {
                    lastEvent = event.eventType
                }
            }
            return lastEvent == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED || 
                   lastEvent == android.app.usage.UsageEvents.Event.USER_INTERACTION
        } catch (_: Exception) {
            return false
        }
    }

    private fun isAppStreamingMedia(context: Context, packageName: String): Boolean {
        try {
            // Check AudioManager first for general music playback
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            if (!audioManager.isMusicActive) return false

            // Check Media Sessions for specific package
            val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
            val componentName = android.content.ComponentName(context, "com.sameerasw.essentials.services.NotificationListener")
            val sessions = mediaSessionManager.getActiveSessions(componentName)
            
            return sessions.any { it.packageName == packageName && it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }
        } catch (_: Exception) {
            return false
        }
    }

    private fun hasOngoingNotification(context: Context, packageName: String): Boolean {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val activeNotifications = notificationManager.activeNotifications
            return activeNotifications.any { 
                it.packageName == packageName && (it.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0 
            }
        } catch (_: Exception) {
            return false
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
