/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays - Island
 * File: FlashlightPill.kt
 * Description: Flashlight content for the Island: icon and brightness in the row, expandable brightness slider.
 */

package com.sameerasw.essentials.utils.island

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.AppUtil
import kotlin.math.abs
import kotlin.math.roundToInt

interface FlashlightPillHost {
    val density: Float
    val cameraCenterX: Float
    val cameraCenterY: Float
    val cameraRadiusPx: Float
    val screenWidth: Float
    val cutoutGap: Float
    val expandedWidthPx: Float
    val expandedCornerRadiusPx: Float
    val accentColor: Int

    fun requestRedraw()
    fun onLayoutChanged()
    fun onDismissed()
}

enum class FlashlightHit { NONE, ICON, SLIDER, PILL }

class FlashlightPill(
    private val context: Context,
    private val host: FlashlightPillHost,
    typeface: Typeface?,
) {
    var isActive: Boolean = false
        private set
    var isExpanded: Boolean = false
        private set
    var supportsLevels: Boolean = true
        private set
    var showFraction: Float = 0f
        private set
    val pillRect = RectF()

    private var expandFraction = 0f
    private var levelDisplay = 1f
    private var isDragging = false
    private var dismissing = false
    private var percentText = "100%"
    private var percentPrev = ""
    private var percentRollFraction = 1f

    private val showAnimator = AnimatedFloatProperty()
    private val expandAnimator = AnimatedFloatProperty()
    private val levelAnimator = AnimatedFloatProperty()
    private val percentRollAnimator = AnimatedFloatProperty()

    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val sliderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        this.typeface = typeface ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
        fontFeatureSettings = "tnum"
    }
    private val icon: Bitmap? by lazy {
        try {
            ContextCompat.getDrawable(context, R.drawable.rounded_flashlight_on_24)
                ?.let { AppUtil.drawableToBitmap(it, (24f * host.density).toInt()) }
        } catch (_: Exception) {
            null
        }
    }

    private class Layout(
        val bounds: RectF,
        val iconRect: RectF,
        val percentRight: Float,
        val textCenterY: Float,
        val sliderRect: RectF,
    )

    private val rowHeight: Float get() = host.cameraRadiusPx * 2f + 14f * host.density
    private val iconSize: Float get() = (rowHeight - 14f * host.density).coerceAtLeast(16f * host.density)
    private val pad: Float get() = (rowHeight - iconSize) / 2f
    private val expandedPad: Float get() = 20f * host.density
    private val rowGap: Float get() = 6f * host.density

    private fun percentSlotWidth(): Float {
        textPaint.textSize = (rowHeight * 0.34f).coerceIn(12f * host.density, 18f * host.density)
        return textPaint.measureText("0") * 3f + textPaint.measureText("%")
    }

    private fun compactBounds(): RectF {
        val top = host.cameraCenterY - rowHeight / 2f
        val leftReach = host.cameraRadiusPx + host.cutoutGap + iconSize + host.cutoutGap * 2f
        val rightReach = host.cameraRadiusPx + host.cutoutGap + percentSlotWidth() + pad * 1.5f
        return RectF(
            (host.cameraCenterX - leftReach).coerceAtLeast(8f * host.density),
            top,
            (host.cameraCenterX + rightReach).coerceAtMost(host.screenWidth - 8f * host.density),
            top + rowHeight,
        )
    }

    private fun expandedBounds(): RectF {
        val top = host.cameraCenterY - rowHeight / 2f
        val isCenter = abs(host.cameraCenterX - host.screenWidth / 2f) < 50f * host.density
        val width = host.expandedWidthPx.coerceAtMost(host.screenWidth - 16f * host.density)
        val left: Float
        val right: Float
        if (isCenter) {
            val half = (width / 2f).coerceAtMost(
                minOf(host.cameraCenterX - 8f * host.density, host.screenWidth - host.cameraCenterX - 8f * host.density),
            )
            left = host.cameraCenterX - half
            right = host.cameraCenterX + half
        } else {
            left = (host.cameraCenterX - host.cameraRadiusPx - 8f * host.density).coerceAtLeast(8f * host.density)
            right = (left + width).coerceAtMost(host.screenWidth - 8f * host.density)
        }
        // Top row (icon, camera, %) is unchanged; only the slider row is added below it with roomier padding.
        return RectF(left, top, right, top + rowHeight + rowGap + iconSize + expandedPad)
    }

    private fun boundsFor(expand: Float): RectF {
        val c = compactBounds()
        val e = expandedBounds()
        return RectF(
            c.left + (e.left - c.left) * expand,
            c.top,
            c.right + (e.right - c.right) * expand,
            c.bottom + (e.bottom - c.bottom) * expand,
        )
    }

    // Single layout source shared by drawing and hit-testing so they can never disagree.
    private fun layout(bounds: RectF, expand: Float): Layout {
        val sideInset = pad + (expandedPad - pad) * expand
        val iconRect = RectF(bounds.left + sideInset, bounds.top + pad, bounds.left + sideInset + iconSize, bounds.top + pad + iconSize)
        val percentRight = bounds.right - (pad * 1.5f + (expandedPad - pad * 1.5f) * expand)
        val sliderTop = bounds.top + rowHeight + rowGap
        val sliderRect = RectF(
            bounds.left + expandedPad,
            sliderTop,
            bounds.right - expandedPad,
            sliderTop + iconSize,
        )
        return Layout(bounds, iconRect, percentRight, bounds.top + rowHeight / 2f, sliderRect)
    }

    fun targetBounds(): RectF = boundsFor(if (isExpanded) 1f else 0f)

    fun show(percent: Int, supportsLevels: Boolean) {
        this.supportsLevels = supportsLevels
        if (isActive && !dismissing) {
            updateLevel(percent)
            return
        }
        dismissing = false
        isActive = true
        isExpanded = false
        levelDisplay = percent.coerceIn(0, 100) / 100f
        percentText = "${percent.coerceIn(0, 100)}%"
        percentPrev = ""
        percentRollFraction = 1f
        showAnimator.animateTo(
            from = showFraction,
            to = 1f,
            spec = IslandTransitionSpec.ContentShow,
            onUpdate = {
                showFraction = it
                host.requestRedraw()
            },
        )
        host.onLayoutChanged()
    }

    fun updateLevel(percent: Int) {
        if (!isActive || isDragging) return
        val clamped = percent.coerceIn(0, 100)
        applyText("$clamped%", roll = true)
        levelAnimator.animateTo(
            from = levelDisplay,
            to = clamped / 100f,
            spec = IslandTransitionSpec.ModeChange,
            onUpdate = {
                levelDisplay = it
                host.requestRedraw()
            },
        )
    }

    fun setLevelFromUser(fraction: Float) {
        if (!isActive) return
        levelAnimator.cancel()
        levelDisplay = fraction.coerceIn(0f, 1f)
        applyText("${(levelDisplay * 100f).roundToInt()}%", roll = false)
        host.requestRedraw()
    }

    fun setDragging(dragging: Boolean) {
        isDragging = dragging
    }

    private fun applyText(text: String, roll: Boolean) {
        if (text == percentText) return
        if (roll && showFraction > 0.05f) {
            percentPrev = percentText
            percentRollFraction = 0f
            percentRollAnimator.animateTo(
                from = 0f,
                to = 1f,
                spec = IslandTransitionSpec.ModeChange,
                onUpdate = {
                    percentRollFraction = it
                    host.requestRedraw()
                },
            )
        } else {
            percentRollAnimator.cancel()
            percentRollFraction = 1f
        }
        percentText = text
    }

    fun toggleExpansion(): Boolean {
        setExpanded(!isExpanded)
        return isExpanded
    }

    fun setExpanded(expanded: Boolean) {
        if (!isActive || isExpanded == expanded) return
        isExpanded = expanded
        expandAnimator.animateTo(
            from = expandFraction,
            to = if (expanded) 1f else 0f,
            spec = IslandTransitionSpec.ExpandOpen,
            onUpdate = {
                expandFraction = it
                host.requestRedraw()
            },
        )
        host.onLayoutChanged()
    }

    fun dismiss() {
        if (!isActive || dismissing) return
        dismissing = true
        isExpanded = false
        isDragging = false
        expandAnimator.animateTo(
            from = expandFraction,
            to = 0f,
            spec = IslandTransitionSpec.MediaDismiss,
            onUpdate = {
                expandFraction = it
                host.requestRedraw()
            },
        )
        showAnimator.animateTo(
            from = showFraction,
            to = 0f,
            spec = IslandTransitionSpec.MediaDismiss,
            onUpdate = {
                showFraction = it
                host.requestRedraw()
            },
            onEnd = {
                if (dismissing && showFraction <= 0.01f) {
                    dismissing = false
                    isActive = false
                    showFraction = 0f
                    expandFraction = 0f
                    host.onDismissed()
                }
            },
        )
    }

    fun hitTest(x: Float, y: Float): FlashlightHit {
        if (!isActive) return FlashlightHit.NONE
        val expand = if (isExpanded) 1f else 0f
        val l = layout(targetBounds(), expand)
        val touchPad = 12f * host.density
        val outer = RectF(l.bounds.left - touchPad, l.bounds.top - touchPad, l.bounds.right + touchPad, l.bounds.bottom + touchPad)
        if (!outer.contains(x, y)) return FlashlightHit.NONE
        val iconHit = RectF(l.iconRect.left - touchPad, l.iconRect.top - touchPad, l.iconRect.right + touchPad, l.iconRect.bottom + touchPad)
        if (iconHit.contains(x, y)) return FlashlightHit.ICON
        if (isExpanded) {
            val sliderHit = RectF(l.sliderRect.left - touchPad, l.sliderRect.top - touchPad, l.sliderRect.right + touchPad, l.sliderRect.bottom + touchPad)
            if (sliderHit.contains(x, y)) return FlashlightHit.SLIDER
        }
        return FlashlightHit.PILL
    }

    fun sliderFractionAt(x: Float): Float {
        val slider = layout(targetBounds(), 1f).sliderRect
        return ((x - slider.left) / slider.width().coerceAtLeast(1f)).coerceIn(0f, 1f)
    }

    fun draw(canvas: Canvas) {
        if (!isActive || showFraction <= 0.001f) return
        val target = boundsFor(expandFraction)
        val initialLeft = host.cameraCenterX - host.cameraRadiusPx
        val initialRight = host.cameraCenterX + host.cameraRadiusPx
        pillRect.set(
            initialLeft + (target.left - initialLeft) * showFraction,
            target.top,
            initialRight + (target.right - initialRight) * showFraction,
            target.bottom,
        )
        val corner = rowHeight / 2f + (host.expandedCornerRadiusPx - rowHeight / 2f).coerceAtLeast(0f) * expandFraction
        canvas.drawRoundRect(pillRect, corner, corner, pillPaint)

        val contentAlpha = (((showFraction - 0.3f) / 0.7f).coerceIn(0f, 1f) * 255f).toInt()
        if (contentAlpha <= 0) return
        val l = layout(pillRect, expandFraction)
        val save = canvas.save()
        path.reset()
        path.addRoundRect(pillRect, corner, corner, Path.Direction.CW)
        canvas.clipPath(path)

        icon?.let {
            iconPaint.alpha = contentAlpha
            iconPaint.colorFilter = PorterDuffColorFilter(host.accentColor, PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(it, null, l.iconRect, iconPaint)
            iconPaint.colorFilter = null
        }

        val textWidth = percentSlotWidth()
        val rowTop = l.textCenterY - rowHeight / 2f
        val rowBottom = l.textCenterY + rowHeight / 2f
        drawRollingDigits(
            canvas = canvas,
            paint = textPaint,
            oldText = percentPrev,
            newText = percentText,
            fraction = percentRollFraction,
            edgeX = l.percentRight,
            baseY = l.textCenterY + textPaint.textSize * 0.35f,
            clipLeft = l.percentRight - textWidth,
            clipTop = rowTop,
            clipRight = l.percentRight,
            clipBottom = rowBottom,
            baseAlpha = contentAlpha,
        )

        val sliderAlpha = (contentAlpha * ((expandFraction - 0.3f) / 0.7f).coerceIn(0f, 1f)).toInt()
        if (sliderAlpha > 0) drawSlider(canvas, l.sliderRect, if (supportsLevels) sliderAlpha else (sliderAlpha * 0.4f).toInt())
        canvas.restoreToCount(save)
    }

    // Material 3 Expressive slider: thick rounded track split around a pill handle, inner corners nearly square.
    private fun drawSlider(canvas: Canvas, rect: RectF, alpha: Int) {
        val outer = rect.height() / 2f
        val inner = 4f * host.density
        val handleWidth = 5f * host.density
        val gap = 6f * host.density
        val handleX = rect.left + rect.width() * levelDisplay

        val activeRight = handleX - gap - handleWidth / 2f
        if (activeRight > rect.left) {
            sliderPaint.color = host.accentColor
            sliderPaint.alpha = alpha
            path.reset()
            path.addRoundRect(
                RectF(rect.left, rect.top, activeRight, rect.bottom),
                floatArrayOf(outer, outer, inner, inner, inner, inner, outer, outer),
                Path.Direction.CW,
            )
            canvas.drawPath(path, sliderPaint)
        }

        val inactiveLeft = handleX + gap + handleWidth / 2f
        if (inactiveLeft < rect.right) {
            sliderPaint.color = Color.WHITE
            sliderPaint.alpha = (alpha * 0.18f).toInt()
            path.reset()
            path.addRoundRect(
                RectF(inactiveLeft, rect.top, rect.right, rect.bottom),
                floatArrayOf(inner, inner, outer, outer, outer, outer, inner, inner),
                Path.Direction.CW,
            )
            canvas.drawPath(path, sliderPaint)
        }

        sliderPaint.color = host.accentColor
        sliderPaint.alpha = alpha
        val handleRect = RectF(
            handleX - handleWidth / 2f,
            rect.top - 4f * host.density,
            handleX + handleWidth / 2f,
            rect.bottom + 4f * host.density,
        )
        canvas.drawRoundRect(handleRect, handleWidth / 2f, handleWidth / 2f, sliderPaint)
    }

    fun cancelAnimations() {
        showAnimator.cancel()
        expandAnimator.cancel()
        levelAnimator.cancel()
        percentRollAnimator.cancel()
    }
}
