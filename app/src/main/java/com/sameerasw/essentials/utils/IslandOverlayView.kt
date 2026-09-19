/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays
 * File: IslandOverlayView.kt
 * Description: Dynamic island notification pill overlay view expanding from camera cutout.
 */

package com.sameerasw.essentials.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.ActiveNotificationAlert
import com.sameerasw.essentials.domain.model.NotificationActionItem
import com.sameerasw.essentials.services.handlers.IslandTouchHandler
import com.sameerasw.essentials.utils.island.AnimatedFloatProperty
import com.sameerasw.essentials.utils.island.EqualizerAnimator
import com.sameerasw.essentials.utils.island.FlashlightHit
import com.sameerasw.essentials.utils.island.FlashlightPill
import com.sameerasw.essentials.utils.island.FlashlightPillHost
import com.sameerasw.essentials.utils.island.IslandBatteryColorConfig
import com.sameerasw.essentials.utils.island.IslandContentState
import com.sameerasw.essentials.utils.island.IslandIdleHost
import com.sameerasw.essentials.utils.island.IslandIdleIndicator
import com.sameerasw.essentials.utils.island.MediaArtworkStyle
import com.sameerasw.essentials.utils.island.WavyProgressRenderer
import com.sameerasw.essentials.utils.island.drawRollingDigits
import com.sameerasw.essentials.utils.island.drawRollingText
import com.sameerasw.essentials.utils.island.drawEqualizerIcon
import com.sameerasw.essentials.utils.island.IslandBubbleIconShape
import com.sameerasw.essentials.utils.island.IslandBubbleRow
import com.sameerasw.essentials.utils.island.IslandBubbleSide
import com.sameerasw.essentials.utils.island.IslandBubbleSpec
import com.sameerasw.essentials.utils.island.IslandTransitionSpec
import com.sameerasw.essentials.utils.island.MarqueeController
import kotlin.math.abs

class IslandOverlayView(context: Context) : View(context) {
    enum class DragCollapseTarget {
        CAMERA,
        COMPACT,
    }

    private companion object {
        private const val MEDIA_BUBBLE_KEY = "media"
        private const val CONSCIOUS_GATE_BUBBLE_KEY = "conscious_gate"
        private const val TOTAL_BUBBLE_BUDGET = 2
    }

    private val density = resources.displayMetrics.density

