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
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.Xfermode
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

    var isDarkTheme: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                resolveColors()
                if (activeSlot == GlanceSlot.MEDIA && rawArtworkPaletteColor != null) {
                    paletteMediaColor = getThemeAdjustedColor(rawArtworkPaletteColor!!)
                    animateColorChange(paletteMediaColor!!)
                } else {
                    animateColorChange(defaultMaterialYouColor)
                }
            }
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

    private var rawArtworkPaletteColor: Int? = null

    var mediaArtworkBitmap: Bitmap? = null
        set(value) {
            field = value
            if (value != null) {
                Palette.from(value).generate { palette ->
                    val color = extractPaletteAccent(palette)
                    rawArtworkPaletteColor = color
                    val adjustedColor = getThemeAdjustedColor(color)
                    paletteMediaColor = adjustedColor
                    if (activeSlot == GlanceSlot.MEDIA) {
                        animateColorChange(adjustedColor)
                    }
                }
            } else {
                rawArtworkPaletteColor = null
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

    private val artworkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    // Bitmaps
    private var flashlightIcon: Bitmap? = null
    private var calendarIcon: Bitmap? = null
    private var mediaIcon: Bitmap? = null
    private var timeIcon: Bitmap? = null

    private val textBounds = Rect()
    private val chipRect = RectF()
    private val marqueeClipPath = Path()
    private val marqueeFadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    init {
        resolveColors()
        loadIcons()
    }

    private fun resolveColors() {
        defaultMaterialYouColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDarkTheme) {
                ContextCompat.getColor(context, android.R.color.system_accent1_200)
            } else {
                ContextCompat.getColor(context, android.R.color.system_accent1_600)
            }
        } else {
            if (isDarkTheme) Color.parseColor("#81C784") else Color.parseColor("#2E7D32")
        }
        currentPillColor = defaultMaterialYouColor
        updateTextColor(currentPillColor)
    }

    private fun extractPaletteAccent(palette: Palette?): Int {
        val dominantSwatch = palette?.dominantSwatch
        val vibrantSwatch = palette?.vibrantSwatch
        val lightVibrantSwatch = palette?.lightVibrantSwatch
        val darkVibrantSwatch = palette?.darkVibrantSwatch
        val mutedSwatch = palette?.mutedSwatch
        val lightMutedSwatch = palette?.lightMutedSwatch
        val darkMutedSwatch = palette?.darkMutedSwatch

        return if (isDarkTheme) {
            vibrantSwatch?.rgb
                ?: lightVibrantSwatch?.rgb
                ?: dominantSwatch?.rgb
                ?: mutedSwatch?.rgb
                ?: lightMutedSwatch?.rgb
                ?: Color.WHITE
        } else {
            darkVibrantSwatch?.rgb
                ?: vibrantSwatch?.rgb
                ?: dominantSwatch?.rgb
                ?: darkMutedSwatch?.rgb
                ?: mutedSwatch?.rgb
                ?: Color.BLACK
        }
    }

    private fun getThemeAdjustedColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        if (isDarkTheme) {
            hsv[1] = (hsv[1] * 0.75f).coerceIn(0.25f, 0.90f)
            hsv[2] = (hsv[2] * 1.25f).coerceIn(0.85f, 1.0f)
        } else {
            hsv[1] = (hsv[1] * 1.25f).coerceIn(0.65f, 1.0f)
            hsv[2] = (hsv[2] * 0.60f).coerceIn(0.20f, 0.55f)
        }
        return Color.HSVToColor(hsv)
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
            currentTextColor = if (isDarkTheme) Color.WHITE else Color.BLACK
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
        val isArtwork = activeSlot == GlanceSlot.MEDIA && mediaArtworkBitmap != null
        val iconSize = if (isArtwork) 22f * density else 14f * density
        val paddingLeft = if (isArtwork) 1f * density else 8f * density
        val paddingRight = 10f * density
        val spacing = if (isArtwork) 5f * density else 6f * density

        val maxAllowedWidth = maxWidthDp * density
        val calculated = paddingLeft + iconSize + spacing + textWidth + paddingRight
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
        val isArtwork = activeSlot == GlanceSlot.MEDIA && mediaArtworkBitmap != null
        val icon = getActiveIcon()
        val iconSize = if (isArtwork) 22f * density else 14f * density
        val paddingLeft = if (isArtwork) 1f * density else 8f * density
        val iconLeft = left + paddingLeft
        val iconTop = glanceCenterY - iconSize / 2f

        iconPaint.alpha = alphaInt
        artworkPaint.alpha = alphaInt

        // Draw Text
        val text = getActiveText()
        if (text.isNotBlank()) {
            val spacing = if (isArtwork) 5f * density else 6f * density
            val textLeft = iconLeft + iconSize + spacing
            val textRight = if (useBackgroundPill) right else right - 8f * density
            val maxTextWidth = (textRight - textLeft).coerceAtLeast(0f)

            checkAndStartMarquee(text, maxTextWidth)

            textPaint.color = currentTextColor
            textPaint.alpha = alphaInt

            textPaint.getTextBounds(text, 0, text.length, textBounds)
            val textY = glanceCenterY - textBounds.exactCenterY()

            if (isMarqueeNeeded) {
                val fadeWidth = 12f * density
                val marqueeBounds = RectF(textLeft, top, textRight, bottom)
                val saveLayerCount = canvas.saveLayer(marqueeBounds, null)

                if (useBackgroundPill) {
                    marqueeClipPath.reset()
                    val radii = floatArrayOf(
                        0f, 0f,
                        cornerRadius, cornerRadius,
                        cornerRadius, cornerRadius,
                        0f, 0f
                    )
                    marqueeClipPath.addRoundRect(
                        RectF(textLeft, top, right, bottom),
                        radii,
                        Path.Direction.CW
                    )
                    canvas.clipPath(marqueeClipPath)
                } else {
                    canvas.clipRect(textLeft, top, textRight, bottom)
                }

                val marqueeGap = 28f * density
                val textWidth = textPaint.measureText(text)
                val x1 = textLeft - marqueeOffset
                val x2 = x1 + textWidth + marqueeGap
                canvas.drawText(text, x1, textY, textPaint)
                canvas.drawText(text, x2, textY, textPaint)

                // fade
                marqueeFadePaint.shader = LinearGradient(
                    textLeft, 0f, textLeft + fadeWidth, 0f,
                    Color.TRANSPARENT, Color.BLACK,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(textLeft, top, textLeft + fadeWidth, bottom, marqueeFadePaint)

                canvas.restoreToCount(saveLayerCount)
            } else {
                canvas.drawText(text, textLeft, textY, textPaint)
            }
        }

        if (icon != null) {
            if (isArtwork) {
                val artworkRadius = iconSize / 2f
                val artworkCenterX = iconLeft + artworkRadius
                val artworkCenterY = glanceCenterY

                val shader = BitmapShader(icon, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                val matrix = android.graphics.Matrix()
                val scale = iconSize / icon.width.coerceAtMost(icon.height).toFloat()
                val dx = iconLeft - (icon.width * scale - iconSize) / 2f
                val dy = iconTop - (icon.height * scale - iconSize) / 2f
                matrix.setScale(scale, scale)
                matrix.postTranslate(dx, dy)
                shader.setLocalMatrix(matrix)

                artworkPaint.shader = shader
                canvas.drawCircle(artworkCenterX, artworkCenterY, artworkRadius, artworkPaint)
            } else {
                iconPaint.colorFilter = PorterDuffColorFilter(currentTextColor, PorterDuff.Mode.SRC_IN)
                val dstRect = RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
                canvas.drawBitmap(icon, null, dstRect, iconPaint)
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

