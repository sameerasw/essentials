package com.sameerasw.essentials.services.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.sameerasw.essentials.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MusicBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsRepository(context)
        val type = settings.getPixelSearchbarType()
        if (type != "music") return

        // Update media info from the current active session
        try {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
            val componentName = android.content.ComponentName(context, com.sameerasw.essentials.services.NotificationListener::class.java)
            val sessions = manager.getActiveSessions(componentName)
            val activeSession = sessions?.sortedWith(
                compareByDescending<MediaController> { 
                    val state = it.playbackState?.state
                    state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
                }.thenByDescending {
                    val state = it.playbackState?.state
                    state == PlaybackState.STATE_PAUSED
                }
            )?.firstOrNull()

            if (activeSession != null) {
                val metadata = activeSession.metadata
                val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                val packageName = activeSession.packageName

                val artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

                val filesDirFile = File(context.filesDir, "music_artwork.png")
                if (artwork != null) {
                    try {
                        FileOutputStream(filesDirFile).use { out ->
                            artwork.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    } catch (_: Exception) {}
                } else {
                    if (filesDirFile.exists()) filesDirFile.delete()
                }

                settings.setPixelSearchbarMusicTitle(title)
                settings.setPixelSearchbarMusicArtist(artist)
                settings.setPixelSearchbarMusicPackage(packageName)
                settings.incrementPixelSearchbarWidgetRevision()

                // Trigger immediate Glance widget redraw
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        val managerGlance = androidx.glance.appwidget.GlanceAppWidgetManager(context)
                        val widgetGlance = PixelSearchbarWidget()
                        val glanceIds = managerGlance.getGlanceIds(PixelSearchbarWidget::class.java)
                        for (glanceId in glanceIds) {
                            widgetGlance.update(context, glanceId)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
