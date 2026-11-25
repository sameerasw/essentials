/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Handlers
 * File: StatusGlanceHandler.kt
 * Description: Background handler managing the Status Glance ambient overlay indicator.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.CalendarRepository
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.CalendarEventUtil
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.utils.StatusGlanceView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatusGlanceHandler(
    private val service: AccessibilityService,
) {
    private var windowManager: WindowManager? = null
    private var glanceView: StatusGlanceView? = null
    private var isOverlayAdded = false
    private var touchAnchorView: View? = null
    private var isTouchAnchorAdded = false
    private var touchHandler: StatusGlanceTouchHandler? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val settingsRepository by lazy { SettingsRepository(service) }
    private val keyguardManager by lazy { service.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }
    private val cameraManager by lazy { service.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val mediaSessionManager by lazy { service.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager }

    private var isScreenOff = false
    private var isLocked = false
    private var isShadeExpanded = false
    private var isFullscreen = false
    private var isLandscape = false
    private var lastUnlockTimestamp = 0L

    private var cachedIsMediaPlaying = false
    private var cachedMediaTitle = ""
    private var cachedMediaArtist = ""
    private var cachedMediaArtwork: Bitmap? = null
    private var cachedEventTitle = ""
    private var cachedEventTimeMillis = 0L
    private var cachedIsEventToday = false
    private var cachedTimeString = ""

    private var cachedBatteryLevel = 100
    private var cachedIsBatteryCharging = false
    private var cachedIsBatteryFull = false
    private var cachedIsPowerSaveMode = false
    private var isBatteryReceiverRegistered = false

    private val handlerScope = CoroutineScope(Dispatchers.Main + Job())
    private var calendarObserverRegistered = false

    private var isFlashlightOn = false
    private var isTorchCallbackRegistered = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            val primaryId = getCameraId()
            if (cameraId != primaryId) return
            mainHandler.post {
                isFlashlightOn = enabled
                glanceView?.isFlashlightOn = enabled
            }
        }
    }

    private val monitoredControllers = mutableListOf<MediaController>()
    private val controllerCallbacks = mutableMapOf<MediaSession.Token, MediaController.Callback>()
    private var activeMediaController: MediaController? = null
    private var isMediaSessionRegistered = false
    private var currentMediaKey: String? = null

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateTime()
            queryUpcomingCalendarEvent()
        }
    }
    private var isTimeReceiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateBatteryStatus(intent)
        }
    }

    fun init() {
        windowManager = service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val powerManager = service.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isInteractive = powerManager?.isInteractive ?: true
        isScreenOff = !isInteractive
        isLocked = keyguardManager?.isKeyguardLocked ?: false
        updateState()
    }

    fun updateState() {
        mainHandler.post {
            if (windowManager == null) {
                windowManager = service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            }
            val isEnabled = settingsRepository.isStatusGlanceEnabled()

            if (isEnabled) {
                registerTorchCallback()
                registerMediaListener()
                registerCalendarObserver()
                registerTimeReceiver()
                registerBatteryReceiver()

                if (!isOverlayAdded) {
                    createOverlay()
                } else {
                    updateOverlayParams()
                }

                updateTime()
                updateBatteryStatus()
                queryUpcomingCalendarEvent()
                reEvaluateAndApplyMedia(isInitial = true)
                syncConfigToView()
                applyOrientationVisibility()
            } else {
                removeOverlay()
                unregisterTorchCallback()
                unregisterMediaListener()
                unregisterCalendarObserver()
                unregisterTimeReceiver()
        mainHandler.removeCallbacks(secondTick)
                unregisterBatteryReceiver()
            }
        }
    }

    private fun isPlacementHidden(): Boolean =
        if (settingsRepository.isFoldableDevice()) settingsRepository.isStatusGlanceHiddenInCurrentOrientation()
        else isLandscape

    private fun applyOrientationVisibility() {
        glanceView?.isLandscape = isPlacementHidden()
        glanceView?.visibility = if (isPlacementHidden()) View.GONE else View.VISIBLE
        updateTouchAnchor()
    }

    fun setFullscreen(fullscreen: Boolean) {
        if (isFullscreen != fullscreen) {
            isFullscreen = fullscreen
            mainHandler.post {
                glanceView?.isFullscreen = fullscreen
                updateTouchAnchor()
            }
        }
    }

    fun setShadeExpanded(expanded: Boolean) {
        if (isScreenOff || isLocked) {
            if (isShadeExpanded) {
                isShadeExpanded = false
                mainHandler.post {
                    glanceView?.isShadeExpanded = false
                    updateTouchAnchor()
                }
            }
            return
        }

        if (expanded) {
            val elapsedSinceUnlock = SystemClock.elapsedRealtime() - lastUnlockTimestamp
            if (elapsedSinceUnlock < 600L) {
                return
            }
        }

        if (isShadeExpanded != expanded) {
            isShadeExpanded = expanded
            mainHandler.post {
                glanceView?.isShadeExpanded = expanded
                updateTouchAnchor()
            }
        }
    }

    private fun syncConfigToView() {
        updateTime()
        if (settingsRepository.isStatusGlanceEnabled() && (!isOverlayAdded || glanceView == null)) {
            createOverlay()
        }
        glanceView?.let { v ->
            val isNightMode = (service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            v.isDarkTheme = isNightMode
            v.showFlashlight = settingsRepository.isStatusGlanceShowFlashlightEnabled()
            v.showCalendar = settingsRepository.isStatusGlanceShowCalendarEnabled()
            v.showMedia = settingsRepository.isStatusGlanceShowMediaEnabled()
            v.showTime = settingsRepository.isStatusGlanceShowTimeEnabled()
            v.useBackgroundPill = settingsRepository.isStatusGlanceBackgroundPillEnabled()
            v.useAlbumArtColors = settingsRepository.isStatusGlanceAlbumArtColorsEnabled()
            v.hideWhenFullscreen = settingsRepository.isStatusGlanceHideWhenFullscreenEnabled()
            v.hideInQuickSettings = settingsRepository.isStatusGlanceHideInQuickSettingsEnabled()
            v.hideWhenLocked = settingsRepository.isStatusGlanceHideWhenLockedEnabled()
            v.maxWidthDp = settingsRepository.getStatusGlanceMaxWidth()
            v.fontSize = settingsRepository.getStatusGlanceFontSize()
            v.isBoldText = settingsRepository.isStatusGlanceBoldTextEnabled()
            v.isFlashlightOn = isFlashlightOn

            v.showBattery = settingsRepository.isStatusGlanceShowBatteryEnabled()
            v.batteryDisplayMode = settingsRepository.getStatusGlanceBatteryDisplayMode()
            v.batteryIconSizeDp = settingsRepository.getStatusGlanceBatteryIconSize()
            v.showBatteryWhenLow = settingsRepository.isStatusGlanceBatteryShowLowEnabled()
            v.showBatteryWhileCharging = settingsRepository.isStatusGlanceBatteryShowChargingEnabled()
            v.showBatteryWhileFull = settingsRepository.isStatusGlanceBatteryShowFullEnabled()
            v.showBatteryOtherwise = settingsRepository.isStatusGlanceBatteryShowOtherwiseEnabled()

            v.batteryLevel = cachedBatteryLevel
            v.isBatteryCharging = cachedIsBatteryCharging
            v.isBatteryFull = cachedIsBatteryFull
            v.isPowerSaveMode = cachedIsPowerSaveMode

            v.nextEventTitle = cachedEventTitle
            v.nextEventTimeMillis = cachedEventTimeMillis
            v.isEventToday = cachedIsEventToday
            v.currentTimeString = cachedTimeString

            v.isMediaPlaying = cachedIsMediaPlaying
            v.mediaTitle = cachedMediaTitle
            v.mediaArtist = cachedMediaArtist
            v.mediaArtworkBitmap = cachedMediaArtwork

            isLandscape = service.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            v.isLandscape = isPlacementHidden()
            v.isFullscreen = isFullscreen
            v.isShadeExpanded = isShadeExpanded
            v.isLocked = isLocked
            v.isScreenOff = isScreenOff

            updateGlancePosition()
            v.reevaluateSlot()
        }
    }

    private fun createOverlay() {
        if (isOverlayAdded) return
        if (windowManager == null) {
            windowManager = service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        }
        val wm = windowManager ?: return

        if (glanceView == null) {
            glanceView = StatusGlanceView(service)
        }
        val params = getOverlayLayoutParams()

        try {
            glanceView?.playEnter()
            wm.addView(glanceView, params)
            isOverlayAdded = true
            updateGlancePosition()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateOverlayParams() {
        if (!isOverlayAdded || glanceView == null || windowManager == null) return
        try {
            val params = getOverlayLayoutParams()
            windowManager?.updateViewLayout(glanceView, params)
            updateGlancePosition()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    private fun getOverlayLayoutParams(): WindowManager.LayoutParams {
        val type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

        val flags = (
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun updateGlancePosition() {
        val view = glanceView ?: return
        val dm = service.resources.displayMetrics
        val offsetXPercent = settingsRepository.getStatusGlanceOffsetX()
        val offsetYPercent = settingsRepository.getStatusGlanceOffsetY()
        view.glanceCenterX = dm.widthPixels * (offsetXPercent / 100f)
        view.glanceCenterY = dm.heightPixels * (offsetYPercent / 100f)
        updateTouchAnchor()
    }

    @Suppress("DEPRECATION")
    private fun updateTouchAnchor() {
        if (!settingsRepository.isStatusGlanceEnabled() || glanceView == null || windowManager == null || isScreenOff || (isLocked && settingsRepository.isStatusGlanceHideWhenLockedEnabled()) || isPlacementHidden() || (isFullscreen && settingsRepository.isStatusGlanceHideWhenFullscreenEnabled()) || (isShadeExpanded && settingsRepository.isStatusGlanceHideInQuickSettingsEnabled())) {
            removeTouchAnchor()
            return
        }

        if (touchHandler == null) {
            touchHandler = StatusGlanceTouchHandler(service)
        }
        touchHandler?.apply {
            this.glanceView = this@StatusGlanceHandler.glanceView
            this.activeMediaControllerProvider = { activeMediaController }
        }

        if (touchAnchorView == null) {
            touchAnchorView = View(service).apply {
                setOnTouchListener { _, event ->
                    touchHandler?.onTouchEvent(event) ?: false
                }
            }
        }

        val dm = service.resources.displayMetrics
        val density = dm.density
        val maxWidth = settingsRepository.getStatusGlanceMaxWidth() * density
        val height = 36f * density
        val width = maxWidth.coerceAtLeast(48f * density).toInt()
        val heightPx = height.toInt()

        val glanceCenterX = glanceView?.glanceCenterX ?: (dm.widthPixels * (settingsRepository.getStatusGlanceOffsetX() / 100f))
        val glanceCenterY = glanceView?.glanceCenterY ?: (dm.heightPixels * (settingsRepository.getStatusGlanceOffsetY() / 100f))

        val touchParams = WindowManager.LayoutParams(
            width,
            heightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (glanceCenterX - 8f * density).toInt().coerceAtLeast(0)
            y = (glanceCenterY - heightPx / 2f).toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        if (!isTouchAnchorAdded) {
            try {
                windowManager?.addView(touchAnchorView, touchParams)
                isTouchAnchorAdded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                windowManager?.updateViewLayout(touchAnchorView, touchParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun removeTouchAnchor() {
        if (isTouchAnchorAdded && touchAnchorView != null && windowManager != null) {
            try {
                windowManager?.removeView(touchAnchorView)
            } catch (_: Exception) {}
            isTouchAnchorAdded = false
        }
    }

    private val secondTick = Runnable { updateTime() }

    private fun updateTime() {
        val is24Hour = DateFormat.is24HourFormat(service)
        val seconds = settingsRepository.isStatusGlanceShowSecondsEnabled()
        val pattern = (if (is24Hour) "HH:mm" else "h:mm") + if (seconds) ":ss" else ""
        val timeFormat = SimpleDateFormat(pattern, Locale.getDefault())
        val formattedTime = timeFormat.format(Date())
        cachedTimeString = formattedTime
        glanceView?.currentTimeString = formattedTime

        mainHandler.removeCallbacks(secondTick)
        if (seconds && !isScreenOff) {
            mainHandler.postDelayed(secondTick, 1000L - System.currentTimeMillis() % 1000L)
        }
    }

    private fun updateBatteryStatus(intent: Intent? = null) {
        val batteryIntent = intent ?: service.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100f).toInt() else 100

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val isCharging = plugged && !com.sameerasw.essentials.utils.battery.ChargeLimitUtil.isShielded(service, batteryPct, plugged)
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL || batteryPct >= 100

        val powerManager = service.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSave = powerManager?.isPowerSaveMode ?: false

        cachedBatteryLevel = batteryPct
        cachedIsBatteryCharging = isCharging
        cachedIsBatteryFull = isFull
        cachedIsPowerSaveMode = isPowerSave

        mainHandler.post {
            glanceView?.let { v ->
                v.batteryLevel = batteryPct
                v.isBatteryCharging = isCharging
                v.isBatteryFull = isFull
                v.isPowerSaveMode = isPowerSave
            }
        }
    }

    private fun registerBatteryReceiver() {
        if (isBatteryReceiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            }
            service.registerReceiver(batteryReceiver, filter)
            isBatteryReceiverRegistered = true
            updateBatteryStatus()
        } catch (_: Exception) {}
    }

    private fun unregisterBatteryReceiver() {
        if (!isBatteryReceiverRegistered) return
        try {
            service.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        isBatteryReceiverRegistered = false
    }

    private fun formatRelativeEventTime(eventTimeMillis: Long, now: Long): String {
        val diffMillis = (eventTimeMillis - now).coerceAtLeast(0L)
        val minutes = (diffMillis / (60 * 1000L)).toInt()
        val hours = minutes / 60
        val days = hours / 24

        val timeStr = when {
            minutes < 1 -> "1m"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> {
                val remMins = minutes % 60
                if (remMins > 0) "${hours}h ${remMins}m" else "${hours}h"
            }
            else -> {
                val remHours = hours % 24
                if (remHours > 0) "${days}d ${remHours}h" else "${days}d"
            }
        }
        return service.getString(R.string.status_glance_calendar_event_in_time, timeStr)
    }

    private fun queryUpcomingCalendarEvent() {
        handlerScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val next =
                CalendarEventUtil.queryNextUpcomingEvent(
                    service,
                    settingsRepository.getStatusGlanceCalendarTimeframe(),
                    settingsRepository.getStatusGlanceCalendarSelectedCalendars().mapNotNull { it.toLongOrNull() }.toSet(),
                    settingsRepository.isStatusGlanceCalendarShowAllDayEnabled(),
                )
            if (next != null) {
                val fullTitle = "${next.title} ${formatRelativeEventTime(next.startTimeMillis, now)}"
                cachedEventTitle = fullTitle
                cachedEventTimeMillis = next.startTimeMillis
                cachedIsEventToday = true
                withContext(Dispatchers.Main) {
                    glanceView?.nextEventTitle = fullTitle
                    glanceView?.nextEventTimeMillis = next.startTimeMillis
                    glanceView?.isEventToday = true
                }
            } else {
                cachedEventTitle = ""
                cachedIsEventToday = false
                withContext(Dispatchers.Main) {
                    glanceView?.nextEventTitle = ""
                    glanceView?.isEventToday = false
                }
            }
        }
    }

    private val onCalendarChanged: () -> Unit = { queryUpcomingCalendarEvent() }

    private fun registerCalendarObserver() {
        if (calendarObserverRegistered) return
        CalendarRepository.addListener(service, onCalendarChanged)
        calendarObserverRegistered = true
    }

    private fun unregisterCalendarObserver() {
        if (!calendarObserverRegistered) return
        CalendarRepository.removeListener(onCalendarChanged)
        calendarObserverRegistered = false
    }

    private fun registerTimeReceiver() {
        if (isTimeReceiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            service.registerReceiver(timeTickReceiver, filter)
            isTimeReceiverRegistered = true
        } catch (_: Exception) {}
    }

    private fun unregisterTimeReceiver() {
        if (!isTimeReceiverRegistered) return
        try {
            service.unregisterReceiver(timeTickReceiver)
        } catch (_: Exception) {}
        isTimeReceiverRegistered = false
    }

    private fun registerTorchCallback() {
        if (com.sameerasw.essentials.utils.DeviceUtils.isTorchAccessRestricted()) return
        if (isTorchCallbackRegistered) return
        try {
            cameraManager.registerTorchCallback(torchCallback, mainHandler)
            isTorchCallbackRegistered = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unregisterTorchCallback() {
        if (!isTorchCallbackRegistered) return
        try {
            cameraManager.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isTorchCallbackRegistered = false
    }

    private fun getExcludedPackages(): Set<String> {
        val excludedAppsJson = settingsRepository.getString(SettingsRepository.KEY_AOD_WALLPAPER_MEDIA_EXCLUDED_APPS, null)
        return if (!excludedAppsJson.isNullOrBlank()) {
            try {
                val listType = object : TypeToken<List<AppSelection>>() {}.type
                val apps: List<AppSelection> = Gson().fromJson(excludedAppsJson, listType) ?: emptyList()
                apps.filter { it.isEnabled }.map { it.packageName }.toSet()
            } catch (_: Exception) {
                emptySet()
            }
        } else {
            emptySet()
        }
    }

    private var activeSessionsChangedListener: MediaSessionManager.OnActiveSessionsChangedListener? = null

    private fun registerMediaListener() {
        if (!isMediaSessionRegistered && mediaSessionManager != null) {
            try {
                val componentName = ComponentName(service, NotificationListener::class.java)
                val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                    updateActiveMediaSessions(controllers)
                }
                activeSessionsChangedListener = listener
                mediaSessionManager?.addOnActiveSessionsChangedListener(
                    listener,
                    componentName,
                    mainHandler
                )
                isMediaSessionRegistered = true
                val initialControllers = mediaSessionManager?.getActiveSessions(componentName)
                updateActiveMediaSessions(initialControllers, isInitial = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun unregisterMediaListener() {
        if (!isMediaSessionRegistered) return
        try {
            activeSessionsChangedListener?.let {
                mediaSessionManager?.removeOnActiveSessionsChangedListener(it)
            }
            activeSessionsChangedListener = null
            val iterator = controllerCallbacks.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val controller = monitoredControllers.find { it.sessionToken == entry.key }
                try {
                    controller?.unregisterCallback(entry.value)
                } catch (_: Exception) {}
                iterator.remove()
            }
            monitoredControllers.clear()
            activeMediaController = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isMediaSessionRegistered = false
    }

    private fun updateActiveMediaSessions(controllers: List<MediaController>?, isInitial: Boolean = false) {
        mainHandler.post {
            val excludedPackages = getExcludedPackages()
            val validControllers = controllers?.filter { !excludedPackages.contains(it.packageName) } ?: emptyList()

            val currentTokens = validControllers.map { it.sessionToken }.toSet()
            val iterator = controllerCallbacks.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (!currentTokens.contains(entry.key)) {
                    val controller = monitoredControllers.find { it.sessionToken == entry.key }
                    try {
                        controller?.unregisterCallback(entry.value)
                    } catch (_: Exception) {}
                    iterator.remove()
                }
            }
            monitoredControllers.removeAll { !currentTokens.contains(it.sessionToken) }

            for (controller in validControllers) {
                if (!controllerCallbacks.containsKey(controller.sessionToken)) {
                    val callback = object : MediaController.Callback() {
                        override fun onPlaybackStateChanged(state: PlaybackState?) {
                            reEvaluateAndApplyMedia()
                        }

                        override fun onMetadataChanged(metadata: MediaMetadata?) {
                            reEvaluateAndApplyMedia()
                        }

                        override fun onSessionDestroyed() {
                            mainHandler.post {
                                try {
                                    controller.unregisterCallback(this)
                                } catch (_: Exception) {}
                                controllerCallbacks.remove(controller.sessionToken)
                                monitoredControllers.removeAll { it.sessionToken == controller.sessionToken }
                                reEvaluateAndApplyMedia()
                            }
                        }
                    }
                    try {
                        controller.registerCallback(callback, mainHandler)
                        controllerCallbacks[controller.sessionToken] = callback
                        monitoredControllers.add(controller)
                    } catch (_: Exception) {}
                }
            }

            reEvaluateAndApplyMedia(isInitial = isInitial)
        }
    }

    private fun reEvaluateAndApplyMedia(isInitial: Boolean = false) {
        mainHandler.post {
            val excludedPackages = getExcludedPackages()
            val valid = monitoredControllers.filter { !excludedPackages.contains(it.packageName) }

            val currentIsPlaying = activeMediaController?.let { current ->
                valid.any { it.sessionToken == current.sessionToken } &&
                    current.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: false

            val target = if (currentIsPlaying) {
                activeMediaController
            } else {
                val playingController = valid.firstOrNull {
                    it.playbackState?.state == PlaybackState.STATE_PLAYING
                }
                playingController
                    ?: valid.firstOrNull { it.sessionToken == activeMediaController?.sessionToken }
                    ?: valid.firstOrNull()
            }

            activeMediaController = target
            checkAndApplyMediaState()
        }
    }

    private fun checkAndApplyMediaState() {
        val showMedia = settingsRepository.isStatusGlanceShowMediaEnabled()
        val controller = activeMediaController
        val excludedPackages = getExcludedPackages()

        val isExcluded = controller != null && excludedPackages.contains(controller.packageName)
        val state = controller?.playbackState
        val isPlaying = showMedia && !isExcluded && state?.state == PlaybackState.STATE_PLAYING

        val title = if (!isExcluded) controller?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "" else ""
        val artist = if (!isExcluded) controller?.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "" else ""
        val pkg = controller?.packageName ?: ""
        val mediaKey = "${pkg}_${title}_$artist"

        cachedIsMediaPlaying = isPlaying
        cachedMediaTitle = title
        cachedMediaArtist = artist

        if (!isPlaying) {
            currentMediaKey = null
            cachedMediaArtwork = null
            mainHandler.post {
                glanceView?.let { v ->
                    v.isMediaPlaying = false
                    v.mediaTitle = ""
                    v.mediaArtist = ""
                    v.mediaArtworkBitmap = null
                }
            }
            return
        }

        if (currentMediaKey != mediaKey || cachedMediaArtwork == null) {
            currentMediaKey = mediaKey
            handlerScope.launch(Dispatchers.IO) {
                val art = extractMediaArtwork(controller.metadata, controller)
                withContext(Dispatchers.Main) {
                    if (currentMediaKey == mediaKey && cachedIsMediaPlaying) {
                        cachedMediaArtwork = art
                        glanceView?.let { v ->
                            v.isMediaPlaying = true
                            v.mediaTitle = title
                            v.mediaArtist = artist
                            v.mediaArtworkBitmap = art
                        }
                    }
                }
            }
        } else {
            mainHandler.post {
                glanceView?.let { v ->
                    v.isMediaPlaying = isPlaying
                    v.mediaTitle = title
                    v.mediaArtist = artist
                    v.mediaArtworkBitmap = cachedMediaArtwork
                }
            }
        }
    }

    private fun extractMediaArtwork(metadata: MediaMetadata?, controller: MediaController?): Bitmap? {
        var bitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        if (bitmap == null) {
            bitmap = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        }

        if (bitmap == null) {
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            if (!title.isNullOrBlank()) {
                val hashToUse = kotlin.math.abs("${title}_$artist".hashCode().toLong())
                bitmap = NotificationListener.getCachedBitmap(hashToUse)
                if (bitmap == null) {
                    val artFile = File(service.cacheDir, "art_$hashToUse.png")
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

        return bitmap
    }

    private fun getCameraId(): String? {
        if (com.sameerasw.essentials.utils.DeviceUtils.isTorchAccessRestricted()) return null
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                facing == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (e: Exception) {
            null
        }
    }

    fun onScreenOn() {
        isScreenOff = false
        isLocked = keyguardManager?.isKeyguardLocked ?: false
        mainHandler.post {
            glanceView?.isScreenOff = false
            glanceView?.isLocked = isLocked
            updateTime()
            updateBatteryStatus()
            queryUpcomingCalendarEvent()
            reEvaluateAndApplyMedia(isInitial = true)
            syncConfigToView()
        }
    }

    fun onScreenOff() {
        isScreenOff = true
        isLocked = true
        mainHandler.removeCallbacks(secondTick)
        mainHandler.post {
            glanceView?.isScreenOff = true
            glanceView?.isLocked = true
            glanceView?.updateVisibilityState(immediate = true)
            removeTouchAnchor()
        }
    }

    fun onUserPresent() {
        isScreenOff = false
        isLocked = false
        isShadeExpanded = false
        lastUnlockTimestamp = SystemClock.elapsedRealtime()
        mainHandler.post {
            glanceView?.isScreenOff = false
            glanceView?.isLocked = false
            glanceView?.isShadeExpanded = false
            updateTime()
            updateBatteryStatus()
            queryUpcomingCalendarEvent()
            reEvaluateAndApplyMedia(isInitial = true)
            syncConfigToView()
        }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        val isNightMode = (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        glanceView?.isDarkTheme = isNightMode
        isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        applyOrientationVisibility()
        if (settingsRepository.isStatusGlanceEnabled()) {
            mainHandler.postDelayed({
                updateGlancePosition()
                applyOrientationVisibility()
            }, 300)
        }
    }

    private fun removeOverlay(animate: Boolean = true) {
        val fading = glanceView
        val wm = windowManager
        if (isOverlayAdded && fading != null && wm != null) {
            val remove = {
                try {
                    wm.removeView(fading)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            isOverlayAdded = false
            glanceView = null
            if (animate) fading.playExit(remove) else remove()
        }
        removeTouchAnchor()
    }

    fun restart() {
        removeOverlay(animate = false)
        updateState()
    }

    fun destroy() {
        removeOverlay(animate = false)
        touchAnchorView = null
        touchHandler = null
        unregisterTorchCallback()
        unregisterMediaListener()
        unregisterCalendarObserver()
        unregisterTimeReceiver()
        unregisterBatteryReceiver()
    }
}
