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
    @Volatile private var monitorJob: Job? = null
    @Volatile private var lastPackageName: String? = null
    private var lastQueryTime = System.currentTimeMillis() - 5000

    @Volatile private var pendingRestoreJob: Job? = null
    private var pendingRestorePackage: String? = null
    @Volatile private var freezeCountdownJob: Job? = null

    // Active config for the currently monitored target app (used for periodic re-enforcement)
    private var activeTargetConfig: com.sameerasw.essentials.domain.model.ShutUpAppConfig? = null
    private var enforceTickCount = 0

    // Cached system service and reusable event object (only accessed from monitorJob / Default dispatcher)
    private val usageStatsManager by lazy { getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager }
    private val reusableEvent = UsageEvents.Event()

    companion object {
        private const val TAG = "ShutUpForegroundService"
        private const val CHANNEL_ID = "shutup_service_channel"
        private const val NOTIFICATION_ID = 1002
        private const val NOTIFICATION_FREEZE_ID = 1003

        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val ACTION_FREEZE_NOW = "ACTION_FREEZE_NOW"
        const val ACTION_ABORT_FREEZE = "ACTION_ABORT_FREEZE"
        const val EXTRA_PACKAGE_NAME = "package_name"

        @Volatile var isRunning = false
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

        // Recover backup before monitoring starts. Avoid startup restore/apply race.
        serviceScope.launch {
            val backup = settingsRepository.getShutUpOriginalSettings()
            if (backup.isNotEmpty()) {
                Log.d(TAG, "Found pending ShutUp backup on startup — checking if restore is needed")
                val now = System.currentTimeMillis()
                val foregroundPackage = getForegroundPackage(now - 5000, now)
                val foregroundShutUp = settingsRepository.loadShutUpConfigs().any {
                    it.isEnabled && it.packageName == foregroundPackage
                }
                if (!foregroundShutUp) {
                    Log.d(TAG, "No shut-up app running — restoring backup on startup")
                    ShutUpManager.restoreOriginalSettings(this@ShutUpForegroundService, settingsRepository)
                }
            }
            startMonitoring()
        }
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
                        val previousPkg = lastPackageName
                        lastPackageName = currentPkg
                        onPackageChanged(previousPkg, currentPkg)
                        enforceTickCount = 0
                    } else {
                        // Re-enforce settings every ~2s while target app stays in foreground
                        // This ensures settings stay hidden even if something re-enables them between opens
                        enforceTickCount++
                        if (enforceTickCount % 5 == 0) {
                            activeTargetConfig?.let { config ->
                                Log.d(TAG, "Re-enforcing ShutUp settings for ${config.packageName}")
                                ShutUpManager.applyShutUpSettings(
                                    this@ShutUpForegroundService,
                                    config,
                                    settingsRepository,
                                    reinforcement = true
                                )
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
        try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            var lastResumedPackage: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(reusableEvent)
                if (reusableEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastResumedPackage = reusableEvent.packageName
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

        val newConfig = configs.find { it.packageName == newPkg && it.isEnabled }

        // 1. Leaving a Shut-Up app. Keep the session snapshot when moving directly to another
        // Shut-Up app; restoring between targets would briefly re-enable protected settings.
        if (oldPkg != null && configs.any { it.packageName == oldPkg && it.isEnabled }) {
            pendingRestoreJob?.cancel()
            if (newConfig == null) {
                activeTargetConfig = null
                pendingRestorePackage = oldPkg
                pendingRestoreJob = serviceScope.launch {
                    delay(settingsRepository.getShutUpRestoreDelay().coerceAtLeast(0) * 1000L)
                    val config = settingsRepository.loadShutUpConfigs().find { it.packageName == oldPkg }
                    if (config != null && config.isEnabled && lastPackageName != oldPkg && activeTargetConfig == null) {
                        ShutUpManager.restoreOriginalSettings(this@ShutUpForegroundService, settingsRepository)
                        if (config.attemptShizukuRestart) {
                            ShutUpManager.restartShizuku(this@ShutUpForegroundService)
                        }
                        if (config.autoArchive) {
                            showAutoFreezeNotification(config.packageName)
                        }
                    } else {
                        Log.d(TAG, "Skipping restore for $oldPkg — another target is active or package returned")
                    }
                    pendingRestorePackage = null
                    pendingRestoreJob = null
                }
            } else {
                pendingRestorePackage = null
            }
        }

        // 2. Entering a Shut-Up app
        if (newConfig != null) {
            activeTargetConfig = newConfig
            freezeCountdownJob?.cancel()
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_FREEZE_ID)

            // Apply inline — no extra coroutine spawn, runs directly in monitoring coroutine
            ShutUpManager.applyShutUpSettings(this@ShutUpForegroundService, newConfig, settingsRepository)
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

        // Build PendingIntents once — they do not change between countdown ticks
        val freezePendingIntent = PendingIntent.getService(
            this@ShutUpForegroundService,
            101,
            Intent(this@ShutUpForegroundService, ShutUpForegroundService::class.java).apply {
                action = ACTION_FREEZE_NOW
                putExtra(EXTRA_PACKAGE_NAME, packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val abortPendingIntent = PendingIntent.getService(
            this@ShutUpForegroundService,
            102,
            Intent(this@ShutUpForegroundService, ShutUpForegroundService::class.java).apply {
                action = ACTION_ABORT_FREEZE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        freezeCountdownJob = serviceScope.launch {
            var secondsRemaining = 5
            while (secondsRemaining > 0) {
                val notification = NotificationCompat.Builder(this@ShutUpForegroundService, CHANNEL_ID)
                    .setContentTitle(getString(R.string.shut_up_auto_archive_notif_title))
                    .setContentText(getString(R.string.shut_up_auto_archive_notif_text, appName, secondsRemaining))
                    .setSmallIcon(R.drawable.rounded_snowflake_24)
                    .setOngoing(true)
                    .addAction(R.drawable.rounded_snowflake_24, getString(R.string.shut_up_auto_archive_action_freeze), freezePendingIntent)
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

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_FREEZE_ID)

        // Best-effort synchronous restore if we were mid-restore cycle when the service was stopped.
        // Only restore if no shut-up app is actively in the foreground (activeTargetConfig == null means
        // we left the target app and were waiting for it to close before reverting).
        val pkg = pendingRestorePackage
        if (pkg != null && activeTargetConfig == null) {
            try {
                val configs = settingsRepository.loadShutUpConfigs()
                val config = configs.find { it.packageName == pkg && it.isEnabled }
                if (config != null && !ShutUpManager.isAppRunning(this, pkg)) {
                    runBlocking(Dispatchers.IO) {
                        ShutUpManager.restoreOriginalSettings(this@ShutUpForegroundService, settingsRepository)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed sync restore on destroy", e)
            }
        }

        serviceScope.cancel()
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
