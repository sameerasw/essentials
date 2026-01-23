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
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            // Initial discovery from active notifications
            activeNotifications?.forEach { sbn ->
                val isSystem = sbn.packageName == "android" || sbn.packageName == "com.android.systemui"
                if (isSystem) {
                    discoverSystemChannel(sbn.packageName, sbn.notification.channelId, sbn.user)
                }
            }
        } catch (_: Exception) {}
    }

    private fun discoverSystemChannel(packageName: String, channelId: String?, userHandle: android.os.UserHandle) {
        if (channelId.isNullOrBlank()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                val discoveredJson = prefs.getString("snooze_discovered_channels", null)
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<com.sameerasw.essentials.domain.model.SnoozeChannel>>() {}.type
                val discoveredChannels: MutableList<com.sameerasw.essentials.domain.model.SnoozeChannel> = if (discoveredJson != null) {
                    try { gson.fromJson(discoveredJson, type) ?: mutableListOf() } catch (_: Exception) { mutableListOf() }
                } else mutableListOf()

                if (discoveredChannels.none { it.id == channelId }) {
                    var foundName: String? = null
                    try {
                        val channels = getNotificationChannels(packageName, userHandle)
                        val channel = channels.find { it.id == channelId }
                        foundName = channel?.name?.toString()
                    } catch (_: Exception) {}

                    val name = if (!foundName.isNullOrBlank()) foundName 
                               else channelId.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                    
                    val finalName = if (packageName == "android") name else "[$packageName] $name"
                    
                    discoveredChannels.add(com.sameerasw.essentials.domain.model.SnoozeChannel(channelId, finalName))
                    prefs.edit().putString("snooze_discovered_channels", gson.toJson(discoveredChannels)).apply()
                }
            } catch (_: Exception) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        onNotificationPostedInternal(sbn)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        onNotificationPostedInternal(sbn)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onNotificationPostedInternal(sbn: StatusBarNotification) {
        // Skip our own app's notifications early to avoid flooding logs and redundant processing
        if (sbn.packageName == packageName) {
            return
        }

        val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)

        // Maps navigation state update
        if (sbn.packageName == "com.google.android.apps.maps") {
            MapsState.hasNavigationNotification = isNavigationNotification(sbn)
        }

        // Handle Snooze System Notifications
        try {
            val pkg = sbn.packageName
            val isSystem = pkg == "android" || pkg.startsWith("com.android.") || pkg == "com.google.android.gms"
            
            if (isSystem) {
                val channelId = sbn.notification.channelId
                
                // 1. Discovery
                discoverSystemChannel(pkg, channelId, sbn.user)

                // 2. Snoozing
                if (channelId != null) {
                    val blockedChannelsJson = prefs.getString("snooze_blocked_channels", null)
                    val blockedChannels: Set<String> = if (blockedChannelsJson != null) {
                        try {
                            val type = object : com.google.gson.reflect.TypeToken<Set<String>>() {}.type
                            com.google.gson.Gson().fromJson(blockedChannelsJson, type) ?: emptySet()
                        } catch (_: Exception) { emptySet() }
                    } else emptySet()

                    if (blockedChannels.contains(channelId)) {
                        snoozeNotification(sbn.key, 24 * 60 * 60 * 1000L) // Snooze for 24 hours
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
                    extras.getString(Notification.EXTRA_TEMPLATE) == "android.app.Notification\$MediaStyle"
            
            if (isMedia) {
                    return
            }

            val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            
            // Skip silent notifications if enabled
            val skipSilent = prefs.getBoolean("edge_lighting_skip_silent", true)
            if (skipSilent) {
                val ranking = Ranking()
                if (currentRanking.getRanking(sbn.key, ranking)) {
                    val importance = ranking.importance
                    val isSilent = importance <= android.app.NotificationManager.IMPORTANCE_LOW
                    if (isSilent) {
                        return
                    }
                }
            }
            
            // Skip persistent notifications if enabled
            val skipPersistent = prefs.getBoolean("edge_lighting_skip_persistent", false)
            if (skipPersistent && isPersistentNotification(notification)) {
                return
            }

            val enabled = prefs.getBoolean("edge_lighting_enabled", false)
            if (enabled) {
                // Check all required permissions before triggering notification lighting
                val hasPermissions = hasAllRequiredPermissions()
                if (hasPermissions) {
                    // Check if the app is selected for notification lighting
                    val appSelected = isAppSelectedForNotificationLighting(sbn.packageName)
                    if (appSelected) {
                        val cornerRadius = try {
                            prefs.getFloat("edge_lighting_corner_radius", 20f)
                        } catch (e: ClassCastException) {
                            prefs.getInt("edge_lighting_corner_radius", 20).toFloat()
                        }
                        val strokeThickness = try {
                            prefs.getFloat("edge_lighting_stroke_thickness", 8f)
                        } catch (e: ClassCastException) {
                            prefs.getInt("edge_lighting_stroke_thickness", 8).toFloat()
                        }
                        val colorModeName = prefs.getString("edge_lighting_color_mode", NotificationLightingColorMode.SYSTEM.name)
                        val colorMode = NotificationLightingColorMode.valueOf(colorModeName ?: NotificationLightingColorMode.SYSTEM.name)
                        val pulseCount = try {
                            prefs.getInt("edge_lighting_pulse_count", 1)
                        } catch (e: ClassCastException) {
                            prefs.getFloat("edge_lighting_pulse_count", 1f).toInt()
                        }
                        val pulseDuration = try {
                            prefs.getFloat("edge_lighting_pulse_duration", 3000f).toLong()
                        } catch (e: ClassCastException) {
                            prefs.getInt("edge_lighting_pulse_duration", 3000).toLong()
                        }
                        val styleName = prefs.getString("edge_lighting_style", com.sameerasw.essentials.domain.model.NotificationLightingStyle.STROKE.name)
                        
                        val gson = com.google.gson.Gson()
                        val glowSidesJson = prefs.getString("edge_lighting_glow_sides", null)
                        val glowSides: Set<NotificationLightingSide> = if (glowSidesJson != null) {
                            val type = object : com.google.gson.reflect.TypeToken<Set<NotificationLightingSide>>() {}.type
                            try { gson.fromJson(glowSidesJson, type) } catch (_: Exception) { setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT) }
                        } else {
                            setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
                        }
                        
                        val indicatorX = try {
                            prefs.getFloat("edge_lighting_indicator_x", 50f)
                        } catch (e: ClassCastException) {
                            prefs.getInt("edge_lighting_indicator_x", 50).toFloat()
                        }
                        val indicatorY = try {
                            prefs.getFloat("edge_lighting_indicator_y", 2f)
                        } catch (e: ClassCastException) {
                            prefs.getInt("edge_lighting_indicator_y", 2).toFloat()
                        }
                        val indicatorScale = try {
                            prefs.getFloat("edge_lighting_indicator_scale", 1.0f)
                        } catch (e: ClassCastException) {
                            prefs.getInt("edge_lighting_indicator_scale", 1).toFloat()
                        }

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

        // Check accessibility service is enabled - only required for Android 12+ AOD support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!isAccessibilityServiceEnabled()) {
                return false
            }
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
                val isScreenOn = powerManager.isInteractive
                if (isScreenOn) {
                    return false
                }
            }

            val json = prefs.getString("edge_lighting_selected_apps", null)
            if (json == null) {
                return true
            }

            // If no saved preferences, allow all apps by default

            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<com.sameerasw.essentials.domain.model.AppSelection>>() {}.type
            val selectedApps: List<com.sameerasw.essentials.domain.model.AppSelection> = gson.fromJson(json, type)

            // Find the app in the saved list
            val app = selectedApps.find { it.packageName == packageName }
            val result = app?.isEnabled ?: true
            return result

        } catch (_: Exception) {
            // If there's an error, default to allowing all apps (backward compatibility)
            return true
        }
    }
}