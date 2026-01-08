package com.sameerasw.essentials.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import com.sameerasw.essentials.utils.HapticFeedbackType
import com.sameerasw.essentials.utils.performHapticFeedback
import com.sameerasw.essentials.domain.model.NotificationLightingColorMode
import com.sameerasw.essentials.domain.model.NotificationLightingStyle
import com.sameerasw.essentials.domain.model.NotificationLightingSide
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.View
import com.sameerasw.essentials.utils.OverlayHelper
import android.util.Log
import android.view.KeyEvent
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.os.PowerManager
import com.google.gson.Gson
import com.sameerasw.essentials.domain.model.AppSelection
import com.google.gson.reflect.TypeToken
import android.media.AudioManager
import android.provider.Settings
import android.app.admin.DevicePolicyManager
import com.sameerasw.essentials.services.receivers.SecurityDeviceAdminReceiver
import android.content.ComponentName
import android.app.KeyguardManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.receivers.FlashlightActionReceiver
import android.app.Notification
import android.graphics.Color
import android.graphics.drawable.Icon
import com.sameerasw.essentials.utils.FlashlightUtil
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager


class ScreenOffAccessibilityService : AccessibilityService(), SensorEventListener {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentTorchId: String? = null
    private var currentIntensityLevel: Int = 1
    private var flashlightJob: Job? = null
    private var isInternalToggle = false
    private val cameraManager by lazy { getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val sensorManager by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private var proximitySensor: Sensor? = null
    private var isProximityBlocked = false
    
    private val NOTIFICATION_ID_FLASHLIGHT = 1001
    private val CHANNEL_ID_FLASHLIGHT = "flashlight_live_update"



    private var windowManager: WindowManager? = null
    private val overlayViews = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())
    private var cornerRadiusDp: Int = OverlayHelper.CORNER_RADIUS_DP
    private var strokeThicknessDp: Int = OverlayHelper.STROKE_DP
    private var isPreview: Boolean = false
    private var ignoreScreenState: Boolean = false
    private var colorMode: NotificationLightingColorMode = NotificationLightingColorMode.SYSTEM
    private var customColor: Int = 0
    private var resolvedColor: Int? = null
    private var pulseCount: Int = 1
    private var pulseDuration: Long = 3000
    private var edgeLightingStyle: NotificationLightingStyle = NotificationLightingStyle.STROKE
    private var glowSides: Set<NotificationLightingSide> = setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
    private var indicatorX: Float = 50f
    private var indicatorY: Float = 2f
    private var indicatorScale: Float = 1.0f
    private var screenReceiver: BroadcastReceiver? = null
    
    private var originalAnimationScale: Float = 1.0f
    private var isScaleModified: Boolean = false
    
    private var isAmbientDisplayRequested: Boolean = false
    private var isInterrupted: Boolean = false
    
    private var isTorchOn = false
    private val torchCallback =
        object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                super.onTorchModeChanged(cameraId, enabled)
                isTorchOn = enabled
                currentTorchId = cameraId
                
                val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                val isGlobalEnabled = prefs.getBoolean("flashlight_global_enabled", false)
                val lastIntensity = prefs.getInt("flashlight_last_intensity", 1)

