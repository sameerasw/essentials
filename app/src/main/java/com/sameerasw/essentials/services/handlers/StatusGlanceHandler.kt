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

                if (!isOverlayAdded) {
                    createOverlay()
                } else {
                    updateOverlayParams()
                }

                updateTime()
                queryUpcomingCalendarEvent()
                reEvaluateAndApplyMedia(isInitial = true)
                syncConfigToView()
            } else {
                removeOverlay()
                unregisterTorchCallback()
                unregisterMediaListener()
                unregisterCalendarObserver()
                unregisterTimeReceiver()
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
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val endOfDay = calendar.timeInMillis

                val projection = arrayOf(
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.ALL_DAY
                )

                val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?) AND (${CalendarContract.Events.DELETED} = 0)"
                val selectionArgs = arrayOf(now.toString(), endOfDay.toString())
                val sortOrder = "${CalendarContract.Events.DTSTART} ASC LIMIT 1"

                val cursor = service.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use {
                    if (it.moveToFirst()) {
                        val title = it.getString(0) ?: ""
                        val dtStart = it.getLong(1)
                        cachedEventTitle = title
                        cachedEventTimeMillis = dtStart
                        cachedIsEventToday = true
                        withContext(Dispatchers.Main) {
                            glanceView?.nextEventTitle = title
                            glanceView?.nextEventTimeMillis = dtStart
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
    }
}
