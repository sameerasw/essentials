/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays - Island
 * File: RollingTextRenderer.kt
 * Description: Vertical ticker-style text/digit transitions shared by Island pills.
 */

package com.sameerasw.essentials.utils.island

import android.graphics.Canvas
import android.graphics.Paint

private class RollLayers(val oldOffsetY: Float, val oldAlpha: Float, val newOffsetY: Float, val newAlpha: Float)

private fun computeRollLayers(fraction: Float, rowHeight: Float): RollLayers {
    val dist = rowHeight * 0.9f
    return RollLayers(
        oldOffsetY = -fraction * dist,
        oldAlpha = 1f - fraction,
        newOffsetY = (1f - fraction) * dist,
        newAlpha = fraction,
    )
}

fun drawRollingDigits(
    canvas: Canvas,
    paint: Paint,
    oldText: String,
    newText: String,
    fraction: Float,
    edgeX: Float,
    baseY: Float,
    clipLeft: Float,
    clipTop: Float,
    clipRight: Float,
    clipBottom: Float,
    baseAlpha: Int,
    alignEnd: Boolean = true,
) {
    if (baseAlpha <= 0) return
    val maxLen = maxOf(oldText.length, newText.length)
    val paddedOld = oldText.padStart(maxLen, ' ')
    val paddedNew = newText.padStart(maxLen, ' ')

    val digitWidth = paint.measureText("0")
    val colonWidth = paint.measureText(":")
    fun getSlotWidth(ch: Char): Float = when {
        ch.isDigit() -> digitWidth
        ch == ':' -> colonWidth
        else -> paint.measureText(ch.toString())
    }

    var totalWidth = 0f
    for (i in 0 until maxLen) {
        totalWidth += getSlotWidth(paddedNew[i])
    }

    val startX = if (alignEnd) edgeX - totalWidth else edgeX
    val rowHeight = clipBottom - clipTop
    val rows = computeRollLayers(fraction, rowHeight)

    var curX = startX
    for (i in 0 until maxLen) {
        val oldChar = paddedOld[i]
        val newChar = paddedNew[i]
        val slotWidth = getSlotWidth(newChar)

        val oldCharStr = oldChar.toString()
        val newCharStr = newChar.toString()

        if (fraction >= 0.999f || oldChar == newChar) {
            paint.alpha = baseAlpha
            val textX = curX + (slotWidth - paint.measureText(newCharStr)) / 2f
            canvas.drawText(newCharStr, textX, baseY, paint)
        } else {
            canvas.save()
            canvas.clipRect(curX.coerceAtLeast(clipLeft), clipTop, (curX + slotWidth).coerceAtMost(clipRight), clipBottom)
            if (!oldChar.isWhitespace()) {
                paint.alpha = (baseAlpha * rows.oldAlpha).toInt().coerceIn(0, 255)
                val oldX = curX + (slotWidth - paint.measureText(oldCharStr)) / 2f
                canvas.drawText(oldCharStr, oldX, baseY + rows.oldOffsetY, paint)
            }
            if (!newChar.isWhitespace()) {
                paint.alpha = (baseAlpha * rows.newAlpha).toInt().coerceIn(0, 255)
                val newX = curX + (slotWidth - paint.measureText(newCharStr)) / 2f
                canvas.drawText(newCharStr, newX, baseY + rows.newOffsetY, paint)
            }
            canvas.restore()
        }
        curX += slotWidth
    }
}

fun drawRollingText(
    canvas: Canvas,
    paint: Paint,
    oldText: String,
    newText: String,
    fraction: Float,
    edgeX: Float,
    baseY: Float,
    clipLeft: Float,
    clipTop: Float,
    clipRight: Float,
    clipBottom: Float,
    baseAlpha: Int,
    alignEnd: Boolean = true,
) {
    if (baseAlpha <= 0) return
    fun drawX(text: String) = if (alignEnd) edgeX - paint.measureText(text) else edgeX
    if (fraction >= 0.999f || oldText == newText) {
        paint.alpha = baseAlpha
        canvas.drawText(newText, drawX(newText), baseY, paint)
        return
    }
    val rows = computeRollLayers(fraction, clipBottom - clipTop)
    canvas.save()
    canvas.clipRect(clipLeft, clipTop, clipRight, clipBottom)
    if (oldText.isNotBlank()) {
        paint.alpha = (baseAlpha * rows.oldAlpha).toInt().coerceIn(0, 255)
        canvas.drawText(oldText, drawX(oldText), baseY + rows.oldOffsetY, paint)
    }
    paint.alpha = (baseAlpha * rows.newAlpha).toInt().coerceIn(0, 255)
    canvas.drawText(newText, drawX(newText), baseY + rows.newOffsetY, paint)
    canvas.restore()
}
