package com.sameerasw.essentials.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.view.animation.AccelerateDecelerateInterpolator
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.sameerasw.essentials.domain.model.EdgeLightingStyle
import com.sameerasw.essentials.domain.model.EdgeLightingSide

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
        cornerRadiusDp: Int = CORNER_RADIUS_DP,
        style: EdgeLightingStyle = EdgeLightingStyle.STROKE,
        glowSides: Set<EdgeLightingSide> = setOf(EdgeLightingSide.LEFT, EdgeLightingSide.RIGHT)
    ): FrameLayout {
        if (style == EdgeLightingStyle.GLOW) {
            return createGlowOverlayView(context, color, glowSides)
        }

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

    private fun createGlowOverlayView(context: Context, color: Int, sides: Set<EdgeLightingSide>): FrameLayout {
        val overlay = FrameLayout(context)
        
        if (sides.contains(EdgeLightingSide.LEFT)) {
            val leftGlow = View(context).apply {
                tag = "left_glow"
                alpha = 0.5f
                layoutParams = FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                    gravity = Gravity.START
                }
                background = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(color, Color.TRANSPARENT)
                )
            }
            overlay.addView(leftGlow)
        }

        if (sides.contains(EdgeLightingSide.RIGHT)) {
            val rightGlow = View(context).apply {
                tag = "right_glow"
                alpha = 0.5f
                layoutParams = FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                    gravity = Gravity.END
                }
                background = GradientDrawable(
                    GradientDrawable.Orientation.RIGHT_LEFT,
                    intArrayOf(color, Color.TRANSPARENT)
                )
            }
            overlay.addView(rightGlow)
        }

        if (sides.contains(EdgeLightingSide.TOP)) {
            val topGlow = View(context).apply {
                tag = "top_glow"
                alpha = 0.5f
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0).apply {
                    gravity = Gravity.TOP
                }
                background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(color, Color.TRANSPARENT)
                )
            }
            overlay.addView(topGlow)
        }

        if (sides.contains(EdgeLightingSide.BOTTOM)) {
            val bottomGlow = View(context).apply {
                tag = "bottom_glow"
                alpha = 0.5f
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0).apply {
                    gravity = Gravity.BOTTOM
                }
                background = GradientDrawable(
                    GradientDrawable.Orientation.BOTTOM_TOP,
                    intArrayOf(color, Color.TRANSPARENT)
                )
            }
            overlay.addView(bottomGlow)
        }

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
     * Shows the overlay in preview mode.
     * For GLOW style, this expands the glow to the full spread immediately.
     * For STROKE style or others, it just fades in.
     */
    fun showPreview(
        view: View, 
        style: EdgeLightingStyle, 
        strokeWidthDp: Int
    ) {
        if (style == EdgeLightingStyle.GLOW) {
            val vg = view as? ViewGroup
            if (vg != null) {
                // Calculate max pixels using same logic as pulseGlowOverlay
                val density = view.resources.displayMetrics.density
                val maxPixels = (strokeWidthDp * density * 12).toInt()
                
                // Force views to max expansion
                vg.findViewWithTag<View>("left_glow")?.updateLayoutParams { width = maxPixels }
                vg.findViewWithTag<View>("right_glow")?.updateLayoutParams { width = maxPixels }
                vg.findViewWithTag<View>("top_glow")?.updateLayoutParams { height = maxPixels }
                vg.findViewWithTag<View>("bottom_glow")?.updateLayoutParams { height = maxPixels }
            }
        }
        
        fadeInOverlay(view)
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
        style: EdgeLightingStyle = EdgeLightingStyle.STROKE,
        strokeWidthDp: Int = STROKE_DP,
        onAnimationEnd: (() -> Unit)? = null
    ) {
        if (style == EdgeLightingStyle.GLOW) {
            pulseGlowOverlay(view as ViewGroup, maxPulses, pulseDurationMillis, strokeWidthDp, onAnimationEnd)
            return
        }

        var pulseCount = 0
        
        val durationIn = (pulseDurationMillis * 0.1).toLong()
        val durationHold = (pulseDurationMillis * 0.4).toLong()
        val durationOut = (pulseDurationMillis * 0.5).toLong()

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

    private fun pulseGlowOverlay(
        view: ViewGroup,
        maxPulses: Int,
        pulseDurationMillis: Long,
        strokeWidthDp: Int,
        onAnimationEnd: (() -> Unit)?
    ) {
        val leftGlow = view.findViewWithTag<View>("left_glow")
        val rightGlow = view.findViewWithTag<View>("right_glow")
        val topGlow = view.findViewWithTag<View>("top_glow")
        val bottomGlow = view.findViewWithTag<View>("bottom_glow")

        val density = view.resources.displayMetrics.density
        val maxPixels = (strokeWidthDp * density * 12).toInt()
        
        var pulseCount = 0

        val expandDuration = (pulseDurationMillis * 0.1).toLong()
        val holdDuration = (pulseDurationMillis * 0.4).toLong()
        val shrinkDuration = (pulseDurationMillis * 0.5).toLong()

        fun startPulse() {
            if (pulseCount >= maxPulses) {
                onAnimationEnd?.invoke()
                return
            }
            pulseCount++

            // Expand
            val expandAnimator = ValueAnimator.ofInt(0, maxPixels).apply {
                duration = expandDuration
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    val dim = animator.animatedValue as Int
                    leftGlow?.updateLayoutParams { this.width = dim }
                    rightGlow?.updateLayoutParams { this.width = dim }
                    topGlow?.updateLayoutParams { this.height = dim }
                    bottomGlow?.updateLayoutParams { this.height = dim }
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // Hold
                        view.postDelayed({
                            // Shrink
                            val shrinkAnimator = ValueAnimator.ofInt(maxPixels, 0).apply {
                                duration = shrinkDuration
                                interpolator = AccelerateDecelerateInterpolator()
                                addUpdateListener { animator ->
                                    val dim = animator.animatedValue as Int
                                    leftGlow?.updateLayoutParams { this.width = dim }
                                    rightGlow?.updateLayoutParams { this.width = dim }
                                    topGlow?.updateLayoutParams { this.height = dim }
                                    bottomGlow?.updateLayoutParams { this.height = dim }
                                }
                                addListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        startPulse()
                                    }
                                })
                            }
                            shrinkAnimator.start()
                        }, holdDuration)
                    }
                })
            }
            expandAnimator.start()
        }

        startPulse()
    }
}
