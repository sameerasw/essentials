package com.sameerasw.essentials.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat

/**
 * Utility helper for creating and managing edge lighting overlays.
 * Provides a single unified implementation for both normal and accessibility service overlays.
 */
object OverlayHelper {

    // Configuration constants
    const val STROKE_DP = 8
    const val CORNER_RADIUS_DP = 20

    /**
     * Creates a rounded rectangle overlay view with stroke.
     *
     * @param context The context to get resources from
     * @param colorResId The color resource ID for the stroke
     * @param strokeDp The stroke width in DP (default: STROKE_DP)
     * @param cornerRadiusDp The corner radius in DP (default: CORNER_RADIUS_DP)
     * @return A FrameLayout with the overlay background drawable
     */
    fun createOverlayView(
        context: Context,
        colorResId: Int,
        strokeDp: Int = STROKE_DP,
        cornerRadiusDp: Int = CORNER_RADIUS_DP
    ): FrameLayout {
        val overlay = FrameLayout(context)
        val color = ContextCompat.getColor(context, colorResId)
        val strokePx = (context.resources.displayMetrics.density * strokeDp).toInt()
        val cornerRadiusPx = (context.resources.displayMetrics.density * cornerRadiusDp).toInt()

        val drawable = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(strokePx, color)
            cornerRadius = cornerRadiusPx.toFloat()
        }

        overlay.background = drawable
        return overlay
    }

    /**
     * Creates WindowManager.LayoutParams configured for an edge lighting overlay.
     *
     * @param overlayType The window type (e.g., TYPE_APPLICATION_OVERLAY, TYPE_ACCESSIBILITY_OVERLAY)
     * @param flags Optional additional flags to combine with default overlay flags
     * @return Configured LayoutParams
     */
    fun createOverlayLayoutParams(
        overlayType: Int,
        flags: Int = 0
    ): WindowManager.LayoutParams {
        val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            baseFlags or flags,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } catch (_: Exception) {}
        }

        return params
    }

    /**
     * Adds an overlay view to the WindowManager.
     *
     * @param windowManager The WindowManager instance
     * @param view The overlay view to add
     * @param params The layout params for the view
     * @return true if successfully added, false otherwise
     */
    fun addOverlayView(
        windowManager: WindowManager?,
        view: View,
        params: WindowManager.LayoutParams
    ): Boolean {
        return try {
            windowManager?.addView(view, params)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Removes an overlay view from the WindowManager.
     *
     * @param windowManager The WindowManager instance
     * @param view The overlay view to remove
     */
    fun removeOverlayView(windowManager: WindowManager?, view: View) {
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {}
    }

    /**
     * Removes all overlay views and clears the list.
     *
     * @param windowManager The WindowManager instance
     * @param overlayViews The list of overlay views to remove
     */
    fun removeAllOverlays(windowManager: WindowManager?, overlayViews: MutableList<View>) {
        try {
            overlayViews.forEach { removeOverlayView(windowManager, it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        overlayViews.clear()
    }
}

