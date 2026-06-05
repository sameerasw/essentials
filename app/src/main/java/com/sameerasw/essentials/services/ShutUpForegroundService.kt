package com.sameerasw.essentials.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.ShutUpAppConfig
import com.sameerasw.essentials.utils.FreezeManager
import com.sameerasw.essentials.utils.ShutUpManager
import kotlinx.coroutines.*

class ShutUpForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var settingsRepository: SettingsRepository
    private var monitorJob: Job? = null
    private var lastPackageName: String? = null
    private var lastQueryTime = System.currentTimeMillis() - 5000

    private var pendingRestoreJob: Job? = null
    private var pendingRestorePackage: String? = null
    private var freezeCountdownJob: Job? = null

    // Active config for the currently monitored target app (used for periodic re-enforcement)
    private var activeTargetConfig: com.sameerasw.essentials.domain.model.ShutUpAppConfig? = null
    private var enforceTickCount = 0

    companion object {
        private const val TAG = "ShutUpForegroundService"
        private const val CHANNEL_ID = "shutup_service_channel"
        private const val NOTIFICATION_ID = 1002
        private const val NOTIFICATION_FREEZE_ID = 1003

        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_FREEZE_NOW = "ACTION_FREEZE_NOW"
        const val ACTION_ABORT_FREEZE = "ACTION_ABORT_FREEZE"
        const val EXTRA_PACKAGE_NAME = "package_name"

        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        settingsRepository = SettingsRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopForeground(true)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_FREEZE_NOW -> {
                val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                if (pkg != null) {
                    freezeCountdownJob?.cancel()
                    val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(NOTIFICATION_FREEZE_ID)
                    serviceScope.launch {
                        FreezeManager.freezeApp(this@ShutUpForegroundService, pkg)
                    }
                }
                return START_STICKY
            }
            ACTION_ABORT_FREEZE -> {
                freezeCountdownJob?.cancel()
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_FREEZE_ID)
                return START_STICKY
            }
        }

        startForeground(
            NOTIFICATION_ID,
            createServiceNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )

        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        if (monitorJob != null) return
        monitorJob = serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val currentPkg = getForegroundPackage(lastQueryTime, now)
                if (currentPkg != null) {
                    lastQueryTime = now
                    if (currentPkg != lastPackageName) {
                        onPackageChanged(lastPackageName, currentPkg)
                        lastPackageName = currentPkg
                        enforceTickCount = 0
                    } else {
                        // Re-enforce settings every ~2s while target app stays in foreground
                        // This ensures settings stay hidden even if something re-enables them between opens
                        enforceTickCount++
                        if (enforceTickCount % 5 == 0) {
                            activeTargetConfig?.let { config ->
                                Log.d(TAG, "Re-enforcing ShutUp settings for ${config.packageName}")
                                ShutUpManager.applyShutUpSettings(this@ShutUpForegroundService, config)
                            }
                        }
                    }
                } else {
                    lastQueryTime = now - 500
                }
                delay(400)
            }
        }
    }

    private fun getForegroundPackage(startTime: Long, endTime: Long): String? {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            var lastResumedPackage: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastResumedPackage = event.packageName
                }
            }
            if (lastResumedPackage != null) {
                return lastResumedPackage
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query usage events", e)
        }
        return null
    }

    private suspend fun onPackageChanged(oldPkg: String?, newPkg: String?) {
        if (newPkg == null || ShutUpManager.isPackageIgnored(newPkg)) return

        val configs = settingsRepository.loadShutUpConfigs()

        // 1. Leaving a Shut-Up app
        if (oldPkg != null && configs.any { it.packageName == oldPkg && it.isEnabled }) {
            if (newPkg != oldPkg) {
                activeTargetConfig = null
                pendingRestoreJob?.cancel()
                pendingRestorePackage = oldPkg
                pendingRestoreJob = serviceScope.launch {
                    var shouldContinue = true
                    while (shouldContinue) {
                        delay(3000) // 3 seconds debounce / check interval
                        val config = settingsRepository.loadShutUpConfigs().find { it.packageName == oldPkg }
                        if (config != null && config.isEnabled) {
                            if (!ShutUpManager.isAppRunning(this@ShutUpForegroundService, oldPkg)) {
                                if (activeTargetConfig != null) {
                                    // A different Shut-Up app is now active; skip restoration to avoid
                                    // re-enabling settings (USB debugging, dev options, etc.) while it is running
                                    Log.d(TAG, "Skipping restore for $oldPkg — another Shut-Up app is active (${activeTargetConfig?.packageName})")
                                    shouldContinue = false
                                } else {
                                    ShutUpManager.revertShutUpSettings(this@ShutUpForegroundService, config)
                                    ShutUpManager.restoreOriginalSettings(this@ShutUpForegroundService, settingsRepository)
                                    if (config.attemptShizukuRestart) {
                                        ShutUpManager.restartShizuku(this@ShutUpForegroundService)
                                    }
                                    if (config.autoArchive) {
                                        showAutoFreezeNotification(config.packageName)
                                    }
                                    shouldContinue = false
                                }
                            } else {
                                Log.d(TAG, "$oldPkg is still running in the background, delaying revert settings...")
                            }
                        } else {
                            shouldContinue = false
                        }
                    }
                    pendingRestorePackage = null
                }
            }
        }

        // 2. Entering a Shut-Up app
        val newConfig = configs.find { it.packageName == newPkg }
        if (newConfig != null && newConfig.isEnabled) {
            activeTargetConfig = newConfig
            if (pendingRestorePackage == newPkg) {
                pendingRestoreJob?.cancel()
                pendingRestorePackage = null
            }
            freezeCountdownJob?.cancel()
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_FREEZE_ID)

            // Apply inline — no extra coroutine spawn, runs directly in monitoring coroutine
            ShutUpManager.applyShutUpSettings(this@ShutUpForegroundService, newConfig)
        } else {
            activeTargetConfig = null
        }
    }

    private fun showAutoFreezeNotification(packageName: String) {
        freezeCountdownJob?.cancel()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        freezeCountdownJob = serviceScope.launch {
            var secondsRemaining = 5
            while (secondsRemaining > 0) {
                val stopIntent = Intent(this@ShutUpForegroundService, ShutUpForegroundService::class.java).apply {
                    action = ACTION_FREEZE_NOW
                    putExtra(EXTRA_PACKAGE_NAME, packageName)
                }
                val stopPendingIntent = PendingIntent.getService(
                    this@ShutUpForegroundService,
                    101,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val abortIntent = Intent(this@ShutUpForegroundService, ShutUpForegroundService::class.java).apply {
                    action = ACTION_ABORT_FREEZE
                }
                val abortPendingIntent = PendingIntent.getService(
                    this@ShutUpForegroundService,
                    102,
                    abortIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(this@ShutUpForegroundService, CHANNEL_ID)
                    .setContentTitle(getString(R.string.shut_up_auto_archive_notif_title))
                    .setContentText(getString(R.string.shut_up_auto_archive_notif_text, appName, secondsRemaining))
                    .setSmallIcon(R.drawable.rounded_snowflake_24)
                    .setOngoing(true)
                    .addAction(R.drawable.rounded_snowflake_24, getString(R.string.shut_up_auto_archive_action_freeze), stopPendingIntent)
                    .addAction(R.drawable.rounded_close_24, getString(R.string.shut_up_auto_archive_action_abort), abortPendingIntent)
                    .build()

                notificationManager.notify(NOTIFICATION_FREEZE_ID, notification)
                delay(1000)
                secondsRemaining--
            }

            FreezeManager.freezeApp(this@ShutUpForegroundService, packageName)
            notificationManager.cancel(NOTIFICATION_FREEZE_ID)
        }
    }

    override fun onDestroy() {
        isRunning = false
        monitorJob?.cancel()
        pendingRestoreJob?.cancel()
        freezeCountdownJob?.cancel()
        serviceScope.cancel()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_FREEZE_ID)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.shut_up_service_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.shut_up_service_desc)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createServiceNotification(): Notification {
        val stopIntent = Intent(this, ShutUpForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            201,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.shut_up_service_notification_title))
            .setContentText(getString(R.string.shut_up_service_notification_desc))
            .setSmallIcon(R.drawable.rounded_shield_lock_24)
            .setOngoing(true)
            .addAction(R.drawable.rounded_close_24, getString(R.string.action_stop), stopPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
