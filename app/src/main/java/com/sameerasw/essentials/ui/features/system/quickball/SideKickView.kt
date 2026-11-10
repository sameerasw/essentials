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

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

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
        val colors = QuickBallTheme.resolveColors(context, settings)
        val pillColor = colors.pillColor
        val idleAlpha = settings.getQuickBallIdleAlpha().coerceIn(0.2f, 1f)
        val alphaInt = (idleAlpha * 255).roundToInt()

        paint.color = (pillColor and 0x00FFFFFF) or (alphaInt shl 24)
        paint.strokeWidth = (settings.getQuickBallPillThickness() * density)
        canvas.drawArc(rectF, startAngle, sweepAngle, false, paint)
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
                    val isInward = if (onRight) {
                        dx < -touchSlop && abs(dx) > abs(dy) * 0.45f
                    } else {
                        dx > touchSlop && abs(dx) > abs(dy) * 0.45f
                    }

                    if (settings.isQuickBallFlickToSelect() && (isFlicking || isInward)) {
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
