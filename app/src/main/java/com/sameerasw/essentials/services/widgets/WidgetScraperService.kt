package com.sameerasw.essentials.services.widgets

import android.app.Service
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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

    private inner class ScrapingHostView(context: Context) : AppWidgetHostView(context) {
        
        private val drawListener = ViewTreeObserver.OnDrawListener {
            notifyWidgetChanged()
        }

        private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            notifyWidgetChanged()
        }

        override fun updateAppWidget(remoteViews: RemoteViews?) {
            super.updateAppWidget(remoteViews)
            if (remoteViews != null) onRemoteViewsReceived(remoteViews)
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            viewTreeObserver.addOnDrawListener(drawListener)
            viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
            setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
                    notifyWidgetChanged()
                }

                override fun onChildViewRemoved(parent: View?, child: View?) {
                    notifyWidgetChanged()
                }
            })
        }

        override fun onDetachedFromWindow() {
            viewTreeObserver.removeOnDrawListener(drawListener)
            viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
            setOnHierarchyChangeListener(null)
            super.onDetachedFromWindow()
        }
    }

    private inner class ScrapingWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?
        ): AppWidgetHostView = ScrapingHostView(context)
    }

    companion object {
        const val HOST_ID = 1025

        @Volatile
        var currentRemoteViews: RemoteViews? = null
            private set

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
    private val handler = Handler(Looper.getMainLooper())

    // Music playback tracking components
    private var mediaSessionManager: MediaSessionManager? = null
    private var currentController: MediaController? = null
    private var musicBroadcastReceiver: MusicBroadcastReceiver? = null

    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaMetadata(currentController)
        }
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaMetadata(currentController)
        }
    }

    private val activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveSession(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
        // Clear any media components
        cleanupMediaListener()

        val widgetId = settingsRepository.getPixelSearchbarWidgetId()
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { stopSelf(); return }

        val awm = AppWidgetManager.getInstance(this)
        val host = ScrapingWidgetHost(this, HOST_ID)
        appWidgetHost = host
        host.startListening()

        val info = awm.getAppWidgetInfo(widgetId) ?: run { stopSelf(); return }

        handler.post { host.createView(this, widgetId, info) }
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
        } catch (_: Exception) {}

        // Dynamically register the MusicBroadcastReceiver
        try {
            if (musicBroadcastReceiver == null) {
                val receiver = MusicBroadcastReceiver()
                musicBroadcastReceiver = receiver
                val filter = android.content.IntentFilter().apply {
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
                    androidx.core.content.ContextCompat.RECEIVER_EXPORTED
                )
            }
        } catch (_: Exception) {}
    }

    private fun updateActiveSession(controllers: List<MediaController>?) {
        val active = controllers?.sortedWith(
            compareByDescending<MediaController> { 
                val state = it.playbackState?.state
                state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
            }.thenByDescending {
                val state = it.playbackState?.state
                state == PlaybackState.STATE_PAUSED
            }
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

        val artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val filesDirFile = File(filesDir, "music_artwork.png")
        if (artwork != null) {
            try {
                FileOutputStream(filesDirFile).use { out ->
                    artwork.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            } catch (_: Exception) {}
        } else {
            if (filesDirFile.exists()) filesDirFile.delete()
        }

        settingsRepository.setPixelSearchbarMusicTitle(title)
        settingsRepository.setPixelSearchbarMusicArtist(artist)
        settingsRepository.setPixelSearchbarMusicPackage(packageName)
        notifyWidgetChanged()
    }

    private fun onRemoteViewsReceived(remoteViews: RemoteViews) {
        currentRemoteViews = remoteViews
        notifyWidgetChanged()
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
                    val manager = androidx.glance.appwidget.GlanceAppWidgetManager(this@WidgetScraperService)
                    val widget = PixelSearchbarWidget()
                    val glanceIds = manager.getGlanceIds(PixelSearchbarWidget::class.java)
                    for (glanceId in glanceIds) widget.update(this@WidgetScraperService, glanceId)
                }
            }
        }, 100L)
    }

    private fun cleanupWidgetListener() {
        appWidgetHost?.stopListening()
        appWidgetHost = null
        currentRemoteViews = null
    }

    private fun cleanupMediaListener() {
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(activeSessionsListener)
        } catch (_: Exception) {}
        currentController?.unregisterCallback(mediaCallback)
        currentController = null
        mediaSessionManager = null

        // Dynamic unregistration
        try {
            musicBroadcastReceiver?.let {
                unregisterReceiver(it)
            }
            musicBroadcastReceiver = null
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        cleanupWidgetListener()
        cleanupMediaListener()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
