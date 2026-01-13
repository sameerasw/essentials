package com.sameerasw.essentials.services.automation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.automation.modules.AutomationModule

class AutomationService : Service() {

    companion object {
        private const val CHANNEL_ID = "automation_service_channel"
        private const val NOTIFICATION_ID = 999
        var isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification(), 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        
        // Modules will be started by AutomationManager calling onServiceCreated/Updated
        AutomationManager.onServiceConnected(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        AutomationManager.onServiceDisconnected(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.automation_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.automation_service_running_title))
            .setContentText(getString(R.string.automation_service_running_desc))
            .setSmallIcon(R.drawable.outline_bubble_chart_24)
            .setOngoing(true)
            .build()
    }
}