                if (enabled) {
                    currentTorchId = cameraId
                    
                    if (isGlobalEnabled && !isInternalToggle) {
                        // External trigger - smoothly fade in to last known intensity
                        Log.d("Flashlight", "Global control detected external ON. Fading in to $lastIntensity")
                        flashlightJob?.cancel()
                        flashlightJob = serviceScope.launch {
                            com.sameerasw.essentials.utils.FlashlightUtil.fadeFlashlight(
                                this@ScreenOffAccessibilityService,
                                cameraId,
                                fromLevel = 1,
                                toLevel = lastIntensity,
                                durationMs = 400L,
                                steps = 20
                            )
                            updateFlashlightNotification(lastIntensity)
                        }
                    } else if (isInternalToggle) {
                        // Internal trigger - we already handled the job
                        isInternalToggle = false
                    } else {
                        // Normal mode or no global - sync level
                        currentIntensityLevel = com.sameerasw.essentials.utils.FlashlightUtil.getDefaultLevel(this@ScreenOffAccessibilityService, cameraId)
                        updateFlashlightNotification(currentIntensityLevel)
                    }
                } else {
                    // Flashlight turned OFF
                    flashlightJob?.cancel() // Stop any ongoing fade-in or fade-out
                    isInternalToggle = false // Reset
                    cancelFlashlightNotification()
                }
            }
        }

    private var lastPressedKeyCode: Int = -1
    private var lastPendingAction: String? = null
    private var isLongPressTriggered: Boolean = false
    private val longPressRunnable = Runnable {
        isLongPressTriggered = true
        lastPendingAction?.let { handleLongPress(it) }
    }
    private val longPressTimeout = 500L
    
    private var wasNightLightOnBeforeAutoToggle = false
    private var isNightLightAutoToggledOff = false
    private var lastForegroundPackage: String? = null
    private var pendingNLRunnable: Runnable? = null
    private val nlDebounceDelay = 500L

    private val ignoredSystemPackages = listOf(
        "android",
    )

    private val authenticatedPackages = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_ON) {
                    val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                    val onlyShowWhenScreenOff = prefs.getBoolean("edge_lighting_only_screen_off", true)
                    if (onlyShowWhenScreenOff && !isPreview) {
                        removeOverlay()
                    }
                } else if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    authenticatedPackages.clear()
                } else if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    restoreAnimationScale()
                } else if (intent?.action == InputEventListenerService.ACTION_VOLUME_LONG_PRESSED) {
                    val direction = intent.getStringExtra(InputEventListenerService.EXTRA_DIRECTION)
                    if (direction != null) {
                         // Map direction to action
                         val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                         val isScreenOn = try {
                             (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
                         } catch(e: Exception) { false }
                         
                         val actionKeySuffix = if (isScreenOn) "_on" else "_off"
                         val actionKey = if (direction == "UP") {
                             "button_remap_vol_up_action$actionKeySuffix"
                         } else {
                             "button_remap_vol_down_action$actionKeySuffix"
                         }
                         val action = prefs.getString(actionKey, "None") ?: "None"
                         handleLongPress(action)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(InputEventListenerService.ACTION_VOLUME_LONG_PRESSED)
        }
        registerReceiver(screenReceiver, filter, Context.RECEIVER_EXPORTED)

        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        torchCallback.let { cameraManager.registerTorchCallback(it, handler) }

        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Ensure flags are set to filter key events
        serviceInfo = serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        torchCallback.let { cameraManager.unregisterTorchCallback(it) }
        sensorManager.unregisterListener(this)
        currentTorchId = null
        restoreAnimationScale()
        removeOverlay()
        serviceScope.cancel()
        super.onDestroy()



    }


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val isScreenLockedSecurityEnabled = prefs.getBoolean("screen_locked_security_enabled", false)

        if (isScreenLockedSecurityEnabled) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (keyguardManager.isKeyguardLocked) {
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED || event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    val source = event.source
                    if (source != null) {
                        val keywords = listOf(
                            "Internet", "Mobile Data", "Wi-Fi", // English
                            "Daten", "WLAN", // German
                            "Datos", // Spanish
                            "Donn", // French (Données)
                            "Cellular" // Some variants
                        )
                        
                        var isNetworkTile = false
                        for (text in keywords) {
                            if (findNodeByText(source, text)) {
                                isNetworkTile = true
                                break
                            }
                        }

                        if (isNetworkTile) {
                            setReducedAnimationScale()
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            lockDeviceHard()
                            Toast.makeText(this, "Unlock phone to change network settings", Toast.LENGTH_SHORT).show()
                            return
                        }
                    }
                }
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            if (packageName == lastForegroundPackage) return
            lastForegroundPackage = packageName
            
            checkHighlightNightLight(packageName)
            checkAppLock(packageName)
        }
    }

    private fun checkAppLock(packageName: String) {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("app_lock_enabled", false)
        if (!isEnabled) return

        if (packageName == this.packageName) {
            return
        }

        val json = prefs.getString("app_lock_selected_apps", null)
        val selectedApps: List<AppSelection> = if (json != null) {
            try {
                Gson().fromJson(json, object : TypeToken<List<AppSelection>>() {}.type)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val isLocked = selectedApps.find { it.packageName == packageName }?.isEnabled ?: false
        
        if (isLocked && !authenticatedPackages.contains(packageName)) {
            Log.d("AppLock", "App $packageName is locked and not authenticated. Showing lock screen.")
            val intent = Intent().apply {
                component = ComponentName(this@ScreenOffAccessibilityService, "com.sameerasw.essentials.AppLockActivity")
                putExtra("package_to_lock", packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            startActivity(intent)
        }
    }


    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): Boolean {
        val nodes = node.findAccessibilityNodeInfosByText(text)
        if (nodes.isNotEmpty()) return true
        
        val desc = node.contentDescription
        return desc != null && desc.toString().contains(text, ignoreCase = true)
    }

    private fun setReducedAnimationScale() {
        if (isScaleModified) return
        try {
            originalAnimationScale = Settings.Global.getFloat(
                contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            Settings.Global.putFloat(
                contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                0.1f
            )
            isScaleModified = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun restoreAnimationScale() {
        if (!isScaleModified) return
        try {
            Settings.Global.putFloat(
                contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                originalAnimationScale
            )
            isScaleModified = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun lockDeviceHard() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(this, SecurityDeviceAdminReceiver::class.java)
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
            } else {
                // Fallback to accessibility power button if admin not active
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Last resort fallback
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }

    private fun checkHighlightNightLight(packageName: String) {
        // Cancel any pending NL toggle
        pendingNLRunnable?.let { handler.removeCallbacks(it) }

        // Skip processing for system packages to avoid transient NL restoration
        if (ignoredSystemPackages.contains(packageName)) {
            Log.d("NightLight", "Ignoring system package $packageName")
            return
        }

        val runnable = Runnable {
            processNightLightChange(packageName)
        }
        pendingNLRunnable = runnable
        handler.postDelayed(runnable, nlDebounceDelay)
    }

    private fun processNightLightChange(packageName: String) {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("dynamic_night_light_enabled", false)
        if (!isEnabled) return

        val json = prefs.getString("dynamic_night_light_selected_apps", null)
        val selectedApps: List<AppSelection> = if (json != null) {
            try {
                Gson().fromJson(json, object : TypeToken<List<AppSelection>>() {}.type)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val isAppSelected = selectedApps.find { it.packageName == packageName }?.isEnabled ?: false
        val isNLCurrentlyOn = isNightLightEnabled()

        if (isAppSelected) {
            // App is selected. If NL is on, turn it off and record that we did so.
            if (isNLCurrentlyOn) {
                Log.d("NightLight", "Turning off night light for $packageName")
                wasNightLightOnBeforeAutoToggle = true
                isNightLightAutoToggledOff = true
                setNightLightEnabled(false)
            }
        } else {
            // App is NOT selected. 
            // Only restore NL if it was auto-toggled off AND it was ON before that.
            if (isNightLightAutoToggledOff && wasNightLightOnBeforeAutoToggle) {
                Log.d("NightLight", "Restoring night light (was turned off for previous app)")
                setNightLightEnabled(true)
                isNightLightAutoToggledOff = false
                wasNightLightOnBeforeAutoToggle = false
            } else if (isNightLightAutoToggledOff) {
                isNightLightAutoToggledOff = false
            }
        }
    }

    private fun isNightLightEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(contentResolver, "night_display_activated", 0) == 1
        } catch (_: Exception) {
            false
        }
    }

    private fun setNightLightEnabled(enabled: Boolean) {
        try {
            Settings.Secure.putInt(contentResolver, "night_display_activated", if (enabled) 1 else 0)
        } catch (e: Exception) {
            Log.w("NightLight", "Failed to set night light: ${e.message}. Ensure WRITE_SECURE_SETTINGS is granted.")
        }
    }

    override fun onInterrupt() {
        // Not used
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            val maxRange = event.sensor.maximumRange
            isProximityBlocked = distance < maxRange && distance < 5f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return super.onStartCommand(intent, flags, startId)
        
        when (action) {
            "LOCK_SCREEN" -> {
                triggerHapticFeedback(useWidgetPreference = true)            
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
            "SHOW_NOTIFICATION_LIGHTING" -> {
                cornerRadiusDp = intent.getIntExtra("corner_radius_dp", OverlayHelper.CORNER_RADIUS_DP)
                strokeThicknessDp = intent.getIntExtra("stroke_thickness_dp", OverlayHelper.STROKE_DP)
                isPreview = intent.getBooleanExtra("is_preview", false)
                ignoreScreenState = intent.getBooleanExtra("ignore_screen_state", false)
                colorMode = NotificationLightingColorMode.valueOf(intent.getStringExtra("color_mode") ?: "SYSTEM")
                customColor = intent.getIntExtra("custom_color", 0)
                resolvedColor = if (intent.hasExtra("resolved_color")) intent.getIntExtra("resolved_color", 0) else null
                pulseCount = intent.getIntExtra("pulse_count", 1)
                pulseDuration = intent.getLongExtra("pulse_duration", 3000)
                val styleName = intent.getStringExtra("style")
                edgeLightingStyle = if (styleName != null) NotificationLightingStyle.valueOf(styleName) else NotificationLightingStyle.STROKE
                val glowSidesArray = intent.getStringArrayExtra("glow_sides")
                glowSides = glowSidesArray?.mapNotNull { try { NotificationLightingSide.valueOf(it) } catch(_: Exception) { null } }?.toSet() 
                    ?: setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
                indicatorX = intent.getFloatExtra("indicator_x", 50f)
                indicatorY = intent.getFloatExtra("indicator_y", 2f)
                indicatorScale = intent.getFloatExtra("indicator_scale", 1.0f)
                isAmbientDisplayRequested = intent.getBooleanExtra("is_ambient_display", false)
                isInterrupted = false
                
                val removePreview = intent.getBooleanExtra("remove_preview", false)
                if (removePreview) {
                    removeOverlay()
                    return super.onStartCommand(intent, flags, startId)
                }
                
                try {
                    showNotificationLighting()
                } catch (e: Exception) {
                    Log.e("ButtonRemap", "Failed to show notification lighting", e)
                }
            }
            "APP_AUTHENTICATED" -> {
                intent.getStringExtra("package_name")?.let { authenticatedPackages.add(it) }
            }
            "APP_AUTHENTICATION_FAILED" -> {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            FlashlightActionReceiver.ACTION_INCREASE -> {
                adjustFlashlightIntensity(true)
            }
            FlashlightActionReceiver.ACTION_DECREASE -> {
                adjustFlashlightIntensity(false)
            }
            FlashlightActionReceiver.ACTION_OFF -> {
                if (isTorchOn) toggleFlashlight()
            }
            FlashlightActionReceiver.ACTION_TOGGLE -> {
                toggleFlashlight()
            }
            FlashlightActionReceiver.ACTION_SET_INTENSITY -> {
                val level = intent.getIntExtra(FlashlightActionReceiver.EXTRA_INTENSITY, 1)
                currentIntensityLevel = level
                if (!isTorchOn) {
                    toggleFlashlight(overrideIntensity = level)
                } else {
                    currentTorchId?.let { id ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                            FlashlightUtil.isIntensitySupported(this, id)) {
                            try {
                                cameraManager.turnOnTorchWithStrengthLevel(id, level)
                                updateFlashlightNotification(level)
                            } catch (e: Exception) {
                                Log.e("Flashlight", "Error setting intensity level", e)
                            }
                        }
                    }
                }
            }
            FlashlightActionReceiver.ACTION_PULSE_NOTIFICATION -> {
                pulseFlashlightForNotification()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun triggerHapticFeedback(specificType: HapticFeedbackType? = null, useWidgetPreference: Boolean = false) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null) {
                val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                val prefKey = if (useWidgetPreference) "haptic_feedback_type" else "button_remap_haptic_type"
                val defaultType = if (useWidgetPreference) HapticFeedbackType.SUBTLE.name else HapticFeedbackType.DOUBLE.name
                
                val typeName = specificType?.name ?: prefs.getString(prefKey, defaultType)
                val feedbackType = try {
                    val type = HapticFeedbackType.valueOf(typeName ?: defaultType)
                    if (type.name == "LONG") HapticFeedbackType.DOUBLE else type
                } catch (_: Exception) {
                    HapticFeedbackType.valueOf(defaultType)
                }

                performHapticFeedback(vibrator, feedbackType)
            }
        } catch (_: Exception) {
            // Silently fail if vibrator is not available
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun showNotificationLighting() {
        // For preview mode, remove existing overlays first to update with new corner radius
        if (isPreview && overlayViews.isNotEmpty()) {
            removeOverlay()
        }

        // Avoid duplicates
        if (overlayViews.isNotEmpty()) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        // Helper to get accessibility overlay type if present
        val overlayType = try {
            WindowManager.LayoutParams::class.java.getField("TYPE_ACCESSIBILITY_OVERLAY").getInt(null)
        } catch (_: Exception) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }

        try {
            val color = when {
                resolvedColor != null -> resolvedColor!!
                colorMode == NotificationLightingColorMode.CUSTOM -> customColor
                else -> getColor(android.R.color.system_accent1_100)
            }
            
            val overlay = OverlayHelper.createOverlayView(
                this, 
                color, 
                strokeDp = strokeThicknessDp, 
                cornerRadiusDp = cornerRadiusDp,
                style = edgeLightingStyle,
                glowSides = glowSides,
                indicatorScale = indicatorScale
            )
            val params = OverlayHelper.createOverlayLayoutParams(overlayType)

            // Ambient Display Logic: Pre-create black overlay before waking screen
            val isScreenOn = powerManager.isInteractive
            val showBackground = isAmbientDisplayRequested && !isScreenOn && !isPreview
            
            if (showBackground) {
                // Re-create overlay with background and touch support
                val ambientOverlay = OverlayHelper.createOverlayView(
                    this,
                    color,
                    strokeDp = strokeThicknessDp,
                    cornerRadiusDp = cornerRadiusDp,
                    style = edgeLightingStyle,
                    glowSides = glowSides,
                    indicatorScale = indicatorScale,
                    showBackground = true
                )
                val ambientParams = OverlayHelper.createOverlayLayoutParams(overlayType, isTouchable = true)
                
                ambientOverlay.setOnTouchListener { _, _ ->
                    isInterrupted = true
                    removeOverlay()
                    true
                }

                if (OverlayHelper.addOverlayView(windowManager, ambientOverlay, ambientParams)) {
                    overlayViews.add(ambientOverlay)
                    
                    // Wake screen AFTER adding black overlay
                    try {
                        val wakeLock = powerManager.newWakeLock(
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                            "essentials:NotificationLighting"
                        )
                        wakeLock.acquire(10000L) // 10s max
                    } catch (e: Exception) {
                        Log.e("ScreenOffAccessibility", "Failed to wake screen", e)
                    }
                    
                    // Delay actual lighting animation until screen is fully on (~500ms)
                    handler.postDelayed({
                        if (!isInterrupted) {
                            startPulsing(ambientOverlay)
                        }
                    }, 500)
                }
            } else {
                // Normal mode
                val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                val onlyShowWhenScreenOff = prefs.getBoolean("edge_lighting_only_screen_off", true)
                if (onlyShowWhenScreenOff && !ignoreScreenState && !isPreview) {
                    if (isScreenOn) {
                        removeOverlay()
                        return
                    }
                }

                if (OverlayHelper.addOverlayView(windowManager, overlay, params)) {
                    overlayViews.add(overlay)
                    if (isPreview) {
                        OverlayHelper.showPreview(overlay, edgeLightingStyle, strokeThicknessDp)
                    } else {
                        startPulsing(overlay)
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun startPulsing(overlay: View) {
        OverlayHelper.pulseOverlay(
            overlay,
            maxPulses = pulseCount,
            pulseDurationMillis = pulseDuration,
            style = edgeLightingStyle,
            strokeWidthDp = strokeThicknessDp,
            indicatorX = indicatorX,
            indicatorY = indicatorY
        ) {
            // When pulsing completes
            if (isAmbientDisplayRequested && !isInterrupted && !isPreview) {
                // Auto-lock FIRST
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                
                // Then remove the overlay after a short delay to ensure screen is off
                handler.postDelayed({
                    OverlayHelper.fadeOutAndRemoveOverlay(windowManager, overlay, overlayViews)
                }, 500)
            } else {
                OverlayHelper.fadeOutAndRemoveOverlay(windowManager, overlay, overlayViews)
            }
        }
    }

    private fun removeOverlay() {
        // Remove all overlays and clear the list
        for (overlay in overlayViews) {
            try {
                OverlayHelper.fadeOutAndRemoveOverlay(windowManager, overlay, overlayViews)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        overlayViews.clear()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return super.onKeyEvent(event)
        }

        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val isButtonRemapEnabled = prefs.getBoolean("button_remap_enabled", false)
        val isButtonRemapUseShizuku = prefs.getBoolean("button_remap_use_shizuku", false)
        val isAdjustEnabled = prefs.getBoolean("flashlight_adjust_intensity_enabled", false)
        val isGlobalEnabled = prefs.getBoolean("flashlight_global_enabled", false)

        // If Shizuku method is enabled, skip accessibility event handling for remapping
        if (isButtonRemapUseShizuku && isButtonRemapEnabled) {
             val isTorchControl = isTorchOn && (isAdjustEnabled || isGlobalEnabled) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
             
             // Check user mapping
             val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
             val isScreenOn = try { powerManager.isInteractive } catch(e: Exception) { false }
             val suffix = if (isScreenOn) "_on" else "_off"
             
             val actionKey = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) "button_remap_vol_up_action$suffix" else "button_remap_vol_down_action$suffix"
             val action = prefs.getString(actionKey, "None")
             val isMapped = action != null && action != "None"
             
             if (isMapped || isTorchControl) {
                 return true
             }
             return super.onKeyEvent(event)
        }

        if (isTorchOn && (isAdjustEnabled || isGlobalEnabled) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0) {
                    isLongPressTriggered = false
                    lastPressedKeyCode = keyCode
                    // We still allow long press to turn off the flashlight
                    lastPendingAction = "Toggle flashlight" 
                    handler.postDelayed(longPressRunnable, longPressTimeout)
                }
                return true
            } else if (event.action == KeyEvent.ACTION_UP) {
                handler.removeCallbacks(longPressRunnable)
                if (!isLongPressTriggered) {
                    adjustFlashlightIntensity(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                }
                return true
            }
        }

        if (!isButtonRemapEnabled) return super.onKeyEvent(event)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive

        val actionKeySuffix = if (isScreenOn) "_on" else "_off"
        val actionKey = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            "button_remap_vol_up_action$actionKeySuffix"
        } else {
            "button_remap_vol_down_action$actionKeySuffix"
        }
        
        val action = prefs.getString(actionKey, "None") ?: "None"
        val isAlwaysTurnOffEnabled = prefs.getBoolean("flashlight_always_turn_off_enabled", false)
        
        // Check if the pressed button is assigned to flashlight in ANY state
        val isVolUpFlashlight = prefs.getString("button_remap_vol_up_action_off", "None") == "Toggle flashlight" ||
                                prefs.getString("button_remap_vol_up_action_on", "None") == "Toggle flashlight"
        val isVolDownFlashlight = prefs.getString("button_remap_vol_down_action_off", "None") == "Toggle flashlight" ||
                                  prefs.getString("button_remap_vol_down_action_on", "None") == "Toggle flashlight"
        
        val isFlashlightCapableButton = (keyCode == KeyEvent.KEYCODE_VOLUME_UP && isVolUpFlashlight) ||
                                       (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && isVolDownFlashlight)

        var finalAction = action
        if (isTorchOn && isAlwaysTurnOffEnabled && isFlashlightCapableButton) {
            finalAction = "Toggle flashlight"
        }

        if (finalAction == "None") return super.onKeyEvent(event)

        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.repeatCount == 0) {
                lastPressedKeyCode = keyCode
                lastPendingAction = finalAction
                isLongPressTriggered = false
                handler.postDelayed(longPressRunnable, longPressTimeout)
            }
            return true
        } else if (event.action == KeyEvent.ACTION_UP) {
            handler.removeCallbacks(longPressRunnable)
            if (!isLongPressTriggered) {
                // Short press - re-simulate volume behavior
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val direction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            }
            return true
        }
        
        return super.onKeyEvent(event)
    }

    private fun updateFlashlightNotification(intensity: Int) {
        val prefs = getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("flashlight_live_update_enabled", true)) {
            cancelFlashlightNotification()
            return
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_FLASHLIGHT,
                "Flashlight Controls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Controls for active flashlight"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val cameraId = currentTorchId ?: return
        val maxLevel = com.sameerasw.essentials.utils.FlashlightUtil.getMaxLevel(this, cameraId)
        val percentage = (intensity * 100) / maxOf(1, maxLevel)
        
        val decreaseIntent = PendingIntent.getBroadcast(this, 1, 
            Intent(this, FlashlightActionReceiver::class.java).apply { action = FlashlightActionReceiver.ACTION_DECREASE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            
        val increaseIntent = PendingIntent.getBroadcast(this, 2, 
            Intent(this, FlashlightActionReceiver::class.java).apply { action = FlashlightActionReceiver.ACTION_INCREASE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            
        val offIntent = PendingIntent.getBroadcast(this, 3, 
            Intent(this, FlashlightActionReceiver::class.java).apply { action = FlashlightActionReceiver.ACTION_OFF },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= 35) {
            // Native API 35+ implementation for Live Updates
            try {
                val builder = Notification.Builder(this, CHANNEL_ID_FLASHLIGHT)
                    .setSmallIcon(R.drawable.rounded_flashlight_on_24)
                    .setContentTitle("Flashlight active")
                    .setContentText("Brightness: $percentage%")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setColorized(false)
                    .setShowWhen(false)
                    .addAction(Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.rounded_keyboard_arrow_down_24),
                        "-", decreaseIntent).build())
                    .addAction(Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.rounded_power_settings_new_24),
                        "Off", offIntent).build())
                    .addAction(Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.rounded_keyboard_arrow_up_24),
                        "+", increaseIntent).build())

                if (Build.VERSION.SDK_INT >= 36) {
                    try {
                        val progressStyle = Notification.ProgressStyle()
                            .setStyledByProgress(true)
                            .setProgress(intensity)
                            .setProgressTrackerIcon(Icon.createWithResource(this, R.drawable.rounded_flashlight_on_24))
                        
                        progressStyle.addProgressSegment(
                            Notification.ProgressStyle.Segment(maxLevel)
                                .setColor(Color.YELLOW)
                        )
                        builder.setStyle(progressStyle)
                    } catch (e: Throwable) {
                        Log.e("FlashlightNotification", "ProgressStyle error", e)
                    }
                }

                // Promotion and Status Chip text (API 35+)
                try {
                    builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                        .invoke(builder, true)
                    builder.javaClass.getMethod("setShortCriticalText", CharSequence::class.java)
                        .invoke(builder, "$percentage%")
                } catch (_: Throwable) {
                    // Methods not found, will rely on extras below
                }
                
                // Add mandatory extras for promotion as fallback or if methods are missing
                val extras = android.os.Bundle()
                extras.putBoolean("android.requestPromotedOngoing", true)
                extras.putString("android.shortCriticalText", "$percentage%")
                builder.addExtras(extras)

                notificationManager.notify(NOTIFICATION_ID_FLASHLIGHT, builder.build())
                return
            } catch (e: Exception) {
                Log.e("FlashlightNotification", "Native builder failed, falling back to compat", e)
            }
        }

        // Fallback for API < 35
        val builder = NotificationCompat.Builder(this, CHANNEL_ID_FLASHLIGHT)
            .setSmallIcon(R.drawable.rounded_flashlight_on_24)
            .setContentTitle("Flashlight active")
            .setContentText("Brightness: $percentage%")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(R.drawable.rounded_keyboard_arrow_down_24, "-", decreaseIntent)
            .addAction(R.drawable.rounded_power_settings_new_24, "Off", offIntent)
            .addAction(R.drawable.rounded_keyboard_arrow_up_24, "+", increaseIntent)
            .addExtras(android.os.Bundle().apply {
                putBoolean("android.requestPromotedOngoing", true)
                putString("android.shortCriticalText", "$percentage%")
            })

        notificationManager.notify(NOTIFICATION_ID_FLASHLIGHT, builder.build())
    }

    private fun getCameraId(): String? {
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash) return id
            }
        } catch (e: Exception) {
            Log.e("Flashlight", "Error getting camera ID", e)
        }
        return null
    }

    private fun pulseFlashlightForNotification() {
        if (isTorchOn) return
        
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val pulseEnabled = prefs.getBoolean("flashlight_pulse_enabled", false)
        if (!pulseEnabled) return
        
        val faceDownOnly = prefs.getBoolean("flashlight_pulse_facedown_only", true)
        if (faceDownOnly && !isProximityBlocked) return

        val cameraId = currentTorchId ?: getCameraId() ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
            FlashlightUtil.isIntensitySupported(this, cameraId)) {
                
                flashlightJob?.cancel()
                flashlightJob = serviceScope.launch {
                    val maxLevel = FlashlightUtil.getMaxLevel(this@ScreenOffAccessibilityService, cameraId)
                    val targetPulseLevel = (maxLevel * 0.2f).toInt().coerceAtLeast(1)
                    
                    isInternalToggle = true // To prevent other logic from interfering
                    
                    // Fade In
                    FlashlightUtil.fadeFlashlight(
                        this@ScreenOffAccessibilityService,
                        cameraId,
                        fromLevel = 0,
                        toLevel = targetPulseLevel,
                        durationMs = 600L,
                        steps = 40
                    )
                    
                    kotlinx.coroutines.delay(800L) // Hold
                    
                    // Fade Out
                    FlashlightUtil.fadeFlashlight(
                        this@ScreenOffAccessibilityService,
                        cameraId,
                        fromLevel = targetPulseLevel,
                        toLevel = 0,
                        durationMs = 600L,
                        steps = 40
                    )
                    
                    isInternalToggle = false
                }
            }
    }

    private fun cancelFlashlightNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_FLASHLIGHT)
    }

    private fun adjustFlashlightIntensity(increase: Boolean) {
        val cameraId = currentTorchId ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val maxLevel = com.sameerasw.essentials.utils.FlashlightUtil.getMaxLevel(this, cameraId)
            
            // Try to get current system level to stay in sync
            val currentSystemLevel = com.sameerasw.essentials.utils.FlashlightUtil.getCurrentLevel(this, cameraId)
            
            val step = maxOf(1, maxLevel / 5)
            
            val isAtLimit = if (increase) currentSystemLevel >= maxLevel else currentSystemLevel <= 1
            
            if (isAtLimit) {
                Log.d("Flashlight", "At limit, giving stronger feedback")
                triggerHapticFeedback(specificType = com.sameerasw.essentials.utils.HapticFeedbackType.DOUBLE)
                return
            }

            val targetLevel = if (increase) {
                (currentSystemLevel + step).coerceAtMost(maxLevel)
            } else {
                (currentSystemLevel - step).coerceAtLeast(1)
            }
            
            Log.d("Flashlight", "Smoothly adjusting intensity to $targetLevel (from $currentSystemLevel)")
            
            // Sync local state
            currentIntensityLevel = targetLevel
            updateFlashlightNotification(targetLevel)
            
            val prefs = getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            // Persist the level if global is enabled
            if (prefs.getBoolean("flashlight_global_enabled", false)) {
                prefs.edit().putInt("flashlight_last_intensity", targetLevel).apply()
            }
            
            // Cancel any ongoing adjustment or toggle fade
            flashlightJob?.cancel()
            flashlightJob = serviceScope.launch {
                com.sameerasw.essentials.utils.FlashlightUtil.fadeFlashlight(
                    this@ScreenOffAccessibilityService,
                    cameraId,
                    fromLevel = currentSystemLevel,
                    toLevel = targetLevel,
                    durationMs = 150L,
                    steps = 5
                )
            }


            
            // Give stronger feedback if we just reached the limit
            if (targetLevel == maxLevel || targetLevel == 1) {
                triggerHapticFeedback(specificType = com.sameerasw.essentials.utils.HapticFeedbackType.DOUBLE)
            } else {
                triggerHapticFeedback(specificType = com.sameerasw.essentials.utils.HapticFeedbackType.SUBTLE)
            }
        } catch (e: Exception) {
            Log.e("Flashlight", "Error adjusting intensity", e)
        }
    }

    private fun handleLongPress(action: String) {
        when (action) {
            "Toggle flashlight" -> toggleFlashlight()
            "Media play/pause" -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            "Media next" -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            "Media previous" -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            "Toggle vibrate" -> toggleRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            "Toggle mute" -> toggleRingerMode(AudioManager.RINGER_MODE_SILENT)
            "AI assistant" -> launchAssistant()
            "Take screenshot" -> takeScreenshot()
        }
    }

    private fun takeScreenshot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            triggerHapticFeedback()
        } else {
            Log.w("ButtonRemap", "Take screenshot is only supported on Android 9+")
        }
    }

    private fun sendMediaKey(keyCode: Int) {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        triggerHapticFeedback()
    }

    private fun toggleRingerMode(targetMode: Int) {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentMode = am.ringerMode
        if (currentMode == targetMode) {
            am.ringerMode = AudioManager.RINGER_MODE_NORMAL
        } else {
            am.ringerMode = targetMode
        }
        triggerHapticFeedback()
    }

    private fun launchAssistant() {
        try {
            val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            triggerHapticFeedback()
        } catch (e: Exception) {
            Log.e("ButtonRemap", "Failed to launch assistant", e)
        }
    }

    private fun toggleFlashlight(overrideIntensity: Int? = null) {
        Log.d("Flashlight", "Toggling flashlight, current state: $isTorchOn")
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val isFadeEnabled = prefs.getBoolean("flashlight_fade_enabled", false)

        try {
            var targetCameraId: String? = null

            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)

                if (flashAvailable && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    targetCameraId = id
                    break
                }
            }

            if (targetCameraId == null) {
                for (id in cameraManager.cameraIdList) {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    if (chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                        targetCameraId = id
                        break
                    }
                }
            }

            if (targetCameraId != null) {
                val finalCameraId = targetCameraId
                currentTorchId = finalCameraId
                val maxLevel = com.sameerasw.essentials.utils.FlashlightUtil.getMaxLevel(this, finalCameraId)
                val defaultLevel = com.sameerasw.essentials.utils.FlashlightUtil.getDefaultLevel(this, finalCameraId)
                
                if (isFadeEnabled && com.sameerasw.essentials.utils.FlashlightUtil.isIntensitySupported(this, finalCameraId)) {
                    val targetState = !isTorchOn
                    if (targetState) {
                        currentIntensityLevel = overrideIntensity ?: if (prefs.getBoolean("flashlight_global_enabled", false)) {
                            prefs.getInt("flashlight_last_intensity", defaultLevel)
                        } else {
                            defaultLevel
                        }
                    }
                    isInternalToggle = true
                    flashlightJob?.cancel()
                    flashlightJob = serviceScope.launch {
                        com.sameerasw.essentials.utils.FlashlightUtil.fadeFlashlight(
                            this@ScreenOffAccessibilityService,
                            finalCameraId,
                            targetState,
                            maxLevel = currentIntensityLevel
                        )
                    }
                    if (targetState) {
                        updateFlashlightNotification(currentIntensityLevel)
                    } else {
                        cancelFlashlightNotification()
                    }
                } else {
                    isInternalToggle = true
                    flashlightJob?.cancel()
                    cameraManager.setTorchMode(finalCameraId, !isTorchOn)
                    currentIntensityLevel = overrideIntensity ?: if (prefs.getBoolean("flashlight_global_enabled", false)) {
                        prefs.getInt("flashlight_last_intensity", defaultLevel)
                    } else {
                        defaultLevel
                    }
                    if (!isTorchOn) {
                        updateFlashlightNotification(currentIntensityLevel)
                    } else {
                        cancelFlashlightNotification()
                    }
                }




                triggerHapticFeedback()
            } else {

                Log.w("Flashlight", "No camera with flash found")
            }
        } catch (e: Exception) {
            Log.e("Flashlight", "Error toggling flashlight", e)
        }
    }


}