/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: LiveWallpaperMediaTracker.kt
 * Description: Follows active media sessions to supply album art to the live wallpaper.
 */

package com.sameerasw.essentials.services

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import com.sameerasw.essentials.data.repository.SettingsRepository

class LiveWallpaperMediaTracker(
    private val context: Context,
    private val repository: SettingsRepository,
    private val onChanged: (art: Bitmap?, artKey: String?, playing: Boolean) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val component = ComponentName(context, NotificationListener::class.java)
    private val sessionManager by lazy { context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager }
    private val callbacks = mutableMapOf<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()
    private var running = false

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers -> updateControllers(controllers) }

    fun start() {
        if (running) return
        val manager = sessionManager ?: return
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
            running = true
            updateControllers(manager.getActiveSessions(component))
        } catch (_: SecurityException) {
            running = false
        }
    }

    fun stop() {
        if (!running) return
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (_: Exception) {
        }
        callbacks.values.forEach { (controller, callback) ->
            try {
                controller.unregisterCallback(callback)
            } catch (_: Exception) {
            }
        }
        callbacks.clear()
        running = false
    }

    fun refresh() {
        if (running) evaluate()
    }

    private fun updateControllers(controllers: List<MediaController>?) {
        val current = controllers.orEmpty()
        val tokens = current.map { it.sessionToken }.toSet()
        callbacks.keys.filter { it !in tokens }.forEach { token ->
            callbacks.remove(token)?.let { (controller, callback) ->
                try {
                    controller.unregisterCallback(callback)
                } catch (_: Exception) {
                }
            }
        }
        current.filter { it.sessionToken !in callbacks }.forEach { controller ->
            val callback =
                object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) = evaluate()

                    override fun onMetadataChanged(metadata: MediaMetadata?) = evaluate()

                    override fun onSessionDestroyed() {
                        callbacks.remove(controller.sessionToken)
                        evaluate()
                    }
                }
            try {
                controller.registerCallback(callback, handler)
                callbacks[controller.sessionToken] = controller to callback
            } catch (_: Exception) {
            }
        }
        evaluate()
    }

    private fun evaluate() {
        val excluded =
            repository
                .loadAodWallpaperMediaExcludedApps()
                .filter { it.isEnabled }
                .map { it.packageName }
                .toSet()
        val playing =
            callbacks.values
                .map { it.first }
                .firstOrNull { it.packageName !in excluded && it.playbackState?.state == PlaybackState.STATE_PLAYING }

        if (playing == null) {
            onChanged(null, null, false)
            return
        }
        val metadata = playing.metadata
        val art =
            metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: NotificationListener.getLatestArtBitmap()
        val key =
            "${playing.packageName}|${metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)}|" +
                "${metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)}|${metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)}"
        onChanged(art, key, true)
    }
}
