package com.sameerasw.essentials.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.handlers.AppFlowHandler

class AppDetectionService : Service() {

    private lateinit var appFlowHandler: AppFlowHandler
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = false
    private var lastPackageName: String? = null

    companion object {
        private const val CHANNEL_ID = "app_detection_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL = 500L
        var isRunning = false
    }

    private val authReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "APP_AUTHENTICATED" -> {
                    val packageName = intent.getStringExtra("package_name")
                    if (packageName != null) {
                        appFlowHandler.onAuthenticated(packageName)
                    }
                }

                "APP_AUTHENTICATION_FAILED" -> {
                    goHome()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        appFlowHandler = AppFlowHandler(this)
        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction("APP_AUTHENTICATED")
            addAction("APP_AUTHENTICATION_FAILED")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(authReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(authReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIFICATION_ID,
            createNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )

        if (!isPolling) {
            isPolling = true
            startPolling()
        }

        return START_STICKY
    }

    private fun startPolling() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isPolling) return

                val currentPackage = getForegroundPackage()
                if (currentPackage != null && currentPackage != lastPackageName) {
                    lastPackageName = currentPackage
                    appFlowHandler.onPackageChanged(currentPackage, isFromUsageStats = true)
                }

                handler.postDelayed(this, POLL_INTERVAL)
            }
        }, POLL_INTERVAL)
    }

    private fun getForegroundPackage(): String? {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            time - 1000 * 10,
            time
        )

        if (stats == null || stats.isEmpty()) return null

        var recentStats: UsageStats? = null
        for (usageStats in stats) {
            if (recentStats == null || usageStats.lastTimeUsed > recentStats.lastTimeUsed) {
                recentStats = usageStats
            }
        }

        return recentStats?.packageName
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
    }

    override fun onDestroy() {
        isRunning = false
        isPolling = false
        handler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(authReceiver)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_detection_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.app_detection_service_running_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_detection_service_running_title))
            .setContentText(getString(R.string.app_detection_service_running_desc))
            .setSmallIcon(R.drawable.rounded_shield_lock_24)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
