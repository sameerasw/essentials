/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Handlers
 * File: DuoOverlayHandler.kt
 * Description: Background handler managing the Duo ambient camera overlay with media playback.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.utils.DuoOverlayView
import com.sameerasw.essentials.utils.OverlayHelper
import java.io.File

class DuoOverlayHandler(
    private val service: AccessibilityService,
) {
    private var windowManager: WindowManager? = null
    private var overlayView: DuoOverlayView? = null
    private var isOverlayAdded = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val settingsRepository by lazy { SettingsRepository(service) }
    private val telephonyManager by lazy { service.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager }
    private val connectivityManager by lazy { service.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    private val wifiManager by lazy { service.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager }

    private var telephonyCallback: Any? = null
    private var isTelephonyRegistered = false
    private var isNetworkCallbackRegistered = false

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeMediaController: MediaController? = null
    private var isMediaListenerRegistered = false
    private var currentArtOrIconBitmap: Bitmap? = null
    private var currentMediaKey: String? = null
    private var isMediaPlaying = false

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            checkAndApplyMediaState()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            checkAndApplyMediaState()
        }

        override fun onSessionDestroyed() {
            checkAndApplyMediaState()
        }
    }

    private val activeSessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateActiveMediaSession(controllers)
        }

    private val mediaProgressTicker = object : Runnable {
        override fun run() {
            if (isMediaPlaying && isOverlayAdded) {
                updateMediaProgress()
                mainHandler.postDelayed(this, 500L)
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateCurrentSignal()
        }
        override fun onLost(network: Network) {
            updateCurrentSignal()
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (level >= 0 && scale > 0) {
                    val batteryPct = (level * 100) / scale
                    overlayView?.batteryLevel = batteryPct
                }
            }
        }
    }
    private var isBatteryReceiverRegistered = false

    private var isScreenOff: Boolean = false
    var isFullscreen: Boolean = false
        private set

    fun setFullscreen(fullscreen: Boolean) {
        if (isFullscreen != fullscreen) {
            isFullscreen = fullscreen
            overlayView?.isFullscreen = fullscreen
        }
    }

    fun init() {
        windowManager = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as? WindowManager
        updateState()
    }

    fun onScreenOn() {
        isScreenOff = false
        overlayView?.isScreenOff = false
        updateState()
    }

    fun onScreenOff() {
        isScreenOff = true
        overlayView?.isScreenOff = true
        updateState()
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        val isNightMode = (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        overlayView?.isDarkTheme = isNightMode
        updateState()
    }

    fun updateState() {
        val isDevMode = settingsRepository.getBoolean(SettingsRepository.KEY_DEVELOPER_MODE_ENABLED, false)
        if (!isDevMode || !settingsRepository.isDuoEnabled()) {
            removeOverlay()
            return
        }
        showOrUpdateOverlay()
    }

    private fun updateCurrentSignal() {
        mainHandler.post {
            var level = -1

            val activeNetwork = connectivityManager?.activeNetwork
            val capabilities = activeNetwork?.let { connectivityManager?.getNetworkCapabilities(it) }
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val transportInfo = capabilities.transportInfo
                    if (transportInfo is WifiInfo) {
                        val rssi = transportInfo.rssi
                        if (rssi > -127) {
                            level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiManager != null) {
                                wifiManager?.calculateSignalLevel(rssi)?.coerceIn(0, 4) ?: 0
                            } else {
                                @Suppress("DEPRECATION")
                                WifiManager.calculateSignalLevel(rssi, 5).coerceIn(0, 4)
                            }
                        }
                    }
                }
                if (level == -1 && wifiManager != null) {
                    @Suppress("DEPRECATION")
                    val info = wifiManager?.connectionInfo
                    if (info != null && info.rssi > -127) {
                        level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            wifiManager?.calculateSignalLevel(info.rssi)?.coerceIn(0, 4) ?: 0
                        } else {
                            @Suppress("DEPRECATION")
                            WifiManager.calculateSignalLevel(info.rssi, 5).coerceIn(0, 4)
                        }
                    }
                }
            }

            if (level == -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val signalStrength = telephonyManager?.signalStrength
                        if (signalStrength != null) {
                            level = signalStrength.level.coerceIn(0, 4)
                        }
                    } catch (_: Exception) {}
                }
            }

            if (level >= 0) {
                overlayView?.signalLevel = level
            }
        }
    }

    private fun updateActiveMediaSession(controllers: List<MediaController>?) {
        mainHandler.post {
            val playingController = controllers?.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            }
            val target = playingController ?: controllers?.firstOrNull()

            if (activeMediaController?.sessionToken != target?.sessionToken) {
                activeMediaController?.unregisterCallback(mediaCallback)
                activeMediaController = target
                target?.registerCallback(mediaCallback, mainHandler)
            }
            checkAndApplyMediaState()
        }
    }

    private fun checkAndApplyMediaState() {
        mainHandler.post {
            val showMedia = settingsRepository.isDuoShowMediaEnabled()
            val controller = activeMediaController

            val excludedAppsJson = settingsRepository.getString(SettingsRepository.KEY_AOD_WALLPAPER_MEDIA_EXCLUDED_APPS, null)
            val excludedPackages: Set<String> = if (!excludedAppsJson.isNullOrBlank()) {
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

            val isExcluded = controller != null && excludedPackages.contains(controller.packageName)
            val state = controller?.playbackState
            val isPlaying = showMedia && !isExcluded && state?.state == PlaybackState.STATE_PLAYING

            isMediaPlaying = isPlaying

            if (!isPlaying) {
                currentMediaKey = null
                currentArtOrIconBitmap = null
                mainHandler.removeCallbacks(mediaProgressTicker)
                overlayView?.setMediaState(isPlaying = false, progress = 0f, appIcon = null)
                return@post
            }

            val title = controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
            val artist = controller.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
            val pkg = controller.packageName
            val mediaKey = "${pkg}_${title}_$artist"

            if (currentMediaKey != mediaKey || currentArtOrIconBitmap == null) {
                currentMediaKey = mediaKey
                currentArtOrIconBitmap = extractMediaArtworkOrAppIcon(controller.metadata, controller)
            }

            updateMediaProgress()

            mainHandler.removeCallbacks(mediaProgressTicker)
            mainHandler.postDelayed(mediaProgressTicker, 500L)
        }
    }

    private fun extractMediaArtworkOrAppIcon(metadata: MediaMetadata?, controller: MediaController): Bitmap? {
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
        if (bitmap == null) {
            val tempArtFile = File(service.cacheDir, "temp_album_art.png")
            if (tempArtFile.exists()) {
                try {
                    bitmap = BitmapFactory.decodeFile(tempArtFile.absolutePath)
                } catch (_: Exception) {}
            }
        }

        if (bitmap == null) {
            bitmap = getAppIconBitmap(service, controller.packageName)
        }

        return bitmap
    }

    private fun updateMediaProgress() {
        val controller = activeMediaController ?: return
        val state = controller.playbackState ?: return
        val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        var currentPos = state.position
        if (state.state == PlaybackState.STATE_PLAYING) {
            val lastUpdate = state.lastPositionUpdateTime
            val now = SystemClock.elapsedRealtime()
            if (lastUpdate > 0 && now > lastUpdate) {
                val speed = if (state.playbackSpeed > 0f) state.playbackSpeed else 1f
                currentPos += ((now - lastUpdate) * speed).toLong()
            }
        }

        val progressPct = if (duration > 0) {
            ((currentPos.toFloat() / duration.toFloat()) * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }

        overlayView?.setMediaState(
            isPlaying = true,
            progress = progressPct,
            appIcon = currentArtOrIconBitmap
        )
    }

    private fun getAppIconBitmap(context: Context, packageName: String): Bitmap? {
        return try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val density = context.resources.displayMetrics.density
            val sizePx = (24 * density).toInt().coerceAtLeast(24)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun registerMediaSessionListener() {
        if (!isMediaListenerRegistered) {
            try {
                val msm = service.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                mediaSessionManager = msm
                if (msm != null) {
                    val componentName = ComponentName(service, NotificationListener::class.java)
                    msm.addOnActiveSessionsChangedListener(activeSessionsListener, componentName)
                    isMediaListenerRegistered = true
                    val initialSessions = msm.getActiveSessions(componentName)
                    updateActiveMediaSession(initialSessions)
                }
            } catch (e: Exception) {
                Log.e("DuoOverlayHandler", "Failed to register media session listener", e)
            }
        }
    }

    private fun unregisterMediaSessionListener() {
        mainHandler.removeCallbacks(mediaProgressTicker)
        if (isMediaListenerRegistered) {
            try {
                mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
                activeMediaController?.unregisterCallback(mediaCallback)
            } catch (_: Exception) {}
            activeMediaController = null
            isMediaListenerRegistered = false
            isMediaPlaying = false
            overlayView?.setMediaState(isPlaying = false, progress = 0f, appIcon = null)
        }
    }

    private fun showOrUpdateOverlay() {
        mainHandler.post {
            val wm = windowManager ?: return@post

            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels.toFloat()
            val screenHeight = displayMetrics.heightPixels.toFloat()
            val density = displayMetrics.density

            var centerX = screenWidth * (settingsRepository.getDuoCameraOffsetX() / 100f)
            var centerY = screenHeight * (settingsRepository.getDuoCameraOffsetY() / 100f)
            var cameraRadiusPx = 18f * density * settingsRepository.getDuoCameraSize()

            @Suppress("DEPRECATION")
            val rotation = wm.defaultDisplay.rotation

            if (settingsRepository.isDuoAutoDetectEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                val cutout = wm.defaultDisplay.cutout
                if (cutout != null && cutout.boundingRects.isNotEmpty()) {
                    val targetRect = when (rotation) {
                        android.view.Surface.ROTATION_90 -> {
                            cutout.boundingRects.filter { it.left < screenWidth / 4 }
                                .minByOrNull { kotlin.math.abs(it.centerY() - screenHeight / 2f) }
                                ?: cutout.boundingRects.firstOrNull()
                        }
                        android.view.Surface.ROTATION_270 -> {
                            cutout.boundingRects.filter { it.right > screenWidth * 0.75f }
                                .minByOrNull { kotlin.math.abs(it.centerY() - screenHeight / 2f) }
                                ?: cutout.boundingRects.firstOrNull()
                        }
                        android.view.Surface.ROTATION_180 -> {
                            cutout.boundingRects.filter { it.bottom > screenHeight * 0.75f }
                                .minByOrNull { kotlin.math.abs(it.centerX() - screenWidth / 2f) }
                                ?: cutout.boundingRects.firstOrNull()
                        }
                        else -> {
                            cutout.boundingRects.filter { it.top < screenHeight / 4 }
                                .minByOrNull { kotlin.math.abs(it.centerX() - screenWidth / 2f) }
                                ?: cutout.boundingRects.firstOrNull()
                        }
                    }

                    if (targetRect != null) {
                        centerX = targetRect.centerX().toFloat()
                        centerY = targetRect.centerY().toFloat()
                        val computedRadius = (targetRect.width().coerceAtLeast(targetRect.height()) / 2f) * settingsRepository.getDuoCameraSize()
                        if (computedRadius > 0) {
                            cameraRadiusPx = computedRadius
                        }
                    }
                }
            } else if (!settingsRepository.isDuoAutoDetectEnabled()) {
                val rawXRatio = settingsRepository.getDuoCameraOffsetX() / 100f
                val rawYRatio = settingsRepository.getDuoCameraOffsetY() / 100f
                when (rotation) {
                    android.view.Surface.ROTATION_90 -> {
                        centerX = screenWidth * rawYRatio
                        centerY = screenHeight * (1f - rawXRatio)
                    }
                    android.view.Surface.ROTATION_270 -> {
                        centerX = screenWidth * (1f - rawYRatio)
                        centerY = screenHeight * rawXRatio
                    }
                    android.view.Surface.ROTATION_180 -> {
                        centerX = screenWidth * (1f - rawXRatio)
                        centerY = screenHeight * (1f - rawYRatio)
                    }
                    else -> {
                        centerX = screenWidth * rawXRatio
                        centerY = screenHeight * rawYRatio
                    }
                }
            }

            val isNightMode = (service.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            if (overlayView == null) {
                overlayView = DuoOverlayView(service)
            }

            overlayView?.apply {
                this.cameraCenterX = centerX
                this.cameraCenterY = centerY
                this.cameraRadiusPx = cameraRadiusPx
                this.ringRadiusScale = settingsRepository.getDuoRingRadius()
                this.arcThicknessPx = settingsRepository.getDuoArcThickness() * density
                this.dotRadiusPx = settingsRepository.getDuoDotSize() * density
                this.isDarkTheme = isNightMode
                this.isScreenOff = this@DuoOverlayHandler.isScreenOff
                this.isFullscreen = this@DuoOverlayHandler.isFullscreen
                this.hideWhenScreenOff = settingsRepository.isDuoHideWhenScreenOffEnabled()
                this.useMaterialYouColors = settingsRepository.isDuoUseMaterialYouEnabled()
                this.showNetworks = settingsRepository.isDuoShowNetworksEnabled()
                this.showMedia = settingsRepository.isDuoShowMediaEnabled()
            }

            if (!isOverlayAdded) {
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
                    Log.e("DuoOverlayHandler", "Failed to add Duo overlay", e)
                }
            } else {
                overlayView?.invalidate()
            }

            registerBatteryReceiver()
            registerSignalListeners()
            updateCurrentSignal()

            if (settingsRepository.isDuoShowMediaEnabled()) {
                registerMediaSessionListener()
            } else {
                unregisterMediaSessionListener()
            }
        }
    }

    private fun registerBatteryReceiver() {
        if (!isBatteryReceiverRegistered) {
            try {
                val intent = service.registerReceiver(
                    batteryReceiver,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                isBatteryReceiverRegistered = true
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    if (level >= 0 && scale > 0) {
                        overlayView?.batteryLevel = (level * 100) / scale
                    }
                }
            } catch (e: Exception) {
                Log.e("DuoOverlayHandler", "Failed to register battery receiver", e)
            }
        }
    }

    private fun unregisterBatteryReceiver() {
        if (isBatteryReceiverRegistered) {
            try {
                service.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
            isBatteryReceiverRegistered = false
        }
    }

    private fun registerSignalListeners() {
        if (!isNetworkCallbackRegistered) {
            try {
                connectivityManager?.registerDefaultNetworkCallback(networkCallback)
                isNetworkCallbackRegistered = true
            } catch (e: Exception) {
                Log.e("DuoOverlayHandler", "Failed to register network callback", e)
            }
        }

        if (!isTelephonyRegistered && telephonyManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                            val lvl = signalStrength.level.coerceIn(0, 4)
                            overlayView?.signalLevel = lvl
                        }
                    }
                    telephonyManager?.registerTelephonyCallback(service.mainExecutor, callback)
                    telephonyCallback = callback
                    isTelephonyRegistered = true
                } else {
                    @Suppress("DEPRECATION")
                    val listener = object : PhoneStateListener() {
                        @Deprecated("Deprecated in Java")
                        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                            if (signalStrength != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val lvl = signalStrength.level.coerceIn(0, 4)
                                overlayView?.signalLevel = lvl
                            }
                        }
                    }
                    @Suppress("DEPRECATION")
                    telephonyManager?.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                    telephonyCallback = listener
                    isTelephonyRegistered = true
                }
            } catch (e: Exception) {
                Log.e("DuoOverlayHandler", "Failed to register telephony listener", e)
            }
        }
    }

    private fun unregisterSignalListeners() {
        if (isNetworkCallbackRegistered) {
            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            } catch (_: Exception) {}
            isNetworkCallbackRegistered = false
        }

        if (isTelephonyRegistered && telephonyCallback != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val callback = telephonyCallback as? TelephonyCallback
                    if (callback != null) {
                        telephonyManager?.unregisterTelephonyCallback(callback)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val listener = telephonyCallback as? PhoneStateListener
                    if (listener != null) {
                        @Suppress("DEPRECATION")
                        telephonyManager?.listen(listener, PhoneStateListener.LISTEN_NONE)
                    }
                }
            } catch (_: Exception) {}
            telephonyCallback = null
            isTelephonyRegistered = false
        }
    }

    fun removeOverlay() {
        mainHandler.post {
            if (isOverlayAdded && overlayView != null) {
                try {
                    windowManager?.removeView(overlayView)
                } catch (_: Exception) {}
                isOverlayAdded = false
            }
            unregisterBatteryReceiver()
            unregisterSignalListeners()
            unregisterMediaSessionListener()
        }
    }

    fun destroy() {
        removeOverlay()
        overlayView = null
        currentArtOrIconBitmap = null
        currentMediaKey = null
    }
}

