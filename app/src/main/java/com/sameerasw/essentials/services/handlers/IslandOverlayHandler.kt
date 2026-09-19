/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Handlers
 * File: IslandOverlayHandler.kt
 * Description: Background handler managing Island dynamic notification overlay window and life-cycle.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.hardware.camera2.CameraManager
import androidx.annotation.RequiresApi
import android.os.BatteryManager
import android.os.PowerManager
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import android.graphics.PixelFormat
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.DisplayCutout
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowMetrics
import android.content.ComponentName
import android.graphics.drawable.Drawable
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.model.ActiveNotificationAlert
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.CalendarEventUtil
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.IslandOverlayView
import com.sameerasw.essentials.utils.OverlayHelper
import com.sameerasw.essentials.utils.FlashlightUtil
import com.sameerasw.essentials.utils.island.IslandBatteryColorConfig
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IslandOverlayHandler(
    private val service: AccessibilityService,
) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val settingsRepository by lazy { SettingsRepository(service) }
    private var windowManager: WindowManager? = null
    private var overlayView: IslandOverlayView? = null
    private var touchAnchorView: View? = null
    private val touchHandler by lazy { IslandTouchHandler(service) }
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isOverlayAdded = false
    private var isTouchAnchorAdded = false
    private var isNotificationsListenerRegistered = false
    private var isMediaSessionRegistered = false
    private val mediaSessionManager by lazy { service.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager }
    private val monitoredControllers = mutableListOf<MediaController>()
    private val controllerCallbacks = mutableMapOf<MediaSession.Token, MediaController.Callback>()
    private var activeMediaController: MediaController? = null
    private var currentMediaKey: String? = null
    private var activeMediaSessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private val handlerScope = CoroutineScope(Dispatchers.Main + Job())

    private var lastMediaChangeTimestamp = 0L
    private val mediaUpdateRunnable = Runnable { applyCurrentMediaState() }

    private fun scheduleMediaUpdate() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastMediaChangeTimestamp < MEDIA_UPDATE_DEBOUNCE_MS) {
            return
        }
        lastMediaChangeTimestamp = now
        mainHandler.removeCallbacks(mediaUpdateRunnable)
        mainHandler.postDelayed(mediaUpdateRunnable, MEDIA_UPDATE_DEBOUNCE_MS)
    }

    private val keyguardManager by lazy { service.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }

    private var cameraCenterX = 0f
    private var cameraCenterY = 0f
    private var cameraRadiusPx = 36f

    private var isScreenOff = false
    private var isLocked = false
    private var isLandscape = false
    private var isFullscreenApp = false

    private val isIslandContentSuppressed: Boolean
        get() = isSuppressedByOrientationOrFullscreen ||
            (settingsRepository.isIslandHideWhenScreenOffEnabled() && (isScreenOff || isLocked || keyguardManager?.isKeyguardLocked == true))

    private val isSuppressedByOrientationOrFullscreen: Boolean
        get() = isLandscape || isFullscreenApp

    fun setFullscreen(fullscreen: Boolean) {
        if (isFullscreenApp == fullscreen) return
        isFullscreenApp = fullscreen
        applyOrientationSuppression()
    }

    private fun applyOrientationSuppression() {
        if (!settingsRepository.isIslandEnabled()) return
        if (isSuppressedByOrientationOrFullscreen) {
            val preservedMediaKey = currentMediaKey
            mainHandler.removeCallbacks(dismissNotificationRunnable)
            mainHandler.removeCallbacks(revertCalendarExpansionRunnable)
            mainHandler.removeCallbacks(revertConsciousGateExpansionRunnable)
            mainHandler.removeCallbacks(consciousGateUpdateRunnable)
            overlayView?.dismissNotificationAlert()
            overlayView?.dismissMediaPlayback()
            overlayView?.dismissCalendarEvent()
            overlayView?.dismissConsciousGateSession()
            unregisterNotificationsListener()
            unregisterMediaListener()
            currentMediaKey = preservedMediaKey
            removeOverlay()
            if (settingsRepository.isIslandSuppressSystemHeadsUpEnabled()) {
                settingsRepository.applyHeadsUpSuppression(false)
            }
        } else {
            ensureOverlayAttached()
            if (settingsRepository.isIslandSuppressSystemHeadsUpEnabled()) {
                settingsRepository.applyHeadsUpSuppression(true)
            }
            applyCurrentMediaState()
            pollCalendarEvent()
            updateConsciousGateState()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOff = true
                    isLocked = true
                    if (settingsRepository.isIslandHideWhenScreenOffEnabled()) {
                        mainHandler.removeCallbacks(dismissNotificationRunnable)
                        mainHandler.removeCallbacks(consciousGateUpdateRunnable)
                        overlayView?.dismissNotificationAlert()
                        overlayView?.dismissMediaPlayback()
                        overlayView?.dismissCalendarEvent()
                        overlayView?.dismissConsciousGateSession()
                        restoreTouchAnchor()
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOff = false
                    isLocked = keyguardManager?.isKeyguardLocked ?: false
                    if (isIslandContentSuppressed) {
                        mainHandler.removeCallbacks(dismissNotificationRunnable)
                        mainHandler.removeCallbacks(consciousGateUpdateRunnable)
                        overlayView?.dismissNotificationAlert()
                        overlayView?.dismissMediaPlayback()
                        overlayView?.dismissCalendarEvent()
                        overlayView?.dismissConsciousGateSession()
                        restoreTouchAnchor()
                    } else {
                        applyCurrentMediaState()
                        pollCalendarEvent()
                        updateConsciousGateState()
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    isScreenOff = false
                    isLocked = false
                    applyCurrentMediaState()
                    pollCalendarEvent()
                    updateConsciousGateState()
                }
            }
            updateIdlePill()
            updateFlashlightState()
        }
    }

    private val idleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> updateIdleBattery(intent)
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> updateIdleBattery()
                Intent.ACTION_TIME_TICK,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> updateIdleTime()
            }
        }
    }

    private fun updateIdleTime() {
        val pattern = if (DateFormat.is24HourFormat(service)) "HH:mm" else "h:mm"
        overlayView?.setIdleTime(SimpleDateFormat(pattern, Locale.getDefault()).format(Date()))
    }

    private fun updateIdleBattery(intent: Intent? = null) {
        val batteryIntent = intent ?: service.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0 || scale <= 0) return
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val powerSave = (service.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode ?: false
        overlayView?.setIdleBattery((level * 100f / scale).toInt(), charging, powerSave)
    }

    private fun buildBatteryColorConfig(): IslandBatteryColorConfig {
        val defaults = IslandBatteryColorConfig()
        fun parse(hex: String, fallback: Int) = try {
            Color.parseColor(hex)
        } catch (_: Exception) {
            fallback
        }
        val charging = settingsRepository.getDuoBatteryChargingColor()
        return IslandBatteryColorConfig(
            chargingEnabled = settingsRepository.isDuoBatteryChargingColorEnabled(),
            chargingColor = if (charging.equals("auto", ignoreCase = true)) null else parse(charging, Color.rgb(0, 230, 118)),
            powerSaveEnabled = settingsRepository.isDuoBatteryPowerSaveColorEnabled(),
            powerSaveColor = parse(settingsRepository.getDuoBatteryPowerSaveColor(), defaults.powerSaveColor),
            lowEnabled = settingsRepository.isDuoBatteryLowColorEnabled(),
            lowColor = parse(settingsRepository.getDuoBatteryLowColor(), defaults.lowColor),
            criticalEnabled = settingsRepository.isDuoBatteryCriticalColorEnabled(),
            criticalColor = parse(settingsRepository.getDuoBatteryCriticalColor(), defaults.criticalColor),
        )
    }

    private fun updateIdlePill() {
        val ov = overlayView ?: return
        val enabled = settingsRepository.isIslandEnabled() &&
            settingsRepository.isIslandShowTimeBatteryEnabled() &&
            !isIslandContentSuppressed
        ov.isIdleBatteryIcon = settingsRepository.getIslandBatteryStyle() == SettingsRepository.ISLAND_BATTERY_STYLE_ICON
        ov.setIdleBatteryColors(buildBatteryColorConfig())
        if (enabled) {
            updateIdleTime()
            updateIdleBattery()
        }
        ov.isIdlePillEnabled = enabled
    }

    private val cameraManager by lazy { service.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private var torchOn = false
    private var torchFadeJob: Job? = null
    private val torchCameraId: String? by lazy { FlashlightUtil.getCameraId(service) }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId != torchCameraId) return
            torchOn = enabled
            updateFlashlightState()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
            if (cameraId != torchCameraId) return
            overlayView?.updateFlashlightLevel(torchPercent(newStrengthLevel))
        }
    }

    private fun torchMaxLevel(): Int = torchCameraId?.let { FlashlightUtil.getMaxLevel(service, it) } ?: 1

    private fun torchPercent(level: Int): Int = (level * 100f / torchMaxLevel().coerceAtLeast(1)).toInt().coerceIn(1, 100)

    fun updateFlashlightState() {
        val show = settingsRepository.isIslandEnabled() &&
            settingsRepository.isIslandShowFlashlightEnabled() &&
            torchOn &&
            !isIslandContentSuppressed
        if (!show) {
            overlayView?.dismissFlashlight()
            return
        }
        val id = torchCameraId ?: return
        ensureOverlayAttached()
        val max = torchMaxLevel()
        overlayView?.showFlashlight(torchPercent(FlashlightUtil.getCurrentLevel(service, id)), max > 1)
        expandTouchAnchorForNotification()
    }

    private fun setTorchFraction(fraction: Float) {
        val id = torchCameraId ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        torchFadeJob?.cancel()
        val max = torchMaxLevel()
        try {
            cameraManager.turnOnTorchWithStrengthLevel(id, (fraction * max).toInt().coerceIn(1, max))
        } catch (_: Exception) {}
    }

    private fun turnOffTorch() {
        val id = torchCameraId ?: return
        val fadeEnabled = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            .getBoolean("flashlight_fade_enabled", false)
        if (fadeEnabled && FlashlightUtil.isIntensitySupported(service, id)) {
            torchFadeJob?.cancel()
            torchFadeJob = handlerScope.launch {
                FlashlightUtil.fadeFlashlight(service, id, targetOn = false)
            }
            return
        }
        try {
            cameraManager.setTorchMode(id, false)
        } catch (_: Exception) {}
    }

    private val dismissNotificationRunnable = Runnable {
        handleNotificationTimeout()
    }

    private val revertExpansionRunnable = Runnable {
        overlayView?.setExpandedState(false)
        if (overlayView?.isMediaPlaybackActive == true) {
            overlayView?.setMediaCompact(true)
        }
        scheduleDismissTimer()
    }

    private var currentCalendarEventStartMillis: Long = 0L
    private val revertCalendarExpansionRunnable = Runnable {
        overlayView?.setCalendarCompact(true)
        expandTouchAnchorForNotification()
    }
    private val calendarPollRunnable = Runnable { pollCalendarEvent() }

    private val revertConsciousGateExpansionRunnable = Runnable {
        overlayView?.setConsciousGateCompact(true)
        expandTouchAnchorForNotification()
    }

    private val consciousGateUpdateRunnable = object : Runnable {
        override fun run() {
            updateConsciousGateState()
        }
    }

    private var cachedConsciousGateAppPkg: String? = null
    private var cachedConsciousGateAppIcon: Bitmap? = null
    private var lastConsciousGateHapticSecond: Long = -1L

    fun updateConsciousGateState() {
        if (!settingsRepository.isIslandEnabled() || !settingsRepository.isIslandShowConsciousGateEnabled()) {
            lastConsciousGateHapticSecond = -1L
            mainHandler.removeCallbacks(consciousGateUpdateRunnable)
            overlayView?.dismissConsciousGateSession()
            return
        }

        val session = ScreenOffAccessibilityService.instance?.getActiveConsciousGateSession()
        if (session == null || isIslandContentSuppressed) {
            lastConsciousGateHapticSecond = -1L
            mainHandler.removeCallbacks(revertConsciousGateExpansionRunnable)
            mainHandler.removeCallbacks(consciousGateUpdateRunnable)
            overlayView?.dismissConsciousGateSession()
            return
        }

        val now = System.currentTimeMillis()
        val elapsed = (now - session.startTimeMillis).coerceAtLeast(0L)
        val remainingMillis = (session.durationMillis - elapsed).coerceAtLeast(0L)
        if (remainingMillis <= 0L) {
            lastConsciousGateHapticSecond = -1L
            mainHandler.removeCallbacks(revertConsciousGateExpansionRunnable)
            mainHandler.removeCallbacks(consciousGateUpdateRunnable)
            overlayView?.dismissConsciousGateSession()
            return
        }

        val totalSec = remainingMillis / 1000L
        val mins = totalSec / 60L
        val secs = totalSec % 60L
        val timeText = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

        if (cachedConsciousGateAppPkg != session.packageName || cachedConsciousGateAppIcon == null) {
            cachedConsciousGateAppPkg = session.packageName
            cachedConsciousGateAppIcon = try {
                val iconDrawable = service.packageManager.getApplicationIcon(session.packageName)
                AppUtil.drawableToBitmap(iconDrawable)
            } catch (_: Exception) {
                null
            }
        }

        ensureOverlayAttached()
        overlayView?.showConsciousGateSession(
            appIcon = cachedConsciousGateAppIcon,
            appLabel = service.getString(com.sameerasw.essentials.R.string.conscious_gate_island_wasting_time),
            timeFormatted = timeText,
        )
        expandTouchAnchorForNotification()

        if (lastConsciousGateHapticSecond != totalSec) {
            lastConsciousGateHapticSecond = totalSec
            val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("conscious_gate_feel_every_second", false)) {
                HapticUtil.performHapticForService(service, HapticFeedbackType.SUBTLE)
            }
        }

        mainHandler.removeCallbacks(consciousGateUpdateRunnable)
        val delayToNextSecond = (remainingMillis % 1000L).let { if (it == 0L) 1000L else it }
        mainHandler.postDelayed(consciousGateUpdateRunnable, delayToNextSecond.coerceIn(50L, 1000L))
    }

    private val mediaProgressRunnable = Runnable { tickMediaProgress() }

    private fun tickMediaProgress() {
        val ov = overlayView ?: return
        val controller = activeMediaController
        if (!ov.isMediaFullPlayerActive || controller == null) return
        val state = controller.playbackState
        val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        if (state != null && duration > 0L) {
            val elapsedMs = if (state.state == android.media.session.PlaybackState.STATE_PLAYING) {
                (android.os.SystemClock.elapsedRealtime() - state.lastPositionUpdateTime) * state.playbackSpeed
            } else {
                0f
            }
            val position = (state.position + elapsedMs).coerceIn(0f, duration.toFloat())
            ov.updateMediaProgress(position / duration.toFloat())
        }
        mainHandler.postDelayed(mediaProgressRunnable, 200L)
    }

    private fun isControllerLiked(controller: MediaController): Boolean {
        try {
            val metadata = controller.metadata
            if (metadata != null) {
                val rating = metadata.getRating(MediaMetadata.METADATA_KEY_USER_RATING)
                if (rating != null && rating.isRated) {
                    val liked = rating.hasHeart() ||
                        rating.isThumbUp ||
                        (rating.ratingStyle == android.media.Rating.RATING_PERCENTAGE && rating.percentRating >= 50)
                    if (liked) return true
                }
            }
            val playbackState = controller.playbackState
            if (playbackState != null) {
                for (action in playbackState.customActions) {
                    val name = action.name?.toString().orEmpty()
                    if (name.contains("Unheart", ignoreCase = true) ||
                        name.contains("Unlike", ignoreCase = true) ||
                        name.contains("Remove from", ignoreCase = true)
                    ) {
                        return true
                    }
                }
            }
        } catch (_: Exception) {}
        return false
    }

    private fun handleMediaControlAction(action: Int) {
        val controller = activeMediaController ?: return
        when (action) {
            0 -> {
                service.sendBroadcast(Intent(NotificationListener.ACTION_LIKE_CURRENT_SONG).setPackage(service.packageName))
                mainHandler.postDelayed({
                    activeMediaController?.let { overlayView?.setMediaLiked(isControllerLiked(it)) }
                }, 400L)
            }
            1 -> {
                if (controller.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            }
            2 -> controller.transportControls.skipToNext()
        }
    }

    private fun handleMediaBackgroundTap() {
        val controller = activeMediaController ?: return

        if (overlayView?.isMediaFullPlayerActive == true) {
            overlayView?.toggleMediaFullPlayer()
            touchHandler.onMediaFullPlayerToggled?.invoke(false)
        }
        overlayView?.setMediaCompact(true)

        try {
            val sessionActivity = controller.sessionActivity
            if (sessionActivity != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val options = android.app.ActivityOptions.makeBasic().apply {
                        pendingIntentBackgroundActivityStartMode =
                            android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    }
                    sessionActivity.send(service, 0, null, null, null, null, options.toBundle())
                } else {
                    sessionActivity.send()
                }
                return
            }
        } catch (_: Exception) {}

        try {
            val launchIntent = service.packageManager.getLaunchIntentForPackage(controller.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                service.startActivity(launchIntent)
            }
        } catch (_: Exception) {}
    }

    private fun scheduleCalendarPoll() {
        mainHandler.removeCallbacks(calendarPollRunnable)
        mainHandler.postDelayed(calendarPollRunnable, CALENDAR_POLL_INTERVAL_MS)
    }

    private fun pollCalendarEvent() {
        scheduleCalendarPoll()
        if (!settingsRepository.isIslandEnabled() || !settingsRepository.isIslandShowCalendarEnabled()) {
            overlayView?.dismissCalendarEvent()
            return
        }
        ensureOverlayAttached()
        val ov = overlayView ?: return
        // Media at normal size has focus — don't re-merge calendar in until it goes compact again.
        if (ov.isNotificationAlertActive || isIslandContentSuppressed || (ov.isMediaPlaybackActive && !ov.isMediaCompact)) return

        handlerScope.launch(Dispatchers.IO) {
            val timeframe = settingsRepository.getStatusGlanceCalendarTimeframe()
            val selectedIds = settingsRepository.getStatusGlanceCalendarSelectedCalendars().mapNotNull { it.toLongOrNull() }.toSet()
            val showAllDay = settingsRepository.isStatusGlanceCalendarShowAllDayEnabled()
            val event = CalendarEventUtil.queryNextUpcomingEvent(service, timeframe, selectedIds, showAllDay)
            withContext(Dispatchers.Main) {
                val liveOv = overlayView ?: return@withContext
                if (liveOv.isNotificationAlertActive || isIslandContentSuppressed || (liveOv.isMediaPlaybackActive && !liveOv.isMediaCompact)) return@withContext
                if (event == null) {
                    liveOv.dismissCalendarEvent()
                    currentCalendarEventStartMillis = 0L
                    return@withContext
                }
                currentCalendarEventStartMillis = event.startTimeMillis
                val now = System.currentTimeMillis()
                val fullTime = CalendarEventUtil.formatRelativeTime(service, event.startTimeMillis, now)
                val compactTime = CalendarEventUtil.formatRelativeTimeCompact(event.startTimeMillis, now)
                ensureOverlayAttached()
                liveOv.showCalendarEvent(event.title, fullTime, compactTime, event.location.orEmpty())
                expandTouchAnchorForNotification()
            }
        }
    }

    private fun handleNotificationTimeout() {
        val ov = overlayView ?: return
        if (ov.isNotificationAlertActive) {
            val hasQueuedAlerts = ov.getQueuedAlerts().isNotEmpty()

            if (!hasQueuedAlerts && ov.canEnterCatchUp && settingsRepository.isIslandCatchUpEnabled() && !ov.isCatchUpMode && !ov.isExpanded) {
                ov.enterCatchUpMode()
                expandTouchAnchorForNotification()
                scheduleCatchUpDismissTimer()
                return
            }

            val hasNext = ov.advanceToNextNotification()
            if (hasNext) {
                expandTouchAnchorForNotification()
                scheduleDismissTimer()
            } else {
                mainHandler.removeCallbacks(dismissNotificationRunnable)
                if (ov.isMediaPlaybackActive) {
                    expandTouchAnchorForNotification()
                } else {
                    restoreTouchAnchor()
                }
            }
            return
        }

        if (ov.isMediaPlaybackActive && !ov.isMediaCompact) {
            ov.setMediaCompact(true)
            expandTouchAnchorForNotification()
            pollCalendarEvent()
            return
        }

        if (ov.isConsciousGateActive && !ov.isConsciousGateCompact) {
            ov.setConsciousGateCompact(true)
            expandTouchAnchorForNotification()
            return
        }

        mainHandler.removeCallbacks(dismissNotificationRunnable)
        if (ov.isMediaPlaybackActive || ov.isCalendarActive || ov.isConsciousGateActive || ov.isFlashlightActive) {
            expandTouchAnchorForNotification()
        } else {
            restoreTouchAnchor()
        }
    }

    private fun scheduleDismissTimer() {
        val ov = overlayView
        if (ov?.isCatchUpMode == true) {
            scheduleCatchUpDismissTimer()
            return
        }
        val timeout = settingsRepository.getIslandTimeoutMs()
        mainHandler.removeCallbacks(dismissNotificationRunnable)
        mainHandler.postDelayed(dismissNotificationRunnable, timeout)
    }

    private fun scheduleCatchUpDismissTimer() {
        val timeout = settingsRepository.getIslandCatchUpTimeoutMs()
        mainHandler.removeCallbacks(dismissNotificationRunnable)
        mainHandler.postDelayed(dismissNotificationRunnable, timeout)
    }

    private val notificationAlertListener = object : NotificationListener.NotificationAlertListener {
        override fun onNotificationAlertPosted(alert: ActiveNotificationAlert) {
            if (!settingsRepository.isIslandEnabled()) return
            if (isIslandContentSuppressed) return

            mainHandler.post {
                ensureOverlayAttached()
                overlayView?.dismissCalendarEvent()
                overlayView?.showNotificationAlert(alert)
                expandTouchAnchorForNotification()
                if (overlayView?.isCatchUpMode == true) {
                    scheduleCatchUpDismissTimer()
                } else {
                    scheduleDismissTimer()
                }
            }
        }

        override fun onNotificationAlertRemoved(key: String) {
            mainHandler.post {
                val isStillActive = overlayView?.removeNotificationByKey(key) ?: false
                if (!isStillActive) {
                    applyCurrentMediaState()
                    mainHandler.removeCallbacks(dismissNotificationRunnable)
                    if (overlayView?.isMediaPlaybackActive == true) {
                        expandTouchAnchorForNotification()
                    } else {
                        restoreTouchAnchor()
                    }
                } else {
                    expandTouchAnchorForNotification()
                }
            }
        }
    }

    init {
        settingsRepository.registerOnSharedPreferenceChangeListener(this)
        windowManager = service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        isLandscape = service.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        service.registerReceiver(screenReceiver, filter)
        service.registerReceiver(
            idleReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            },
        )

        touchHandler.onNotificationDismissRequested = {
            val hasNext = overlayView?.advanceToNextNotification() ?: false
            if (hasNext) {
                expandTouchAnchorForNotification()
                scheduleDismissTimer()
            } else {
                mainHandler.removeCallbacks(dismissNotificationRunnable)
                restoreTouchAnchor()
            }
        }

        touchHandler.onNotificationSwitched = {
            expandTouchAnchorForNotification()
            scheduleDismissTimer()
        }

        touchHandler.onCatchUpRestored = {
            expandTouchAnchorForNotification()
            scheduleDismissTimer()
        }

        touchHandler.onMediaDismissRequested = {
            mainHandler.removeCallbacks(dismissNotificationRunnable)
        }

        touchHandler.onMediaTapped = {
            overlayView?.setMediaCompact(false)
            if (overlayView?.isCalendarActive == true) {
                mainHandler.removeCallbacks(revertCalendarExpansionRunnable)
                overlayView?.reclaimMediaFromCalendar()
            } else {
                overlayView?.dismissNotificationAlert()
            }
            scheduleDismissTimer()
        }

        touchHandler.onNotificationExpandToggled = { isExpanded ->
            expandTouchAnchorForNotification()
            mainHandler.removeCallbacks(dismissNotificationRunnable)
            mainHandler.removeCallbacks(revertExpansionRunnable)

            if (isExpanded) {
                val expTimeout = settingsRepository.getIslandExpandedTimeoutMs()
                if (expTimeout > 0L) {
                    mainHandler.postDelayed(revertExpansionRunnable, expTimeout)
                }
            } else {
                scheduleDismissTimer()
            }
        }

        touchHandler.onCalendarToggled = {
            expandTouchAnchorForNotification()
            mainHandler.removeCallbacks(revertCalendarExpansionRunnable)
            if (overlayView?.isCalendarCompact == false) {
                val expTimeout = settingsRepository.getIslandExpandedTimeoutMs().takeIf { it > 0L } ?: CALENDAR_DEFAULT_EXPANDED_MS
                mainHandler.postDelayed(revertCalendarExpansionRunnable, expTimeout)
            }
        }

        touchHandler.onConsciousGateToggled = {
            expandTouchAnchorForNotification()
            mainHandler.removeCallbacks(revertConsciousGateExpansionRunnable)
            if (overlayView?.isConsciousGateCompact == false) {
                val expTimeout = settingsRepository.getIslandExpandedTimeoutMs().takeIf { it > 0L } ?: 8000L
                mainHandler.postDelayed(revertConsciousGateExpansionRunnable, expTimeout)
            }
        }

        touchHandler.onMediaFullPlayerToggled = { isActive ->
            expandTouchAnchorForNotification()
            mainHandler.removeCallbacks(dismissNotificationRunnable)
            mainHandler.removeCallbacks(mediaProgressRunnable)
            if (isActive) {
                tickMediaProgress()
            } else {
                scheduleDismissTimer()
            }
        }

        touchHandler.onFlashlightLevelChanged = { setTorchFraction(it) }
        touchHandler.onFlashlightTurnOff = { turnOffTorch() }
        try {
            cameraManager.registerTorchCallback(torchCallback, mainHandler)
        } catch (_: Exception) {}

        touchHandler.onMediaBackgroundTapped = {
            handleMediaBackgroundTap()
        }

        touchHandler.onMediaControlTapped = { action ->
            handleMediaControlAction(action)
        }

        updateOverlay()
        scheduleCalendarPoll()
    }

    fun onConfigurationChanged() {
        val landscape = service.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape != landscape) {
            isLandscape = landscape
            applyOrientationSuppression()
        }
        if (settingsRepository.isIslandEnabled() && !isSuppressedByOrientationOrFullscreen) {
            updateOverlayPosition()
        }
    }

    private fun registerNotificationsListener() {
        if (!isNotificationsListenerRegistered) {
            NotificationListener.addNotificationAlertListener(notificationAlertListener)
            isNotificationsListenerRegistered = true
        }
    }

    private fun registerMediaListener() {
        if (isMediaSessionRegistered || mediaSessionManager == null) return
        try {
            val componentName = ComponentName(service, NotificationListener::class.java)
            val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                updateActiveMediaSessions(controllers)
            }
            activeMediaSessionsListener = listener
            mediaSessionManager?.addOnActiveSessionsChangedListener(listener, componentName, mainHandler)
            isMediaSessionRegistered = true
            updateActiveMediaSessions(mediaSessionManager?.getActiveSessions(componentName), isInitial = true)
        } catch (_: Exception) {}
    }

    private fun unregisterMediaListener() {
        if (!isMediaSessionRegistered) return
        mainHandler.removeCallbacks(mediaUpdateRunnable)
        mainHandler.removeCallbacks(mediaProgressRunnable)
        cancelPausedMediaGrace()
        try { activeMediaSessionsListener?.let { mediaSessionManager?.removeOnActiveSessionsChangedListener(it) } } catch (_: Exception) {}
        activeMediaSessionsListener = null
        for ((token, callback) in controllerCallbacks) {
            monitoredControllers.find { it.sessionToken == token }?.let {
                try { it.unregisterCallback(callback) } catch (_: Exception) {}
            }
        }
        controllerCallbacks.clear()
        monitoredControllers.clear()
        activeMediaController = null
        currentMediaKey = null
        overlayView?.dismissMediaPlayback()
        isMediaSessionRegistered = false
    }

    private fun updateActiveMediaSessions(controllers: List<MediaController>?, isInitial: Boolean = false) {
        mainHandler.post {
            val valid = controllers ?: emptyList()
            val tokens = valid.map { it.sessionToken }.toSet()
            controllerCallbacks.entries.removeAll { (token, callback) ->
                if (tokens.contains(token)) return@removeAll false
                monitoredControllers.find { it.sessionToken == token }?.let {
                    try { it.unregisterCallback(callback) } catch (_: Exception) {}
                }
                true
            }
            monitoredControllers.removeAll { !tokens.contains(it.sessionToken) }
            valid.forEach { controller ->
                if (!controllerCallbacks.containsKey(controller.sessionToken)) {
                    val callback = object : MediaController.Callback() {
                        override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) = scheduleMediaUpdate()
                        override fun onMetadataChanged(metadata: MediaMetadata?) = scheduleMediaUpdate()
                        override fun onSessionDestroyed() = scheduleMediaUpdate()
                    }
                    try {
                        controller.registerCallback(callback, mainHandler)
                        controllerCallbacks[controller.sessionToken] = callback
                        monitoredControllers.add(controller)
                    } catch (_: Exception) {}
                }
            }
            if (isInitial) {
                applyCurrentMediaState()
            } else {
                scheduleMediaUpdate()
            }
        }
    }

    private fun getExcludedMediaPackages(): Set<String> =
        settingsRepository.loadIslandMediaExcludedApps().filter { it.isEnabled }.map { it.packageName }.toSet()

    private var pausedMediaRunnable: Runnable? = null

    private fun cancelPausedMediaGrace() {
        pausedMediaRunnable?.let { mainHandler.removeCallbacks(it) }
        pausedMediaRunnable = null
    }

    private fun applyCurrentMediaState() {
        mainHandler.post {
            val ov = overlayView ?: return@post
            if (!settingsRepository.isIslandShowMediaEnabled() || isIslandContentSuppressed) {
                cancelPausedMediaGrace()
                activeMediaController = null
                ov.dismissMediaPlayback()
                return@post
            }
            val excludedPackages = getExcludedMediaPackages()
            val playing = monitoredControllers.firstOrNull {
                it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING &&
                    !excludedPackages.contains(it.packageName)
            }

            if (playing != null) {
                cancelPausedMediaGrace()
                activeMediaController = playing
                ov.setMediaPaused(false)
                ov.setMediaTransportPlaying(true)
                ov.setMediaLiked(isControllerLiked(playing))
                val metadata = playing.metadata
                val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
                val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
                if (title.isBlank() && artist.isBlank()) return@post
                val key = "${playing.packageName}_${title}_${artist}"
                if (currentMediaKey == key && ov.isMediaPlaybackActive) return@post
                val isSameTrackAsBefore = currentMediaKey == key
                currentMediaKey = key
                handlerScope.launch(Dispatchers.IO) {
                    val artwork = extractMediaArtwork(metadata)
                    withContext(Dispatchers.Main) {
                        if (activeMediaController?.sessionToken == playing.sessionToken && !ov.isNotificationAlertActive && !isIslandContentSuppressed) {
                            ensureOverlayAttached()
                            ov.showMediaPlayback(title, artist, artwork, startCompact = isSameTrackAsBefore)
                            expandTouchAnchorForNotification()
                            scheduleDismissTimer()
                            pollCalendarEvent()
                        }
                    }
                }
                return@post
            }

            val current = activeMediaController
            val isCurrentPaused = current != null && ov.isMediaPlaybackActive &&
                monitoredControllers.any { it.sessionToken == current.sessionToken } &&
                !excludedPackages.contains(current.packageName) &&
                current.playbackState?.state == android.media.session.PlaybackState.STATE_PAUSED

            if (isCurrentPaused) {
                ov.setMediaTransportPlaying(false)
                if (pausedMediaRunnable == null) {
                    ov.setMediaPaused(true)
                    pausedMediaRunnable = Runnable {
                        pausedMediaRunnable = null
                        activeMediaController = null
                        ov.dismissMediaPlayback()
                    }
                    mainHandler.postDelayed(pausedMediaRunnable!!, PAUSED_MEDIA_GRACE_MS)
                }
            } else {
                cancelPausedMediaGrace()
                activeMediaController = null
                ov.dismissMediaPlayback()
            }
        }
    }

    private fun extractMediaArtwork(metadata: MediaMetadata?): Bitmap? {
        var bitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        if (bitmap == null) {
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            if (!title.isNullOrBlank()) {
                val hash = kotlin.math.abs("${title}_$artist".hashCode().toLong())
                bitmap = NotificationListener.getCachedBitmap(hash)
                if (bitmap == null) {
                    val artFile = File(service.cacheDir, "art_$hash.png")
                    if (artFile.exists()) {
                        try {
                            bitmap = BitmapFactory.decodeFile(artFile.absolutePath)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        if (bitmap == null) {
            bitmap = NotificationListener.getLatestArtBitmap()
        }
        if (bitmap == null) {
            val tempArtFile = File(service.cacheDir, "temp_album_art.png")
            if (tempArtFile.exists()) {
                try {
                    bitmap = BitmapFactory.decodeFile(tempArtFile.absolutePath)
                } catch (_: Exception) {}
            }
        }
        return bitmap
    }

    private fun unregisterNotificationsListener() {
        if (isNotificationsListenerRegistered) {
            NotificationListener.removeNotificationAlertListener(notificationAlertListener)
            isNotificationsListenerRegistered = false
            mainHandler.removeCallbacks(dismissNotificationRunnable)
            overlayView?.dismissNotificationAlert()
            restoreTouchAnchor()
        }
    }

    private fun ensureOverlayAttached() {
        if (isOverlayAdded && overlayView != null) return
        updateOverlay()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateOverlay() {
        val wm = windowManager ?: return

        if (!settingsRepository.isIslandEnabled() || isSuppressedByOrientationOrFullscreen) {
            unregisterNotificationsListener()
            unregisterMediaListener()
            removeOverlay()
            return
        }

        registerNotificationsListener()
        registerMediaListener()

        if (overlayView == null) {
            overlayView = IslandOverlayView(service).apply {
                this.isIslandEnabled = settingsRepository.isIslandEnabled()
                this.isShowGlow = settingsRepository.isIslandShowGlowEnabled()
                this.expandedWidthDp = settingsRepository.getIslandExpandedWidth()
                this.expandedCornerRadiusDp = settingsRepository.getIslandExpandedRoundness()
                this.expandedPaddingDp = settingsRepository.getIslandExpandedPadding()
                this.expandedTopPaddingDp = settingsRepository.getIslandExpandedTopPadding()
                this.touchHandler = this@IslandOverlayHandler.touchHandler
                this.onAlertsChanged = {
                    if (hasActiveContent()) expandTouchAnchorForNotification()
                }
                this.onDismissAnimationEnd = {
                    if (hasActiveContent()) expandTouchAnchorForNotification() else restoreTouchAnchor()
                    pollCalendarEvent()
                }
            }

            val params = OverlayHelper.createOverlayLayoutParams(
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                isTouchable = false
            )

            try {
                wm.addView(overlayView, params)
                isOverlayAdded = true
            } catch (e: Exception) {
                Log.e("IslandOverlayHandler", "Failed to add Island overlay", e)
            }
        } else {
            overlayView?.touchHandler = touchHandler
        }

        if (touchAnchorView == null) {
            touchAnchorView = View(service).apply {
                setOnTouchListener { _, event ->
                    touchHandler.onTouchEvent(event)
                }
            }
        }
        touchHandler.overlayView = overlayView

        updateOverlayPosition()
        updateIdlePill()
        updateFlashlightState()
    }

    private fun updateOverlayPosition() {
        val wm = windowManager ?: return

        val screenWidth: Int
        val screenHeight: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics: WindowMetrics = wm.maximumWindowMetrics
            val bounds: Rect = metrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
        } else {
            val size = Point()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealSize(size)
            screenWidth = size.x
            screenHeight = size.y
        }

        val sizeScale = settingsRepository.getIslandCameraSize()
        val useAutoDetect = settingsRepository.isIslandAutoDetectEnabled()
        var detectedCutout = false

        if (useAutoDetect && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val cutout: DisplayCutout? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    wm.maximumWindowMetrics.windowInsets.displayCutout
                } else {
                    null
                }

                if (cutout != null) {
                    val rects = cutout.boundingRects
                    if (rects.isNotEmpty()) {
                        val topCutout = rects.find { it.top == 0 } ?: rects.first()
                        cameraCenterX = topCutout.centerX().toFloat()
                        cameraCenterY = topCutout.centerY().toFloat()
                        val baseRadius = (topCutout.width().coerceAtMost(topCutout.height()) / 2f).coerceAtLeast(12f * service.resources.displayMetrics.density)
                        cameraRadiusPx = baseRadius * sizeScale
                        detectedCutout = true
                    }
                }
            } catch (_: Exception) {}
        }

        if (!detectedCutout) {
            val offsetXPercent = settingsRepository.getIslandCameraOffsetX()
            val offsetYPercent = settingsRepository.getIslandCameraOffsetY()

            cameraCenterX = (offsetXPercent / 100f) * screenWidth
            cameraCenterY = (offsetYPercent / 100f) * screenHeight
            cameraRadiusPx = (16f * service.resources.displayMetrics.density) * sizeScale
        }

        overlayView?.cameraCenterX = cameraCenterX
        overlayView?.cameraCenterY = cameraCenterY
        overlayView?.cameraRadiusPx = cameraRadiusPx
        overlayView?.maxWidthDp = settingsRepository.getIslandMaxWidth()
        overlayView?.cutoutGapDp = settingsRepository.getIslandCutoutGap()
        overlayView?.invalidate()
    }

    private fun expandTouchAnchorForNotification() {
        val wm = windowManager ?: return
        val anchor = touchAnchorView ?: return
        val ov = overlayView ?: return

        touchHandler.overlayView = ov

        val targetBounds = ov.getNotificationTargetBounds()
        val density = service.resources.displayMetrics.density
        val padH = 16f * density
        val padV = 10f * density

        val touchWidth = ((targetBounds.right - targetBounds.left) + padH * 2).toInt().coerceAtLeast((48f * density).toInt())
        val touchHeight = ((targetBounds.bottom - targetBounds.top) + padV * 2).toInt().coerceAtLeast((48f * density).toInt())

        val touchParams = WindowManager.LayoutParams(
            touchWidth,
            touchHeight,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (targetBounds.left - padH).toInt().coerceAtLeast(0)
            y = (targetBounds.top - padV).toInt().coerceAtLeast(0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        if (!isTouchAnchorAdded) {
            try {
                wm.addView(anchor, touchParams)
                isTouchAnchorAdded = true
            } catch (e: Exception) {
                Log.e("IslandOverlayHandler", "Failed to add Island touch anchor", e)
            }
        } else {
            try {
                wm.updateViewLayout(anchor, touchParams)
            } catch (e: Exception) {
                Log.e("IslandOverlayHandler", "Failed to update Island touch anchor", e)
            }
        }
    }

    private fun hasActiveContent(): Boolean {
        val ov = overlayView ?: return false
        return ov.isNotificationAlertActive || ov.isMediaPlaybackActive || ov.isCalendarActive ||
            ov.isConsciousGateActive || ov.isFlashlightActive
    }

    private fun restoreTouchAnchor() {
        val wm = windowManager ?: return
        val anchor = touchAnchorView ?: return
        if (isTouchAnchorAdded) {
            try {
                wm.removeViewImmediate(anchor)
                isTouchAnchorAdded = false
            } catch (_: Exception) {}
        }
    }

    private fun removeOverlay() {
        val wm = windowManager ?: return
        restoreTouchAnchor()
        overlayView?.let {
            if (isOverlayAdded) {
                try {
                    wm.removeViewImmediate(it)
                } catch (_: Exception) {}
                isOverlayAdded = false
            }
        }
        overlayView = null
    }

    fun onDestroy() {
        settingsRepository.unregisterOnSharedPreferenceChangeListener(this)
        try {
            service.unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
        try {
            service.unregisterReceiver(idleReceiver)
        } catch (_: Exception) {}
        try {
            cameraManager.unregisterTorchCallback(torchCallback)
        } catch (_: Exception) {}
        mainHandler.removeCallbacks(calendarPollRunnable)
        mainHandler.removeCallbacks(revertCalendarExpansionRunnable)
        mainHandler.removeCallbacks(revertConsciousGateExpansionRunnable)
        mainHandler.removeCallbacks(mediaProgressRunnable)
        mainHandler.removeCallbacks(consciousGateUpdateRunnable)
        unregisterNotificationsListener()
        unregisterMediaListener()
        removeOverlay()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            SettingsRepository.KEY_ISLAND_ENABLED -> updateOverlay()
            SettingsRepository.KEY_ISLAND_SHOW_GLOW -> {
                overlayView?.isShowGlow = settingsRepository.isIslandShowGlowEnabled()
            }
            SettingsRepository.KEY_ISLAND_SHOW_CONSCIOUS_GATE -> {
                updateConsciousGateState()
            }
            SettingsRepository.KEY_ISLAND_SHOW_FLASHLIGHT -> updateFlashlightState()
            SettingsRepository.KEY_ISLAND_SHOW_TIME_BATTERY,
            SettingsRepository.KEY_ISLAND_BATTERY_STYLE,
            SettingsRepository.KEY_DUO_BATTERY_CHARGING_COLOR_ENABLED,
            SettingsRepository.KEY_DUO_BATTERY_CHARGING_COLOR,
            SettingsRepository.KEY_DUO_BATTERY_POWER_SAVE_COLOR_ENABLED,
            SettingsRepository.KEY_DUO_BATTERY_POWER_SAVE_COLOR,
            SettingsRepository.KEY_DUO_BATTERY_LOW_COLOR_ENABLED,
            SettingsRepository.KEY_DUO_BATTERY_LOW_COLOR,
            SettingsRepository.KEY_DUO_BATTERY_CRITICAL_COLOR_ENABLED,
            SettingsRepository.KEY_DUO_BATTERY_CRITICAL_COLOR -> {
                updateIdlePill()
            }
            SettingsRepository.KEY_ISLAND_SHOW_MEDIA,
            SettingsRepository.KEY_ISLAND_MEDIA_EXCLUDED_APPS -> {
                applyCurrentMediaState()
            }
            SettingsRepository.KEY_ISLAND_SHOW_CALENDAR -> {
                pollCalendarEvent()
            }
            SettingsRepository.KEY_ISLAND_EXPANDED_WIDTH -> {
                overlayView?.expandedWidthDp = settingsRepository.getIslandExpandedWidth()
            }
            SettingsRepository.KEY_ISLAND_EXPANDED_ROUNDNESS -> {
                overlayView?.expandedCornerRadiusDp = settingsRepository.getIslandExpandedRoundness()
            }
            SettingsRepository.KEY_ISLAND_EXPANDED_PADDING -> {
                overlayView?.expandedPaddingDp = settingsRepository.getIslandExpandedPadding()
            }
            SettingsRepository.KEY_ISLAND_EXPANDED_TOP_PADDING -> {
                overlayView?.expandedTopPaddingDp = settingsRepository.getIslandExpandedTopPadding()
            }
            SettingsRepository.KEY_ISLAND_HIDE_WHEN_SCREEN_OFF -> {
                if (isIslandContentSuppressed) {
                    mainHandler.removeCallbacks(dismissNotificationRunnable)
                    mainHandler.removeCallbacks(consciousGateUpdateRunnable)
                    overlayView?.dismissNotificationAlert()
                    overlayView?.dismissMediaPlayback()
                    overlayView?.dismissCalendarEvent()
                    overlayView?.dismissConsciousGateSession()
                    restoreTouchAnchor()
                } else {
                    applyCurrentMediaState()
                    pollCalendarEvent()
                    updateConsciousGateState()
                }
                updateIdlePill()
            }
            SettingsRepository.KEY_ISLAND_USE_AUTO_DETECT,
            SettingsRepository.KEY_ISLAND_CAMERA_OFFSET_X,
            SettingsRepository.KEY_ISLAND_CAMERA_OFFSET_Y,
            SettingsRepository.KEY_ISLAND_CAMERA_SIZE,
            SettingsRepository.KEY_ISLAND_MAX_WIDTH,
            SettingsRepository.KEY_ISLAND_CUTOUT_GAP -> updateOverlayPosition()
        }
    }

    private companion object {
        private const val MEDIA_UPDATE_DEBOUNCE_MS = 2000L
        private const val PAUSED_MEDIA_GRACE_MS = 3000L
        private const val CALENDAR_POLL_INTERVAL_MS = 60000L
        private const val CALENDAR_DEFAULT_EXPANDED_MS = 8000L
    }
}
