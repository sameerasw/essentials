package com.sameerasw.essentials.island.plugins.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.Rating
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.SystemClock
import com.sameerasw.essentials.services.NotificationListener
import java.io.File
import kotlin.math.abs

class MediaSessionSource(
    private val context: Context,
    private val handler: Handler,
    private val onChanged: () -> Unit,
) {
    private val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    private val controllers = mutableListOf<MediaController>()
    private val callbacks = mutableMapOf<MediaSession.Token, MediaController.Callback>()
    private var sessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var lastChange = 0L
    private val debounced = Runnable { onChanged() }

    val all: List<MediaController> get() = controllers

    fun start() {
        val mgr = manager ?: return
        if (sessionsListener != null) return
        try {
            val component = ComponentName(context, NotificationListener::class.java)
            val listener = MediaSessionManager.OnActiveSessionsChangedListener { update(it) }
            sessionsListener = listener
            mgr.addOnActiveSessionsChangedListener(listener, component, handler)
            update(mgr.getActiveSessions(component))
            onChanged()
        } catch (_: Exception) {
        }
    }

    fun stop() {
        handler.removeCallbacks(debounced)
        try {
            sessionsListener?.let { manager?.removeOnActiveSessionsChangedListener(it) }
        } catch (_: Exception) {
        }
        sessionsListener = null
        callbacks.forEach { (token, cb) ->
            controllers.find { it.sessionToken == token }?.let {
                try {
                    it.unregisterCallback(cb)
                } catch (_: Exception) {
                }
            }
        }
        callbacks.clear()
        controllers.clear()
    }

    private fun update(list: List<MediaController>?) {
        val valid = list.orEmpty()
        val tokens = valid.map { it.sessionToken }.toSet()
        callbacks.entries.removeAll { (token, cb) ->
            if (token in tokens) return@removeAll false
            controllers.find { it.sessionToken == token }?.let {
                try {
                    it.unregisterCallback(cb)
                } catch (_: Exception) {
                }
            }
            true
        }
        controllers.removeAll { it.sessionToken !in tokens }
        valid.forEach { controller ->
            if (callbacks.containsKey(controller.sessionToken)) return@forEach
            val cb = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = schedule()
                override fun onMetadataChanged(metadata: MediaMetadata?) = schedule()
                override fun onSessionDestroyed() = schedule()
            }
            try {
                controller.registerCallback(cb, handler)
                callbacks[controller.sessionToken] = cb
                controllers.add(controller)
            } catch (_: Exception) {
            }
        }
        schedule()
    }

    private fun schedule() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastChange < DEBOUNCE_MS) return
        lastChange = now
        handler.removeCallbacks(debounced)
        handler.postDelayed(debounced, DEBOUNCE_MS)
    }

    companion object {
        private const val DEBOUNCE_MS = 400L

        fun position(controller: MediaController): Float {
            val state = controller.playbackState ?: return 0f
            val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            if (duration <= 0L) return 0f
            val elapsed = if (state.state == PlaybackState.STATE_PLAYING) {
                (SystemClock.elapsedRealtime() - state.lastPositionUpdateTime) * state.playbackSpeed
            } else {
                0f
            }
            return ((state.position + elapsed) / duration.toFloat()).coerceIn(0f, 1f)
        }

        fun isLiked(controller: MediaController): Boolean {
            try {
                val rating = controller.metadata?.getRating(MediaMetadata.METADATA_KEY_USER_RATING)
                if (rating != null && rating.isRated) {
                    val liked = rating.hasHeart() || rating.isThumbUp ||
                        (rating.ratingStyle == Rating.RATING_PERCENTAGE && rating.percentRating >= 50)
                    if (liked) return true
                }
                controller.playbackState?.customActions?.forEach { action ->
                    val name = action.name?.toString().orEmpty()
                    if (name.contains("Unheart", true) || name.contains("Unlike", true) || name.contains("Remove from", true)) {
                        return true
                    }
                }
            } catch (_: Exception) {
            }
            return false
        }

        fun artwork(context: Context, metadata: MediaMetadata?): Bitmap? {
            metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)?.let { return it }
            metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)?.let { return it }
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            if (!title.isNullOrBlank()) {
                val hash = abs("${title}_$artist".hashCode().toLong())
                NotificationListener.getCachedBitmap(hash)?.let { return it }
                decode(File(context.cacheDir, "art_$hash.png"))?.let { return it }
            }
            NotificationListener.getLatestArtBitmap()?.let { return it }
            return decode(File(context.cacheDir, "temp_album_art.png"))
        }

        private fun decode(file: File): Bitmap? = if (file.exists()) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }
}
