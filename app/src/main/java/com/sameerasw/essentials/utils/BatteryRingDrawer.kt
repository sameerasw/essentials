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

    /**
     * Optimized ring drawing for Glance components.
     * Only draws the arcs, leaving icons and backgrounds to Glance native primitives.
     */
    fun drawBatteryRing(
        context: Context,
        batteryLevel: Int,
        @ColorInt ringColor: Int,
        @ColorInt trackColor: Int,
        hasStatusIcon: Boolean,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val progressBitmap = drawProgressArc(batteryLevel, ringColor, hasStatusIcon, width, height)
        val trackBitmap = drawTrackArc(batteryLevel, trackColor, hasStatusIcon, width, height)

        canvas.drawBitmap(trackBitmap, 0f, 0f, null)
        canvas.drawBitmap(progressBitmap, 0f, 0f, null)

        return bitmap
    }

    fun drawProgressArc(
        batteryLevel: Int,
        @ColorInt color: Int,
        hasStatusIcon: Boolean,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val clampedLevel = batteryLevel.coerceIn(0, 100)
        if (clampedLevel <= 0) return bitmap

        val strokeWidth = width * 0.11f
        val padding = strokeWidth + (width * 0.05f)
        val rect = RectF(padding, padding, width - padding, height - padding)
        val radius = rect.width() / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            this.strokeWidth = strokeWidth
            this.color = color
        }

        val topGapDegrees = if (hasStatusIcon) 60f else 0f
        val capAngleDegrees = ((strokeWidth / 2f) / radius) * (180f / PI.toFloat())

        if (clampedLevel >= 100 && !hasStatusIcon) {
            canvas.drawArc(rect, -90f, 360f, false, paint)
            return bitmap
        }

        val segmentGapDegrees = 8f
        val startAngle = -90f + (topGapDegrees / 2)
        val totalAvailableSweep = 360f - topGapDegrees

        val topPadding = if (!hasStatusIcon) segmentGapDegrees / 2f else 0f
        val visualStart = startAngle + topPadding
        val progressSweepRaw = (clampedLevel / 100f) * totalAvailableSweep
        val visualEnd =
            (startAngle + progressSweepRaw - (segmentGapDegrees / 2)).coerceAtLeast(visualStart)

        val visualSpan = visualEnd - visualStart
        if (visualSpan > (capAngleDegrees * 2)) {
            val drawStart = visualStart + capAngleDegrees
            val drawSweep = (visualEnd - capAngleDegrees) - drawStart
            if (drawSweep > 0) {
                canvas.drawArc(rect, drawStart, drawSweep, false, paint)
            }
        } else if (visualSpan > 0) {
            val center = visualStart + visualSpan / 2
            paint.style = Paint.Style.FILL
            canvas.drawArc(rect, center, 0.1f, false, paint)
        }

        return bitmap
    }

    fun drawTrackArc(
        batteryLevel: Int,
        @ColorInt color: Int,
        hasStatusIcon: Boolean,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val clampedLevel = batteryLevel.coerceIn(0, 100)
        if (clampedLevel >= 100) return bitmap

        val strokeWidth = width * 0.11f
        val trackStrokeWidth = strokeWidth * 0.5f
        val padding = strokeWidth + (width * 0.05f)
        val rect = RectF(padding, padding, width - padding, height - padding)
        val radius = rect.width() / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            this.strokeWidth = trackStrokeWidth
            this.color = color
        }

        val topGapDegrees = if (hasStatusIcon) 60f else 0f
        val trackCapAngleDegrees = ((trackStrokeWidth / 2f) / radius) * (180f / PI.toFloat())
        val startAngle = -90f + (topGapDegrees / 2)
        val totalAvailableSweep = 360f - topGapDegrees
        val progressSweepRaw = (clampedLevel / 100f) * totalAvailableSweep
        val segmentGapDegrees = 8f

        val visualStart = (startAngle + progressSweepRaw + (segmentGapDegrees / 2))
            .coerceAtMost(startAngle + totalAvailableSweep)

        val topPadding = if (!hasStatusIcon) segmentGapDegrees / 2f else 0f
        val visualEnd = startAngle + totalAvailableSweep - topPadding

        val visualSpan = visualEnd - visualStart
        if (visualSpan > (trackCapAngleDegrees * 2)) {
            val drawStart = visualStart + trackCapAngleDegrees
            val drawSweep = (visualEnd - trackCapAngleDegrees) - drawStart
            if (drawSweep > 0) {
                canvas.drawArc(rect, drawStart, drawSweep, false, paint)
            }
        }

        return bitmap
    }

    /**
     * Legacy method for non-Glance components (e.g. Notifications).
     */
    fun drawBatteryWidget(
        context: Context,
        batteryLevel: Int,
        @ColorInt ringColor: Int,
        @ColorInt trackColor: Int,
        @ColorInt iconTint: Int,
        @ColorInt backgroundColor: Int,
        deviceIcon: Drawable?,
        statusIcon: Drawable?,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val strokeWidth = width * 0.11f
        val centerX = width / 2f
        val centerY = height / 2f

        // 1. Draw Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = backgroundColor
        }
        val padding = strokeWidth + (width * 0.05f)
        val rect = RectF(padding, padding, width - padding, height - padding)
        val bubbleRadius = (rect.width() / 2f) + (strokeWidth / 2f)
        canvas.drawCircle(centerX, centerY, bubbleRadius, bgPaint)

        // 2. Draw Ring
        val ringBitmap = drawBatteryRing(
            context, batteryLevel, ringColor, trackColor, statusIcon != null, width, height
        )
        canvas.drawBitmap(ringBitmap, 0f, 0f, null)

        // 3. Draw Status Icon Bubble
        if (statusIcon != null) {
            val smallIconRadius = strokeWidth * 1.3f
            val iconCenterY = rect.top

            bgPaint.color = ringColor
            canvas.drawCircle(centerX, iconCenterY, smallIconRadius, bgPaint)

            val iconSize = (smallIconRadius * 1.5f).toInt()
            val iconOffset = iconSize / 2
            statusIcon.setBounds(
                (centerX - iconOffset).toInt(),
                (iconCenterY - iconOffset).toInt(),
                (centerX + iconOffset).toInt(),
                (iconCenterY + iconOffset).toInt()
            )
            statusIcon.setTint(backgroundColor)
            statusIcon.draw(canvas)
        }

        // 4. Draw Device Icon
        deviceIcon?.let {
            val innerPadding = strokeWidth * 1.5f
            val iconSize = (rect.width() - innerPadding * 2).toInt()
            val iconBitmap = it.toBitmap(iconSize, iconSize)

            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = android.graphics.PorterDuffColorFilter(
                    iconTint,
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
            canvas.drawBitmap(
                iconBitmap,
                (width - iconBitmap.width) / 2f,
                (height - iconBitmap.height) / 2f,
                iconPaint
            )
        }

        return bitmap
    }
}
