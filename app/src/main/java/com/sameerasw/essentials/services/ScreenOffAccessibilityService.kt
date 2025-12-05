package com.sameerasw.essentials.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.view.accessibility.AccessibilityEvent
import com.sameerasw.essentials.utils.HapticFeedbackType
import com.sameerasw.essentials.utils.performHapticFeedback
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.View
import androidx.core.content.ContextCompat
import android.provider.Settings
import com.sameerasw.essentials.utils.OverlayHelper

class ScreenOffAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private val overlayViews = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())

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
                val typeName = prefs.getString("haptic_feedback_type", HapticFeedbackType.SUBTLE.name)
                val feedbackType = try {
                    HapticFeedbackType.valueOf(typeName ?: HapticFeedbackType.SUBTLE.name)
                } catch (e: Exception) {
                    HapticFeedbackType.SUBTLE
                }

                performHapticFeedback(vibrator, feedbackType)
            }
        } catch (e: Exception) {
            // Silently fail if vibrator is not available
        }
    }

    private fun showEdgeLighting() {
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
            val overlay = OverlayHelper.createOverlayView(this, com.sameerasw.essentials.R.color.material_color_primary_expressive)
            val params = OverlayHelper.createOverlayLayoutParams(overlayType)

            if (OverlayHelper.addOverlayView(windowManager, overlay, params)) {
                overlayViews.add(overlay)
            }
        } catch (e: Exception) { e.printStackTrace() }

        // Remove after 5s
        handler.postDelayed({ removeOverlay() }, 5000)
    }

    private fun removeOverlay() {
        OverlayHelper.removeAllOverlays(windowManager, overlayViews)
    }

}