/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlay
 * File: RippleGlitterView.kt
 * Description: Utility helper for RippleGlitterView.kt.
 */

package com.sameerasw.essentials.utils.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.WindowInsets
import com.sameerasw.essentials.domain.model.NotificationLightingRipplePosition
import com.sameerasw.essentials.domain.model.RippleConfig
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

internal class RippleGlitterView(
    context: Context,
    private val color: Int,
    private val config: RippleConfig,
) : View(context) {
    private val density = context.resources.displayMetrics.density

    private var originX = 0f
    private var originY = 0f
    private var maxRadius = 0f

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val waveMatrix = Matrix()
    private val waveShader: RadialGradient

    private val sparklePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = blendWithWhite(this@RippleGlitterView.color, SPARKLE_WHITE_MIX)
        }

    private val sparklePath =
        Path().apply {
            moveTo(0f, -1f)
            quadTo(0.16f, -0.16f, 1f, 0f)
            quadTo(0.16f, 0.16f, 0f, 1f)
            quadTo(-0.16f, 0.16f, -1f, 0f)
            quadTo(-0.16f, -0.16f, 0f, -1f)
            close()
        }

    private var sparkleCount = 0
    private var cosAngle = FloatArray(0)
    private var sinAngle = FloatArray(0)
    private var distanceFactor = FloatArray(0)
    private var sizePx = FloatArray(0)
    private var phase = FloatArray(0)
    private var rotation = FloatArray(0)
    private var twinkleRate = FloatArray(0)
    private var peakAlpha = FloatArray(0)

    var progress: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    init {
        val innerStop = (1f - INNER_STOP_PER_WAVE * config.waveSize).coerceIn(0.02f, 0.95f)
        val edgeStop =
            (1f - EDGE_STOP_PER_WAVE * config.waveSize).coerceIn(innerStop + 0.01f, 0.99f)
        val interiorAlpha =
            (INTERIOR_ALPHA_BASE + INTERIOR_ALPHA_PER_WAVE * (config.waveSize - 1f))
                .coerceIn(0.05f, 0.32f)

        waveShader =
            RadialGradient(
                0f,
                0f,
                1f,
                intArrayOf(
                    withAlpha(color, 0f),
                    withAlpha(color, interiorAlpha * config.opacity),
                    withAlpha(color, EDGE_ALPHA * config.opacity),
                    withAlpha(color, 0f),
                ),
                floatArrayOf(0f, innerStop, edgeStop, 1f),
                Shader.TileMode.CLAMP,
            )
        wavePaint.shader = waveShader

        buildSparkles()
    }

    fun newSeed() {
        buildSparkles()
    }

    private fun buildSparkles() {
        val count = config.effectiveSparkleCount
        if (count != sparkleCount) {
            sparkleCount = count
            cosAngle = FloatArray(count)
            sinAngle = FloatArray(count)
            distanceFactor = FloatArray(count)
            sizePx = FloatArray(count)
            phase = FloatArray(count)
            rotation = FloatArray(count)
            twinkleRate = FloatArray(count)
            peakAlpha = FloatArray(count)
        }

        val sizeScale = density * config.sparkleSize
        for (i in 0 until count) {
            val angle = Random.nextFloat() * TWO_PI
            cosAngle[i] = cos(angle)
            sinAngle[i] = sin(angle)
            distanceFactor[i] = 0.45f + Random.nextFloat() * 0.58f
            sizePx[i] = (1.2f + Random.nextFloat() * 3.4f) * sizeScale
            phase[i] = Random.nextFloat() * TWO_PI
            rotation[i] = Random.nextFloat() * 90f
            twinkleRate[i] = (2f + Random.nextFloat() * 4f) * TWO_PI
            peakAlpha[i] = 0.45f + Random.nextFloat() * 0.55f
        }
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGeometry(w, h)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (width > 0 && height > 0) {
            updateGeometry(width, height)
        }
        return super.onApplyWindowInsets(insets)
    }

    private fun updateGeometry(
        w: Int,
        h: Int,
    ) {
        resolveOrigin(w, h)
        maxRadius =
            hypot(
                maxOf(originX, w - originX),
                maxOf(originY, h - originY),
            )
    }

    private fun resolveOrigin(
        w: Int,
        h: Int,
    ) {
        when (config.position) {
            NotificationLightingRipplePosition.CENTER -> {
                originX = w / 2f
                originY = h / 2f
            }

            NotificationLightingRipplePosition.MANUAL -> {
                originX = w * (config.positionX / 100f)
                originY = h * (config.positionY / 100f)
            }

            NotificationLightingRipplePosition.AUTO -> {
                val cutout = topCutoutCenter()
                if (cutout != null) {
                    originX = cutout.first
                    originY = cutout.second
                } else {
                    originX = w / 2f
                    originY = FALLBACK_ORIGIN_DP * density
                }
            }
        }
    }

    private fun topCutoutCenter(): Pair<Float, Float>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return try {
            rootWindowInsets
                ?.displayCutout
                ?.boundingRects
                ?.filter { !it.isEmpty }
                ?.minByOrNull { it.top }
                ?.let { it.exactCenterX() to it.exactCenterY() }
        } catch (_: Exception) {
            null
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progress <= 0f || maxRadius <= 0f) return

        val radius = progress * maxRadius
        if (radius < 1f) return

        val fadeIn = (progress / FADE_IN_FRACTION).coerceAtMost(1f)
        val fade = fadeIn * fadeOutCurve(1f - progress)
        if (fade <= MIN_VISIBLE_ALPHA) return

        waveMatrix.setScale(radius, radius)
        waveMatrix.postTranslate(originX, originY)
        waveShader.setLocalMatrix(waveMatrix)
        wavePaint.shader = waveShader
        wavePaint.alpha = toAlphaByte(fade)
        canvas.drawCircle(originX, originY, radius, wavePaint)

        if (sparkleCount == 0) return

        val sparkleBase = fade * config.opacity

        for (i in 0 until sparkleCount) {
            val distance = radius * distanceFactor[i]
            if (distance > maxRadius) continue

            val twinkle = abs(sin(progress * twinkleRate[i] + phase[i]))
            val sparkleAlpha = sparkleBase * twinkle * peakAlpha[i]
            if (sparkleAlpha <= MIN_VISIBLE_ALPHA) continue

            sparklePaint.alpha = toAlphaByte(sparkleAlpha)
            val scale = sizePx[i] * (0.55f + 0.45f * twinkle)

            canvas.save()
            canvas.translate(
                originX + cosAngle[i] * distance,
                originY + sinAngle[i] * distance,
            )
            canvas.rotate(rotation[i])
            canvas.scale(scale, scale)
            canvas.drawPath(sparklePath, sparklePaint)
            canvas.restore()
        }
    }

    private companion object {
        const val TWO_PI = 6.2831855f
        const val FADE_IN_FRACTION = 0.08f
        const val MIN_VISIBLE_ALPHA = 0.01f
        const val EDGE_ALPHA = 0.55f
        const val SPARKLE_WHITE_MIX = 0.45f
        const val FALLBACK_ORIGIN_DP = 24f

        const val INNER_STOP_PER_WAVE = 0.40f
        const val EDGE_STOP_PER_WAVE = 0.07f
        const val INTERIOR_ALPHA_BASE = 0.10f
        const val INTERIOR_ALPHA_PER_WAVE = 0.04f

        fun fadeOutCurve(x: Float): Float {
            val clamped = x.coerceIn(0f, 1f)
            val root = sqrt(clamped)
            return root + (clamped - root) * 0.1f
        }

        fun toAlphaByte(fraction: Float): Int = (fraction * 255f).toInt().coerceIn(0, 255)

        fun withAlpha(
            color: Int,
            fraction: Float,
        ): Int =
            Color.argb(
                toAlphaByte(fraction),
                Color.red(color),
                Color.green(color),
                Color.blue(color),
            )

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
