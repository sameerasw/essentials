/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays
 * File: DuoOverlayView.kt
 * Description: Ambient camera ring and dots overlay view.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
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
            field = value
            updateColors()
            invalidate()
        }

    var isScreenOff: Boolean = false
        set(value) {
            field = value
            updateColors()
            invalidate()
        }

    var showNetworks: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                animateLayoutChange(value)
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

    private var animatedBatteryProgress: Float = 100f
    private var batteryAnimator: android.animation.ValueAnimator? = null

    private var animatedSignalLevel: Float = 4f
    private var signalAnimator: android.animation.ValueAnimator? = null

    private var animatedStartAngle: Float = 140f
    private var animatedTotalSweep: Float = 260f
    private var animatedDotAlpha: Float = 1.0f
    private var animatedScaleBounce: Float = 1.0f
    private var layoutAnimator: android.animation.ValueAnimator? = null
    private var scaleAnimator: android.animation.ValueAnimator? = null

    private fun animateSignalLevelChange(targetLevel: Float) {
        signalAnimator?.cancel()
        signalAnimator = android.animation.ValueAnimator.ofFloat(animatedSignalLevel, targetLevel).apply {
            duration = 750
            interpolator = android.view.animation.OvershootInterpolator(1.1f)
            addUpdateListener { animation ->
                animatedSignalLevel = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateBatteryChange(targetLevel: Float) {
        batteryAnimator?.cancel()
        batteryAnimator = android.animation.ValueAnimator.ofFloat(animatedBatteryProgress, targetLevel).apply {
            duration = 900
            interpolator = android.view.animation.OvershootInterpolator(1.1f)
            addUpdateListener { animation ->
                animatedBatteryProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateLayoutChange(showingNetworks: Boolean) {
        layoutAnimator?.cancel()
        scaleAnimator?.cancel()

        val targetStartAngle = if (showingNetworks) 140f else -90f
        val targetTotalSweep = if (showingNetworks) 260f else 360f
        val targetDotAlpha = if (showingNetworks) 1.0f else 0.0f

        val startStartAngle = animatedStartAngle
        val startTotalSweep = animatedTotalSweep
        val startDotAlpha = animatedDotAlpha

        layoutAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 850
            interpolator = android.view.animation.OvershootInterpolator(1.15f)
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                animatedStartAngle = startStartAngle + (targetStartAngle - startStartAngle) * fraction
                animatedTotalSweep = startTotalSweep + (targetTotalSweep - startTotalSweep) * fraction
                animatedDotAlpha = (startDotAlpha + (targetDotAlpha - startDotAlpha) * fraction).coerceIn(0f, 1f)
                invalidate()
            }
            start()
        }

        scaleAnimator = android.animation.ValueAnimator.ofFloat(1.0f, 1.04f, 1.0f).apply {
            duration = 850
            interpolator = android.view.animation.OvershootInterpolator(1.1f)
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

    private val arcBounds = RectF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        updateColors()
    }

    private fun updateColors() {
        if (isScreenOff) {
            trackPaint.color = Color.argb(40, 255, 255, 255)
            progressPaint.color = Color.argb(128, 255, 255, 255)
            dotPaint.color = Color.argb((128 * animatedDotAlpha).toInt(), 255, 255, 255)
        } else if (isDarkTheme) {
            trackPaint.color = Color.argb(60, 255, 255, 255)
            progressPaint.color = Color.WHITE
            dotPaint.color = Color.argb((255 * animatedDotAlpha).toInt(), 255, 255, 255)
        } else {
            trackPaint.color = Color.argb(60, 0, 0, 0)
            progressPaint.color = Color.BLACK
            dotPaint.color = Color.argb((255 * animatedDotAlpha).toInt(), 0, 0, 0)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cameraCenterX <= 0 && cameraCenterY <= 0) return

        val baseRadius = (cameraRadiusPx + 14f * resources.displayMetrics.density) * ringRadiusScale * animatedScaleBounce
        val strokeHalf = arcThicknessPx / 2f
        arcBounds.set(
            cameraCenterX - baseRadius,
            cameraCenterY - baseRadius,
            cameraCenterX + baseRadius,
            cameraCenterY + baseRadius
        )

        updateColors()

        canvas.drawArc(arcBounds, animatedStartAngle, animatedTotalSweep, false, trackPaint)

        val progressSweep = (animatedBatteryProgress / 100f) * animatedTotalSweep
        if (progressSweep > 0.5f) {
            canvas.drawArc(arcBounds, animatedStartAngle, progressSweep, false, progressPaint)
        }

        if (animatedDotAlpha > 0.01f) {
            val dotAngles = floatArrayOf(120f, 100f, 80f, 60f)
            val baseDotColor = if (isScreenOff) {
                Color.argb(128, 255, 255, 255)
            } else if (isDarkTheme) {
                Color.WHITE
            } else {
                Color.BLACK
            }
            val baseAlpha = Color.alpha(baseDotColor)
            val red = Color.red(baseDotColor)
            val green = Color.green(baseDotColor)
            val blue = Color.blue(baseDotColor)

            for (i in dotAngles.indices) {
                val angleDeg = dotAngles[i]
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val dotX = (cameraCenterX + baseRadius * cos(angleRad)).toFloat()
                val dotY = (cameraCenterY + baseRadius * sin(angleRad)).toFloat()

                // Signal level from 0..4 smoothly determines opacity of each dot (dot 0: 0..1, dot 1: 1..2, etc.)
                val dotActiveFraction = (animatedSignalLevel - i).coerceIn(0f, 1f)
                val dotOpacity = (0.22f + 0.78f * dotActiveFraction) * animatedDotAlpha
                dotPaint.color = Color.argb((baseAlpha * dotOpacity).toInt(), red, green, blue)

                canvas.drawCircle(dotX, dotY, dotRadiusPx * animatedDotAlpha, dotPaint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        batteryAnimator?.cancel()
        signalAnimator?.cancel()
        layoutAnimator?.cancel()
        scaleAnimator?.cancel()
    }
}

