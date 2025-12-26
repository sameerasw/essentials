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

    private val longPressRunnable = Runnable {
        toggleFlashlight()
    }
    private val LONG_PRESS_TIMEOUT = 500L
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
        // Not used for this feature
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
                val typeName = specificType?.name ?: prefs.getString("haptic_feedback_type", HapticFeedbackType.NONE.name)
                val feedbackType = try {
                    HapticFeedbackType.valueOf(typeName ?: HapticFeedbackType.NONE.name)
                } catch (e: Exception) {
                    HapticFeedbackType.NONE
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
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val triggerButton = prefs.getString("flashlight_trigger_button", "Volume Up")
        val targetKeyCode = if (triggerButton == "Volume Down") KeyEvent.KEYCODE_VOLUME_DOWN else KeyEvent.KEYCODE_VOLUME_UP

        if (event.keyCode == targetKeyCode) {
            val isEnabled = prefs.getBoolean("flashlight_volume_toggle_enabled", false)
            if (!isEnabled) return super.onKeyEvent(event)

            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                powerManager.isInteractive
            } else {
                @Suppress("DEPRECATION")
                powerManager.isScreenOn
            }

            // Log event for debugging
            Log.d("Flashlight", "KeyEvent: action=${event.action}, screenOn=$isScreenOn")

            // Only intercept if screen is off
            if (!isScreenOn) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (event.repeatCount == 0) {
                        Log.d("Flashlight", "Long press timer started")
                        handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT)
                    }
                    return true // Consume event
                } else if (event.action == KeyEvent.ACTION_UP) {
                    Log.d("Flashlight", "Long press timer removed")
                    handler.removeCallbacks(longPressRunnable)
                    return true // Consume event
                }
            }
        }
        return super.onKeyEvent(event)
    }

    private fun toggleFlashlight() {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val hapticName = prefs.getString("flashlight_haptic_type", HapticFeedbackType.LONG.name)
        val hapticType = try {
            HapticFeedbackType.valueOf(hapticName ?: HapticFeedbackType.LONG.name)
        } catch (e: Exception) {
            HapticFeedbackType.LONG
        }

        Log.d("Flashlight", "Toggling flashlight, current state: $isTorchOn, haptic: $hapticType")
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
                        triggerHapticFeedback(hapticType)
                        return
                    }
                }
                
                // Fallback: use first camera with flash if no back camera found with flash
                for (id in cameraManager.cameraIdList) {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    if (chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                        Log.d("Flashlight", "Fallback: Setting torch mode for camera $id to ${!isTorchOn}")
                        cameraManager.setTorchMode(id, !isTorchOn)
                        triggerHapticFeedback(hapticType)
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