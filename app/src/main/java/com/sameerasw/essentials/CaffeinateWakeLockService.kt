package com.sameerasw.essentials

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class CaffeinateWakeLockService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        startForeground()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "Caffeinate::WakeLock")
        wakeLock?.acquire()
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.release()
        wakeLock = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startForeground() {
        val channelId = "caffeinate_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Caffeinate", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Caffeinate Active")
            .setContentText("Keeping screen awake")
            .setSmallIcon(R.drawable.rounded_coffee_24)
            .build()
        startForeground(1, notification)
    }
}
