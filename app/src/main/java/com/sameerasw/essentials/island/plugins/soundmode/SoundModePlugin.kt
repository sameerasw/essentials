package com.sameerasw.essentials.island.plugins.soundmode

import com.sameerasw.essentials.island.ui.components.islandAccent

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.controller.ZenModeController
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.utils.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundModePlugin : BaseIslandPlugin() {
    override val id = "sound_mode"

    override val settingKeys = setOf(SettingsRepository.KEY_ISLAND_SHOW_SOUND_MODE, SettingsRepository.KEY_ISLAND_SOUND_MODE_KEEP_ICON)

    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private var mode = AudioManager.RINGER_MODE_NORMAL
    private var zenActive = false
    private var zenName: String? = null
    private var zenType = AutomaticZenRule.TYPE_UNKNOWN
    private var registered = false
    private var announcing = false
    private val endAnnounce = Runnable {
        announcing = false
        render()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) {
                readZen()
                return
            }
            val previous = mode
            mode = audioManager.ringerMode
            if (zenActive || isZenOn()) render() else if (mode != previous) announce() else render()
        }
    }

    override fun onStart() {
        mode = audioManager.ringerMode
        zenActive = isZenOn()
        try {
            context.registerReceiver(
                receiver,
                IntentFilter().apply {
                    addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                    addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
                },
            )
            registered = true
        } catch (_: Exception) {
        }
        readZen(announceChange = false)
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
        readZen(announceChange = false)
    }

    private fun isZenOn(): Boolean {
        val filter = notificationManager.currentInterruptionFilter
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    private fun readZen(announceChange: Boolean = true) {
        val c = ctx ?: return
        val wasActive = zenActive
        val previousName = zenName
        zenActive = isZenOn()
        if (!zenActive) {
            zenName = null
            zenType = AutomaticZenRule.TYPE_UNKNOWN
            if (announceChange && wasActive) announce() else render()
            return
        }
        render()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM || !ShellUtils.hasPermission(context)) {
            if (announceChange && !wasActive) announce()
            return
        }
        c.scope.launch(Dispatchers.IO) {
            val active = runCatching { ZenModeController.activeModes() }.getOrDefault(emptyList()).firstOrNull()
            c.mainHandler.post {
                zenName = active?.name?.takeIf { it.isNotBlank() }
                zenType = active?.type ?: AutomaticZenRule.TYPE_UNKNOWN
                if (announceChange && (!wasActive || zenName != previousName)) announce() else render()
            }
        }
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

    private fun zenIcon(): Int = when (zenType) {
        AutomaticZenRule.TYPE_BEDTIME -> R.drawable.rounded_bedtime_24
        AutomaticZenRule.TYPE_DRIVING -> R.drawable.rounded_directions_car_24
        AutomaticZenRule.TYPE_IMMERSIVE, AutomaticZenRule.TYPE_THEATER -> R.drawable.rounded_center_focus_strong_24
        AutomaticZenRule.TYPE_SCHEDULE_TIME, AutomaticZenRule.TYPE_SCHEDULE_CALENDAR -> R.drawable.rounded_schedule_24
        else -> R.drawable.rounded_do_not_disturb_on_24
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
        if (!zenActive && mode == AudioManager.RINGER_MODE_NORMAL && !announcing) {
            publish(null)
            return
        }
        if (!announcing && !settings.isIslandSoundModeKeepIconEnabled()) {
            publish(null)
            return
        }
        val icon = if (zenActive) zenIcon() else iconFor(mode)
        val title = if (zenActive) zenName ?: context.getString(R.string.island_sound_mode_mode_fallback) else context.getString(R.string.island_show_sound_mode_title)
        val state = if (zenActive) context.getString(R.string.on) else context.getString(labelFor(mode))
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.SOUND_MODE,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("sound_mode.icon") { IslandIcon(icon, size = 18.dp, tint = islandAccent()) },
                ),
                line = LineContent(
                    icon = { IslandIcon(icon, tint = islandAccent()) },
                    start = title,
                    end = state,
                ),
            ),
        )
    }

    companion object {
        const val ITEM_KEY = "sound_mode"
    }
}
