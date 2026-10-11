/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: WidgetScraperService.kt
 * Description: Background service component for WidgetScraperService.kt.
 */

package com.sameerasw.essentials.services.widgets

import android.app.Service
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.SizeF
import android.view.Gravity
import android.view.WindowManager
import android.widget.RemoteViews
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.services.NotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class WidgetScraperService : Service() {
    private inner class ScrapingHostView(
        context: Context,
    ) : AppWidgetHostView(context) {
        override fun updateAppWidget(remoteViews: RemoteViews?) {
            if (remoteViews != null) onRemoteViewsReceived(remoteViews)
        }
    }

    private inner class ScrapingWidgetHost(
        context: Context,
        hostId: Int,
    ) : AppWidgetHost(context, hostId) {
        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?,
        ): AppWidgetHostView = ScrapingHostView(context)
    }

    companion object {
        const val HOST_ID = 1025

        @Volatile
        var currentRemoteViews: RemoteViews? = null
            private set

        @Volatile
        var isUnsupported = false
            private set

        @Volatile
        private var rawRemoteViews: RemoteViews? = null

        @Volatile
        private var hostSizeDp: SizeF? = null

        @Volatile
        private var forwarded: Pair<Int, SizeF>? = null

        @Volatile
        private var instance: WidgetScraperService? = null

        fun syncHostSize(context: Context) {
            val awm = AppWidgetManager.getInstance(context)
            val searchbarId =
                awm
                    .getAppWidgetIds(ComponentName(context, PixelSearchbarWidgetReceiver::class.java))
                    .firstOrNull() ?: return
            val settings = SettingsRepository(context)
            val options = awm.getAppWidgetOptions(searchbarId)
            val portrait = context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
            val width =
                options.getInt(
                    if (portrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                ) - settings.getPixelSearchbarWidgetPaddingH() * 2
            val height =
                options.getInt(
                    if (portrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                ) - settings.getPixelSearchbarWidgetPaddingV() * 2
            if (width <= 0 || height <= 0) return

            val size = SizeF(width.toFloat(), height.toFloat())
            hostSizeDp = size

            val hostedId = settings.getPixelSearchbarWidgetId()
            if (settings.getPixelSearchbarType() != "widget" || hostedId == AppWidgetManager.INVALID_APPWIDGET_ID) return
            if (forwarded == hostedId to size) return
            forwarded = hostedId to size
            try {
                awm.updateAppWidgetOptions(
                    hostedId,
                    NestedWidgetSupport.sizedOptions(awm.getAppWidgetOptions(hostedId), width, height),
                )
                NestedWidgetSupport.requestUpdate(context, awm, hostedId)
            } catch (e: Exception) {
                Log.w("WidgetScraperService", "Failed to forward size to $hostedId", e)
            }
            instance?.let { service ->
                service.handler.post {
                    service.publishRemoteViews()
                    service.notifyWidgetChanged()
                }
            }
        }

        fun start(context: Context) {
            context.startService(Intent(context, WidgetScraperService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WidgetScraperService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settingsRepository: SettingsRepository
    private var appWidgetHost: ScrapingWidgetHost? = null
    private var scrapingHostView: ScrapingHostView? = null
    private var windowManager: WindowManager? = null
    private var isViewAttached = false
    private val handler = Handler(Looper.getMainLooper())

    // Music playback tracking components
    private var mediaSessionManager: MediaSessionManager? = null
    private var currentController: MediaController? = null
    private var musicBroadcastReceiver: MusicBroadcastReceiver? = null

    private val mediaCallback =
        object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                updateMediaMetadata(currentController)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                updateMediaMetadata(currentController)
            }
        }

    private val activeSessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateActiveSession(controllers)
        }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        instance = this
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val type = settingsRepository.getPixelSearchbarType()
        if (type == "widget") {
            bindAndListenWidget()
        } else if (type == "music") {
            listenToMusicSession()
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    private fun bindAndListenWidget() {
        // Clear any previous widget or media components before binding
        cleanupWidgetListener()
        cleanupMediaListener()

        val widgetId = settingsRepository.getPixelSearchbarWidgetId()
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            stopSelf()
            return
        }

        val awm = AppWidgetManager.getInstance(this)
        val host = ScrapingWidgetHost(this, HOST_ID)
        appWidgetHost = host
        host.startListening()

        val info =
            awm.getAppWidgetInfo(widgetId) ?: run {
                stopSelf()
                return
            }

        handler.post {
            try {
                if (appWidgetHost == null) return@post

                if (isViewAttached && scrapingHostView != null && windowManager != null) {
                    try {
                        windowManager?.removeView(scrapingHostView)
                    } catch (_: Exception) {
                    }
                    isViewAttached = false
                }

                val view = host.createView(this, widgetId, info) as? ScrapingHostView
                scrapingHostView = view

                if (Settings.canDrawOverlays(this)) {
                    val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    windowManager = wm
                    val params =
                        WindowManager.LayoutParams(
                            1,
                            1,
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                            PixelFormat.TRANSLUCENT,
                        ).apply {
                            gravity = Gravity.TOP or Gravity.START
                            x = -100
                            y = -100
                        }
                    if (view != null && wm != null && !isViewAttached) {
                        wm.addView(view, params)
                        isViewAttached = true
                    }
                }
                syncHostSize(this)
            } catch (e: Exception) {
                Log.e("WidgetScraperService", "Failed to create/attach ScrapingHostView", e)
            }
        }
    }

    private fun listenToMusicSession() {
        // Clear any widget hosting components
        cleanupWidgetListener()

        try {
            val manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            mediaSessionManager = manager
            if (manager != null) {
                val componentName = ComponentName(this, NotificationListener::class.java)
                val initialSessions = manager.getActiveSessions(componentName)
                updateActiveSession(initialSessions)
                manager.addOnActiveSessionsChangedListener(activeSessionsListener, componentName)
            }
        } catch (_: Exception) {
        }

        // Dynamically register the MusicBroadcastReceiver
        try {
            if (musicBroadcastReceiver == null) {
                val receiver = MusicBroadcastReceiver()
                musicBroadcastReceiver = receiver
                val filter =
                    android.content.IntentFilter().apply {
                        addAction("com.android.music.metadatachanged")
                        addAction("com.android.music.playstatechanged")
                        addAction("com.android.music.playbackcomplete")
                        addAction("com.android.music.queuechanged")
                        addAction("com.spotify.music.metadatachanged")
                        addAction("com.spotify.music.playbackstatechanged")
                        addAction("com.htc.music.metadatachanged")
                        addAction("com.real.music.metadatachanged")
                        addAction("com.sonyericsson.music.metadatachanged")
                        addAction("com.sec.android.app.music.metadatachanged")
                        addAction("com.sec.android.app.music.playstatechanged")
                        addAction("com.miui.player.metadatachanged")
                    }
                androidx.core.content.ContextCompat.registerReceiver(
                    this,
                    receiver,
                    filter,
                    androidx.core.content.ContextCompat.RECEIVER_EXPORTED,
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun updateActiveSession(controllers: List<MediaController>?) {
        val active =
            controllers
                ?.sortedWith(
                    compareByDescending<MediaController> {
                        val state = it.playbackState?.state
                        state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
                    }.thenByDescending {
                        val state = it.playbackState?.state
                        state == PlaybackState.STATE_PAUSED
                    },
                )?.firstOrNull()

        if (active != currentController) {
            currentController?.unregisterCallback(mediaCallback)
            currentController = active
            active?.registerCallback(mediaCallback)
            updateMediaMetadata(active)
        }
    }

    private fun updateMediaMetadata(controller: MediaController?) {
        if (controller == null) return
        val metadata = controller.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val packageName = controller.packageName

        val artwork =
            metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val filesDirFile = File(filesDir, "music_artwork.png")
        if (artwork != null) {
            try {
                FileOutputStream(filesDirFile).use { out ->
                    artwork.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            } catch (_: Exception) {
            }
        } else {
            if (filesDirFile.exists()) filesDirFile.delete()
        }

        settingsRepository.setPixelSearchbarMusicTitle(title)
        settingsRepository.setPixelSearchbarMusicArtist(artist)
        settingsRepository.setPixelSearchbarMusicPackage(packageName)
        notifyWidgetChanged()
    }

    private fun onRemoteViewsReceived(remoteViews: RemoteViews) {
        rawRemoteViews = remoteViews
        publishRemoteViews()
        notifyWidgetChanged()
    }

    private var checkedLayoutId: Int? = null

    private fun publishRemoteViews() {
        val raw = rawRemoteViews ?: return
        val resolved = NestedWidgetSupport.resolveSized(this, raw, hostSizeDp)
        if (checkedLayoutId == resolved.layoutId || NestedWidgetSupport.canRender(this, resolved)) {
            checkedLayoutId = resolved.layoutId
            currentRemoteViews = resolved
            isUnsupported = false
        } else {
            checkedLayoutId = null
            currentRemoteViews = null
            isUnsupported = true
        }
    }

    private var updatePending = false

    private fun notifyWidgetChanged() {
        if (updatePending) return
        updatePending = true

        handler.postDelayed({
            updatePending = false
            settingsRepository.incrementPixelSearchbarWidgetRevision()

            serviceScope.launch {
                runCatching {
                    val manager =
                        androidx.glance.appwidget.GlanceAppWidgetManager(this@WidgetScraperService)
                    val widget = PixelSearchbarWidget()
                    val glanceIds = manager.getGlanceIds(PixelSearchbarWidget::class.java)
                    for (glanceId in glanceIds) widget.update(this@WidgetScraperService, glanceId)
                }
            }
        }, 100L)
    }

    private fun cleanupWidgetListener() {
        handler.removeCallbacksAndMessages(null)
        if (isViewAttached && scrapingHostView != null && windowManager != null) {
            try {
                windowManager?.removeView(scrapingHostView)
            } catch (_: Exception) {
            }
            isViewAttached = false
        }
        scrapingHostView = null
        appWidgetHost?.stopListening()
        appWidgetHost = null
        currentRemoteViews = null
        rawRemoteViews = null
        checkedLayoutId = null
        isUnsupported = false
    }

    private fun cleanupMediaListener() {
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
        } catch (_: Exception) {
        }
        currentController?.unregisterCallback(mediaCallback)
        currentController = null
        mediaSessionManager = null

        // Dynamic unregistration
        try {
            musicBroadcastReceiver?.let {
                unregisterReceiver(it)
            }
            musicBroadcastReceiver = null
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
        cleanupWidgetListener()
        cleanupMediaListener()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
