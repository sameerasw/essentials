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
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.utils.StatusGlanceView
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
    private val cameraManager by lazy { service.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val mediaSessionManager by lazy { service.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager }

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

    private var activeMediaController: MediaController? = null
    private var isMediaSessionRegistered = false

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaState(state, activeMediaController?.metadata)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaState(activeMediaController?.playbackState, metadata)
        }

        override fun onSessionDestroyed() {
            activeMediaController = null
            findActiveMediaSession()
        }
    }

    private val activeSessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            val excludedPackages = getExcludedPackages()
            val valid = controllers?.filter { !excludedPackages.contains(it.packageName) }
            val active = valid?.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: valid?.firstOrNull()

            if (active?.sessionToken != activeMediaController?.sessionToken) {
                activeMediaController?.unregisterCallback(mediaCallback)
                activeMediaController = active
                active?.registerCallback(mediaCallback, mainHandler)
                updateMediaState(active?.playbackState, active?.metadata)
            }
        }

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateTime()
            queryUpcomingCalendarEvent()
        }
    }
    private var isTimeReceiverRegistered = false

    fun init() {
        windowManager = service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
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
                syncConfigToView()
                queryUpcomingCalendarEvent()
            } else {
                removeOverlay()
                unregisterTorchCallback()
                unregisterMediaListener()
                unregisterCalendarObserver()
                unregisterTimeReceiver()
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
            v.maxWidthDp = settingsRepository.getStatusGlanceMaxWidth()
            v.fontSize = settingsRepository.getStatusGlanceFontSize()
            v.isFlashlightOn = isFlashlightOn
            updateGlancePosition()
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

    private fun getCutoutBounds(): Rect? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager?.currentWindowMetrics
            val cutout = windowMetrics?.windowInsets?.displayCutout
            val rects = cutout?.boundingRects
            if (!rects.isNullOrEmpty()) {
                return rects[0]
            }
        }
        return null
    }

    private fun getStatusBarHeight(): Float {
        val resourceId = service.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            service.resources.getDimensionPixelSize(resourceId).toFloat()
        } else {
            24f * service.resources.displayMetrics.density
        }
    }

    private fun updateTime() {
        val is24Hour = DateFormat.is24HourFormat(service)
        val pattern = if (is24Hour) "HH:mm" else "h:mm"
        val timeFormat = SimpleDateFormat(pattern, Locale.getDefault())
        val formattedTime = timeFormat.format(Date())
        glanceView?.currentTimeString = formattedTime
    }

    private fun queryUpcomingCalendarEvent() {
        if (ContextCompat.checkSelfPermission(service, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
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
                        withContext(Dispatchers.Main) {
                            glanceView?.nextEventTitle = title
                            glanceView?.nextEventTimeMillis = dtStart
                            glanceView?.isEventToday = true
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            glanceView?.nextEventTitle = ""
                            glanceView?.isEventToday = false
                        }
                    }
                }
            } catch (e: Exception) {
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

    private fun registerMediaListener() {
        if (isMediaSessionRegistered) return
        try {
            val componentName = ComponentName(service, NotificationListener::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(
                activeSessionsListener,
                componentName,
                mainHandler
            )
            isMediaSessionRegistered = true
            findActiveMediaSession()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unregisterMediaListener() {
        if (!isMediaSessionRegistered) return
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
            activeMediaController?.unregisterCallback(mediaCallback)
            activeMediaController = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isMediaSessionRegistered = false
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

    private fun findActiveMediaSession() {
        try {
            val componentName = ComponentName(service, NotificationListener::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            val excludedPackages = getExcludedPackages()
            val valid = controllers?.filter { !excludedPackages.contains(it.packageName) }
            val active = valid?.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: valid?.firstOrNull()

            activeMediaController?.unregisterCallback(mediaCallback)
            activeMediaController = active
            active?.registerCallback(mediaCallback, mainHandler)
            updateMediaState(active?.playbackState, active?.metadata)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateMediaState(playbackState: PlaybackState?, metadata: MediaMetadata?) {
        val excludedPackages = getExcludedPackages()
        val isExcluded = activeMediaController != null && excludedPackages.contains(activeMediaController?.packageName)
        val isPlaying = !isExcluded && playbackState?.state == PlaybackState.STATE_PLAYING
        val title = if (!isExcluded) metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "" else ""
        val artist = if (!isExcluded) metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "" else ""
        val artwork = if (!isExcluded) {
            metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        } else null

        mainHandler.post {
            glanceView?.let { v ->
                v.isMediaPlaying = isPlaying
                v.mediaTitle = title
                v.mediaArtist = artist
                v.mediaArtworkBitmap = artwork
            }
        }
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
        if (settingsRepository.isStatusGlanceEnabled()) {
            updateTime()
            queryUpcomingCalendarEvent()
            updateGlancePosition()
        }
    }

    fun onScreenOff() {
        // Overlay can remain or suspend updates
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
