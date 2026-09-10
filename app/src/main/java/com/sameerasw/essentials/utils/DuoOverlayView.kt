/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays
 * File: DuoOverlayView.kt
 * Description: Ambient camera ring and dots overlay view with media playback seekbar support.
 */

package com.sameerasw.essentials.utils

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
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import kotlin.math.cos
import kotlin.math.sin

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
        val shouldHide = isFullscreen || (isScreenOff && hideWhenScreenOff)
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

    private var mediaPaletteColors: Pair<Int, Int>? = null

    private fun extractMediaColors(bitmap: Bitmap): Pair<Int, Int> {
        val palette = try {
            Palette.from(bitmap).generate()
        } catch (_: Exception) {
            null
        }

        val accent = if (isDarkTheme) {
            palette?.getVibrantColor(0)
                ?.takeIf { it != 0 }
                ?: palette?.getLightVibrantColor(0)?.takeIf { it != 0 }
                ?: palette?.getDominantColor(Color.WHITE)
                ?: Color.WHITE
        } else {
            palette?.getDarkVibrantColor(0)
                ?.takeIf { it != 0 }
                ?: palette?.getVibrantColor(0)?.takeIf { it != 0 }
                ?: palette?.getDominantColor(Color.BLACK)
                ?: Color.BLACK
        }

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
        }
        if (progressChanged || stateChanged) {
            updateProgressAnimation()
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
    }
}


