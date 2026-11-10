/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Edge Pill
 * File: SideKickView.kt
 * Description: Renders the curved translucent edge handle arc and detects gestures.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.sameerasw.essentials.data.repository.SettingsRepository
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

class SideKickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val settings = SettingsRepository(context)
    private val density = context.resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    private val visiblePillWidthPx = 10f * density
    private val arcIndentAngle = 36f
    private val sweepAngle get() = 180f - (2f * arcIndentAngle)

    private val strokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

    private val fillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

    private val accentPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }

    private val path = android.graphics.Path()
    private val rectF = RectF()
    private var startAngle = 0f

    private var startX = 0f
    private var startY = 0f
    private var isFlicking = false

    var onRight: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateArcGeometry()
                postInvalidate()
            }
        }

    var listener: GestureListener?
        get() = gestureDetector.listener
        set(value) {
            gestureDetector.listener = value
        }

    private val gestureDetector by lazy {
        QuickBallGestureDetector(
            context = context,
            isGestureEnabled = { settings.isQuickBallGesturesEnabled() },
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val colors = QuickBallTheme.resolveColors(context, settings)
        val pillColor = colors.pillColor
        val idleAlpha = settings.getQuickBallIdleAlpha().coerceIn(0.2f, 1f)
        val alphaInt = (idleAlpha * 255).roundToInt()
        val pillColorWithAlpha = (pillColor and 0x00FFFFFF) or (alphaInt shl 24)

        val isBorder = settings.isQuickBallBorderEnabled()
        val borderWidthPx = if (isBorder) (settings.getQuickBallBorderWidth() * density) else 0f
        val borderColorWithAlpha = (colors.borderColor and 0x00FFFFFF) or (alphaInt shl 24)

        val strokeWidthPx = (settings.getQuickBallPillThickness() * density).coerceAtLeast(1f)
        val pillStyle = QuickBallPillStyle.fromName(settings.getQuickBallPillStyle())

        when (pillStyle) {
            QuickBallPillStyle.MIUI_ARC -> {
                strokePaint.color = pillColorWithAlpha
                strokePaint.strokeWidth = strokeWidthPx
                canvas.drawArc(rectF, startAngle, sweepAngle, false, strokePaint)
            }

            QuickBallPillStyle.IOS_CAPSULE -> {
                val capsuleWidth = (strokeWidthPx * 1.35f).coerceIn(4f * density, 9f * density)
                val capsuleHeight = (h * 0.72f).coerceAtLeast(16f * density)
                val margin = 2.5f * density
                val top = (h - capsuleHeight) / 2f
                val bottom = (h + capsuleHeight) / 2f
                val left = if (onRight) w - capsuleWidth - margin else margin
                val right = if (onRight) w - margin else margin + capsuleWidth
                val radius = capsuleWidth / 2f

                fillPaint.color = pillColorWithAlpha
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, fillPaint)

                if (borderWidthPx > 0f) {
                    borderPaint.color = borderColorWithAlpha
                    borderPaint.strokeWidth = borderWidthPx
                    canvas.drawRoundRect(left, top, right, bottom, radius, radius, borderPaint)
                }
            }

            QuickBallPillStyle.ANDROID_BAR -> {
                val barWidth = strokeWidthPx.coerceIn(3.5f * density, 6f * density)
                val barHeight = (h * 0.62f).coerceAtLeast(14f * density)
                val top = (h - barHeight) / 2f
                val bottom = (h + barHeight) / 2f
                val left = if (onRight) w - barWidth else 0f
                val right = if (onRight) w else barWidth
                val radius = barWidth / 2f

                fillPaint.color = pillColorWithAlpha
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, fillPaint)

                if (borderWidthPx > 0f) {
                    borderPaint.color = borderColorWithAlpha
                    borderPaint.strokeWidth = borderWidthPx
                    canvas.drawRoundRect(left, top, right, bottom, radius, radius, borderPaint)
                }
            }

            QuickBallPillStyle.SAMSUNG_EDGE -> {
                val tabWidth = (strokeWidthPx * 1.6f).coerceIn(8f * density, 14f * density)
                val tabHeight = (h * 0.74f).coerceAtLeast(18f * density)
                val top = (h - tabHeight) / 2f
                val bottom = (h + tabHeight) / 2f
                val cornerRadius = 6f * density

                path.reset()
                val left: Float
                val right: Float
                if (onRight) {
                    left = w - tabWidth
                    right = w
                    path.moveTo(right, top)
                    path.lineTo(left + cornerRadius, top)
                    path.quadTo(left, top, left, top + cornerRadius)
                    path.lineTo(left, bottom - cornerRadius)
                    path.quadTo(left, bottom, left + cornerRadius, bottom)
                    path.lineTo(right, bottom)
                    path.close()
                } else {
                    left = 0f
                    right = tabWidth
                    path.moveTo(left, top)
                    path.lineTo(right - cornerRadius, top)
                    path.quadTo(right, top, right, top + cornerRadius)
                    path.lineTo(right, bottom - cornerRadius)
                    path.quadTo(right, bottom, right - cornerRadius, bottom)
                    path.lineTo(left, bottom)
                    path.close()
                }

                fillPaint.color = pillColorWithAlpha
                canvas.drawPath(path, fillPaint)

                if (borderWidthPx > 0f) {
                    borderPaint.color = borderColorWithAlpha
                    borderPaint.strokeWidth = borderWidthPx
                    canvas.drawPath(path, borderPaint)
                }

                // Inner chevron (< or >)
                val cx = (left + right) / 2f
                val cy = (top + bottom) / 2f
                val chevronW = 3.2f * density
                val chevronH = 4.8f * density
                accentPaint.strokeWidth = 1.8f * density
                accentPaint.color = (colors.contentColor and 0x00FFFFFF) or (alphaInt shl 24)

                path.reset()
                if (onRight) {
                    path.moveTo(cx + chevronW / 2f, cy - chevronH)
                    path.lineTo(cx - chevronW / 2f, cy)
                    path.lineTo(cx + chevronW / 2f, cy + chevronH)
                } else {
                    path.moveTo(cx - chevronW / 2f, cy - chevronH)
                    path.lineTo(cx + chevronW / 2f, cy)
                    path.lineTo(cx - chevronW / 2f, cy + chevronH)
                }
                canvas.drawPath(path, accentPaint)
            }

            QuickBallPillStyle.ONEPLUS_SLIDER -> {
                val tabWidth = (strokeWidthPx * 1.4f).coerceIn(6f * density, 11f * density)
                val tabHeight = (h * 0.78f).coerceAtLeast(18f * density)
                val top = (h - tabHeight) / 2f
                val bottom = (h + tabHeight) / 2f
                val left = if (onRight) w - tabWidth else 0f
                val right = if (onRight) w else tabWidth
                val radius = tabWidth / 2f

                fillPaint.color = pillColorWithAlpha
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, fillPaint)

                if (borderWidthPx > 0f) {
                    borderPaint.color = borderColorWithAlpha
                    borderPaint.strokeWidth = borderWidthPx
                    canvas.drawRoundRect(left, top, right, bottom, radius, radius, borderPaint)
                }

                // Accent center line
                val cx = (left + right) / 2f
                val lineH = tabHeight * 0.42f
                val lineTop = (h - lineH) / 2f
                val lineBottom = (h + lineH) / 2f
                accentPaint.strokeWidth = 2.2f * density
                accentPaint.color = (colors.accentColor and 0x00FFFFFF) or (alphaInt shl 24)
                canvas.drawLine(cx, lineTop, cx, lineBottom, accentPaint)
            }

            QuickBallPillStyle.NOTHING_GLYPH -> {
                val dashWidth = strokeWidthPx.coerceIn(3.5f * density, 6f * density)
                val left = if (onRight) w - dashWidth else 0f
                val right = if (onRight) w else dashWidth
                val radius = dashWidth / 2f
                val mainHeight = (h * 0.4f).coerceAtLeast(10f * density)
                val mainTop = (h - mainHeight) / 2f
                val mainBottom = (h + mainHeight) / 2f
                val gap = 3.5f * density
                val dotHeight = 4f * density

                fillPaint.color = pillColorWithAlpha
                canvas.drawRoundRect(left, mainTop, right, mainBottom, radius, radius, fillPaint)
                canvas.drawRoundRect(left, mainTop - gap - dotHeight, right, mainTop - gap, radius, radius, fillPaint)
                canvas.drawRoundRect(left, mainBottom + gap, right, mainBottom + gap + dotHeight, radius, radius, fillPaint)

                if (borderWidthPx > 0f) {
                    borderPaint.color = borderColorWithAlpha
                    borderPaint.strokeWidth = borderWidthPx
                    canvas.drawRoundRect(left, mainTop, right, mainBottom, radius, radius, borderPaint)
                    canvas.drawRoundRect(left, mainTop - gap - dotHeight, right, mainTop - gap, radius, radius, borderPaint)
                    canvas.drawRoundRect(left, mainBottom + gap, right, mainBottom + gap + dotHeight, radius, radius, borderPaint)
                }
            }

            QuickBallPillStyle.MINIMAL_DOT -> {
                val diameter = (strokeWidthPx * 2.2f).coerceIn(12f * density, 22f * density)
                val radius = diameter / 2f
                val cy = h / 2f

                fillPaint.color = pillColorWithAlpha
                if (onRight) {
                    rectF.set(w - diameter, cy - radius, w + diameter, cy + radius)
                    canvas.drawArc(rectF, 90f, 180f, true, fillPaint)
                    if (borderWidthPx > 0f) {
                        borderPaint.color = borderColorWithAlpha
                        borderPaint.strokeWidth = borderWidthPx
                        canvas.drawArc(rectF, 90f, 180f, false, borderPaint)
                    }
                } else {
                    rectF.set(-radius, cy - radius, diameter, cy + radius)
                    canvas.drawArc(rectF, 270f, 180f, true, fillPaint)
                    if (borderWidthPx > 0f) {
                        borderPaint.color = borderColorWithAlpha
                        borderPaint.strokeWidth = borderWidthPx
                        canvas.drawArc(rectF, 270f, 180f, false, borderPaint)
                    }
                }
            }
        }
    }

    fun update() {
        updateArcGeometry()
        postInvalidate()
    }

    private fun updateArcGeometry() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val strokeWidthPx = settings.getQuickBallPillThickness() * density
        val halfStroke = strokeWidthPx / 2f
        val topBound = halfStroke
        val bottomBound = h - halfStroke

        if (onRight) {
            val centerX = w
            val leftBound = w - visiblePillWidthPx
            val rightBound = 2f * centerX - leftBound
            rectF.set(leftBound, topBound, rightBound, bottomBound)
            startAngle = 90f + arcIndentAngle
        } else {
            val centerX = 0f
            val rightBound = visiblePillWidthPx
            val leftBound = 2f * centerX - rightBound
            rectF.set(leftBound, topBound, rightBound, bottomBound)
            startAngle = 270f + arcIndentAngle
        }
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        super.onLayout(changed, left, top, right, bottom)
        updateArcGeometry()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                isFlicking = false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - startX
                val dy = event.rawY - startY
                val dist = hypot(dx, dy)

                if (dist > touchSlop) {
                    val isVerticalEdge = abs(dy) > abs(dx) * 1.25f
                    val isInward = if (onRight) {
                        dx < -touchSlop * 0.45f
                    } else {
                        dx > touchSlop * 0.45f
                    }

                    if (settings.isQuickBallFlickToSelect() && (isFlicking || (isInward && !isVerticalEdge))) {
                        if (!isFlicking) {
                            isFlicking = true
                            listener?.onFlickStart()
                        }
                        listener?.onFlickMove(event.rawX, event.rawY)
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isFlicking) {
                    isFlicking = false
                    listener?.onFlickEnd()
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isFlicking) {
                    isFlicking = false
                    listener?.onFlickEnd()
                    return true
                }
            }
        }

        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gestureDetector.cleanup()
    }
}
