/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: InputEventListenerService.kt
 * Description: Background service component for InputEventListenerService.kt.
 */

package com.sameerasw.essentials.services

import com.sameerasw.essentials.R
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.Display
import androidx.core.app.NotificationCompat
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.RemapSlot
import com.sameerasw.essentials.input.InputDeviceScanner
import com.sameerasw.essentials.input.PowerPressDetector
import com.sameerasw.essentials.input.VolumeLongPressDetector
import com.sameerasw.essentials.input.VolumePressEvent
import com.sameerasw.essentials.shizuku.ShizukuPermissionHelper
import com.sameerasw.essentials.utils.PowerButtonGestures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class InputEventListenerService : Service() {
    companion object {
        const val ACTION_VOLUME_LONG_PRESSED = "com.sameerasw.essentials.VOLUME_LONG_PRESSED"
        const val EXTRA_DIRECTION = "direction"
        const val EXTRA_DURATION_MS = "duration_ms"
        const val ACTION_STOP = "com.sameerasw.essentials.STOP_INPUT_LISTENER"
        const val ACTION_POWER_GESTURE = "com.sameerasw.essentials.POWER_GESTURE"
        const val EXTRA_POWER_GESTURE = "power_gesture"
        const val EXTRA_SCREEN_ON_BEFORE = "screen_on_before"
        const val EXTRA_FIRST_PRESS_AT = "first_press_at"
        private const val NOTIFICATION_ID = 4242
        const val PREF_LISTENER_ACTIVE = "shizuku_listener_active"
        private const val WATCHDOG_RETRY_MS = 2_000L
        private const val WATCHDOG_IDLE_MS = 10_000L
        private const val WATCHDOG_MAX_RETRY_MS = 60_000L
    }

    private fun startForegroundSafely(notification: android.app.Notification): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: Exception) {
            Log.e("InputEventListener", "Failed to start foreground", e)
            false
        }
    }

    private val settingsRepo by lazy { SettingsRepository(this) }

    private fun wantsVolume(): Boolean {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        return prefs.getBoolean("button_remap_enabled", false) &&
            prefs.getBoolean("button_remap_use_shizuku", false)
    }

    private fun wantsPower(): Boolean =
        getSharedPreferences("essentials_prefs", MODE_PRIVATE).getBoolean("button_remap_enabled", false) &&
            PowerButtonGestures.isListenerNeeded(settingsRepo)

    private fun isFeatureEnabled(): Boolean = wantsVolume() || wantsPower()

    private fun isVolumeListening(): Boolean = detector?.isListening == true

    private fun isPowerListening(): Boolean = synchronized(powerDetectors) { powerDetectors.any { it.isListening } }

    private fun isFullyListening(): Boolean =
        (!wantsVolume() || isVolumeListening()) && (!wantsPower() || isPowerListening())

    private fun updatePowerOverride() {
        if (wantsPower() && isPowerListening()) {
            PowerButtonGestures.onListenerAvailable(this, settingsRepo)
        } else {
            PowerButtonGestures.onListenerUnavailable(this, settingsRepo)
        }
    }

    private var scope: CoroutineScope? = null
    private var detector: VolumeLongPressDetector? = null
    private var collectJob: Job? = null
    private val startMutex = Mutex()
    private val powerDetectors = mutableListOf<PowerPressDetector>()
    private val powerCollectJobs = mutableListOf<Job>()

    @Volatile private var screenOn = true
    private lateinit var shizukuHelper: ShizukuPermissionHelper
    private var isTorchOn = false

    private val torchCallback =
        object : android.hardware.camera2.CameraManager.TorchCallback() {
            override fun onTorchModeChanged(
                cameraId: String,
                enabled: Boolean,
            ) {
                super.onTorchModeChanged(cameraId, enabled)
                isTorchOn = enabled
            }
        }

    private val screenReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> screenOn = true
                    Intent.ACTION_SCREEN_OFF -> screenOn = false
                }
            }
        }

    private val binderReceivedListener =
        Shizuku.OnBinderReceivedListener {
            Log.d("InputEventListener", "Shizuku binder received, attempting to start listening")
            ensureListening()
        }

    private val binderDeadListener =
        Shizuku.OnBinderDeadListener {
            Log.w("InputEventListener", "Shizuku binder died, stopping listener")
            stopListening()
            PowerButtonGestures.onListenerUnavailable(this, settingsRepo)
        }

    override fun onCreate() {
        super.onCreate()
        shizukuHelper = ShizukuPermissionHelper(this)
        setListenerActive(false)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel =
                android.app.NotificationChannel(
                    "service_channel",
                    getString(R.string.input_listener_channel_name),
                    android.app.NotificationManager.IMPORTANCE_MIN,
                )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat
                .Builder(this, "service_channel")
                .setContentTitle(getString(R.string.input_listener_running_title))
                .setContentText(getString(R.string.input_listener_running_text))
                .setSmallIcon(android.R.drawable.star_on)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
        if (!startForegroundSafely(notification)) {
            stopSelf()
            return
        }

        if (!com.sameerasw.essentials.utils.DeviceUtils.isTorchAccessRestricted()) {
            try {
                (getSystemService(CAMERA_SERVICE) as? android.hardware.camera2.CameraManager)
                    ?.registerTorchCallback(torchCallback, null)
            } catch (e: Exception) {
                Log.w("InputEventListener", "Torch callback unavailable", e)
            }
        }

        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        screenOn = powerManager?.isInteractive ?: true

        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)

        scope?.launch {
            delay(500)
            var retryDelay = WATCHDOG_RETRY_MS
            while (isActive) {
                if (!isFeatureEnabled()) {
                    stopListening()
                    PowerButtonGestures.onListenerUnavailable(this@InputEventListenerService, settingsRepo)
                    stopSelf()
                    return@launch
                }
                if (!isFullyListening()) startListeningNow()
                val listening = isFullyListening()
                updatePowerOverride()
                if (listening) {
                    retryDelay = WATCHDOG_RETRY_MS
                    delay(WATCHDOG_IDLE_MS)
                } else {
                    setListenerActive(false)
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(WATCHDOG_MAX_RETRY_MS)
                }
            }
        }
    }

    private fun startListening() {
        scope?.launch { startListeningNow() }
    }

    private suspend fun startListeningNow() {
        startMutex.withLock {
            if (isFullyListening()) return
            stopListening()

            val permitted =
                withContext(Dispatchers.IO) {
                    try {
                        com.sameerasw.essentials.utils.ShellUtils
                            .hasPermission(this@InputEventListenerService)
                    } catch (_: Exception) {
                        false
                    }
                }
            if (!permitted) {
                Log.e("InputEventListener", "Shell permission not granted")
                return
            }

            if (wantsVolume()) startVolumeListening()
            if (wantsPower()) startPowerListening()
        }
    }

    private suspend fun startVolumeListening() {
        run {
            val devices =
                withContext(Dispatchers.IO) {
                    try {
                        InputDeviceScanner().scanForVolumeDevices()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

            if (devices.isEmpty()) {
                Log.e("InputEventListener", "No devices found")
                getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                    .edit()
                    .remove("shizuku_detected_device_path")
                    .apply()
                return
            }

            val devicePath = devices.first().path
            Log.d("InputEventListener", "Listening on device: $devicePath")

            getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                .edit()
                .putString("shizuku_detected_device_path", devicePath)
                .apply()

            val activeDetector = VolumeLongPressDetector(devicePath, 500)
            detector = activeDetector

            collectJob = scope?.launch {
                activeDetector.events.collect { event ->
                    val powerManager =
                        getSystemService(POWER_SERVICE) as android.os.PowerManager
                    val isScreenInteractive =
                        try {
                            powerManager.isInteractive
                        } catch (e: Exception) {
                            false
                        }
                    val isAod = isAodShowing()

                    if (isScreenInteractive || isAod) {
                        return@collect
                    }

                    if (event is VolumePressEvent.LongPress) {
                        Log.i("InputEventListener", "# LONG PRESS: ${event.direction}")
                        sendBroadcast(
                            Intent(ACTION_VOLUME_LONG_PRESSED).apply {
                                putExtra(EXTRA_DIRECTION, event.direction.name)
                                putExtra(EXTRA_DURATION_MS, event.durationMs)
                                setPackage(packageName)
                            },
                        )
                    } else if (event is VolumePressEvent.ShortPress) {
                        if (isTorchOn) {
                            val prefs =
                                getSharedPreferences(
                                    "essentials_prefs",
                                    MODE_PRIVATE,
                                )
                            val isAdjustEnabled =
                                prefs.getBoolean("flashlight_adjust_intensity_enabled", false)
                            val isGlobalEnabled =
                                prefs.getBoolean("flashlight_global_enabled", false)

                            if (isAdjustEnabled || isGlobalEnabled) {
                                val action =
                                    if (event.direction == com.sameerasw.essentials.input.VolumeDirection.UP) {
                                        "com.sameerasw.essentials.ACTION_FLASHLIGHT_INCREASE"
                                    } else {
                                        "com.sameerasw.essentials.ACTION_FLASHLIGHT_DECREASE"
                                    }
                                sendBroadcast(Intent(action).setPackage(packageName))
                            } else {
                                val pm =
                                    getSystemService(POWER_SERVICE) as android.os.PowerManager
                                val isScreenOn = pm.isInteractive
                                val slot =
                                    RemapSlot.forVolume(
                                        isUp = event.direction == com.sameerasw.essentials.input.VolumeDirection.UP,
                                        isScreenOn = isScreenOn,
                                    )
                                val isMapped =
                                    SettingsRepository(this@InputEventListenerService)
                                        .getRemapActions(slot.prefKey)
                                        .isNotEmpty()
                                if (isMapped) {
                                    val am =
                                        getSystemService(AUDIO_SERVICE) as android.media.AudioManager
                                    val direction =
                                        if (event.direction == com.sameerasw.essentials.input.VolumeDirection.UP) {
                                            android.media.AudioManager.ADJUST_RAISE
                                        } else {
                                            android.media.AudioManager.ADJUST_LOWER
                                        }
                                    am.adjustSuggestedStreamVolume(
                                        direction,
                                        android.media.AudioManager.USE_DEFAULT_STREAM_TYPE,
                                        android.media.AudioManager.FLAG_SHOW_UI,
                                    )
                                }
                            }
                        } else {
                            val pm =
                                getSystemService(POWER_SERVICE) as android.os.PowerManager
                            val isScreenOn = pm.isInteractive
                            val slot =
                                RemapSlot.forVolume(
                                    isUp = event.direction == com.sameerasw.essentials.input.VolumeDirection.UP,
                                    isScreenOn = isScreenOn,
                                )
                            val isMapped =
                                SettingsRepository(this@InputEventListenerService)
                                    .getRemapActions(slot.prefKey)
                                    .isNotEmpty()

                            if (isMapped) {
                                val am =
                                    getSystemService(AUDIO_SERVICE) as android.media.AudioManager
                                val dirKey =
                                    if (event.direction == com.sameerasw.essentials.input.VolumeDirection.UP) {
                                        android.media.AudioManager.ADJUST_RAISE
                                    } else {
                                        android.media.AudioManager.ADJUST_LOWER
                                    }
                                am.adjustSuggestedStreamVolume(
                                    dirKey,
                                    android.media.AudioManager.USE_DEFAULT_STREAM_TYPE,
                                    android.media.AudioManager.FLAG_SHOW_UI,
                                )
                            }
                        }
                    }
                }
            }

            scope?.let { setListenerActive(activeDetector.startListening(it)) }
        }
    }

    private suspend fun startPowerListening() {
        run {
            val powerPaths =
                withContext(Dispatchers.IO) {
                    try {
                        InputDeviceScanner().scanForPowerDevices(this@InputEventListenerService)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

            powerPaths.forEach { path ->
                val pDetector =
                    PowerPressDetector(
                        path,
                        { screenOn },
                    )
                val job =
                    scope?.launch {
                        pDetector.events.collect { event ->
                            sendBroadcast(
                                Intent(ACTION_POWER_GESTURE).apply {
                                    putExtra(EXTRA_POWER_GESTURE, event.gesture.name)
                                    putExtra(EXTRA_SCREEN_ON_BEFORE, event.screenOnBefore)
                                    putExtra(EXTRA_FIRST_PRESS_AT, event.firstPressAt)
                                    setPackage(packageName)
                                },
                            )
                        }
                    }
                synchronized(powerDetectors) {
                    powerDetectors.add(pDetector)
                    job?.let { powerCollectJobs.add(it) }
                }
                scope?.let { pDetector.startListening(it) }
            }
        }
    }

    private fun stopListening() {
        collectJob?.cancel()
        collectJob = null
        detector?.stopListening()
        detector = null
        val toStop =
            synchronized(powerDetectors) {
                powerCollectJobs.forEach { it.cancel() }
                powerCollectJobs.clear()
                val list = powerDetectors.toList()
                powerDetectors.clear()
                list
            }
        toStop.forEach { it.stopListening() }
        setListenerActive(false)
    }

    private fun setListenerActive(active: Boolean) {
        getSharedPreferences("essentials_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_LISTENER_ACTIVE, active)
            .apply()
    }

    private fun ensureListening() {
        if (!isFullyListening()) startListening()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // Reinforce foreground
        val notification =
            NotificationCompat
                .Builder(this, "service_channel")
                .setContentTitle(getString(R.string.input_listener_running_title))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build()
        if (!startForegroundSafely(notification) || !isFeatureEnabled()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        ensureListening()
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            (getSystemService(CAMERA_SERVICE) as? android.hardware.camera2.CameraManager)
                ?.unregisterTorchCallback(torchCallback)
        } catch (_: Exception) {
        }
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        stopListening()
        PowerButtonGestures.onListenerUnavailable(this, settingsRepo)
        scope?.cancel()
        super.onDestroy()
    }

    private fun isAodShowing(): Boolean =
        try {
            val displayManager = getSystemService(DISPLAY_SERVICE) as? DisplayManager
            val defaultDisplay = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
            defaultDisplay?.state == Display.STATE_DOZE || defaultDisplay?.state == Display.STATE_DOZE_SUSPEND
        } catch (_: Exception) {
            false
        }

    override fun onBind(intent: Intent?): IBinder? = null
}
