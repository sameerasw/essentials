package com.sameerasw.essentials.island.plugins.calendar

import androidx.compose.material3.MaterialTheme
import com.sameerasw.essentials.island.ui.components.MarqueeText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.island.ui.components.CameraRow
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.utils.CalendarEventUtil
import com.sameerasw.essentials.utils.UpcomingCalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarPlugin : BaseIslandPlugin() {
    override val id = "calendar"

    override val settingKeys = setOf(SettingsRepository.KEY_ISLAND_SHOW_CALENDAR)

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
        if (e == null || ctx == null) {
            publish(null)
            return
        }
        val now = System.currentTimeMillis()
        val full = CalendarEventUtil.formatRelativeTime(context, e.startTimeMillis, now)
        val short = CalendarEventUtil.formatRelativeTimeCompact(e.startTimeMillis, now)
        val location = e.location.orEmpty()
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.CALENDAR,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("cal.icon") { IslandIcon(R.drawable.rounded_calendar_today_24, size = 18.dp, tint = MaterialTheme.colorScheme.primary) },
                    CompactCell("cal.time") { RollingText(short) },
                ),
                line = LineContent(
                    icon = { IslandIcon(R.drawable.rounded_calendar_today_24, tint = MaterialTheme.colorScheme.primary) },
                    start = e.title,
                    end = full,
                ),
                expanded = ExpandedContent { scope ->
                    Column(Modifier.padding(scope.spec.expandedOutset).padding(top = scope.spec.expandedTopPadding, bottom = scope.spec.expandedPadding)) {
                        scope.CameraRow(
                            start = {
                                IslandIcon(R.drawable.rounded_calendar_today_24, size = 20.dp, tint = MaterialTheme.colorScheme.primary)
                                MarqueeText(text = e.title, style = IslandTextStyles.title, modifier = Modifier.weight(1f))
                            },
                            end = { RollingText(short) },
                        )
                        Column(
                            Modifier.padding(horizontal = scope.spec.expandedPadding + scope.spec.expandedCorner * 0.35f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(full, style = IslandTextStyles.body.copy(color = Color.White, fontSize = 15.sp))
                            if (location.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IslandIcon(R.drawable.rounded_location_on_24, size = 16.dp, tint = Color.White.copy(alpha = 0.72f))
                                    Text(location, style = IslandTextStyles.body, maxLines = 1)
                                }
                            }
                        }
                    }
                },
            ),
        )
    }

    companion object {
        const val ITEM_KEY = "calendar"
        private const val POLL_MS = 60_000L
    }
}