    var touchHandler: IslandTouchHandler? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isNotificationAlertActive && !isMediaPlaybackActive && !isCalendarActive && !isConsciousGateActive && !flashlightPill.isActive) return false
        val x = event.x
        val y = event.y

        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val totalBounds = getTotalAlertsBounds()
            val pad = 16f * density
            val expandedRect = RectF(
                totalBounds.left - pad,
                totalBounds.top - pad,
                totalBounds.right + pad,
                totalBounds.bottom + pad,
            )
            if (!expandedRect.contains(x, y)) {
                return false
            }
        }

        return touchHandler?.onTouchEvent(event) ?: super.onTouchEvent(event)
    }

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

    var cutoutGapDp: Float = 6f
        set(value) {
            field = value
            invalidate()
        }
    private val cutoutGap: Float get() = cutoutGapDp * density
    private val cutoutIconGap: Float get() = cutoutGap + 2f * density

    var maxWidthDp: Float = 360f
        set(value) {
            field = value
            invalidate()
        }

    var expandedWidthDp: Float = 360f
        set(value) {
            field = value
            invalidate()
        }

    var expandedCornerRadiusDp: Float = 24f
        set(value) {
            field = value
            invalidate()
        }

    var expandedPaddingDp: Float = 16f
        set(value) {
            field = value
            invalidate()
        }

    var expandedTopPaddingDp: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var isIslandEnabled: Boolean = true
    var isShowGlow: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var isCatchUpMode: Boolean = false
        private set
    var canEnterCatchUp: Boolean = true
    var catchUpFraction: Float = 0f
        private set
    private val catchUpAnimator = AnimatedFloatProperty()
    var onCatchUpModeChanged: ((Boolean) -> Unit)? = null

    private val unreadIconBitmap: Bitmap? by lazy {
        try {
            val d = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.rounded_notifications_unread_24)
            if (d != null) {
                AppUtil.drawableToBitmap(d, (24f * density).toInt())
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private val calendarIconBitmap: Bitmap? by lazy {
        try {
            val d = ContextCompat.getDrawable(context, R.drawable.rounded_calendar_today_24)
            d?.let { AppUtil.drawableToBitmap(it, (24f * density).toInt()) }
        } catch (_: Exception) {
            null
        }
    }

    private val timerIconBitmap: Bitmap? by lazy {
        try {
            val d = ContextCompat.getDrawable(context, R.drawable.rounded_timer_24)
            d?.let { AppUtil.drawableToBitmap(it, (24f * density).toInt()) }
        } catch (_: Exception) {
            null
        }
    }

    private val equalizerAnimator = EqualizerAnimator()
    private val equalizerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private fun materialYouAccentColor(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            ContextCompat.getColor(context, android.R.color.system_accent1_200)
        } catch (_: Exception) {
            0xFF80D8FF.toInt()
        }
    } else {
        0xFF80D8FF.toInt()
    }

    private val artworkStyle = MediaArtworkStyle { invalidate() }

    private fun playerAccentColor(): Int = artworkStyle.accentColor ?: materialYouAccentColor()

    private var activeNotificationAlert: ActiveNotificationAlert? = null
    var isNotificationAlertActive: Boolean = false
        private set
    var isMediaPlaybackActive: Boolean = false
        private set
    var isMediaCompact: Boolean = false
        private set
    private var mediaTitle: String = ""
    private var mediaArtist: String = ""
    private var mediaArtwork: Bitmap? = null
    private val mediaAnimator = AnimatedFloatProperty()
    private val mediaCompactAnimator = AnimatedFloatProperty()
    private val mediaBubbleAnimator = AnimatedFloatProperty()
    private var mediaFraction: Float = 0f
    var mediaCompactFraction: Float = 0f
        private set
    private var mediaBubbleFraction: Float = 0f
    private val mediaPillRect = RectF()

    // Rolls the title/artist/artwork up-and-out while the new track rolls in, mirroring Status Glance's ticker.
    private var mediaTitlePrev: String = ""
    private var mediaArtistPrev: String = ""
    private var mediaArtworkPrev: Bitmap? = null
    private var mediaContentRollFraction: Float = 1f
    private val mediaContentRollAnimator = AnimatedFloatProperty()

    var isMediaFullPlayerActive: Boolean = false
        private set
    var mediaFullPlayerFraction: Float = 0f
        private set
    private val mediaFullPlayerAnimator = AnimatedFloatProperty()
    var mediaProgressFraction: Float = 0f
        private set
    var isMediaTransportPlaying: Boolean = true
        private set
    var isMediaLiked: Boolean = false
        private set
    private val wavyProgress = WavyProgressRenderer(density) { invalidate() }
    private val mediaLikeButtonRect = RectF()
    private val mediaPlayPauseButtonRect = RectF()
    private val mediaNextButtonRect = RectF()
    private val cutoutFadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mediaControlBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mediaFullPlayerPath = Path()

    private fun loadIconBitmap(resId: Int, sizeDp: Float = 22f): Bitmap? = try {
        ContextCompat.getDrawable(context, resId)?.let { AppUtil.drawableToBitmap(it, (sizeDp * density).toInt()) }
    } catch (_: Exception) {
        null
    }

    private val playIconBitmap: Bitmap? by lazy { loadIconBitmap(R.drawable.rounded_play_arrow_24, 26f) }
    private val pauseIconBitmap: Bitmap? by lazy { loadIconBitmap(R.drawable.rounded_pause_24, 26f) }
    private val skipNextIconBitmap: Bitmap? by lazy { loadIconBitmap(R.drawable.rounded_skip_next_24, 24f) }
    private val favoriteIconBitmap: Bitmap? by lazy { loadIconBitmap(R.drawable.round_favorite_24, 22f) }
    private val favoriteOutlineIconBitmap: Bitmap? by lazy { loadIconBitmap(R.drawable.rounded_favorite_24, 22f) }

    var isCalendarActive: Boolean = false
        private set
    var isCalendarCompact: Boolean = true
        private set
    private var calendarEventTitle: String = ""
    private var calendarLocationText: String = ""
    private var calendarCompactTimeText: String = ""
    private var calendarFullTimeText: String = ""
    private val calendarAnimator = AnimatedFloatProperty()
    private val calendarCompactAnimator = AnimatedFloatProperty()
    private var calendarFraction: Float = 0f
    private var calendarCompactFraction: Float = 1f
    private val calendarPillRect = RectF()

    private var calendarCompactTimePrev: String = ""
    private var calendarCompactTimeRollFraction: Float = 1f
    private val calendarCompactTimeRollAnimator = AnimatedFloatProperty()
    private val calendarIconRevealAnimator = AnimatedFloatProperty()
    private var calendarIconRevealFraction: Float = 1f
    private var calendarTrailingSlotFraction: Float = 0f
    private val calendarTrailingSlotAnimator = AnimatedFloatProperty()

    var isConsciousGateActive: Boolean = false
        private set
    var isConsciousGateCompact: Boolean = true
        private set
    private var consciousGateFraction: Float = 0f
    private val consciousGateAnimator = AnimatedFloatProperty()
    var consciousGateCompactFraction: Float = 1f
        private set
    private val consciousGateCompactAnimator = AnimatedFloatProperty()
    private var consciousGateBubbleFraction: Float = 0f
    private val consciousGateBubbleAnimator = AnimatedFloatProperty()
    private val consciousGateDockOriginRect = RectF()
    private val consciousGatePillRect = RectF()

    private var consciousGateAppIcon: Bitmap? = null
    private var consciousGateAppLabel: String = ""
    private var consciousGateTimeText: String = "00:00"
    private var consciousGateTimePrev: String = ""
    private var consciousGateTimeRollFraction: Float = 1f
    private val consciousGateTimeRollAnimator = AnimatedFloatProperty()

    private fun updateCalendarTrailingSlot(animate: Boolean = true) {
        val target = if (isMediaPlaybackActive) 1f else 0f
        if (!animate) {
            calendarTrailingSlotAnimator.cancel()
            calendarTrailingSlotFraction = target
            invalidate()
            return
        }
        if (abs(calendarTrailingSlotFraction - target) < 0.01f) return
        calendarTrailingSlotAnimator.animateTo(
            from = calendarTrailingSlotFraction,
            to = target,
            spec = IslandTransitionSpec.ModeChange,
            onUpdate = {
                calendarTrailingSlotFraction = it
                invalidate()
            },
        )
    }

    var animatedNotificationFraction: Float = 0f
        private set
    private val notificationAnimator = AnimatedFloatProperty()
    private val notificationPillRect = RectF()
    private val notificationIconClipPath = Path()
    private val notificationContentClipPath = Path()
    private val catchUpIconClipPath = Path()

    private val queuedNotificationAlerts = mutableListOf<ActiveNotificationAlert>()
    private val bubbleFractions = floatArrayOf(0f, 0f)
    private val bubbleAnimators = arrayOf(AnimatedFloatProperty(), AnimatedFloatProperty())
    private val bubbleSlots = floatArrayOf(0f, 1f)
    private val bubbleSlotAnimators = arrayOf(AnimatedFloatProperty(), AnimatedFloatProperty())
    private val catchUpUnreadIconRect = RectF()

    private val leadingBubbleRects = mutableMapOf<Any, RectF>()
    private val trailingBubbleRects = mutableMapOf<Any, RectF>()

    private val mediaRevealOriginRect = RectF()
    private var mediaMorphFraction: Float = 1f
    private val mediaMorphAnimator = AnimatedFloatProperty()
    private val mediaDockOriginRect = RectF()

    // Media/Conscious Gate takes one of the TOTAL_BUBBLE_BUDGET slots when docked; the queue gets the rest.
    private val isConsciousGateBubbleOccupyingSlot: Boolean
        get() = isConsciousGateActive && !isCatchUpMode
    private val isMediaBubbleOccupyingSlot: Boolean
        get() = isMediaPlaybackActive && !isCatchUpMode
    private val maxQueuedBubbleSlots: Int
        get() = TOTAL_BUBBLE_BUDGET - (if (isConsciousGateBubbleOccupyingSlot || isMediaBubbleOccupyingSlot) 1 else 0)

    private fun trimQueueTo(maxSize: Int) {
        while (queuedNotificationAlerts.size > maxSize) {
            queuedNotificationAlerts.removeAt(0)
            shiftBubbleRankDown()
        }
    }

    private var isMerging: Boolean = false
    private var mergeFraction: Float = 1.0f
    private val mergeAnimator = AnimatedFloatProperty()
    private var previousAlert: ActiveNotificationAlert? = null
    private var mergeSourceBubbleLeft: Float = 0f
    private var mergeSourceBubbleCenterX: Float = 0f
    private var mergeSourceBubbleCenterY: Float = 0f
    private var mergeSourcePillLeft: Float = 0f
    private var mergeSourcePillRight: Float = 0f

    private val leftMarquee = MarqueeController()
    private val rightMarquee = MarqueeController()
    private val marqueeFadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    var dragTranslationX: Float = 0f
        private set
    var dragTranslationY: Float = 0f
        private set
    var dragScale: Float = 1.0f
        private set

    var dragCollapseFraction: Float = 0f
        private set
    private var dragCollapseTarget = DragCollapseTarget.CAMERA

    fun updateDragCollapseFraction(
        fraction: Float,
        target: DragCollapseTarget = DragCollapseTarget.CAMERA,
    ) {
        dragCollapseTarget = target
        dragCollapseFraction = fraction.coerceIn(0f, 1f)
        invalidate()
    }

    fun updateDragTranslation(dx: Float, dy: Float = 0f) {
        dragTranslationX = dx
        dragTranslationY = dy
        invalidate()
    }

    fun resetDragOffset() {
        dragCollapseFraction = 0f
        dragTranslationX = 0f
        dragTranslationY = 0f
        dragScale = 1.0f
        invalidate()
    }

    private val dragAnimator = AnimatedFloatProperty()

    fun animateHorizontalSwipeDismiss(direction: Float, onEnd: () -> Unit) {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val startX = dragTranslationX
        val targetX = if (direction > 0f) (screenWidth * 0.85f) else (-screenWidth * 0.85f)
        val hasQueued = queuedNotificationAlerts.isNotEmpty()
        val startNotif = animatedNotificationFraction
        dragAnimator.animateTo(
            from = 0f,
            to = 1.0f,
            spec = IslandTransitionSpec.SwipeDismiss,
            onUpdate = { f ->
                dragTranslationX = startX + (targetX - startX) * f
                if (!hasQueued) {
                    animatedNotificationFraction = startNotif * (1f - f)
                }
                invalidate()
            },
            onEnd = {
                resetDragOffset()
                animatedNotificationFraction = 1f
                onEnd()
            },
        )
    }

    fun animateDragDismissCollapse(
        target: DragCollapseTarget = DragCollapseTarget.CAMERA,
        onEnd: () -> Unit,
    ) {
        dragCollapseTarget = target
        val startVal = dragCollapseFraction
        dragAnimator.animateTo(
            from = startVal,
            to = 1.0f,
            spec = IslandTransitionSpec.DragCollapse,
            onUpdate = {
                dragCollapseFraction = it
                invalidate()
            },
            onEnd = {
                resetDragOffset()
                onEnd()
            },
        )
    }

    fun animateDragSnapBack() {
        val startVal = dragCollapseFraction
        val startX = dragTranslationX
        val startY = dragTranslationY
        val startScale = dragScale

        dragAnimator.animateTo(
            from = 0f,
            to = 1f,
            spec = IslandTransitionSpec.DragSnapBack,
            onUpdate = { f ->
                dragCollapseFraction = startVal * (1f - f)
                dragTranslationX = startX * (1f - f)
                dragTranslationY = startY * (1f - f)
                dragScale = startScale + (1.0f - startScale) * f
                invalidate()
            },
            onEnd = {
                dragCollapseTarget = DragCollapseTarget.CAMERA
            },
        )
    }

    var onDismissAnimationEnd: (() -> Unit)? = null
    var onAlertsChanged: (() -> Unit)? = null

    private val notificationPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private val googleSansFlexTypeface: Typeface? by lazy {
        try {
            ResourcesCompat.getFont(context, R.font.google_sans_flex)
        } catch (_: Exception) {
            null
        }
    }

    private val notificationSenderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = googleSansFlexTypeface ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val notificationBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = googleSansFlexTypeface ?: Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private val consciousGateTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = googleSansFlexTypeface ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
        fontFeatureSettings = "tnum"
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val catchUpUnreadPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val actionButtonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(40, 255, 255, 255)
    }

    private val actionButtonTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = googleSansFlexTypeface ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val actionButtonPath = Path()
    private val actionButtonRects = mutableMapOf<NotificationActionItem, RectF>()
    private var highlightedAction: NotificationActionItem? = null
    private var actionExecutionFraction: Float = 0f
    private val actionExecutionAnimator = AnimatedFloatProperty()

    fun animateActionExecution(action: NotificationActionItem, onComplete: () -> Unit) {
        highlightedAction = action
        actionExecutionFraction = 0f
        actionExecutionAnimator.animateTo(
            from = 0f,
            to = 1f,
            spec = IslandTransitionSpec.ActionExecution,
            onUpdate = {
                actionExecutionFraction = it
                invalidate()
            },
            onEnd = {
                highlightedAction = null
                actionExecutionFraction = 0f
                invalidate()
                onComplete()
            },
        )
    }

    fun showNotificationAlert(alert: ActiveNotificationAlert) {
        if (!isIslandEnabled) return

        if (isMediaFullPlayerActive) {
            setMediaFullPlayer(false)
        }

        if (isConsciousGateActive && !isNotificationAlertActive) {
            trailingBubbleRects[CONSCIOUS_GATE_BUBBLE_KEY]?.let { consciousGateDockOriginRect.set(it) }
        }
        if (isConsciousGateActive) setConsciousGateBubbleVisible(true)

        if (isMediaPlaybackActive && !isNotificationAlertActive) {
            trailingBubbleRects[MEDIA_BUBBLE_KEY]?.let { mediaDockOriginRect.set(it) }
        }
        if (isMediaPlaybackActive) setMediaBubbleVisible(true)
        canEnterCatchUp = true

        if (isCatchUpMode) {
            if (activeNotificationAlert?.key == alert.key) {
                activeNotificationAlert = alert
                invalidate()
            } else {
                dismissCatchUpAndShow(alert)
            }
            return
        }

        if (!isNotificationAlertActive || activeNotificationAlert == null) {
            activeNotificationAlert = alert
            isNotificationAlertActive = true

            val startVal = animatedNotificationFraction
            notificationAnimator.animateTo(
                from = startVal,
                to = 1.0f,
                spec = IslandTransitionSpec.ContentShow,
                onUpdate = {
                    animatedNotificationFraction = it
                    invalidate()
                },
            )
            onAlertsChanged?.invoke()
            return
        }

        if (activeNotificationAlert?.key == alert.key) {
            activeNotificationAlert = alert
            invalidate()
            return
        }

        val existingIdx = queuedNotificationAlerts.indexOfFirst { it.key == alert.key }
        if (existingIdx >= 0) {
            queuedNotificationAlerts[existingIdx] = alert
            invalidate()
            return
        }

        trimQueueTo((maxQueuedBubbleSlots - 1).coerceAtLeast(0))
        queuedNotificationAlerts.add(alert)
        val bubbleIdx = queuedNotificationAlerts.size - 1
        animateBubbleIn(bubbleIdx)
        onAlertsChanged?.invoke()
    }

    fun showMediaPlayback(title: String, artist: String, artwork: Bitmap?, startCompact: Boolean = false) {
        if (!isIslandEnabled) return

        val wasActive = isMediaPlaybackActive
        val contentChanged = wasActive && (title != mediaTitle || artist != mediaArtist || artwork !== mediaArtwork)
        if (contentChanged) {
            mediaTitlePrev = mediaTitle
            mediaArtistPrev = mediaArtist
            mediaArtworkPrev = mediaArtwork
            mediaContentRollFraction = 0f
            mediaContentRollAnimator.animateTo(
                from = 0f,
                to = 1f,
                spec = IslandTransitionSpec.ModeChange,
                onUpdate = {
                    mediaContentRollFraction = it
                    invalidate()
                },
            )
        }
        isMediaPlaybackActive = true
        mediaTitle = title
        mediaArtist = artist
        mediaArtwork = artwork
        artworkStyle.update(artwork)
        if (isCalendarActive) {
            updateCalendarTrailingSlot(animate = true)
        }
        if (isNotificationAlertActive) setMediaBubbleVisible(true)
        if (!wasActive) {
            equalizerAnimator.start { invalidate() }
            trimQueueTo(maxQueuedBubbleSlots)
            isMediaCompact = startCompact
            mediaCompactFraction = if (startCompact) 1f else 0f
            mediaAnimator.animateTo(
                from = mediaFraction,
                to = 1f,
                spec = IslandTransitionSpec.ContentShow,
                onUpdate = {
                    mediaFraction = it
                    invalidate()
                },
            )
            onAlertsChanged?.invoke()
        } else {
            invalidate()
        }
    }

    fun updateMediaCompactFraction(fraction: Float) {
        if (!isMediaPlaybackActive || isMediaFullPlayerActive) return
        mediaCompactAnimator.cancel()
        mediaCompactFraction = fraction.coerceIn(0f, 1f)
        invalidate()
    }

    fun animateMediaToCompact(targetCompact: Boolean) {
        if (!isMediaPlaybackActive) return
        isMediaCompact = targetCompact
        if (targetCompact) setMediaFullPlayer(false)
        resetDragOffset()
        val fromVal = mediaCompactFraction
        val targetVal = if (targetCompact) 1f else 0f
        mediaCompactAnimator.animateTo(
            from = fromVal,
            to = targetVal,
            spec = if (targetCompact) IslandTransitionSpec.ModeChange else IslandTransitionSpec.DragSnapBack,
            onUpdate = {
                mediaCompactFraction = it
                invalidate()
            },
            onEnd = {
                onAlertsChanged?.invoke()
            },
        )
        onAlertsChanged?.invoke()
    }

    fun setMediaCompact(compact: Boolean) {
        if (!isMediaPlaybackActive || isMediaCompact == compact) return
        animateMediaToCompact(compact)
    }

    fun toggleMediaFullPlayer(): Boolean {
        if (!isMediaPlaybackActive || isMediaCompact) return isMediaFullPlayerActive
        setMediaFullPlayer(!isMediaFullPlayerActive)
        return isMediaFullPlayerActive
    }

    fun setMediaFullPlayer(active: Boolean) {
        if (isMediaFullPlayerActive == active) return
        isMediaFullPlayerActive = active
        mediaFullPlayerAnimator.animateTo(
            from = mediaFullPlayerFraction,
            to = if (active) 1f else 0f,
            spec = IslandTransitionSpec.ModeChange,
            onUpdate = {
                mediaFullPlayerFraction = it
                invalidate()
            },
            onEnd = {
                onAlertsChanged?.invoke()
            },
        )
        refreshWavyProgressAnimation()
        onAlertsChanged?.invoke()
    }

    fun setMediaLiked(liked: Boolean) {
        if (isMediaLiked == liked) return
        isMediaLiked = liked
        invalidate()
    }

    fun setMediaTransportPlaying(playing: Boolean) {
        if (isMediaTransportPlaying == playing) return
        isMediaTransportPlaying = playing
        refreshWavyProgressAnimation()
        invalidate()
    }

    private fun refreshWavyProgressAnimation() {
        wavyProgress.setRunning(isMediaFullPlayerActive && isMediaTransportPlaying)
    }

    fun updateMediaProgress(fraction: Float) {
        mediaProgressFraction = fraction.coerceIn(0f, 1f)
        if (mediaFullPlayerFraction > 0.01f) invalidate()
    }

    fun getMediaControlActionAt(x: Float, y: Float): Int {
        if (mediaFullPlayerFraction < 0.6f) return -1
        val pad = 10f * density
        fun hit(r: RectF) = r.width() > 0f && RectF(r.left - pad, r.top - pad, r.right + pad, r.bottom + pad).contains(x, y)
        return when {
            hit(mediaLikeButtonRect) -> 0
            hit(mediaPlayPauseButtonRect) -> 1
            hit(mediaNextButtonRect) -> 2
            else -> -1
        }
    }

    fun setMediaPaused(paused: Boolean) {
        if (!isMediaPlaybackActive) return
        if (paused) {
            setMediaCompact(true)
            equalizerAnimator.freezeFlat { invalidate() }
        } else if (equalizerAnimator.isFrozenFlat()) {
            equalizerAnimator.start { invalidate() }
        }
    }

    fun dismissMediaPlayback() {
        if (!isMediaPlaybackActive) return
        mediaCompactAnimator.cancel()
        mediaBubbleAnimator.cancel()
        mediaMorphAnimator.cancel()
        mediaMorphFraction = 1f
        mediaRevealOriginRect.setEmpty()
        mediaContentRollAnimator.cancel()
        mediaContentRollFraction = 1f
        mediaFullPlayerAnimator.cancel()
        isMediaFullPlayerActive = false
        mediaFullPlayerFraction = 0f
        mediaProgressFraction = 0f
        wavyProgress.setRunning(false)
        artworkStyle.clear()
        isMediaPlaybackActive = false
        if (isCalendarActive) {
            updateCalendarTrailingSlot(animate = true)
        }
        mediaAnimator.animateTo(
            from = mediaFraction,
            to = 0f,
            spec = IslandTransitionSpec.MediaDismiss,
            onUpdate = {
                mediaFraction = it
                invalidate()
            },
            onEnd = {
                equalizerAnimator.stop()
                isMediaCompact = false
                mediaFraction = 0f
                mediaCompactFraction = 0f
                mediaBubbleFraction = 0f
                mediaTitle = ""
                mediaArtist = ""
                mediaArtwork = null
                onDismissAnimationEnd?.invoke()
                onAlertsChanged?.invoke()
            },
        )
    }

    fun showCalendarEvent(title: String, fullTimeText: String, compactTimeText: String, location: String) {
        if (!isIslandEnabled || isNotificationAlertActive || (isMediaPlaybackActive && !isMediaCompact)) return
        val wasActive = isCalendarActive
        val mergingFromMedia = !wasActive && isMediaPlaybackActive
        if (wasActive && !mergingFromMedia && compactTimeText != calendarCompactTimeText) {
            calendarCompactTimePrev = calendarCompactTimeText
            calendarCompactTimeRollFraction = 0f
            calendarCompactTimeRollAnimator.animateTo(
                from = 0f,
                to = 1f,
                spec = IslandTransitionSpec.ModeChange,
                onUpdate = {
                    calendarCompactTimeRollFraction = it
                    invalidate()
                },
            )
        }
        isCalendarActive = true
        calendarEventTitle = title
        calendarFullTimeText = fullTimeText
        calendarCompactTimeText = compactTimeText
        calendarLocationText = location
        updateCalendarTrailingSlot(animate = wasActive)
        if (mergingFromMedia) {
            mediaCompactAnimator.cancel()
            isCalendarCompact = true
            calendarFraction = 1f
            calendarCompactFraction = mediaCompactFraction
            calendarIconRevealAnimator.cancel()
            calendarIconRevealFraction = 0f
            calendarCompactAnimator.animateTo(
                from = calendarCompactFraction,
                to = 1f,
                spec = IslandTransitionSpec.ModeChange,
                onUpdate = {
                    calendarCompactFraction = it
                    invalidate()
                },
            )
            calendarIconRevealAnimator.animateTo(
                from = 0f,
                to = 1f,
                spec = IslandTransitionSpec.ModeChange,
                onUpdate = {
                    calendarIconRevealFraction = it
                    invalidate()
                },
            )
            onAlertsChanged?.invoke()
        } else if (!wasActive) {
            isCalendarCompact = true
            calendarCompactFraction = 1f
            calendarIconRevealFraction = 1f
            calendarAnimator.animateTo(
                from = calendarFraction,
                to = 1f,
                spec = IslandTransitionSpec.ContentShow,
                onUpdate = {
                    calendarFraction = it
                    invalidate()
                },
            )
            onAlertsChanged?.invoke()
        } else {
            invalidate()
        }
    }

    fun setCalendarCompact(compact: Boolean) {
        if (!isCalendarActive || isCalendarCompact == compact) return
        isCalendarCompact = compact
        calendarCompactAnimator.animateTo(
            from = calendarCompactFraction,
            to = if (compact) 1f else 0f,
            spec = IslandTransitionSpec.ModeChange,
            onUpdate = {
                calendarCompactFraction = it
                invalidate()
            },
            onEnd = {
                onAlertsChanged?.invoke()
            },
        )
        onAlertsChanged?.invoke()
    }

    fun toggleCalendarExpansion(): Boolean {
        if (!isCalendarActive) return false
        setCalendarCompact(!isCalendarCompact)
        return !isCalendarCompact
    }

    fun dismissCalendarEvent() {
        if (!isCalendarActive) return
        calendarCompactAnimator.cancel()
        calendarIconRevealAnimator.cancel()
        calendarIconRevealFraction = 1f
        calendarCompactTimeRollAnimator.cancel()
        calendarCompactTimeRollFraction = 1f
        calendarTrailingSlotAnimator.cancel()
        calendarTrailingSlotFraction = 0f
        calendarAnimator.animateTo(
            from = calendarFraction,
            to = 0f,
            spec = IslandTransitionSpec.MediaDismiss,
            onUpdate = {
                calendarFraction = it
                invalidate()
            },
            onEnd = {
                isCalendarActive = false
                isCalendarCompact = true
                calendarFraction = 0f
                calendarCompactFraction = 1f
                calendarEventTitle = ""
                calendarLocationText = ""
                calendarCompactTimeText = ""
                calendarCompactTimePrev = ""
                calendarFullTimeText = ""
                if (isMediaPlaybackActive) reclaimMediaPillFromBubble()
                onDismissAnimationEnd?.invoke()
                onAlertsChanged?.invoke()
            },
        )
    }

    private fun computeCalendarBounds(compactFraction: Float): RectF {
        val height = cameraRadiusPx * 2f + 14f * density
        val top = cameraCenterY - height / 2f
        val bottom = cameraCenterY + height / 2f
        val iconSize = (height - 14f * density).coerceAtLeast(16f * density)
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val isCenterCamera = abs(cameraCenterX - screenWidth / 2f) < 50f * density
        val maxWidth = (maxWidthDp * density).coerceAtMost(screenWidth - 16f * density)
        val normalHalfWidth = (maxWidth / 2f).coerceAtMost(
            minOf(cameraCenterX - 8f * density, screenWidth - cameraCenterX - 8f * density),
        )
        val normalBounds = if (isCenterCamera) {
            RectF(cameraCenterX - normalHalfWidth, top, cameraCenterX + normalHalfWidth, bottom)
        } else {
            val left = (cameraCenterX - cameraRadiusPx - cutoutGap).coerceAtLeast(8f * density)
            val right = (left + maxWidth).coerceAtMost(screenWidth - 8f * density)
            RectF(left, top, right, bottom)
        }

        val compactBounds = if (isCenterCamera) {
            val sideWidth = cameraRadiusPx + cutoutGap + iconSize + cutoutGap * 2f
            RectF(
                (cameraCenterX - sideWidth - idleIndicator.leftInset()).coerceAtLeast(8f * density),
                top,
                (cameraCenterX + sideWidth + idleIndicator.rightInset()).coerceAtMost(screenWidth - 8f * density),
                bottom,
            )
        } else {
            val left = (cameraCenterX - cameraRadiusPx - cutoutGap).coerceAtLeast(8f * density)
            val right = (cameraCenterX + cameraRadiusPx + cutoutIconGap + iconSize + cutoutGap * 2f)
                .coerceAtMost(screenWidth - 8f * density)
            RectF(left, top, right, bottom)
        }

        return RectF(
            normalBounds.left + (compactBounds.left - normalBounds.left) * compactFraction,
            top,
            normalBounds.right + (compactBounds.right - normalBounds.right) * compactFraction,
            bottom,
        )
    }

    private fun drawCalendarEvent(canvas: Canvas) {
        val height = cameraRadiusPx * 2f + 14f * density
        val initialLeft = cameraCenterX - cameraRadiusPx
        val initialRight = cameraCenterX + cameraRadiusPx
        val target = computeCalendarBounds(calendarCompactFraction)
        val left = initialLeft + (target.left - initialLeft) * calendarFraction
        val right = initialRight + (target.right - initialRight) * calendarFraction
        calendarPillRect.set(left, target.top, right, target.bottom)

        notificationPillPaint.color = Color.BLACK
        notificationPillPaint.alpha = 255
        canvas.drawRoundRect(calendarPillRect, height / 2f, height / 2f, notificationPillPaint)

        val alpha = (calendarFraction * 255f).toInt().coerceIn(0, 255)
        val iconSize = (height - 14f * density).coerceAtLeast(16f * density)
        val pad = (height - iconSize) / 2f
        val iconRect = RectF(calendarPillRect.left + pad, calendarPillRect.top + pad, calendarPillRect.left + pad + iconSize, calendarPillRect.top + pad + iconSize)
        if (calendarIconRevealFraction < 0.999f) {
            val art = mediaArtwork
            val outAlpha = (alpha * (1f - calendarIconRevealFraction)).toInt().coerceIn(0, 255)
            if (art != null && outAlpha > 0) {
                catchUpIconClipPath.reset()
                catchUpIconClipPath.addCircle(iconRect.centerX(), iconRect.centerY(), iconSize / 2f, Path.Direction.CW)
                canvas.save()
                canvas.clipPath(catchUpIconClipPath)
                iconPaint.alpha = outAlpha
                canvas.drawBitmap(art, null, iconRect, iconPaint)
                canvas.restore()
            }
        }
        calendarIconBitmap?.let {
            iconPaint.alpha = (alpha * calendarIconRevealFraction).toInt().coerceIn(0, 255)
            iconPaint.colorFilter = PorterDuffColorFilter(materialYouAccentColor(), PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(it, null, iconRect, iconPaint)
        }
        iconPaint.colorFilter = null

        val compactAlpha = (alpha * calendarCompactFraction).toInt().coerceIn(0, 255)
        if (compactAlpha > 0) {
            val trailingSlotT = calendarTrailingSlotFraction
            val mediaIconRect = RectF(calendarPillRect.right - pad - iconSize, calendarPillRect.top + pad, calendarPillRect.right - pad, calendarPillRect.top + pad + iconSize)

            if (trailingSlotT > 0.001f) {
                val mediaSlotAlpha = (compactAlpha * trailingSlotT).toInt().coerceIn(0, 255)
                trailingBubbleRects[MEDIA_BUBBLE_KEY] = RectF(mediaIconRect)
                val art = mediaArtwork
                val artAlpha = (mediaSlotAlpha * calendarIconRevealFraction).toInt().coerceIn(0, 255)
                if (art != null && artAlpha > 0) {
                    catchUpIconClipPath.reset()
                    catchUpIconClipPath.addCircle(mediaIconRect.centerX(), mediaIconRect.centerY(), iconSize / 2f, Path.Direction.CW)
                    canvas.save()
                    canvas.clipPath(catchUpIconClipPath)
                    iconPaint.alpha = artAlpha
                    canvas.drawBitmap(art, null, mediaIconRect, iconPaint)
                    canvas.restore()
                }
                if (art == null || calendarIconRevealFraction < 0.999f) {
                    equalizerPaint.color = materialYouAccentColor()
                    equalizerPaint.alpha = if (art != null) {
                        (mediaSlotAlpha * (1f - calendarIconRevealFraction)).toInt().coerceIn(0, 255)
                    } else {
                        mediaSlotAlpha
                    }
                    if (equalizerPaint.alpha > 0) {
                        drawEqualizerIcon(canvas, mediaIconRect, equalizerAnimator.barLevels, equalizerPaint)
                    }
                }
            }

            if (trailingSlotT < 0.999f) {
                val timeSlotAlpha = (compactAlpha * (1f - trailingSlotT)).toInt().coerceIn(0, 255)
                val compactTextRight = calendarPillRect.right - pad
                val compactTextLeft = iconRect.right + 6f * density
                if (compactTextRight > compactTextLeft && timeSlotAlpha > 0) {
                    notificationBodyPaint.textSize = (height * 0.34f).coerceIn(12f * density, 18f * density)
                    val textY = (calendarPillRect.top + calendarPillRect.bottom) / 2f + notificationBodyPaint.textSize * 0.35f
                    drawRollingText(
                        canvas = canvas,
                        paint = notificationBodyPaint,
                        oldText = calendarCompactTimePrev,
                        newText = calendarCompactTimeText,
                        fraction = calendarCompactTimeRollFraction,
                        edgeX = compactTextRight,
                        baseY = textY,
                        clipLeft = compactTextLeft,
                        clipTop = calendarPillRect.top,
                        clipRight = compactTextRight,
                        clipBottom = calendarPillRect.bottom,
                        baseAlpha = timeSlotAlpha,
                    )
                }
            }
        }

        val textAlpha = (alpha * (1f - calendarCompactFraction)).toInt().coerceIn(0, 255)
        if (textAlpha > 0) {
            notificationSenderPaint.alpha = textAlpha
            notificationBodyPaint.alpha = textAlpha
            notificationSenderPaint.textSize = (height * 0.34f).coerceIn(12f * density, 18f * density)
            notificationBodyPaint.textSize = notificationSenderPaint.textSize
            val nameLeft = iconRect.right + 8f * density
            val nameRight = cameraCenterX - cameraRadiusPx - cutoutGap
            val timeLeft = cameraCenterX + cameraRadiusPx + cutoutGap
            val timeRight = pillTextRightEdge(calendarPillRect.right)
            val calendarRevealFraction = calendarFraction * (1f - calendarCompactFraction)
            val rightText = if (calendarLocationText.isNotBlank()) "$calendarFullTimeText • $calendarLocationText" else calendarFullTimeText
            drawMarqueeText(canvas, calendarEventTitle, notificationSenderPaint, leftMarquee, nameLeft, nameRight, calendarPillRect.top, calendarPillRect.bottom, false, height / 2f, calendarRevealFraction)
            drawMarqueeText(canvas, rightText, notificationBodyPaint, rightMarquee, timeLeft, timeRight, calendarPillRect.top, calendarPillRect.bottom, true, height / 2f, calendarRevealFraction, alignTextToEnd = true)
        }
    }

    fun showConsciousGateSession(
        appIcon: Bitmap?,
        appLabel: String,
        timeFormatted: String,
    ) {
        if (!isIslandEnabled) return
        val wasActive = isConsciousGateActive && consciousGateFraction > 0.05f
        if (wasActive && timeFormatted != consciousGateTimeText) {
            consciousGateTimePrev = consciousGateTimeText
            consciousGateTimeRollFraction = 0f
            consciousGateTimeRollAnimator.animateTo(
                from = 0f,
                to = 1f,
                spec = IslandTransitionSpec.ModeChange,
                onUpdate = {
                    consciousGateTimeRollFraction = it
                    invalidate()
                },
            )
        }
        isConsciousGateActive = true
        consciousGateAppIcon = appIcon
        consciousGateAppLabel = appLabel
        consciousGateTimeText = timeFormatted

        if (!wasActive || consciousGateFraction < 0.99f) {
            consciousGateAnimator.cancel()
            isConsciousGateCompact = true
            consciousGateCompactFraction = 1f
            consciousGateAnimator.animateTo(
                from = consciousGateFraction,
                to = 1f,
                spec = IslandTransitionSpec.ContentShow,
                onUpdate = {
                    consciousGateFraction = it
                    invalidate()
                },
            )
            onAlertsChanged?.invoke()
        } else {
            invalidate()
        }
    }

    fun dismissConsciousGateSession() {
        if (!isConsciousGateActive) return
        consciousGateCompactAnimator.cancel()
        consciousGateTimeRollAnimator.cancel()
        consciousGateTimeRollFraction = 1f
        consciousGateBubbleAnimator.cancel()
        consciousGateBubbleFraction = 0f
        consciousGateAnimator.cancel()
        consciousGateAnimator.animateTo(
            from = consciousGateFraction,
            to = 0f,
            spec = IslandTransitionSpec.MediaDismiss,
            onUpdate = {
                consciousGateFraction = it
                invalidate()
            },
            onEnd = {
                if (consciousGateFraction <= 0.01f) {
                    isConsciousGateActive = false
                    isConsciousGateCompact = true
                    consciousGateFraction = 0f
                    consciousGateCompactFraction = 1f
                    consciousGateAppIcon = null
                    consciousGateAppLabel = ""
                    consciousGateTimeText = "00:00"
                    consciousGateTimePrev = ""
                    onDismissAnimationEnd?.invoke()
                    onAlertsChanged?.invoke()
                }
            },
        )
    }

    fun updateConsciousGateCompactFraction(fraction: Float) {
        if (!isConsciousGateActive) return
        consciousGateCompactAnimator.cancel()
        consciousGateCompactFraction = fraction.coerceIn(0f, 1f)
        invalidate()
    }

    fun animateConsciousGateToCompact(targetCompact: Boolean) {
        if (!isConsciousGateActive) return
        isConsciousGateCompact = targetCompact
        resetDragOffset()
        val fromVal = consciousGateCompactFraction
        val targetVal = if (targetCompact) 1f else 0f
        consciousGateCompactAnimator.animateTo(
            from = fromVal,
            to = targetVal,
            spec = if (targetCompact) IslandTransitionSpec.ModeChange else IslandTransitionSpec.DragSnapBack,
            onUpdate = {
                consciousGateCompactFraction = it
                invalidate()
            },
            onEnd = {
                onAlertsChanged?.invoke()
            },
        )
        onAlertsChanged?.invoke()
    }

    fun setConsciousGateCompact(compact: Boolean) {
        if (!isConsciousGateActive || isConsciousGateCompact == compact) return
        animateConsciousGateToCompact(compact)
    }

    fun toggleConsciousGateExpansion(): Boolean {
        if (!isConsciousGateActive) return false
        setConsciousGateCompact(!isConsciousGateCompact)
        return !isConsciousGateCompact
    }

    private fun setConsciousGateBubbleVisible(visible: Boolean) {
        if (!isConsciousGateActive) return
        val target = if (visible) 1f else 0f
        consciousGateBubbleAnimator.animateTo(
            from = consciousGateBubbleFraction,
            to = target,
            spec = IslandTransitionSpec.ModeChange,
            onUpdate = {
                consciousGateBubbleFraction = it
                invalidate()
                onAlertsChanged?.invoke()
            },
        )
    }

    private fun computeConsciousGateBounds(compactFraction: Float): RectF {
        val height = cameraRadiusPx * 2f + 14f * density
        val top = cameraCenterY - height / 2f
        val bottom = cameraCenterY + height / 2f
        val iconSize = (height - 14f * density).coerceAtLeast(16f * density)
        val timerIconSize = iconSize * 0.85f
        val pad = (height - iconSize) / 2f
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()

        // Compact Left: appIcon + 4dp gap + timerIcon
        val compactLeftWidth = pad + iconSize + 4f * density + timerIconSize + cutoutIconGap
        val compactTargetLeft = (cameraCenterX - cameraRadiusPx - compactLeftWidth - idleIndicator.leftInset()).coerceAtLeast(8f * density)

        // Compact Right: Fixed width for "00:00" tabular time text
        consciousGateTimePaint.textSize = (height * 0.34f).coerceIn(12f * density, 18f * density)
        val fixedTimeWidth = consciousGateTimePaint.measureText("0") * 4f + consciousGateTimePaint.measureText(":")
        val compactRightWidth = cutoutIconGap + fixedTimeWidth + pad * 1.5f
        val compactTargetRight = (cameraCenterX + cameraRadiusPx + compactRightWidth + idleIndicator.rightInset()).coerceAtMost(screenWidth - 8f * density)
        val compactBounds = RectF(compactTargetLeft, top, compactTargetRight, bottom)

        // Normal Left: appIcon + 4dp gap + timerIcon + 6dp gap + "Wasting time"
        val wastingTimeText = context.getString(R.string.conscious_gate_island_wasting_time)
        notificationSenderPaint.textSize = (height * 0.34f).coerceIn(12f * density, 18f * density)
        val textWidth = notificationSenderPaint.measureText(wastingTimeText)
        val normalLeftWidth = pad + iconSize + 4f * density + timerIconSize + 6f * density + textWidth + cutoutIconGap
        val normalTargetLeft = (cameraCenterX - cameraRadiusPx - normalLeftWidth).coerceAtLeast(8f * density)

        // Normal Right: "$timeText remaining"
        val remainingLabel = context.getString(R.string.conscious_gate_island_remaining)
        notificationBodyPaint.textSize = (height * 0.34f).coerceIn(12f * density, 18f * density)
        val fullRightWidth = fixedTimeWidth + notificationBodyPaint.measureText(" $remainingLabel")
        val normalRightWidth = cutoutIconGap + fullRightWidth + pad * 1.5f
        val normalTargetRight = (cameraCenterX + cameraRadiusPx + normalRightWidth).coerceAtMost(screenWidth - 8f * density)
        val normalBounds = RectF(normalTargetLeft, top, normalTargetRight, bottom)

        return RectF(
            normalBounds.left + (compactBounds.left - normalBounds.left) * compactFraction,
            top,
            normalBounds.right + (compactBounds.right - normalBounds.right) * compactFraction,
            bottom,
        )
    }

    private fun drawConsciousGate(canvas: Canvas) {
        val height = cameraRadiusPx * 2f + 14f * density
        val initialLeft = cameraCenterX - cameraRadiusPx
        val initialRight = cameraCenterX + cameraRadiusPx
        val target = computeConsciousGateBounds(consciousGateCompactFraction)
        val left = initialLeft + (target.left - initialLeft) * consciousGateFraction
        val right = initialRight + (target.right - initialRight) * consciousGateFraction
        consciousGatePillRect.set(left, target.top, right, target.bottom)

        notificationPillPaint.color = Color.BLACK
        notificationPillPaint.alpha = 255
        canvas.drawRoundRect(consciousGatePillRect, height / 2f, height / 2f, notificationPillPaint)

        val alpha = (consciousGateFraction * 255f).toInt().coerceIn(0, 255)
        val iconSize = (height - 14f * density).coerceAtLeast(16f * density)
        val timerIconSize = iconSize * 0.85f
        val pad = (height - iconSize) / 2f

        // App Icon
        val appIconRect = RectF(
            consciousGatePillRect.left + pad,
            consciousGatePillRect.top + pad,
            consciousGatePillRect.left + pad + iconSize,
            consciousGatePillRect.top + pad + iconSize,
        )
        val appIcon = consciousGateAppIcon
        if (appIcon != null) {
            catchUpIconClipPath.reset()
            catchUpIconClipPath.addCircle(appIconRect.centerX(), appIconRect.centerY(), iconSize / 2f, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(catchUpIconClipPath)
            iconPaint.alpha = alpha
            canvas.drawBitmap(appIcon, null, appIconRect, iconPaint)
            canvas.restore()
        }

        // Timer Icon (right to app icon)
        val timerTop = consciousGatePillRect.top + (height - timerIconSize) / 2f
        val timerIconRect = RectF(
            appIconRect.right + 4f * density,
            timerTop,
            appIconRect.right + 4f * density + timerIconSize,
            timerTop + timerIconSize,
        )
        timerIconBitmap?.let {
            iconPaint.alpha = alpha
            iconPaint.colorFilter = PorterDuffColorFilter(materialYouAccentColor(), PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(it, null, timerIconRect, iconPaint)
            iconPaint.colorFilter = null
        }

        // Compact Mode: Time on right side
        val compactAlpha = (alpha * consciousGateCompactFraction).toInt().coerceIn(0, 255)
        if (compactAlpha > 0) {
            val compactTextRight = consciousGatePillRect.right - pad
            val compactTextLeft = cameraCenterX + cameraRadiusPx + cutoutGap
            if (compactTextRight > compactTextLeft) {
                consciousGateTimePaint.textSize = (height * 0.34f).coerceIn(12f * density, 18f * density)
                val textY = (consciousGatePillRect.top + consciousGatePillRect.bottom) / 2f + consciousGateTimePaint.textSize * 0.35f
                drawRollingDigits(
                    canvas = canvas,
                    paint = consciousGateTimePaint,
                    oldText = consciousGateTimePrev,
                    newText = consciousGateTimeText,
                    fraction = consciousGateTimeRollFraction,
                    edgeX = compactTextRight,
                    baseY = textY,
                    clipLeft = compactTextLeft,
                    clipTop = consciousGatePillRect.top,
                    clipRight = compactTextRight,
                    clipBottom = consciousGatePillRect.bottom,
                    baseAlpha = compactAlpha,
                )
            }
        }

        // Normal / Expanded Mode: "Wasting time" on left + "$timeText remaining" on right
        val textAlpha = (alpha * (1f - consciousGateCompactFraction)).toInt().coerceIn(0, 255)
        if (textAlpha > 0) {
            notificationSenderPaint.alpha = textAlpha
            notificationBodyPaint.alpha = textAlpha
            notificationSenderPaint.textSize = (height * 0.34f).coerceIn(12f * density, 18f * density)
            notificationBodyPaint.textSize = notificationSenderPaint.textSize

            val nameLeft = timerIconRect.right + 6f * density
            val nameRight = cameraCenterX - cameraRadiusPx - cutoutGap
            val timeLeft = cameraCenterX + cameraRadiusPx + cutoutGap
            val timeRight = pillTextRightEdge(consciousGatePillRect.right)
            val revealFraction = consciousGateFraction * (1f - consciousGateCompactFraction)

            val wastingTimeText = context.getString(R.string.conscious_gate_island_wasting_time)
            val remainingLabel = context.getString(R.string.conscious_gate_island_remaining)
            val rightText = "$consciousGateTimeText $remainingLabel"

            drawMarqueeText(
                canvas, wastingTimeText, notificationSenderPaint, leftMarquee,
                nameLeft, nameRight, consciousGatePillRect.top, consciousGatePillRect.bottom,
                false, height / 2f, revealFraction
            )
            drawMarqueeText(
                canvas, rightText, notificationBodyPaint, rightMarquee,
                timeLeft, timeRight, consciousGatePillRect.top, consciousGatePillRect.bottom,
                true, height / 2f, revealFraction, alignTextToEnd = true
            )
        }
    }

    private fun setMediaBubbleVisible(visible: Boolean) {
        if (!isMediaPlaybackActive) return
        val target = if (visible) 1f else 0f
        mediaBubbleAnimator.animateTo(
            from = mediaBubbleFraction,
            to = target,
            spec = IslandTransitionSpec.ModeChange,
            onUpdate = {
                mediaBubbleFraction = it
                invalidate()
                onAlertsChanged?.invoke()
            },
        )
    }

    private val idleHost = object : IslandIdleHost {
        override val density: Float get() = this@IslandOverlayView.density
        override val cameraCenterX: Float get() = this@IslandOverlayView.cameraCenterX
        override val cameraCenterY: Float get() = this@IslandOverlayView.cameraCenterY
        override val cameraRadiusPx: Float get() = this@IslandOverlayView.cameraRadiusPx
        override val cutoutGap: Float get() = this@IslandOverlayView.cutoutGap
        override val screenWidth: Float get() = resources.displayMetrics.widthPixels.toFloat()
        override val canMerge: Boolean get() = abs(cameraCenterX - screenWidth / 2f) < 50f * density
        override val accentColor: Int get() = materialYouAccentColor()

        override fun requestRedraw() = invalidate()

        override fun onInsetChanged() {
            if (currentContentState() != null) onAlertsChanged?.invoke()
        }

        override fun contentState(): IslandContentState? = currentContentState()

        override fun contentBounds(): RectF? = when {
            flashlightPill.isActive -> flashlightPill.pillRect
            isNotificationAlertActive -> notificationPillRect
            isConsciousGateActive && consciousGateFraction > 0.001f -> consciousGatePillRect
            isCalendarActive && calendarFraction > 0.001f -> calendarPillRect
            isMediaPlaybackActive && mediaFraction > 0.001f -> mediaPillRect
            else -> null
        }
    }

    private val flashlightHost = object : FlashlightPillHost {
        override val density: Float get() = this@IslandOverlayView.density
        override val cameraCenterX: Float get() = this@IslandOverlayView.cameraCenterX
        override val cameraCenterY: Float get() = this@IslandOverlayView.cameraCenterY
        override val cameraRadiusPx: Float get() = this@IslandOverlayView.cameraRadiusPx
        override val screenWidth: Float get() = resources.displayMetrics.widthPixels.toFloat()
        override val cutoutGap: Float get() = this@IslandOverlayView.cutoutGap
        override val expandedWidthPx: Float get() = expandedWidthDp * density
        override val expandedCornerRadiusPx: Float get() = expandedCornerRadiusDp * density
        override val accentColor: Int get() = materialYouAccentColor()

        override fun requestRedraw() = invalidate()

        override fun onLayoutChanged() {
            onAlertsChanged?.invoke()
        }

        override fun onDismissed() {
            onDismissAnimationEnd?.invoke()
        }
    }

    private val flashlightPill by lazy { FlashlightPill(context, flashlightHost, googleSansFlexTypeface) }

    val isFlashlightActive: Boolean get() = flashlightPill.isActive
    val isFlashlightExpanded: Boolean get() = flashlightPill.isExpanded
    val flashlightSupportsLevels: Boolean get() = flashlightPill.supportsLevels

    fun showFlashlight(percent: Int, supportsLevels: Boolean) = flashlightPill.show(percent, supportsLevels)

    fun updateFlashlightLevel(percent: Int) = flashlightPill.updateLevel(percent)

    fun dismissFlashlight() = flashlightPill.dismiss()

    fun toggleFlashlightExpansion(): Boolean = flashlightPill.toggleExpansion()

    fun setFlashlightDragging(dragging: Boolean) = flashlightPill.setDragging(dragging)

    fun setFlashlightLevelFraction(fraction: Float) = flashlightPill.setLevelFromUser(fraction)

    fun flashlightHitTest(x: Float, y: Float): FlashlightHit = flashlightPill.hitTest(x, y)

    fun flashlightFractionAt(x: Float): Float = flashlightPill.sliderFractionAt(x)

    private val idleIndicator by lazy { IslandIdleIndicator(context, idleHost, googleSansFlexTypeface) }

    var isIdlePillEnabled: Boolean
        get() = idleIndicator.isEnabled
        set(value) {
            idleIndicator.isEnabled = value
        }

    var isIdleBatteryIcon: Boolean
        get() = idleIndicator.useBatteryIcon
        set(value) {
            idleIndicator.useBatteryIcon = value
        }

    fun setIdleTime(text: String) = idleIndicator.setTime(text)

    fun setIdleBattery(level: Int, charging: Boolean, powerSave: Boolean) =
        idleIndicator.setBattery(level, charging, powerSave)

    fun setIdleBatteryColors(config: IslandBatteryColorConfig) {
        idleIndicator.colorConfig = config
    }

    // (visible fraction, compact fraction) of whatever content onDraw currently renders, by priority.
    private fun currentContentState(): IslandContentState? = when {
        flashlightPill.isActive -> IslandContentState(flashlightPill.showFraction, 0f)
        isNotificationAlertActive -> IslandContentState(animatedNotificationFraction, catchUpFraction.coerceIn(0f, 1f) * (1f - expandedFraction))
        isConsciousGateActive && consciousGateFraction > 0.001f -> IslandContentState(consciousGateFraction, consciousGateCompactFraction)
        isCalendarActive && calendarFraction > 0.001f -> IslandContentState(calendarFraction, calendarCompactFraction)
        isMediaPlaybackActive && mediaFraction > 0.001f -> IslandContentState(mediaFraction, mediaCompactFraction * (1f - mediaFullPlayerFraction))
        else -> null
    }

    // Morphs media's bounds from [origin] to its target with alpha untouched — one smooth resize.
    private fun beginMediaPillMorph(origin: RectF) {
        mediaRevealOriginRect.set(origin)
        mediaMorphFraction = 0f
        mediaMorphAnimator.animateTo(
            from = 0f,
            to = 1f,
            spec = IslandTransitionSpec.ModeChange,
            onUpdate = {
                mediaMorphFraction = it
                invalidate()
            },
            onEnd = {
                mediaRevealOriginRect.setEmpty()
            },
        )
    }

    private fun reclaimMediaPillFromBubble() {
        val bubbleRect = trailingBubbleRects[MEDIA_BUBBLE_KEY] ?: return
        if (bubbleRect.isEmpty) return
        beginMediaPillMorph(bubbleRect)
    }

    fun reclaimMediaFromCalendar() {
        if (!isCalendarActive || !isMediaPlaybackActive) return
        val origin = RectF(calendarPillRect)

        calendarAnimator.cancel()
        calendarCompactAnimator.cancel()
        calendarIconRevealAnimator.cancel()
        calendarIconRevealFraction = 1f
        calendarCompactTimeRollAnimator.cancel()
        calendarCompactTimeRollFraction = 1f
        isCalendarActive = false
        isCalendarCompact = true
        calendarFraction = 0f
        calendarCompactFraction = 1f
        calendarEventTitle = ""
        calendarLocationText = ""
        calendarCompactTimeText = ""
        calendarCompactTimePrev = ""
        calendarFullTimeText = ""

        beginMediaPillMorph(origin)
        onAlertsChanged?.invoke()
    }

    private fun animateBubbleIn(index: Int) {
        if (index !in 0..1) return
        bubbleSlotAnimators[index].cancel()
        bubbleSlots[index] = index.toFloat()
        val startVal = bubbleFractions[index]
        bubbleAnimators[index].animateTo(
            from = startVal,
            to = 1.0f,
            spec = IslandTransitionSpec.BubbleIn,
            onUpdate = {
                bubbleFractions[index] = it
                invalidate()
            },
        )
    }

    private fun shiftBubbleRankDown() {
        bubbleFractions[0] = bubbleFractions[1]
        bubbleFractions[1] = 0f
        bubbleSlots[0] = 1f
        bubbleSlotAnimators[0].animateTo(
            from = 1f,
            to = 0f,
            spec = IslandTransitionSpec.BubbleIn,
            onUpdate = {
                bubbleSlots[0] = it
                invalidate()
            },
        )
        bubbleSlotAnimators[1].cancel()
        bubbleSlots[1] = 1f
    }

    fun getQueuedAlertIndexAt(x: Float, y: Float): Int {
        if (expandedFraction > 0.1f) return -1
        for (i in queuedNotificationAlerts.indices) {
            val rect = leadingBubbleRects[i] ?: continue
            if (i in 0..1 && bubbleFractions[i] > 0.3f) {
                val pad = 6f * density
                val touchRect = RectF(rect.left - pad, rect.top - pad, rect.right + pad, rect.bottom + pad)
                if (touchRect.contains(x, y)) {
                    return i
                }
            }
        }
        return -1
    }

    fun switchToQueuedNotification(index: Int): Boolean {
        if (index !in queuedNotificationAlerts.indices) return false
        stopAllMarquees()

        val outgoingAlert = activeNotificationAlert
        val incomingAlert = queuedNotificationAlerts.removeAt(index)

        mergeSourcePillLeft = notificationPillRect.left
        mergeSourcePillRight = notificationPillRect.right
        val sourceBubbleRect = leadingBubbleRects[index]
        mergeSourceBubbleLeft = sourceBubbleRect?.left?.takeIf { it > 0f } ?: (notificationPillRect.left - 40f * density)
        mergeSourceBubbleCenterX = sourceBubbleRect?.centerX()?.takeIf { it > 0f } ?: (mergeSourceBubbleLeft + 20f * density)
        mergeSourceBubbleCenterY = sourceBubbleRect?.centerY()?.takeIf { it > 0f } ?: notificationPillRect.centerY()

        previousAlert = outgoingAlert
        activeNotificationAlert = incomingAlert
        isMerging = true
        mergeFraction = 0f

        if (index == 0 && queuedNotificationAlerts.isNotEmpty()) {
            shiftBubbleRankDown()
        } else {
            bubbleFractions[index] = 0f
            if (queuedNotificationAlerts.isEmpty()) {
                bubbleFractions[0] = 0f
                bubbleFractions[1] = 0f
            }
        }

        mergeAnimator.animateTo(
            from = 0f,
            to = 1.0f,
            spec = IslandTransitionSpec.Merge,
            onUpdate = {
                mergeFraction = it
                invalidate()
            },
            onEnd = {
                isMerging = false
                previousAlert = null
                mergeFraction = 1.0f
                invalidate()
            },
        )

        onAlertsChanged?.invoke()
        return true
    }

    var isExpanded: Boolean = false
        private set
    var expandedFraction: Float = 0f
        private set
    private val expansionAnimator = AnimatedFloatProperty()

    private var onExpandedStateChanged: ((Boolean) -> Unit)? = null

    fun setOnExpandedStateChangedListener(listener: ((Boolean) -> Unit)?) {
        onExpandedStateChanged = listener
    }

    fun toggleExpansion(): Boolean {
        if (!isNotificationAlertActive && !isMediaPlaybackActive) return false
        setExpandedState(!isExpanded)
        return isExpanded
    }

    fun setExpandedState(expand: Boolean) {
        if (isExpanded == expand) return
        isExpanded = expand
        stopAllMarquees()

        val startVal = expandedFraction
        val targetVal = if (expand) 1.0f else 0.0f
        expansionAnimator.animateTo(
            from = startVal,
            to = targetVal,
            spec = if (expand) IslandTransitionSpec.ExpandOpen else IslandTransitionSpec.ModeChange,
            onUpdate = {
                expandedFraction = it
                invalidate()
            },
            onEnd = {
                onAlertsChanged?.invoke()
            },
        )
        onExpandedStateChanged?.invoke(expand)
        onAlertsChanged?.invoke()
    }

    fun enterCatchUpMode() {
        if (!isNotificationAlertActive || activeNotificationAlert == null || isCatchUpMode) return
        isCatchUpMode = true
        if (isMediaPlaybackActive) setMediaBubbleVisible(false)
        if (isExpanded) {
            isExpanded = false
            expandedFraction = 0f
            expansionAnimator.cancel()
        }
        stopAllMarquees()
        val startVal = catchUpFraction
        catchUpAnimator.animateTo(
            from = startVal,
            to = 1.0f,
            spec = IslandTransitionSpec.ModeChange,
            onUpdate = {
                catchUpFraction = it
                invalidate()
            },
            onEnd = {
                onAlertsChanged?.invoke()
            },
        )
        onCatchUpModeChanged?.invoke(true)
        onAlertsChanged?.invoke()
    }

    fun exitCatchUpMode() {
        if (!isCatchUpMode) return
        isCatchUpMode = false
        if (isMediaPlaybackActive) setMediaBubbleVisible(true)
        val startVal = catchUpFraction
        catchUpFraction = 0f
        catchUpAnimator.animateTo(
            from = startVal,
            to = 0.0f,
            spec = IslandTransitionSpec.ExpandOpen,
            onUpdate = {
                catchUpFraction = it
                invalidate()
            },
            onEnd = {
                onAlertsChanged?.invoke()
            },
        )
        onCatchUpModeChanged?.invoke(false)
        onAlertsChanged?.invoke()
    }

    private fun dismissCatchUpAndShow(newAlert: ActiveNotificationAlert) {
        stopAllMarquees()
        isCatchUpMode = false
        canEnterCatchUp = true
        catchUpAnimator.cancel()

        val startNotif = animatedNotificationFraction
        val startCatchUp = catchUpFraction
        notificationAnimator.animateTo(
            from = 1.0f,
            to = 0.0f,
            spec = IslandTransitionSpec.notificationDismiss(wasExpanded = false),
            onUpdate = { f ->
                animatedNotificationFraction = startNotif * f
                catchUpFraction = startCatchUp * f
                invalidate()
            },
            onEnd = {
                animatedNotificationFraction = 0f
                catchUpFraction = 0f
                activeNotificationAlert = null
                isNotificationAlertActive = false
                showNotificationAlert(newAlert)
            },
        )
        onCatchUpModeChanged?.invoke(false)
        onAlertsChanged?.invoke()
    }

    private fun computeExpandedHeight(alert: ActiveNotificationAlert, width: Float): Float {
        val basePillHeight = cameraRadiusPx * 2f + 14f * density
        val (_, message) = computeSenderAndMessage(alert)
        if (message.isBlank()) {
            return basePillHeight
        }

        val textSize = (basePillHeight * 0.38f).coerceIn(14f * density, 18f * density)
        val bodyTextPaint = TextPaint().apply {
            set(notificationBodyPaint)
            typeface = googleSansFlexTypeface ?: Typeface.create("sans-serif", Typeface.NORMAL)
            setTextSize(textSize)
        }
        val cornerExtraPadding = (expandedCornerRadiusDp * 0.35f * density)
        val innerPadding = (expandedPaddingDp * density) + cornerExtraPadding
        val textWidth = (width - innerPadding * 2).toInt().coerceAtLeast(50)

        val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(message, 0, message.length, bodyTextPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f * density, 1.0f)
                .setMaxLines(7)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                message,
                bodyTextPaint,
                textWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                2f * density,
                true,
            )
        }

        val textHeight = layout.height.toFloat()
        val topPad = expandedTopPaddingDp * density
        val hasActions = alert.actions.isNotEmpty()
        val headerSpacing = if (hasActions) 0f else (4f * density)
        val actionsHeight = if (hasActions) 46f * density else 0f
        val bottomPad = if (hasActions) {
            (expandedPaddingDp * 0.70f * density) + cornerExtraPadding * 0.3f
        } else {
            (expandedPaddingDp * 0.90f * density) + cornerExtraPadding * 0.3f
        }
        val totalHeight = basePillHeight + topPad + headerSpacing + textHeight + actionsHeight + bottomPad
        return totalHeight.coerceAtLeast(basePillHeight)
    }

    private fun computeCollapsedTargetBounds(alert: ActiveNotificationAlert): RectF {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val targetPillHeight = cameraRadiusPx * 2f + 14f * density
        val targetTop = cameraCenterY - targetPillHeight / 2f
        val targetBottom = cameraCenterY + targetPillHeight / 2f
        val iconSize = (targetPillHeight - 14f * density).coerceAtLeast(16f * density)
        val verticalPadding = (targetPillHeight - iconSize) / 2f

        val textSize = (targetPillHeight * 0.38f).coerceIn(13f * density, 20f * density)
        notificationSenderPaint.typeface = googleSansFlexTypeface ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
        notificationSenderPaint.textSize = textSize
        notificationBodyPaint.typeface = googleSansFlexTypeface ?: Typeface.create("sans-serif", Typeface.NORMAL)
        notificationBodyPaint.textSize = textSize

        val isCenterCamera = abs(cameraCenterX - screenWidth / 2f) < 50f * density

        if (isCenterCamera) {
            val minHalfWidth = cameraRadiusPx + iconSize + cutoutGap * 2f + 4f * density
            val maxScreenHalfWidth = minOf(
                cameraCenterX - 8f * density,
                screenWidth - cameraCenterX - 8f * density,
            ).coerceAtLeast(minHalfWidth)
            val halfWidth = (maxWidthDp * density / 2f).coerceAtMost(maxScreenHalfWidth).coerceAtLeast(minHalfWidth)

            val targetLeft = cameraCenterX - halfWidth
            val targetRight = cameraCenterX + halfWidth

            return RectF(targetLeft, targetTop, targetRight, targetBottom)
        } else {
            val targetLeft = (cameraCenterX - cameraRadiusPx - verticalPadding).coerceAtLeast(8f * density)
            val maxAllowedWidthPx = (maxWidthDp * density).coerceAtMost(screenWidth - 16f * density)
            val targetRight = (targetLeft + maxAllowedWidthPx).coerceAtMost(screenWidth - 8f * density).coerceAtLeast(targetLeft + 80f * density)

            return RectF(targetLeft, targetTop, targetRight, targetBottom)
        }
    }

    private fun computeCatchUpTargetBounds(alert: ActiveNotificationAlert): RectF {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val targetPillHeight = cameraRadiusPx * 2f + 14f * density
        val targetTop = cameraCenterY - targetPillHeight / 2f
        val targetBottom = cameraCenterY + targetPillHeight / 2f
        val iconSize = (targetPillHeight - 14f * density).coerceAtLeast(16f * density)
        val verticalPadding = (targetPillHeight - iconSize) / 2f
        val isCenterCamera = abs(cameraCenterX - screenWidth / 2f) < 50f * density
        val trailingSize = if (isMediaPlaybackActive) iconSize else (18f * density)

        if (isCenterCamera) {
            val leftDist = cameraRadiusPx + cutoutGap + iconSize + verticalPadding + 4f * density + idleIndicator.leftInset()
            val rightDist = cameraRadiusPx + cutoutGap + trailingSize + verticalPadding + 4f * density + idleIndicator.rightInset()
            return RectF(cameraCenterX - leftDist, targetTop, cameraCenterX + rightDist, targetBottom)
        } else {
            val targetLeft = (cameraCenterX - cameraRadiusPx - verticalPadding).coerceAtLeast(8f * density)
            val iconLeft = cameraCenterX + cameraRadiusPx + cutoutIconGap
            val targetRight = (iconLeft + iconSize + 14f * density + trailingSize + verticalPadding + 6f * density).coerceAtMost(screenWidth - 8f * density)
            return RectF(targetLeft, targetTop, targetRight, targetBottom)
        }
    }

    private fun computeNotificationTargetBounds(alert: ActiveNotificationAlert): RectF {
        val normalBounds = computeCollapsedTargetBounds(alert)
        val collapsedBounds = if (catchUpFraction > 0f) {
            val catchUpBounds = computeCatchUpTargetBounds(alert)
            RectF(
                normalBounds.left + (catchUpBounds.left - normalBounds.left) * catchUpFraction,
                normalBounds.top + (catchUpBounds.top - normalBounds.top) * catchUpFraction,
                normalBounds.right + (catchUpBounds.right - normalBounds.right) * catchUpFraction,
                normalBounds.bottom + (catchUpBounds.bottom - normalBounds.bottom) * catchUpFraction,
            )
        } else {
            normalBounds
        }

        if (expandedFraction <= 0.001f && !isExpanded) {
            return collapsedBounds
        }

        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val isCenterCamera = abs(cameraCenterX - screenWidth / 2f) < 50f * density

        val maxAllowedWidthPx = (expandedWidthDp * density).coerceAtMost(screenWidth - 16f * density)
        val expTargetLeft: Float
        val expTargetRight: Float

        if (isCenterCamera) {
            val expHalfWidth = (maxAllowedWidthPx / 2f).coerceAtMost(minOf(cameraCenterX - 8f * density, screenWidth - cameraCenterX - 8f * density))
            expTargetLeft = cameraCenterX - expHalfWidth
            expTargetRight = cameraCenterX + expHalfWidth
        } else {
            val basePillHeight = cameraRadiusPx * 2f + 14f * density
            val iconSize = (basePillHeight - 14f * density).coerceAtLeast(16f * density)
            val verticalPadding = (basePillHeight - iconSize) / 2f
            expTargetLeft = (cameraCenterX - cameraRadiusPx - verticalPadding).coerceAtLeast(8f * density)
            expTargetRight = (expTargetLeft + maxAllowedWidthPx).coerceAtMost(screenWidth - 8f * density)
        }

        val expandedWidth = expTargetRight - expTargetLeft
        val expandedHeight = computeExpandedHeight(alert, expandedWidth)
        val expTargetTop = collapsedBounds.top
        val expTargetBottom = expTargetTop + expandedHeight

        val expLeft = collapsedBounds.left + (expTargetLeft - collapsedBounds.left) * expandedFraction
        val expRight = collapsedBounds.right + (expTargetRight - collapsedBounds.right) * expandedFraction
        val expBottom = collapsedBounds.bottom + (expTargetBottom - collapsedBounds.bottom) * expandedFraction

        return RectF(expLeft, expTargetTop, expRight, expBottom)
    }

    fun advanceToNextNotification(): Boolean {
        if (queuedNotificationAlerts.isNotEmpty()) {
            return switchToQueuedNotification(0)
        } else {
            if (isNotificationAlertActive) {
                dismissNotificationAlert()
            }
            return false
        }
    }

    fun removeNotificationByKey(key: String): Boolean {
        if (activeNotificationAlert?.key == key) {
            return advanceToNextNotification()
        }
        val idx = queuedNotificationAlerts.indexOfFirst { it.key == key }
        if (idx >= 0) {
            queuedNotificationAlerts.removeAt(idx)
            if (idx == 0 && queuedNotificationAlerts.isNotEmpty()) {
                shiftBubbleRankDown()
            } else {
                bubbleFractions[idx] = 0f
            }
            invalidate()
            onAlertsChanged?.invoke()
            return true
        }
        return isNotificationAlertActive
    }

    fun dismissNotificationAlert() {
        if (!isNotificationAlertActive && activeNotificationAlert == null) return
        stopAllMarquees()
        isMerging = false
        previousAlert = null
        mergeFraction = 1.0f
        mergeAnimator.cancel()
        queuedNotificationAlerts.clear()
        bubbleFractions[0] = 0f
        bubbleFractions[1] = 0f
        bubbleSlots[0] = 0f
        bubbleSlots[1] = 1f
        for (i in 0..1) {
            bubbleAnimators[i].cancel()
            bubbleSlotAnimators[i].cancel()
        }

        expansionAnimator.cancel()
        catchUpAnimator.cancel()

        val startExpanded = expandedFraction
        val startNotif = animatedNotificationFraction

        notificationAnimator.animateTo(
            from = 1.0f,
            to = 0.0f,
            spec = IslandTransitionSpec.notificationDismiss(wasExpanded = startExpanded > 0.05f),
            onUpdate = { f ->
                animatedNotificationFraction = startNotif * f
                expandedFraction = startExpanded * f
                invalidate()
            },
            onEnd = {
                isExpanded = false
                expandedFraction = 0f
                isCatchUpMode = false
                catchUpFraction = 0f
                isNotificationAlertActive = false
                activeNotificationAlert = null
                animatedNotificationFraction = 0f
                if (isMediaPlaybackActive) {
                    setMediaBubbleVisible(false)
                    reclaimMediaPillFromBubble()
                }
                invalidate()
                onDismissAnimationEnd?.invoke()
                onAlertsChanged?.invoke()
            },
        )
    }

    private fun stopAllMarquees() {
        leftMarquee.stop()
        rightMarquee.stop()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        idleIndicator.cancelAnimations()
        flashlightPill.cancelAnimations()
        notificationAnimator.cancel()
        mergeAnimator.cancel()
        expansionAnimator.cancel()
        mediaBubbleAnimator.cancel()
        for (i in 0..1) {
            bubbleAnimators[i].cancel()
        }
        equalizerAnimator.stop()
        calendarAnimator.cancel()
        calendarCompactAnimator.cancel()
        calendarIconRevealAnimator.cancel()
        calendarCompactTimeRollAnimator.cancel()
        mediaContentRollAnimator.cancel()
        mediaMorphAnimator.cancel()
        mediaFullPlayerAnimator.cancel()
        wavyProgress.setRunning(false)
        stopAllMarquees()
    }

    fun getActiveNotificationAlert(): ActiveNotificationAlert? = activeNotificationAlert

    fun getQueuedAlerts(): List<ActiveNotificationAlert> = queuedNotificationAlerts

    fun getNotificationPillBounds(): RectF = notificationPillRect

    fun getTotalAlertsBounds(): RectF {
        if (flashlightPill.isActive) return flashlightPill.targetBounds()
        val alert = activeNotificationAlert
        if (alert == null) {
            return when {
                isConsciousGateActive -> {
                    val target = computeConsciousGateBounds(if (isConsciousGateCompact) 1f else 0f)
                    if (!consciousGatePillRect.isEmpty) {
                        RectF(
                            minOf(target.left, consciousGatePillRect.left),
                            minOf(target.top, consciousGatePillRect.top),
                            maxOf(target.right, consciousGatePillRect.right),
                            maxOf(target.bottom, consciousGatePillRect.bottom),
                        )
                    } else {
                        target
                    }
                }
                isCalendarActive -> {
                    val target = computeCalendarBounds(if (isCalendarCompact) 1f else 0f)
                    if (!calendarPillRect.isEmpty) {
                        RectF(
                            minOf(target.left, calendarPillRect.left),
                            minOf(target.top, calendarPillRect.top),
                            maxOf(target.right, calendarPillRect.right),
                            maxOf(target.bottom, calendarPillRect.bottom),
                        )
                    } else {
                        target
                    }
                }
                isMediaPlaybackActive -> {
                    val target = if (isMediaFullPlayerActive) computeMediaFullPlayerTargetBounds() else computeMediaTargetBounds()
                    if (!mediaPillRect.isEmpty) {
                        RectF(
                            minOf(target.left, mediaPillRect.left),
                            minOf(target.top, mediaPillRect.top),
                            maxOf(target.right, mediaPillRect.right),
                            maxOf(target.bottom, mediaPillRect.bottom),
                        )
                    } else {
                        target
                    }
                }
                else -> notificationPillRect
            }
        }
        val mainBounds = computeNotificationTargetBounds(alert)
        val numBubbles = queuedNotificationAlerts.size
        if (numBubbles == 0 || expandedFraction >= 0.99f) return unionWithMediaBounds(mainBounds)

        val basePillHeight = cameraRadiusPx * 2f + 14f * density
        val bubbleSize = basePillHeight
        val bubbleGap = 8f * density
        val totalBubblesWidth = numBubbles * (bubbleSize + bubbleGap) * (1f - expandedFraction)

        return unionWithMediaBounds(RectF(
            mainBounds.left - totalBubblesWidth,
            mainBounds.top,
            mainBounds.right,
            mainBounds.bottom,
        ))
    }

    fun getAlertAt(x: Float, y: Float): ActiveNotificationAlert? {
        if (expandedFraction < 0.5f) {
            for (i in queuedNotificationAlerts.indices) {
                if (i in 0..1 && bubbleFractions[i] > 0.5f) {
                    if (leadingBubbleRects[i]?.contains(x, y) == true) {
                        return queuedNotificationAlerts[i]
                    }
                }
            }
        }
        if (notificationPillRect.contains(x, y)) {
            return activeNotificationAlert
        }
        return null
    }

    fun isPointInsideActiveAlert(x: Float, y: Float): Boolean {
        if (!isNotificationAlertActive) return false
        val pad = 12f * density
        val bounds = getTotalAlertsBounds()
        val touchRect = RectF(bounds.left - pad, bounds.top - pad, bounds.right + pad, bounds.bottom + pad)
        return touchRect.contains(x, y)
    }

    fun getActionAt(x: Float, y: Float): NotificationActionItem? {
        if (expandedFraction < 0.6f) return null
        for ((action, rect) in actionButtonRects) {
            val pad = 6f * density
            val expandedRect = RectF(rect.left - pad, rect.top - pad, rect.right + pad, rect.bottom + pad)
            if (expandedRect.contains(x, y)) {
                return action
            }
        }
        return null
    }

    fun getNotificationTargetBounds(): RectF {
        return getTotalAlertsBounds()
    }

    fun isPointInsideMedia(x: Float, y: Float): Boolean {
        if (!isMediaPlaybackActive) return false
        val pad = 16f * density
        if (isNotificationAlertActive) {
            if (isCatchUpMode) {
                val rect = RectF(notificationPillRect.left - pad, notificationPillRect.top - pad, notificationPillRect.right + pad, notificationPillRect.bottom + pad)
                return rect.contains(x, y) && x >= notificationPillRect.centerX()
            }
            val mediaBubbleRect = trailingBubbleRects[MEDIA_BUBBLE_KEY] ?: return false
            return RectF(mediaBubbleRect.left - pad, mediaBubbleRect.top - pad, mediaBubbleRect.right + pad, mediaBubbleRect.bottom + pad).contains(x, y)
        }
        if (isCalendarActive) {
            if (!isCalendarCompact) return false
            val mediaIconRect = trailingBubbleRects[MEDIA_BUBBLE_KEY] ?: return false
            return RectF(mediaIconRect.left - pad, mediaIconRect.top - pad, mediaIconRect.right + pad, mediaIconRect.bottom + pad).contains(x, y)
        }
        val bounds = currentMediaBounds()
        val touchRect = RectF(
            minOf(bounds.left, mediaPillRect.left.takeIf { it > 0f } ?: bounds.left) - pad,
            minOf(bounds.top, mediaPillRect.top.takeIf { it > 0f } ?: bounds.top) - pad,
            maxOf(bounds.right, mediaPillRect.right.takeIf { it > 0f } ?: bounds.right) + pad,
            maxOf(bounds.bottom, mediaPillRect.bottom.takeIf { it > 0f } ?: bounds.bottom) + pad,
        )
        return touchRect.contains(x, y)
    }

    private fun unionWithMediaBounds(bounds: RectF): RectF {
        var res = bounds
        if (isConsciousGateActive && !isCatchUpMode && consciousGateBubbleFraction > 0.01f) {
            trailingBubbleRects[CONSCIOUS_GATE_BUBBLE_KEY]?.let { bRect ->
                res = RectF(
                    minOf(res.left, bRect.left),
                    minOf(res.top, bRect.top),
                    maxOf(res.right, bRect.right),
                    maxOf(res.bottom, bRect.bottom),
                )
            }
        }
        if (!isMediaPlaybackActive || isCatchUpMode || mediaBubbleFraction <= 0.01f) return res
        val mediaBounds = currentMediaBounds()
        return RectF(
            minOf(res.left, mediaBounds.left),
            minOf(res.top, mediaBounds.top),
            maxOf(res.right, mediaBounds.right),
            maxOf(res.bottom, mediaBounds.bottom),
        )
    }

    private fun computeMediaBubbleBounds(): RectF {
        trailingBubbleRects[MEDIA_BUBBLE_KEY]?.let { return RectF(it) }
        val size = cameraRadiusPx * 2f + 14f * density
        val top = cameraCenterY - size / 2f
        val left = cameraCenterX + cameraRadiusPx + cutoutIconGap
        val screenRight = resources.displayMetrics.widthPixels.toFloat() - 8f * density
        return RectF(left, top, minOf(left + size, screenRight), top + size)
    }

    private fun currentMediaBounds(): RectF {
        val fullBounds = if (isMediaFullPlayerActive) computeMediaFullPlayerTargetBounds() else computeMediaTargetBounds()
        if (!isNotificationAlertActive || mediaBubbleFraction <= 0.001f) return fullBounds
        val bubbleBounds = computeMediaBubbleBounds()
        return RectF(
            fullBounds.left + (bubbleBounds.left - fullBounds.left) * mediaBubbleFraction,
            fullBounds.top + (bubbleBounds.top - fullBounds.top) * mediaBubbleFraction,
            fullBounds.right + (bubbleBounds.right - fullBounds.right) * mediaBubbleFraction,
            fullBounds.bottom + (bubbleBounds.bottom - fullBounds.bottom) * mediaBubbleFraction,
        )
    }

    private fun computeMediaBounds(compactFraction: Float): RectF {
        val height = cameraRadiusPx * 2f + 14f * density
        val top = cameraCenterY - height / 2f
        val bottom = cameraCenterY + height / 2f
        val iconSize = (height - 14f * density).coerceAtLeast(16f * density)
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val isCenterCamera = abs(cameraCenterX - screenWidth / 2f) < 50f * density
        val maxWidth = (maxWidthDp * density).coerceAtMost(screenWidth - 16f * density)
        val normalHalfWidth = (maxWidth / 2f).coerceAtMost(
            minOf(cameraCenterX - 8f * density, screenWidth - cameraCenterX - 8f * density),
        )
        val normalBounds = if (isCenterCamera) {
            RectF(cameraCenterX - normalHalfWidth, top, cameraCenterX + normalHalfWidth, bottom)
        } else {
            val left = (cameraCenterX - cameraRadiusPx - cutoutGap).coerceAtLeast(8f * density)
            val right = (left + maxWidth).coerceAtMost(screenWidth - 8f * density)
            RectF(left, top, right, bottom)
        }

        val compactBounds = if (isCenterCamera) {
            val sideWidth = cameraRadiusPx + cutoutGap + iconSize + cutoutGap * 2f
            RectF(
                (cameraCenterX - sideWidth - idleIndicator.leftInset()).coerceAtLeast(8f * density),
                top,
                (cameraCenterX + sideWidth + idleIndicator.rightInset()).coerceAtMost(screenWidth - 8f * density),
                bottom,
            )
        } else {
            val left = (cameraCenterX - cameraRadiusPx - cutoutGap).coerceAtLeast(8f * density)
            val right = (cameraCenterX + cameraRadiusPx + cutoutIconGap + iconSize + cutoutGap * 2f)
                .coerceAtMost(screenWidth - 8f * density)
            RectF(left, top, right, bottom)
        }

        return RectF(
            normalBounds.left + (compactBounds.left - normalBounds.left) * compactFraction,
            top,
            normalBounds.right + (compactBounds.right - normalBounds.right) * compactFraction,
            bottom,
        )
    }

    private fun computeMediaTargetBounds(): RectF =
        computeMediaBounds(if (isMediaCompact) 1f else 0f)

    private fun computeMediaFullPlayerWidthBounds(): RectF {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val isCenterCamera = abs(cameraCenterX - screenWidth / 2f) < 50f * density
        val maxAllowedWidthPx = (expandedWidthDp * density).coerceAtMost(screenWidth - 16f * density)
        return if (isCenterCamera) {
            val halfWidth = (maxAllowedWidthPx / 2f).coerceAtMost(minOf(cameraCenterX - 8f * density, screenWidth - cameraCenterX - 8f * density))
            RectF(cameraCenterX - halfWidth, 0f, cameraCenterX + halfWidth, 0f)
        } else {
            val left = (cameraCenterX - cameraRadiusPx - cutoutGap).coerceAtLeast(8f * density)
            val right = (left + maxAllowedWidthPx).coerceAtMost(screenWidth - 8f * density)
            RectF(left, 0f, right, 0f)
        }
    }

    private fun computeMediaFullPlayerTargetBounds(): RectF {
        val width = computeMediaFullPlayerWidthBounds()
        val collapsed = computeMediaTargetBounds()
        return RectF(width.left, collapsed.top, width.right, collapsed.top + computeMediaFullPlayerHeight())
    }

    private class MediaFullPlayerMetrics(
        val topPad: Float,
        val artSize: Float,
        val titleGap: Float,
        val titleTextSize: Float,
        val artistGap: Float,
        val artistTextSize: Float,
        val progressGap: Float,
        val barGap: Float,
        val barHeight: Float,
        val bottomPad: Float,
    ) {
        val totalHeight: Float
            get() = topPad + artSize + titleGap + titleTextSize + artistGap + artistTextSize + progressGap + barGap + barHeight + bottomPad
    }

    private fun mediaFullPlayerMetrics(): MediaFullPlayerMetrics = MediaFullPlayerMetrics(
        topPad = cameraRadiusPx * 2f + 28f * density + expandedTopPaddingDp * density,
        artSize = 88f * density,
        titleGap = 12f * density,
        titleTextSize = 16f * density,
        artistGap = 6f * density,
        artistTextSize = 13f * density,
        progressGap = 20f * density,
        barGap = 20f * density,
        barHeight = 52f * density,
        bottomPad = (expandedPaddingDp * density).coerceAtLeast(16f * density),
    )

    private fun computeMediaFullPlayerHeight(): Float = mediaFullPlayerMetrics().totalHeight

    private fun computeSenderAndMessage(alert: ActiveNotificationAlert): Pair<String, String> {
        val appName = alert.appName?.trim() ?: try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(alert.packageName, 0)
            pm.getApplicationLabel(appInfo).toString().trim()
        } catch (_: Exception) {
            ""
        }

        var sender = alert.senderName?.trim() ?: ""
        if (sender.isBlank() || sender.equals("You", ignoreCase = true)) {
            val cleanTitle = when {
                appName.isNotBlank() && alert.title.startsWith("$appName: ", ignoreCase = true) ->
                    alert.title.substring(appName.length + 2).trim()
                appName.isNotBlank() && alert.title.startsWith("$appName - ", ignoreCase = true) ->
                    alert.title.substring(appName.length + 3).trim()
                appName.isNotBlank() && alert.title.startsWith("$appName • ", ignoreCase = true) ->
                    alert.title.substring(appName.length + 3).trim()
                else -> alert.title.trim()
            }

            val isTitleAppName = cleanTitle.isBlank() ||
                cleanTitle.equals("You", ignoreCase = true) ||
                (appName.isNotBlank() && cleanTitle.equals(appName, ignoreCase = true)) ||
                cleanTitle.equals(alert.packageName, ignoreCase = true) ||
                cleanTitle.equals("WhatsApp", ignoreCase = true) ||
                cleanTitle.equals("Messages", ignoreCase = true) ||
                cleanTitle.equals("Telegram", ignoreCase = true) ||
                cleanTitle.equals("Gmail", ignoreCase = true) ||
                cleanTitle.equals("Instagram", ignoreCase = true) ||
                cleanTitle.equals("Slack", ignoreCase = true) ||
                cleanTitle.equals("Discord", ignoreCase = true) ||
                cleanTitle.equals("Essentials", ignoreCase = true)

            sender = if (isTitleAppName) {
                if (appName.isNotBlank()) appName else cleanTitle
            } else {
                cleanTitle
            }
        }

        if (sender.isBlank() || sender.equals("You", ignoreCase = true)) {
            sender = if (appName.isNotBlank()) appName else "Notification"
        }

        var message = alert.text.trim()
        if (message.isBlank()) {
            val cleanTitle = alert.title.trim()
            if (!cleanTitle.equals(sender, ignoreCase = true) && !cleanTitle.equals(appName, ignoreCase = true) && !cleanTitle.equals("You", ignoreCase = true)) {
                message = cleanTitle
            }
        } else {
            if (sender.isNotBlank() && message.startsWith("$sender: ", ignoreCase = true)) {
                message = message.substring(sender.length + 2).trim()
            }
        }

        return Pair(sender, message)
    }

    private fun pillTextRightEdge(pillRight: Float, cornerExtraPad: Float = 0f, reservedRight: Float = 0f): Float =
        pillRight - cornerExtraPad - reservedRight

    private fun drawCatchUpTrailingIndicator(
        canvas: Canvas,
        currentRight: Float,
        currentTop: Float,
        currentBottom: Float,
        verticalPadding: Float,
        cornerExtraPad: Float,
        iconSize: Float,
        catchUpIndicatorAlpha: Int,
        catchUpTintColor: Int,
        unreadBmp: Bitmap?,
    ) {
        if (catchUpIndicatorAlpha <= 0) return

        if (isMediaPlaybackActive) {
            val artRight = currentRight - verticalPadding - cornerExtraPad - 2f * density
            val artLeft = artRight - iconSize
            val artTop = (currentTop + currentBottom) / 2f - iconSize / 2f
            val artRect = RectF(artLeft, artTop, artRight, artTop + iconSize)
            trailingBubbleRects[MEDIA_BUBBLE_KEY] = RectF(artRect)
            val art = mediaArtwork
            if (art != null) {
                catchUpIconClipPath.reset()
                catchUpIconClipPath.addCircle(artRect.centerX(), artRect.centerY(), iconSize / 2f, Path.Direction.CW)
                canvas.save()
                canvas.clipPath(catchUpIconClipPath)
                iconPaint.alpha = catchUpIndicatorAlpha
                canvas.drawBitmap(art, null, artRect, iconPaint)
                canvas.restore()
            } else {
                equalizerPaint.color = materialYouAccentColor()
                equalizerPaint.alpha = catchUpIndicatorAlpha
                drawEqualizerIcon(canvas, artRect, equalizerAnimator.barLevels, equalizerPaint)
            }
            return
        }

        if (unreadBmp == null) return
        val unreadSize = (16f * density)
        val unreadRight = currentRight - verticalPadding - cornerExtraPad - 2f * density
        val unreadLeft = unreadRight - unreadSize
        val unreadTop = (currentTop + currentBottom) / 2f - unreadSize / 2f
        catchUpUnreadIconRect.set(unreadLeft, unreadTop, unreadRight, unreadTop + unreadSize)

        catchUpUnreadPaint.colorFilter = PorterDuffColorFilter(catchUpTintColor, PorterDuff.Mode.SRC_IN)
        catchUpUnreadPaint.alpha = catchUpIndicatorAlpha
        canvas.drawBitmap(unreadBmp, null, catchUpUnreadIconRect, catchUpUnreadPaint)
    }

    private fun drawMediaPlayback(canvas: Canvas) {
        val height = cameraRadiusPx * 2f + 14f * density
        val baseTop = cameraCenterY - height / 2f
        val baseBottom = cameraCenterY + height / 2f
        val initialLeft: Float
        val initialRight: Float
        val boundsProgress: Float
        if (!mediaRevealOriginRect.isEmpty) {
            initialLeft = mediaRevealOriginRect.left
            initialRight = mediaRevealOriginRect.right
            boundsProgress = mediaMorphFraction
        } else {
            initialLeft = cameraCenterX - cameraRadiusPx
            initialRight = cameraCenterX + cameraRadiusPx
            boundsProgress = mediaFraction
        }
        val fullPlayerT = mediaFullPlayerFraction
        val targetCollapsed = computeMediaBounds(mediaCompactFraction.coerceIn(0f, 1f))
        val target = if (fullPlayerT > 0.001f) {
            val fpWidth = computeMediaFullPlayerWidthBounds()
            val fpBottom = baseTop + computeMediaFullPlayerHeight()
            RectF(
                targetCollapsed.left + (fpWidth.left - targetCollapsed.left) * fullPlayerT,
                baseTop,
                targetCollapsed.right + (fpWidth.right - targetCollapsed.right) * fullPlayerT,
                baseBottom + (fpBottom - baseBottom) * fullPlayerT,
            )
        } else {
            targetCollapsed
        }

        val left = initialLeft + (target.left - initialLeft) * boundsProgress
        val right = initialRight + (target.right - initialRight) * boundsProgress
        mediaPillRect.set(left, target.top, right, target.bottom)
        val cornerRadius = height / 2f + ((expandedCornerRadiusDp * density) - height / 2f) * fullPlayerT

        notificationPillPaint.color = Color.BLACK
        notificationPillPaint.alpha = 255
        canvas.drawRoundRect(mediaPillRect, cornerRadius, cornerRadius, notificationPillPaint)

        if (fullPlayerT > 0.001f) {
            drawMediaFullPlayerFill(canvas, cornerRadius, fullPlayerT)
        }

        val alpha = (mediaFraction * 255f * (1f - fullPlayerT)).toInt().coerceIn(0, 255)
        val iconSize = (height - 14f * density).coerceAtLeast(16f * density)
        val pad = (height - iconSize) / 2f
        val artRect = RectF(mediaPillRect.left + pad, mediaPillRect.top + pad, mediaPillRect.left + pad + iconSize, mediaPillRect.top + pad + iconSize)
        if (mediaFraction > 0.5f) trailingBubbleRects[MEDIA_BUBBLE_KEY] = RectF(artRect)
        val artRollFraction = mediaContentRollFraction
        if (artRollFraction < 0.999f) {
            mediaArtworkPrev?.let {
                iconPaint.alpha = (alpha * (1f - artRollFraction)).toInt().coerceIn(0, 255)
                canvas.save()
                notificationIconClipPath.reset()
                notificationIconClipPath.addCircle(artRect.centerX(), artRect.centerY(), iconSize / 2f, Path.Direction.CW)
                canvas.clipPath(notificationIconClipPath)
                canvas.drawBitmap(it, null, artRect, iconPaint)
                canvas.restore()
            }
        }
        mediaArtwork?.let {
            iconPaint.alpha = (alpha * artRollFraction).toInt().coerceIn(0, 255)
            canvas.save()
            notificationIconClipPath.reset()
            notificationIconClipPath.addCircle(artRect.centerX(), artRect.centerY(), iconSize / 2f, Path.Direction.CW)
            canvas.clipPath(notificationIconClipPath)
            canvas.drawBitmap(it, null, artRect, iconPaint)
            canvas.restore()
        }

        val compactIconRect = RectF(mediaPillRect.right - pad - iconSize, mediaPillRect.top + pad, mediaPillRect.right - pad, mediaPillRect.top + pad + iconSize)
        equalizerPaint.color = materialYouAccentColor()
        equalizerPaint.alpha = (alpha * mediaCompactFraction).toInt().coerceIn(0, 255)
        drawEqualizerIcon(canvas, compactIconRect, equalizerAnimator.barLevels, equalizerPaint)

        val textAlpha = (alpha * (1f - mediaCompactFraction)).toInt().coerceIn(0, 255)
        if (textAlpha > 0) {
            notificationSenderPaint.alpha = textAlpha
            notificationBodyPaint.alpha = textAlpha
            notificationSenderPaint.textSize = (height * 0.34f).coerceIn(12f * density, 18f * density)
            notificationBodyPaint.textSize = notificationSenderPaint.textSize
            val artistLeft = artRect.right + 8f * density
            val artistRight = cameraCenterX - cameraRadiusPx - cutoutGap
            val titleLeft = cameraCenterX + cameraRadiusPx + cutoutGap
            val compactIconReserve = mediaPillRect.right - (compactIconRect.left - 8f * density)
            val titleRight = pillTextRightEdge(mediaPillRect.right, reservedRight = compactIconReserve * mediaCompactFraction)
            val mediaRevealFraction = mediaFraction * (1f - mediaCompactFraction)
            if (mediaContentRollFraction < 0.999f) {
                val artistY = (mediaPillRect.top + mediaPillRect.bottom) / 2f + notificationSenderPaint.textSize * 0.35f
                val titleY = (mediaPillRect.top + mediaPillRect.bottom) / 2f + notificationBodyPaint.textSize * 0.35f
                drawRollingText(canvas, notificationSenderPaint, mediaArtistPrev, mediaArtist, mediaContentRollFraction, artistLeft, artistY, artistLeft, mediaPillRect.top, artistRight, mediaPillRect.bottom, textAlpha, alignEnd = false)
                drawRollingText(canvas, notificationBodyPaint, mediaTitlePrev, mediaTitle, mediaContentRollFraction, titleRight, titleY, titleLeft, mediaPillRect.top, titleRight, mediaPillRect.bottom, textAlpha, alignEnd = true)
            } else {
                drawMarqueeText(canvas, mediaArtist, notificationSenderPaint, leftMarquee, artistLeft, artistRight, mediaPillRect.top, mediaPillRect.bottom, false, height / 2f, mediaRevealFraction)
                drawMarqueeText(canvas, mediaTitle, notificationBodyPaint, rightMarquee, titleLeft, titleRight, mediaPillRect.top, mediaPillRect.bottom, true, height / 2f, mediaRevealFraction, alignTextToEnd = true)
            }
        }

        if (fullPlayerT > 0.01f) {
            drawMediaFullPlayerContent(canvas, (mediaFraction * 255f).toInt().coerceIn(0, 255), fullPlayerT)
        } else {
            mediaLikeButtonRect.setEmpty()
            mediaPlayPauseButtonRect.setEmpty()
            mediaNextButtonRect.setEmpty()
        }
    }

    private fun drawMediaFullPlayerFill(canvas: Canvas, cornerRadius: Float, fullPlayerT: Float) {
        val art = artworkStyle.blurredFor(mediaArtwork) ?: return
        val fillAlpha = (170 * fullPlayerT).toInt().coerceIn(0, 255)
        if (fillAlpha <= 0) return
        canvas.save()
        mediaFullPlayerPath.reset()
        mediaFullPlayerPath.addRoundRect(mediaPillRect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(mediaFullPlayerPath)

        val rectW = mediaPillRect.width()
        val rectH = mediaPillRect.height()
        if (rectW > 0f && rectH > 0f && art.width > 0 && art.height > 0) {
            val srcAspect = art.width.toFloat() / art.height.toFloat()
            val dstAspect = rectW / rectH
            val srcRect = if (srcAspect > dstAspect) {
                val srcW = (art.height * dstAspect).toInt().coerceIn(1, art.width)
                val srcX = (art.width - srcW) / 2
                Rect(srcX, 0, srcX + srcW, art.height)
            } else {
                val srcH = (art.width / dstAspect).toInt().coerceIn(1, art.height)
                val srcY = (art.height - srcH) / 2
                Rect(0, srcY, art.width, srcY + srcH)
            }
            iconPaint.alpha = fillAlpha
            iconPaint.colorFilter = null
            canvas.drawBitmap(art, srcRect, mediaPillRect, iconPaint)
        }

        val cameraAreaBottom = cameraCenterY + cameraRadiusPx + 4f * density
        val fadeBottom = mediaPillRect.bottom
        val maxAllowedFadeHeight = (fadeBottom - cameraAreaBottom).coerceAtLeast(40f * density)
        val fadeHeight = (170f * density).coerceAtMost(maxAllowedFadeHeight)
        val fadeTop = fadeBottom - fadeHeight
        val overlayAlpha = (255 * fullPlayerT).toInt().coerceIn(0, 255)

        cutoutFadePaint.shader = null
        cutoutFadePaint.color = Color.BLACK
        cutoutFadePaint.alpha = overlayAlpha
        canvas.drawRect(mediaPillRect.left, mediaPillRect.top, mediaPillRect.right, fadeTop, cutoutFadePaint)

        cutoutFadePaint.shader = LinearGradient(
            0f, fadeBottom, 0f, fadeTop,
            intArrayOf(Color.TRANSPARENT, Color.argb(150, 0, 0, 0), Color.BLACK),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        cutoutFadePaint.alpha = overlayAlpha
        canvas.drawRect(mediaPillRect.left, fadeTop, mediaPillRect.right, fadeBottom, cutoutFadePaint)
        canvas.restore()
    }

    private fun drawMediaControlButtons(canvas: Canvas, barRect: RectF, alpha: Int) {
        val rects = arrayOf(mediaLikeButtonRect, mediaPlayPauseButtonRect, mediaNextButtonRect)
        val icons = arrayOf(
            (if (isMediaLiked) favoriteIconBitmap else favoriteOutlineIconBitmap) to (if (isMediaLiked) playerAccentColor() else Color.WHITE),
            (if (isMediaTransportPlaying) pauseIconBitmap else playIconBitmap) to Color.WHITE,
            skipNextIconBitmap to Color.WHITE,
        )
        val count = rects.size
        val gap = 4f * density
        val outerRadius = barRect.height() / 2f
        val innerRadius = 4f * density
        val segmentWidth = (barRect.width() - gap * (count - 1)) / count

        for (i in 0 until count) {
            val left = barRect.left + i * (segmentWidth + gap)
            rects[i].set(left, barRect.top, left + segmentWidth, barRect.bottom)

            val radii = when (i) {
                0 -> floatArrayOf(outerRadius, outerRadius, innerRadius, innerRadius, innerRadius, innerRadius, outerRadius, outerRadius)
                count - 1 -> floatArrayOf(innerRadius, innerRadius, outerRadius, outerRadius, outerRadius, outerRadius, innerRadius, innerRadius)
                else -> floatArrayOf(innerRadius, innerRadius, innerRadius, innerRadius, innerRadius, innerRadius, innerRadius, innerRadius)
            }
            mediaFullPlayerPath.reset()
            mediaFullPlayerPath.addRoundRect(rects[i], radii, Path.Direction.CW)
            mediaControlBgPaint.color = Color.WHITE
            mediaControlBgPaint.alpha = (alpha * 0.14f).toInt().coerceIn(0, 255)
            canvas.drawPath(mediaFullPlayerPath, mediaControlBgPaint)

            val (icon, tint) = icons[i]
            if (icon != null) {
                val iconSize = rects[i].height() * 0.46f
                val iconRect = RectF(
                    rects[i].centerX() - iconSize / 2f,
                    rects[i].centerY() - iconSize / 2f,
                    rects[i].centerX() + iconSize / 2f,
                    rects[i].centerY() + iconSize / 2f,
                )
                iconPaint.alpha = alpha
                iconPaint.colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(icon, null, iconRect, iconPaint)
                iconPaint.colorFilter = null
            }
        }
    }

    private fun drawMediaFullPlayerContent(canvas: Canvas, baseAlpha: Int, fullPlayerT: Float) {
        val contentAlpha = (baseAlpha * fullPlayerT).toInt().coerceIn(0, 255)
        if (contentAlpha <= 0) {
            mediaLikeButtonRect.setEmpty()
            mediaPlayPauseButtonRect.setEmpty()
            mediaNextButtonRect.setEmpty()
            return
        }
        val m = mediaFullPlayerMetrics()
        val pillLeft = mediaPillRect.left
        val pillRight = mediaPillRect.right
        val pillCenterX = (pillLeft + pillRight) / 2f
        val artRect = RectF(
            pillCenterX - m.artSize / 2f,
            mediaPillRect.top + m.topPad,
            pillCenterX + m.artSize / 2f,
            mediaPillRect.top + m.topPad + m.artSize,
        )

        mediaArtwork?.let {
            iconPaint.alpha = contentAlpha
            iconPaint.colorFilter = null
            canvas.save()
            mediaFullPlayerPath.reset()
            mediaFullPlayerPath.addRoundRect(artRect, 20f * density, 20f * density, Path.Direction.CW)
            canvas.clipPath(mediaFullPlayerPath)
            canvas.drawBitmap(it, null, artRect, iconPaint)
            canvas.restore()
        }

        notificationBodyPaint.alpha = contentAlpha
        notificationBodyPaint.textSize = m.titleTextSize
        notificationBodyPaint.textAlign = Paint.Align.CENTER
        val titleY = artRect.bottom + m.titleGap + m.titleTextSize
        canvas.drawText(mediaTitle, pillCenterX, titleY, notificationBodyPaint)
        notificationBodyPaint.textAlign = Paint.Align.LEFT

        notificationSenderPaint.alpha = contentAlpha
        notificationSenderPaint.textSize = m.artistTextSize
        notificationSenderPaint.textAlign = Paint.Align.CENTER
        val artistY = titleY + m.artistTextSize + m.artistGap
        canvas.drawText(mediaArtist, pillCenterX, artistY, notificationSenderPaint)
        notificationSenderPaint.textAlign = Paint.Align.LEFT

        val progressY = artistY + m.progressGap
        wavyProgress.draw(canvas, pillLeft + 16f * density, progressY, pillRight - 16f * density, mediaProgressFraction, contentAlpha, playerAccentColor())

        val barTop = progressY + m.barGap
        val barRect = RectF(pillLeft + 12f * density, barTop, pillRight - 12f * density, barTop + m.barHeight)

        drawMediaControlButtons(canvas, barRect, contentAlpha)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        idleIndicator.drawBackground(canvas)
        if (flashlightPill.showFraction < 0.5f) drawIslandContent(canvas)
        flashlightPill.draw(canvas)
        idleIndicator.drawForeground(canvas)
    }

    private fun drawIslandContent(canvas: Canvas) {

        val alert: ActiveNotificationAlert = activeNotificationAlert ?: run {
            if (isConsciousGateActive && consciousGateFraction > 0.001f) {
                drawConsciousGate(canvas)
            } else if (isCalendarActive && calendarFraction > 0.001f) {
                drawCalendarEvent(canvas)
            } else if (isMediaPlaybackActive && mediaFraction > 0.001f) {
                drawMediaPlayback(canvas)
            }
            return
        }
        if (animatedNotificationFraction > 0.001f) {
            val fraction = animatedNotificationFraction
            val screenWidth = resources.displayMetrics.widthPixels.toFloat()

            val canvasSaveCount = canvas.save()
            if (dragTranslationX != 0f || dragTranslationY != 0f || dragScale != 1.0f) {
                val pivotX = notificationPillRect.centerX().takeIf { it > 0f } ?: cameraCenterX
                val pivotY = notificationPillRect.centerY().takeIf { it > 0f } ?: cameraCenterY
                canvas.translate(dragTranslationX, dragTranslationY)
                canvas.scale(dragScale, dragScale, pivotX, pivotY)
            }

            val targetBounds = computeNotificationTargetBounds(alert)
            val targetTop = targetBounds.top
            val targetBottom = targetBounds.bottom
            val targetRight = targetBounds.right

            val basePillHeight = cameraRadiusPx * 2f + 14f * density
            val bubbleSize = basePillHeight
            val bubbleGap = 8f * density

            val queueVisibleFraction = (1f - expandedFraction).coerceIn(0f, 1f)
            val leadingBubbleSpecs = queuedNotificationAlerts.indices.filter { it in 0..1 }.map { i ->
                val queuedAlert = queuedNotificationAlerts[i]
                IslandBubbleSpec(
                    key = i,
                    visibleFraction = bubbleFractions[i] * queueVisibleFraction,
                    icon = queuedAlert.icon ?: queuedAlert.appIcon,
                    slot = bubbleSlots[i],
                )
            }
            if (mediaBubbleFraction > 0.98f) mediaDockOriginRect.setEmpty()
            if (consciousGateBubbleFraction > 0.98f) consciousGateDockOriginRect.setEmpty()

            val trailingBubbleSpecs = buildList {
                if (isConsciousGateActive && consciousGateBubbleFraction > 0.01f) {
                    val bubbleFrac = consciousGateBubbleFraction * queueVisibleFraction
                    add(
                        IslandBubbleSpec(
                            key = CONSCIOUS_GATE_BUBBLE_KEY,
                            visibleFraction = bubbleFrac,
                            icon = timerIconBitmap,
                            iconShape = IslandBubbleIconShape.CIRCLE,
                            iconTint = materialYouAccentColor(),
                            enterFrom = if (!consciousGateDockOriginRect.isEmpty) consciousGateDockOriginRect else null,
                        )
                    )
                }
                if (isMediaPlaybackActive && mediaBubbleFraction > 0.01f) {
                    val bubbleFrac = mediaBubbleFraction * queueVisibleFraction
                    add(
                        IslandBubbleSpec(
                            key = MEDIA_BUBBLE_KEY,
                            visibleFraction = bubbleFrac,
                            icon = mediaArtwork,
                            iconShape = IslandBubbleIconShape.CIRCLE,
                            enterFrom = if (!mediaDockOriginRect.isEmpty) mediaDockOriginRect else null,
                            customIconDraw = if (mediaArtwork == null) {
                                { canvas, iconRect ->
                                    equalizerPaint.color = materialYouAccentColor()
                                    equalizerPaint.alpha = (bubbleFrac * 255).toInt().coerceIn(0, 255)
                                    drawEqualizerIcon(canvas, iconRect, equalizerAnimator.barLevels, equalizerPaint)
                                }
                            } else {
                                null
                            },
                        )
                    )
                }
            }
            val leadingBubblesWidth = IslandBubbleRow.reservedWidth(leadingBubbleSpecs, bubbleSize, bubbleGap)
            val trailingBubblesWidth = IslandBubbleRow.reservedWidth(trailingBubbleSpecs, bubbleSize, bubbleGap)

            val fullTargetLeft = targetBounds.left
            val adjustedTargetLeft = (fullTargetLeft + leadingBubblesWidth).coerceAtMost(cameraCenterX - cameraRadiusPx - (cutoutGap * 2f + 2f * density))
            val adjustedTargetRight = (targetRight - trailingBubblesWidth).coerceAtLeast(cameraCenterX + cameraRadiusPx + (cutoutGap * 2f + 2f * density))

            val initialLeft = cameraCenterX - cameraRadiusPx
            val initialRight = cameraCenterX + cameraRadiusPx

            val normalLeft = initialLeft + (adjustedTargetLeft - initialLeft) * fraction
            val baseLeft = if (isMerging && mergeSourcePillLeft > 0f) {
                mergeSourcePillLeft + (adjustedTargetLeft - mergeSourcePillLeft) * mergeFraction
            } else {
                normalLeft
            }
            val currentLeft = baseLeft + (initialLeft - baseLeft) * dragCollapseFraction

            val normalRight = initialRight + (adjustedTargetRight - initialRight) * fraction
            val baseRight = if (isMerging && mergeSourcePillRight > 0f) {
                mergeSourcePillRight + (adjustedTargetRight - mergeSourcePillRight) * mergeFraction
            } else {
                normalRight
            }
            val currentRight = baseRight - (baseRight - initialRight) * dragCollapseFraction

            val currentTop = targetTop
            val currentBottom = if (dragCollapseFraction > 0f && expandedFraction > 0f) {
                val initialBottom = cameraCenterY + cameraRadiusPx
                targetBottom - (targetBottom - initialBottom) * (dragCollapseFraction * expandedFraction)
            } else {
                targetBottom
            }

            notificationPillRect.set(currentLeft, currentTop, currentRight, currentBottom)
            val pillRadius = basePillHeight / 2f
            val targetExpCornerRadius = expandedCornerRadiusDp * density
            val expRadius = pillRadius + (targetExpCornerRadius - pillRadius) * expandedFraction
            val cornerRadius = if (dragCollapseFraction > 0f && expandedFraction > 0f) {
                cameraRadiusPx + (expRadius - cameraRadiusPx) * (1f - dragCollapseFraction * expandedFraction)
            } else {
                expRadius
            }

            notificationPillPaint.color = Color.BLACK
            notificationPillPaint.alpha = 255

            canvas.drawRoundRect(notificationPillRect, cornerRadius, cornerRadius, notificationPillPaint)

            val inAlpha = ((fraction - 0.20f) / 0.80f).coerceIn(0f, 1f)
            val dragAlpha = (1f - dragCollapseFraction * 2.5f).coerceIn(0f, 1f)
            val baseGlowAlpha = (130f * expandedFraction) * inAlpha * dragAlpha
            val glowAlpha = if (isShowGlow) baseGlowAlpha.toInt().coerceIn(0, 255) else 0

            if (glowAlpha > 0) {
                val accentColor = alert.appColor ?: Color.WHITE
                val r = Color.red(accentColor)
                val g = Color.green(accentColor)
                val b = Color.blue(accentColor)

                val glowStartColor = Color.argb(glowAlpha, r, g, b)
                val glowMidColor = Color.argb((glowAlpha * 0.48f).toInt(), r, g, b)
                val glowEndColor = Color.argb(0, r, g, b)

                val cameraAreaBottom = cameraCenterY + cameraRadiusPx + 4f * density
                val maxAllowedGlowHeight = (currentBottom - cameraAreaBottom).coerceAtLeast(basePillHeight * 0.45f)
                val requestedGlowHeight = (basePillHeight * 0.55f) + (65f * expandedFraction * density)
                val glowHeight = requestedGlowHeight.coerceAtMost(maxAllowedGlowHeight)
                val glowTop = currentBottom - glowHeight

                val glowSave = canvas.save()
                notificationContentClipPath.reset()
                notificationContentClipPath.addRoundRect(notificationPillRect, cornerRadius, cornerRadius, Path.Direction.CW)
                canvas.clipPath(notificationContentClipPath)

                glowPaint.shader = LinearGradient(
                    currentLeft, currentBottom,
                    currentLeft, glowTop,
                    intArrayOf(glowStartColor, glowMidColor, glowEndColor),
                    floatArrayOf(0f, 0.42f, 1f),
                    Shader.TileMode.CLAMP,
                )
                canvas.drawRect(currentLeft, currentTop, currentRight, currentBottom, glowPaint)
                canvas.restoreToCount(glowSave)
            }

            val contentAlphaProgress = ((fraction - 0.15f) / 0.65f).coerceIn(0f, 1f)
            val baseContentAlpha = (contentAlphaProgress * 255).toInt()

            if (baseContentAlpha > 0) {
                val contentSaveCount = canvas.save()
                notificationContentClipPath.reset()
                notificationContentClipPath.addRoundRect(notificationPillRect, cornerRadius, cornerRadius, Path.Direction.CW)
                canvas.clipPath(notificationContentClipPath)

                val textSize = (basePillHeight * 0.38f).coerceIn(13f * density, 20f * density)
                notificationSenderPaint.typeface = googleSansFlexTypeface ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
                notificationSenderPaint.textSize = textSize
                notificationBodyPaint.typeface = googleSansFlexTypeface ?: Typeface.create("sans-serif", Typeface.NORMAL)
                notificationBodyPaint.textSize = textSize

                val isCenterCamera = abs(cameraCenterX - screenWidth / 2f) < 50f * density
                val iconSize = (basePillHeight - 14f * density).coerceAtLeast(16f * density)
                val verticalPadding = (basePillHeight - iconSize) / 2f
                val topPad = (expandedTopPaddingDp * density) * expandedFraction
                val topRowExtraPad = (expandedPaddingDp * 0.25f * density) * expandedFraction
                val cornerExtraPad = (expandedCornerRadiusDp * 0.35f * density) * expandedFraction + ((expandedPaddingDp - 16f).coerceAtLeast(0f) * 0.4f * density) * expandedFraction + topRowExtraPad

                val prev: ActiveNotificationAlert? = previousAlert
                if (isMerging && prev != null) {
                    val outAlphaProgress = (1f - mergeFraction * 2.2f).coerceIn(0f, 1f)
                    val outAlpha = (outAlphaProgress * 255).toInt()
                    val slideOutX = mergeFraction * 32f * density

                    if (outAlpha > 0) {
                        val (prevSender, prevMessage) = computeSenderAndMessage(prev)
                        notificationSenderPaint.alpha = outAlpha
                        notificationBodyPaint.alpha = outAlpha

                        if (isCenterCamera) {
                            val prevIconLeft = currentLeft + verticalPadding + slideOutX
                            val prevIconTop = currentTop + verticalPadding + topPad
                            val prevIconRect = RectF(prevIconLeft, prevIconTop, prevIconLeft + iconSize, prevIconTop + iconSize)
                            val prevDisplayIcon = prev.icon ?: prev.appIcon

                            if (prevDisplayIcon != null) {
                                iconPaint.alpha = outAlpha
                                canvas.save()
                                notificationIconClipPath.reset()
                                notificationIconClipPath.addRoundRect(prevIconRect, iconSize * 0.28f, iconSize * 0.28f, Path.Direction.CW)
                                canvas.clipPath(notificationIconClipPath)
                                canvas.drawBitmap(prevDisplayIcon, null, prevIconRect, iconPaint)
                                canvas.restore()
                            }

                            val prevSenderStart = prevIconLeft + iconSize + 8f * density
                            val prevSenderY = cameraCenterY + topPad + notificationSenderPaint.textSize * 0.35f
                            canvas.drawText(prevSender, prevSenderStart, prevSenderY, notificationSenderPaint)

                            val prevMsgStart = cameraCenterX + cameraRadiusPx + cutoutGap + slideOutX
                            val prevMsgY = cameraCenterY + topPad + notificationBodyPaint.textSize * 0.35f
                            if (prevMessage.isNotBlank()) {
                                canvas.drawText(prevMessage, prevMsgStart, prevMsgY, notificationBodyPaint)
                            }
                        }
                    }
                }

                val (sender, message) = computeSenderAndMessage(alert)
                val inAlphaProgress = if (isMerging) {
                    ((mergeFraction - 0.25f) / 0.65f).coerceIn(0f, 1f)
                } else {
                    ((fraction - 0.25f) / 0.60f).coerceIn(0f, 1f)
                }
                val inSlideX = if (isMerging) {
                    (1f - mergeFraction) * -28f * density
                } else {
                    0f
                }

                val textAlpha = (inAlphaProgress * 255 * (1f - catchUpFraction)).toInt().coerceIn(0, 255)
                notificationSenderPaint.alpha = textAlpha
                notificationBodyPaint.alpha = textAlpha

                val catchUpIndicatorAlpha = (catchUpFraction * 255).toInt().coerceIn(0, 255)
                val unreadBmp = unreadIconBitmap
                val catchUpTintColor = materialYouAccentColor()

                if (isCenterCamera) {
                    val iconLeft = currentLeft + verticalPadding + cornerExtraPad + inSlideX
                    val iconTop = currentTop + verticalPadding + topPad
                    val iconRect = RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)

                    val displayIcon = alert.icon ?: alert.appIcon
                    if (displayIcon != null) {
                        iconPaint.alpha = (inAlphaProgress * 255).toInt().coerceIn(0, 255)
                        val iconScale = 0.85f + 0.15f * inAlphaProgress
                        val iconCenterX = iconRect.centerX()
                        val iconCenterY = iconRect.centerY()

                        canvas.save()
                        canvas.scale(iconScale, iconScale, iconCenterX, iconCenterY)
                        notificationIconClipPath.reset()
                        notificationIconClipPath.addRoundRect(iconRect, iconSize * 0.28f, iconSize * 0.28f, Path.Direction.CW)
                        canvas.clipPath(notificationIconClipPath)
                        canvas.drawBitmap(displayIcon, null, iconRect, iconPaint)
                        canvas.restore()
                    }

                    drawCatchUpTrailingIndicator(
                        canvas, currentRight, currentTop, currentBottom,
                        verticalPadding, cornerExtraPad, iconSize,
                        catchUpIndicatorAlpha, catchUpTintColor, unreadBmp,
                    )

                    if (textAlpha > 0) {
                        val senderStart = iconLeft + iconSize + 8f * density
                        val senderEnd = cameraCenterX - cameraRadiusPx - cutoutGap
                        if (senderEnd > senderStart + 10f * density) {
                            drawMarqueeText(
                                canvas = canvas,
                                text = sender,
                                paint = notificationSenderPaint,
                                marqueeController = leftMarquee,
                                clipLeft = senderStart,
                                clipRight = senderEnd,
                                currentTop = currentTop + topPad,
                                currentBottom = currentTop + topPad + basePillHeight,
                                isRightPillEdge = false,
                                cornerRadius = cornerRadius,
                            )
                        }

                        val collapsedRightAlpha = (textAlpha * (1f - expandedFraction * 2.5f).coerceIn(0f, 1f)).toInt()
                        if (collapsedRightAlpha > 0) {
                            notificationBodyPaint.alpha = collapsedRightAlpha
                            val msgStart = cameraCenterX + cameraRadiusPx + cutoutGap + inSlideX
                            val msgEnd = pillTextRightEdge(currentRight, cornerExtraPad)
                            if (msgEnd > msgStart + 10f * density && message.isNotBlank()) {
                                drawMarqueeText(
                                    canvas = canvas,
                                    text = message,
                                    paint = notificationBodyPaint,
                                    marqueeController = rightMarquee,
                                    clipLeft = msgStart,
                                    clipRight = msgEnd,
                                    currentTop = currentTop + topPad,
                                    currentBottom = currentTop + topPad + basePillHeight,
                                    isRightPillEdge = true,
                                    cornerRadius = cornerRadius,
                                    alignTextToEnd = true,
                                    )
                            }
                        }
                    }
                } else {
                    val spaceFromCutout = cutoutGap + 4f * density
                    val iconLeft = cameraCenterX + cameraRadiusPx + spaceFromCutout + cornerExtraPad + inSlideX
                    val iconTop = currentTop + verticalPadding + topPad
                    val iconRect = RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)

                    val displayIcon = alert.icon ?: alert.appIcon
                    if (displayIcon != null) {
                        iconPaint.alpha = (inAlphaProgress * 255).toInt().coerceIn(0, 255)
                        val iconScale = 0.85f + 0.15f * inAlphaProgress
                        val iconCenterX = iconRect.centerX()
                        val iconCenterY = iconRect.centerY()

                        canvas.save()
                        canvas.scale(iconScale, iconScale, iconCenterX, iconCenterY)
                        notificationIconClipPath.reset()
                        notificationIconClipPath.addRoundRect(iconRect, iconSize * 0.28f, iconSize * 0.28f, Path.Direction.CW)
                        canvas.clipPath(notificationIconClipPath)
                        canvas.drawBitmap(displayIcon, null, iconRect, iconPaint)
                        canvas.restore()
                    }

                    drawCatchUpTrailingIndicator(
                        canvas, currentRight, currentTop, currentBottom,
                        verticalPadding, cornerExtraPad, iconSize,
                        catchUpIndicatorAlpha, catchUpTintColor, unreadBmp,
                    )

                    if (textAlpha > 0) {
                        val textStart = iconLeft + iconSize + 8f * density
                        val textEnd = pillTextRightEdge(currentRight, cornerExtraPad)
                        val collapsedRightAlpha = (textAlpha * (1f - expandedFraction * 2.5f).coerceIn(0f, 1f)).toInt()

                        if (collapsedRightAlpha > 0) {
                            notificationSenderPaint.alpha = collapsedRightAlpha
                            val combinedText = if (message.isNotBlank()) "$sender • $message" else sender
                            if (textEnd > textStart + 10f * density) {
                                drawMarqueeText(
                                    canvas = canvas,
                                    text = combinedText,
                                    paint = notificationSenderPaint,
                                    marqueeController = rightMarquee,
                                    clipLeft = textStart,
                                    clipRight = textEnd,
                                    currentTop = currentTop,
                                    currentBottom = currentTop + basePillHeight,
                                    isRightPillEdge = true,
                                    cornerRadius = cornerRadius,
                                )
                            }
                        }
                    }
                }

                if (expandedFraction > 0.01f && message.isNotBlank()) {
                    val expandedContentAlpha = ((expandedFraction - 0.20f) / 0.80f).coerceIn(0f, 1f)
                    val expAlpha = (expandedContentAlpha * 255).toInt()
                    if (expAlpha > 0) {
                        val textSize = (basePillHeight * 0.38f).coerceIn(14f * density, 18f * density)
                        val bodyTextPaint = TextPaint().apply {
                            set(notificationBodyPaint)
                            typeface = googleSansFlexTypeface ?: Typeface.create("sans-serif", Typeface.NORMAL)
                            setTextSize(textSize)
                            setAlpha(expAlpha)
                        }
                        val middlePadding = (expandedPaddingDp * density) + (expandedCornerRadiusDp * 0.35f * density) * expandedFraction
                        val bodyLeft = currentLeft + middlePadding
                        val hasActions = alert.actions.isNotEmpty()
                        val headerSpacing = if (hasActions) 0f else (4f * density)
                        val bodyTop = currentTop + basePillHeight + topPad + headerSpacing + (1f - expandedFraction) * -8f * density
                        val bodyWidth = (currentRight - currentLeft - middlePadding * 2).toInt().coerceAtLeast(50)

                        val bodyLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            StaticLayout.Builder.obtain(message, 0, message.length, bodyTextPaint, bodyWidth)
                                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                .setLineSpacing(2f * density, 1.0f)
                                .setMaxLines(7)
                                .setEllipsize(TextUtils.TruncateAt.END)
                                .build()
                        } else {
                            @Suppress("DEPRECATION")
                            StaticLayout(
                                message,
                                bodyTextPaint,
                                bodyWidth,
                                Layout.Alignment.ALIGN_NORMAL,
                                1.0f,
                                2f * density,
                                true,
                            )
                        }

                        canvas.save()
                        canvas.translate(bodyLeft, bodyTop)
                        bodyLayout.draw(canvas)
                        canvas.restore()

                        if (alert.actions.isNotEmpty()) {
                            actionButtonRects.clear()

                            val buttonRowTop = bodyTop + bodyLayout.height + 8f * density
                            val buttonHeight = 36f * density
                            val actions = alert.actions
                            val count = actions.size
                            val spaceBetween = 4f * density
                            val totalSpace = spaceBetween * (count - 1)
                            val buttonWidth = ((bodyWidth - totalSpace) / count).coerceAtLeast(40f * density)

                            actionButtonTextPaint.textSize = (buttonHeight * 0.38f).coerceIn(12f * density, 15f * density)
                            val fontMetrics = actionButtonTextPaint.fontMetrics
                            val textBaselineOffset = (buttonHeight - (fontMetrics.descent + fontMetrics.ascent)) / 2f

                            val appColor = alert.appColor
                            val (r, g, b) = if (appColor != null && appColor != 0 && appColor != Color.TRANSPARENT) {
                                Triple(Color.red(appColor), Color.green(appColor), Color.blue(appColor))
                            } else {
                                Triple(255, 255, 255)
                            }
                            val isBgLight = (0.299 * r + 0.587 * g + 0.114 * b) > 180

                            val outerRadius = 18f * density
                            val innerRadius = 4f * density

                            for (i in 0 until count) {
                                val action = actions[i]
                                val bLeft = bodyLeft + i * (buttonWidth + spaceBetween)
                                val bRight = bLeft + buttonWidth
                                val bBottom = buttonRowTop + buttonHeight
                                val bRect = RectF(bLeft, buttonRowTop, bRight, bBottom)
                                actionButtonRects[action] = bRect

                                val isActionHighlighted = (highlightedAction != null && (action === highlightedAction || (action.actionKey.isNotEmpty() && action.actionKey == highlightedAction?.actionKey && action.title == highlightedAction?.title)))

                                val (btnBgColor, btnTextColor, btnTextAlpha) = if (isActionHighlighted) {
                                    val pulseAlpha = (220 - (30 * actionExecutionFraction)).toInt().coerceIn(160, 240)
                                    val textColor = if (isBgLight) Color.BLACK else Color.WHITE
                                    Triple(Color.argb(pulseAlpha, r, g, b), textColor, 255)
                                } else if (highlightedAction != null) {
                                    val fadeOutFraction = (1f - actionExecutionFraction * 1.5f).coerceIn(0f, 1f)
                                    val currentExpAlpha = (fadeOutFraction * expAlpha).toInt()
                                    val bgA = (50 * fadeOutFraction * (expAlpha / 255f)).toInt()
                                    Triple(Color.argb(bgA, 255, 255, 255), Color.WHITE, currentExpAlpha)
                                } else {
                                    val bgA = (50 * (expAlpha / 255f)).toInt()
                                    Triple(Color.argb(bgA, 255, 255, 255), Color.WHITE, expAlpha)
                                }

                                actionButtonBgPaint.color = btnBgColor
                                actionButtonTextPaint.color = btnTextColor
                                actionButtonTextPaint.alpha = btnTextAlpha

                                actionButtonPath.reset()
                                val radii = when {
                                    count == 1 -> floatArrayOf(
                                        outerRadius, outerRadius,
                                        outerRadius, outerRadius,
                                        outerRadius, outerRadius,
                                        outerRadius, outerRadius
                                    )
                                    i == 0 -> floatArrayOf(
                                        outerRadius, outerRadius,
                                        innerRadius, innerRadius,
                                        innerRadius, innerRadius,
                                        outerRadius, outerRadius
                                    )
                                    i == count - 1 -> floatArrayOf(
                                        innerRadius, innerRadius,
                                        outerRadius, outerRadius,
                                        outerRadius, outerRadius,
                                        innerRadius, innerRadius
                                    )
                                    else -> floatArrayOf(
                                        innerRadius, innerRadius,
                                        innerRadius, innerRadius,
                                        innerRadius, innerRadius,
                                        innerRadius, innerRadius
                                    )
                                }
                                actionButtonPath.addRoundRect(bRect, radii, Path.Direction.CW)
                                canvas.drawPath(actionButtonPath, actionButtonBgPaint)

                                val title = if (action.isQuickReply) "${action.title} ↩" else action.title
                                val truncatedTitle = TextUtils.ellipsize(
                                    title,
                                    actionButtonTextPaint,
                                    buttonWidth - 16f * density,
                                    TextUtils.TruncateAt.END
                                ).toString()

                                val titleWidth = actionButtonTextPaint.measureText(truncatedTitle)
                                val textX = bLeft + (buttonWidth - titleWidth) / 2f
                                val textY = buttonRowTop + textBaselineOffset
                                canvas.drawText(truncatedTitle, textX, textY, actionButtonTextPaint)
                            }
                        }
                    }
                } else {
                    actionButtonRects.clear()
                }

                canvas.restoreToCount(contentSaveCount)
            }

            if (isMerging) {
                val mergeBubbleAlphaProgress = (1f - mergeFraction * 1.8f).coerceIn(0f, 1f)
                val mergeBubbleAlpha = (mergeBubbleAlphaProgress * 255).toInt()

                if (mergeBubbleAlpha > 0) {
                    val startBubbleLeft = mergeSourceBubbleLeft
                    val targetMergeLeft = currentLeft - bubbleSize / 2f
                    val bLeft = startBubbleLeft + (targetMergeLeft - startBubbleLeft) * mergeFraction
                    val bRight = bLeft + bubbleSize
                    val bRadius = bubbleSize / 2f
                    val bRect = RectF(bLeft, currentTop, bRight, currentBottom)

                    val bSaveCount = canvas.save()
                    val bScale = 1f - 0.2f * mergeFraction
                    canvas.scale(bScale, bScale, bRect.centerX(), bRect.centerY())

                    notificationPillPaint.color = Color.BLACK
                    notificationPillPaint.alpha = mergeBubbleAlpha
                    canvas.drawRoundRect(bRect, bRadius, bRadius, notificationPillPaint)

                    val mIcon = alert.icon ?: alert.appIcon
                    if (mIcon != null) {
                        val bIconSize = (bubbleSize - 12f * density).coerceAtLeast(14f * density)
                        val bIconPad = (bubbleSize - bIconSize) / 2f
                        val bIconRect = RectF(
                            bLeft + bIconPad,
                            currentTop + bIconPad,
                            bLeft + bIconPad + bIconSize,
                            currentTop + bIconPad + bIconSize,
                        )

                        val bClipPath = Path().apply {
                            addRoundRect(bIconRect, bIconSize * 0.28f, bIconSize * 0.28f, Path.Direction.CW)
                        }
                        canvas.save()
                        canvas.clipPath(bClipPath)
                        iconPaint.alpha = mergeBubbleAlpha
                        canvas.drawBitmap(mIcon, null, bIconRect, iconPaint)
                        canvas.restore()
                    }

                    canvas.restoreToCount(bSaveCount)
                }
            }

            val mergeSlideGap = if (isMerging) (bubbleSize + bubbleGap) * (1f - mergeFraction) else 0f
            notificationPillPaint.color = Color.BLACK
            IslandBubbleRow.draw(
                canvas = canvas,
                side = IslandBubbleSide.LEADING,
                bubbles = leadingBubbleSpecs,
                anchorEdge = currentLeft,
                top = currentTop,
                bottom = currentBottom,
                bubbleSize = bubbleSize,
                bubbleGap = bubbleGap,
                density = density,
                pillPaint = notificationPillPaint,
                iconPaint = iconPaint,
                tintPaint = catchUpUnreadPaint,
                outRects = leadingBubbleRects,
                extraGap = mergeSlideGap,
            )

            notificationPillPaint.color = Color.BLACK
            IslandBubbleRow.draw(
                canvas = canvas,
                side = IslandBubbleSide.TRAILING,
                bubbles = trailingBubbleSpecs,
                anchorEdge = currentRight,
                top = currentTop,
                bottom = currentBottom,
                bubbleSize = bubbleSize,
                bubbleGap = bubbleGap,
                density = density,
                pillPaint = notificationPillPaint,
                iconPaint = iconPaint,
                tintPaint = catchUpUnreadPaint,
                outRects = trailingBubbleRects,
            )

            canvas.restoreToCount(canvasSaveCount)
        }
    }

    private fun drawMarqueeText(
        canvas: Canvas,
        text: String,
        paint: Paint,
        marqueeController: MarqueeController,
        clipLeft: Float,
        clipRight: Float,
        currentTop: Float,
        currentBottom: Float,
        isRightPillEdge: Boolean,
        cornerRadius: Float,
        revealFraction: Float = animatedNotificationFraction,
        alignTextToEnd: Boolean = false,
    ) {
        val availableWidth = (clipRight - clipLeft).coerceAtLeast(10f * density)
        if (revealFraction >= 0.98f) {
            marqueeController.update(text, availableWidth, paint, density) {
                invalidate()
            }
        }

        val textY = (currentTop + currentBottom) / 2f + paint.textSize * 0.35f

        if (marqueeController.isNeeded && revealFraction >= 0.98f) {
            val marqueeBounds = RectF(clipLeft, currentTop, clipRight, currentBottom)
            val saveLayerCount = canvas.saveLayer(marqueeBounds, null)

            val marqueeClipPath = Path()
            if (isRightPillEdge) {
                val radii = floatArrayOf(
                    0f, 0f,
                    cornerRadius, cornerRadius,
                    cornerRadius, cornerRadius,
                    0f, 0f,
                )
                marqueeClipPath.addRoundRect(marqueeBounds, radii, Path.Direction.CW)
            } else {
                marqueeClipPath.addRect(marqueeBounds, Path.Direction.CW)
            }
            canvas.clipPath(marqueeClipPath)

            val marqueeGap = 28f * density
            val textWidthMeasure = paint.measureText(text)
            val x1 = clipLeft - marqueeController.offset
            val x2 = x1 + textWidthMeasure + marqueeGap
            canvas.drawText(text, x1, textY, paint)
            canvas.drawText(text, x2, textY, paint)

            val totalWidth = clipRight - clipLeft
            val fadeWidth = 14f * density
            if (totalWidth > fadeWidth * 2f) {
                val fLeft = (fadeWidth / totalWidth).coerceIn(0f, 0.4f)
                val fRight = 1f - fLeft
                if (!isRightPillEdge) {
                    marqueeFadePaint.shader = LinearGradient(
                        clipLeft, 0f, clipRight, 0f,
                        intArrayOf(Color.TRANSPARENT, Color.BLACK, Color.BLACK, Color.TRANSPARENT),
                        floatArrayOf(0f, fLeft, fRight, 1f),
                        Shader.TileMode.CLAMP,
                    )
                } else {
                    marqueeFadePaint.shader = LinearGradient(
                        clipLeft, 0f, clipRight, 0f,
                        intArrayOf(Color.TRANSPARENT, Color.BLACK, Color.BLACK),
                        floatArrayOf(0f, fLeft, 1f),
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(marqueeBounds, marqueeFadePaint)
            }

            canvas.restoreToCount(saveLayerCount)
        } else {
            canvas.save()
            val textClipRect = RectF(clipLeft, currentTop, clipRight, currentBottom)
            canvas.clipRect(textClipRect)
            val textX = if (alignTextToEnd) (clipRight - paint.measureText(text) - 8f * density).coerceAtLeast(clipLeft) else clipLeft
            canvas.drawText(text, textX, textY, paint)
            canvas.restore()
        }
    }
}


