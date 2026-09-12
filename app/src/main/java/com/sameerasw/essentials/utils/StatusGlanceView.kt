/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays
 * File: StatusGlanceView.kt
 * Description: Status Glance ambient chip indicator canvas view for status bar display.
 */

package com.sameerasw.essentials.utils

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.sameerasw.essentials.R

class StatusGlanceView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density

    enum class GlanceSlot {
        NONE,
        FLASHLIGHT,
        CALENDAR_URGENT,
        MEDIA,
        CALENDAR_TODAY,
        TIME
    }

    var glanceCenterX: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var glanceCenterY: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var showFlashlight: Boolean = true
        set(value) {
            field = value
            reevaluateSlot()
        }

    var showCalendar: Boolean = true
        set(value) {
            field = value
            reevaluateSlot()
        }

    var showMedia: Boolean = true
        set(value) {
            field = value
            reevaluateSlot()
        }

    var showTime: Boolean = true
        set(value) {
            field = value
            reevaluateSlot()
        }

    var useBackgroundPill: Boolean = false
        set(value) {
            field = value
            updateTextColor(currentPillColor)
            invalidate()
        }

    var maxWidthDp: Float = 180f
        set(value) {
            field = value
            reevaluateSlot()
        }

    var fontSize: Float = 13f
        set(value) {
            field = value
            textPaint.textSize = value * density
            reevaluateSlot()
        }

    // State data
    var isFlashlightOn: Boolean = false
        set(value) {
            field = value
            reevaluateSlot()
        }

    var isMediaPlaying: Boolean = false
        set(value) {
            field = value
            reevaluateSlot()
        }

    var mediaTitle: String = ""
        set(value) {
            field = value
            reevaluateSlot()
        }

    var mediaArtist: String = ""
        set(value) {
            field = value
            reevaluateSlot()
        }

    var mediaArtworkBitmap: Bitmap? = null
        set(value) {
            field = value
            if (value != null) {
                Palette.from(value).generate { palette ->
                    palette?.dominantSwatch?.rgb?.let { color ->
                        paletteMediaColor = color
                        if (activeSlot == GlanceSlot.MEDIA) {
                            animateColorChange(color)
                        }
                    }
                }
            } else {
                paletteMediaColor = null
                if (activeSlot == GlanceSlot.MEDIA) {
                    animateColorChange(defaultMaterialYouColor)
                }
            }
            reevaluateSlot()
        }

    var nextEventTitle: String = ""
        set(value) {
            field = value
            reevaluateSlot()
        }

    var nextEventTimeMillis: Long = 0L
        set(value) {
            field = value
            reevaluateSlot()
        }

    var isEventToday: Boolean = false
        set(value) {
            field = value
            reevaluateSlot()
        }

    var currentTimeString: String = ""
        set(value) {
            field = value
            reevaluateSlot()
        }

    // Active Slot
    var activeSlot: GlanceSlot = GlanceSlot.NONE
        private set

    // Colors
    private var defaultMaterialYouColor: Int = Color.parseColor("#388E3C")
    private var paletteMediaColor: Int? = null
    private var currentPillColor: Int = defaultMaterialYouColor
    private var currentTextColor: Int = Color.WHITE

    // Animated values
    private var currentContentWidth: Float = 0f
    private var targetContentWidth: Float = 0f
    private var widthAnimator: ValueAnimator? = null

    private var slotAlpha: Float = 1f
    private var alphaAnimator: ValueAnimator? = null
    private var colorAnimator: ValueAnimator? = null

    // Marquee values
    private var marqueeOffset: Float = 0f
    private var marqueeAnimator: ValueAnimator? = null
    private var isMarqueeNeeded: Boolean = false
    private var lastMarqueeText: String = ""
    private var lastMaxTextWidth: Float = 0f

    // Paints
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    // Bitmaps
    private var flashlightIcon: Bitmap? = null
    private var calendarIcon: Bitmap? = null
    private var mediaIcon: Bitmap? = null
    private var timeIcon: Bitmap? = null

    private val textBounds = Rect()
    private val chipRect = RectF()

    init {
        resolveColors()
        loadIcons()
    }

    private fun resolveColors() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val primaryColor = ContextCompat.getColor(context, android.R.color.system_accent1_600)
            defaultMaterialYouColor = primaryColor
        } else {
            defaultMaterialYouColor = Color.parseColor("#388E3C")
        }
        currentPillColor = defaultMaterialYouColor
        updateTextColor(currentPillColor)
    }

    private fun loadIcons() {
        flashlightIcon = getBitmapFromVector(R.drawable.rounded_flashlight_on_24, (14 * density).toInt())
        calendarIcon = getBitmapFromVector(R.drawable.rounded_calendar_today_24, (14 * density).toInt())
        mediaIcon = getBitmapFromVector(R.drawable.rounded_motion_play_24, (14 * density).toInt())
        timeIcon = getBitmapFromVector(R.drawable.rounded_schedule_24, (14 * density).toInt())
    }

    private fun getBitmapFromVector(drawableId: Int, sizePx: Int): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun reevaluateSlot() {
        val now = System.currentTimeMillis()
        val isEventWithin15Min = isEventToday && (nextEventTimeMillis - now) in 0..(15 * 60 * 1000L)

        val newSlot = when {
            showFlashlight && isFlashlightOn -> GlanceSlot.FLASHLIGHT
            showCalendar && isEventWithin15Min && nextEventTitle.isNotBlank() -> GlanceSlot.CALENDAR_URGENT
            showMedia && isMediaPlaying -> GlanceSlot.MEDIA
            showCalendar && isEventToday && nextEventTitle.isNotBlank() -> GlanceSlot.CALENDAR_TODAY
            showTime && currentTimeString.isNotBlank() -> GlanceSlot.TIME
            else -> GlanceSlot.NONE
        }

        if (newSlot != activeSlot) {
            transitionToSlot(newSlot)
        } else {
            targetContentWidth = calculateTargetWidth()
            animateWidth()
            invalidate()
        }
    }

    private fun transitionToSlot(newSlot: GlanceSlot) {
        stopMarquee()
        if (activeSlot == GlanceSlot.NONE) {
            activeSlot = newSlot
            val targetColor = if (newSlot == GlanceSlot.MEDIA && paletteMediaColor != null) {
                paletteMediaColor!!
            } else {
                defaultMaterialYouColor
            }
            currentPillColor = targetColor
            updateTextColor(targetColor)
            targetContentWidth = calculateTargetWidth()
            currentContentWidth = targetContentWidth
            slotAlpha = 1f
            invalidate()
            return
        }

        alphaAnimator?.cancel()
        alphaAnimator = ValueAnimator.ofFloat(slotAlpha, 0f).apply {
            duration = 150
            addUpdateListener {
                slotAlpha = it.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    activeSlot = newSlot
                    val targetColor = if (newSlot == GlanceSlot.MEDIA && paletteMediaColor != null) {
                        paletteMediaColor!!
                    } else {
                        defaultMaterialYouColor
                    }
                    currentPillColor = targetColor
                    updateTextColor(targetColor)
                    targetContentWidth = calculateTargetWidth()
                    animateWidth()

                    alphaAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 200
                        addUpdateListener { anim ->
                            slotAlpha = anim.animatedValue as Float
                            invalidate()
                        }
                        start()
                    }
                }
            })
            start()
        }
    }

    private fun updateTextColor(bgColor: Int) {
        if (!useBackgroundPill) {
            currentTextColor = Color.WHITE
            return
        }
        val luminance = (0.299 * Color.red(bgColor) + 0.587 * Color.green(bgColor) + 0.114 * Color.blue(bgColor)) / 255.0
        currentTextColor = if (luminance > 0.5) Color.BLACK else Color.WHITE
    }

    private fun animateColorChange(targetColor: Int) {
        colorAnimator?.cancel()
        colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), currentPillColor, targetColor).apply {
            duration = 300
            addUpdateListener {
                currentPillColor = it.animatedValue as Int
                updateTextColor(currentPillColor)
                invalidate()
            }
            start()
        }
    }

    private fun animateWidth() {
        widthAnimator?.cancel()
        widthAnimator = ValueAnimator.ofFloat(currentContentWidth, targetContentWidth).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                currentContentWidth = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun checkAndStartMarquee(text: String, maxTextWidth: Float) {
        val textWidth = textPaint.measureText(text)
        val needed = textWidth > maxTextWidth && maxTextWidth > 0f

        if (needed) {
            if (!isMarqueeNeeded || text != lastMarqueeText || kotlin.math.abs(maxTextWidth - lastMaxTextWidth) > 1f) {
                isMarqueeNeeded = true
                lastMarqueeText = text
                lastMaxTextWidth = maxTextWidth
                marqueeAnimator?.cancel()
                marqueeOffset = 0f

                val marqueeGap = 28f * density
                val totalDistance = textWidth + marqueeGap
                val speedDpPerSec = 30f
                val durationMs = ((totalDistance / density) / speedDpPerSec * 1000L).toLong().coerceAtLeast(2000L)

                marqueeAnimator = ValueAnimator.ofFloat(0f, totalDistance).apply {
                    duration = durationMs
                    interpolator = LinearInterpolator()
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.RESTART
                    startDelay = 1200L
                    addUpdateListener {
                        marqueeOffset = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
        } else {
            if (isMarqueeNeeded || text != lastMarqueeText) {
                stopMarquee()
                lastMarqueeText = text
                lastMaxTextWidth = maxTextWidth
            }
        }
    }

    private fun stopMarquee() {
        isMarqueeNeeded = false
        lastMarqueeText = ""
        lastMaxTextWidth = 0f
        marqueeAnimator?.cancel()
        marqueeAnimator = null
        marqueeOffset = 0f
    }

    private fun getActiveText(): String {
        return when (activeSlot) {
            GlanceSlot.FLASHLIGHT -> context.getString(R.string.status_glance_slot_flashlight)
            GlanceSlot.CALENDAR_URGENT, GlanceSlot.CALENDAR_TODAY -> nextEventTitle
            GlanceSlot.MEDIA -> {
                if (mediaArtist.isNotBlank()) "$mediaTitle • $mediaArtist" else mediaTitle
            }
            GlanceSlot.TIME -> currentTimeString
            GlanceSlot.NONE -> ""
        }
    }

    private fun getActiveIcon(): Bitmap? {
        return when (activeSlot) {
            GlanceSlot.FLASHLIGHT -> flashlightIcon
            GlanceSlot.CALENDAR_URGENT, GlanceSlot.CALENDAR_TODAY -> calendarIcon
            GlanceSlot.MEDIA -> mediaArtworkBitmap ?: mediaIcon
            GlanceSlot.TIME -> timeIcon
            GlanceSlot.NONE -> null
        }
    }

    private fun calculateTargetWidth(): Float {
        if (activeSlot == GlanceSlot.NONE) return 0f
        val text = getActiveText()
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val textWidth = textBounds.width().toFloat()
        val iconSize = 14f * density
        val padding = 10f * density
        val spacing = 6f * density

        val maxAllowedWidth = maxWidthDp * density
        val calculated = padding * 2 + iconSize + spacing + textWidth
        return calculated.coerceAtMost(maxAllowedWidth)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (activeSlot == GlanceSlot.NONE && currentContentWidth <= 0f) return

        val height = 24f * density
        val width = currentContentWidth
        if (width <= 0f) return

        val left = glanceCenterX
        val top = glanceCenterY - height / 2f
        val right = left + width
        val bottom = glanceCenterY + height / 2f
        val cornerRadius = height / 2f

        chipRect.set(left, top, right, bottom)

        val alphaInt = (slotAlpha * 255).toInt().coerceIn(0, 255)

        // Draw background pill if enabled
        if (useBackgroundPill) {
            pillPaint.color = currentPillColor
            pillPaint.alpha = alphaInt
            canvas.drawRoundRect(chipRect, cornerRadius, cornerRadius, pillPaint)
        }

        // Draw Icon and Text
        val icon = getActiveIcon()
        val iconSize = 14f * density
        val paddingLeft = 8f * density
        val iconLeft = left + paddingLeft
        val iconTop = glanceCenterY - iconSize / 2f

        iconPaint.alpha = alphaInt
        if (icon != null) {
            if (activeSlot == GlanceSlot.MEDIA && mediaArtworkBitmap != null) {
                val dstRect = RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                iconPaint.colorFilter = null
                canvas.drawBitmap(icon, null, dstRect, iconPaint)
            } else {
                iconPaint.colorFilter = PorterDuffColorFilter(currentTextColor, PorterDuff.Mode.SRC_IN)
                val dstRect = RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                canvas.drawBitmap(icon, null, dstRect, iconPaint)
            }
        }

        // Draw Text
        val text = getActiveText()
        if (text.isNotBlank()) {
            val textLeft = iconLeft + iconSize + 6f * density
            val textRight = right - 8f * density
            val maxTextWidth = (textRight - textLeft).coerceAtLeast(0f)

            checkAndStartMarquee(text, maxTextWidth)

            textPaint.color = currentTextColor
            textPaint.alpha = alphaInt

            textPaint.getTextBounds(text, 0, text.length, textBounds)
            val textY = glanceCenterY - textBounds.exactCenterY()

            if (isMarqueeNeeded) {
                val saveCount = canvas.save()
                canvas.clipRect(textLeft, top, textRight, bottom)
                val marqueeGap = 28f * density
                val textWidth = textPaint.measureText(text)
                val x1 = textLeft - marqueeOffset
                val x2 = x1 + textWidth + marqueeGap
                canvas.drawText(text, x1, textY, textPaint)
                canvas.drawText(text, x2, textY, textPaint)
                canvas.restoreToCount(saveCount)
            } else {
                canvas.drawText(text, textLeft, textY, textPaint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopMarquee()
        widthAnimator?.cancel()
        alphaAnimator?.cancel()
        colorAnimator?.cancel()
    }
}
