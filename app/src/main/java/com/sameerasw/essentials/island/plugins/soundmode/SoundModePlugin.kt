package com.sameerasw.essentials.island.plugins.soundmode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.components.IslandIcon

class SoundModePlugin : BaseIslandPlugin() {
    override val id = "sound_mode"

    override val settingKeys = setOf(SettingsRepository.KEY_ISLAND_SHOW_SOUND_MODE, SettingsRepository.KEY_ISLAND_SOUND_MODE_KEEP_ICON)

    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private var mode = AudioManager.RINGER_MODE_NORMAL
    private var registered = false
    private var announcing = false
    private val endAnnounce = Runnable {
        announcing = false
        render()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val previous = mode
            mode = audioManager.ringerMode
            if (mode != previous) announce() else render()
        }
    }

    override fun onStart() {
        mode = audioManager.ringerMode
        try {
            context.registerReceiver(receiver, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))
            registered = true
        } catch (_: Exception) {
        }
    }

    override fun onStop() {
        ctx?.mainHandler?.removeCallbacks(endAnnounce)
        announcing = false
        if (registered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
            registered = false
        }
    }

    override fun refresh() {
        ctx ?: return
        mode = audioManager.ringerMode
        render()
    }

    private fun announce() {
        val c = ctx ?: return
        if (!settings.isIslandShowSoundModeEnabled()) {
            render()
            return
        }
        val duration = settings.getIslandPeekDurationMs()
        announcing = true
        render()
        c.request(PluginRequest.Peek(ITEM_KEY, duration))
        c.mainHandler.removeCallbacks(endAnnounce)
        c.mainHandler.postDelayed(endAnnounce, duration + 500L)
    }

    private fun iconFor(mode: Int): Int = when (mode) {
        AudioManager.RINGER_MODE_VIBRATE -> R.drawable.rounded_mobile_vibrate_24
        AudioManager.RINGER_MODE_SILENT -> R.drawable.rounded_volume_off_24
        else -> R.drawable.rounded_volume_up_24
    }

    private fun labelFor(mode: Int): Int = when (mode) {
        AudioManager.RINGER_MODE_VIBRATE -> R.string.island_sound_mode_vibrate
        AudioManager.RINGER_MODE_SILENT -> R.string.island_sound_mode_silent
        else -> R.string.island_sound_mode_sound
    }

    private fun render() {
        if (ctx == null || !settings.isIslandShowSoundModeEnabled()) {
            publish(null)
            return
        }
        if (mode == AudioManager.RINGER_MODE_NORMAL && !announcing) {
            publish(null)
            return
        }
        if (!announcing && !settings.isIslandSoundModeKeepIconEnabled()) {
            publish(null)
            return
        }
        val icon = iconFor(mode)
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.SOUND_MODE,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("sound_mode.icon") { IslandIcon(icon, size = 18.dp, tint = MaterialTheme.colorScheme.primary) },
                ),
                line = LineContent(
                    icon = { IslandIcon(icon, tint = MaterialTheme.colorScheme.primary) },
                    start = context.getString(R.string.island_show_sound_mode_title),
                    end = context.getString(labelFor(mode)),
                ),
            ),
        )
    }

    companion object {
        const val ITEM_KEY = "sound_mode"
    }
}
