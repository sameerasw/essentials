package com.sameerasw.essentials.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sameerasw.essentials.MainActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.LocationReachedRepository
import com.sameerasw.essentials.domain.model.LocationAlarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LocationReachedService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var trackingJob: Job? = null
    private var isAlarmTriggered = false

    private val repository by lazy { LocationReachedRepository(this) }
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val ALARM_NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_reached_live"
        const val ACTION_STOP = "com.sameerasw.essentials.STOP_LOCATION_REACHED"
        const val ACTION_PAUSE = "com.sameerasw.essentials.PAUSE_LOCATION_REACHED"
        const val ACTION_RESUME = "com.sameerasw.essentials.RESUME_LOCATION_REACHED"

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
        if (intent?.action == ACTION_PAUSE) {
            pauseTracking()
            return START_STICKY
        }
        if (intent?.action == ACTION_RESUME) {
            resumeTracking()
            return START_STICKY
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
                val activeId = repository.getActiveAlarmId()
                val alarms = repository.getAlarms()
                val alarm = alarms.find { it.id == activeId }

                if (alarm != null) {
                    updateProgress(alarm)
                } else {
                    stopSelf()
                    break
                }
                delay(10000)
            }
        }
    }

    private fun stopTracking() {
        val activeId = repository.getActiveAlarmId()
        val alarms = repository.getAlarms()
        val alarm = alarms.find { it.id == activeId }

        if (alarm != null) {
            repository.saveLastTrip(alarm)
            repository.updatePausedState(alarm.id, false)
        }

        repository.saveActiveAlarmId(null)
        stopSelf()
    }

    private fun pauseTracking() {
        val activeId = repository.getActiveAlarmId() ?: return
        repository.updatePausedState(activeId, true)
        // Force an update to show paused state in notification
        val alarms = repository.getAlarms()
        val alarm = alarms.find { it.id == activeId }
        if (alarm != null) {
            updateNotification(null)
        }
    }

    private fun resumeTracking() {
        val activeId = repository.getActiveAlarmId() ?: return
        repository.updatePausedState(activeId, false)
        // Force an update to refresh notification buttons immediately
        updateNotification(null)

        // Then try to get location
        val alarms = repository.getAlarms()
        val alarm = alarms.find { it.id == activeId }
        if (alarm != null) {
            updateProgress(alarm)
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun updateProgress(alarm: LocationAlarm) {
        if (alarm.isPaused) {
            updateNotification(null)
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    val distance = calculateDistance(
                        it.latitude,
                        it.longitude,
                        alarm.latitude,
                        alarm.longitude
                    )
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

        val fullScreenIntent = Intent(
            this,
            com.sameerasw.essentials.ui.activities.LocationAlarmActivity::class.java
        ).apply {
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

    private fun updateNotification(distanceKm: Float?) {
        val startDist = repository.getStartDistance()
        val startTime = repository.getStartTime()
        val progressPercent = if (startDist > 0 && distanceKm != null) {
            ((1.0f - (distanceKm * 1000f / startDist)) * 100).toInt().coerceIn(0, 100)
        } else 0

        var etaText: String? = null
        if (startDist > 0 && startTime > 0 && distanceKm != null) {
            val elapsed = System.currentTimeMillis() - startTime
            val currentDistMeters = distanceKm * 1000f
            val distanceTravelled = startDist - currentDistMeters
            if (distanceTravelled > 0 && elapsed > 0) {
                val remainingMillis = (currentDistMeters * elapsed / distanceTravelled).toLong()
                val remainingMinutes = (remainingMillis / 60000).toInt().coerceAtLeast(1)

                etaText = if (remainingMinutes >= 60) {
                    val hrs = remainingMinutes / 60
                    val mins = remainingMinutes % 60
                    getString(R.string.location_reached_eta_hr_min, hrs, mins)
                } else {
                    getString(R.string.location_reached_eta_min, remainingMinutes)
                }
            }
        }

        val notification = buildOngoingNotification(distanceKm, progressPercent, etaText)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildInitialNotification(): Notification {
        return buildOngoingNotification(null, 0, null)
    }

    private fun buildOngoingNotification(
        distanceKm: Float?,
        progress: Int,
        etaText: String?
    ): Notification {
        val stopIntent = Intent(this, LocationReachedService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("feature", "Location reached")
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distanceText = distanceKm?.let {
            if (it < 1.0) getString(R.string.location_reached_dist_m, (it * 1000).toInt())
            else getString(R.string.location_reached_dist_km, it)
        } ?: getString(R.string.location_reached_calculating)

        val contentText = if (etaText != null) {
            getString(
                R.string.location_reached_service_remaining_with_eta,
                distanceText,
                progress,
                etaText
            )
        } else {
            getString(R.string.location_reached_service_remaining, distanceText, progress)
        }

        val pauseIntent = Intent(this, LocationReachedService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = Intent(this, LocationReachedService::class.java).apply {
            action = ACTION_RESUME
        }
        val resumePendingIntent = PendingIntent.getService(
            this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activeId = repository.getActiveAlarmId()
        val alarm = repository.getAlarms().find { it.id == activeId }
        val isPaused = alarm?.isPaused == true
        val iconResName = alarm?.iconResName ?: "round_navigation_24"
        val iconResId = resources.getIdentifier(iconResName, "drawable", packageName)
        val finalIconId = if (iconResId != 0) iconResId else R.drawable.round_navigation_24

        if (Build.VERSION.SDK_INT >= 35) {
            val destinationName = alarm?.name?.ifEmpty { "Destination" } ?: "Destination"
            val builder = Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(finalIconId)
                .setContentTitle(
                    getString(
                        R.string.location_reached_service_title,
                        destinationName
                    )
                )
                .setContentText(if (isPaused) getString(R.string.location_reached_pause) else contentText)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(mainPendingIntent)
                .addAction(
                    if (isPaused) {
                        Notification.Action.Builder(
                            Icon.createWithResource(this, R.drawable.round_play_arrow_24),
                            getString(R.string.location_reached_resume), resumePendingIntent
                        ).build()
                    } else {
                        Notification.Action.Builder(
                            Icon.createWithResource(this, R.drawable.rounded_pause_24),
                            getString(R.string.location_reached_pause), pausePendingIntent
                        ).build()
                    }
                )
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.rounded_power_settings_new_24),
                        getString(R.string.location_reached_stop_tracking), stopPendingIntent
                    ).build()
                )

            if (Build.VERSION.SDK_INT >= 36) {
                try {
                    val progressStyle = Notification.ProgressStyle()
                        .setStyledByProgress(true)
                        .setProgress(progress)
                        .setProgressTrackerIcon(
                            Icon.createWithResource(
                                this,
                                if (isPaused) R.drawable.rounded_pause_24 else R.drawable.round_play_arrow_24
                            ).setTint(getColor(android.R.color.system_accent1_300))
                        )
                    builder.style = progressStyle
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
                if (isPaused) {
                    extras.putString("android.shortCriticalText", "Paused")
                } else {
                    distanceKm?.let { extras.putString("android.shortCriticalText", distanceText) }
                }
                builder.addExtras(extras)

                builder.javaClass.getMethod(
                    "setRequestPromotedOngoing",
                    Boolean::class.javaPrimitiveType
                )
                    .invoke(builder, true)

                if (isPaused) {
                    builder.javaClass.getMethod("setShortCriticalText", CharSequence::class.java)
                        .invoke(builder, "Paused")
                } else {
                    distanceKm?.let {
                        builder.javaClass.getMethod(
                            "setShortCriticalText",
                            CharSequence::class.java
                        )
                            .invoke(builder, distanceText)
                    }
                }
            } catch (_: Throwable) {
            }

            return builder.build()
        }

        val destinationName = alarm?.name?.ifEmpty { "Destination" } ?: "Destination"
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(finalIconId)
            .setContentTitle(getString(R.string.location_reached_service_title, destinationName))
            .setContentText(if (isPaused) getString(R.string.location_reached_pause) else contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(mainPendingIntent)
            .setProgress(100, progress, false)
            .addAction(
                if (isPaused) R.drawable.round_play_arrow_24 else R.drawable.rounded_pause_24,
                if (isPaused) getString(R.string.location_reached_resume) else getString(R.string.location_reached_pause),
                if (isPaused) resumePendingIntent else pausePendingIntent
            )
            .addAction(
                R.drawable.rounded_power_settings_new_24,
                getString(R.string.location_reached_stop_tracking),
                stopPendingIntent
            )

        val extras = android.os.Bundle()
        extras.putBoolean("android.requestPromotedOngoing", true)
        if (isPaused) {
            extras.putString("android.shortCriticalText", "Paused")
        } else {
            distanceKm?.let { extras.putString("android.shortCriticalText", distanceText) }
        }
        builder.addExtras(extras)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.location_reached_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.location_reached_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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
