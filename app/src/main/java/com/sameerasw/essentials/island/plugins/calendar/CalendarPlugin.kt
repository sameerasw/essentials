package com.sameerasw.essentials.island.plugins.calendar

import android.app.KeyguardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.essentials.utils.CalendarEventUtil
import com.sameerasw.essentials.utils.UpcomingCalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarPlugin : BaseIslandPlugin() {
    override val id = "calendar"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_CALENDAR,
        SettingsRepository.KEY_ISLAND_SHOW_GLOW,
        SettingsRepository.KEY_ISLAND_CALENDAR_EMOJIS,
        SettingsRepository.KEY_ISLAND_CALENDAR_PRIORITY_MINUTES,
        SettingsRepository.KEY_ISLAND_CALENDAR_HIDE_LOCKED,
    )

    private var event: UpcomingCalendarEvent? = null
    private val poll = Runnable { refresh() }

    override fun onStop() {
        ctx?.mainHandler?.removeCallbacks(poll)
        event = null
    }

    override fun refresh() {
        val c = ctx ?: return
        c.mainHandler.removeCallbacks(poll)
        c.mainHandler.postDelayed(poll, POLL_MS)
        if (!settings.isIslandShowCalendarEnabled()) {
            event = null
            render()
            return
        }
        c.scope.launch {
            val next = withContext(Dispatchers.IO) {
                val ids = settings.getStatusGlanceCalendarSelectedCalendars().mapNotNull { it.toLongOrNull() }.toSet()
                CalendarEventUtil.queryNextUpcomingEvent(
                    context,
                    settings.getStatusGlanceCalendarTimeframe(),
                    ids,
                    settings.isStatusGlanceCalendarShowAllDayEnabled(),
                )
            }
            event = next
            render()
        }
    }

    private fun render() {
        val e = event
        val hiddenWhileLocked = settings.getBoolean(SettingsRepository.KEY_ISLAND_CALENDAR_HIDE_LOCKED, false) &&
            (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true
        if (e == null || ctx == null || hiddenWhileLocked) {
            publish(null)
            return
        }
        val now = System.currentTimeMillis()
        val full = CalendarEventUtil.formatRelativeTime(context, e.startTimeMillis, now)
        val short = CalendarEventUtil.formatRelativeTimeCompact(e.startTimeMillis, now)
        val showGlow = settings.isIslandShowGlowEnabled()
        val emoji = settings.getIslandCalendarEmojis()[e.calendarId]
        val priorityMs = settings.getIslandCalendarPriorityMinutes() * 60_000L
        val urgent = priorityMs > 0 && e.startTimeMillis - now in 0..priorityMs
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.CALENDAR,
                priorityOverride = IslandPriority.CALENDAR_OVERRIDE.takeIf { urgent },
                bypassLauncherOnly = urgent,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("cal.icon") { CalendarGlyph(emoji, size = 18.dp, tint = MaterialTheme.colorScheme.primary) },
                    CompactCell("cal.time") { RollingText(short) },
                ),
                line = LineContent(
                    icon = { CalendarGlyph(emoji, size = 20.dp, tint = MaterialTheme.colorScheme.primary) },
                    start = e.title,
                    end = full,
                ),
                expanded = ExpandedContent { scope ->
                    CalendarExpanded(
                        event = e,
                        emoji = emoji,
                        relative = full,
                        countdown = short,
                        showGlow = showGlow,
                        onView = {
                            openEvent(e)
                            scope.collapse()
                        },
                        scope = scope,
                    )
                },
                onOpen = { openEvent(e) },
            ),
        )
    }

    private fun openEvent(e: UpcomingCalendarEvent) {
        val intent = if (e.eventId >= 0) {
            Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, e.eventId))
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, e.startTimeMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, e.endTimeMillis)
        } else {
            Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(e.startTimeMillis.toString()).build())
        }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    companion object {
        const val ITEM_KEY = "calendar"
        private const val POLL_MS = 60_000L
    }
}
