/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays - Island
 * File: IslandBubbleRow.kt
 * Description: Reusable renderer for bubbles docked to an edge of the island pill.
 */

package com.sameerasw.essentials.utils.island

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF

enum class IslandBubbleSide { LEADING, TRAILING }

enum class IslandBubbleIconShape { ROUNDED_SQUARE, CIRCLE }

class IslandBubbleSpec(
    val key: Any,
    val visibleFraction: Float,
    val icon: Bitmap?,
    val iconShape: IslandBubbleIconShape = IslandBubbleIconShape.ROUNDED_SQUARE,
    val iconTint: Int? = null,
    val slot: Float = 0f,
    val enterFrom: RectF? = null,
    // Drawn instead of [icon] when it's null — for non-bitmap content like an animated glyph.
    val customIconDraw: ((Canvas, RectF) -> Unit)? = null,
)

object IslandBubbleRow {
    fun reservedWidth(bubbles: List<IslandBubbleSpec>, bubbleSize: Float, bubbleGap: Float): Float {
        var maxExtent = 0f
        for (bubble in bubbles) {
            val extent = (bubble.slot + 1f) * (bubbleSize + bubbleGap) * bubble.visibleFraction
            if (extent > maxExtent) maxExtent = extent
        }
        return maxExtent
    }

    fun draw(
        canvas: Canvas,
        side: IslandBubbleSide,
        bubbles: List<IslandBubbleSpec>,
        anchorEdge: Float,
        top: Float,
        bottom: Float,
        bubbleSize: Float,
        bubbleGap: Float,
        density: Float,
        pillPaint: Paint,
        iconPaint: Paint,
        tintPaint: Paint,
        outRects: MutableMap<Any, RectF>,
        extraGap: Float = 0f,
    ) {
        outRects.clear()
        val clipPath = Path()
        val gap = bubbleGap + extraGap
        val step = bubbleSize + gap

        for (bubble in bubbles) {
            val frac = bubble.visibleFraction
            if (frac <= 0.01f) continue

            val targetLeft: Float
            val targetRight: Float
            if (side == IslandBubbleSide.LEADING) {
                targetRight = anchorEdge - gap - bubble.slot * step
                targetLeft = targetRight - bubbleSize
            } else {
                targetLeft = anchorEdge + gap + bubble.slot * step
                targetRight = targetLeft + bubbleSize
            }

            val origin = bubble.enterFrom
            val rect = if (origin != null) {
                RectF(
                    origin.left + (targetLeft - origin.left) * frac,
                    origin.top + (top - origin.top) * frac,
                    origin.right + (targetRight - origin.right) * frac,
                    origin.bottom + (bottom - origin.bottom) * frac,
                )
            } else {
                RectF(targetLeft, top, targetRight, bottom)
            }
            outRects[bubble.key] = rect

            val radius = rect.height() / 2f
            val iconSize = (rect.height() - 12f * density).coerceAtLeast(14f * density)
            val iconPad = (rect.height() - iconSize) / 2f

            val saveCount = canvas.save()
            if (origin == null) {
                canvas.scale(frac, frac, rect.centerX(), rect.centerY())
            }

            pillPaint.alpha = (frac * 255).toInt().coerceIn(0, 255)
            canvas.drawRoundRect(rect, radius, radius, pillPaint)

            val icon = bubble.icon
            if (icon != null) {
                val iconRect = RectF(rect.left + iconPad, rect.top + iconPad, rect.right - iconPad, rect.bottom - iconPad)
                clipPath.reset()
                if (bubble.iconShape == IslandBubbleIconShape.CIRCLE) {
                    clipPath.addCircle(iconRect.centerX(), iconRect.centerY(), iconSize / 2f, Path.Direction.CW)
                } else {
                    clipPath.addRoundRect(iconRect, iconSize * 0.28f, iconSize * 0.28f, Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(clipPath)
                val paint = if (bubble.iconTint != null) tintPaint else iconPaint
                if (bubble.iconTint != null) {
                    paint.colorFilter = PorterDuffColorFilter(bubble.iconTint, PorterDuff.Mode.SRC_IN)
                }
                paint.alpha = (frac * 255).toInt().coerceIn(0, 255)
                canvas.drawBitmap(icon, null, iconRect, paint)
                canvas.restore()
            } else {
                bubble.customIconDraw?.let { draw ->
                    val iconRect = RectF(rect.left + iconPad, rect.top + iconPad, rect.right - iconPad, rect.bottom - iconPad)
                    draw(canvas, iconRect)
                }
            }

            canvas.restoreToCount(saveCount)
        }
    }
}
