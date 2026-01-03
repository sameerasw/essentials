package com.sameerasw.essentials.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R

class CaffeinateWakeLockService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isNotificationShown = false
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "Caffeinate::WakeLock")
        wakeLock?.acquire()

        // Setup SharedPreferences listener
        prefs = getSharedPreferences("caffeinate_prefs", MODE_PRIVATE)
        prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "show_notification") {
                updateNotificationState()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)

        // Initial state
        updateNotificationState()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        if (isNotificationShown) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.cancel(2)
        }
        wakeLock?.release()
        wakeLock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "caffeinate_channel",
            "Caffeinate",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }


    private fun updateNotificationState() {
        val shouldShow = prefs.getBoolean("show_notification", false)
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (shouldShow && !isNotificationShown) {
            // Show the notification
            val stopIntent = Intent(this, CaffeinateWakeLockService::class.java).apply { action = "STOP" }
            val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val configureIntent = Intent(this, FeatureSettingsActivity::class.java).apply { putExtra("feature", "Caffeinate") }
            val configurePendingIntent = PendingIntent.getActivity(this, 1, configureIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val notification = NotificationCompat.Builder(this, "caffeinate_channel")
                .setContentTitle("Caffeinate Active")
                .setContentText("Screen is being kept awake")
                .setSmallIcon(R.drawable.rounded_coffee_24)
                .setOngoing(true)
                .setSilent(true)
                .setCategory("Caffeinate")
                .addAction(R.drawable.rounded_stop_circle_24, "Stop", stopPendingIntent)
                .addAction(R.drawable.rounded_settings_accessibility_24, "Configure", configurePendingIntent)
                .build()

            notificationManager.notify(2, notification)
            isNotificationShown = true
        } else if (!shouldShow && isNotificationShown) {
            // Dismiss the notification
            notificationManager.cancel(2)
            isNotificationShown = false
        }
    }
}
