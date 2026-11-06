/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: AodWallpaperOverlayHandler.kt
 * Description: Background service component managing AOD wallpaper overlay at 30% opacity.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.NotificationManager
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.hardware.display.DisplayManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.sameerasw.essentials.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.PriorityModeUtil
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.utils.AppUtil
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AodWallpaperOverlayHandler(
    private val service: AccessibilityService,
) {
    private var windowManager: WindowManager? = null
    private var overlayContainer: FrameLayout? = null
    private var maskedContainer: FrameLayout? = null
    private var wallpaperImageView: ImageView? = null
    private var extendedLayer: FrameLayout? = null
    private var extendedInfoColumn: LinearLayout? = null
    private var mediaTitleText: TextView? = null
    private var mediaNoteIcon: ImageView? = null
    private var appIconCachePackage: String? = null
    private var mediaSubtitleText: TextView? = null
    private var isOverlayAdded = false
    private var isScreenOff = false
    private var cachedWallpaperBitmap: Bitmap? = null
    private var currentDisplayedBitmap: Bitmap? = null
    private var currentArtBitmap: Bitmap? = null
    private var isMediaPlaying = false
    private var lastSharedArtKey: String? = null
    private var lastSelfWallpaperSetAt = 0L
    private var shareJob: Job? = null

    private var mediaSessionManager: MediaSessionManager? = null
    private val monitoredControllers = mutableListOf<MediaController>()
    private val controllerCallbacks = mutableMapOf<MediaSession.Token, MediaController.Callback>()
    private var activeMediaController: MediaController? = null
    private var isMediaListenerRegistered = false
    private var crossfadeAnimator: ValueAnimator? = null
    private var fadeInAnimator: ObjectAnimator? = null
    private var fadeOutAnimator: ObjectAnimator? = null
    private var pendingRemoval: Runnable? = null
    private var pendingSwapBitmap: Bitmap? = null

    private val powerManager by lazy { service.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    private val displayManager by lazy { service.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager }
    private var isDisplayListenerRegistered = false
    private val displayListener =
        object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}

            override fun onDisplayRemoved(displayId: Int) {}

            override fun onDisplayChanged(displayId: Int) {
                if (displayId != Display.DEFAULT_DISPLAY) return
                val state = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.state
                if (state == Display.STATE_ON && isDeviceInteractive()) {
                    onScreenOn()
                }
            }
        }

    private val handler = Handler(Looper.getMainLooper())
    private val handlerScope = CoroutineScope(Dispatchers.Main + Job())

    private val BURN_IN_INTERVAL_MS = 5 * 60 * 1000L
    // private val BURN_IN_INTERVAL_MS = 10 * 1500L // 10s for testing

    private var lastMediaChangeTimestamp = 0L
    private val MEDIA_UPDATE_DEBOUNCE_MS = 2000L

    private val mediaUpdateRunnable = Runnable {
        checkAndApplyMediaState()
    }

    private fun scheduleMediaUpdate() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastMediaChangeTimestamp < MEDIA_UPDATE_DEBOUNCE_MS) {
            return
        }
        lastMediaChangeTimestamp = now
        handler.removeCallbacks(mediaUpdateRunnable)
        handler.postDelayed(mediaUpdateRunnable, MEDIA_UPDATE_DEBOUNCE_MS)
    }

    private fun getExcludedPackages(): Set<String> {
        val excludedAppsJson = prefs.getString(SettingsRepository.KEY_AOD_WALLPAPER_MEDIA_EXCLUDED_APPS, null)
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

    private fun checkAndApplyMediaState() {
        val keepOnMedia = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_KEEP_ON_MEDIA, false)
        val useAlbumArt = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_USE_ALBUM_ART, false)
        val aodEnabled = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_ENABLED, false)

        val controller = activeMediaController
        val excludedPackages = getExcludedPackages()

        val isExcluded = controller != null && excludedPackages.contains(controller.packageName)
        val wasPlaying = isMediaPlaying
        val nowPlaying = !isExcluded && controller?.playbackState?.state == PlaybackState.STATE_PLAYING

        isMediaPlaying = nowPlaying

        if (controller == null || isExcluded) {
            currentArtBitmap = null
            if (isShareLockActive()) restoreLockWallpaper()
            if (wasPlaying && !nowPlaying && isOverlayAdded && isScreenOff) {
                scheduleTimeout()
            }
            evaluateAndApplyDisplayBitmap(animated = isOverlayAdded)
            return
        }

        if (keepOnMedia && useAlbumArt && isScreenOff && aodEnabled) {
            if (nowPlaying) {
                handler.removeCallbacks(timeoutRunnable)
                if (!isOverlayAdded) {
                    showOverlay()
                }
            } else if (wasPlaying && !nowPlaying && isOverlayAdded) {
                scheduleTimeout()
            }
        }

        val metadata = controller.metadata
        if (!nowPlaying && isShareLockActive()) restoreLockWallpaper()
        handlerScope.launch(Dispatchers.IO) {
            val art = extractMediaArtwork(metadata, controller)
            withContext(Dispatchers.Main) {
                currentArtBitmap = art
                evaluateAndApplyDisplayBitmap(animated = isOverlayAdded)
                if (nowPlaying && art != null) shareArtWithLockScreen(art, metadata)
            }
        }
    }

    private fun isShareLockActive(): Boolean =
        prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_ENABLED, false) &&
            prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_USE_ALBUM_ART, false) &&
            prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_SHARE_LOCK, false)

    private fun centerCropToScreen(src: Bitmap): Bitmap {
        val bounds = (service.getSystemService(Context.WINDOW_SERVICE) as WindowManager).currentWindowMetrics.bounds
        val w = bounds.width()
        val h = bounds.height()
        val scale = maxOf(w.toFloat() / src.width, h.toFloat() / src.height)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.DITHER_FLAG)
        paint.isFilterBitmap = true
        val matrix = android.graphics.Matrix().apply {
            setScale(scale, scale)
            postTranslate((w - src.width * scale) / 2f, (h - src.height * scale) / 2f)
        }
        val canvas = Canvas(out)
        canvas.drawBitmap(src, matrix, paint)

        val shade = darkAccentShade(src)
        val fadeHeight = h * 0.4f
        val fadePaint = android.graphics.Paint()
        fadePaint.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, fadeHeight, shade, Color.TRANSPARENT, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), fadeHeight, fadePaint)
        fadePaint.shader = android.graphics.LinearGradient(
            0f, h - fadeHeight, 0f, h.toFloat(), Color.TRANSPARENT, shade, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h - fadeHeight, w.toFloat(), h.toFloat(), fadePaint)
        return out
    }

    private fun darkAccentShade(bitmap: Bitmap): Int {
        val palette = try {
            androidx.palette.graphics.Palette.from(bitmap).maximumColorCount(24).generate()
        } catch (_: Exception) {
            null
        }
        val accent = palette?.vibrantSwatch?.rgb
            ?: palette?.lightVibrantSwatch?.rgb
            ?: palette?.dominantSwatch?.rgb
            ?: palette?.mutedSwatch?.rgb
            ?: Color.BLACK
        val hsv = FloatArray(3)
        Color.colorToHSV(accent, hsv)
        hsv[1] = hsv[1].coerceIn(0.6f, 1f)
        hsv[2] = (hsv[2] * 0.65f).coerceIn(0.4f, 0.65f)
        return Color.HSVToColor(hsv)
    }

    private fun loadHighResArt(metadata: MediaMetadata?): Bitmap? {
        val uriKeys = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
        )
        for (key in uriKeys) {
            val uri = metadata?.getString(key)?.takeIf { it.isNotBlank() } ?: continue
            val parsed = android.net.Uri.parse(uri)
            if (parsed.scheme != "content" && parsed.scheme != "file") continue
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                service.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) continue
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= 1440 && bounds.outHeight / (sample * 2) >= 1440) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                service.contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, opts) }
                    ?.let { return it }
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun restoreIfNotPlaying() {
        if (runtimeState.getInt(KEY_LOCK_BACKUP_STATE, LOCK_BACKUP_NONE) == LOCK_BACKUP_NONE) return
        val excluded = getExcludedPackages()
        val playing = try {
            val msm = service.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            msm?.getActiveSessions(ComponentName(service, NotificationListener::class.java))
                ?.any { it.packageName !in excluded && it.playbackState?.state == PlaybackState.STATE_PLAYING } == true
        } catch (_: Exception) {
            false
        }
        if (!playing) restoreLockWallpaper()
    }

    private fun shareArtWithLockScreen(art: Bitmap, metadata: MediaMetadata?) {
        if (!isShareLockActive()) return
        val key = "${metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)}|" +
            "${metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)}|" +
            "${metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)}"
        if (key == lastSharedArtKey) return
        lastSharedArtKey = key
        shareJob?.cancel()
        shareJob = handlerScope.launch(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(service)
                backupLockWallpaper(wallpaperManager)
                val rendered = loadHighResArt(metadata)?.takeIf { it.width * it.height > art.width * art.height } ?: art
                val aligned = centerCropToScreen(rendered)
                lastSelfWallpaperSetAt = System.currentTimeMillis()
                wallpaperManager.setBitmap(aligned, null, true, WallpaperManager.FLAG_LOCK)
                lastSelfWallpaperSetAt = System.currentTimeMillis()
            } catch (e: Throwable) {
                Log.e("AodWallpaperOverlay", "Failed to share art with lock screen", e)
                lastSharedArtKey = null
            }
        }
    }

    private fun backupLockWallpaper(wallpaperManager: WallpaperManager) {
        if (runtimeState.getInt(KEY_LOCK_BACKUP_STATE, LOCK_BACKUP_NONE) != LOCK_BACKUP_NONE) return
        val lockId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) wallpaperManager.getWallpaperId(WallpaperManager.FLAG_LOCK) else -1
        if (lockId <= 0) {
            runtimeState.edit().putInt(KEY_LOCK_BACKUP_STATE, LOCK_BACKUP_SYSTEM).apply()
            return
        }
        val original = drawableToBitmap(wallpaperManager.getDrawable(WallpaperManager.FLAG_LOCK)) ?: return
        File(service.filesDir, LOCK_BACKUP_FILE_NAME).outputStream().use {
            original.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        runtimeState.edit().putInt(KEY_LOCK_BACKUP_STATE, LOCK_BACKUP_FILE).apply()
    }

    private fun restoreLockWallpaper() {
        val state = runtimeState.getInt(KEY_LOCK_BACKUP_STATE, LOCK_BACKUP_NONE)
        if (state == LOCK_BACKUP_NONE) return
        shareJob?.cancel()
        lastSharedArtKey = null
        handlerScope.launch(Dispatchers.IO) {
            try {
                val wallpaperManager = WallpaperManager.getInstance(service)
                val file = File(service.filesDir, LOCK_BACKUP_FILE_NAME)
                lastSelfWallpaperSetAt = System.currentTimeMillis()
                if (state == LOCK_BACKUP_FILE && file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                } else if (state == LOCK_BACKUP_SYSTEM) {
                    wallpaperManager.clear(WallpaperManager.FLAG_LOCK)
                }
                lastSelfWallpaperSetAt = System.currentTimeMillis()
                clearLockBackup()
                withContext(Dispatchers.Main) { invalidateWallpaperCache() }
            } catch (e: Throwable) {
                Log.e("AodWallpaperOverlay", "Failed to restore lock wallpaper", e)
            }
        }
    }

    private fun clearLockBackup() {
        File(service.filesDir, LOCK_BACKUP_FILE_NAME).delete()
        runtimeState.edit().putInt(KEY_LOCK_BACKUP_STATE, LOCK_BACKUP_NONE).apply()
    }

    private fun onSystemWallpaperChanged() {
        if (System.currentTimeMillis() - lastSelfWallpaperSetAt < SELF_WALLPAPER_CHANGE_WINDOW_MS) return
        if (runtimeState.getInt(KEY_LOCK_BACKUP_STATE, LOCK_BACKUP_NONE) != LOCK_BACKUP_NONE) {
            clearLockBackup()
            lastSharedArtKey = null
        }
    }

    private val activeSessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateActiveMediaSession(controllers)
        }

    private val burnInShiftRunnable = object : Runnable {
        override fun run() {
            if (isOverlayAdded && isScreenOff) {
                applyBurnInShift(animated = false)
                handler.postDelayed(this, BURN_IN_INTERVAL_MS)
            }
        }
    }

    private val removalFallbackRunnable = Runnable {
        if (pendingRemoval != null) {
            Log.w("AodWallpaperOverlay", "Fade-out did not finish, removing overlay directly")
            runPendingRemoval()
        }
    }

    private val fadeInFallbackRunnable = Runnable {
        val container = overlayContainer ?: return@Runnable
        if (isOverlayAdded && pendingRemoval == null && container.alpha < 1f) {
            fadeInAnimator?.cancel()
            fadeInAnimator = null
            container.alpha = 1f
        }
    }

    private val bitmapSwapFallbackRunnable = Runnable {
        val bitmap = pendingSwapBitmap ?: return@Runnable
        pendingSwapBitmap = null
        val imageView = wallpaperImageView ?: return@Runnable
        if (currentDisplayedBitmap !== bitmap) return@Runnable
        crossfadeAnimator?.let {
            crossfadeAnimator = null
            it.removeAllListeners()
            it.cancel()
        }
        imageView.setImageBitmap(bitmap)
        imageView.alpha = prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_OPACITY, 0.3f)
    }

    private val timeoutRunnable: Runnable = Runnable {
        if (isOverlayAdded && isScreenOff) {
            val keepOnMedia = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_KEEP_ON_MEDIA, false)
            val useAlbumArt = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_USE_ALBUM_ART, false)
            if (keepOnMedia && useAlbumArt && isMediaPlaying) {
                handler.removeCallbacks(timeoutRunnable)
                return@Runnable
            }
            hideOverlay()
        }
    }

    private val prefs by lazy {
        service.getSharedPreferences(
            SettingsRepository.PREFS_NAME,
            AccessibilityService.MODE_PRIVATE,
        )
    }

    private val wallpaperChangeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            onSystemWallpaperChanged()
            invalidateWallpaperCache()
        }
    }

    private val wallpaperColorsListener =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            WallpaperManager.OnColorsChangedListener { _, _ ->
                onSystemWallpaperChanged()
                invalidateWallpaperCache()
            }
        } else {
            null
        }

    private var isReceiverRegistered = false
    private var isPriorityModeReceiverRegistered = false

    private val priorityModeReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                updateState()
            }
        }

    private val runtimeState by lazy {
        service.getSharedPreferences("aod_wallpaper_runtime_state", Context.MODE_PRIVATE)
    }

    init {
        registerWallpaperChangeListeners()
        setLockScreenMediaSuppressed(false)
    }

    private fun hasSecureWritePermission(): Boolean =
        service.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    private var lockScreenMediaSuppressDesired = false

    // The key is @hide and unreadable for apps on S+, so the previous value comes from the shell when available.
    private fun readLockScreenMediaValue(): Int =
        ShellUtils
            .runCommandWithOutput(
                service,
                "settings get secure $LOCK_SCREEN_MEDIA_KEY",
                notifyOnError = false,
            )?.toIntOrNull() ?: 1

    private fun writeLockScreenMediaValue(value: Int) {
        try {
            Settings.Secure.putInt(service.contentResolver, LOCK_SCREEN_MEDIA_KEY, value)
        } catch (e: Exception) {
            Log.e("AodWallpaperOverlay", "Failed to write lock screen media setting", e)
        }
    }

    // Hides the system lock screen media player on AOD, restoring the user's value afterwards.
    private fun setLockScreenMediaSuppressed(suppress: Boolean) {
        lockScreenMediaSuppressDesired = suppress
        val savedValue = runtimeState.getInt(KEY_SAVED_LOCK_SCREEN_MEDIA, NO_SAVED_VALUE)
        if (suppress) {
            if (savedValue != NO_SAVED_VALUE || !hasSecureWritePermission()) return
            handlerScope.launch(Dispatchers.IO) {
                val previous = readLockScreenMediaValue()
                withContext(Dispatchers.Main) {
                    if (!lockScreenMediaSuppressDesired) return@withContext
                    if (runtimeState.getInt(KEY_SAVED_LOCK_SCREEN_MEDIA, NO_SAVED_VALUE) != NO_SAVED_VALUE) {
                        return@withContext
                    }
                    runtimeState.edit().putInt(KEY_SAVED_LOCK_SCREEN_MEDIA, previous).apply()
                    if (previous != 0) writeLockScreenMediaValue(0)
                }
            }
        } else if (savedValue != NO_SAVED_VALUE) {
            if (hasSecureWritePermission()) writeLockScreenMediaValue(savedValue)
            runtimeState.edit().remove(KEY_SAVED_LOCK_SCREEN_MEDIA).apply()
        }
    }

    private fun registerPriorityModeListener() {
        if (isPriorityModeReceiverRegistered) return
        try {
            val filter =
                android.content.IntentFilter(
                    NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED,
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                service.registerReceiver(
                    priorityModeReceiver,
                    filter,
                    AccessibilityService.RECEIVER_NOT_EXPORTED,
                )
            } else {
                service.registerReceiver(priorityModeReceiver, filter)
            }
            isPriorityModeReceiverRegistered = true
        } catch (e: Exception) {
            Log.e("AodWallpaperOverlay", "Failed to register priority mode receiver", e)
        }
    }

    private fun unregisterPriorityModeListener() {
        if (!isPriorityModeReceiverRegistered) return
        try {
            service.unregisterReceiver(priorityModeReceiver)
        } catch (_: Exception) {
        }
        isPriorityModeReceiverRegistered = false
    }

    private fun registerWallpaperChangeListeners() {
        registerPriorityModeListener()
        if (!isReceiverRegistered) {
            try {
                val filter = android.content.IntentFilter(Intent.ACTION_WALLPAPER_CHANGED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    service.registerReceiver(
                        wallpaperChangeReceiver,
                        filter,
                        AccessibilityService.RECEIVER_NOT_EXPORTED,
                    )
                } else {
                    service.registerReceiver(wallpaperChangeReceiver, filter)
                }
                isReceiverRegistered = true
            } catch (e: Exception) {
                Log.e("AodWallpaperOverlay", "Failed to register wallpaper broadcast receiver", e)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wallpaperColorsListener?.let {
                try {
                    val wm = WallpaperManager.getInstance(service)
                    wm.addOnColorsChangedListener(it, handler)
                } catch (e: Exception) {
                    Log.e("AodWallpaperOverlay", "Failed to add wallpaper colors changed listener", e)
                }
            }
        }
    }

    private fun unregisterWallpaperChangeListeners() {
        unregisterPriorityModeListener()
        if (isReceiverRegistered) {
            try {
                service.unregisterReceiver(wallpaperChangeReceiver)
            } catch (e: Exception) {
                // Ignore
            }
            isReceiverRegistered = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wallpaperColorsListener?.let {
                try {
                    val wm = WallpaperManager.getInstance(service)
                    wm.removeOnColorsChangedListener(it)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun onScreenOff() {
        isScreenOff = true
        updateState()
    }

    fun onScreenOn() {
        isScreenOff = false
        if (isShareLockActive()) restoreIfNotPlaying()
        handler.removeCallbacks(timeoutRunnable)
        hideOverlay()
    }

    fun onUserPresent() {
        if (!prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_ENABLED, false)) return
        onScreenOn()
    }

    private fun isDeviceInteractive(): Boolean = powerManager?.isInteractive == true

    private fun registerDisplayListener() {
        if (isDisplayListenerRegistered) return
        try {
            displayManager?.registerDisplayListener(displayListener, handler)
            isDisplayListenerRegistered = true
        } catch (e: Exception) {
            Log.e("AodWallpaperOverlay", "Failed to register display listener", e)
        }
    }

    private fun unregisterDisplayListener() {
        if (!isDisplayListenerRegistered) return
        try {
            displayManager?.unregisterDisplayListener(displayListener)
        } catch (_: Exception) {
        }
        isDisplayListenerRegistered = false
    }

    fun updateState() {
        if (!isShareLockActive()) restoreLockWallpaper()
        if (isScreenOff && isDeviceInteractive()) isScreenOff = false
        val enabled = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_ENABLED, false)
        if (enabled && isScreenOff && !isSuppressedByPriorityMode()) {
            showOverlay()
        } else {
            hideOverlay()
        }
    }

    private fun isSuppressedByPriorityMode(): Boolean {
        if (!prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_DISABLE_ON_DND, false)) return false
        return PriorityModeUtil.isActive(service)
    }

    private fun applyBurnInShift(animated: Boolean) {
        applyExtendedInfoShift(animated)
        val imageView = wallpaperImageView ?: return
        val maxShiftPx = (8 * service.resources.displayMetrics.density).toInt()
        val targetX = (-maxShiftPx..maxShiftPx).random().toFloat()
        val targetY = (-maxShiftPx..maxShiftPx).random().toFloat()

        if (animated) {
            ObjectAnimator.ofFloat(imageView, "scaleX", imageView.scaleX, 1.05f).apply {
                duration = 1000
                start()
            }
            ObjectAnimator.ofFloat(imageView, "scaleY", imageView.scaleY, 1.05f).apply {
                duration = 1000
                start()
            }
            ObjectAnimator.ofFloat(imageView, "translationX", imageView.translationX, targetX).apply {
                duration = 1000
                start()
            }
            ObjectAnimator.ofFloat(imageView, "translationY", imageView.translationY, targetY).apply {
                duration = 1000
                start()
            }
        } else {
            imageView.scaleX = 1.05f
            imageView.scaleY = 1.05f
            imageView.translationX = targetX
            imageView.translationY = targetY
        }
    }

    // Chains the color filter and blur into one RenderEffect graph on S+ so neither silently drops the other.
    private fun applyImageEffects(
        imageView: ImageView,
        blurRadius: Float,
        colorFilter: android.graphics.ColorFilter,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            imageView.colorFilter = null
            val colorFilterEffect = android.graphics.RenderEffect.createColorFilterEffect(colorFilter)
            val effect =
                if (blurRadius > 0f) {
                    android.graphics.RenderEffect.createBlurEffect(
                        blurRadius * 4f,
                        blurRadius * 4f,
                        colorFilterEffect,
                        Shader.TileMode.CLAMP,
                    )
                } else {
                    colorFilterEffect
                }
            imageView.setRenderEffect(effect)
        } else {
            imageView.colorFilter = colorFilter
        }
    }

    private fun applyExtendedInfoShift(animated: Boolean) {
        val column = extendedInfoColumn ?: return
        val maxShiftPx = (4 * service.resources.displayMetrics.density).toInt()
        val targetX = (-maxShiftPx..maxShiftPx).random().toFloat()
        val targetY = (-maxShiftPx..maxShiftPx).random().toFloat()
        if (animated) {
            column.animate().translationX(targetX).translationY(targetY).setDuration(1000).start()
        } else {
            column.translationX = targetX
            column.translationY = targetY
        }
    }

    private fun buildExtendedLayer(): FrameLayout {
        val density = service.resources.displayMetrics.density
        val typeface = ResourcesCompat.getFont(service, R.font.google_sans_flex)

        fun label(
            sizeSp: Float,
            alphaValue: Float,
            weight: Int,
        ) = TextView(service).apply {
            setTextColor(Color.WHITE)
            alpha = alphaValue
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTypeface(typeface)
            fontVariationSettings = "'wght' $weight, 'ROND' 100"
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setSingleLine(true)
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val iconSizePx =
            TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_SP, 34f, service.resources.displayMetrics)
                .toInt()
        val noteIcon =
            ImageView(service).apply {
                setImageResource(R.drawable.rounded_music_note_24)
                setColorFilter(Color.WHITE)
                alpha = 0.8f
            }

        val title = label(16f, 0.95f, 800)
        val subtitle = label(14f, 0.7f, 550)

        val column =
            LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                val pad = (32 * density).toInt()
                setPadding(pad, 0, pad, 0)
                addView(
                    noteIcon,
                    LinearLayout.LayoutParams(iconSizePx, iconSizePx).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        bottomMargin = (2 * density).toInt()
                    },
                )
                addView(
                    title,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
                addView(
                    subtitle,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { topMargin = (1 * density).toInt() },
                )
                layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM,
                    ).apply { bottomMargin = (96 * density).toInt() }
            }

        mediaTitleText = title
        mediaNoteIcon = noteIcon
        mediaSubtitleText = subtitle
        extendedInfoColumn = column

        return FrameLayout(service).apply { addView(column) }
    }

    private fun applyExtendedTextScale() {
        val scale = prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_EXTENDED_TEXT_SCALE, 1f).coerceIn(0.7f, 2f)
        mediaTitleText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * scale)
        mediaSubtitleText?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f * scale)
        val useAppIcon = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_EXTENDED_APP_ICON, false)
        val iconSizePx =
            TypedValue
                .applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    (if (useAppIcon) 44f else 34f) * scale,
                    service.resources.displayMetrics,
                ).toInt()
        mediaNoteIcon?.let { icon ->
            val params = icon.layoutParams
            if (params != null && (params.width != iconSizePx || params.height != iconSizePx)) {
                params.width = iconSizePx
                params.height = iconSizePx
                icon.layoutParams = params
            }
        }
    }

    private fun updateExtendedInfo() {
        val layer = extendedLayer ?: return
        applyExtendedTextScale()
        val enabled = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_EXTENDED_MEDIA, false)
        setLockScreenMediaSuppressed(enabled && isScreenOff)

        val metadata = activeMediaController?.metadata
        val trackTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        if (enabled && isMediaPlaying && !trackTitle.isNullOrBlank()) {
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            val subtitle = listOf(artist, album).filter { !it.isNullOrBlank() }.joinToString(" · ")
            updateNoteIcon()
            mediaTitleText?.text = trackTitle
            mediaSubtitleText?.text = subtitle
            mediaSubtitleText?.visibility = if (subtitle.isBlank()) View.GONE else View.VISIBLE
            layer.visibility = View.VISIBLE
        } else {
            layer.visibility = View.GONE
        }
    }

    // Prefers the themed monochrome layer on Android 13+, otherwise falls back to a grayscale icon.
    private fun updateNoteIcon() {
        val icon = mediaNoteIcon ?: return
        val packageName = activeMediaController?.packageName
        if (!prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_EXTENDED_APP_ICON, false) || packageName == null) {
            appIconCachePackage = null
            icon.colorFilter = null
            icon.setImageResource(R.drawable.rounded_music_note_24)
            icon.setColorFilter(Color.WHITE)
            return
        }
        if (appIconCachePackage == packageName) return
        try {
            val drawable = service.packageManager.getApplicationIcon(packageName)
            val monochrome =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable) {
                    drawable.monochrome
                } else {
                    null
                }
            if (monochrome != null) {
                icon.setImageDrawable(monochrome)
                icon.setColorFilter(Color.WHITE)
            } else {
                icon.setImageDrawable(drawable)
                icon.colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(0f) })
            }
            appIconCachePackage = packageName
        } catch (_: Exception) {
            appIconCachePackage = null
            icon.setImageResource(R.drawable.rounded_music_note_24)
            icon.setColorFilter(Color.WHITE)
        }
    }

    private fun needsMediaSession(): Boolean =
        prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_USE_ALBUM_ART, false) ||
            prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_EXTENDED_MEDIA, false)

    private fun buildMaskedContainer(): FrameLayout {
        return object : FrameLayout(service) {
            private val vignettePaint = android.graphics.Paint().apply {
                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
                isAntiAlias = true
            }

            override fun dispatchDraw(canvas: android.graphics.Canvas) {
                if (width == 0 || height == 0) {
                    super.dispatchDraw(canvas)
                    return
                }

                val intensity = prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_VIGNETTE, 0f)
                val hasVignette = intensity > 0f

                val saveCount = if (hasVignette) {
                    canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
                } else {
                    -1
                }

                super.dispatchDraw(canvas)

                if (hasVignette) {
                    val cx = width / 2f
                    val cy = height / 2f
                    val radius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()
                    val edgeAlpha = ((1f - intensity / 100f).coerceIn(0f, 1f) * 255).toInt()
                    vignettePaint.shader = RadialGradient(
                        cx, cy, radius,
                        intArrayOf(
                            Color.BLACK,
                            Color.BLACK,
                            Color.argb(edgeAlpha, 0, 0, 0),
                        ),
                        floatArrayOf(0f, 0.45f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
                    canvas.restoreToCount(saveCount)
                }
            }
        }.apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun showOverlay() {
        if (isDeviceInteractive()) {
            isScreenOff = false
            hideOverlay()
            return
        }
        runPendingRemoval()
        registerDisplayListener()
        val opacity = prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_OPACITY, 0.3f)
        val blurRadius = prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_BLUR, 0f)
        val blackThreshold = prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_BLACK_THRESHOLD, 15f)

        val luminanceFilter = android.graphics.ColorMatrixColorFilter(
            android.graphics.ColorMatrix(
                floatArrayOf(
                    1.2f, 0f, 0f, 0f, 0f,
                    0f, 1.2f, 0f, 0f, 0f,
                    0f, 0f, 1.2f, 0f, 0f,
                    0.5f, 1.5f, 0.2f, 0f, -blackThreshold,
                )
            )
        )

        if (overlayContainer == null) {
            val root = buildMaskedContainer()
            val imageView = ImageView(service).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                alpha = opacity
            }
            applyImageEffects(imageView, blurRadius, luminanceFilter)
            root.addView(imageView)
            wallpaperImageView = imageView
            maskedContainer = root
            val layer = buildExtendedLayer()
            extendedLayer = layer
            overlayContainer =
                FrameLayout(service).apply {
                    addView(root)
                    addView(layer)
                }
        } else {
            val imageView = wallpaperImageView ?: return
            imageView.alpha = opacity
            applyImageEffects(imageView, blurRadius, luminanceFilter)
            maskedContainer?.invalidate()
        }

        if (needsMediaSession()) {
            registerMediaSessionListener()
        } else {
            unregisterMediaSessionListener()
        }

        loadAndApplyWallpaper()
        updateExtendedInfo()

        if (windowManager == null) {
            windowManager = service.getSystemService(AccessibilityService.WINDOW_SERVICE) as? WindowManager
        }

        if (!isOverlayAdded && windowManager != null && overlayContainer != null) {
            val params =
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                }

            try {
                val container = overlayContainer ?: return
                val fadeIn = !compatMode()
                container.visibility = View.VISIBLE
                container.alpha = if (fadeIn) 0f else 1f
                applyBurnInShift(animated = false)
                windowManager?.addView(container, params)
                isOverlayAdded = true

                fadeInAnimator?.cancel()
                fadeInAnimator = null
                handler.removeCallbacks(fadeInFallbackRunnable)
                if (fadeIn) {
                    fadeInAnimator =
                        ObjectAnimator.ofFloat(container, "alpha", 0f, 1f).apply {
                            duration = FADE_IN_DURATION_MS
                            start()
                        }
                    handler.postDelayed(fadeInFallbackRunnable, FADE_IN_DURATION_MS + ANIMATION_FALLBACK_SLACK_MS)
                }

                handler.removeCallbacks(burnInShiftRunnable)
                handler.postDelayed(burnInShiftRunnable, BURN_IN_INTERVAL_MS)
                scheduleTimeout()
            } catch (e: Exception) {
                Log.e("AodWallpaperOverlay", "Failed to add AOD wallpaper overlay", e)
            }
        } else if (isOverlayAdded && overlayContainer != null) {
            val container = overlayContainer ?: return
            container.visibility = View.VISIBLE
            container.alpha = 1f
            scheduleTimeout()
        }
    }

    private fun scheduleTimeout() {
        handler.removeCallbacks(timeoutRunnable)
        val keepOnMedia = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_KEEP_ON_MEDIA, false)
        val useAlbumArt = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_USE_ALBUM_ART, false)
        if (keepOnMedia && useAlbumArt && isMediaPlaying) {
            return
        }

        val timeoutStr = try {
            prefs.getString(SettingsRepository.KEY_AOD_WALLPAPER_TIMEOUT, "3m") ?: "3m"
        } catch (e: Exception) {
            val oldInt = prefs.getInt(SettingsRepository.KEY_AOD_WALLPAPER_TIMEOUT, 3)
            if (oldInt == 0) "never" else "${oldInt}m"
        }

        val millis = parseTimeoutToMillis(timeoutStr)
        if (millis > 0L) {
            handler.postDelayed(timeoutRunnable, millis)
        }
    }

    private fun parseTimeoutToMillis(timeout: String): Long {
        return when {
            timeout == "never" || timeout == "0" -> 0L
            timeout == "custom" -> {
                val customSecs = prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_CUSTOM_TIMEOUT, 30f).toLong()
                customSecs * 1000L
            }
            timeout.endsWith("s") -> (timeout.dropLast(1).toLongOrNull() ?: 0L) * 1000L
            timeout.endsWith("m") -> (timeout.dropLast(1).toLongOrNull() ?: 0L) * 60_000L
            else -> (timeout.toLongOrNull() ?: 0L) * 60_000L
        }
    }

    private fun reEvaluateAndApplyMedia(isInitial: Boolean = false) {
        handler.post {
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
        handler.post {
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
                            handler.post {
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
                        controller.registerCallback(callback, handler)
                        controllerCallbacks[controller.sessionToken] = callback
                        monitoredControllers.add(controller)
                    } catch (_: Exception) {}
                }
            }

            reEvaluateAndApplyMedia(isInitial = isInitial)
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
                Log.e("AodWallpaperOverlay", "Failed to register active media session listener", e)
            }
        }
    }

    private fun unregisterMediaSessionListener() {
        handler.removeCallbacks(mediaUpdateRunnable)
        if (isMediaListenerRegistered) {
            try {
                mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
            } catch (e: Exception) {
                // Ignore
            }
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
            currentArtBitmap = null
            isMediaPlaying = false
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
                        } catch (_: Exception) {
                        }
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
                } catch (_: Exception) {
                }
            }
        }

        return bitmap
    }

    private fun evaluateAndApplyDisplayBitmap(animated: Boolean = false) {
        updateExtendedInfo()
        val useAlbumArt = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_USE_ALBUM_ART, false)
        val art = currentArtBitmap
        val shouldShowArt = useAlbumArt && isMediaPlaying && art != null

        if (shouldShowArt) {
            applyTargetBitmap(art, animated)
        } else {
            if (cachedWallpaperBitmap != null) {
                applyTargetBitmap(cachedWallpaperBitmap!!, animated)
            } else {
                handlerScope.launch(Dispatchers.IO) {
                    val wp = extractCurrentWallpaper()
                    if (wp != null) {
                        cachedWallpaperBitmap = wp
                        withContext(Dispatchers.Main) {
                            applyTargetBitmap(wp, animated)
                        }
                    }
                }
            }
        }
    }

    private fun applyTargetBitmap(bitmap: Bitmap, animated: Boolean) {
        val imageView = wallpaperImageView ?: return
        if (currentDisplayedBitmap == bitmap) return

        currentDisplayedBitmap = bitmap

        handler.removeCallbacks(bitmapSwapFallbackRunnable)
        pendingSwapBitmap = null
        if (!animated || compatMode() || imageView.drawable == null) {
            crossfadeAnimator?.cancel()
            crossfadeAnimator = null
            imageView.setImageBitmap(bitmap)
        } else {
            val targetOpacity = prefs.getFloat(SettingsRepository.KEY_AOD_WALLPAPER_OPACITY, 0.3f)
            crossfadeAnimator?.cancel()
            crossfadeAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = CROSSFADE_HALF_DURATION_MS
                addUpdateListener { animator ->
                    val fraction = animator.animatedValue as Float
                    imageView.alpha = targetOpacity * fraction
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        imageView.setImageBitmap(bitmap)
                        crossfadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                            duration = CROSSFADE_HALF_DURATION_MS
                            addUpdateListener { inAnim ->
                                val fraction = inAnim.animatedValue as Float
                                imageView.alpha = targetOpacity * fraction
                            }
                            addListener(object : android.animation.AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: android.animation.Animator) {
                                    pendingSwapBitmap = null
                                    handler.removeCallbacks(bitmapSwapFallbackRunnable)
                                }
                            })
                            start()
                        }
                    }
                })
                start()
            }
            pendingSwapBitmap = bitmap
            handler.postDelayed(
                bitmapSwapFallbackRunnable,
                CROSSFADE_HALF_DURATION_MS * 2 + ANIMATION_FALLBACK_SLACK_MS,
            )
        }
    }

    private var lastWallpaperId = -1

    private fun loadAndApplyWallpaper() {
        val wallpaperManager = WallpaperManager.getInstance(service)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val currentLockId = wallpaperManager.getWallpaperId(WallpaperManager.FLAG_LOCK)
            val currentSystemId = wallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
            val effectiveId = if (currentLockId > 0) currentLockId else currentSystemId
            if (effectiveId > 0 && effectiveId != lastWallpaperId) {
                cachedWallpaperBitmap = null
                lastWallpaperId = effectiveId
            }
        }

        evaluateAndApplyDisplayBitmap(animated = false)
    }

    private fun extractCurrentWallpaper(): Bitmap? {
        return try {
            if (prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_CUSTOM_IMAGE, false)) {
                val file = File(service.filesDir, CUSTOM_IMAGE_FILE_NAME)
                if (file.exists()) {
                    val customBmp = AppUtil.decodeSampledBitmapFromFile(file.absolutePath, reqWidth = 1440, reqHeight = 3200)
                    if (customBmp != null) return customBmp
                }
            }

            val backupFile = File(service.filesDir, LOCK_BACKUP_FILE_NAME)
            val backupState = runtimeState.getInt(KEY_LOCK_BACKUP_STATE, LOCK_BACKUP_NONE)
            if (backupState == LOCK_BACKUP_FILE && backupFile.exists()) {
                BitmapFactory.decodeFile(backupFile.absolutePath)?.let { return it }
            }

            val wallpaperManager = WallpaperManager.getInstance(service)
            val drawable: Drawable? =
                if (backupState == LOCK_BACKUP_SYSTEM) {
                    wallpaperManager.getDrawable(WallpaperManager.FLAG_SYSTEM)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    wallpaperManager.getDrawable(WallpaperManager.FLAG_LOCK)
                        ?: wallpaperManager.drawable
                } else {
                    wallpaperManager.drawable
                }

            drawableToBitmap(drawable)
        } catch (e: Throwable) {
            Log.e("AodWallpaperOverlay", "Error extracting current wallpaper", e)
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) return null
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val orig = drawable.bitmap
            if (orig.width > 2560 || orig.height > 2560) {
                val scale = minOf(1440f / orig.width, 3200f / orig.height, 1.0f)
                return if (scale < 1.0f) {
                    Bitmap.createScaledBitmap(orig, (orig.width * scale).toInt(), (orig.height * scale).toInt(), true)
                } else {
                    orig
                }
            }
            return orig
        }

        val width = if (drawable.intrinsicWidth in 1..2560) drawable.intrinsicWidth else 1080
        val height = if (drawable.intrinsicHeight in 1..3200) drawable.intrinsicHeight else 2400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun invalidateWallpaperCache() {
        cachedWallpaperBitmap = null
        lastWallpaperId = -1
        if (isOverlayAdded) {
            evaluateAndApplyDisplayBitmap(animated = true)
        }
    }

    private fun hideOverlay() {
        val keepOnMedia = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_KEEP_ON_MEDIA, false)
        val useAlbumArt = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_USE_ALBUM_ART, false)
        val aodEnabled = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_ENABLED, false)

        if (!isScreenOff || !aodEnabled || !useAlbumArt || !keepOnMedia) {
            unregisterMediaSessionListener()
        }
        crossfadeAnimator?.cancel()
        crossfadeAnimator = null
        fadeInAnimator?.cancel()
        fadeInAnimator = null
        unregisterDisplayListener()
        setLockScreenMediaSuppressed(false)
        currentDisplayedBitmap = null
        handler.removeCallbacks(burnInShiftRunnable)
        handler.removeCallbacks(timeoutRunnable)
        handler.removeCallbacks(fadeInFallbackRunnable)
        handler.removeCallbacks(bitmapSwapFallbackRunnable)
        if (isOverlayAdded && overlayContainer != null && pendingRemoval == null) {
            val currentView = overlayContainer ?: return
            val imageView = wallpaperImageView

            if (imageView != null) {
                if (imageView.translationX != 0f || imageView.translationY != 0f) {
                    ObjectAnimator.ofFloat(imageView, "translationX", imageView.translationX, 0f).apply {
                        duration = 500
                        start()
                    }
                    ObjectAnimator.ofFloat(imageView, "translationY", imageView.translationY, 0f).apply {
                        duration = 500
                        start()
                    }
                }
                if (imageView.scaleX != 1.0f || imageView.scaleY != 1.0f) {
                    ObjectAnimator.ofFloat(imageView, "scaleX", imageView.scaleX, 1.0f).apply {
                        duration = 500
                        start()
                    }
                    ObjectAnimator.ofFloat(imageView, "scaleY", imageView.scaleY, 1.0f).apply {
                        duration = 500
                        start()
                    }
                }
            }

            pendingRemoval = Runnable {
                currentView.visibility = View.GONE
                imageView?.translationX = 0f
                imageView?.translationY = 0f
                imageView?.scaleX = 1.0f
                imageView?.scaleY = 1.0f
                extendedInfoColumn?.translationX = 0f
                extendedInfoColumn?.translationY = 0f
                try {
                    windowManager?.removeView(currentView)
                } catch (_: Exception) {
                }
                isOverlayAdded = false
                overlayContainer = null
                maskedContainer = null
                wallpaperImageView = null
                extendedLayer = null
                extendedInfoColumn = null
                mediaTitleText = null
                mediaNoteIcon = null
                appIconCachePackage = null
                mediaSubtitleText = null
            }

            if (compatMode()) {
                runPendingRemoval()
                return
            }
            fadeOutAnimator = ObjectAnimator.ofFloat(currentView, "alpha", currentView.alpha, 0f).apply {
                duration = FADE_OUT_DURATION_MS
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        runPendingRemoval()
                    }
                })
                start()
            }
            handler.postDelayed(removalFallbackRunnable, FADE_OUT_DURATION_MS + ANIMATION_FALLBACK_SLACK_MS)
        }
    }

    private fun compatMode(): Boolean = prefs.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_COMPAT, false)

    private fun runPendingRemoval() {
        val removal = pendingRemoval ?: return
        pendingRemoval = null
        handler.removeCallbacks(removalFallbackRunnable)
        fadeOutAnimator?.let {
            fadeOutAnimator = null
            it.removeAllListeners()
            it.cancel()
        }
        removal.run()
    }

    fun removeOverlay() {
        unregisterWallpaperChangeListeners()
        handler.removeCallbacks(burnInShiftRunnable)
        handler.removeCallbacks(timeoutRunnable)
        hideOverlay()
        runPendingRemoval()
        setLockScreenMediaSuppressed(false)
        overlayContainer = null
        maskedContainer = null
        wallpaperImageView = null
        extendedLayer = null
        extendedInfoColumn = null
        cachedWallpaperBitmap = null
    }

    private companion object {
        const val LOCK_SCREEN_MEDIA_KEY = "media_controls_lock_screen"
        const val KEY_SAVED_LOCK_SCREEN_MEDIA = "saved_lock_screen_media"
        const val NO_SAVED_VALUE = -1
        const val KEY_LOCK_BACKUP_STATE = "lock_wallpaper_backup_state"
        const val LOCK_BACKUP_NONE = 0
        const val LOCK_BACKUP_FILE = 1
        const val LOCK_BACKUP_SYSTEM = 2
        const val LOCK_BACKUP_FILE_NAME = "lock_wallpaper_backup.png"
        const val SELF_WALLPAPER_CHANGE_WINDOW_MS = 5000L
        const val CUSTOM_IMAGE_FILE_NAME = "custom_aod_wallpaper.png"
        const val FADE_IN_DURATION_MS = 2000L
        const val FADE_OUT_DURATION_MS = 500L
        const val CROSSFADE_HALF_DURATION_MS = 350L
        const val ANIMATION_FALLBACK_SLACK_MS = 250L
    }
}
