package com.sameerasw.essentials.island.plugins.alarm

import com.sameerasw.essentials.island.ui.components.islandAccent

import com.sameerasw.essentials.island.IslandTimeFormat
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.plugins.sendPendingIntent
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.ConnectedTextLabel
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.MarqueeText
import com.sameerasw.essentials.utils.CalendarEventUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmPlugin : BaseIslandPlugin() {
    override val id = "alarm"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_ALARM,
        SettingsRepository.KEY_ISLAND_ALARM_WINDOW_HOURS,
        SettingsRepository.KEY_ISLAND_ALARM_HIDE_TIME,
        SettingsRepository.KEY_ISLAND_ALARM_SHOW_REMAINING,
        SettingsRepository.KEY_ISLAND_TIME_FORMAT,
        SettingsRepository.KEY_ISLAND_TIME_LEADING_ZERO,
        SettingsRepository.KEY_ISLAND_TIME_PERIOD,
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
        val untilAlarm = info.triggerTime - now
        val shortTime = NextAlarm.format(context, info.triggerTime, withPeriod = false)
        val fullTime = NextAlarm.format(context, info.triggerTime, withPeriod = true)
        val hideTime = settings.getBoolean(SettingsRepository.KEY_ISLAND_ALARM_HIDE_TIME, false)
        val remaining = if (settings.isIslandAlarmShowRemainingEnabled()) {
            CalendarEventUtil.formatRelativeTimeCompact(info.triggerTime, now)
        } else null
        c.mainHandler.postDelayed(recheck, untilAlarm % 60_000L + 50L)
        val title = context.getString(R.string.island_show_alarm_title)
        val remainingFull = CalendarEventUtil.formatRelativeTime(context, info.triggerTime, now)
        val compactText = remaining ?: shortTime.takeUnless { hideTime }
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.ALARM,
                placement = CompactPlacement.Dynamic,
                compact = buildList {
                    add(CompactCell("alarm.icon") { IslandIcon(R.drawable.rounded_alarm_24, size = 18.dp, tint = islandAccent()) })
                    if (compactText != null) add(CompactCell("alarm.time") { Text(compactText, style = IslandTextStyles.compact) })
                },
                line = LineContent(
                    icon = { IslandIcon(R.drawable.rounded_alarm_24, tint = islandAccent()) },
                    start = title,
                    end = listOfNotNull(
                        fullTime.takeUnless { hideTime },
                        remaining?.let { context.getString(R.string.status_glance_calendar_event_in_time, it) },
                    ).joinToString(" · "),
                ),
                expanded = ExpandedContent { scope ->
                    AlarmExpanded(title, fullTime, remainingFull, scope) { sendPendingIntent(context, info.showIntent) }
                },
                onOpen = { sendPendingIntent(context, info.showIntent) },
            ),
        )
    }

    @Composable
    private fun AlarmExpanded(title: String, time: String, remaining: String, scope: IslandExpandedScope, open: () -> Unit) {
        val spec = scope.spec
        val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
        Box(propagateMinConstraints = true) {
            Column(Modifier.fillMaxWidth().padding(spec.expandedOutset)) {
                Spacer(Modifier.height(spec.expandedTopPadding))
                scope.CameraRow(
                    horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                    start = {
                        IslandIcon(R.drawable.rounded_alarm_24, size = 20.dp, tint = islandAccent())
                        MarqueeText(text = title, style = IslandTextStyles.title, modifier = Modifier.weight(1f))
                    },
                    end = { Text(time, style = IslandTextStyles.title) },
                )
                Spacer(Modifier.height(4.dp))
                MarqueeText(
                    text = remaining,
                    style = IslandTextStyles.body,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                )
                Spacer(Modifier.height(16.dp))
                ConnectedButtonRow(
                    height = 44.dp,
                    modifier = Modifier.padding(horizontal = sidePadding).padding(bottom = spec.expandedBottomPadding),
                    container = Color.White.copy(alpha = 0.2f),
                    items = listOf(
                        ConnectedItem({
                            open()
                            scope.collapse()
                        }) { ConnectedTextLabel(stringResource(R.string.island_progress_open)) },
                    ),
                )
            }
        }
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
        return IslandTimeFormat.format(context, SettingsRepository(context), millis, period = withPeriod)
    }
}
