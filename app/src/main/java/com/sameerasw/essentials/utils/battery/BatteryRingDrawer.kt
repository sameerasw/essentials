/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Battery Utilities
 * File: BatteryRingDrawer.kt
 * Description: Draws dynamic battery level ring indicators on camera cutouts.
 */

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
        height: Int,
        gapDegrees: Float? = null,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val progressBitmap = drawProgressArc(batteryLevel, ringColor, hasStatusIcon, width, height, gapDegrees)
        val trackBitmap = drawTrackArc(batteryLevel, trackColor, hasStatusIcon, width, height, gapDegrees)

        canvas.drawBitmap(trackBitmap, 0f, 0f, null)
        canvas.drawBitmap(progressBitmap, 0f, 0f, null)

        return bitmap
    }

    fun drawProgressArc(
        batteryLevel: Int,
        @ColorInt color: Int,
        hasStatusIcon: Boolean,
        width: Int,
        height: Int,
        gapDegrees: Float? = null,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val clampedLevel = batteryLevel.coerceIn(0, 100)
        if (clampedLevel <= 0) return bitmap

        val strokeWidth = width * 0.11f
        val padding = strokeWidth + (width * 0.05f)
        val rect = RectF(padding, padding, width - padding, height - padding)
        val radius = rect.width() / 2f

        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                this.strokeWidth = strokeWidth
                this.color = color
            }

        val topGapDegrees = gapDegrees ?: if (hasStatusIcon) 60f else 0f
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
        height: Int,
        gapDegrees: Float? = null,
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

        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                this.strokeWidth = trackStrokeWidth
                this.color = color
            }

        val topGapDegrees = gapDegrees ?: if (hasStatusIcon) 60f else 0f
        val trackCapAngleDegrees = ((trackStrokeWidth / 2f) / radius) * (180f / PI.toFloat())
        val startAngle = -90f + (topGapDegrees / 2)
        val totalAvailableSweep = 360f - topGapDegrees
        val progressSweepRaw = (clampedLevel / 100f) * totalAvailableSweep
        val segmentGapDegrees = 8f

        val visualStart =
            (startAngle + progressSweepRaw + (segmentGapDegrees / 2))
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
        height: Int,
        tintDeviceIcon: Boolean = true,
        percentText: String? = null,
        percentIcon: Drawable? = null,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val strokeWidth = width * 0.11f
        val centerX = width / 2f
        val centerY = height / 2f

        // 1. Draw Background
        val bgPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = backgroundColor
            }
        val padding = strokeWidth + (width * 0.05f)
        val rect = RectF(padding, padding, width - padding, height - padding)
        val bubbleRadius = (rect.width() / 2f) + (strokeWidth / 2f)
        canvas.drawCircle(centerX, centerY, bubbleRadius, bgPaint)

        // 2. Draw Ring
        val ringBitmap =
            drawBatteryRing(
                context,
                batteryLevel,
                ringColor,
                trackColor,
                statusIcon != null || percentText != null,
                width,
                height,
                percentText?.let { percentGapDegrees(context, width, it, percentIcon != null) },
            )
        canvas.drawBitmap(ringBitmap, 0f, 0f, null)

        // 3. Draw Status Icon Bubble or percentage chip
        if (percentText != null) {
            drawPercent(context, canvas, percentText, percentIcon, centerX, rect.top, width, ringColor, backgroundColor, iconTint)
        } else if (statusIcon != null) {
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
                (iconCenterY + iconOffset).toInt(),
            )
            statusIcon.setTint(backgroundColor)
            statusIcon.draw(canvas)
        }

        // 4. Draw Device Icon
        deviceIcon?.let {
            val innerPadding = strokeWidth * 1.5f
            val iconSize = (rect.width() - innerPadding * 2).toInt()
            val iconBitmap = it.toBitmap(iconSize, iconSize)

            val iconPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    if (tintDeviceIcon) {
                        colorFilter =
                            android.graphics.PorterDuffColorFilter(
                                iconTint,
                                android.graphics.PorterDuff.Mode.SRC_IN,
                            )
                    }
                }
            canvas.drawBitmap(
                iconBitmap,
                (width - iconBitmap.width) / 2f,
                (height - iconBitmap.height) / 2f,
                iconPaint,
            )
        }

        return bitmap
    }

    fun percentTextSize(width: Int): Float = width * 0.16f

    private fun percentTypeface(context: Context): android.graphics.Typeface {
        val base = androidx.core.content.res.ResourcesCompat.getFont(context, com.sameerasw.essentials.R.font.google_sans_flex)
            ?: return android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            android.graphics.Typeface.create(base, 700, false)
        } else {
            android.graphics.Typeface.create(base, android.graphics.Typeface.BOLD)
        }
    }

    private fun percentPaint(
        context: Context,
        width: Int,
    ): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = percentTypeface(context)
            textSize = percentTextSize(width)
        }

    class PercentLayers(
        val chip: Bitmap?,
        val content: Bitmap,
        val widthPx: Int,
        val heightPx: Int,
    )

    fun drawPercentLayers(
        context: Context,
        width: Int,
        text: String,
        icon: Drawable?,
    ): PercentLayers {
        val paint = percentPaint(context, width)
        val textSize = paint.textSize
        val textWidth = paint.measureText(text)
        val iconSize = if (icon != null) textSize * 1.0f else 0f
        val iconGap = if (icon != null) textSize * 0.1f else 0f
        val chipPadding = if (icon != null) textSize * 0.5f else 0f
        val chipWidth = (textWidth + iconSize + iconGap + chipPadding).toInt().coerceAtLeast(1)
        val chipHeight = (textSize * if (icon != null) 1.3f else 1.2f).toInt().coerceAtLeast(1)
        val content = Bitmap.createBitmap(chipWidth, chipHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(content)
        var x = chipPadding / 2f
        if (icon != null) {
            val top = (chipHeight - iconSize) / 2f
            icon.setBounds(x.toInt(), top.toInt(), (x + iconSize).toInt(), (top + iconSize).toInt())
            icon.setTint(android.graphics.Color.WHITE)
            icon.draw(canvas)
            x += iconSize + iconGap
        }
        paint.color = android.graphics.Color.WHITE
        paint.textAlign = Paint.Align.LEFT
        val baseline = chipHeight / 2f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
        canvas.drawText(text, x, baseline, paint)
        val chip =
            if (icon != null) {
                Bitmap.createBitmap(chipWidth, chipHeight, Bitmap.Config.ARGB_8888).also {
                    Canvas(it).drawRoundRect(
                        RectF(0f, 0f, chipWidth.toFloat(), chipHeight.toFloat()),
                        chipHeight / 2f,
                        chipHeight / 2f,
                        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE },
                    )
                }
            } else {
                null
            }
        return PercentLayers(chip, content, chipWidth, chipHeight)
    }

    fun percentGapDegrees(
        context: Context,
        width: Int,
        text: String,
        charging: Boolean,
    ): Float {
        val textSize = percentTextSize(width)
        val radius = width * 0.5f - (width * 0.11f + width * 0.05f)
        var contentWidth = percentPaint(context, width).measureText(text)
        contentWidth +=
            if (charging) {
                textSize * 1.0f + textSize * 0.1f + textSize * 0.5f + textSize * 1.0f
            } else {
                width * 0.08f
            }
        val degrees = Math.toDegrees((contentWidth / radius).toDouble()).toFloat()
        return if (charging) degrees.coerceIn(56f, 100f) else degrees.coerceIn(44f, 66f)
    }

    private fun drawPercent(
        context: Context,
        canvas: Canvas,
        text: String,
        icon: Drawable?,
        centerX: Float,
        centerY: Float,
        width: Int,
        @ColorInt chipColor: Int,
        @ColorInt chipContentColor: Int,
        @ColorInt textColor: Int,
    ) {
        val paint = percentPaint(context, width)
        val textSize = paint.textSize
        val textWidth = paint.measureText(text)
        if (icon == null) {
            paint.color = textColor
            paint.textAlign = Paint.Align.CENTER
            val baseline = centerY - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
            canvas.drawText(text, centerX, baseline, paint)
            return
        }
        val iconSize = textSize * 1.0f
        val iconGap = textSize * 0.1f
        val chipWidth = textWidth + iconSize + iconGap + textSize * 0.5f
        val chipHeight = textSize * 1.3f
        val left = centerX - chipWidth / 2f
        val top = centerY - chipHeight / 2f
        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = chipColor }
        canvas.drawRoundRect(RectF(left, top, left + chipWidth, top + chipHeight), chipHeight / 2f, chipHeight / 2f, chipPaint)
        var x = left + textSize * 0.25f
        val iconTop = centerY - iconSize / 2f
        icon.setBounds(x.toInt(), iconTop.toInt(), (x + iconSize).toInt(), (iconTop + iconSize).toInt())
        icon.setTint(chipContentColor)
        icon.draw(canvas)
        x += iconSize + iconGap
        paint.color = chipContentColor
        paint.textAlign = Paint.Align.LEFT
        val baseline = centerY - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
        canvas.drawText(text, x, baseline, paint)
    }
}
