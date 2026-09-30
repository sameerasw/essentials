package com.sameerasw.essentials.island.gestures

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.utils.LinearBrightness
import com.sameerasw.essentials.utils.media.MusicSessionUtil
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CompactGestureController(
    private val context: Context,
    private val settings: SettingsRepository,
    private val scope: () -> CoroutineScope?,
    private val openBrief: () -> Unit,
) : CompactGestures {
    private val brightness by lazy { LinearBrightness(context) }
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    private val musicPlaying: Boolean
        get() {
            val excluded = settings.loadIslandMediaExcludedApps().filter { it.isEnabled }.map { it.packageName }.toSet()
            return MusicSessionUtil.isPlayingMusic(context, excluded)
        }

    private val likeWhilePlaying get() = settings.isIslandLikeWhilePlayingEnabled() && musicPlaying

    override val longPressOpensBrief get() = settings.isIslandBriefEnabled() && !likeWhilePlaying

    override val hasLongPress get() = settings.isIslandBriefEnabled() || settings.getIslandLongPressAction() != null || likeWhilePlaying

    override val slideMode: SlideMode
        get() {
            if (settings.isIslandSlideTrackEnabled() && musicPlaying) return SlideMode.Track
            return when (settings.getIslandSlideMode()) {
                "volume" -> SlideMode.Volume
                "brightness" -> SlideMode.Brightness
                "sound_mode" -> SlideMode.SoundMode
                else -> SlideMode.None
            }
        }

    override fun longPress() {
        if (likeWhilePlaying) {
            scope()?.let(IslandMediaCue::flashLiked)
            execute(Action.LikeCurrentSong)
            return
        }
        if (settings.isIslandBriefEnabled()) {
            openBrief()
            return
        }
        settings.getIslandLongPressAction()?.let(::execute)
    }

    override fun slideStep(forward: Boolean) {
        when (slideMode) {
            SlideMode.Volume -> audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (forward) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI,
            )
            else -> {}
        }
    }

    override fun slideBegin() {
        if (slideMode == SlideMode.Brightness) brightness.begin()
    }

    override fun slideTo(dx: Float, rangePx: Float): Int = brightness.slide(dx, rangePx)

    override fun slideCommit(dx: Float) {
        val forward = forward(dx)
        when (slideMode) {
            SlideMode.Track -> mediaKey(if (forward) KeyEvent.KEYCODE_MEDIA_NEXT else KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            SlideMode.SoundMode -> cycleSoundMode(forward)
            else -> {}
        }
    }

    override fun levelPercent(): Int = when (slideMode) {
        SlideMode.Volume -> {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max
        }
        SlideMode.Brightness -> brightness.percent()
        else -> 0
    }

    override fun soundMode(): RingMode = when (audioManager.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> RingMode.Silent
        AudioManager.RINGER_MODE_VIBRATE -> RingMode.Vibrate
        else -> RingMode.Normal
    }

    override fun soundModeAfter(dx: Float): RingMode {
        val order = listOf(RingMode.Normal, RingMode.Vibrate, RingMode.Silent)
        val index = order.indexOf(soundMode()).coerceAtLeast(0)
        return order[(index + if (forward(dx)) 1 else order.size - 1) % order.size]
    }

    override fun trackForward(dx: Float): Boolean = forward(dx)

    private fun forward(dx: Float): Boolean {
        val inverted = settings.isIslandSlideInvertDirectionEnabled()
        return if (inverted) dx < 0f else dx > 0f
    }

    private fun cycleSoundMode(forward: Boolean) {
        try {
            val order = listOf(AudioManager.RINGER_MODE_NORMAL, AudioManager.RINGER_MODE_VIBRATE, AudioManager.RINGER_MODE_SILENT)
            val index = order.indexOf(audioManager.ringerMode).coerceAtLeast(0)
            audioManager.ringerMode = order[(index + if (forward) 1 else order.size - 1) % order.size]
        } catch (_: Exception) {
        }
    }

    private fun mediaKey(code: Int) {
        try {
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        } catch (_: Exception) {
        }
    }

    private fun execute(action: Action) {
        scope()?.launch {
            try {
                CombinedActionExecutor.execute(context, action)
            } catch (_: Exception) {
            }
        }
    }
}
