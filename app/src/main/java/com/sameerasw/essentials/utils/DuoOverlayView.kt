/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays
 * File: DuoOverlayView.kt
 * Description: Ambient camera ring and dots overlay view with media playback seekbar support.
 */

package com.sameerasw.essentials.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.os.Build
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import kotlin.math.cos
import kotlin.math.sin

enum class DuoFeedbackType {
    NONE,
    SCREENSHOT,
    LOCK_SCREEN,
    NOTIFICATIONS,
    QUICK_SETTINGS,
    MEDIA_PLAY,
    MEDIA_PAUSE,
    TRACK_NEXT,
    TRACK_PREV,
    TORCH_ON,
    TORCH_OFF,
    RECENTS
}

class DuoOverlayView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density

    var cameraCenterX: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var cameraCenterY: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var cameraRadiusPx: Float = 36f
        set(value) {
            field = value
            invalidate()
        }

    var ringRadiusScale: Float = 1.0f
        set(value) {
            field = value
            invalidate()
        }

    var arcThicknessPx: Float = 10f
        set(value) {
            field = value
            trackPaint.strokeWidth = value
            progressPaint.strokeWidth = value
            invalidate()
        }

    var dotRadiusPx: Float = 5f
        set(value) {
            field = value
            invalidate()
        }

    var batteryLevel: Int = 100
        set(value) {
            val clamped = value.coerceIn(0, 100)
            field = clamped
            if (!isCustomProgressActive()) {
                updateProgressAnimation(clamped.toFloat())
            }
        }

    var isDarkTheme: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                mediaAppIcon?.let {
                    if (isMediaPlaying) {
                        mediaPaletteColors = extractMediaColors(it)
                    }
                }
                animateThemeChange()
            }
        }

    var isScreenOff: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityAnimation()
            }
        }

    var isFullscreen: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityAnimation()
            }
        }

    var hideWhenScreenOff: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityAnimation()
            }
        }

    var hideWhenScreenOffOnlyIdle: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityAnimation()
            }
        }

    var showMedia: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateActiveProgressMode()
            }
        }

    var showProgress: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateActiveProgressMode()
            }
        }

    var showFlashlight: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateActiveProgressMode()
            }
        }

    private fun updateVisibilityAnimation() {
        val isScreenOffHiding = if (hideWhenScreenOff) {
            if (hideWhenScreenOffOnlyIdle) {
                !isCustomProgressActive()
            } else {
                true
            }
        } else {
            false
        }
        val shouldHide = isFullscreen || (isScreenOff && isScreenOffHiding)
        if (shouldHide) {
            animateScreenOffVisibility(false)
        } else {
            animateScreenOffVisibility(true)
            animateThemeChange()
        }
    }

    var useMaterialYouColors: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                animateThemeChange()
            }
        }

    var showNetworks: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                animateLayoutChange()
            }
        }

    var signalLevel: Int = 4
        set(value) {
            val clamped = value.coerceIn(0, 4)
            if (field != clamped) {
                field = clamped
                animateSignalLevelChange(clamped.toFloat())
            }
        }

    var isMediaPlaying: Boolean = false
        private set

    var mediaAppIcon: Bitmap? = null
        private set

    var mediaProgress: Float = 0f
        private set

    var isProgressNotificationActive: Boolean = false
        private set

    var progressNotificationIcon: Bitmap? = null
        private set

    var progressNotificationProgress: Float = 0f
        private set

    var isFlashlightOn: Boolean = false
        private set

    var flashlightIcon: Bitmap? = null
        private set

    var flashlightBrightnessProgress: Float = 100f
        private set

    private var touchBounceAnimator: ValueAnimator? = null
    private var mediaPaletteColors: Pair<Int, Int>? = null

    private fun extractMediaColors(bitmap: Bitmap): Pair<Int, Int> {
        val palette = try {
            Palette.from(bitmap)
                .maximumColorCount(24)
                .generate()
        } catch (_: Exception) {
            null
        }

        val dominantSwatch = palette?.dominantSwatch
        val vibrantSwatch = palette?.vibrantSwatch
        val lightVibrantSwatch = palette?.lightVibrantSwatch
        val darkVibrantSwatch = palette?.darkVibrantSwatch
        val mutedSwatch = palette?.mutedSwatch
        val lightMutedSwatch = palette?.lightMutedSwatch
        val darkMutedSwatch = palette?.darkMutedSwatch

        val rawAccent = if (isDarkTheme) {
            vibrantSwatch?.rgb
                ?: lightVibrantSwatch?.rgb
                ?: dominantSwatch?.rgb
                ?: mutedSwatch?.rgb
                ?: lightMutedSwatch?.rgb
                ?: Color.WHITE
        } else {
            darkVibrantSwatch?.rgb
                ?: vibrantSwatch?.rgb
                ?: dominantSwatch?.rgb
                ?: darkMutedSwatch?.rgb
                ?: mutedSwatch?.rgb
                ?: Color.BLACK
        }

        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(rawAccent, hsv)
        if (isDarkTheme) {
            hsv[1] = hsv[1].coerceIn(0.4f, 0.95f)
            hsv[2] = hsv[2].coerceIn(0.7f, 1.0f)
        } else {
            hsv[1] = hsv[1].coerceIn(0.5f, 1.0f)
            hsv[2] = hsv[2].coerceIn(0.2f, 0.65f)
        }
        val accent = android.graphics.Color.HSVToColor(hsv)

        val trackAlpha = if (isDarkTheme) 90 else 110
        val track = Color.argb(trackAlpha, Color.red(accent), Color.green(accent), Color.blue(accent))
        return Pair(track, accent)
    }

    private fun isCustomProgressActive(): Boolean {
        return (isFlashlightOn && showFlashlight) ||
            (isMediaPlaying && showMedia) ||
            (isProgressNotificationActive && showProgress)
    }

    private fun getCurrentCustomProgress(): Float {
        return when {
            isFlashlightOn && showFlashlight -> flashlightBrightnessProgress
            isMediaPlaying && showMedia -> mediaProgress
            else -> progressNotificationProgress
        }
    }

    private fun getCurrentCustomIcon(): Bitmap? {
        return when {
            isFlashlightOn && showFlashlight -> flashlightIcon
            isMediaPlaying && showMedia -> mediaAppIcon
            else -> progressNotificationIcon
        }
    }

    private fun updateActiveProgressMode() {
        val wasActive = animatedCustomFraction > 0.5f
        val isNowActive = isCustomProgressActive()
        if (isMediaPlaying && showMedia && !isFlashlightOn && mediaAppIcon != null) {
            mediaPaletteColors = extractMediaColors(mediaAppIcon!!)
        } else {
            mediaPaletteColors = null
        }
        animateThemeChange()
        if (wasActive != isNowActive) {
            animateLayoutChange()
            if (isScreenOff && hideWhenScreenOff && hideWhenScreenOffOnlyIdle) {
                updateVisibilityAnimation()
            }
        }
        updateProgressAnimation()
    }

    fun setMediaState(isPlaying: Boolean, progress: Float, appIcon: Bitmap?) {
        val wasActive = isCustomProgressActive()
        val iconChanged = mediaAppIcon != appIcon
        isMediaPlaying = isPlaying
        if (appIcon != null || !isPlaying) {
            mediaAppIcon = appIcon
        }
        mediaProgress = progress.coerceIn(0f, 100f)

        if (isPlaying && showMedia && !isFlashlightOn && appIcon != null && (iconChanged || mediaPaletteColors == null)) {
            mediaPaletteColors = extractMediaColors(appIcon)
            animateThemeChange()
        } else if ((!isPlaying || !showMedia || isFlashlightOn) && mediaPaletteColors != null) {
            mediaPaletteColors = null
            animateThemeChange()
        }

        val isNowActive = isCustomProgressActive()
        if (wasActive != isNowActive) {
            animateLayoutChange()
            if (isScreenOff && hideWhenScreenOff && hideWhenScreenOffOnlyIdle) {
                updateVisibilityAnimation()
            }
        }
        updateProgressAnimation()
    }

    fun setProgressNotificationState(isActive: Boolean, progress: Float, icon: Bitmap?) {
        val wasActive = isCustomProgressActive()
        isProgressNotificationActive = isActive
        if (icon != null || !isActive) {
            progressNotificationIcon = icon
        }
        progressNotificationProgress = progress.coerceIn(0f, 100f)

        if (!isMediaPlaying || !showMedia || isFlashlightOn) {
            if (mediaPaletteColors != null) {
                mediaPaletteColors = null
                animateThemeChange()
            }
        }

        val isNowActive = isCustomProgressActive()
        if (wasActive != isNowActive) {
            animateLayoutChange()
            if (isScreenOff && hideWhenScreenOff && hideWhenScreenOffOnlyIdle) {
                updateVisibilityAnimation()
            }
        }
        updateProgressAnimation()
    }

    fun setFlashlightState(isOn: Boolean, brightnessProgress: Float, icon: Bitmap?) {
        val wasActive = isCustomProgressActive()
        val stateChanged = isFlashlightOn != isOn
        isFlashlightOn = isOn
        if (icon != null || !isOn) {
            flashlightIcon = icon
        }
        val clampedProgress = brightnessProgress.coerceIn(0f, 100f)
        val progressChanged = kotlin.math.abs(flashlightBrightnessProgress - clampedProgress) > 0.1f
        flashlightBrightnessProgress = clampedProgress

        if (stateChanged) {
            if (isOn && showFlashlight) {
                if (mediaPaletteColors != null) {
                    mediaPaletteColors = null
                    animateThemeChange()
                }
            } else if (isMediaPlaying && showMedia && mediaAppIcon != null) {
                mediaPaletteColors = extractMediaColors(mediaAppIcon!!)
                animateThemeChange()
            }
        }

        val isNowActive = isCustomProgressActive()
        if (wasActive != isNowActive) {
            animateLayoutChange()
            if (isScreenOff && hideWhenScreenOff && hideWhenScreenOffOnlyIdle) {
                updateVisibilityAnimation()
            }
        }
        if (progressChanged || stateChanged) {
            updateProgressAnimation()
        }
    }

    fun triggerTouchBounce() {
        touchBounceAnimator?.cancel()
        touchBounceAnimator = ValueAnimator.ofFloat(1.0f, 0.93f, 1.05f, 1.0f).apply {
            duration = 260
            interpolator = OvershootInterpolator(1.4f)
            addUpdateListener { animator ->
                animatedScaleBounce = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private var activeFeedbackType: DuoFeedbackType = DuoFeedbackType.NONE
    private var feedbackProgress: Float = 0f
    private var feedbackAnimator: ValueAnimator? = null

    private var chargeProgress: Float = 0f
    private var chargeAnimator: ValueAnimator? = null

    private var isRotaryVisible: Boolean = false
    private var rotaryLevel: Float = 0f
    private var rotaryAlpha: Float = 0f
    private var rotaryAnimator: ValueAnimator? = null
    private val rotaryFadeRunnable = Runnable { fadeOutRotary() }

    fun triggerActionFeedback(type: DuoFeedbackType) {
        if (type == DuoFeedbackType.NONE) return
        feedbackAnimator?.cancel()
        activeFeedbackType = type
        feedbackProgress = 0f

        val targetDuration: Long = when (type) {
            DuoFeedbackType.SCREENSHOT -> 350L
            DuoFeedbackType.LOCK_SCREEN -> 320L
            DuoFeedbackType.NOTIFICATIONS -> 380L
            DuoFeedbackType.QUICK_SETTINGS -> 300L
            DuoFeedbackType.MEDIA_PLAY, DuoFeedbackType.MEDIA_PAUSE -> 360L
            DuoFeedbackType.TRACK_NEXT, DuoFeedbackType.TRACK_PREV -> 320L
            DuoFeedbackType.TORCH_ON -> 380L
            DuoFeedbackType.TORCH_OFF -> 260L
            DuoFeedbackType.RECENTS -> 320L
            DuoFeedbackType.NONE -> return
        }

        val interp = when (type) {
            DuoFeedbackType.LOCK_SCREEN -> OvershootInterpolator(1.8f)
            DuoFeedbackType.TORCH_ON -> OvershootInterpolator(1.3f)
            DuoFeedbackType.SCREENSHOT -> AccelerateDecelerateInterpolator()
            DuoFeedbackType.TORCH_OFF -> AccelerateInterpolator()
            else -> DecelerateInterpolator()
        }

        feedbackAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = targetDuration
            interpolator = interp
            addUpdateListener { animator ->
                feedbackProgress = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    activeFeedbackType = DuoFeedbackType.NONE
                    feedbackProgress = 0f
                    invalidate()
                }
            })
            start()
        }
    }

    fun setChargeProgress(progress: Float) {
        chargeAnimator?.cancel()
        if (progress <= 0f && chargeProgress > 0f) {
            chargeAnimator = ValueAnimator.ofFloat(chargeProgress, 0f).apply {
                duration = 180L
                interpolator = DecelerateInterpolator()
                addUpdateListener { animator ->
                    chargeProgress = animator.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            chargeProgress = progress.coerceIn(0f, 1f)
            invalidate()
        }
    }

    fun showRotaryLevel(level: Float, isVolume: Boolean = true) {
        rotaryLevel = level.coerceIn(0f, 1f)
        removeCallbacks(rotaryFadeRunnable)
        if (!isRotaryVisible || rotaryAlpha < 0.99f) {
            rotaryAnimator?.cancel()
            isRotaryVisible = true
            rotaryAnimator = ValueAnimator.ofFloat(rotaryAlpha, 1.0f).apply {
                duration = 150L
                interpolator = DecelerateInterpolator()
                addUpdateListener { animator ->
                    rotaryAlpha = animator.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            invalidate()
        }
        postDelayed(rotaryFadeRunnable, 1200L)
    }

    private fun fadeOutRotary() {
        rotaryAnimator?.cancel()
        rotaryAnimator = ValueAnimator.ofFloat(rotaryAlpha, 0f).apply {
            duration = 250L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                rotaryAlpha = animator.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isRotaryVisible = false
                    rotaryAlpha = 0f
                    invalidate()
                }
            })
            start()
        }
    }

    private var targetProgress: Float = 100f
    private var animatedProgress: Float = 100f
    private var progressAnimator: ValueAnimator? = null

    private var animatedSignalLevel: Float = 4f
    private var signalAnimator: ValueAnimator? = null

    private var animatedStartAngle: Float = 140f
    private var animatedTotalSweep: Float = 260f
    private var animatedDotAlpha: Float = 1.0f
    private var animatedCustomFraction: Float = 0.0f
    private var animatedScaleBounce: Float = 1.0f
    private var layoutAnimator: ValueAnimator? = null
    private var scaleAnimator: ValueAnimator? = null

    private var animatedVisibilityAlpha: Float = 1.0f
    private var animatedVisibilityScale: Float = 1.0f
    private var animatedVisibilityRotation: Float = 0f
    private var visibilityAnimator: ValueAnimator? = null

    private var currentTrackColor: Int = Color.argb(60, 255, 255, 255)
    private var currentProgressColor: Int = Color.WHITE
    private var currentDotBaseColor: Int = Color.WHITE
    private var themeAnimator: ValueAnimator? = null

    private fun animateScreenOffVisibility(visible: Boolean) {
        visibilityAnimator?.cancel()
        val startAlpha = animatedVisibilityAlpha
        val targetAlpha = if (visible) 1.0f else 0.0f

        val startScale = animatedVisibilityScale
        val targetScale = if (visible) 1.0f else 0.35f

        val startRotation = animatedVisibilityRotation
        val targetRotation = if (visible) 0f else -65f

        visibilityAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 750
            interpolator = if (visible) {
                OvershootInterpolator(1.15f)
            } else {
                DecelerateInterpolator()
            }
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                animatedVisibilityAlpha = (startAlpha + (targetAlpha - startAlpha) * fraction).coerceIn(0f, 1f)
                animatedVisibilityScale = (startScale + (targetScale - startScale) * fraction).coerceAtLeast(0.01f)
                animatedVisibilityRotation = startRotation + (targetRotation - startRotation) * fraction
                invalidate()
            }
            start()
        }
    }

    private fun getTargetColors(): Triple<Int, Int, Int> {
        if (isScreenOff) {
            if (isMediaPlaying && showMedia && mediaPaletteColors != null) {
                val (_, accent) = mediaPaletteColors!!
                val dimProgress = Color.argb(160, Color.red(accent), Color.green(accent), Color.blue(accent))
                val dimTrack = Color.argb(50, Color.red(accent), Color.green(accent), Color.blue(accent))
                return Triple(dimTrack, dimProgress, dimProgress)
            }
            return Triple(
                Color.argb(40, 255, 255, 255),
                Color.argb(128, 255, 255, 255),
                Color.argb(128, 255, 255, 255)
            )
        }

        if (isMediaPlaying && showMedia && mediaPaletteColors != null) {
            val (track, progress) = mediaPaletteColors!!
            return Triple(track, progress, progress)
        }

        if (useMaterialYouColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return if (isDarkTheme) {
                val accent = ContextCompat.getColor(context, android.R.color.system_accent1_200)
                val track = ContextCompat.getColor(context, android.R.color.system_neutral1_800)
                val trackWithAlpha = Color.argb(90, Color.red(track), Color.green(track), Color.blue(track))
                Triple(trackWithAlpha, accent, accent)
            } else {
                val accent = ContextCompat.getColor(context, android.R.color.system_accent1_600)
                val track = ContextCompat.getColor(context, android.R.color.system_neutral1_200)
                val trackWithAlpha = Color.argb(110, Color.red(track), Color.green(track), Color.blue(track))
                Triple(trackWithAlpha, accent, accent)
            }
        }

        return if (isDarkTheme) {
            Triple(
                Color.argb(60, 255, 255, 255),
                Color.WHITE,
                Color.WHITE
            )
        } else {
            Triple(
                Color.argb(60, 0, 0, 0),
                Color.BLACK,
                Color.BLACK
            )
        }
    }

    private fun animateThemeChange() {
        themeAnimator?.cancel()
        val (targetTrack, targetProgress, targetDot) = getTargetColors()
        val startTrack = currentTrackColor
        val startProgress = currentProgressColor
        val startDot = currentDotBaseColor
        val evaluator = ArgbEvaluator()

        themeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                currentTrackColor = evaluator.evaluate(fraction, startTrack, targetTrack) as Int
                currentProgressColor = evaluator.evaluate(fraction, startProgress, targetProgress) as Int
                currentDotBaseColor = evaluator.evaluate(fraction, startDot, targetDot) as Int
                trackPaint.color = currentTrackColor
                progressPaint.color = currentProgressColor
                invalidate()
            }
            start()
        }
    }

    private fun animateSignalLevelChange(targetLevel: Float) {
        signalAnimator?.cancel()
        signalAnimator = ValueAnimator.ofFloat(animatedSignalLevel, targetLevel).apply {
            duration = 600
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener { animation ->
                animatedSignalLevel = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun getActiveTargetProgress(): Float {
        return if (isCustomProgressActive()) {
            getCurrentCustomProgress()
        } else {
            batteryLevel.toFloat()
        }
    }

    private fun updateProgressAnimation(target: Float = getActiveTargetProgress()) {
        targetProgress = target
        if (kotlin.math.abs(animatedProgress - targetProgress) < 0.05f) {
            animatedProgress = targetProgress
            invalidate()
            return
        }
        val startVal = animatedProgress
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(startVal, targetProgress).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                animatedProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateLayoutChange() {
        layoutAnimator?.cancel()
        scaleAnimator?.cancel()

        val isCustom = isCustomProgressActive()
        val targetCustomFraction = if (isCustom) 1.0f else 0.0f
        val targetStartAngle = if (isCustom) 120f else (if (showNetworks) 140f else -90f)
        val targetTotalSweep = if (isCustom) 300f else (if (showNetworks) 260f else 360f)
        val targetDotAlpha = if (isCustom) 0.0f else (if (showNetworks) 1.0f else 0.0f)

        val startStartAngle = animatedStartAngle
        val startTotalSweep = animatedTotalSweep
        val startDotAlpha = animatedDotAlpha
        val startCustomFraction = animatedCustomFraction

        layoutAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 650
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                animatedStartAngle = startStartAngle + (targetStartAngle - startStartAngle) * fraction
                animatedTotalSweep = startTotalSweep + (targetTotalSweep - startTotalSweep) * fraction
                animatedDotAlpha = (startDotAlpha + (targetDotAlpha - startDotAlpha) * fraction).coerceIn(0f, 1f)
                animatedCustomFraction = (startCustomFraction + (targetCustomFraction - startCustomFraction) * fraction).coerceIn(0f, 1f)
                invalidate()
            }
            start()
        }

        scaleAnimator = ValueAnimator.ofFloat(1.0f, 1.03f, 1.0f).apply {
            duration = 650
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener { animation ->
                animatedScaleBounce = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val iconClipPath = Path()
    private val iconRect = RectF()
    private val arcBounds = RectF()

    private val feedbackStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val feedbackFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val feedbackBounds = RectF()
    private val teardropPath = Path()
    private val chevronPath = Path()

    private val chargePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val chargeBounds = RectF()

    private val rotaryTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val rotaryProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val rotaryTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val rotaryBounds = RectF()

    init {
        val (track, progress, dot) = getTargetColors()
        currentTrackColor = track
        currentProgressColor = progress
        currentDotBaseColor = dot
        trackPaint.color = currentTrackColor
        progressPaint.color = currentProgressColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cameraCenterX <= 0 && cameraCenterY <= 0) return
        if (animatedVisibilityAlpha <= 0.005f) return

        val baseRadius = (cameraRadiusPx + 14f * density) * ringRadiusScale * animatedScaleBounce
        arcBounds.set(
            cameraCenterX - baseRadius,
            cameraCenterY - baseRadius,
            cameraCenterX + baseRadius,
            cameraCenterY + baseRadius
        )

        canvas.save()
        canvas.translate(cameraCenterX, cameraCenterY)
        canvas.rotate(animatedVisibilityRotation)
        canvas.scale(animatedVisibilityScale, animatedVisibilityScale)
        canvas.translate(-cameraCenterX, -cameraCenterY)

        val trackAlpha = (Color.alpha(currentTrackColor) * animatedVisibilityAlpha).toInt()
        trackPaint.color = Color.argb(
            trackAlpha,
            Color.red(currentTrackColor),
            Color.green(currentTrackColor),
            Color.blue(currentTrackColor)
        )
        canvas.drawArc(arcBounds, animatedStartAngle, animatedTotalSweep, false, trackPaint)

        val progressSweep = (animatedProgress.coerceIn(0f, 100f) / 100f) * animatedTotalSweep
        if (progressSweep > 0.5f) {
            val progAlpha = (Color.alpha(currentProgressColor) * animatedVisibilityAlpha).toInt()
            progressPaint.color = Color.argb(
                progAlpha,
                Color.red(currentProgressColor),
                Color.green(currentProgressColor),
                Color.blue(currentProgressColor)
            )
            canvas.drawArc(arcBounds, animatedStartAngle, progressSweep, false, progressPaint)
        }

        val effectiveDotAlpha = animatedDotAlpha * (1f - animatedCustomFraction)
        if (effectiveDotAlpha > 0.01f) {
            val dotAngles = floatArrayOf(120f, 100f, 80f, 60f)
            val baseAlpha = Color.alpha(currentDotBaseColor)
            val red = Color.red(currentDotBaseColor)
            val green = Color.green(currentDotBaseColor)
            val blue = Color.blue(currentDotBaseColor)

            for (i in dotAngles.indices) {
                val angleDeg = dotAngles[i]
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val dotX = (cameraCenterX + baseRadius * cos(angleRad)).toFloat()
                val dotY = (cameraCenterY + baseRadius * sin(angleRad)).toFloat()

                val dotActiveFraction = (animatedSignalLevel - i).coerceIn(0f, 1f)
                val dotOpacity = (0.22f + 0.78f * dotActiveFraction) * effectiveDotAlpha * animatedVisibilityAlpha
                dotPaint.color = Color.argb((baseAlpha * dotOpacity).toInt(), red, green, blue)

                canvas.drawCircle(dotX, dotY, dotRadiusPx * effectiveDotAlpha, dotPaint)
            }
        }

        if (animatedCustomFraction > 0.01f) {
            val icon = getCurrentCustomIcon()
            if (icon != null) {
                val gapCenterAngleDeg = (animatedStartAngle + animatedTotalSweep + (360f - animatedTotalSweep) / 2f) % 360f
                val angleRad = Math.toRadians(gapCenterAngleDeg.toDouble())
                val downwardOffset = 4f * density
                val iconRadius = (dotRadiusPx * 2.85f) * animatedCustomFraction
                val iconCenterX = (cameraCenterX + (baseRadius + downwardOffset) * cos(angleRad)).toFloat()
                val iconCenterY = (cameraCenterY + (baseRadius + downwardOffset) * sin(angleRad)).toFloat()

                if (iconRadius > 1f) {
                    val saveIcon = canvas.save()
                    iconClipPath.reset()
                    iconClipPath.addCircle(iconCenterX, iconCenterY, iconRadius, Path.Direction.CW)
                    canvas.clipPath(iconClipPath)

                    iconRect.set(
                        iconCenterX - iconRadius,
                        iconCenterY - iconRadius,
                        iconCenterX + iconRadius,
                        iconCenterY + iconRadius
                    )
                    iconPaint.alpha = (255 * animatedCustomFraction * animatedVisibilityAlpha).toInt()
                    if (isFlashlightOn && showFlashlight) {
                        iconPaint.colorFilter = PorterDuffColorFilter(currentProgressColor, PorterDuff.Mode.SRC_IN)
                    } else {
                        iconPaint.colorFilter = null
                    }
                    canvas.drawBitmap(icon, null, iconRect, iconPaint)
                    canvas.restoreToCount(saveIcon)
                }
            }
        }

        // Long Press Charge Ring
        if (chargeProgress > 0.01f) {
            chargePaint.strokeWidth = 3.5f * density
            val chargeAlpha = (240 * chargeProgress * animatedVisibilityAlpha).toInt().coerceIn(0, 255)
            chargePaint.color = Color.argb(
                chargeAlpha,
                Color.red(currentProgressColor),
                Color.green(currentProgressColor),
                Color.blue(currentProgressColor)
            )
            val chargeRadius = baseRadius + 3.5f * density
            chargeBounds.set(
                cameraCenterX - chargeRadius,
                cameraCenterY - chargeRadius,
                cameraCenterX + chargeRadius,
                cameraCenterY + chargeRadius
            )
            val chargeSweep = 360f * chargeProgress
            canvas.drawArc(chargeBounds, 270f, chargeSweep, false, chargePaint)
        }

        // Action-Specific Visual Feedback
        if (feedbackProgress > 0.001f && activeFeedbackType != DuoFeedbackType.NONE) {
            when (activeFeedbackType) {
                DuoFeedbackType.SCREENSHOT -> {
                    val flashAlpha = (220 * (1f - feedbackProgress) * animatedVisibilityAlpha).toInt().coerceIn(0, 255)
                    feedbackFillPaint.color = Color.argb(flashAlpha, 255, 255, 255)
                    val flashRadius = cameraRadiusPx * (1f + 0.35f * feedbackProgress)
                    canvas.drawCircle(cameraCenterX, cameraCenterY, flashRadius, feedbackFillPaint)

                    feedbackStrokePaint.strokeWidth = 2f * density
                    feedbackStrokePaint.color = Color.argb(flashAlpha, 255, 255, 255)
                    for (i in 0..5) {
                        val angle = i * 60f + (45f * feedbackProgress)
                        val rad = Math.toRadians(angle.toDouble())
                        val cosVal = cos(rad).toFloat()
                        val sinVal = sin(rad).toFloat()
                        val x1 = cameraCenterX + cameraRadiusPx * 0.7f * cosVal
                        val y1 = cameraCenterY + cameraRadiusPx * 0.7f * sinVal
                        val x2 = cameraCenterX + (baseRadius + 6f * density) * cosVal
                        val y2 = cameraCenterY + (baseRadius + 6f * density) * sinVal
                        canvas.drawLine(x1, y1, x2, y2, feedbackStrokePaint)
                    }
                }

                DuoFeedbackType.LOCK_SCREEN -> {
                    val lockScale = 1f - 0.20f * sin(feedbackProgress * Math.PI.toFloat())
                    val lockRadius = baseRadius * lockScale
                    val lockAlpha = (220 * (1f - feedbackProgress) * animatedVisibilityAlpha).toInt().coerceIn(0, 255)
                    feedbackStrokePaint.strokeWidth = 4f * density
                    feedbackStrokePaint.color = Color.argb(lockAlpha, 0, 229, 255)
                    feedbackBounds.set(
                        cameraCenterX - lockRadius,
                        cameraCenterY - lockRadius,
                        cameraCenterX + lockRadius,
                        cameraCenterY + lockRadius
                    )
                    canvas.drawArc(feedbackBounds, 0f, 360f, false, feedbackStrokePaint)
                }

                DuoFeedbackType.NOTIFICATIONS -> {
                    val dropAlpha = (220 * (1f - feedbackProgress) * animatedVisibilityAlpha).toInt().coerceIn(0, 255)
                    feedbackFillPaint.color = Color.argb(
                        dropAlpha,
                        Color.red(currentProgressColor),
                        Color.green(currentProgressColor),
                        Color.blue(currentProgressColor)
                    )
                    val dropExtension = 26f * density * sin(feedbackProgress * Math.PI.toFloat())
                    val leftX = cameraCenterX - 12f * density
                    val rightX = cameraCenterX + 12f * density
                    val topY = cameraCenterY + baseRadius
                    val tipY = topY + dropExtension
                    teardropPath.reset()
                    teardropPath.moveTo(leftX, topY)
                    teardropPath.cubicTo(leftX, topY + dropExtension * 0.5f, cameraCenterX - 2f * density, tipY, cameraCenterX, tipY)
                    teardropPath.cubicTo(cameraCenterX + 2f * density, tipY, rightX, topY + dropExtension * 0.5f, rightX, topY)
                    teardropPath.close()
                    canvas.drawPath(teardropPath, feedbackFillPaint)
                }

                DuoFeedbackType.MEDIA_PLAY, DuoFeedbackType.MEDIA_PAUSE -> {
                    val rippleRadius = baseRadius + (28f * density * feedbackProgress)
                    val rippleAlpha = (200 * (1f - feedbackProgress) * animatedVisibilityAlpha).toInt().coerceIn(0, 255)
                    feedbackStrokePaint.strokeWidth = (5f * density * (1f - feedbackProgress)).coerceAtLeast(1.5f)
                    feedbackStrokePaint.color = Color.argb(
                        rippleAlpha,
                        Color.red(currentProgressColor),
                        Color.green(currentProgressColor),
                        Color.blue(currentProgressColor)
                    )
                    canvas.drawCircle(cameraCenterX, cameraCenterY, rippleRadius, feedbackStrokePaint)
                }

                DuoFeedbackType.TRACK_NEXT, DuoFeedbackType.TRACK_PREV -> {
                    val isNext = activeFeedbackType == DuoFeedbackType.TRACK_NEXT
                    val chevronSweepAngle = if (isNext) 45f * feedbackProgress else -45f * feedbackProgress
                    val baseAngle = (if (isNext) 250f else 290f) + chevronSweepAngle
                    val chevAlpha = (240 * (1f - feedbackProgress) * animatedVisibilityAlpha).toInt().coerceIn(0, 255)
                    feedbackStrokePaint.strokeWidth = 3f * density
                    feedbackStrokePaint.color = Color.argb(
                        chevAlpha,
                        Color.red(currentProgressColor),
                        Color.green(currentProgressColor),
                        Color.blue(currentProgressColor)
                    )
                    val offsets = if (isNext) floatArrayOf(-8f, 8f) else floatArrayOf(8f, -8f)
                    for (offset in offsets) {
                        val rad = Math.toRadians((baseAngle + offset).toDouble())
                        val cx = (cameraCenterX + baseRadius * cos(rad)).toFloat()
                        val cy = (cameraCenterY + baseRadius * sin(rad)).toFloat()
                        val wingLen = 6f * density
                        val dir = if (isNext) 1f else -1f
                        val tangentAngle = baseAngle + 90f
                        val tanRad = Math.toRadians(tangentAngle.toDouble())
                        val normRad = Math.toRadians(baseAngle.toDouble())
                        val cosTan = cos(tanRad).toFloat()
                        val sinTan = sin(tanRad).toFloat()
                        val cosNorm = cos(normRad).toFloat()
                        val sinNorm = sin(normRad).toFloat()

                        chevronPath.reset()
                        chevronPath.moveTo(
                            cx - (wingLen * 0.7f * cosTan * dir - wingLen * 0.7f * cosNorm),
                            cy - (wingLen * 0.7f * sinTan * dir - wingLen * 0.7f * sinNorm)
                        )
                        chevronPath.lineTo(cx, cy)
                        chevronPath.lineTo(
                            cx - (wingLen * 0.7f * cosTan * dir + wingLen * 0.7f * cosNorm),
                            cy - (wingLen * 0.7f * sinTan * dir + wingLen * 0.7f * sinNorm)
                        )
                        canvas.drawPath(chevronPath, feedbackStrokePaint)
                    }
                }

                DuoFeedbackType.TORCH_ON, DuoFeedbackType.TORCH_OFF -> {
                    val isTorchOnAnim = activeFeedbackType == DuoFeedbackType.TORCH_ON
                    val flareAlpha = (if (isTorchOnAnim) 220 * (1f - feedbackProgress) else 180 * (1f - feedbackProgress)).toInt().coerceIn(0, 255)
                    feedbackStrokePaint.strokeWidth = 2.5f * density
                    feedbackStrokePaint.color = Color.argb(flareAlpha, 255, 193, 7)
                    for (ray in 0..7) {
                        val angleDeg = ray * 45f + (feedbackProgress * 20f)
                        val rad = Math.toRadians(angleDeg.toDouble())
                        val cosVal = cos(rad).toFloat()
                        val sinVal = sin(rad).toFloat()
                        val rStart = baseRadius + 2f * density
                        val rEnd = rStart + (16f * density * (if (isTorchOnAnim) feedbackProgress else (1f - feedbackProgress)))
                        val x1 = cameraCenterX + rStart * cosVal
                        val y1 = cameraCenterY + rStart * sinVal
                        val x2 = cameraCenterX + rEnd * cosVal
                        val y2 = cameraCenterY + rEnd * sinVal
                        canvas.drawLine(x1, y1, x2, y2, feedbackStrokePaint)
                    }
                }

                DuoFeedbackType.RECENTS -> {
                    val recAlpha = (210 * (1f - feedbackProgress) * animatedVisibilityAlpha).toInt().coerceIn(0, 255)
                    feedbackStrokePaint.strokeWidth = 3f * density
                    feedbackStrokePaint.color = Color.argb(
                        recAlpha,
                        Color.red(currentProgressColor),
                        Color.green(currentProgressColor),
                        Color.blue(currentProgressColor)
                    )
                    val recRadius = baseRadius + (12f * density * feedbackProgress)
                    feedbackBounds.set(
                        cameraCenterX - recRadius,
                        cameraCenterY - recRadius,
                        cameraCenterX + recRadius,
                        cameraCenterY + recRadius
                    )
                    canvas.drawArc(feedbackBounds, 140f, 80f, false, feedbackStrokePaint)
                    canvas.drawArc(feedbackBounds, 320f, 80f, false, feedbackStrokePaint)
                }

                DuoFeedbackType.QUICK_SETTINGS -> {
                    val qsAlpha = (210 * (1f - feedbackProgress) * animatedVisibilityAlpha).toInt().coerceIn(0, 255)
                    feedbackStrokePaint.strokeWidth = 3.5f * density
                    feedbackStrokePaint.color = Color.argb(
                        qsAlpha,
                        Color.red(currentProgressColor),
                        Color.green(currentProgressColor),
                        Color.blue(currentProgressColor)
                    )
                    val qsSpread = 16f * density * feedbackProgress
                    val yPos = cameraCenterY
                    canvas.drawLine(cameraCenterX - baseRadius, yPos, cameraCenterX - baseRadius - qsSpread, yPos, feedbackStrokePaint)
                    canvas.drawLine(cameraCenterX + baseRadius, yPos, cameraCenterX + baseRadius + qsSpread, yPos, feedbackStrokePaint)
                }

                DuoFeedbackType.NONE -> {}
            }
        }

        // Rotary Virtual Dial Gauge
        if (rotaryAlpha > 0.01f) {
            val dialRadius = baseRadius + 10f * density
            rotaryBounds.set(
                cameraCenterX - dialRadius,
                cameraCenterY - dialRadius,
                cameraCenterX + dialRadius,
                cameraCenterY + dialRadius
            )
            val dialStartAngle = 150f
            val dialTotalSweep = 240f

            rotaryTrackPaint.strokeWidth = 3.5f * density
            val trackA = (70 * rotaryAlpha * animatedVisibilityAlpha).toInt().coerceIn(0, 255)
            rotaryTrackPaint.color = Color.argb(trackA, 255, 255, 255)
            canvas.drawArc(rotaryBounds, dialStartAngle, dialTotalSweep, false, rotaryTrackPaint)

            val activeSweep = dialTotalSweep * rotaryLevel
            if (activeSweep > 0.5f) {
                rotaryProgressPaint.strokeWidth = 4f * density
                val progA = (240 * rotaryAlpha * animatedVisibilityAlpha).toInt().coerceIn(0, 255)
                rotaryProgressPaint.color = Color.argb(
                    progA,
                    Color.red(currentProgressColor),
                    Color.green(currentProgressColor),
                    Color.blue(currentProgressColor)
                )
                canvas.drawArc(rotaryBounds, dialStartAngle, activeSweep, false, rotaryProgressPaint)
            }

            rotaryTickPaint.strokeWidth = 2f * density
            val tickA = (120 * rotaryAlpha * animatedVisibilityAlpha).toInt().coerceIn(0, 255)
            rotaryTickPaint.color = Color.argb(tickA, 255, 255, 255)
            val stepDegrees = dialTotalSweep / 6f
            for (step in 0..6) {
                val tickAngle = dialStartAngle + stepDegrees * step
                val tickRad = Math.toRadians(tickAngle.toDouble())
                val cosVal = cos(tickRad).toFloat()
                val sinVal = sin(tickRad).toFloat()
                val rInner = dialRadius - 3.5f * density
                val rOuter = dialRadius + 3.5f * density
                val tx1 = cameraCenterX + rInner * cosVal
                val ty1 = cameraCenterY + rInner * sinVal
                val tx2 = cameraCenterX + rOuter * cosVal
                val ty2 = cameraCenterY + rOuter * sinVal
                canvas.drawLine(tx1, ty1, tx2, ty2, rotaryTickPaint)
            }
        }

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressAnimator?.cancel()
        signalAnimator?.cancel()
        layoutAnimator?.cancel()
        scaleAnimator?.cancel()
        themeAnimator?.cancel()
        visibilityAnimator?.cancel()
        touchBounceAnimator?.cancel()
        feedbackAnimator?.cancel()
        chargeAnimator?.cancel()
        rotaryAnimator?.cancel()
        removeCallbacks(rotaryFadeRunnable)
    }
}


