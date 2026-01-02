package com.sameerasw.essentials.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sameerasw.essentials.MapsState
import com.sameerasw.essentials.domain.model.EdgeLightingColorMode
import com.sameerasw.essentials.services.ScreenOffAccessibilityService
import com.sameerasw.essentials.utils.AppUtil

class NotificationListener : NotificationListenerService() {

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
        } catch (e: Exception) {
            // Safe to ignore
        }

        // trigger edge lighting for any newly posted notification if feature enabled
        try {
            val packageName = sbn.packageName
            val notification = sbn.notification
            val extras = notification.extras

            // Skip media sessions
            val isMedia = extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                    extras.getString(Notification.EXTRA_TEMPLATE) == "android.app.Notification\$MediaStyle"
            
            if (isMedia) {
                android.util.Log.d("NotificationListener", "Skipping edge lighting for media notification from $packageName")
                return
            }

            val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            
            // Skip silent notifications if enabled
            val skipSilent = prefs.getBoolean("edge_lighting_skip_silent", true)
            if (skipSilent) {
                val ranking = Ranking()
                if (currentRanking.getRanking(sbn.key, ranking)) {
                    val importance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ranking.importance
                    } else {
                        @Suppress("DEPRECATION")
                        notification.priority
                    }
                    
                    val isSilent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        importance <= android.app.NotificationManager.IMPORTANCE_LOW
                    } else {
                        @Suppress("DEPRECATION")
                        importance <= Notification.PRIORITY_LOW
                    }
                    
                    if (isSilent) {
                        android.util.Log.d("NotificationListener", "Skipping edge lighting for silent notification from $packageName")
                        return
                    }
                }
            }

            val enabled = prefs.getBoolean("edge_lighting_enabled", false)
            if (enabled) {
                // Check all required permissions before triggering edge lighting
                if (hasAllRequiredPermissions()) {
                    // Check if the app is selected for edge lighting
                    val appSelected = isAppSelectedForEdgeLighting(sbn.packageName)
                    android.util.Log.d("NotificationListener", "Edge lighting enabled, app ${sbn.packageName} selected: $appSelected")
                    if (appSelected) {
                        val cornerRadius = prefs.getInt("edge_lighting_corner_radius", 20)
                        val strokeThickness = prefs.getInt("edge_lighting_stroke_thickness", 8)
                        val colorModeName = prefs.getString("edge_lighting_color_mode", EdgeLightingColorMode.SYSTEM.name)
                        val colorMode = EdgeLightingColorMode.valueOf(colorModeName ?: EdgeLightingColorMode.SYSTEM.name)
                        
                        fun startEdgeLighting(resolvedColor: Int? = null) {
                            val intent = Intent(applicationContext, EdgeLightingService::class.java).apply {
                                putExtra("corner_radius_dp", cornerRadius)
                                putExtra("stroke_thickness_dp", strokeThickness)
                                putExtra("color_mode", colorMode.name)
                                if (resolvedColor != null) {
                                    putExtra("resolved_color", resolvedColor)
                                } else if (colorMode == EdgeLightingColorMode.CUSTOM) {
                                    putExtra("custom_color", prefs.getInt("edge_lighting_custom_color", 0xFF6200EE.toInt()))
                                }
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                applicationContext.startForegroundService(intent)
                            } else {
                                applicationContext.startService(intent)
                            }
                        }

                        if (colorMode == EdgeLightingColorMode.APP_SPECIFIC) {
                            AppUtil.getAppBrandColor(applicationContext, sbn.packageName) { brandColor ->
                                startEdgeLighting(brandColor)
                            }
                        } else {
                            startEdgeLighting()
                        }
                    }
                }
            }
        } catch (e: Exception) {
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(applicationContext)
        } else {
            true
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val serviceName = "${applicationContext.packageName}/${ScreenOffAccessibilityService::class.java.name}"
            enabledServices?.contains(serviceName) == true
        } catch (e: Exception) {
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

    private fun isAppSelectedForEdgeLighting(packageName: String): Boolean {
        try {
            val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)

            // Check if only show when screen off is enabled
            val onlyShowWhenScreenOff = prefs.getBoolean("edge_lighting_only_screen_off", true)
            if (onlyShowWhenScreenOff) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val isScreenOn = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
                    powerManager.isInteractive
                } else {
                    @Suppress("DEPRECATION")
                    powerManager.isScreenOn
                }
                if (isScreenOn) {
                    android.util.Log.d("NotificationListener", "Screen is ON and 'Only show when screen off' is enabled. Skipping edge lighting.")
                    return false
                }
            }

            val json = prefs.getString("edge_lighting_selected_apps", null)

            // If no saved preferences, allow all apps by default
            if (json == null) {
                return true
            }

            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<com.sameerasw.essentials.domain.model.NotificationApp>>() {}.type
            val selectedApps: List<com.sameerasw.essentials.domain.model.NotificationApp> = gson.fromJson(json, type)

            // Find the app in the saved list
            val app = selectedApps.find { it.packageName == packageName }
            return app?.isEnabled ?: true // Default to true if app not found

        } catch (e: Exception) {
            // If there's an error, default to allowing all apps (backward compatibility)
            return true
        }
    }
}