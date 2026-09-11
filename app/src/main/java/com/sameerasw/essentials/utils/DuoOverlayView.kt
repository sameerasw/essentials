/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays
 * File: DuoOverlayView.kt
 * Description: Ambient camera ring and dots overlay view with media playback seekbar support.
 */

package com.sameerasw.essentials.utils

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.os.Build
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.sameerasw.essentials.R
import kotlin.math.cos
import kotlin.math.sin

class DuoOverlayView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density

    var cameraCenterX: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var cameraCenterY: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var cameraRadiusPx: Float = 36f
        set(value) {
            field = value
            invalidate()
        }

    var ringRadiusScale: Float = 1.0f
        set(value) {
            field = value
            invalidate()
        }

    var useUniversalContrast: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var arcThicknessPx: Float = 10f
        set(value) {
            field = value
            trackPaint.strokeWidth = value
            progressPaint.strokeWidth = value
            contrastTrackPaint.strokeWidth = value + 0.8f * density
            invalidate()
        }

    var dotRadiusPx: Float = 5f
        set(value) {
            field = value
            invalidate()
        }

    var batteryLevel: Int = 100
        set(value) {
            val clamped = value.coerceIn(0, 100)
            field = clamped
            if (!isCustomProgressActive()) {
                updateProgressAnimation(clamped.toFloat())
            }
        }

    var isDarkTheme: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                mediaAppIcon?.let {
                    if (isMediaPlaying) {
                        mediaPaletteColors = extractMediaColors(it)
                    }
                }
                animateThemeChange()
            }
        }

    var isScreenOff: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityAnimation()
            }
        }

    var isFullscreen: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityAnimation()
            }
        }

    var hideWhenScreenOff: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityAnimation()
            }
        }

    var hideWhenScreenOffOnlyIdle: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateVisibilityAnimation()
            }
        }

    var showBattery: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateActiveProgressMode()
                updateVisibilityAnimation()
            }
        }

    var showMedia: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateActiveProgressMode()
            }
        }

    var showProgress: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateActiveProgressMode()
            }
        }

    var showFlashlight: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateActiveProgressMode()
            }
        }

    private fun updateVisibilityAnimation() {
        val isScreenOffHiding = if (hideWhenScreenOff) {
            if (hideWhenScreenOffOnlyIdle) {
                !isCustomProgressActive()
            } else {
                true
            }
        } else {
            false
        }
        val isNoActivityHiding = !showBattery && !isCustomProgressActive()
        val shouldHide = isFullscreen || (isScreenOff && isScreenOffHiding) || isNoActivityHiding
        if (shouldHide) {
            animateScreenOffVisibility(false)
        } else {
            animateScreenOffVisibility(true)
            animateThemeChange()
        }
    }

    var useMaterialYouColors: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                animateThemeChange()
            }
        }

    var showNetworks: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                animateLayoutChange()
            }
        }

    var signalLevel: Int = 4
        set(value) {
            val clamped = value.coerceIn(0, 4)
            if (field != clamped) {
                field = clamped
                animateSignalLevelChange(clamped.toFloat())
            }
        }

    var isMediaPlaying: Boolean = false
        private set

    var mediaAppIcon: Bitmap? = null
        private set

    var mediaProgress: Float = 0f
        private set

    var isProgressNotificationActive: Boolean = false
        private set

    var progressNotificationIcon: Bitmap? = null
        private set

    var progressNotificationProgress: Float = 0f
        private set

    var isFlashlightOn: Boolean = false
        private set

    var flashlightIcon: Bitmap? = null
        private set

    var flashlightBrightnessProgress: Float = 100f
        private set

    var isCharging: Boolean = false
        private set

    var isFastCharging: Boolean = false
        private set

    companion object {
        const val INTERACTIVE_MODE_NONE = 0
        const val INTERACTIVE_MODE_VOLUME = 1
        const val INTERACTIVE_MODE_BRIGHTNESS = 2
        const val INTERACTIVE_MODE_TRACK = 3
        const val INTERACTIVE_MODE_SOUND_MODE = 4
    }

    private var interactiveMode: Int = INTERACTIVE_MODE_NONE
    private var interactiveProgress: Float = 0f
    private var interactiveIcon: Bitmap? = null
    private var interactiveTrackRotation: Float = 0f
    private var pullDownOffsetY: Float = 0f
    private var pullDownStretchY: Float = 1.0f
    private var tapBounceScale: Float = 1.0f

    private var tapAnimator: ValueAnimator? = null
    private var pullDownAnimator: ValueAnimator? = null
    private var trackRotationAnimator: ValueAnimator? = null

    private var brightnessBitmap: Bitmap? = null

    private val revertInteractiveRunnable = Runnable {
        resetInteractiveState(animate = true)
    }

    private var isChargingAnnounce: Boolean = false
    private var chargingBoltBitmap: Bitmap? = null

    private val revertChargingRunnable = Runnable {
        if (isChargingAnnounce) {
            isChargingAnnounce = false
            updateActiveProgressMode()
        }
    }

    private fun getThemedBitmap(resId: Int): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, resId)?.mutate()
            drawable?.setTint(Color.WHITE)
            if (drawable != null) AppUtil.drawableToBitmap(drawable, 64) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun getVolumeIcon(currentVol: Int): Bitmap? {
        val res = when {
            currentVol <= 0 -> R.drawable.rounded_volume_off_24
            currentVol <= 5 -> R.drawable.rounded_volume_down_24
            else -> R.drawable.rounded_volume_up_24
        }
        return getThemedBitmap(res)
    }

    private fun getBrightnessIcon(): Bitmap? {
        if (brightnessBitmap == null) {
            brightnessBitmap = getThemedBitmap(R.drawable.rounded_brightness_6_24)
        }
        return brightnessBitmap
    }

    private fun getSoundModeIcon(ringerMode: Int): Bitmap? {
        val res = when (ringerMode) {
            android.media.AudioManager.RINGER_MODE_NORMAL -> R.drawable.rounded_mobile_sound_24
            android.media.AudioManager.RINGER_MODE_VIBRATE -> R.drawable.rounded_mobile_vibrate_24
            else -> R.drawable.rounded_volume_off_24
        }
        return getThemedBitmap(res)
    }

    fun triggerTapAnimation() {
        tapAnimator?.cancel()
        tapAnimator = ValueAnimator.ofFloat(1.0f, 0.93f, 1.05f, 1.0f).apply {
            duration = 240
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                tapBounceScale = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setPullDownOffset(offset: Float, stretch: Float) {
        pullDownAnimator?.cancel()
        pullDownOffsetY = offset
        pullDownStretchY = stretch
        invalidate()
    }

    fun releasePullDown() {
        pullDownAnimator?.cancel()
        val startOffset = pullDownOffsetY
        val startStretch = pullDownStretchY
        if (startOffset == 0f && startStretch == 1.0f) return

        pullDownAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320
            interpolator = OvershootInterpolator(1.3f)
            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                pullDownOffsetY = startOffset * (1f - fraction)
                pullDownStretchY = 1.0f + (startStretch - 1.0f) * (1f - fraction)
                invalidate()
            }
            start()
        }
    }

    fun setInteractiveVolume(currentVol: Int, maxVol: Int) {
        interactiveMode = INTERACTIVE_MODE_VOLUME
        val progress = if (maxVol > 0) (currentVol.toFloat() / maxVol.toFloat() * 100f).coerceIn(0f, 100f) else 0f
        interactiveProgress = progress
        interactiveIcon = getVolumeIcon(currentVol)

        removeCallbacks(revertInteractiveRunnable)
        postDelayed(revertInteractiveRunnable, 2000L)

        updateActiveProgressMode()
        updateProgressAnimation(progress)
    }

    fun setInteractiveBrightness(brightness: Int, maxBrightness: Int = 255) {
        interactiveMode = INTERACTIVE_MODE_BRIGHTNESS
        val progress = (brightness.toFloat() / maxBrightness.toFloat() * 100f).coerceIn(0f, 100f)
        interactiveProgress = progress
        interactiveIcon = getBrightnessIcon()

        removeCallbacks(revertInteractiveRunnable)
        postDelayed(revertInteractiveRunnable, 2000L)

        updateActiveProgressMode()
        updateProgressAnimation(progress)
    }

    fun setInteractiveSoundMode(ringerMode: Int) {
        interactiveMode = INTERACTIVE_MODE_SOUND_MODE
        interactiveProgress = when (ringerMode) {
            android.media.AudioManager.RINGER_MODE_NORMAL -> 100f
            android.media.AudioManager.RINGER_MODE_VIBRATE -> 50f
            else -> 0f
        }
        interactiveIcon = getSoundModeIcon(ringerMode)

        removeCallbacks(revertInteractiveRunnable)
        postDelayed(revertInteractiveRunnable, 2000L)

        updateActiveProgressMode()
        updateProgressAnimation(interactiveProgress)
    }

    fun setInteractiveTrackRotation(rotationDegrees: Float) {
        trackRotationAnimator?.cancel()
        interactiveTrackRotation = rotationDegrees
        invalidate()
    }

    fun releaseTrackRotation() {
        trackRotationAnimator?.cancel()
        val startRot = interactiveTrackRotation
        if (startRot == 0f) return

        trackRotationAnimator = ValueAnimator.ofFloat(startRot, 0f).apply {
            duration = 320
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { anim ->
                interactiveTrackRotation = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun resetInteractiveState(animate: Boolean = true) {
        removeCallbacks(revertInteractiveRunnable)
        if (interactiveMode == INTERACTIVE_MODE_NONE) return
        interactiveMode = INTERACTIVE_MODE_NONE
        interactiveIcon = null
        releaseTrackRotation()
        updateActiveProgressMode()
    }

    private fun getChargingBoltBitmap(): Bitmap? {
        if (chargingBoltBitmap == null) {
            try {
                val drawable = ContextCompat.getDrawable(context, R.drawable.rounded_bolt_24)?.mutate()
                drawable?.setTint(Color.WHITE)
                if (drawable != null) {
                    chargingBoltBitmap = AppUtil.drawableToBitmap(drawable, 64)
                }
            } catch (_: Exception) {}
        }
        return chargingBoltBitmap
    }

    fun triggerChargingAnimation(isFastCharging: Boolean) {
        this.isCharging = true
        this.isFastCharging = isFastCharging
        this.isChargingAnnounce = true

        removeCallbacks(revertChargingRunnable)
        postDelayed(revertChargingRunnable, 4000L)

        updateActiveProgressMode()

        val target = batteryLevel.toFloat()
        progressAnimator?.cancel()
        animatedProgress = 0f
        progressAnimator = ValueAnimator.ofFloat(0f, target).apply {
            duration = 750
            interpolator = DecelerateInterpolator(1.4f)
            addUpdateListener { animation ->
                animatedProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setCharging(isCharging: Boolean, isFastCharging: Boolean = false) {
        val wasCharging = this.isCharging
        this.isCharging = isCharging
        this.isFastCharging = isFastCharging

        if (!isCharging) {
            isChargingAnnounce = false
            removeCallbacks(revertChargingRunnable)
            updateActiveProgressMode()
        } else if (wasCharging != isCharging) {
            animateThemeChange()
        }
    }

    private var mediaPaletteColors: Pair<Int, Int>? = null

    private fun extractMediaColors(bitmap: Bitmap): Pair<Int, Int> {
        val palette = try {
            Palette.from(bitmap)
                .maximumColorCount(24)
                .generate()
        } catch (_: Exception) {
            null
        }

        val dominantSwatch = palette?.dominantSwatch
        val vibrantSwatch = palette?.vibrantSwatch
        val lightVibrantSwatch = palette?.lightVibrantSwatch
        val darkVibrantSwatch = palette?.darkVibrantSwatch
        val mutedSwatch = palette?.mutedSwatch
        val lightMutedSwatch = palette?.lightMutedSwatch
        val darkMutedSwatch = palette?.darkMutedSwatch

        val rawAccent = if (isDarkTheme) {
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

        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(rawAccent, hsv)
        if (isDarkTheme) {
            hsv[1] = hsv[1].coerceIn(0.4f, 0.95f)
            hsv[2] = hsv[2].coerceIn(0.7f, 1.0f)
        } else {
            hsv[1] = hsv[1].coerceIn(0.5f, 1.0f)
            hsv[2] = hsv[2].coerceIn(0.2f, 0.65f)
        }
        val accent = android.graphics.Color.HSVToColor(hsv)

        val trackAlpha = if (isDarkTheme) 90 else 110
        val track = Color.argb(trackAlpha, Color.red(accent), Color.green(accent), Color.blue(accent))
        return Pair(track, accent)
    }

    private fun isCustomProgressActive(): Boolean {
        return interactiveMode != INTERACTIVE_MODE_NONE ||
            isChargingAnnounce ||
            (isFlashlightOn && showFlashlight) ||
            (isMediaPlaying && showMedia) ||
            (isProgressNotificationActive && showProgress)
    }

    private fun getCurrentCustomProgress(): Float {
        return when {
            interactiveMode != INTERACTIVE_MODE_NONE -> interactiveProgress
            isChargingAnnounce -> batteryLevel.toFloat()
            isFlashlightOn && showFlashlight -> flashlightBrightnessProgress
            isMediaPlaying && showMedia -> mediaProgress
            else -> progressNotificationProgress
        }
    }

    private fun getCurrentCustomIcon(): Bitmap? {
        return when {
            interactiveMode != INTERACTIVE_MODE_NONE -> interactiveIcon
            isChargingAnnounce -> getChargingBoltBitmap()
            isFlashlightOn && showFlashlight -> flashlightIcon
            isMediaPlaying && showMedia -> mediaAppIcon
            else -> progressNotificationIcon
        }
    }

    private fun updateActiveProgressMode() {
        val wasActive = animatedCustomFraction > 0.5f
        val isNowActive = isCustomProgressActive()
        if (isMediaPlaying && showMedia && !isFlashlightOn && !isChargingAnnounce && mediaAppIcon != null) {
            mediaPaletteColors = extractMediaColors(mediaAppIcon!!)
        } else {
            mediaPaletteColors = null
        }
        animateThemeChange()
        if (wasActive != isNowActive) {
            animateLayoutChange()
            updateVisibilityAnimation()
        }
        updateProgressAnimation()
    }

    fun setMediaState(isPlaying: Boolean, progress: Float, appIcon: Bitmap?) {
        val wasActive = isCustomProgressActive()
        val iconChanged = mediaAppIcon != appIcon
        isMediaPlaying = isPlaying
        if (appIcon != null || !isPlaying) {
            mediaAppIcon = appIcon
        }
        mediaProgress = progress.coerceIn(0f, 100f)

        if (isPlaying && showMedia && !isFlashlightOn && !isChargingAnnounce && appIcon != null && (iconChanged || mediaPaletteColors == null)) {
            mediaPaletteColors = extractMediaColors(appIcon)
            animateThemeChange()
        } else if ((!isPlaying || !showMedia || isFlashlightOn || isChargingAnnounce) && mediaPaletteColors != null) {
            mediaPaletteColors = null
            animateThemeChange()
        }

        val isNowActive = isCustomProgressActive()
        if (wasActive != isNowActive) {
            animateLayoutChange()
            updateVisibilityAnimation()
        }
        updateProgressAnimation()
    }

    fun setProgressNotificationState(isActive: Boolean, progress: Float, icon: Bitmap?) {
        val wasActive = isCustomProgressActive()
        isProgressNotificationActive = isActive
        if (icon != null || !isActive) {
            progressNotificationIcon = icon
        }
        progressNotificationProgress = progress.coerceIn(0f, 100f)

        if (!isMediaPlaying || !showMedia || isFlashlightOn || isChargingAnnounce) {
            if (mediaPaletteColors != null) {
                mediaPaletteColors = null
                animateThemeChange()
            }
        }

        val isNowActive = isCustomProgressActive()
        if (wasActive != isNowActive) {
            animateLayoutChange()
            updateVisibilityAnimation()
        }
        updateProgressAnimation()
    }

    fun setFlashlightState(isOn: Boolean, brightnessProgress: Float, icon: Bitmap?) {
        val wasActive = isCustomProgressActive()
        val stateChanged = isFlashlightOn != isOn
        isFlashlightOn = isOn
        if (icon != null || !isOn) {
            flashlightIcon = icon
        }
        val clampedProgress = brightnessProgress.coerceIn(0f, 100f)
        val progressChanged = kotlin.math.abs(flashlightBrightnessProgress - clampedProgress) > 0.1f
        flashlightBrightnessProgress = clampedProgress

        if (stateChanged) {
            if (isOn && showFlashlight) {
                if (mediaPaletteColors != null) {
                    mediaPaletteColors = null
                    animateThemeChange()
                }
            } else if (isMediaPlaying && showMedia && !isChargingAnnounce && mediaAppIcon != null) {
                mediaPaletteColors = extractMediaColors(mediaAppIcon!!)
                animateThemeChange()
            }
        }

        val isNowActive = isCustomProgressActive()
        if (wasActive != isNowActive) {
            animateLayoutChange()
            updateVisibilityAnimation()
        }
        if (progressChanged || stateChanged) {
            updateProgressAnimation()
        }
    }

    private var targetProgress: Float = 100f
    private var animatedProgress: Float = 100f
    private var progressAnimator: ValueAnimator? = null

    private var animatedSignalLevel: Float = 4f
    private var signalAnimator: ValueAnimator? = null

    private var animatedStartAngle: Float = 140f
    private var animatedTotalSweep: Float = 260f
    private var animatedDotAlpha: Float = 1.0f
    private var animatedCustomFraction: Float = 0.0f
    private var animatedScaleBounce: Float = 1.0f
    private var layoutAnimator: ValueAnimator? = null
    private var scaleAnimator: ValueAnimator? = null

    private var animatedVisibilityAlpha: Float = 1.0f
    private var animatedVisibilityScale: Float = 1.0f
    private var animatedVisibilityRotation: Float = 0f
    private var visibilityAnimator: ValueAnimator? = null

    private var currentTrackColor: Int = Color.argb(60, 255, 255, 255)
    private var currentProgressColor: Int = Color.WHITE
    private var currentDotBaseColor: Int = Color.WHITE
    private var themeAnimator: ValueAnimator? = null

    private fun animateScreenOffVisibility(visible: Boolean) {
        visibilityAnimator?.cancel()
        val startAlpha = animatedVisibilityAlpha
        val targetAlpha = if (visible) 1.0f else 0.0f

        val startScale = animatedVisibilityScale
        val targetScale = if (visible) 1.0f else 0.35f

        val startRotation = animatedVisibilityRotation
        val targetRotation = if (visible) 0f else -65f

        visibilityAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 750
            interpolator = if (visible) {
                OvershootInterpolator(1.15f)
            } else {
                DecelerateInterpolator()
            }
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                animatedVisibilityAlpha = (startAlpha + (targetAlpha - startAlpha) * fraction).coerceIn(0f, 1f)
                animatedVisibilityScale = (startScale + (targetScale - startScale) * fraction).coerceAtLeast(0.01f)
                animatedVisibilityRotation = startRotation + (targetRotation - startRotation) * fraction
                invalidate()
            }
            start()
        }
    }

    private fun getTargetColors(): Triple<Int, Int, Int> {
        if (isScreenOff) {
            if (isMediaPlaying && showMedia && mediaPaletteColors != null) {
                val (_, accent) = mediaPaletteColors!!
                val dimProgress = Color.argb(160, Color.red(accent), Color.green(accent), Color.blue(accent))
                val dimTrack = Color.argb(50, Color.red(accent), Color.green(accent), Color.blue(accent))
                return Triple(dimTrack, dimProgress, dimProgress)
            }
            if (isCharging) {
                val chargeAccent = if (isFastCharging) Color.rgb(0, 229, 255) else Color.rgb(0, 230, 118)
                val dimProgress = Color.argb(160, Color.red(chargeAccent), Color.green(chargeAccent), Color.blue(chargeAccent))
                val dimTrack = Color.argb(50, Color.red(chargeAccent), Color.green(chargeAccent), Color.blue(chargeAccent))
                return Triple(dimTrack, dimProgress, dimProgress)
            }
            return Triple(
                Color.argb(40, 255, 255, 255),
                Color.argb(128, 255, 255, 255),
                Color.argb(128, 255, 255, 255)
            )
        }

        if (isMediaPlaying && showMedia && mediaPaletteColors != null) {
            val (track, progress) = mediaPaletteColors!!
            return Triple(track, progress, progress)
        }

        if (isCharging) {
            val chargeAccent = if (isFastCharging) Color.rgb(0, 229, 255) else Color.rgb(0, 230, 118)
            val trackAlpha = if (isDarkTheme) 90 else 110
            val track = Color.argb(trackAlpha, Color.red(chargeAccent), Color.green(chargeAccent), Color.blue(chargeAccent))
            return Triple(track, chargeAccent, chargeAccent)
        }

        if (useMaterialYouColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return if (isDarkTheme) {
                val accent = ContextCompat.getColor(context, android.R.color.system_accent1_200)
                val track = ContextCompat.getColor(context, android.R.color.system_neutral1_800)
                val trackWithAlpha = Color.argb(90, Color.red(track), Color.green(track), Color.blue(track))
                Triple(trackWithAlpha, accent, accent)
            } else {
                val accent = ContextCompat.getColor(context, android.R.color.system_accent1_600)
                val track = ContextCompat.getColor(context, android.R.color.system_neutral1_200)
                val trackWithAlpha = Color.argb(110, Color.red(track), Color.green(track), Color.blue(track))
                Triple(trackWithAlpha, accent, accent)
            }
        }

        return if (isDarkTheme) {
            Triple(
                Color.argb(60, 255, 255, 255),
                Color.WHITE,
                Color.WHITE
            )
        } else {
            Triple(
                Color.argb(60, 0, 0, 0),
                Color.BLACK,
                Color.BLACK
            )
        }
    }

    private fun animateThemeChange() {
        themeAnimator?.cancel()
        val (targetTrack, targetProgress, targetDot) = getTargetColors()
        val startTrack = currentTrackColor
        val startProgress = currentProgressColor
        val startDot = currentDotBaseColor
        val evaluator = ArgbEvaluator()

        themeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                currentTrackColor = evaluator.evaluate(fraction, startTrack, targetTrack) as Int
                currentProgressColor = evaluator.evaluate(fraction, startProgress, targetProgress) as Int
                currentDotBaseColor = evaluator.evaluate(fraction, startDot, targetDot) as Int
                trackPaint.color = currentTrackColor
                progressPaint.color = currentProgressColor
                invalidate()
            }
            start()
        }
    }

    private fun animateSignalLevelChange(targetLevel: Float) {
        signalAnimator?.cancel()
        signalAnimator = ValueAnimator.ofFloat(animatedSignalLevel, targetLevel).apply {
            duration = 600
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener { animation ->
                animatedSignalLevel = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun getActiveTargetProgress(): Float {
        return if (isCustomProgressActive()) {
            getCurrentCustomProgress()
        } else {
            batteryLevel.toFloat()
        }
    }

    private fun updateProgressAnimation(target: Float = getActiveTargetProgress()) {
        targetProgress = target
        if (kotlin.math.abs(animatedProgress - targetProgress) < 0.05f) {
            animatedProgress = targetProgress
            invalidate()
            return
        }
        val startVal = animatedProgress
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(startVal, targetProgress).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                animatedProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateLayoutChange() {
        layoutAnimator?.cancel()
        scaleAnimator?.cancel()

        val isCustom = isCustomProgressActive()
        val targetCustomFraction = if (isCustom) 1.0f else 0.0f
        val targetStartAngle = if (isCustom) 120f else (if (showNetworks) 140f else -90f)
        val targetTotalSweep = if (isCustom) 300f else (if (showNetworks) 260f else 360f)
        val targetDotAlpha = if (isCustom) 0.0f else (if (showNetworks) 1.0f else 0.0f)

        val startStartAngle = animatedStartAngle
        val startTotalSweep = animatedTotalSweep
        val startDotAlpha = animatedDotAlpha
        val startCustomFraction = animatedCustomFraction

        layoutAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 650
            interpolator = OvershootInterpolator(1.15f)
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                animatedStartAngle = startStartAngle + (targetStartAngle - startStartAngle) * fraction
                animatedTotalSweep = startTotalSweep + (targetTotalSweep - startTotalSweep) * fraction
                animatedDotAlpha = (startDotAlpha + (targetDotAlpha - startDotAlpha) * fraction).coerceIn(0f, 1f)
                animatedCustomFraction = (startCustomFraction + (targetCustomFraction - startCustomFraction) * fraction).coerceIn(0f, 1f)
                invalidate()
            }
            start()
        }

        scaleAnimator = ValueAnimator.ofFloat(1.0f, 1.04f, 1.0f).apply {
            duration = 650
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener { animation ->
                animatedScaleBounce = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val contrastTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val contrastDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val contrastIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val iconClipPath = Path()
    private val iconRect = RectF()
    private val arcBounds = RectF()

    init {
        val (track, progress, dot) = getTargetColors()
        currentTrackColor = track
        currentProgressColor = progress
        currentDotBaseColor = dot
        trackPaint.color = currentTrackColor
        progressPaint.color = currentProgressColor
        contrastTrackPaint.strokeWidth = arcThicknessPx + 0.8f * density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cameraCenterX <= 0 && cameraCenterY <= 0) return
        if (animatedVisibilityAlpha <= 0.005f) return

        val baseRadius = (cameraRadiusPx + 14f * density) * ringRadiusScale * animatedScaleBounce * tapBounceScale
        arcBounds.set(
            cameraCenterX - baseRadius,
            cameraCenterY - baseRadius,
            cameraCenterX + baseRadius,
            cameraCenterY + baseRadius
        )

        canvas.save()
        canvas.translate(cameraCenterX, cameraCenterY + pullDownOffsetY)
        canvas.rotate(animatedVisibilityRotation + interactiveTrackRotation)
        canvas.scale(animatedVisibilityScale, animatedVisibilityScale * pullDownStretchY)
        canvas.translate(-cameraCenterX, -cameraCenterY)

        val r = Color.red(currentProgressColor) / 255.0
        val g = Color.green(currentProgressColor) / 255.0
        val b = Color.blue(currentProgressColor) / 255.0
        val isProgressLight = (0.299 * r + 0.587 * g + 0.114 * b) > 0.45
        val contrastColor = if (isProgressLight) Color.BLACK else Color.WHITE
        val baseContrastAlpha = if (isProgressLight) 24 else 28
        val contrastAlpha = (baseContrastAlpha * animatedVisibilityAlpha).toInt()

        if (useUniversalContrast && contrastAlpha > 0) {
            contrastTrackPaint.color = Color.argb(
                contrastAlpha,
                Color.red(contrastColor),
                Color.green(contrastColor),
                Color.blue(contrastColor)
            )
            canvas.drawArc(arcBounds, animatedStartAngle, animatedTotalSweep, false, contrastTrackPaint)
        }

        val trackAlpha = (Color.alpha(currentTrackColor) * animatedVisibilityAlpha).toInt()
        trackPaint.color = Color.argb(
            trackAlpha,
            Color.red(currentTrackColor),
            Color.green(currentTrackColor),
            Color.blue(currentTrackColor)
        )
        canvas.drawArc(arcBounds, animatedStartAngle, animatedTotalSweep, false, trackPaint)

        val progressSweep = (animatedProgress.coerceIn(0f, 100f) / 100f) * animatedTotalSweep
        if (progressSweep > 0.5f) {
            val progAlpha = (Color.alpha(currentProgressColor) * animatedVisibilityAlpha).toInt()
            progressPaint.color = Color.argb(
                progAlpha,
                Color.red(currentProgressColor),
                Color.green(currentProgressColor),
                Color.blue(currentProgressColor)
            )
            canvas.drawArc(arcBounds, animatedStartAngle, progressSweep, false, progressPaint)
        }

        val effectiveDotAlpha = animatedDotAlpha * (1f - animatedCustomFraction)
        if (effectiveDotAlpha > 0.01f) {
            val dotAngles = floatArrayOf(120f, 100f, 80f, 60f)
            val baseAlpha = Color.alpha(currentDotBaseColor)
            val red = Color.red(currentDotBaseColor)
            val green = Color.green(currentDotBaseColor)
            val blue = Color.blue(currentDotBaseColor)

            for (i in dotAngles.indices) {
                val angleDeg = dotAngles[i]
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val dotX = (cameraCenterX + baseRadius * cos(angleRad)).toFloat()
                val dotY = (cameraCenterY + baseRadius * sin(angleRad)).toFloat()

                val dotActiveFraction = (animatedSignalLevel - i).coerceIn(0f, 1f)
                val dotOpacity = (0.22f + 0.78f * dotActiveFraction) * effectiveDotAlpha * animatedVisibilityAlpha
                dotPaint.color = Color.argb((baseAlpha * dotOpacity).toInt(), red, green, blue)

                if (useUniversalContrast && contrastAlpha > 0) {
                    val shadowAlpha = (contrastAlpha * effectiveDotAlpha).toInt().coerceIn(0, 255)
                    contrastDotPaint.color = Color.argb(
                        shadowAlpha,
                        Color.red(contrastColor),
                        Color.green(contrastColor),
                        Color.blue(contrastColor)
                    )
                    canvas.drawCircle(dotX, dotY, (dotRadiusPx + 0.5f * density) * effectiveDotAlpha, contrastDotPaint)
                }

                canvas.drawCircle(dotX, dotY, dotRadiusPx * effectiveDotAlpha, dotPaint)
            }
        }

        if (animatedCustomFraction > 0.01f) {
            val icon = getCurrentCustomIcon()
            if (icon != null) {
                val gapCenterAngleDeg = (animatedStartAngle + animatedTotalSweep + (360f - animatedTotalSweep) / 2f) % 360f
                val angleRad = Math.toRadians(gapCenterAngleDeg.toDouble())
                val downwardOffset = 4f * density
                val iconRadius = (dotRadiusPx * 2.85f) * animatedCustomFraction
                val iconCenterX = (cameraCenterX + (baseRadius + downwardOffset) * cos(angleRad)).toFloat()
                val iconCenterY = (cameraCenterY + (baseRadius + downwardOffset) * sin(angleRad)).toFloat()

                if (iconRadius > 1f) {
                    if (useUniversalContrast && contrastAlpha > 0) {
                        val iconShadowAlpha = (contrastAlpha * animatedCustomFraction).toInt().coerceIn(0, 255)
                        contrastIconPaint.color = Color.argb(
                            iconShadowAlpha,
                            Color.red(contrastColor),
                            Color.green(contrastColor),
                            Color.blue(contrastColor)
                        )
                        canvas.drawCircle(iconCenterX, iconCenterY, iconRadius + 0.5f * density, contrastIconPaint)
                    }

                    val saveIcon = canvas.save()
                    iconClipPath.reset()
                    iconClipPath.addCircle(iconCenterX, iconCenterY, iconRadius, Path.Direction.CW)
                    canvas.clipPath(iconClipPath)

                    iconRect.set(
                        iconCenterX - iconRadius,
                        iconCenterY - iconRadius,
                        iconCenterX + iconRadius,
                        iconCenterY + iconRadius
                    )
                    iconPaint.alpha = (255 * animatedCustomFraction * animatedVisibilityAlpha).toInt()
                    val isTintable = (isFlashlightOn && showFlashlight) ||
                        isChargingAnnounce ||
                        interactiveMode == INTERACTIVE_MODE_VOLUME ||
                        interactiveMode == INTERACTIVE_MODE_BRIGHTNESS ||
                        interactiveMode == INTERACTIVE_MODE_SOUND_MODE
                    if (isTintable) {
                        iconPaint.colorFilter = PorterDuffColorFilter(currentProgressColor, PorterDuff.Mode.SRC_IN)
                    } else {
                        iconPaint.colorFilter = null
                    }
                    canvas.drawBitmap(icon, null, iconRect, iconPaint)
                    canvas.restoreToCount(saveIcon)
                }
            }
        }

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(revertChargingRunnable)
        removeCallbacks(revertInteractiveRunnable)
        progressAnimator?.cancel()
        signalAnimator?.cancel()
        layoutAnimator?.cancel()
        scaleAnimator?.cancel()
        themeAnimator?.cancel()
        visibilityAnimator?.cancel()
        tapAnimator?.cancel()
        pullDownAnimator?.cancel()
        trackRotationAnimator?.cancel()
    }
}


