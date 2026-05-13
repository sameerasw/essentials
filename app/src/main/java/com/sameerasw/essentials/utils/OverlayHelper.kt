package com.sameerasw.essentials.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.updateLayoutParams
import androidx.graphics.shapes.toPath
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sameerasw.essentials.domain.model.NotificationLightingSide
import com.sameerasw.essentials.domain.model.NotificationLightingStyle
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * Utility helper for creating and managing notification lighting overlays.
 * Provides a single unified implementation for both normal and accessibility service overlays.
 */
object OverlayHelper {

    // Configuration constants
    const val STROKE_DP = 8
    const val CORNER_RADIUS_DP = 20
    const val INDICATOR_SIZE_DP = 48

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
        strokeDp: Float = STROKE_DP.toFloat(),
        cornerRadiusDp: Float = CORNER_RADIUS_DP.toFloat(),
        style: NotificationLightingStyle = NotificationLightingStyle.STROKE,
        glowSides: Set<NotificationLightingSide> = setOf(
            NotificationLightingSide.LEFT,
            NotificationLightingSide.RIGHT
        ),
        indicatorScale: Float = 1.0f,
        randomShapes: Boolean = false,
        showBackground: Boolean = false
    ): FrameLayout {
        if (style == NotificationLightingStyle.GLOW) {
            return createGlowOverlayView(context, color, glowSides, showBackground)
        }
        if (style == NotificationLightingStyle.INDICATOR) {
            return createIndicatorOverlayView(context, color, indicatorScale, showBackground)
        }
        if (style == NotificationLightingStyle.SWEEP) {
            return createSweepOverlayView(context, color, strokeDp, randomShapes, showBackground)
        }

        val overlay = FrameLayout(context)
        if (showBackground) {
            overlay.setBackgroundColor(Color.BLACK)
        }
        val strokePx = (context.resources.displayMetrics.density * strokeDp).toInt()
        val cornerRadiusPx = (context.resources.displayMetrics.density * cornerRadiusDp).toInt()

        val drawable = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(strokePx, color)
            cornerRadius = cornerRadiusPx.toFloat()
        }

        if (showBackground) {
            val strokeView = View(context).apply {
                background = drawable
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            overlay.addView(strokeView)
        } else {
            overlay.background = drawable
        }

        return overlay
    }

    private fun createGlowOverlayView(
        context: Context,
        color: Int,
        sides: Set<NotificationLightingSide>,
        showBackground: Boolean
    ): FrameLayout {
        val blurRadiusDp = 15f
        val overlay = FrameLayout(context)
        if (showBackground) {
            overlay.setBackgroundColor(Color.BLACK)
        }

        val density = context.resources.displayMetrics.density
        val glowSizePx = (80 * density).toInt()

        if (sides.contains(NotificationLightingSide.LEFT)) {
            val leftGlow =
                GlowSideView(context, color, NotificationLightingSide.LEFT, blurRadiusDp).apply {
                    tag = "left_glow"
                    alpha = 0.5f
                    layoutParams =
                        FrameLayout.LayoutParams(glowSizePx, ViewGroup.LayoutParams.MATCH_PARENT)
                            .apply {
                                gravity = Gravity.START
                            }
                }
            overlay.addView(leftGlow)
        }

        if (sides.contains(NotificationLightingSide.RIGHT)) {
            val rightGlow =
                GlowSideView(context, color, NotificationLightingSide.RIGHT, blurRadiusDp).apply {
                    tag = "right_glow"
                    alpha = 0.5f
                    layoutParams =
                        FrameLayout.LayoutParams(glowSizePx, ViewGroup.LayoutParams.MATCH_PARENT)
                            .apply {
                                gravity = Gravity.END
                            }
                }
            overlay.addView(rightGlow)
        }

        if (sides.contains(NotificationLightingSide.TOP)) {
            val topGlow =
                GlowSideView(context, color, NotificationLightingSide.TOP, blurRadiusDp).apply {
                    tag = "top_glow"
                    alpha = 0.5f
                    layoutParams =
                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, glowSizePx)
                            .apply {
                                gravity = Gravity.TOP
                            }
                }
            overlay.addView(topGlow)
        }

        if (sides.contains(NotificationLightingSide.BOTTOM)) {
            val bottomGlow =
                GlowSideView(context, color, NotificationLightingSide.BOTTOM, blurRadiusDp).apply {
                    tag = "bottom_glow"
                    alpha = 0.5f
                    layoutParams =
                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, glowSizePx)
                            .apply {
                                gravity = Gravity.BOTTOM
                            }
                }
            overlay.addView(bottomGlow)
        }

        return overlay
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun createIndicatorOverlayView(
        context: Context,
        color: Int,
        indicatorScale: Float,
        showBackground: Boolean
    ): FrameLayout {
        // gettign the new LoadingIndicator on an overlay was not easy.... not at all :)
        val overlay = FrameLayout(context)
        if (showBackground) {
            overlay.setBackgroundColor(Color.BLACK)
        }

        // 1. Initialize the fake owners for the ROOT view
        val lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner.onCreate()

        overlay.setViewTreeLifecycleOwner(lifecycleOwner)
        overlay.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        overlay.setViewTreeViewModelStoreOwner(lifecycleOwner)

        val density = context.resources.displayMetrics.density
        val size = (INDICATOR_SIZE_DP * density * indicatorScale).toInt()

        val composeView = ComposeView(context).apply {
            tag = "loading_indicator"
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            }

            // Dispose when removed from window
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

            setContent {
                LoadingIndicator(color = ComposeColor(color))
            }

            this.scaleX = 0f
            this.scaleY = 0f
        }

        overlay.addView(composeView)
        return overlay
    }

    private fun createSweepOverlayView(
        context: Context,
        color: Int,
        strokeDp: Float,
        randomShapes: Boolean,
        showBackground: Boolean
    ): FrameLayout {
        val overlay = FrameLayout(context)
        if (showBackground) {
            overlay.setBackgroundColor(Color.BLACK)
        }

        val sweepView = SweepShapeView(context, color, strokeDp, randomShapes).apply {
            tag = "sweep_view"
            alpha = 0f
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        overlay.addView(sweepView)

        return overlay
    }

    private class SweepShapeView(
        context: Context,
        val color: Int,
        val strokeDp: Float,
        val useRandomShapes: Boolean
    ) : View(context) {
        var centerX: Float = 0f
        var centerY: Float = 0f

        private val polygon = if (useRandomShapes) {
            AmbientMusicShapeHelper.getRandomPolygon()
        } else null

        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            this.color = this@SweepShapeView.color
            strokeWidth = context.resources.displayMetrics.density * strokeDp

            maskFilter = android.graphics.BlurMaskFilter(
                context.resources.displayMetrics.density * 15f,
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }

        init {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }

        var currentRadius: Float = 0f
            set(value) {
                field = value
                invalidate()
            }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)
            if (currentRadius <= 0) return

            if (polygon != null) {
                // Get path from polygon scaled to size
                val shapePath = polygon.toPath()

                // Scale and move path
                val matrix = android.graphics.Matrix()
                // Shapes from toPath() are normalized to [0, 1] range.
                // Scale to currentRadius * 2 and center it.
                matrix.postScale(currentRadius * 2f, currentRadius * 2f)
                matrix.postTranslate(centerX - currentRadius, centerY - currentRadius)

                shapePath.transform(matrix)
                canvas.drawPath(shapePath, paint)
            } else {
                canvas.drawCircle(centerX, centerY, currentRadius, paint)
            }
        }
    }

    private class GlowSideView(
        context: Context,
        val color: Int,
        val side: NotificationLightingSide,
        val blurRadiusDp: Float
    ) : View(context) {
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.FILL
            if (blurRadiusDp > 0) {
                maskFilter = android.graphics.BlurMaskFilter(
                    context.resources.displayMetrics.density * blurRadiusDp,
                    android.graphics.BlurMaskFilter.Blur.NORMAL
                )
            }
        }

        init {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }

        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()

            // Define the gradient based on side
            val shader = when (side) {
                NotificationLightingSide.LEFT -> android.graphics.LinearGradient(
                    0f, 0f, w, 0f,
                    color, Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP
                )

                NotificationLightingSide.RIGHT -> android.graphics.LinearGradient(
                    w, 0f, 0f, 0f,
                    color, Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP
                )

                NotificationLightingSide.TOP -> android.graphics.LinearGradient(
                    0f, 0f, 0f, h,
                    color, Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP
                )

                NotificationLightingSide.BOTTOM -> android.graphics.LinearGradient(
                    0f, h, 0f, 0f,
                    color, Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP
                )
            }
            paint.shader = shader

            canvas.drawRect(0f, 0f, w, h, paint)
        }
    }

    /**
     * A lightweight implementation of the owners required by Jetpack Compose
     * to run inside a WindowManager overlay.
     */
    private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner,
        ViewModelStoreOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry =
            savedStateRegistryController.savedStateRegistry
        override val viewModelStore: ViewModelStore = store

        fun onCreate() {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun onDestroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        }
    }


    /**
     * Creates WindowManager.LayoutParams configured for an notification lighting overlay.
     *
     * @param overlayType The window type (e.g., TYPE_APPLICATION_OVERLAY, TYPE_ACCESSIBILITY_OVERLAY)
     * @param flags Optional additional flags to combine with default overlay flags
     * @return Configured LayoutParams
     */
    fun createOverlayLayoutParams(
        overlayType: Int,
        flags: Int = 0,
        isTouchable: Boolean = false
    ): WindowManager.LayoutParams {
        var baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD

        if (!isTouchable) {
            baseFlags = baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

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
                params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } catch (_: Exception) {
            }
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
        } catch (_: Exception) {
        }
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
        style: NotificationLightingStyle,
        strokeWidthDp: Float,
        indicatorX: Float = 50f,
        indicatorY: Float = 2f,
        indicatorScale: Float = 1.0f,
        randomShapes: Boolean = false,
        pulseDurationMillis: Long = 3000L,
        onAnimationEnd: (() -> Unit)? = null
    ) {
        if (style == NotificationLightingStyle.GLOW) {
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
        } else if (style == NotificationLightingStyle.INDICATOR) {
            view.alpha = 1f
            val indicator = view.findViewWithTag<View>("loading_indicator")
            indicator?.apply {
                scaleX = 1f
                scaleY = 1f

                // Position based on percentages
                val parentWidth = view.resources.displayMetrics.widthPixels
                val parentHeight = view.resources.displayMetrics.heightPixels

                translationX = (parentWidth * (indicatorX / 100f)) - (parentWidth / 2f)
                translationY = (parentHeight * (indicatorY / 100f)) - (parentHeight / 2f)
                scaleX = indicatorScale
                scaleY = indicatorScale
            }
        } else if (style == NotificationLightingStyle.SWEEP) {
            pulseSweepOverlay(
                view as ViewGroup,
                maxPulses = 1,
                pulseDurationMillis = pulseDurationMillis,
                strokeWidthDp = strokeWidthDp,
                sweepPositionX = indicatorX,
                onAnimationEnd = onAnimationEnd
            )
        }

        fadeInOverlay(view, onAnimationEnd)
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
        style: NotificationLightingStyle = NotificationLightingStyle.STROKE,
        strokeWidthDp: Float = STROKE_DP.toFloat(),
        indicatorX: Float = 50f,
        indicatorY: Float = 2f,
        indicatorScale: Float = 1.0f,
        randomShapes: Boolean = false,
        onAnimationEnd: (() -> Unit)? = null
    ) {
        if (style == NotificationLightingStyle.GLOW) {
            pulseGlowOverlay(
                view as ViewGroup,
                maxPulses,
                pulseDurationMillis,
                strokeWidthDp,
                onAnimationEnd
            )
            return
        }

        if (style == NotificationLightingStyle.INDICATOR) {
            pulseIndicatorOverlay(
                view as ViewGroup,
                pulseDurationMillis,
                indicatorX,
                indicatorY,
                indicatorScale,
                onAnimationEnd
            )
            return
        }

        if (style == NotificationLightingStyle.SWEEP) {
            pulseSweepOverlay(
                view as ViewGroup,
                maxPulses,
                pulseDurationMillis,
                strokeWidthDp,
                indicatorX,
                onAnimationEnd
            )
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
        strokeWidthDp: Float,
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

    private fun pulseIndicatorOverlay(
        view: ViewGroup,
        durationMillis: Long,
        indicatorX: Float,
        indicatorY: Float,
        indicatorScale: Float,
        onAnimationEnd: (() -> Unit)? = null
    ) {
        val indicator = view.findViewWithTag<View>("loading_indicator") ?: return

        val parentWidth = view.resources.displayMetrics.widthPixels
        val parentHeight = view.resources.displayMetrics.heightPixels

        indicator.translationX = (parentWidth * (indicatorX / 100f)) - (parentWidth / 2f)
        indicator.translationY = (parentHeight * (indicatorY / 100f)) - (parentHeight / 2f)

        view.alpha = 1f

        indicator.animate()
            .scaleX(indicatorScale)
            .scaleY(indicatorScale)
            .setDuration(400) // Slightly slower for the morphing effect to catch the eye
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.postDelayed({
                        indicator.animate()
                            .scaleX(0.0f)
                            .scaleY(0.0f)
                            .setDuration(400)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    onAnimationEnd?.invoke()
                                }
                            }).start()
                    }, (durationMillis - 800).coerceAtLeast(0))
                }
            }).start()
    }

    private fun pulseSweepOverlay(
        view: ViewGroup,
        maxPulses: Int,
        pulseDurationMillis: Long,
        strokeWidthDp: Float,
        sweepPositionX: Float,
        onAnimationEnd: (() -> Unit)? = null
    ) {
        val sweepView = view.findViewWithTag<View>("sweep_view") as? SweepShapeView ?: return
        val displayMetrics = view.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val startX = when {
            sweepPositionX < 34f -> 0f
            sweepPositionX > 66f -> screenWidth.toFloat()
            else -> screenWidth / 2f
        }
        val startY = 16f * displayMetrics.density // top gap

        sweepView.centerX = startX
        sweepView.centerY = startY

        // Max radius to cover the whole screen from the start point
        val maxDistX = Math.max(startX, screenWidth - startX)
        val maxDistY = Math.max(startY, screenHeight - startY)
        val maxRadius = Math.sqrt((maxDistX * maxDistX + maxDistY * maxDistY).toDouble())
            .toFloat() + (15f * displayMetrics.density)

        var pulseCount = 0

        fun startPulse() {
            if (pulseCount >= maxPulses) {
                onAnimationEnd?.invoke()
                return
            }
            pulseCount++

            sweepView.alpha = 1f
            sweepView.currentRadius = 0f

            val animator = ValueAnimator.ofFloat(0f, maxRadius).apply {
                duration = pulseDurationMillis
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { anim ->
                    val radius = anim.animatedValue as Float
                    sweepView.currentRadius = radius
                    sweepView.alpha = 1f - (radius / maxRadius)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        startPulse()
                    }
                })
            }
            animator.start()
        }

        startPulse()
    }
}
