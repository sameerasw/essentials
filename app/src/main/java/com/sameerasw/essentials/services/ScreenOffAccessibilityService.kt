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

class ScreenOffAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private val overlayViews = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())
    private var cornerRadiusDp: Int = OverlayHelper.CORNER_RADIUS_DP
    private var strokeThicknessDp: Int = OverlayHelper.STROKE_DP
    private var isPreview: Boolean = false
    private var screenReceiver: BroadcastReceiver? = null
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
    }

    override fun onDestroy() {
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
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

    private fun triggerHapticFeedback() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null) {
                val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                val typeName = prefs.getString("haptic_feedback_type", HapticFeedbackType.NONE.name)
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

}