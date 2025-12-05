package com.sameerasw.essentials.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sameerasw.essentials.MapsState
import com.sameerasw.essentials.services.ScreenOffAccessibilityService

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // trigger edge lighting for any newly posted notification if feature enabled
        try {
            val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("edge_lighting_enabled", false)
            if (enabled) {
                // Check all required permissions before triggering edge lighting
                if (hasAllRequiredPermissions()) {
                    // Check if the app is selected for edge lighting
                    val appSelected = isAppSelectedForEdgeLighting(sbn.packageName)
                    android.util.Log.d("NotificationListener", "Edge lighting enabled, app ${sbn.packageName} selected: $appSelected")
                    if (appSelected) {
                        // Start the overlay service to show the lighting
                        val intent = Intent(applicationContext, EdgeLightingService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            applicationContext.startForegroundService(intent)
                        } else {
                            applicationContext.startService(intent)
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