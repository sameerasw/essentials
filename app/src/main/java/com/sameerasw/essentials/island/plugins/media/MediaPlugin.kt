package com.sameerasw.essentials.island.plugins.media

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.compose.ui.graphics.Color
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.plugins.accentFrom
import com.sameerasw.essentials.island.plugins.launchPackage
import com.sameerasw.essentials.island.plugins.sendPendingIntent
import com.sameerasw.essentials.island.ui.components.EqualizerBars
import com.sameerasw.essentials.island.ui.components.IslandBitmap
import com.sameerasw.essentials.services.NotificationListener
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaPlugin : BaseIslandPlugin() {
    override val id = "media"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_MEDIA,
        SettingsRepository.KEY_ISLAND_MEDIA_EXCLUDED_APPS,
    )

    private data class Track(
        val key: String,
        val title: String,
        val artist: String,
        val artwork: Bitmap?,
        val accent: Int?,
    )

    private var source: MediaSessionSource? = null
    private var active: MediaController? = null
    private var track: Track? = null
    private var playing = false
    private var liked = false
    private val pausedGrace = Runnable {
        active = null
        track = null
        render()
    }

    override fun onStart() {
        source = MediaSessionSource(context, ctx!!.mainHandler) { evaluate() }.also { it.start() }
    }

    override fun onStop() {
        ctx?.mainHandler?.removeCallbacks(pausedGrace)
        source?.stop()
        source = null
        active = null
        track = null
    }

    override fun refresh() = evaluate()

    private fun evaluate() {
        val c = ctx ?: return
        if (!settings.isIslandShowMediaEnabled()) {
            c.mainHandler.removeCallbacks(pausedGrace)
            active = null
            track = null
            render()
            return
        }
        val excluded = settings.loadIslandMediaExcludedApps().filter { it.isEnabled }.map { it.packageName }.toSet()
        val controllers = source?.all.orEmpty()
        val playingController = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING && it.packageName !in excluded
        }

        if (playingController != null) {
            c.mainHandler.removeCallbacks(pausedGrace)
            active = playingController
            playing = true
            liked = MediaSessionSource.isLiked(playingController)
            val metadata = playingController.metadata
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
            if (title.isBlank() && artist.isBlank()) return
            val key = "${playingController.packageName}_${title}_$artist"
            if (track?.key == key) {
                render()
                return
            }
            val isNewTrack = track != null
            c.scope.launch {
                val (art, accent) = withContext(Dispatchers.IO) {
                    val bmp = MediaSessionSource.artwork(context, metadata)
                    bmp to accentFrom(bmp)
                }
                if (active?.sessionToken != playingController.sessionToken) return@launch
                track = Track(key, title, artist, art, accent)
                render()
                if (isNewTrack && settings.isIslandMediaPeekSongChangeEnabled()) {
                    c.request(PluginRequest.Peek(ITEM_KEY, settings.getIslandPeekDurationMs()))
                }
            }
            return
        }

        val current = active
        val paused = current != null && track != null &&
            controllers.any { it.sessionToken == current.sessionToken } &&
            current.packageName !in excluded &&
            current.playbackState?.state == PlaybackState.STATE_PAUSED
        if (paused) {
            playing = false
            render()
            c.mainHandler.removeCallbacks(pausedGrace)
            c.mainHandler.postDelayed(pausedGrace, PAUSED_GRACE_MS)
        } else {
            c.mainHandler.removeCallbacks(pausedGrace)
            active = null
            track = null
            render()
        }
    }

    private fun render() {
        val t = track
        val controller = active
        if (t == null || controller == null) {
            publish(null)
            return
        }
        val accent = t.accent?.let { Color(it) } ?: Color.White
        val isPlaying = playing
        val isLiked = liked
        val actions = MediaActions(
            playPause = { togglePlay() },
            next = { active?.transportControls?.skipToNext() },
            previous = { active?.transportControls?.skipToPrevious() },
            like = { like() },
            progress = { active?.let(MediaSessionSource::position) ?: 0f },
        )
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.MEDIA,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("media.art") { IslandBitmap(t.artwork, 22.dp, circle = true) },
                    CompactCell("media.eq") { EqualizerBars(isPlaying, accent) },
                ),
                line = LineContent(
                    icon = { IslandBitmap(t.artwork, 24.dp, circle = true) },
                    start = t.artist,
                    end = t.title,
                    endSlot = { EqualizerBars(isPlaying, accent) },
                ),
                expanded = ExpandedContent { scope ->
                    MediaExpanded(t.title, t.artist, t.artwork, accent, isPlaying, isLiked, actions, scope)
                },
                accent = accent,
                onOpen = { openPlayer() },
            ),
        )
    }

    private fun togglePlay() {
        val controller = active ?: return
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    private fun like() {
        context.sendBroadcast(Intent(NotificationListener.ACTION_LIKE_CURRENT_SONG).setPackage(context.packageName))
        ctx?.mainHandler?.postDelayed({
            active?.let {
                liked = MediaSessionSource.isLiked(it)
                render()
            }
        }, 400L)
    }

    private fun openPlayer() {
        val controller = active ?: return
        if (!sendPendingIntent(context, controller.sessionActivity)) launchPackage(context, controller.packageName)
    }

    companion object {
        const val ITEM_KEY = "media"
        private const val PAUSED_GRACE_MS = 3000L
    }
}
