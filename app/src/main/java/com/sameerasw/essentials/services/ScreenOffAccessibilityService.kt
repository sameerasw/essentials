package com.sameerasw.essentials.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import com.sameerasw.essentials.utils.HapticFeedbackType
import com.sameerasw.essentials.utils.performHapticFeedback
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
import android.provider.Settings
import com.google.gson.Gson
import com.sameerasw.essentials.domain.model.AppSelection
import com.google.gson.reflect.TypeToken
import android.media.AudioManager

class ScreenOffAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private val overlayViews = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())
    private var cornerRadiusDp: Int = OverlayHelper.CORNER_RADIUS_DP
    private var strokeThicknessDp: Int = OverlayHelper.STROKE_DP
    private var isPreview: Boolean = false
    private var screenReceiver: BroadcastReceiver? = null
    
    private var isTorchOn = false
    private val torchCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                super.onTorchModeChanged(cameraId, enabled)
                isTorchOn = enabled
            }
        }
    } else null

    private var lastPressedKeyCode: Int = -1
    private val longPressRunnable = Runnable {
        handleLongPress(lastPressedKeyCode)
    }
    private val LONG_PRESS_TIMEOUT = 500L
    
    private var wasNightLightOnBeforeAutoToggle = false
    private var isNightLightAutoToggledOff = false
    private var lastForegroundPackage: String? = null

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
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        registerReceiver(screenReceiver, filter)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            torchCallback?.let { cameraManager.registerTorchCallback(it, handler) }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            torchCallback?.let { cameraManager.unregisterTorchCallback(it) }
        }
        removeOverlay()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            if (packageName == lastForegroundPackage) return
            lastForegroundPackage = packageName
            
            checkHighlightNightLight(packageName)
        }
    }

    private fun checkHighlightNightLight(packageName: String) {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("dynamic_night_light_enabled", false)
        if (!isEnabled) return

        val json = prefs.getString("dynamic_night_light_selected_apps", null)
        val selectedApps: List<AppSelection> = if (json != null) {
            try {
                Gson().fromJson(json, object : TypeToken<List<AppSelection>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val isAppSelected = selectedApps.find { it.packageName == packageName }?.isEnabled ?: false
        
        if (isAppSelected) {
            // App is selected, turn off night light if it's currently on
            if (isNightLightEnabled()) {
                Log.d("NightLight", "Turning off night light for $packageName")
                wasNightLightOnBeforeAutoToggle = true
                isNightLightAutoToggledOff = true
                setNightLightEnabled(false)
            }
        } else {
            // App is NOT selected, restore night light if we previously turned it off
            if (isNightLightAutoToggledOff) {
                Log.d("NightLight", "Restoring night light (was turned off for previous app)")
                setNightLightEnabled(true)
                isNightLightAutoToggledOff = false
                wasNightLightOnBeforeAutoToggle = false
            }
        }
    }

    private fun isNightLightEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(contentResolver, "night_display_activated", 0) == 1
        } catch (e: Exception) {
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "LOCK_SCREEN") {
            // Trigger haptic feedback based on user preference
            triggerHapticFeedback()
            // Lock the screen
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else if (intent?.action == "SHOW_EDGE_LIGHTING") {
            // Extract corner radius and preview flag from intent
            cornerRadiusDp = intent.getIntExtra("corner_radius_dp", OverlayHelper.CORNER_RADIUS_DP)
            strokeThicknessDp = intent.getIntExtra("stroke_thickness_dp", OverlayHelper.STROKE_DP)
            isPreview = intent.getBooleanExtra("is_preview", false)
            val removePreview = intent.getBooleanExtra("remove_preview", false)
            if (removePreview) {
                // Remove preview overlay
                removeOverlay()
                return super.onStartCommand(intent, flags, startId)
            }
            // Accessibility elevation: show overlay from accessibility service so it can appear above more surfaces
            try {
                showEdgeLighting()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun triggerHapticFeedback(specificType: HapticFeedbackType? = null) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null) {
                val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                val typeName = specificType?.name ?: prefs.getString("button_remap_haptic_type", HapticFeedbackType.DOUBLE.name)
                val feedbackType = try {
                    val type = HapticFeedbackType.valueOf(typeName ?: HapticFeedbackType.DOUBLE.name)
                    if (type.name == "LONG") HapticFeedbackType.DOUBLE else type
                } catch (e: Exception) {
                    HapticFeedbackType.DOUBLE
                }

                performHapticFeedback(vibrator, feedbackType)
            }
        } catch (e: Exception) {
            // Silently fail if vibrator is not available
        }
    }

    private fun showEdgeLighting() {
        // For preview mode, remove existing overlays first to update with new corner radius
        if (isPreview && overlayViews.isNotEmpty()) {
            removeOverlay()
        }

        // Avoid duplicates
        if (overlayViews.isNotEmpty()) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Helper to get accessibility overlay type if present
        val overlayType = try {
            WindowManager.LayoutParams::class.java.getField("TYPE_ACCESSIBILITY_OVERLAY").getInt(null)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        }

        try {
            val overlay = OverlayHelper.createOverlayView(this, android.R.color.system_accent1_100, strokeDp = strokeThicknessDp, cornerRadiusDp = cornerRadiusDp)
            val params = OverlayHelper.createOverlayLayoutParams(overlayType)

            if (OverlayHelper.addOverlayView(windowManager, overlay, params)) {
                overlayViews.add(overlay)
                if (isPreview) {
                    // For preview mode, just fade in and keep visible
                    OverlayHelper.fadeInOverlay(overlay)
                } else {
                    // If only show when screen off is enabled, check before pulsing
                    val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                    val onlyShowWhenScreenOff = prefs.getBoolean("edge_lighting_only_screen_off", true)
                    if (onlyShowWhenScreenOff) {
                        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                        val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                            powerManager.isInteractive
                        } else {
                            @Suppress("DEPRECATION")
                            powerManager.isScreenOn
                        }
                        if (isScreenOn) {
                            removeOverlay()
                            return
                        }
                    }

                    // Normal mode: pulse the overlay
                    OverlayHelper.pulseOverlay(overlay) {
                        // When pulsing completes, remove the overlay
                        OverlayHelper.fadeOutAndRemoveOverlay(windowManager, overlay, overlayViews)
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

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
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP && event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return super.onKeyEvent(event)
        }

        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("button_remap_enabled", false)
        if (!isEnabled) return super.onKeyEvent(event)

        val action = if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            prefs.getString("button_remap_vol_up_action", "None")
        } else {
            prefs.getString("button_remap_vol_down_action", "None")
        }

        if (action == null || action == "None") return super.onKeyEvent(event)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }

        val isAlwaysTurnOffEnabled = prefs.getBoolean("flashlight_always_turn_off_enabled", false)
        
        // Special case for flashlight: allow turning OFF while screen is on if enabled
        val isFlashlightAction = action == "Toggle flashlight"
        val shouldIntercept = !isScreenOn || (isFlashlightAction && isAlwaysTurnOffEnabled && isTorchOn)

        if (shouldIntercept) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0) {
                    lastPressedKeyCode = event.keyCode
                    handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT)
                }
                return true // Consume event
            } else if (event.action == KeyEvent.ACTION_UP) {
                handler.removeCallbacks(longPressRunnable)
                return true // Consume event
            }
        }
        
        return super.onKeyEvent(event)
    }

    private fun handleLongPress(keyCode: Int) {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val action = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            prefs.getString("button_remap_vol_up_action", "None")
        } else {
            prefs.getString("button_remap_vol_down_action", "None")
        }

        when (action) {
            "Toggle flashlight" -> toggleFlashlight()
            "Media play/pause" -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            "Media next" -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            "Media previous" -> sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            "Toggle vibrate" -> toggleRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            "Toggle mute" -> toggleRingerMode(AudioManager.RINGER_MODE_SILENT)
            "AI assistant" -> launchAssistant()
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

    private fun toggleFlashlight() {
        Log.d("Flashlight", "Toggling flashlight, current state: $isTorchOn")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                for (id in cameraManager.cameraIdList) {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
                    
                    // Prefer back camera with flash
                    if (flashAvailable && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        Log.d("Flashlight", "Setting torch mode for camera $id to ${!isTorchOn}")
                        cameraManager.setTorchMode(id, !isTorchOn)
                        triggerHapticFeedback()
                        return
                    }
                }
                
                // Fallback: use first camera with flash if no back camera found with flash
                for (id in cameraManager.cameraIdList) {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    if (chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                        Log.d("Flashlight", "Fallback: Setting torch mode for camera $id to ${!isTorchOn}")
                        cameraManager.setTorchMode(id, !isTorchOn)
                        triggerHapticFeedback()
                        return
                    }
                }
                
                Log.w("Flashlight", "No camera with flash found")
            } catch (e: Exception) {
                Log.e("Flashlight", "Error toggling flashlight", e)
            }
        }
    }

}