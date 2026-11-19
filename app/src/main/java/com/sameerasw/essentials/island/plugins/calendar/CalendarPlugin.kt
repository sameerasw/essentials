package com.sameerasw.essentials.island.plugins.calendar

import com.sameerasw.essentials.island.ui.components.islandAccent

import com.sameerasw.essentials.island.IslandTimeFormat
import android.app.KeyguardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
        SettingsRepository.KEY_ISLAND_CALENDAR_ICONS,
        SettingsRepository.KEY_ISLAND_CALENDAR_PRIORITY_MINUTES,
        SettingsRepository.KEY_ISLAND_CALENDAR_HIDE_LOCKED,
        SettingsRepository.KEY_ISLAND_CALENDAR_COMPACT_TIME,
        SettingsRepository.KEY_ISLAND_CALENDAR_ICON_ONLY,
        SettingsRepository.KEY_ISLAND_CALENDAR_HIDE_NOW,
        SettingsRepository.KEY_ISLAND_CALENDAR_SHOW_ONGOING,
        SettingsRepository.KEY_ISLAND_TIME_FORMAT,
        SettingsRepository.KEY_ISLAND_TIME_LEADING_ZERO,
        SettingsRepository.KEY_ISLAND_TIME_PERIOD,
        SettingsRepository.KEY_STATUS_GLANCE_CALENDAR_SELECTED_CALENDARS,
        SettingsRepository.KEY_STATUS_GLANCE_CALENDAR_TIMEFRAME,
        SettingsRepository.KEY_STATUS_GLANCE_CALENDAR_SHOW_ALL_DAY,
        SettingsRepository.KEY_ISLAND_CALENDAR_HIDDEN_EVENTS,
        SettingsRepository.KEY_ISLAND_BRIEF_ENABLED,
    )

    private var events: List<UpcomingCalendarEvent> = emptyList()
    private val poll = Runnable { refresh() }

    override fun onStop() {
        ctx?.mainHandler?.removeCallbacks(poll)
        events = emptyList()
    }

    override fun refresh() {
        val c = ctx ?: return
        c.mainHandler.removeCallbacks(poll)
        c.mainHandler.postDelayed(poll, POLL_MS)
        if (!settings.isIslandShowCalendarEnabled()) {
            events = emptyList()
            render()
            return
        }
        c.scope.launch {
            val next = withContext(Dispatchers.IO) {
                val ids = c.settings.getStatusGlanceCalendarSelectedCalendars().mapNotNull { it.toLongOrNull() }.toSet()
                CalendarEventUtil.queryUpcomingEvents(
                    c.service,
                    c.settings.getStatusGlanceCalendarTimeframe(),
                    ids,
                    c.settings.isStatusGlanceCalendarShowAllDayEnabled(),
                    c.settings.getBoolean(SettingsRepository.KEY_ISLAND_CALENDAR_SHOW_ONGOING, false),
                    limit = QUERY_LIMIT,
                )
            }
            if (ctx == null) return@launch
            events = next
            render()
        }
    }

    private fun render() {
        if (ctx == null) {
            publish(emptyList())
            return
        }
        val hiddenWhileLocked = settings.getBoolean(SettingsRepository.KEY_ISLAND_CALENDAR_HIDE_LOCKED, false) &&
            (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true
        if (events.isEmpty() || hiddenWhileLocked) {
            publish(emptyList())
            return
        }
        val now = System.currentTimeMillis()
        val hiddenKeys = pruneHidden(now)
        val (hidden, visible) = events.partition { eventKey(it) in hiddenKeys }
        val shown = selection(visible, now)
        val parked = if (settings.isIslandBriefEnabled()) hidden.take(MAX_EVENTS) else emptyList()
        publish(shown.map { item(it, now, hidden = false) } + parked.map { item(it, now, hidden = true) })
    }

    private fun selection(events: List<UpcomingCalendarEvent>, now: Long): List<UpcomingCalendarEvent> {
        val (ongoing, upcoming) = events.partition { it.startTimeMillis <= now }
        val next = upcoming.firstOrNull()
        val nextGroup = next?.let { n -> upcoming.filter { it.startTimeMillis / 60_000L == n.startTimeMillis / 60_000L } }.orEmpty()
        val shown = if (ongoing.isEmpty() || (next != null && isUrgent(next, now))) nextGroup else ongoing
        return shown.take(MAX_EVENTS)
    }

    private fun isUrgent(e: UpcomingCalendarEvent, now: Long): Boolean {
        val priorityMs = settings.getIslandCalendarPriorityMinutes() * 60_000L
        return priorityMs > 0 && e.startTimeMillis - now in 0..priorityMs
    }

    private fun item(e: UpcomingCalendarEvent, now: Long, hidden: Boolean): IslandItem {
        val full = CalendarEventUtil.formatRelativeTime(context, e.startTimeMillis, now)
        val short = if (settings.getBoolean(SettingsRepository.KEY_ISLAND_CALENDAR_COMPACT_TIME, false) && e.startTimeMillis - now >= 60 * 60_000L) {
            IslandTimeFormat.format(context, settings, e.startTimeMillis, period = false)
        } else {
            CalendarEventUtil.formatRelativeTimeCompact(e.startTimeMillis, now)
        }
        val iconOnly = settings.getBoolean(SettingsRepository.KEY_ISLAND_CALENDAR_ICON_ONLY, false)
        val hideNow = iconOnly || (e.startTimeMillis <= now && settings.getBoolean(SettingsRepository.KEY_ISLAND_CALENDAR_HIDE_NOW, false))
        val showGlow = settings.isIslandShowGlowEnabled()
        val icon = settings.getIslandCalendarIcons()[e.calendarId]
        val urgent = !hidden && isUrgent(e, now)
        val key = "$ITEM_KEY:${eventKey(e)}"
        return IslandItem(
            key = key,
            priority = IslandPriority.CALENDAR,
            priorityOverride = IslandPriority.CALENDAR_OVERRIDE.takeIf { urgent },
            bypassLauncherOnly = urgent,
            placement = CompactPlacement.Dynamic,
            compact = listOf(
                CompactCell("$key.icon") { CalendarGlyph(icon, size = 18.dp, tint = islandAccent()) },
                CompactCell("$key.time") { RollingText(short) },
            ).filter { !(hideNow && it.key.endsWith(".time")) },
            line = LineContent(
                icon = { CalendarGlyph(icon, size = 20.dp, tint = islandAccent()) },
                start = e.title,
                end = full,
            ),
            expanded = ExpandedContent { scope ->
                CalendarExpanded(
                    event = e,
                    icon = icon,
                    relative = full,
                    countdown = short,
                    showGlow = showGlow,
                    onView = {
                        openEvent(e)
                        scope.collapse()
                    },
                    scope = scope,
                    hidden = hidden,
                    onToggleHidden = {
                        setHidden(e, !hidden)
                        scope.collapse()
                    },
                )
            },
            onOpen = { openEvent(e) },
            compactVisible = !hidden,
            chip = true,
        )
    }

    private fun eventKey(e: UpcomingCalendarEvent): String =
        "${if (e.eventId >= 0) e.eventId.toString() else e.title.hashCode().toString()}_${e.startTimeMillis}_${e.endTimeMillis}"

    private fun expiresAt(key: String): Long {
        val parts = key.split('_')
        val start = parts.getOrNull(parts.size - 2)?.toLongOrNull() ?: return 0L
        val end = parts.lastOrNull()?.toLongOrNull() ?: 0L
        return maxOf(start, end)
    }

    private fun pruneHidden(now: Long): Set<String> {
        val stored = settings.getIslandCalendarHiddenEvents()
        val live = stored.filter { expiresAt(it) > now }.toSet()
        if (live.size != stored.size) settings.setIslandCalendarHiddenEvents(live)
        return live
    }

    private fun setHidden(e: UpcomingCalendarEvent, hide: Boolean) {
        val key = eventKey(e)
        val stored = settings.getIslandCalendarHiddenEvents()
        settings.setIslandCalendarHiddenEvents(if (hide) stored + key else stored - key)
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
        private const val MAX_EVENTS = 3
        private const val QUERY_LIMIT = 20
    }
}
