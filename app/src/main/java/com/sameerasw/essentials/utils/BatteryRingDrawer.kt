package com.sameerasw.essentials.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.PI

object BatteryRingDrawer {

    fun drawBatteryWidget(
        context: Context,
        batteryLevel: Int,
        @ColorInt ringColor: Int,
        @ColorInt trackColor: Int,
        @ColorInt iconTint: Int,
        @ColorInt backgroundColor: Int,
        deviceIcon: Drawable?,
        statusIcon: Drawable?,  // New parameter for charging/warning icon
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = backgroundColor
        }
        val centerX = width / 2f
        val centerY = height / 2f
        
        val strokeWidth = width * 0.11f
        val padding = strokeWidth + (width * 0.05f)
        val rect = RectF(
            padding,
            padding,
            width - padding,
            height - padding
        )
        val radius = rect.width() / 2f
       
        val bubbleRadius = radius + (strokeWidth / 2f)
        
        canvas.drawCircle(centerX, centerY, bubbleRadius, bgPaint)

        // Config
        val trackStrokeWidth = strokeWidth * 0.5f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        // Dynamic Gap: 60 degrees if status icon present, otherwise 0 (full circle)
        val topGapDegrees = if (statusIcon != null) 60f else 0f
        
        val capAngleDegrees = ((strokeWidth / 2f) / radius) * (180f / PI.toFloat())
        val trackCapAngleDegrees = ((trackStrokeWidth / 2f) / radius) * (180f / PI.toFloat())

        // Start drawing from: -90 (top) + half gap
        val startAngle = -90f + (topGapDegrees / 2)
        val totalAvailableSweep = 360f - topGapDegrees

        val clampedLevel = batteryLevel.coerceIn(0, 100)
        
        val progressSweepRaw = (clampedLevel / 100f) * totalAvailableSweep

        // Visual Gap between segments
        val segmentGapDegrees = 8f 
        
        // --- Draw Progress Arc ---
        if (clampedLevel > 0) {
            paint.strokeWidth = strokeWidth
            paint.color = ringColor
            
            val visualStart = startAngle
            val visualEnd = if (clampedLevel >= 100) {
                 startAngle + totalAvailableSweep
            } else {
                 (startAngle + progressSweepRaw - (segmentGapDegrees / 2)).coerceAtLeast(startAngle)
            }
            
            val visualSpan = visualEnd - visualStart
            
            if (visualSpan > (capAngleDegrees * 2)) {
                val drawStart = visualStart + capAngleDegrees
                val drawSweep = (visualEnd - capAngleDegrees) - drawStart
                if (drawSweep > 0) {
                     canvas.drawArc(rect, drawStart, drawSweep, false, paint)
                }
            } else {
                if (visualSpan > 0) {
                      val center = visualStart + visualSpan/2
                      paint.style = Paint.Style.FILL
                      canvas.drawArc(rect, center, 0.1f, false, paint)
                 }
            }
        }

        // --- Draw Track Arc (Filler) ---
        if (clampedLevel < 100) {
            paint.strokeWidth = trackStrokeWidth
            paint.color = trackColor
            
            val visualStart = (startAngle + progressSweepRaw + (segmentGapDegrees / 2))
                .coerceAtMost(startAngle + totalAvailableSweep)
            
            val visualEnd = startAngle + totalAvailableSweep
            
            val visualSpan = visualEnd - visualStart
             if (visualSpan > (trackCapAngleDegrees * 2)) {
                val drawStart = visualStart + trackCapAngleDegrees
                val drawSweep = (visualEnd - trackCapAngleDegrees) - drawStart
                if (drawSweep > 0) {
                     canvas.drawArc(rect, drawStart, drawSweep, false, paint)
                }
             }
        }

        // --- Draw Status Icon (Top) if present ---
        if (statusIcon != null) {
            val smallIconRadius = strokeWidth * 1.3f
            val centerX = width / 2f
            val iconCenterY = rect.top
    
            val smallIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = ringColor
            }
            // Bubble Background
            canvas.drawCircle(centerX, iconCenterY, smallIconRadius, smallIconPaint)
    
            val iconSize = (smallIconRadius * 1.5f).toInt()
            val iconLeft = (centerX - iconSize / 2).toInt()
            val iconTop = (iconCenterY - iconSize / 2).toInt()
            statusIcon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            statusIcon.setTint(backgroundColor) 
            statusIcon.draw(canvas)
        }
        
        // --- Draw Center Device Icon ---
        val innerPadding = strokeWidth * 1.5f
        deviceIcon?.let {
            val availableWidth = (rect.width() - innerPadding * 2).toInt()
            val availableHeight = (rect.height() - innerPadding * 2).toInt()
            val iconBitmap = it.toBitmap(availableWidth, availableHeight)
            
            val iconLeft = (width - iconBitmap.width) / 2f
            val iconTop = (height - iconBitmap.height) / 2f
            
            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val colorFilter = android.graphics.PorterDuffColorFilter(iconTint, android.graphics.PorterDuff.Mode.SRC_IN)
            iconPaint.colorFilter = colorFilter
            
            canvas.drawBitmap(iconBitmap, iconLeft, iconTop, iconPaint)
        }

        return bitmap
    }
}
