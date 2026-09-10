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
            field = value.coerceIn(0, 100)
            invalidate()
        }

    var isDarkTheme: Boolean = true
        set(value) {
            field = value
            updateColors()
            invalidate()
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
        if (isDarkTheme) {
            trackPaint.color = Color.argb(60, 255, 255, 255)
            progressPaint.color = Color.WHITE
            dotPaint.color = Color.WHITE
        } else {
            trackPaint.color = Color.argb(60, 0, 0, 0)
            progressPaint.color = Color.BLACK
            dotPaint.color = Color.BLACK
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cameraCenterX <= 0 && cameraCenterY <= 0) return

        val baseRadius = (cameraRadiusPx + 14f * resources.displayMetrics.density) * ringRadiusScale
        val strokeHalf = arcThicknessPx / 2f
        arcBounds.set(
            cameraCenterX - baseRadius,
            cameraCenterY - baseRadius,
            cameraCenterX + baseRadius,
            cameraCenterY + baseRadius
        )

        val startAngle = 140f
        val totalSweep = 260f

        canvas.drawArc(arcBounds, startAngle, totalSweep, false, trackPaint)

        val progressSweep = (batteryLevel / 100f) * totalSweep
        if (progressSweep > 0.5f) {
            canvas.drawArc(arcBounds, startAngle, progressSweep, false, progressPaint)
        }

        val dotAngles = floatArrayOf(60f, 80f, 100f, 120f)
        for (angleDeg in dotAngles) {
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val dotX = (cameraCenterX + baseRadius * cos(angleRad)).toFloat()
            val dotY = (cameraCenterY + baseRadius * sin(angleRad)).toFloat()
            canvas.drawCircle(dotX, dotY, dotRadiusPx, dotPaint)
        }
    }
}
