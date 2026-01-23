package com.sameerasw.essentials.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.LocationReachedRepository
import com.sameerasw.essentials.MainActivity
import kotlinx.coroutines.*
import kotlin.math.*

class LocationReachedService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var trackingJob: Job? = null
    private var isAlarmTriggered = false
    
    private val repository by lazy { LocationReachedRepository(this) }
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val ALARM_NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_reached_live"
        private const val ACTION_STOP = "com.sameerasw.essentials.STOP_LOCATION_REACHED"

        fun start(context: Context) {
            val intent = Intent(context, LocationReachedService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationReachedService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking()
            return START_NOT_STICKY
        }

        isAlarmTriggered = false
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildInitialNotification())
        startTracking()
        
        return START_STICKY
    }

    private fun startTracking() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            while (isActive) {
                val alarm = repository.getAlarm()
                if (alarm.isEnabled && alarm.latitude != 0.0 && alarm.longitude != 0.0) {
                    updateProgress(alarm)
                } else {
                    stopSelf()
                    break
                }
                delay(10000) // Update every 10 seconds for better responsiveness
            }
        }
    }

    private fun stopTracking() { 
        val alarm = repository.getAlarm()
        repository.saveAlarm(alarm.copy(isEnabled = false))
        stopSelf()
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun updateProgress(alarm: com.sameerasw.essentials.domain.model.LocationAlarm) {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    val distance = calculateDistance(it.latitude, it.longitude, alarm.latitude, alarm.longitude)
                    val distanceKm = distance / 1000f
                    
                    // Watchdog: If we reached the radius but geofence didn't trigger
                    if (distance <= alarm.radius && !isAlarmTriggered) {
                        isAlarmTriggered = true
                        triggerArrivalAlarm()
                    }
                    
                    updateNotification(distanceKm)
                }
            }
    }

    private fun triggerArrivalAlarm() {
        val channelId = "location_reached_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.feat_location_reached_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(this, com.sameerasw.essentials.ui.activities.LocationAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.rounded_navigation_24)
            .setContentTitle(getString(R.string.location_reached_notification_title))
            .setContentText(getString(R.string.location_reached_notification_desc))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(ALARM_NOTIFICATION_ID, notification)
    }

    private fun updateNotification(distanceKm: Float) {
        val startDist = repository.getStartDistance()
        val progressPercent = if (startDist > 0) {
            ((1.0f - (distanceKm * 1000f / startDist)) * 100).toInt().coerceIn(0, 100)
        } else 0
        
        val notification = buildOngoingNotification(distanceKm, progressPercent)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildInitialNotification(): Notification {
        return buildOngoingNotification(null, 0)
    }

    private fun buildOngoingNotification(distanceKm: Float?, progress: Int): Notification {
        val stopIntent = Intent(this, LocationReachedService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("feature", "Location reached")
        }
        val mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val distanceText = distanceKm?.let { 
            if (it < 1.0) getString(R.string.location_reached_dist_m, (it * 1000).toInt()) 
            else getString(R.string.location_reached_dist_km, it)
        } ?: getString(R.string.location_reached_calculating)
        
        val contentText = getString(R.string.location_reached_service_remaining, distanceText, progress)

        if (Build.VERSION.SDK_INT >= 35) {
            val builder = Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.rounded_navigation_24)
                .setContentTitle(getString(R.string.location_reached_service_title))
                .setContentText(contentText)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(mainPendingIntent)
                .addAction(Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.rounded_power_settings_new_24),
                    getString(R.string.location_reached_stop_tracking), stopPendingIntent).build())

            if (Build.VERSION.SDK_INT >= 36) {
                try {
                    val progressStyle = Notification.ProgressStyle()
                        .setStyledByProgress(true)
                        .setProgress(progress)
                        .setProgressTrackerIcon(Icon.createWithResource(this, R.drawable.rounded_navigation_24))
                    builder.setStyle(progressStyle)
                } catch (_: Throwable) {
                    builder.setProgress(100, progress, false)
                }
            } else {
                builder.setProgress(100, progress, false)
            }

            try {
                val extras = android.os.Bundle()
                extras.putBoolean("android.requestPromotedOngoing", true)
                extras.putBoolean("android.substituteContextualActions", true)
                distanceKm?.let { extras.putString("android.shortCriticalText", distanceText) }
                builder.addExtras(extras)
                
                builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                    .invoke(builder, true)
                
                distanceKm?.let {
                    builder.javaClass.getMethod("setShortCriticalText", CharSequence::class.java)
                        .invoke(builder, distanceText)
                }
            } catch (_: Throwable) {}

            return builder.build()
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.rounded_navigation_24)
            .setContentTitle(getString(R.string.location_reached_service_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(mainPendingIntent)
            .setProgress(100, progress, false)
            .addAction(R.drawable.rounded_power_settings_new_24, getString(R.string.location_reached_stop_tracking), stopPendingIntent)

        val extras = android.os.Bundle()
        extras.putBoolean("android.requestPromotedOngoing", true)
        distanceKm?.let { extras.putString("android.shortCriticalText", distanceText) }
        builder.addExtras(extras)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.location_reached_channel_name),
                NotificationManager.IMPORTANCE_HIGH // Increased importance
            ).apply {
                description = getString(R.string.location_reached_channel_desc)
                setShowBadge(false)
                setLockscreenVisibility(Notification.VISIBILITY_PUBLIC) // Ensure it's visible on lockscreen
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371e3
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toFloat()
    }

    override fun onDestroy() {
        trackingJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
