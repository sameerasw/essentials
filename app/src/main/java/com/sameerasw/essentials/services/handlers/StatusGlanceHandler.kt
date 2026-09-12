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
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
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
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
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
    private val mainHandler = Handler(Looper.getMainLooper())

    private val settingsRepository by lazy { SettingsRepository(service) }
    private val keyguardManager by lazy { service.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager }
    private val cameraManager by lazy { service.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val mediaSessionManager by lazy { service.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager }

    private var isScreenOff = false
    private var isLocked = false
    private var isShadeExpanded = false
    private var isFullscreen = false
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
    private var calendarObserver: ContentObserver? = null

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
            } else {
                removeOverlay()
                unregisterTorchCallback()
                unregisterMediaListener()
                unregisterCalendarObserver()
                unregisterTimeReceiver()
                unregisterBatteryReceiver()
            }
        }
    }

    fun setFullscreen(fullscreen: Boolean) {
        if (isFullscreen != fullscreen) {
            isFullscreen = fullscreen
            mainHandler.post {
                glanceView?.isFullscreen = fullscreen
            }
        }
    }

    fun setShadeExpanded(expanded: Boolean) {
        if (isScreenOff || isLocked) {
            if (isShadeExpanded) {
                isShadeExpanded = false
                mainHandler.post {
                    glanceView?.isShadeExpanded = false
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
            }
        }
    }

    private fun syncConfigToView() {
        glanceView?.let { v ->
            val isNightMode = (service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            v.isDarkTheme = isNightMode
            v.showFlashlight = settingsRepository.isStatusGlanceShowFlashlightEnabled()
            v.showCalendar = settingsRepository.isStatusGlanceShowCalendarEnabled()
            v.showMedia = settingsRepository.isStatusGlanceShowMediaEnabled()
            v.showTime = settingsRepository.isStatusGlanceShowTimeEnabled()
            v.useBackgroundPill = settingsRepository.isStatusGlanceBackgroundPillEnabled()
            v.hideWhenFullscreen = settingsRepository.isStatusGlanceHideWhenFullscreenEnabled()
            v.hideInQuickSettings = settingsRepository.isStatusGlanceHideInQuickSettingsEnabled()
            v.maxWidthDp = settingsRepository.getStatusGlanceMaxWidth()
            v.fontSize = settingsRepository.getStatusGlanceFontSize()
            v.isFlashlightOn = isFlashlightOn

            v.showBattery = settingsRepository.isStatusGlanceShowBatteryEnabled()
            v.batteryDisplayMode = settingsRepository.getStatusGlanceBatteryDisplayMode()
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

            v.isFullscreen = isFullscreen
            v.isShadeExpanded = isShadeExpanded
            v.isLocked = isLocked
            v.isScreenOff = isScreenOff

            updateGlancePosition()
            v.reevaluateSlot()
        }
    }

    private fun createOverlay() {
        if (isOverlayAdded || windowManager == null) return

        glanceView = StatusGlanceView(service)
        val params = getOverlayLayoutParams()

        try {
            windowManager?.addView(glanceView, params)
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

    private fun getOverlayLayoutParams(): WindowManager.LayoutParams {
        val type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

        val flags = (
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
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
    }

    private fun updateTime() {
        val is24Hour = DateFormat.is24HourFormat(service)
        val pattern = if (is24Hour) "HH:mm" else "h:mm"
        val timeFormat = SimpleDateFormat(pattern, Locale.getDefault())
        val formattedTime = timeFormat.format(Date())
        cachedTimeString = formattedTime
        glanceView?.currentTimeString = formattedTime
    }

    private fun updateBatteryStatus(intent: Intent? = null) {
        val batteryIntent = intent ?: service.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100f).toInt() else 100

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
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
        if (ContextCompat.checkSelfPermission(service, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            cachedEventTitle = ""
            cachedIsEventToday = false
            glanceView?.nextEventTitle = ""
            glanceView?.isEventToday = false
            return
        }

        handlerScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val timeframe = settingsRepository.getStatusGlanceCalendarTimeframe()
                val selectedCalIds = settingsRepository.getStatusGlanceCalendarSelectedCalendars()
                    .mapNotNull { it.toLongOrNull() }.toSet()

                val maxTimeMillis: Long = when (timeframe) {
                    "15m" -> now + (15 * 60 * 1000L)
                    "30m" -> now + (30 * 60 * 1000L)
                    "1h" -> now + (60 * 60 * 1000L)
                    "2h" -> now + (2 * 60 * 60 * 1000L)
                    "6h" -> now + (6 * 60 * 60 * 1000L)
                    "24h" -> now + (24 * 60 * 60 * 1000L)
                    "today" -> {
                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        calendar.timeInMillis
                    }
                    "next" -> now + (30 * 24 * 60 * 60 * 1000L)
                    else -> {
                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        calendar.timeInMillis
                    }
                }

                val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                android.content.ContentUris.appendId(builder, now)
                android.content.ContentUris.appendId(builder, maxTimeMillis)

                val projection = arrayOf(
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.ALL_DAY,
                    CalendarContract.Instances.SELF_ATTENDEE_STATUS,
                    CalendarContract.Instances.CALENDAR_ID
                )

                val cursor = service.contentResolver.query(
                    builder.build(),
                    projection,
                    null,
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC"
                )

                var foundTitle: String? = null
                var foundBegin: Long = 0L

                cursor?.use {
                    val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
                    val beginIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                    val statusIndex = it.getColumnIndex(CalendarContract.Instances.SELF_ATTENDEE_STATUS)
                    val calIdIndex = it.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)

                    while (it.moveToNext()) {
                        val calId = it.getLong(calIdIndex)
                        if (selectedCalIds.isNotEmpty() && !selectedCalIds.contains(calId)) {
                            continue
                        }

                        if (statusIndex != -1 && it.getInt(statusIndex) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) {
                            continue
                        }

                        val rawTitle = it.getString(titleIndex)
                        if (rawTitle.isNullOrBlank()) {
                            continue
                        }

                        val begin = it.getLong(beginIndex)
                        if (begin >= now && begin <= maxTimeMillis) {
                            foundTitle = rawTitle.trim()
                            foundBegin = begin
                            break
                        }
                    }
                }

                if (foundTitle != null) {
                    val inTimeStr = formatRelativeEventTime(foundBegin, now)
                    val fullTitle = "$foundTitle $inTimeStr"
                    cachedEventTitle = fullTitle
                    cachedEventTimeMillis = foundBegin
                    cachedIsEventToday = true
                    withContext(Dispatchers.Main) {
                        glanceView?.nextEventTitle = fullTitle
                        glanceView?.nextEventTimeMillis = foundBegin
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
            } catch (e: Exception) {
                cachedEventTitle = ""
                cachedIsEventToday = false
                withContext(Dispatchers.Main) {
                    glanceView?.nextEventTitle = ""
                    glanceView?.isEventToday = false
                }
            }
        }
    }

    private fun registerCalendarObserver() {
        if (calendarObserver != null) return
        try {
            calendarObserver = object : ContentObserver(mainHandler) {
                override fun onChange(selfChange: Boolean) {
                    queryUpcomingCalendarEvent()
                }
            }
            service.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                calendarObserver!!
            )
            service.contentResolver.registerContentObserver(
                CalendarContract.Instances.CONTENT_URI,
                true,
                calendarObserver!!
            )
        } catch (_: Exception) {}
    }

    private fun unregisterCalendarObserver() {
        calendarObserver?.let {
            try {
                service.contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
        }
        calendarObserver = null
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
        mainHandler.post {
            glanceView?.isScreenOff = true
            glanceView?.isLocked = true
            glanceView?.updateVisibilityState(immediate = true)
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
        if (settingsRepository.isStatusGlanceEnabled()) {
            mainHandler.postDelayed({
                updateGlancePosition()
            }, 300)
        }
    }

    private fun removeOverlay() {
        if (isOverlayAdded && glanceView != null && windowManager != null) {
            try {
                windowManager?.removeView(glanceView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isOverlayAdded = false
            glanceView = null
        }
    }

    fun destroy() {
        removeOverlay()
        unregisterTorchCallback()
        unregisterMediaListener()
        unregisterCalendarObserver()
        unregisterTimeReceiver()
        unregisterBatteryReceiver()
    }
}
