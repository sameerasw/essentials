/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlay
 * File: DashOverlay.kt
 * Description: Utility helper for DashOverlay.kt.
 */

package com.sameerasw.essentials.utils.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import com.sameerasw.essentials.domain.model.DashConfig

/**
 * Builds and drives the edge dash overlay: a glowing dash that runs from the top centre
 * down both sides of the stroke and meets again at the bottom centre.
 */
object DashOverlay {
    private const val VIEW_TAG = "dash_view"

    fun createOverlay(
        context: Context,
        color: Int,
        cornerRadiusDp: Float,
        config: DashConfig,
        showBackground: Boolean,
    ): FrameLayout {
        val overlay = FrameLayout(context)
        if (showBackground) {
            overlay.setBackgroundColor(Color.BLACK)
        }

        overlay.addView(
            DashGlowView(context, color, cornerRadiusDp, config).apply {
                tag = VIEW_TAG
                layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            },
        )

        return overlay
    }

    fun pulse(
        view: View,
        maxPulses: Int,
        durationMillis: Long,
        onAnimationEnd: (() -> Unit)? = null,
    ) {
        view.alpha = 1f
        val dashView = (view as? ViewGroup)?.findViewWithTag<View>(VIEW_TAG) as? DashGlowView
        if (dashView == null) {
            onAnimationEnd?.invoke()
            return
        }

        val pulses = maxPulses.coerceAtLeast(1)
        var completed = 0

        fun startPulse() {
            if (completed >= pulses) {
                onAnimationEnd?.invoke()
                return
            }
            completed++

            dashView.progress = 0f

            ValueAnimator
                .ofFloat(0f, 1f)
                .apply {
                    duration = durationMillis
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { anim -> dashView.progress = anim.animatedValue as Float }
                    addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                dashView.progress = 0f
                                startPulse()
                            }
                        },
                    )
                }.start()
        }

        startPulse()
    }
}

