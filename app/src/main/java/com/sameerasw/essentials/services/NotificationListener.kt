package com.sameerasw.essentials.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sameerasw.essentials.MapsState

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // trigger edge lighting for any newly posted notification if feature enabled
        try {
            val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("edge_lighting_enabled", false)
            if (enabled) {
                // Start the overlay service to show the lighting
                val intent = Intent(applicationContext, EdgeLightingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(intent)
                } else {
                    applicationContext.startService(intent)
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
}