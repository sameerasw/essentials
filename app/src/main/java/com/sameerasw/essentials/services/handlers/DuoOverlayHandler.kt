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
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
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
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.ProgressNotificationData
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.DuoOverlayView
import com.sameerasw.essentials.utils.FlashlightUtil
import com.sameerasw.essentials.utils.OverlayHelper
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DuoOverlayHandler(
    private val service: AccessibilityService,
) {
    private var windowManager: WindowManager? = null
    private var overlayView: DuoOverlayView? = null
    private var isOverlayAdded = false
    private var touchAnchorView: View? = null
    private var isTouchAnchorAdded = false
    private var duoTouchHandler: DuoTouchHandler? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val settingsRepository by lazy { SettingsRepository(service) }
    private val telephonyManager by lazy { service.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager }
    private val connectivityManager by lazy { service.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    private val wifiManager by lazy { service.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager }
    private val cameraManager by lazy { service.getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    private var isFlashlightOn = false
    private var currentFlashlightLevel = 1
    private var flashlightIconBitmap: Bitmap? = null
    private var isTorchCallbackRegistered = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            val primaryId = getCameraId()
            if (cameraId != primaryId) return
            mainHandler.post {
                isFlashlightOn = enabled
                checkAndApplyFlashlightState()
            }
        }

        override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
            val primaryId = getCameraId()
            if (cameraId != primaryId) return
            mainHandler.post {
                currentFlashlightLevel = newStrengthLevel
                checkAndApplyFlashlightState()
            }
        }
    }

    private var telephonyCallback: Any? = null
    private var isTelephonyRegistered = false
    private var isNetworkCallbackRegistered = false

    private var mediaSessionManager: MediaSessionManager? = null
    private val monitoredControllers = mutableListOf<MediaController>()
    private val controllerCallbacks = mutableMapOf<MediaSession.Token, MediaController.Callback>()
    private var activeMediaController: MediaController? = null
    private var isMediaListenerRegistered = false
    private var currentArtOrIconBitmap: Bitmap? = null
    private var currentMediaKey: String? = null
    private var isMediaPlaying = false
    private var isProgressListenerRegistered = false

    private val handlerScope = CoroutineScope(Dispatchers.Main + Job())
    private var lastMediaChangeTimestamp = 0L
    private val MEDIA_UPDATE_DEBOUNCE_MS = 2000L

    private val mediaUpdateRunnable = Runnable {
        checkAndApplyMediaState()
    }

    private fun scheduleMediaUpdate() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastMediaChangeTimestamp < MEDIA_UPDATE_DEBOUNCE_MS) {
            return
        }
        lastMediaChangeTimestamp = now
        mainHandler.removeCallbacks(mediaUpdateRunnable)
        mainHandler.postDelayed(mediaUpdateRunnable, MEDIA_UPDATE_DEBOUNCE_MS)
    }

    private val progressNotificationListener = object : NotificationListener.ProgressNotificationListener {
        override fun onProgressNotificationUpdated(data: ProgressNotificationData?) {
            mainHandler.post {
                checkAndApplyProgressNotificationState(data)
            }
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

    private var isChargingState = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == Intent.ACTION_BATTERY_CHANGED ||
                action == Intent.ACTION_POWER_CONNECTED ||
                action == Intent.ACTION_POWER_DISCONNECTED
            ) {
                val batteryIntent = if (action == Intent.ACTION_BATTERY_CHANGED) {
                    intent
                } else {
                    service.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                }

                val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                if (level >= 0 && scale > 0) {
                    val batteryPct = (level * 100) / scale
                    overlayView?.batteryLevel = batteryPct
                }

                val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isChargingStatus = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

                val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
                val isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                    plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                    plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS

                val isNowCharging = (isChargingStatus && isPlugged) || action == Intent.ACTION_POWER_CONNECTED

                if (action == Intent.ACTION_POWER_CONNECTED) {
                    val isFastCharging = plugged == BatteryManager.BATTERY_PLUGGED_AC
                    isChargingState = true
                    overlayView?.triggerChargingAnimation(isFastCharging)
                } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
                    isChargingState = false
                    overlayView?.setCharging(false)
                } else if (isNowCharging != isChargingState) {
                    val isFastCharging = plugged == BatteryManager.BATTERY_PLUGGED_AC
                    isChargingState = isNowCharging
                    if (isNowCharging) {
                        overlayView?.triggerChargingAnimation(isFastCharging)
                    } else {
                        overlayView?.setCharging(false)
                    }
                } else if (isNowCharging) {
                    val isFastCharging = plugged == BatteryManager.BATTERY_PLUGGED_AC
                    overlayView?.setCharging(true, isFastCharging)
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
        if (!settingsRepository.isDuoEnabled()) {
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

            if (isInitial) {
                checkAndApplyMediaState()
            } else {
                scheduleMediaUpdate()
            }
        }
    }

    private fun updateActiveMediaSession(controllers: List<MediaController>?, isInitial: Boolean = false) {
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

    private fun checkAndApplyMediaState() {
        mainHandler.post {
            val showMedia = settingsRepository.isDuoShowMediaEnabled()
            val controller = activeMediaController
            val excludedPackages = getExcludedPackages()

            val isExcluded = controller != null && excludedPackages.contains(controller.packageName)
            val state = controller?.playbackState
            val isPlaying = showMedia && !isExcluded && state?.state == PlaybackState.STATE_PLAYING

            isMediaPlaying = isPlaying

            if (!isPlaying) {
                currentMediaKey = null
                currentArtOrIconBitmap = null
                mainHandler.removeCallbacks(mediaProgressTicker)
                overlayView?.setMediaState(isPlaying = false, progress = 0f, appIcon = null)
                checkAndApplyProgressNotificationState()
                return@post
            }

            val title = controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
            val artist = controller.metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
            val pkg = controller.packageName
            val mediaKey = "${pkg}_${title}_$artist"

            if (currentMediaKey != mediaKey || currentArtOrIconBitmap == null) {
                currentMediaKey = mediaKey
                handlerScope.launch(Dispatchers.IO) {
                    val artOrIcon = extractMediaArtworkOrAppIcon(controller.metadata, controller)
                    withContext(Dispatchers.Main) {
                        if (currentMediaKey == mediaKey && isMediaPlaying) {
                            currentArtOrIconBitmap = artOrIcon
                            updateMediaProgress()
                            checkAndApplyProgressNotificationState()
                        }
                    }
                }
            }

            updateMediaProgress()
            checkAndApplyProgressNotificationState()

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
                    updateActiveMediaSession(initialSessions, isInitial = true)
                }
            } catch (e: Exception) {
                Log.e("DuoOverlayHandler", "Failed to register media session listener", e)
            }
        }
    }

    private fun unregisterMediaSessionListener() {
        mainHandler.removeCallbacks(mediaProgressTicker)
        mainHandler.removeCallbacks(mediaUpdateRunnable)
        if (isMediaListenerRegistered) {
            try {
                mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
            } catch (_: Exception) {}
            for (controller in monitoredControllers) {
                val callback = controllerCallbacks[controller.sessionToken]
                if (callback != null) {
                    try {
                        controller.unregisterCallback(callback)
                    } catch (_: Exception) {}
                }
            }
            controllerCallbacks.clear()
            monitoredControllers.clear()
            activeMediaController = null
            isMediaListenerRegistered = false
            isMediaPlaying = false
            currentMediaKey = null
            currentArtOrIconBitmap = null
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
                this.hideWhenScreenOffOnlyIdle = settingsRepository.isDuoHideWhenScreenOffOnlyIdleEnabled()
                this.useMaterialYouColors = settingsRepository.isDuoUseMaterialYouEnabled()
                this.showNetworks = settingsRepository.isDuoShowNetworksEnabled()
                this.showMedia = settingsRepository.isDuoShowMediaEnabled()
                this.showProgress = settingsRepository.isDuoShowProgressEnabled()
                this.showFlashlight = settingsRepository.isDuoShowFlashlightEnabled()
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

            val isTouchEnabled = settingsRepository.getDuoTapAction() != null ||
                settingsRepository.getDuoDoubleTapAction() != null ||
                settingsRepository.getDuoLongPressAction() != null ||
                settingsRepository.getDuoSwipeDownAction() != null ||
                settingsRepository.getDuoSlideMode() != "none"

            if (isTouchEnabled) {
                if (duoTouchHandler == null) {
                    duoTouchHandler = DuoTouchHandler(service)
                }
                duoTouchHandler?.apply {
                    this.cameraCenterX = centerX
                    this.cameraCenterY = centerY
                    this.cameraRadiusPx = cameraRadiusPx
                    this.ringRadiusScale = settingsRepository.getDuoRingRadius()
                }

                if (touchAnchorView == null) {
                    touchAnchorView = View(service).apply {
                        setOnTouchListener { _, event ->
                            duoTouchHandler?.onTouchEvent(event) ?: false
                        }
                    }
                }

                val diameter = ((cameraRadiusPx + 20f * density) * 2 * settingsRepository.getDuoRingRadius()).toInt()
                val touchParams = WindowManager.LayoutParams(
                    diameter,
                    diameter,
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
                    x = (centerX - diameter / 2f).toInt()
                    y = (centerY - diameter / 2f).toInt()
                }

                if (!isTouchAnchorAdded) {
                    try {
                        wm.addView(touchAnchorView, touchParams)
                        isTouchAnchorAdded = true
                    } catch (e: Exception) {
                        Log.e("DuoOverlayHandler", "Failed to add Duo touch anchor", e)
                    }
                } else {
                    try {
                        wm.updateViewLayout(touchAnchorView, touchParams)
                    } catch (e: Exception) {
                        Log.e("DuoOverlayHandler", "Failed to update Duo touch anchor", e)
                    }
                }
            } else {
                if (isTouchAnchorAdded && touchAnchorView != null) {
                    try {
                        wm.removeView(touchAnchorView)
                    } catch (_: Exception) {}
                    isTouchAnchorAdded = false
                }
            }

            registerBatteryReceiver()
            registerSignalListeners()
            updateCurrentSignal()

            if (settingsRepository.isDuoShowMediaEnabled()) {
                registerMediaSessionListener()
            } else {
                unregisterMediaSessionListener()
            }

            if (settingsRepository.isDuoShowProgressEnabled()) {
                registerProgressNotificationListener()
            } else {
                unregisterProgressNotificationListener()
            }

            if (settingsRepository.isDuoShowFlashlightEnabled()) {
                registerTorchCallback()
            } else {
                unregisterTorchCallback()
            }
        }
    }

    private var primaryCameraId: String? = null
    private var maxFlashlightLevel: Int = -1

    private fun getCameraId(): String? {
        if (primaryCameraId != null) return primaryCameraId
        return try {
            val id = cameraManager.cameraIdList.firstOrNull { camId ->
                val chars = cameraManager.getCameraCharacteristics(camId)
                val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                flashAvailable && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull()
            primaryCameraId = id
            if (id != null) {
                maxFlashlightLevel = FlashlightUtil.getMaxLevel(service, id)
            }
            id
        } catch (_: Exception) {
            null
        }
    }

    private fun getFlashlightIcon(): Bitmap? {
        if (flashlightIconBitmap == null) {
            try {
                val drawable = ContextCompat.getDrawable(service, R.drawable.rounded_flashlight_on_24)?.mutate()
                drawable?.setTint(Color.WHITE)
                if (drawable != null) {
                    flashlightIconBitmap = AppUtil.drawableToBitmap(drawable, 64)
                }
            } catch (_: Exception) {}
        }
        return flashlightIconBitmap
    }

    private fun checkAndApplyFlashlightState() {
        mainHandler.post {
            val showFlashlight = settingsRepository.isDuoShowFlashlightEnabled()
            val isOn = isFlashlightOn && showFlashlight
            if (isOn) {
                if (primaryCameraId == null) {
                    getCameraId()
                }
                val maxLevel = if (maxFlashlightLevel > 1) maxFlashlightLevel else 1
                val progress = if (maxLevel > 1) {
                    (currentFlashlightLevel.toFloat() / maxLevel.toFloat() * 100f).coerceIn(0f, 100f)
                } else {
                    100f
                }
                overlayView?.setFlashlightState(
                    isOn = true,
                    brightnessProgress = progress,
                    icon = getFlashlightIcon()
                )
            } else {
                overlayView?.setFlashlightState(
                    isOn = false,
                    brightnessProgress = 0f,
                    icon = null
                )
            }
        }
    }

    private fun registerTorchCallback() {
        if (!isTorchCallbackRegistered) {
            try {
                cameraManager.registerTorchCallback(torchCallback, mainHandler)
                isTorchCallbackRegistered = true
                checkAndApplyFlashlightState()
            } catch (e: Exception) {
                Log.e("DuoOverlayHandler", "Failed to register torch callback", e)
            }
        }
    }

    private fun unregisterTorchCallback() {
        if (isTorchCallbackRegistered) {
            try {
                cameraManager.unregisterTorchCallback(torchCallback)
            } catch (_: Exception) {}
            isTorchCallbackRegistered = false
            checkAndApplyFlashlightState()
        }
    }

    private fun checkAndApplyProgressNotificationState(incomingData: ProgressNotificationData? = null) {
        mainHandler.post {
            val showProgress = settingsRepository.isDuoShowProgressEnabled()
            val data = incomingData ?: if (showProgress) NotificationListener.getLatestProgressNotification() else null
            val isActive = showProgress && data != null && !isMediaPlaying

            if (data != null && isActive) {
                overlayView?.setProgressNotificationState(
                    isActive = true,
                    progress = data.progress,
                    icon = data.icon
                )
            } else {
                overlayView?.setProgressNotificationState(
                    isActive = false,
                    progress = 0f,
                    icon = null
                )
            }
        }
    }

    private fun registerProgressNotificationListener() {
        if (!isProgressListenerRegistered) {
            NotificationListener.addProgressNotificationListener(progressNotificationListener)
            isProgressListenerRegistered = true
            checkAndApplyProgressNotificationState()
        }
    }

    private fun unregisterProgressNotificationListener() {
        if (isProgressListenerRegistered) {
            NotificationListener.removeProgressNotificationListener(progressNotificationListener)
            isProgressListenerRegistered = false
            checkAndApplyProgressNotificationState(null)
        }
    }

    private fun registerBatteryReceiver() {
        if (!isBatteryReceiverRegistered) {
            try {
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_BATTERY_CHANGED)
                    addAction(Intent.ACTION_POWER_CONNECTED)
                    addAction(Intent.ACTION_POWER_DISCONNECTED)
                }
                val intent = service.registerReceiver(
                    batteryReceiver,
                    filter
                )
                isBatteryReceiverRegistered = true
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    if (level >= 0 && scale > 0) {
                        overlayView?.batteryLevel = (level * 100) / scale
                    }
                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isChargingStatus = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                    val plugged = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                    val isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                        plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                        plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
                    if (isChargingStatus && isPlugged) {
                        isChargingState = true
                        val isFast = plugged == BatteryManager.BATTERY_PLUGGED_AC
                        overlayView?.setCharging(true, isFast)
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
            if (isTouchAnchorAdded && touchAnchorView != null) {
                try {
                    windowManager?.removeView(touchAnchorView)
                } catch (_: Exception) {}
                isTouchAnchorAdded = false
            }
            unregisterBatteryReceiver()
            unregisterSignalListeners()
            unregisterMediaSessionListener()
            unregisterProgressNotificationListener()
            unregisterTorchCallback()
        }
    }

    fun destroy() {
        removeOverlay()
        overlayView = null
        touchAnchorView = null
        currentArtOrIconBitmap = null
        currentMediaKey = null
        flashlightIconBitmap = null
    }
}

