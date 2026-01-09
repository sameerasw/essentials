package com.sameerasw.essentials.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.domain.MapsState
import com.sameerasw.essentials.domain.model.NotificationLightingColorMode
import com.sameerasw.essentials.domain.model.NotificationLightingSide
import com.sameerasw.essentials.services.receivers.FlashlightActionReceiver
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import com.sameerasw.essentials.utils.AppUtil

class NotificationListener : NotificationListenerService() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)

        // Handle Snooze System Notifications
        try {
            val packageName = sbn.packageName
            val isSystem = packageName == "android" || packageName == "com.android.systemui"

            if (isSystem) {
                val extras = sbn.notification.extras
                val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
                val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
                val content = "$title $text"

                // 1. Debugging
                if (prefs.getBoolean("snooze_debugging_enabled", false)) {
                    val debugRegex = Regex("(?i).*(usb|wireless)\\s*debugging\\s*connected.*")
                    if (debugRegex.containsMatchIn(content)) {
                        snoozeNotification(sbn.key, 24 * 60 * 60 * 1000L) // Snooze for 24 hours
                    }
                }

                // 2. File Transfer
                if (prefs.getBoolean("snooze_file_transfer_enabled", false)) {
                    val fileTransferRegex = Regex("(?i).*usb\\s*file\\s*transfer.*")
                    if (fileTransferRegex.containsMatchIn(content)) {
                         snoozeNotification(sbn.key, 24 * 60 * 60 * 1000L)
                    }
                }

                // 3. Charging
                if (prefs.getBoolean("snooze_charging_enabled", false)) {
                    val chargingRegex = Regex("(?i).*charging\\s*this\\s*device.*")
                    if (chargingRegex.containsMatchIn(content)) {
                         snoozeNotification(sbn.key, 24 * 60 * 60 * 1000L)
                    }
                }
            }
        } catch (_: Exception) {
            // Safe to ignore
        }

        // trigger notification lighting for any newly posted notification if feature enabled
        try {
            val packageName = sbn.packageName
            val notification = sbn.notification
            val extras = notification.extras

            // Skip media sessions
            val isMedia = extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                    extras.getString(Notification.EXTRA_TEMPLATE) == $$"android.app.Notification$MediaStyle"
            
            if (isMedia) {
                Log.d("NotificationListener", "Skipping notification lighting for media notification from $packageName")
                return
            }

            val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            
            // Skip silent notifications if enabled
            val skipSilent = prefs.getBoolean("edge_lighting_skip_silent", true)
            if (skipSilent) {
                val ranking = Ranking()
                if (currentRanking.getRanking(sbn.key, ranking)) {
                    val importance =
                        ranking.importance

                    val isSilent =
                        importance <= android.app.NotificationManager.IMPORTANCE_LOW

                    if (isSilent) {
                        Log.d("NotificationListener", "Skipping notification lighting for silent notification from $packageName")
                        return
                    }
                }
            }
            
            // Skip persistent notifications if enabled
            val skipPersistent = prefs.getBoolean("edge_lighting_skip_persistent", false)
            if (skipPersistent && isPersistentNotification(notification)) {
                Log.d("NotificationListener", "Skipping notification lighting for persistent notification from $packageName")
                return
            }

            val enabled = prefs.getBoolean("edge_lighting_enabled", false)
            if (enabled) {
                // Check all required permissions before triggering notification lighting
                if (hasAllRequiredPermissions()) {
                    // Check if the app is selected for notification lighting
                    val appSelected = isAppSelectedForNotificationLighting(sbn.packageName)
                    Log.d("NotificationListener", "Notification lighting enabled, app ${sbn.packageName} selected: $appSelected")
                    if (appSelected) {
                        val cornerRadius = prefs.getInt("edge_lighting_corner_radius", 20)
                        val strokeThickness = prefs.getInt("edge_lighting_stroke_thickness", 8)
                        val colorModeName = prefs.getString("edge_lighting_color_mode", NotificationLightingColorMode.SYSTEM.name)
                        val colorMode = NotificationLightingColorMode.valueOf(colorModeName ?: NotificationLightingColorMode.SYSTEM.name)
                        val pulseCount = prefs.getInt("edge_lighting_pulse_count", 1)
                        val pulseDuration = prefs.getFloat("edge_lighting_pulse_duration", 3000f).toLong()
                        val styleName = prefs.getString("edge_lighting_style", com.sameerasw.essentials.domain.model.NotificationLightingStyle.STROKE.name)
                        
                        val gson = com.google.gson.Gson()
                        val glowSidesJson = prefs.getString("edge_lighting_glow_sides", null)
                        val glowSides: Set<NotificationLightingSide> = if (glowSidesJson != null) {
                            val type = object : com.google.gson.reflect.TypeToken<Set<NotificationLightingSide>>() {}.type
                            try { gson.fromJson(glowSidesJson, type) } catch (_: Exception) { setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT) }
                        } else {
                            setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
                        }
                        
                        val indicatorX = prefs.getFloat("edge_lighting_indicator_x", 50f)
                        val indicatorY = prefs.getFloat("edge_lighting_indicator_y", 2f)
                        val indicatorScale = prefs.getFloat("edge_lighting_indicator_scale", 1.0f)

                        fun startNotificationLighting(resolvedColor: Int? = null) {
                            val intent = Intent(applicationContext, NotificationLightingService::class.java).apply {
                                putExtra("corner_radius_dp", cornerRadius)
                                putExtra("stroke_thickness_dp", strokeThickness)
                                putExtra("color_mode", colorMode.name)
                                putExtra("pulse_count", pulseCount)
                                putExtra("pulse_duration", pulseDuration)
                                putExtra("style", styleName)
                                putExtra("glow_sides", glowSides.map { it.name }.toTypedArray())
                                putExtra("indicator_x", indicatorX)
                                putExtra("indicator_y", indicatorY)
                                putExtra("indicator_scale", indicatorScale)
                                if (resolvedColor != null) {
                                    putExtra("resolved_color", resolvedColor)
                                } else if (colorMode == NotificationLightingColorMode.CUSTOM) {
                                    putExtra("custom_color", prefs.getInt("edge_lighting_custom_color", 0xFF6200EE.toInt()))
                                }
                                putExtra("is_ambient_display", prefs.getBoolean("edge_lighting_ambient_display", false))
                                putExtra("is_ambient_show_lock_screen", prefs.getBoolean("edge_lighting_ambient_show_lock_screen", false))
                            }
                            applicationContext.startForegroundService(intent)
                        }

                        if (colorMode == NotificationLightingColorMode.APP_SPECIFIC) {
                            AppUtil.getAppBrandColor(applicationContext, sbn.packageName) { brandColor ->
                                startNotificationLighting(brandColor)
                            }
                        } else {
                            startNotificationLighting()
                        }

                        // Also trigger flashlight pulse if enabled
                        if (prefs.getBoolean("flashlight_pulse_enabled", false)) {
                            val pulseIntent = Intent(applicationContext, FlashlightActionReceiver::class.java).apply {
                                action = FlashlightActionReceiver.ACTION_PULSE_NOTIFICATION
                            }
                            applicationContext.sendBroadcast(pulseIntent)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // ignore failures
        }

        if (sbn.packageName == "com.google.android.apps.maps") {
            MapsState.hasNavigationNotification = isNavigationNotification(sbn)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.google.android.apps.maps") {
            MapsState.hasNavigationNotification = false
        }
    }

    private fun hasAllRequiredPermissions(): Boolean {
        // Check overlay permission
        if (!canDrawOverlays()) {
            return false
        }

        // Check accessibility service is enabled
        if (!isAccessibilityServiceEnabled()) {
            return false
        }

        return true
    }

    private fun canDrawOverlays(): Boolean {
            return Settings.canDrawOverlays(applicationContext)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val serviceName = "${applicationContext.packageName}/${ScreenOffAccessibilityService::class.java.name}"
            enabledServices?.contains(serviceName) == true
        } catch (_: Exception) {
            false
        }
    }

    private fun isNavigationNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        if (!isPersistentNotification(notification)) return false
        return hasNavigationCategory(notification)
    }

    private fun isPersistentNotification(notification: Notification): Boolean {
        return (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
    }

    private fun hasNavigationCategory(notification: Notification): Boolean {
        val category = notification.category ?: return false
        val navigationRegex = Regex("(?i).*navigation.*")
        return navigationRegex.containsMatchIn(category)
    }

    private fun isAppSelectedForNotificationLighting(packageName: String): Boolean {
        try {
            val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)

            // Check if only show when screen off is enabled
            val onlyShowWhenScreenOff = prefs.getBoolean("edge_lighting_only_screen_off", true)
            if (onlyShowWhenScreenOff) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val isScreenOn =
                    powerManager.isInteractive
                if (isScreenOn) {
                    Log.d("NotificationListener", "Screen is ON and 'Only show when screen off' is enabled. Skipping notification lighting.")
                    return false
                }
            }

            val json = prefs.getString("edge_lighting_selected_apps", null) ?: return true

            // If no saved preferences, allow all apps by default

            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<com.sameerasw.essentials.domain.model.AppSelection>>() {}.type
            val selectedApps: List<com.sameerasw.essentials.domain.model.AppSelection> = gson.fromJson(json, type)

            // Find the app in the saved list
            val app = selectedApps.find { it.packageName == packageName }
            return app?.isEnabled ?: true // Default to true if app not found

        } catch (_: Exception) {
            // If there's an error, default to allowing all apps (backward compatibility)
            return true
        }
    }
}