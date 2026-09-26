package com.sameerasw.essentials.island.plugins.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.plugins.sendPendingIntent
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.IslandIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmPlugin : BaseIslandPlugin() {
    override val id = "alarm"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_ALARM,
        SettingsRepository.KEY_ISLAND_ALARM_WINDOW_HOURS,
    )

    private var registered = false
    private val recheck = Runnable { render() }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) = render()
    }

    override fun onStart() {
        try {
            context.registerReceiver(
                receiver,
                IntentFilter().apply {
                    addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                },
            )
            registered = true
        } catch (_: Exception) {
        }
    }

    override fun onStop() {
        ctx?.mainHandler?.removeCallbacks(recheck)
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
        render()
    }

    private fun render() {
        val c = ctx ?: return
        c.mainHandler.removeCallbacks(recheck)
        if (!settings.isIslandShowAlarmEnabled()) {
            publish(null)
            return
        }
        val windowMs = settings.getIslandAlarmWindowHours() * HOUR_MS
        val info = NextAlarm.get(context)
        val now = System.currentTimeMillis()
        if (info == null || info.triggerTime <= now) {
            publish(null)
            return
        }
        val untilWindow = info.triggerTime - windowMs - now
        if (untilWindow > 0) {
            c.mainHandler.postDelayed(recheck, untilWindow + 1_000L)
            publish(null)
            return
        }
        c.mainHandler.postDelayed(recheck, info.triggerTime - now + 60_000L)
        val shortTime = NextAlarm.format(context, info.triggerTime, withPeriod = false)
        val fullTime = NextAlarm.format(context, info.triggerTime, withPeriod = true)
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.ALARM,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("alarm.icon") { IslandIcon(R.drawable.rounded_alarm_24, size = 18.dp, tint = MaterialTheme.colorScheme.primary) },
                    CompactCell("alarm.time") { Text(shortTime, style = IslandTextStyles.compact) },
                ),
                line = LineContent(
                    icon = { IslandIcon(R.drawable.rounded_alarm_24, tint = MaterialTheme.colorScheme.primary) },
                    start = context.getString(R.string.island_show_alarm_title),
                    end = fullTime,
                ),
                onOpen = { sendPendingIntent(context, info.showIntent) },
            ),
        )
    }

    companion object {
        const val ITEM_KEY = "alarm"
        private const val HOUR_MS = 3_600_000L
    }
}

object NextAlarm {
    fun get(context: Context): AlarmManager.AlarmClockInfo? =
        try {
            context.getSystemService(AlarmManager::class.java)?.nextAlarmClock
        } catch (_: Exception) {
            null
        }

    fun within(context: Context, hours: Int): AlarmManager.AlarmClockInfo? {
        val info = get(context) ?: return null
        val delta = info.triggerTime - System.currentTimeMillis()
        return info.takeIf { delta in 0..hours * 3_600_000L }
    }

    fun format(context: Context, millis: Long, withPeriod: Boolean): String {
        val pattern = when {
            DateFormat.is24HourFormat(context) -> "HH:mm"
            withPeriod -> "h:mm a"
            else -> "h:mm"
        }
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }
}
