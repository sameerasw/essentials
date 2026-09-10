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
import android.graphics.RectF
import android.os.Build
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin

class DuoOverlayView(context: Context) : View(context) {

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
            animateBatteryChange(clamped.toFloat())
        }

    var isDarkTheme: Boolean = true
        set(value) {
            if (field != value) {
                field = value
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
                if (!value && isMediaPlaying) {
                    setMediaState(isPlaying = false, progress = 0f, appIcon = null)
                }
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

    fun setMediaState(isPlaying: Boolean, progress: Float, appIcon: Bitmap?) {
        val effectivePlaying = isPlaying && showMedia
        val playingChanged = isMediaPlaying != effectivePlaying
        isMediaPlaying = effectivePlaying
        if (appIcon != null || !effectivePlaying) {
            mediaAppIcon = appIcon
        }
        mediaProgress = progress.coerceIn(0f, 100f)

        if (playingChanged) {
            animateLayoutChange()
        }
        animateMediaProgressChange(mediaProgress)
    }

    private var animatedBatteryProgress: Float = 100f
    private var batteryAnimator: ValueAnimator? = null

    private var animatedMediaProgress: Float = 0f
    private var mediaProgressAnimator: ValueAnimator? = null

    private var animatedSignalLevel: Float = 4f
    private var signalAnimator: ValueAnimator? = null

    private var animatedStartAngle: Float = 140f
    private var animatedTotalSweep: Float = 260f
    private var animatedDotAlpha: Float = 1.0f
    private var animatedMediaFraction: Float = 0.0f
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
            return Triple(
                Color.argb(40, 255, 255, 255),
                Color.argb(128, 255, 255, 255),
                Color.argb(128, 255, 255, 255)
            )
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
            duration = 600
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
            duration = 750
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener { animation ->
                animatedSignalLevel = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateBatteryChange(targetLevel: Float) {
        batteryAnimator?.cancel()
        batteryAnimator = ValueAnimator.ofFloat(animatedBatteryProgress, targetLevel).apply {
            duration = 900
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener { animation ->
                animatedBatteryProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateMediaProgressChange(targetProgress: Float) {
        mediaProgressAnimator?.cancel()
        mediaProgressAnimator = ValueAnimator.ofFloat(animatedMediaProgress, targetProgress).apply {
            duration = 600
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                animatedMediaProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateLayoutChange() {
        layoutAnimator?.cancel()
        scaleAnimator?.cancel()

        val targetMediaFraction = if (isMediaPlaying) 1.0f else 0.0f
        val targetStartAngle = if (isMediaPlaying) 120f else (if (showNetworks) 140f else -90f)
        val targetTotalSweep = if (isMediaPlaying) 300f else (if (showNetworks) 260f else 360f)
        val targetDotAlpha = if (isMediaPlaying) 0.0f else (if (showNetworks) 1.0f else 0.0f)

        val startStartAngle = animatedStartAngle
        val startTotalSweep = animatedTotalSweep
        val startDotAlpha = animatedDotAlpha
        val startMediaFraction = animatedMediaFraction

        layoutAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 850
            interpolator = OvershootInterpolator(1.15f)
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                animatedStartAngle = startStartAngle + (targetStartAngle - startStartAngle) * fraction
                animatedTotalSweep = startTotalSweep + (targetTotalSweep - startTotalSweep) * fraction
                animatedDotAlpha = (startDotAlpha + (targetDotAlpha - startDotAlpha) * fraction).coerceIn(0f, 1f)
                animatedMediaFraction = (startMediaFraction + (targetMediaFraction - startMediaFraction) * fraction).coerceIn(0f, 1f)
                invalidate()
            }
            start()
        }

        scaleAnimator = ValueAnimator.ofFloat(1.0f, 1.04f, 1.0f).apply {
            duration = 850
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
        setLayerType(LAYER_TYPE_SOFTWARE, null)
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

        val baseRadius = (cameraRadiusPx + 14f * resources.displayMetrics.density) * ringRadiusScale * animatedScaleBounce
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

        val effectiveProgress = animatedBatteryProgress + (animatedMediaProgress - animatedBatteryProgress) * animatedMediaFraction
        val progressSweep = (effectiveProgress / 100f) * animatedTotalSweep
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

        val effectiveDotAlpha = animatedDotAlpha * (1f - animatedMediaFraction)
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

        if (animatedMediaFraction > 0.01f && mediaAppIcon != null) {
            val icon = mediaAppIcon
            if (icon != null) {
                val gapCenterAngleDeg = (animatedStartAngle + animatedTotalSweep + (360f - animatedTotalSweep) / 2f) % 360f
                val angleRad = Math.toRadians(gapCenterAngleDeg.toDouble())
                val iconCenterX = (cameraCenterX + baseRadius * cos(angleRad)).toFloat()
                val iconCenterY = (cameraCenterY + baseRadius * sin(angleRad)).toFloat()

                val iconRadius = (dotRadiusPx * 2.2f) * animatedMediaFraction
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
                    iconPaint.alpha = (255 * animatedMediaFraction * animatedVisibilityAlpha).toInt()
                    canvas.drawBitmap(icon, null, iconRect, iconPaint)
                    canvas.restoreToCount(saveIcon)
                }
            }
        }

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        batteryAnimator?.cancel()
        mediaProgressAnimator?.cancel()
        signalAnimator?.cancel()
        layoutAnimator?.cancel()
        scaleAnimator?.cancel()
        themeAnimator?.cancel()
        visibilityAnimator?.cancel()
    }
}


