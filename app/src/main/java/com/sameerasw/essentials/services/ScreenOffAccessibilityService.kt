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
import android.widget.FrameLayout
import android.graphics.Color
import androidx.core.content.ContextCompat
import android.provider.Settings

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

        val color = ContextCompat.getColor(this, com.sameerasw.essentials.R.color.material_color_primary_expressive)
        val strokeDp = 6
        val strokePx = (resources.displayMetrics.density * strokeDp).toInt()

        // Helper to get accessibility overlay type if present
        val overlayType = try {
            WindowManager.LayoutParams::class.java.getField("TYPE_ACCESSIBILITY_OVERLAY").getInt(null)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        }

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD

        // Top
        try {
            val top = View(this)
            top.setBackgroundColor(color)
            val topParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                strokePx,
                overlayType,
                flags,
                android.graphics.PixelFormat.TRANSLUCENT
            )
            topParams.gravity = android.view.Gravity.TOP or android.view.Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try { topParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES } catch (_: Exception) {}
            }
            windowManager?.addView(top, topParams)
            overlayViews.add(top)
        } catch (e: Exception) { e.printStackTrace() }

        // Bottom
        try {
            val bottom = View(this)
            bottom.setBackgroundColor(color)
            val bottomParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                strokePx,
                overlayType,
                flags,
                android.graphics.PixelFormat.TRANSLUCENT
            )
            bottomParams.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.START
            windowManager?.addView(bottom, bottomParams)
            overlayViews.add(bottom)
        } catch (e: Exception) { e.printStackTrace() }

        // Left
        try {
            val left = View(this)
            left.setBackgroundColor(color)
            val leftParams = WindowManager.LayoutParams(
                strokePx,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                flags,
                android.graphics.PixelFormat.TRANSLUCENT
            )
            leftParams.gravity = android.view.Gravity.START or android.view.Gravity.TOP
            windowManager?.addView(left, leftParams)
            overlayViews.add(left)
        } catch (e: Exception) { e.printStackTrace() }

        // Right
        try {
            val right = View(this)
            right.setBackgroundColor(color)
            val rightParams = WindowManager.LayoutParams(
                strokePx,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                flags,
                android.graphics.PixelFormat.TRANSLUCENT
            )
            rightParams.gravity = android.view.Gravity.END or android.view.Gravity.TOP
            windowManager?.addView(right, rightParams)
            overlayViews.add(right)
        } catch (e: Exception) { e.printStackTrace() }

        // Remove after 5s
        handler.postDelayed({ removeOverlay() }, 5000)
    }

    private fun removeOverlay() {
        try {
            overlayViews.forEach { try { windowManager?.removeView(it) } catch (_: Exception) {} }
        } catch (e: Exception) { e.printStackTrace() }
        overlayViews.clear()
    }

    private fun getSystemBarHeight(name: String): Int {
        val resId = resources.getIdentifier(name, "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
    }

}