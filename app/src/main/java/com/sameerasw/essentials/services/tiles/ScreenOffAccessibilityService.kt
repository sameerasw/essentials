/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: ScreenOffAccessibilityService.kt
 * Description: Background service component for ScreenOffAccessibilityService.kt.
 */

package com.sameerasw.essentials.services.tiles

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.services.InputEventListenerService
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.services.handlers.AmbientGlanceHandler
import com.sameerasw.essentials.services.handlers.AodForceTurnOffHandler
import com.sameerasw.essentials.services.handlers.AppFlowHandler
import com.sameerasw.essentials.services.handlers.ButtonRemapHandler
import com.sameerasw.essentials.services.handlers.FlashlightHandler
import com.sameerasw.essentials.services.handlers.NotificationLightingHandler
import com.sameerasw.essentials.services.handlers.OmniGestureOverlayHandler
import com.sameerasw.essentials.services.handlers.PocketModeHandler
import com.sameerasw.essentials.services.handlers.StatusBarIconHandler
import com.sameerasw.essentials.services.receivers.FlashlightActionReceiver
import com.sameerasw.essentials.utils.FreezeManager
import com.sameerasw.essentials.utils.performHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class ScreenOffAccessibilityService : AccessibilityService(), SensorEventListener {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val sensorManager by lazy { getSystemService(SENSOR_SERVICE) as SensorManager }
    private var proximitySensor: Sensor? = null

    // Handlers
    lateinit var flashlightHandler: FlashlightHandler
    private lateinit var notificationLightingHandler: NotificationLightingHandler
    private lateinit var buttonRemapHandler: ButtonRemapHandler
    private lateinit var appFlowHandler: AppFlowHandler
    private lateinit var ambientGlanceHandler: AmbientGlanceHandler
    private lateinit var aodForceTurnOffHandler: AodForceTurnOffHandler
    private lateinit var omniGestureOverlayHandler: OmniGestureOverlayHandler
    private lateinit var statusBarIconHandler: StatusBarIconHandler
    private lateinit var pocketModeHandler: PocketModeHandler
    private lateinit var smartPixelsHandler: com.sameerasw.essentials.services.handlers.SmartPixelsHandler

    private var lightSensor: Sensor? = null
    private var lightSensorLux: Float = 100f
    @Volatile private var pocketModeExcludedAppsSet: Set<String> = emptySet()
    private val appCategoryCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    private val keyguardManager by lazy { getSystemService(KEYGUARD_SERVICE) as KeyguardManager }

    private var isScreenOn = true
    private var isKeyguardLocked = false
    private var isLightSensorRegistered = false
    private var isProximityRegisteredForPocket = false
    private var isProximityRegistered = false

    private val prefs by lazy { getSharedPreferences("essentials_prefs", MODE_PRIVATE) }
    private val notificationListenerComponent by lazy {
        android.content.ComponentName(this, NotificationListener::class.java)
    }

    private var pocketModeEnabled = false
    private var pocketModeUseLightSensor = false
    private var pocketModeTriggerDelayMs = 3000L
    private var pocketModeLockScreenOnly = false
    private var flashlightPocketTurnOffEnabled = false

    @Volatile private var cachedBypassedPackage: String? = null
    @Volatile private var cachedBypassedKeyguardLocked: Boolean? = null
    @Volatile private var cachedBypassedResult: Boolean = false
    @Volatile private var isMediaCurrentlyPlaying: Boolean = false

    private fun invalidateBypassCache() {
        cachedBypassedPackage = null
        cachedBypassedKeyguardLocked = null
        // Refresh media state on main thread so sensor thread doesn't need binder IPC
        val pkg = appFlowHandler.currentPackage
        isMediaCurrentlyPlaying = if (pkg != null) hasActiveMediaSession(pkg) else false
    }

    private fun updatePocketModePrefs() {
        pocketModeEnabled = prefs.getBoolean("pocket_mode_enabled", false)
        pocketModeUseLightSensor = prefs.getBoolean("pocket_mode_use_light_sensor", false)
        pocketModeTriggerDelayMs = (prefs.getFloat("pocket_mode_trigger_delay", 3f) * 1000).toLong()
        pocketModeLockScreenOnly = prefs.getBoolean("pocket_mode_lock_screen_only", false)
        flashlightPocketTurnOffEnabled = prefs.getBoolean("flashlight_pocket_turn_off_enabled", false)
        invalidateBypassCache()
    }

    private fun updatePocketModeExcludedAppsSet() {
        val json = prefs.getString("pocket_mode_excluded_apps", null)
        pocketModeExcludedAppsSet = if (json != null) {
            try {
                val gson = com.google.gson.GsonBuilder().create()
                gson.fromJson(json, Array<AppSelection>::class.java)
                    .filter { it.isEnabled }
                    .map { it.packageName }
                    .toSet()
            } catch (e: Exception) {
                emptySet()
            }
        } else {
            emptySet()
        }
        invalidateBypassCache()
    }

    private fun isGameOrVideoApp(packageName: String): Boolean {
        return appCategoryCache.getOrPut(packageName) {
            try {
                val info = packageManager.getApplicationInfo(packageName, 0)
                val isLegacyGame =
                    (info.flags and android.content.pm.ApplicationInfo.FLAG_IS_GAME) != 0
                val isCategoryMatch =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val category = info.category
                        category == android.content.pm.ApplicationInfo.CATEGORY_GAME ||
                                category == android.content.pm.ApplicationInfo.CATEGORY_VIDEO
                    } else {
                        false
                    }
                isLegacyGame || isCategoryMatch
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun hasActiveMediaSession(packageName: String): Boolean {
        return try {
            val msm = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return false
            val sessions = msm.getActiveSessions(notificationListenerComponent)
            sessions?.any {
                it.packageName == packageName &&
                        it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
            } ?: false
        } catch (e: SecurityException) {
            android.util.Log.w("ScreenOffService", "SecurityException checking media sessions for $packageName: ${e.message}")
            false
        } catch (e: Exception) {
            false
        }
    }

    private var screenReceiver: BroadcastReceiver? = null


    // Freeze Logic
    private val freezeHandler = Handler(Looper.getMainLooper())
    private val freezeRunnable = Runnable {
        FreezeManager.freezeAll(this)
    }

    // Pocket Detection
    private val pocketFlashlightHandler = Handler(Looper.getMainLooper())
    private val pocketFlashlightRunnable = Runnable {
        // Re-check at fire time — guards against external torch-off between scheduling and firing
        if (flashlightPocketTurnOffEnabled && flashlightHandler.isProximityBlocked && flashlightHandler.isTorchOn) {
            flashlightHandler.toggleFlashlight()
        }
    }

    private fun schedulePocketFlashlightTurnOff() {
        pocketFlashlightHandler.removeCallbacks(pocketFlashlightRunnable)
        pocketFlashlightHandler.postDelayed(pocketFlashlightRunnable, 1500L)
    }

    private fun cancelPocketFlashlightTurnOff() {
        pocketFlashlightHandler.removeCallbacks(pocketFlashlightRunnable)
    }

    private fun updateProximitySensorRegistration() {
        val flashlightNeedsProximity = flashlightPocketTurnOffEnabled && flashlightHandler.isTorchOn

        val shouldRegister = isProximityRegisteredForPocket || flashlightNeedsProximity

        if (shouldRegister) {
            if (!isProximityRegistered) {
                if (proximitySensor == null) {
                    proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
                }
                proximitySensor?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                    isProximityRegistered = true
                    android.util.Log.d("ScreenOffService", "Registered proximity sensor listener")
                }
            }
        } else {
            if (isProximityRegistered) {
                proximitySensor?.let {
                    sensorManager.unregisterListener(this, it)
                }
                isProximityRegistered = false
                android.util.Log.d("ScreenOffService", "Unregistered proximity sensor listener")
            }
        }
    }

    fun updateFlashlightProximityRegistration() {
        updateProximitySensorRegistration()
    }

    private val preferenceChangeListener =
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "circle_to_search_gesture_enabled" ||
                key == "circle_to_search_gesture_height" ||
                key == "circle_to_search_gesture_width" ||
                key == "circle_to_search_preview_enabled"
            ) {
                updateOmniOverlay()
            } else if (key == "smart_wifi_enabled" || key == "smart_data_enabled" || key == "battery_percent_mode" || key?.startsWith(
                    "icon_"
                ) == true
            ) {
                statusBarIconHandler.updateAll()
            } else if (key == "pocket_mode_enabled" || key == "pocket_mode_use_light_sensor" || key == "pocket_mode_trigger_delay" || key == "pocket_mode_lock_screen_only" || key == "flashlight_pocket_turn_off_enabled") {
                updatePocketModePrefs()
                updatePocketModeSensors()
                com.sameerasw.essentials.utils.ServiceUtils.startRequiredServices(this)
            } else if (key == "pocket_mode_excluded_apps") {
                updatePocketModeExcludedAppsSet()
                com.sameerasw.essentials.utils.ServiceUtils.startRequiredServices(this)
            } else if (key == SettingsRepository.KEY_SMART_PIXELS_ENABLED || key == SettingsRepository.KEY_SMART_PIXELS_INTENSITY) {
                smartPixelsHandler.updateState()
            }
        }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Handlers
        flashlightHandler = FlashlightHandler(this, serviceScope)
        notificationLightingHandler = NotificationLightingHandler(this)
        buttonRemapHandler = ButtonRemapHandler(this, flashlightHandler)
        appFlowHandler = AppFlowHandler.getInstance(this)
        ambientGlanceHandler = AmbientGlanceHandler(this)
        aodForceTurnOffHandler = AodForceTurnOffHandler(this)
        omniGestureOverlayHandler = OmniGestureOverlayHandler(this)
        statusBarIconHandler = StatusBarIconHandler(this)
        pocketModeHandler = PocketModeHandler(this)
        smartPixelsHandler = com.sameerasw.essentials.services.handlers.SmartPixelsHandler(this)

        flashlightHandler.register()
        statusBarIconHandler.register()
        smartPixelsHandler.init()

        // Screen Receiver
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        isScreenOn = true
                        isKeyguardLocked = keyguardManager.isKeyguardLocked
                        invalidateBypassCache()
                        notificationLightingHandler.onScreenOn()
                        ambientGlanceHandler.dismissImmediately()
                        aodForceTurnOffHandler.removeOverlay()
                        freezeHandler.removeCallbacks(freezeRunnable)
                        stopInputEventListener()
                        updateOmniOverlay()
                        updatePocketModeSensors()
                    }

                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenOn = false
                        isKeyguardLocked = true
                        invalidateBypassCache()
                        appFlowHandler.clearAuthenticated()
                        scheduleFreeze()
                        startInputEventListenerIfEnabled()
                        ambientGlanceHandler.checkAndShowOnScreenOff()
                        omniGestureOverlayHandler.updateOverlay(false) // Always hide when screen is off
                        pocketModeHandler.onScreenOff()
                        updatePocketModeSensors()
                    }

                    Intent.ACTION_USER_PRESENT -> {
                        isKeyguardLocked = false
                        invalidateBypassCache()
                        val currentApp = appFlowHandler.currentPackage
                        if (pocketModeLockScreenOnly || isAppBypassedForPocketMode(currentApp)) {
                            pocketModeHandler.onScreenOff() // cancel pending timer + remove overlay + reset isBypassed
                        }
                    }

                    "com.sameerasw.essentials.MEDIA_PLAYBACK_CHANGED" -> {
                        invalidateBypassCache()
                    }

                    InputEventListenerService.ACTION_VOLUME_LONG_PRESSED -> {
                        buttonRemapHandler.handleExternalVolumeLongPress(intent)
                    }

                    "SHOW_AMBIENT_GLANCE",
                    "HIDE_AMBIENT_GLANCE_TEMPORARILY" -> {
                        ambientGlanceHandler.handleIntent(intent)
                    }

                    "FORCE_TURN_OFF_AOD" -> {
                        aodForceTurnOffHandler.forceTurnOff()
                    }

                    FlashlightActionReceiver.ACTION_TOGGLE,
                    FlashlightActionReceiver.ACTION_OFF,
                    FlashlightActionReceiver.ACTION_SET_INTENSITY,
                    FlashlightActionReceiver.ACTION_INCREASE,
                    FlashlightActionReceiver.ACTION_DECREASE -> {
                        flashlightHandler.handleIntent(intent)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction("com.sameerasw.essentials.MEDIA_PLAYBACK_CHANGED")
            addAction(InputEventListenerService.ACTION_VOLUME_LONG_PRESSED)
            addAction("SHOW_AMBIENT_GLANCE")
            addAction("HIDE_AMBIENT_GLANCE_TEMPORARILY")
            addAction("FORCE_TURN_OFF_AOD")
            addAction(FlashlightActionReceiver.ACTION_TOGGLE)
            addAction(FlashlightActionReceiver.ACTION_OFF)
            addAction(FlashlightActionReceiver.ACTION_SET_INTENSITY)
            addAction(FlashlightActionReceiver.ACTION_INCREASE)
            addAction(FlashlightActionReceiver.ACTION_DECREASE)
        }
        registerReceiver(screenReceiver, filter, RECEIVER_EXPORTED)


        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        val powerManager = getSystemService(POWER_SERVICE) as? android.os.PowerManager
        isScreenOn = powerManager?.isInteractive ?: true
        isKeyguardLocked = keyguardManager.isKeyguardLocked

        updatePocketModePrefs()
        updatePocketModeExcludedAppsSet()
        updatePocketModeSensors()
    }

    private fun scheduleFreeze() {
        val isFreezeWhenLockedEnabled = prefs.getBoolean("freeze_when_locked_enabled", false)

        if (isFreezeWhenLockedEnabled) {
            val delayIndex = prefs.getInt("freeze_lock_delay_index", 1)
            val delayMs = when (delayIndex) {
                0 -> 0L // Immediately
                1 -> 60_000L // 1 minute
                2 -> 300_000L // 5 minutes
                3 -> 900_000L // 15 minutes
                else -> -1L // Never
            }

            if (delayMs >= 0) {
                freezeHandler.removeCallbacks(freezeRunnable)
                freezeHandler.postDelayed(freezeRunnable, delayMs)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        updateOmniOverlay()
    }

    private fun updateOmniOverlay() {
        val isGestureEnabled = prefs.getBoolean("circle_to_search_gesture_enabled", false)
        val height = try {
            prefs.getFloat("circle_to_search_gesture_height", 48f)
        } catch (e: Exception) {
            48f
        }
        val width = try {
            prefs.getFloat("circle_to_search_gesture_width", 240f)
        } catch (e: Exception) {
            240f
        }
        val isPreview = prefs.getBoolean("circle_to_search_preview_enabled", false)
        omniGestureOverlayHandler.updateOverlay(isGestureEnabled, height, width, isPreview)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        flashlightHandler.unregister()
        notificationLightingHandler.removeOverlay()
        ambientGlanceHandler.removeOverlay()
        aodForceTurnOffHandler.removeOverlay()
        pocketModeHandler.removeOverlay()
        omniGestureOverlayHandler.removeOverlay()
        smartPixelsHandler.destroy()
        statusBarIconHandler.unregister()
        stopInputEventListener()
        cancelPocketFlashlightTurnOff()
        if (isProximityRegistered) {
            proximitySensor?.let {
                sensorManager.unregisterListener(this, it)
            }
            isProximityRegistered = false
        }
        if (isLightSensorRegistered) {
            lightSensor?.let {
                sensorManager.unregisterListener(this, it)
            }
            isLightSensorRegistered = false
        }

        serviceScope.cancel()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        try {
            appFlowHandler.destroy()
        } catch (_: Exception) {
        }
        instance = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            appFlowHandler.onPackageChanged(packageName)
        }
    }

    override fun onInterrupt() {}

    fun isAppBypassedForPocketMode(packageName: String?): Boolean {
        val isLocked = isKeyguardLocked
        if (packageName == cachedBypassedPackage && isLocked == cachedBypassedKeyguardLocked) {
            return cachedBypassedResult
        }

        // Never treat an app as excluded when the keyguard is locked — the lock screen
        // must always be protected regardless of which app was last in the foreground.
        // Note: isMediaCurrentlyPlaying is updated on the main thread via invalidateBypassCache(),
        // so we avoid a binder IPC (getActiveSessions) on the sensor thread here.
        val isExcluded = !isLocked && packageName != null && (
            pocketModeExcludedAppsSet.contains(packageName) ||
            isGameOrVideoApp(packageName) ||
            isMediaCurrentlyPlaying
        )
        val isKeyguardBypassed = pocketModeLockScreenOnly && !isLocked
        val result = isExcluded || isKeyguardBypassed

        cachedBypassedPackage = packageName
        cachedBypassedKeyguardLocked = isLocked
        cachedBypassedResult = result
        return result
    }

    fun dismissPocketMode() {
        pocketModeHandler.dismissForAppSwitch()
    }

    private fun updatePocketModeSensors() {
        val shouldRegisterLight = pocketModeEnabled && pocketModeUseLightSensor && isScreenOn
        val shouldRegisterProximity = pocketModeEnabled && isScreenOn

        if (shouldRegisterLight) {
            if (lightSensor == null) {
                lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            }
            if (lightSensor != null && !isLightSensorRegistered) {
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
                isLightSensorRegistered = true
                android.util.Log.d("ScreenOffService", "Registered light sensor for pocket mode")
            }
        } else {
            if (isLightSensorRegistered) {
                lightSensor?.let {
                    sensorManager.unregisterListener(this, it)
                }
                isLightSensorRegistered = false
                android.util.Log.d("ScreenOffService", "Unregistered light sensor for pocket mode")
            }
            lightSensorLux = 100f
        }

        isProximityRegisteredForPocket = shouldRegisterProximity

        updateProximitySensorRegistration()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_LIGHT) {
            lightSensorLux = event.values[0]
            if (pocketModeEnabled && !pocketModeHandler.isBypassed) {
                val currentApp = appFlowHandler.currentPackage
                val shouldBypass = isAppBypassedForPocketMode(currentApp)
                if (shouldBypass) {
                    pocketModeHandler.cancelPending()
                } else {
                    pocketModeHandler.onProximityChanged(
                        isBlocked = flashlightHandler.isProximityBlocked,
                        isLightDark = lightSensorLux <= 3f,
                        useLightSensor = pocketModeUseLightSensor,
                        triggerDelayMs = pocketModeTriggerDelayMs
                    )
                }
            }
        } else if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            val maxRange = event.sensor.maximumRange
            val isBlocked = distance < maxRange && distance < 5f

            flashlightHandler.isProximityBlocked = isBlocked

            if (flashlightPocketTurnOffEnabled && isBlocked && flashlightHandler.isTorchOn) {
                schedulePocketFlashlightTurnOff()
            } else {
                cancelPocketFlashlightTurnOff()
            }

            if (pocketModeEnabled && !pocketModeHandler.isBypassed) {
                val currentApp = appFlowHandler.currentPackage
                val shouldBypass = isAppBypassedForPocketMode(currentApp)
                if (shouldBypass) {
                    pocketModeHandler.cancelPending()
                } else {
                    pocketModeHandler.onProximityChanged(
                        isBlocked = isBlocked,
                        isLightDark = lightSensorLux <= 3f,
                        useLightSensor = pocketModeUseLightSensor,
                        triggerDelayMs = pocketModeTriggerDelayMs
                    )
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOmniOverlay() // Force refresh overlay on rotation
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val isVolumeKey =
            keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN

        if (isVolumeKey) {
            if (pocketModeHandler.isOverlayVisible) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    pocketModeHandler.isBypassed = true
                    pocketModeHandler.removeOverlay()
                }
                return true
            }
            // Bypass logic for Camera apps to resolve conflicts with shutter/zoom functions
            val foregroundPackage =
                rootInActiveWindow?.packageName?.toString() ?: appFlowHandler.currentPackage
            if (appFlowHandler.isCameraApp(foregroundPackage)) {
                return false
            }

            val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
            if (!powerManager.isInteractive && event.action == KeyEvent.ACTION_DOWN) {
                triggerAmbientGlanceVolume(keyCode)
            }
        }
        return buttonRemapHandler.onKeyEvent(event) || super.onKeyEvent(event)
    }

    private fun triggerAmbientGlanceVolume(keyCode: Int) {
        if (prefs.getBoolean(SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_ENABLED, false)) {
            // Skip if Android Auto is running
            if (com.sameerasw.essentials.utils.AppUtil.isAndroidAutoRunning(this)) {
                return
            }

            val mediaSessionManager =
                getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName =
                android.content.ComponentName(this, NotificationListener::class.java)
            val sessions = try {
                mediaSessionManager.getActiveSessions(componentName)
            } catch (e: Exception) {
                emptyList()
            }
            val isPlaying =
                sessions.any { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }
            if (!isPlaying) {
                return
            }

            val title = prefs.getString("current_media_title", null)
            val artist = prefs.getString("current_media_artist", null)

            val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
            val currentVolume =
                audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            val percentage = (currentVolume.toFloat() / maxVolume.toFloat() * 100).toInt()

            val isDockedMode =
                prefs.getBoolean(SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_DOCKED_MODE, false)

            val intent = Intent("SHOW_AMBIENT_GLANCE").apply {
                putExtra("event_type", "volume")
                putExtra("track_title", title)
                putExtra("artist_name", artist)
                putExtra("volume_percentage", percentage)
                putExtra("volume_key_code", keyCode)
                putExtra("is_docked_mode", isDockedMode)
            }
            ambientGlanceHandler.handleIntent(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return super.onStartCommand(intent, flags, startId)

        when (action) {
            "LOCK_SCREEN" -> {
                val hapticTypeStr =
                    prefs.getString("haptic_feedback_type", HapticFeedbackType.NONE.name)
                val hapticType = try {
                    HapticFeedbackType.valueOf(hapticTypeStr ?: HapticFeedbackType.NONE.name)
                } catch (e: Exception) {
                    HapticFeedbackType.NONE
                }

                if (hapticType != HapticFeedbackType.NONE) {
                    val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.let { performHapticFeedback(it, hapticType) }
                }
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }

            "SHOW_NOTIFICATION_LIGHTING" -> notificationLightingHandler.handleIntent(intent)
            "SHOW_AMBIENT_GLANCE" -> ambientGlanceHandler.handleIntent(intent)
            "FORCE_TURN_OFF_AOD" -> aodForceTurnOffHandler.forceTurnOff()

            "APP_AUTHENTICATED" -> intent.getStringExtra("package_name")
                ?.let { appFlowHandler.onAuthenticated(it) }

            "APP_AUTHENTICATION_FAILED" -> performGlobalAction(GLOBAL_ACTION_HOME)

            FlashlightActionReceiver.ACTION_INCREASE,
            FlashlightActionReceiver.ACTION_DECREASE,
            FlashlightActionReceiver.ACTION_OFF,
            FlashlightActionReceiver.ACTION_TOGGLE,
            FlashlightActionReceiver.ACTION_SET_INTENSITY,
            FlashlightActionReceiver.ACTION_START_SOS,
            FlashlightActionReceiver.ACTION_START_STROBE,
            FlashlightActionReceiver.ACTION_STOP_SPECIAL_MODES,
            FlashlightActionReceiver.ACTION_PULSE_NOTIFICATION -> flashlightHandler.handleIntent(
                intent
            )
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startInputEventListenerIfEnabled() {
        val isEnabled = prefs.getBoolean("button_remap_enabled", false)
        val useShizuku = prefs.getBoolean("button_remap_use_shizuku", false)

        if (isEnabled && useShizuku) {
            try {
                val intent = Intent(this, InputEventListenerService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun stopInputEventListener() {
        try {
            stopService(Intent(this, InputEventListenerService::class.java))
        } catch (e: Exception) {
            // Ignore
        }
    }

    companion object {
        var instance: ScreenOffAccessibilityService? = null
    }
}