internal class DashGlowView(
    context: Context,
    private val color: Int,
    private val cornerRadiusDp: Float,
    private val config: DashConfig,
) : View(context) {
    private val density = context.resources.displayMetrics.density
    private val strokePx = config.thickness * density
    private val glowBlurPx = density * BASE_BLUR_DP * config.glow
    private val barBlurPx = glowBlurPx * BAR_BLUR_SCALE
    private val isGlowing = glowBlurPx >= MIN_BLUR_PX

    private val glowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = strokePx
            color = this@DashGlowView.color
            if (glowBlurPx >= MIN_BLUR_PX) {
                maskFilter = BlurMaskFilter(glowBlurPx, BlurMaskFilter.Blur.NORMAL)
            }
        }

    private val barPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = strokePx
            color = this@DashGlowView.color
            if (barBlurPx >= MIN_BLUR_PX) {
                maskFilter = BlurMaskFilter(barBlurPx, BlurMaskFilter.Blur.NORMAL)
            }
        }

    private val tipPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = strokePx * TIP_WIDTH_FRACTION
            color = blendWithWhite(this@DashGlowView.color, TIP_WHITE_MIX)
        }

    private val rightPath = Path()
    private val leftPath = Path()
    private val rightMeasure = PathMeasure()
    private val leftMeasure = PathMeasure()
    private val segment = Path()
    private val cornerRect = RectF()
    private var pathLength = 0f

    var progress: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    init {
        if (isGlowing) {
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildPaths(w.toFloat(), h.toFloat())
    }

    private fun buildPaths(
        w: Float,
        h: Float,
    ) {
        if (w <= 0f || h <= 0f) return

        val inset = strokePx / 2f
        val left = inset
        val top = inset
        val right = w - inset
        val bottom = h - inset
        if (right <= left || bottom <= top) return

        val radius = (cornerRadiusDp * density).coerceAtMost(minOf(right - left, bottom - top) / 2f)
        val centerX = w / 2f

        rightPath.reset()
        rightPath.moveTo(centerX, top)
        rightPath.lineTo(right - radius, top)
        cornerRect.set(right - 2 * radius, top, right, top + 2 * radius)
        rightPath.arcTo(cornerRect, -90f, 90f, false)
        rightPath.lineTo(right, bottom - radius)
        cornerRect.set(right - 2 * radius, bottom - 2 * radius, right, bottom)
        rightPath.arcTo(cornerRect, 0f, 90f, false)
        rightPath.lineTo(centerX, bottom)

        leftPath.reset()
        leftPath.moveTo(centerX, top)
        leftPath.lineTo(left + radius, top)
        cornerRect.set(left, top, left + 2 * radius, top + 2 * radius)
        leftPath.arcTo(cornerRect, -90f, -90f, false)
        leftPath.lineTo(left, bottom - radius)
        cornerRect.set(left, bottom - 2 * radius, left + 2 * radius, bottom)
        leftPath.arcTo(cornerRect, 180f, -90f, false)
        leftPath.lineTo(centerX, bottom)

        rightMeasure.setPath(rightPath, false)
        leftMeasure.setPath(leftPath, false)
        pathLength = rightMeasure.length
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pathLength <= 0f || progress <= 0f) return

        val barLength = pathLength * config.lengthFraction
        val glowLength = barLength * config.glowLength

        val head = progress * (pathLength + glowLength)
        val headAt = head.coerceIn(0f, pathLength)
        val barAt = (head - barLength).coerceIn(0f, pathLength)
        val glowAt = (head - glowLength).coerceIn(0f, pathLength)

        val alpha = edgeFade(progress)
        if (alpha <= 0.01f) return

        if (headAt - glowAt > 1f) {
            glowPaint.alpha = toAlphaByte(alpha * GLOW_ALPHA)
            drawSlice(canvas, rightMeasure, glowAt, headAt, glowPaint)
            drawSlice(canvas, leftMeasure, glowAt, headAt, glowPaint)
        }

        val barVisible = headAt - barAt
        if (barVisible <= 1f) return

        barPaint.alpha = toAlphaByte(alpha)
        drawSlice(canvas, rightMeasure, barAt, headAt, barPaint)
        drawSlice(canvas, leftMeasure, barAt, headAt, barPaint)

        val tipAt = (headAt - barVisible * TIP_LENGTH_FRACTION).coerceAtLeast(barAt)
        tipPaint.alpha = toAlphaByte(alpha)
        drawSlice(canvas, rightMeasure, tipAt, headAt, tipPaint)
        drawSlice(canvas, leftMeasure, tipAt, headAt, tipPaint)
    }

    private fun drawSlice(
        canvas: Canvas,
        measure: PathMeasure,
        start: Float,
        end: Float,
        paint: Paint,
    ) {
        segment.reset()
        if (measure.getSegment(start, end, segment, true)) {
            canvas.drawPath(segment, paint)
        }
    }

    private companion object {
        const val TIP_LENGTH_FRACTION = 0.22f
        const val TIP_WIDTH_FRACTION = 0.45f
        const val TIP_WHITE_MIX = 0.4f

        const val BASE_BLUR_DP = 12f

        const val BAR_BLUR_SCALE = 0.18f
        const val MIN_BLUR_PX = 0.5f

        const val GLOW_ALPHA = 0.7f

        const val FADE_IN_UNTIL = 0.06f
        const val FADE_OUT_FROM = 0.88f

        fun edgeFade(progress: Float): Float =
            when {
                progress < FADE_IN_UNTIL -> progress / FADE_IN_UNTIL
                progress > FADE_OUT_FROM -> ((1f - progress) / (1f - FADE_OUT_FROM)).coerceAtLeast(0f)
                else -> 1f
            }

        fun toAlphaByte(fraction: Float): Int = (fraction * 255f).toInt().coerceIn(0, 255)

        fun blendWithWhite(
            color: Int,
            fraction: Float,
        ): Int =
            Color.rgb(
                (Color.red(color) + (255 - Color.red(color)) * fraction).toInt(),
                (Color.green(color) + (255 - Color.green(color)) * fraction).toInt(),
                (Color.blue(color) + (255 - Color.blue(color)) * fraction).toInt(),
            )
    }
}
