package com.sameerasw.essentials.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.MainActivity
import com.sameerasw.essentials.domain.controller.CaffeinateController

class CaffeinateWakeLockService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var abortWithScreenOff = true
    private var timeoutMinutes = -1
    private var remainingMillis = 0L
    private var startTime = 0L

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (timeoutMinutes == -1) return
            
            val elapsed = System.currentTimeMillis() - startTime
            remainingMillis = (timeoutMinutes * 60 * 1000L) - elapsed
            
            if (remainingMillis <= 0) {
                CaffeinateController.isActive.value = false
                stopSelf()
                return
            }
            
            updateNotification()
            handler.postDelayed(this, 1000)
        }
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF && abortWithScreenOff) {
                stopSelf()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        updatePrefs()

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "Caffeinate::WakeLock")
        wakeLock?.acquire()

        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        // Initial state - Always show notification for foreground service
        startForeground(2, createNotification())
    }

    override fun onDestroy() {
        CaffeinateController.isActive.value = false
        handler.removeCallbacks(countdownRunnable)
        try { unregisterReceiver(screenOffReceiver) } catch (_: Exception) {}
        super.onDestroy()
        wakeLock?.release()
        wakeLock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "STOP" -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            "UPDATE_PREFS" -> {
                updatePrefs()
            }
            else -> {
                // Feature start or update
                val newTimeout = intent?.getIntExtra("timeout_minutes", -1) ?: -1
                if (newTimeout != timeoutMinutes) {
                    timeoutMinutes = newTimeout
                    if (timeoutMinutes != -1) {
                        startTime = System.currentTimeMillis()
                        handler.removeCallbacks(countdownRunnable)
                        handler.post(countdownRunnable)
                    } else {
                        handler.removeCallbacks(countdownRunnable)
                        updateNotification()
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun updatePrefs() {
        val prefs = getSharedPreferences("caffeinate_prefs", MODE_PRIVATE)
        abortWithScreenOff = prefs.getBoolean("abort_screen_off", true)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "caffeinate_live",
            getString(R.string.feat_caffeinate_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.caffeinate_live_channel_desc)
            setShowBadge(false)
            setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
            setSound(null, null)
            enableVibration(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): android.app.Notification {
        val stopIntent = Intent(this, CaffeinateWakeLockService::class.java).apply { action = "STOP" }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val mainIntent = Intent(this, MainActivity::class.java).apply { putExtra("feature", "Caffeinate") }
        val mainPendingIntent = PendingIntent.getActivity(this, 1, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val activeText = if (timeoutMinutes == -1) "∞" else {
            val totalSeconds = (remainingMillis / 1000).toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            if (minutes > 0) "${minutes}m" else "${seconds}s"
        }
        
        val descText = if (timeoutMinutes == -1) {
            getString(R.string.caffeinate_notification_desc)
        } else {
            val totalSeconds = (remainingMillis / 1000).toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val timeStr = if (minutes > 0) "${minutes}m" else "${seconds}s"
            getString(R.string.caffeinate_remaining, timeStr)
        }

        if (Build.VERSION.SDK_INT >= 35) {
            val builder = Notification.Builder(this, "caffeinate_live")
                .setSmallIcon(R.drawable.rounded_coffee_24)
                .setContentTitle(getString(R.string.caffeinate_notification_title))
                .setContentText(descText)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(mainPendingIntent)
                .addAction(Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.rounded_stop_circle_24),
                    getString(R.string.action_stop), stopPendingIntent).build())

            try {
                val extras = android.os.Bundle()
                extras.putBoolean("android.requestPromotedOngoing", true)
                extras.putString("android.shortCriticalText", activeText)
                builder.addExtras(extras)
                
                Notification.Builder::class.java.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                    .invoke(builder, true)
                Notification.Builder::class.java.getMethod("setShortCriticalText", CharSequence::class.java)
                    .invoke(builder, activeText)
            } catch (_: Throwable) {}

            return builder.build()
        }

        val builder = NotificationCompat.Builder(this, "caffeinate_live")
            .setSmallIcon(R.drawable.rounded_coffee_24)
            .setContentTitle(getString(R.string.caffeinate_notification_title))
            .setContentText(descText)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.rounded_stop_circle_24, getString(R.string.action_stop), stopPendingIntent)

        val extras = android.os.Bundle()
        extras.putBoolean("android.requestPromotedOngoing", true)
        extras.putString("android.shortCriticalText", activeText)
        builder.addExtras(extras)

        return builder.build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(2, createNotification())
    }
}
