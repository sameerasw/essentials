/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays
 * File: StatusGlanceView.kt
 * Description: Status Glance ambient chip indicator canvas view for status bar display.
 */

package com.sameerasw.essentials.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import android.os.Build
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
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
        DEFAULT
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

    var showBattery: Boolean = false
        set(value) {
            field = value
            reevaluateBatteryState()
        }

    var batteryDisplayMode: String = "icon"
        set(value) {
            field = value
            reevaluateBatteryState()
        }

    var batteryIconSizeDp: Float = 16f
        set(value) {
            if (field != value) {
                field = value
                batteryIconBitmapCache.clear()
                reevaluateBatteryState()
            }
        }

    var showBatteryWhenLow: Boolean = true
        set(value) {
            field = value
            reevaluateBatteryState()
        }

    var showBatteryWhileCharging: Boolean = true
        set(value) {
            field = value
            reevaluateBatteryState()
        }

    var showBatteryWhileFull: Boolean = true
        set(value) {
            field = value
            reevaluateBatteryState()
        }

    var showBatteryOtherwise: Boolean = true
        set(value) {
            field = value
            reevaluateBatteryState()
        }

    var batteryLevel: Int = 100
        set(value) {
            field = value
            reevaluateBatteryState()
        }

    var isBatteryCharging: Boolean = false
        set(value) {
            field = value
            reevaluateBatteryState()
        }

    var isBatteryFull: Boolean = false
        set(value) {
            field = value
            reevaluateBatteryState()
        }

    var isPowerSaveMode: Boolean = false
        set(value) {
            field = value
            reevaluateBatteryState()
        }

    var useBackgroundPill: Boolean = false
        set(value) {
            field = value
            updateTextColor(currentPillColor)
            invalidate()
        }

    var useAlbumArtColors: Boolean = true
        set(value) {
            field = value
            val targetColor = if (value && activeSlot == GlanceSlot.MEDIA && paletteMediaColor != null) {
                paletteMediaColor!!
            } else {
                defaultMaterialYouColor
            }
            animateColorChange(targetColor)
            updateTextColor(targetColor)
            invalidate()
        }

    private fun isPillActive(): Boolean = useBackgroundPill || (useAlbumArtColors && activeSlot == GlanceSlot.MEDIA)

    var maxWidthDp: Float = 180f
        set(value) {
            field = value
            reevaluateSlot()
        }

    var hideWhenFullscreen: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityState()
            }
        }

    var hideInQuickSettings: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityState()
            }
        }

    var hideWhenLocked: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityState()
            }
        }

    var isFullscreen: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityState()
            }
        }

    var isShadeExpanded: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityState()
            }
        }

    var isLocked: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityState()
            }
        }

    var isScreenOff: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityState()
            }
        }

    var isLandscape: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityState()
            }
        }

    var fontSize: Float = 13f
        set(value) {
            field = value
            textPaint.textSize = value * density
            batteryTextPaint.textSize = (value * 0.85f) * density
            reevaluateSlot()
        }

    var isDarkTheme: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                resolveColors()
                if (useAlbumArtColors && activeSlot == GlanceSlot.MEDIA && rawArtworkPaletteColor != null) {
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
                    if (useAlbumArtColors && activeSlot == GlanceSlot.MEDIA) {
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

    // Slot transition state for car meter roll / flip animation
    private var previousSlot: GlanceSlot = GlanceSlot.NONE
    private var previousClockText: String = ""
    private var previousMarqueeText: String = ""
    private var previousIcon: Bitmap? = null
    private var previousIsArtwork: Boolean = false

    private var transitionProgress: Float = 0f
    private var transitionDirection: Int = 1
    private var isTransitioning: Boolean = false
    private var transitionAnimator: ValueAnimator? = null

    // Colors
    private var defaultMaterialYouColor: Int = Color.parseColor("#388E3C")
    private var paletteMediaColor: Int? = null
    private var currentPillColor: Int = defaultMaterialYouColor
    private var currentTextColor: Int = Color.WHITE

    // Animated values
    private var currentContentWidth: Float = 0f
    private var targetContentWidth: Float = 0f
    private var widthAnimator: ValueAnimator? = null

    // Visibility and reveal animation
    private var isVisibilityHidden: Boolean = false
    private var revealProgress: Float = 1f
    private var revealAnimator: ValueAnimator? = null

    // Battery state & animation
    private var isBatteryVisible: Boolean = false
    private var batteryAlpha: Float = 0f
    private var batteryAlphaAnimator: ValueAnimator? = null
    private val batteryIconBitmapCache = mutableMapOf<Int, Bitmap>()

    private var slotAlpha: Float = 1f
    private var alphaAnimator: ValueAnimator? = null
    private var colorAnimator: ValueAnimator? = null

    // Marquee values
    private var marqueeOffset: Float = 0f
    private var marqueeAnimator: ValueAnimator? = null
    private var isMarqueeNeeded: Boolean = false
    private var lastMarqueeText: String = ""
    private var lastMaxTextWidth: Float = 0f

    // Gesture animations
    private var swipeArtworkOffset: Float = 0f
    private var swipeArtworkAnimator: ValueAnimator? = null
    private var animatedTapScale: Float = 1.0f
    private var tapScaleAnimator: ValueAnimator? = null

    // Paints
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 13f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val batteryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = (17f * 0.85f) * density
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

    private val textBounds = Rect()
    private val chipRect = RectF()
    private val containerClipPath = Path()
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

    private fun getSlotPriority(slot: GlanceSlot): Int {
        return when (slot) {
            GlanceSlot.FLASHLIGHT -> 4
            GlanceSlot.CALENDAR_URGENT -> 3
            GlanceSlot.MEDIA -> 2
            GlanceSlot.DEFAULT -> 1
            GlanceSlot.NONE -> 0
        }
    }

    private fun isBatteryConditionMet(): Boolean {
        if (!showBattery) return false
        val isLow = batteryLevel <= 20
        val isCharging = isBatteryCharging
        val isFull = isBatteryFull || batteryLevel >= 100
        val isOtherwise = !isLow && !isCharging && !isFull

        return (isLow && showBatteryWhenLow) ||
            (isCharging && showBatteryWhileCharging) ||
            (isFull && showBatteryWhileFull) ||
            (isOtherwise && showBatteryOtherwise)
    }

    private fun reevaluateBatteryState() {
        val shouldShow = isBatteryConditionMet()
        if (isBatteryVisible != shouldShow) {
            isBatteryVisible = shouldShow
            batteryAlphaAnimator?.cancel()
            val targetAlpha = if (shouldShow) 1f else 0f
            batteryAlphaAnimator = ValueAnimator.ofFloat(batteryAlpha, targetAlpha).apply {
                duration = 260L
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    batteryAlpha = it.animatedValue as Float
                    targetContentWidth = calculateTargetWidth()
                    animateWidth()
                    invalidate()
                }
                start()
            }
        } else {
            targetContentWidth = calculateTargetWidth()
            animateWidth()
            invalidate()
        }
    }

    fun reevaluateSlot() {
        val now = System.currentTimeMillis()
        val isEventWithin15Min = isEventToday && (nextEventTimeMillis - now) in 0..(15 * 60 * 1000L)

        val newSlot = when {
            showFlashlight && isFlashlightOn -> GlanceSlot.FLASHLIGHT
            showCalendar && isEventWithin15Min && nextEventTitle.isNotBlank() -> GlanceSlot.CALENDAR_URGENT
            showMedia && isMediaPlaying -> GlanceSlot.MEDIA
            showTime || (showCalendar && isEventToday && nextEventTitle.isNotBlank()) -> GlanceSlot.DEFAULT
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
            val targetColor = if (useAlbumArtColors && newSlot == GlanceSlot.MEDIA && paletteMediaColor != null) {
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

        if (newSlot == GlanceSlot.NONE) {
            alphaAnimator?.cancel()
            alphaAnimator = ValueAnimator.ofFloat(slotAlpha, 0f).apply {
                duration = 200
                addUpdateListener {
                    slotAlpha = it.animatedValue as Float
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        activeSlot = GlanceSlot.NONE
                        isTransitioning = false
                        currentContentWidth = 0f
                        invalidate()
                    }
                })
                start()
            }
            return
        }

        previousSlot = activeSlot
        val (prevClock, prevMarquee) = getSlotTexts(activeSlot)
        previousClockText = prevClock
        previousMarqueeText = prevMarquee
        previousIcon = getSlotIcon(activeSlot)
        previousIsArtwork = activeSlot == GlanceSlot.MEDIA && mediaArtworkBitmap != null

        val oldPriority = getSlotPriority(activeSlot)
        val newPriority = getSlotPriority(newSlot)
        transitionDirection = if (newPriority >= oldPriority) 1 else -1

        activeSlot = newSlot
        isTransitioning = true

        val targetColor = if (useAlbumArtColors && newSlot == GlanceSlot.MEDIA && paletteMediaColor != null) {
            paletteMediaColor!!
        } else {
            defaultMaterialYouColor
        }

        targetContentWidth = calculateTargetWidth()
        animateWidth()
        animateColorChange(targetColor)

        transitionAnimator?.cancel()
        transitionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 360L
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener {
                transitionProgress = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isTransitioning = false
                    transitionProgress = 0f
                    invalidate()
                }
            })
            start()
        }
    }

    private fun updateTextColor(bgColor: Int) {
        if (!isPillActive()) {
            currentTextColor = if (isDarkTheme) Color.WHITE else Color.BLACK
            return
        }
        val luminance = (0.299 * Color.red(bgColor) + 0.587 * Color.green(bgColor) + 0.114 * Color.blue(bgColor)) / 255.0
        currentTextColor = if (luminance > 0.5) Color.BLACK else Color.WHITE
    }

    private fun animateColorChange(targetColor: Int) {
        colorAnimator?.cancel()
        colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), currentPillColor, targetColor).apply {
            duration = 380L
            interpolator = DecelerateInterpolator()
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
            duration = 320L
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                currentContentWidth = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun checkAndStartMarquee(text: String, maxTextWidth: Float) {
        if (isTransitioning) return

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

    private fun getSlotTexts(slot: GlanceSlot): Pair<String, String> {
        return when (slot) {
            GlanceSlot.FLASHLIGHT -> Pair("", context.getString(R.string.status_glance_slot_flashlight))
            GlanceSlot.CALENDAR_URGENT -> Pair("", nextEventTitle)
            GlanceSlot.MEDIA -> {
                val mediaText = if (mediaArtist.isNotBlank()) "$mediaTitle • $mediaArtist" else mediaTitle
                Pair("", mediaText)
            }
            GlanceSlot.DEFAULT -> {
                val clock = if (showTime) currentTimeString else ""
                val calendar = if (showCalendar && isEventToday && nextEventTitle.isNotBlank()) nextEventTitle else ""
                Pair(clock, calendar)
            }
            GlanceSlot.NONE -> Pair("", "")
        }
    }

    private fun getSlotIcon(slot: GlanceSlot): Bitmap? {
        return when (slot) {
            GlanceSlot.FLASHLIGHT -> flashlightIcon
            GlanceSlot.CALENDAR_URGENT -> calendarIcon
            GlanceSlot.MEDIA -> mediaArtworkBitmap ?: mediaIcon
            GlanceSlot.DEFAULT -> {
                if (showCalendar && isEventToday && nextEventTitle.isNotBlank()) calendarIcon else null
            }
            GlanceSlot.NONE -> null
        }
    }

    private fun getBatteryDrawableId(): Int {
        return when {
            isPowerSaveMode -> R.drawable.battery_android_frame_plus_24px
            isBatteryCharging -> R.drawable.battery_android_frame_bolt_24px
            batteryLevel <= 15 -> R.drawable.battery_android_frame_alert_24px
            batteryLevel <= 10 -> R.drawable.battery_android_0_24px
            batteryLevel <= 25 -> R.drawable.battery_android_frame_1_24px
            batteryLevel <= 40 -> R.drawable.battery_android_frame_2_24px
            batteryLevel <= 60 -> R.drawable.battery_android_frame_3_24px
            batteryLevel <= 75 -> R.drawable.battery_android_frame_4_24px
            batteryLevel <= 90 -> R.drawable.battery_android_frame_5_24px
            else -> R.drawable.battery_android_frame_full_24px
        }
    }

    private fun getBatteryBitmap(sizePx: Int): Bitmap? {
        val drawableId = getBatteryDrawableId()
        return batteryIconBitmapCache.getOrPut(drawableId) {
            getBitmapFromVector(drawableId, sizePx) ?: return null
        }
    }

    private fun calculateTargetWidth(): Float {
        if (activeSlot == GlanceSlot.NONE) return 0f
        val (clockText, marqueeText) = getSlotTexts(activeSlot)
        val icon = getSlotIcon(activeSlot)
        val isArtwork = activeSlot == GlanceSlot.MEDIA && mediaArtworkBitmap != null
        val iconSize = if (icon != null) {
            if (isArtwork) 22f * density else 14f * density
        } else 0f

        val paddingLeft = if (icon != null) {
            if (isArtwork) 1f * density else 8f * density
        } else 10f * density

        val paddingRight = 10f * density
        val iconSpacing = if (icon != null) {
            if (isArtwork) 3f * density else 4f * density
        } else 0f

        var calculated = paddingLeft + iconSize + iconSpacing + paddingRight

        if (clockText.isNotBlank()) {
            val clockWidth = textPaint.measureText(clockText)
            calculated += clockWidth
            if (marqueeText.isNotBlank()) {
                calculated += 4f * density
            }
        }

        if (marqueeText.isNotBlank()) {
            val marqueeWidth = textPaint.measureText(marqueeText)
            calculated += marqueeWidth
        }

        if (isBatteryConditionMet() || batteryAlpha > 0f) {
            val iconSizePx = batteryIconSizeDp * density
            val batteryWidth = when (batteryDisplayMode) {
                "icon" -> iconSizePx
                "percentage" -> batteryTextPaint.measureText("$batteryLevel%")
                "both" -> iconSizePx + 2f * density + batteryTextPaint.measureText("$batteryLevel%")
                else -> iconSizePx
            }
            val batterySpacing = 5f * density
            calculated += (batterySpacing + batteryWidth) * batteryAlpha
        }

        val maxAllowedWidth = maxWidthDp * density
        return calculated.coerceAtMost(maxAllowedWidth)
    }

    fun updateVisibilityState(immediate: Boolean = false) {
        val shouldHide = (isLocked && hideWhenLocked) || isScreenOff || isLandscape || (isFullscreen && hideWhenFullscreen) || (isShadeExpanded && hideInQuickSettings)
        if (isVisibilityHidden == shouldHide && !immediate) return
        isVisibilityHidden = shouldHide

        revealAnimator?.cancel()
        val startVal = revealProgress
        val targetVal = if (shouldHide) 0f else 1f

        if (immediate) {
            revealProgress = targetVal
            invalidate()
            return
        }

        val durationMs = if (shouldHide) 240L else 380L

        revealAnimator = ValueAnimator.ofFloat(startVal, targetVal).apply {
            duration = durationMs
            interpolator = if (shouldHide) {
                LinearInterpolator()
            } else {
                DecelerateInterpolator(1.8f)
            }
            addUpdateListener {
                revealProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setSwipeArtworkOffset(offset: Float) {
        swipeArtworkAnimator?.cancel()
        swipeArtworkOffset = offset.coerceAtLeast(0f)
        invalidate()
    }

    fun releaseSwipeArtwork() {
        swipeArtworkAnimator?.cancel()
        val start = swipeArtworkOffset
        if (start == 0f) return
        swipeArtworkAnimator = ValueAnimator.ofFloat(start, 0f).apply {
            duration = 320L
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener {
                swipeArtworkOffset = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun triggerTapAnimation() {
        tapScaleAnimator?.cancel()
        tapScaleAnimator = ValueAnimator.ofFloat(0.92f, 1.0f).apply {
            duration = 300L
            interpolator = OvershootInterpolator(2.0f)
            addUpdateListener {
                animatedTapScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun drawSlotContent(
        canvas: Canvas,
        clockText: String,
        marqueeText: String,
        icon: Bitmap?,
        isArtwork: Boolean,
        alphaMultiplier: Float,
        offsetY: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        cornerRadius: Float,
        chipHeight: Float,
        revealTextAlpha: Float = 1f,
        revealIconAlpha: Float = 1f,
        expandPhase: Float = 1f
    ) {
        if (alphaMultiplier <= 0f) return

        val combinedTextAlpha = (slotAlpha * alphaMultiplier * revealTextAlpha * 255).toInt().coerceIn(0, 255)
        val combinedIconAlpha = (slotAlpha * alphaMultiplier * revealIconAlpha * 255).toInt().coerceIn(0, 255)

        iconPaint.alpha = combinedIconAlpha
        artworkPaint.alpha = combinedIconAlpha
        textPaint.color = currentTextColor
        textPaint.alpha = combinedTextAlpha
        batteryTextPaint.color = currentTextColor
        batteryTextPaint.alpha = combinedTextAlpha

        var cursorX = left

        if (clockText.isNotBlank()) {
            val clockPaddingLeft = 10f * density
            cursorX += clockPaddingLeft
            textPaint.getTextBounds(clockText, 0, clockText.length, textBounds)
            val clockY = glanceCenterY - textBounds.exactCenterY() + offsetY
            if (combinedTextAlpha > 0) {
                canvas.drawText(clockText, cursorX, clockY, textPaint)
            }
            cursorX += textPaint.measureText(clockText) + (4f * density)
        }

        val hasIcon = icon != null
        val iconSize = if (isArtwork) 22f * density else 14f * density
        val iconPaddingLeft = if (clockText.isBlank()) {
            if (isArtwork) 1f * density else 8f * density
        } else 0f

        val restingIconLeft = cursorX + iconPaddingLeft
        val centeredIconLeft = left + (chipHeight - iconSize) / 2f
        val iconLeft = if (expandPhase < 1f) {
            centeredIconLeft + (restingIconLeft - centeredIconLeft) * expandPhase
        } else {
            restingIconLeft
        }
        val iconTop = glanceCenterY - iconSize / 2f + offsetY

        // Compute reserved right space for battery
        val batteryReservedWidth = if (batteryAlpha > 0f) {
            val iconSizePx = batteryIconSizeDp * density
            val itemW = when (batteryDisplayMode) {
                "icon" -> iconSizePx
                "percentage" -> batteryTextPaint.measureText("$batteryLevel%")
                "both" -> iconSizePx + 2f * density + batteryTextPaint.measureText("$batteryLevel%")
                else -> iconSizePx
            }
            (itemW + 5f * density) * batteryAlpha
        } else {
            0f
        }

        if (marqueeText.isNotBlank() && combinedTextAlpha > 0) {
            val textSpacing = if (hasIcon) {
                if (isArtwork) 3f * density else 4f * density
            } else {
                if (clockText.isBlank()) 10f * density else 0f
            }
            val textLeft = if (hasIcon) (iconLeft + iconSize + textSpacing) else (cursorX + textSpacing)
            val textRight = (if (isPillActive()) right else right - 8f * density) - batteryReservedWidth
            val maxTextWidth = (textRight - textLeft).coerceAtLeast(0f)

            if (!isTransitioning) {
                checkAndStartMarquee(marqueeText, maxTextWidth)
            }

            textPaint.getTextBounds(marqueeText, 0, marqueeText.length, textBounds)
            val textY = glanceCenterY - textBounds.exactCenterY() + offsetY

            if (isMarqueeNeeded && !isTransitioning) {
                val fadeOverlap = if (hasIcon) {
                    if (isArtwork) 4f * density else 3f * density
                } else {
                    4f * density
                }
                val fadeStart = (textLeft - fadeOverlap).coerceAtLeast(left)
                val fadeWidth = 14f * density
                val marqueeBounds = RectF(fadeStart, top, textRight, bottom)
                val saveLayerCount = canvas.saveLayer(marqueeBounds, null)

                if (isPillActive() && batteryAlpha <= 0f) {
                    marqueeClipPath.reset()
                    val radii = floatArrayOf(
                        0f, 0f,
                        cornerRadius, cornerRadius,
                        cornerRadius, cornerRadius,
                        0f, 0f
                    )
                    marqueeClipPath.addRoundRect(
                        RectF(fadeStart, top, right, bottom),
                        radii,
                        Path.Direction.CW
                    )
                    canvas.clipPath(marqueeClipPath)
                } else {
                    canvas.clipRect(fadeStart, top, textRight, bottom)
                }

                val marqueeGap = 28f * density
                val textWidthMeasure = textPaint.measureText(marqueeText)
                val x1 = textLeft - marqueeOffset
                val x2 = x1 + textWidthMeasure + marqueeGap
                canvas.drawText(marqueeText, x1, textY, textPaint)
                canvas.drawText(marqueeText, x2, textY, textPaint)

                // Left fade gradient
                marqueeFadePaint.shader = LinearGradient(
                    fadeStart, 0f, fadeStart + fadeWidth, 0f,
                    Color.TRANSPARENT, Color.BLACK,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(fadeStart, top, fadeStart + fadeWidth, bottom, marqueeFadePaint)

                // Right fade gradient when battery indicator is present
                if (batteryAlpha > 0f) {
                    val rightFadeWidth = 14f * density
                    val rightFadeStart = (textRight - rightFadeWidth).coerceAtLeast(fadeStart + fadeWidth)
                    if (rightFadeStart < textRight) {
                        marqueeFadePaint.shader = LinearGradient(
                            rightFadeStart, 0f, textRight, 0f,
                            Color.BLACK, Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                        canvas.drawRect(rightFadeStart, top, textRight, bottom, marqueeFadePaint)
                    }
                }

                canvas.restoreToCount(saveLayerCount)
            } else {
                canvas.drawText(marqueeText, textLeft, textY, textPaint)
            }
        }

        if (hasIcon && combinedIconAlpha > 0) {
            if (isArtwork) {
                val artworkRadius = iconSize / 2f
                val artworkCenterX = iconLeft + artworkRadius + swipeArtworkOffset
                val artworkCenterY = glanceCenterY + offsetY

                val shader = BitmapShader(icon, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                val matrix = android.graphics.Matrix()
                val scale = iconSize / icon.width.coerceAtMost(icon.height).toFloat()
                val dx = (iconLeft + swipeArtworkOffset) - (icon.width * scale - iconSize) / 2f
                val dy = (glanceCenterY - iconSize / 2f + offsetY) - (icon.height * scale - iconSize) / 2f
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

        if (batteryAlpha > 0f && expandPhase > 0.3f) {
            val batteryPhaseAlpha = ((expandPhase - 0.3f) / 0.7f).coerceIn(0f, 1f)
            val combinedBatteryAlpha = (slotAlpha * alphaMultiplier * revealTextAlpha * batteryAlpha * batteryPhaseAlpha * 255).toInt().coerceIn(0, 255)
            if (combinedBatteryAlpha > 0) {
                val iconSizePx = batteryIconSizeDp * density
                val pctText = "$batteryLevel%"
                val pctWidth = batteryTextPaint.measureText(pctText)

                when (batteryDisplayMode) {
                    "icon" -> {
                        val batteryIcon = getBatteryBitmap(iconSizePx.toInt())
                        if (batteryIcon != null) {
                            iconPaint.alpha = combinedBatteryAlpha
                            iconPaint.colorFilter = PorterDuffColorFilter(currentTextColor, PorterDuff.Mode.SRC_IN)
                            val batteryLeft = right - (5.5f * density) - iconSizePx
                            val batteryTop = glanceCenterY - iconSizePx / 2f + offsetY
                            canvas.drawBitmap(batteryIcon, null, RectF(batteryLeft, batteryTop, batteryLeft + iconSizePx, batteryTop + iconSizePx), iconPaint)
                        }
                    }
                    "percentage" -> {
                        batteryTextPaint.alpha = combinedBatteryAlpha
                        batteryTextPaint.color = currentTextColor
                        batteryTextPaint.getTextBounds(pctText, 0, pctText.length, textBounds)
                        val pctX = right - (5.5f * density) - pctWidth
                        val pctY = glanceCenterY - textBounds.exactCenterY() + offsetY
                        canvas.drawText(pctText, pctX, pctY, batteryTextPaint)
                    }
                    "both" -> {
                        // Draw percentage text first (rightmost), then icon to its left
                        batteryTextPaint.alpha = combinedBatteryAlpha
                        batteryTextPaint.color = currentTextColor
                        batteryTextPaint.getTextBounds(pctText, 0, pctText.length, textBounds)
                        val pctX = right - (5.5f * density) - pctWidth
                        val pctY = glanceCenterY - textBounds.exactCenterY() + offsetY
                        canvas.drawText(pctText, pctX, pctY, batteryTextPaint)

                        val batteryIcon = getBatteryBitmap(iconSizePx.toInt())
                        if (batteryIcon != null) {
                            iconPaint.alpha = combinedBatteryAlpha
                            iconPaint.colorFilter = PorterDuffColorFilter(currentTextColor, PorterDuff.Mode.SRC_IN)
                            val batteryLeft = pctX - 2f * density - iconSizePx
                            val batteryTop = glanceCenterY - iconSizePx / 2f + offsetY
                            canvas.drawBitmap(batteryIcon, null, RectF(batteryLeft, batteryTop, batteryLeft + iconSizePx, batteryTop + iconSizePx), iconPaint)
                        }
                    }
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (revealProgress <= 0f) return
        if (activeSlot == GlanceSlot.NONE && currentContentWidth <= 0f) return

        val height = 24f * density
        val baseCircularWidth = height
        val fullWidth = currentContentWidth.coerceAtLeast(baseCircularWidth)

        val iconPhase = (revealProgress / 0.35f).coerceIn(0f, 1f)
        val expandPhase = ((revealProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
        val textAlpha = ((revealProgress - 0.45f) / 0.55f).coerceIn(0f, 1f)
        val iconAlpha = iconPhase
        val iconScale = 0.5f + 0.5f * iconPhase

        val width = if (revealProgress < 1f) {
            (baseCircularWidth + (fullWidth - baseCircularWidth) * expandPhase) * (if (expandPhase == 0f) iconScale else 1f)
        } else {
            currentContentWidth
        }
        if (width <= 0f) return

        val currentHeight = if (revealProgress < 1f && expandPhase == 0f) height * iconScale else height
        val left = glanceCenterX
        val top = glanceCenterY - currentHeight / 2f
        val right = left + width
        val bottom = glanceCenterY + currentHeight / 2f
        val cornerRadius = currentHeight / 2f

        chipRect.set(left, top, right, bottom)

        val saveContainerCount = canvas.save()
        if (animatedTapScale != 1.0f) {
            canvas.scale(animatedTapScale, animatedTapScale, left + width / 2f, glanceCenterY)
        }

        val alphaInt = (slotAlpha * iconAlpha * 255).toInt().coerceIn(0, 255)

        if (isPillActive()) {
            pillPaint.color = currentPillColor
            pillPaint.alpha = alphaInt
            canvas.drawRoundRect(chipRect, cornerRadius, cornerRadius, pillPaint)
        }

        containerClipPath.reset()
        containerClipPath.addRoundRect(chipRect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(containerClipPath)

        if (isTransitioning) {
            val flipDistance = height * 0.95f
            val p = transitionProgress

            val oldOffsetY = -transitionDirection * p * flipDistance
            val oldAlpha = (1f - p).coerceIn(0f, 1f)

            val newOffsetY = transitionDirection * (1f - p) * flipDistance
            val newAlpha = p.coerceIn(0f, 1f)

            drawSlotContent(
                canvas = canvas,
                clockText = previousClockText,
                marqueeText = previousMarqueeText,
                icon = previousIcon,
                isArtwork = previousIsArtwork,
                alphaMultiplier = oldAlpha,
                offsetY = oldOffsetY,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                cornerRadius = cornerRadius,
                chipHeight = height,
                revealTextAlpha = textAlpha,
                revealIconAlpha = iconAlpha,
                expandPhase = expandPhase
            )

            val (currentClock, currentMarquee) = getSlotTexts(activeSlot)
            val currentIcon = getSlotIcon(activeSlot)
            val currentIsArtwork = activeSlot == GlanceSlot.MEDIA && mediaArtworkBitmap != null
            drawSlotContent(
                canvas = canvas,
                clockText = currentClock,
                marqueeText = currentMarquee,
                icon = currentIcon,
                isArtwork = currentIsArtwork,
                alphaMultiplier = newAlpha,
                offsetY = newOffsetY,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                cornerRadius = cornerRadius,
                chipHeight = height,
                revealTextAlpha = textAlpha,
                revealIconAlpha = iconAlpha,
                expandPhase = expandPhase
            )
        } else {
            val (currentClock, currentMarquee) = getSlotTexts(activeSlot)
            val currentIcon = getSlotIcon(activeSlot)
            val currentIsArtwork = activeSlot == GlanceSlot.MEDIA && mediaArtworkBitmap != null
            drawSlotContent(
                canvas = canvas,
                clockText = currentClock,
                marqueeText = currentMarquee,
                icon = currentIcon,
                isArtwork = currentIsArtwork,
                alphaMultiplier = 1f,
                offsetY = 0f,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                cornerRadius = cornerRadius,
                chipHeight = height,
                revealTextAlpha = textAlpha,
                revealIconAlpha = iconAlpha,
                expandPhase = expandPhase
            )
        }

        canvas.restoreToCount(saveContainerCount)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopMarquee()
        widthAnimator?.cancel()
        alphaAnimator?.cancel()
        colorAnimator?.cancel()
        transitionAnimator?.cancel()
        revealAnimator?.cancel()
        batteryAlphaAnimator?.cancel()
        swipeArtworkAnimator?.cancel()
        tapScaleAnimator?.cancel()
    }
}
