package com.sameerasw.essentials.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
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
     * @param color The color integer for the stroke
     * @param strokeDp The stroke width in DP (default: STROKE_DP)
     * @param cornerRadiusDp The corner radius in DP (default: CORNER_RADIUS_DP)
     * @return A FrameLayout with the overlay background drawable
     */
    fun createOverlayView(
        context: Context,
        color: Int,
        strokeDp: Int = STROKE_DP,
        cornerRadiusDp: Int = CORNER_RADIUS_DP
    ): FrameLayout {
        val overlay = FrameLayout(context)
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

    /**
     * Animates the overlay view to fade in over 1 second.
     *
     * @param view The overlay view to animate
     * @param onAnimationEnd Optional callback when animation completes
     */
    fun fadeInOverlay(view: View, onAnimationEnd: (() -> Unit)? = null) {
        view.alpha = 0f
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            duration = 1000 // 1 second
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onAnimationEnd?.invoke()
                }
            })
            start()
        }
    }

    /**
     * Animates the overlay view to fade out over 1 second, then removes it from WindowManager.
     *
     * @param windowManager The WindowManager instance
     * @param view The overlay view to animate and remove
     * @param overlayViews The list to remove the view from
     * @param onAnimationEnd Optional callback when animation completes
     */
    fun fadeOutAndRemoveOverlay(
        windowManager: WindowManager?,
        view: View,
        overlayViews: MutableList<View>,
        onAnimationEnd: (() -> Unit)? = null
    ) {
        ObjectAnimator.ofFloat(view, "alpha", view.alpha, 0f).apply {
            duration = 1000 // 1 second
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    removeOverlayView(windowManager, view)
                    overlayViews.remove(view)
                    onAnimationEnd?.invoke()
                }
            })
            start()
        }
    }

    /**
     * Animates the overlay with a pulsing effect.
     *
     * @param view The overlay view to animate
     * @param maxPulses Number of times to pulse (default: 3)
     * @param pulseDurationMillis Total duration of one pulse cycle in ms (default: 3000)
     * @param onAnimationEnd Optional callback when the complete pulsing sequence ends
     */
    fun pulseOverlay(
        view: View,
        maxPulses: Int = 3,
        pulseDurationMillis: Long = 3000,
        onAnimationEnd: (() -> Unit)? = null
    ) {
        var pulseCount = 0
        
        // Calculate durations proportionally
        // Fade In: 10%
        // Hold: 30%
        // Fade Out: 60%
        val durationIn = (pulseDurationMillis * 0.1).toLong()
        val durationHold = (pulseDurationMillis * 0.3).toLong()
        val durationOut = (pulseDurationMillis * 0.6).toLong()

        fun startPulse() {
            if (pulseCount >= maxPulses) {
                onAnimationEnd?.invoke()
                return
            }

            pulseCount++

            // Fade in
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                duration = durationIn
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // Stay visible for hold duration, then fade out
                        view.postDelayed({
                            // Fade out
                            ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
                                duration = durationOut
                                addListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        // Start next pulse immediately
                                        startPulse()
                                    }
                                })
                                start()
                            }
                        }, durationHold)
                    }
                })
                start()
            }
        }

        startPulse()
    }
}
