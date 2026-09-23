package com.sameerasw.essentials.island.plugins.brief

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.ContentUris
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.sameerasw.essentials.island.ui.IslandMotion
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.sameerasw.essentials.island.plugins.calendar.CalendarExpanded
import com.sameerasw.essentials.island.plugins.soften
import com.sameerasw.essentials.island.ui.IslandHaptics
import kotlinx.coroutines.launch
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.BatteryGlyph
import com.sameerasw.essentials.island.ui.components.BatteryRing
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.utils.CalendarEventUtil
import com.sameerasw.essentials.utils.UpcomingCalendarEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class BriefPlugin : BaseIslandPlugin() {
    override val id = "brief"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_BRIEF_ENABLED,
        SettingsRepository.KEY_ISLAND_BATTERY_STYLE,
        SettingsRepository.KEY_STATUS_GLANCE_CALENDAR_SHOW_ALL_DAY,
    )

    override fun refresh() {
        if (ctx == null || !settings.isIslandBriefEnabled()) {
            publish(null)
            return
        }
        val iconStyle = settings.getIslandBatteryStyle() == SettingsRepository.ISLAND_BATTERY_STYLE_ICON
        val calendarIds = settings.getStatusGlanceCalendarSelectedCalendars().mapNotNull { it.toLongOrNull() }.toSet()
        val showAllDay = settings.isStatusGlanceCalendarShowAllDayEnabled()
        val showGlow = settings.isIslandShowGlowEnabled()
        val calendarEnabled = context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.DEFAULT,
                placement = CompactPlacement.Dynamic,
                compact = listOf(CompactCell("brief.placeholder") {}),
                expanded = ExpandedContent { scope ->
                    BriefExpanded(scope, iconStyle, calendarEnabled, calendarIds, showAllDay, showGlow)
                },
                compactVisible = false,
            ),
        )
    }

    companion object {
        const val ITEM_KEY = "brief"
    }
}

@Composable
private fun BriefExpanded(
    scope: IslandExpandedScope,
    iconStyle: Boolean,
    calendarEnabled: Boolean,
    calendarIds: Set<Long>,
    showAllDay: Boolean,
    showGlow: Boolean,
) {
    var selected by remember { mutableStateOf<UpcomingCalendarEvent?>(null) }
    Box(propagateMinConstraints = true) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                val forward = targetState != null
                (slideInHorizontally(spring(stiffness = IslandMotion.STIFFNESS, dampingRatio = IslandMotion.DAMPING)) { if (forward) it / 8 else -it / 8 } + fadeIn(tween(220, delayMillis = 60))) togetherWith
                    (slideOutHorizontally(spring(stiffness = IslandMotion.STIFFNESS, dampingRatio = IslandMotion.DAMPING)) { if (forward) -it / 8 else it / 8 } + fadeOut(tween(120))) using
                    SizeTransform(clip = false) { _, _ -> spring(stiffness = IslandMotion.STIFFNESS, dampingRatio = IslandMotion.DAMPING) }
            },
            label = "brief",
        ) { event ->
            Box(propagateMinConstraints = true) {
                if (event == null) {
                    BriefOverview(scope, iconStyle, calendarEnabled, calendarIds, showAllDay) { selected = it }
                } else {
                    BriefEventDetail(event, showGlow, scope) { selected = null }
                }
            }
        }
    }
}

@Composable
private fun BriefEventDetail(event: UpcomingCalendarEvent, showGlow: Boolean, scope: IslandExpandedScope, onBack: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val backThreshold = with(density) { 64.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val now = System.currentTimeMillis()
    Box(
        propagateMinConstraints = true,
        modifier = Modifier
            .graphicsLayer {
                val progress = (offset.value / (backThreshold * 2f)).coerceIn(0f, 1f)
                translationX = offset.value * 0.5f
                alpha = 1f - progress * 0.6f
                scaleX = 1f - progress * 0.05f
                scaleY = 1f - progress * 0.05f
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offset.value > backThreshold) {
                            IslandHaptics.commit(context)
                            onBack()
                        } else {
                            coroutineScope.launch { offset.animateTo(0f) }
                        }
                    },
                    onDragCancel = { coroutineScope.launch { offset.animateTo(0f) } },
                ) { change, amount ->
                    change.consume()
                    scope.keepAlive()
                    coroutineScope.launch { offset.snapTo((offset.value + amount).coerceAtLeast(0f)) }
                }
            },
    ) {
        CalendarExpanded(
            event = event,
            relative = CalendarEventUtil.formatRelativeTime(context, event.startTimeMillis, now),
            countdown = CalendarEventUtil.formatRelativeTimeCompact(event.startTimeMillis, now),
            showGlow = showGlow,
            onView = {
                openEvent(context, event)
                scope.collapse()
            },
            scope = scope,
        )
    }
}

private fun openEvent(context: Context, event: UpcomingCalendarEvent) {
    val intent = if (event.eventId >= 0) {
        Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startTimeMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endTimeMillis)
    } else {
        Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath(event.startTimeMillis.toString()).build())
    }
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}

@Composable
private fun BriefOverview(
    scope: IslandExpandedScope,
    iconStyle: Boolean,
    calendarEnabled: Boolean,
    calendarIds: Set<Long>,
    showAllDay: Boolean,
    onEventClick: (UpcomingCalendarEvent) -> Unit,
) {
    val context = LocalContext.current
    val spec = scope.spec
    val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
    val accent = MaterialTheme.colorScheme.primary

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L - now % 60_000L)
            now = System.currentTimeMillis()
        }
    }
    val battery = remember(now) { readBattery(context) }
    val events by produceState<List<UpcomingCalendarEvent>?>(null, calendarEnabled) {
        value = if (calendarEnabled) {
            withContext(Dispatchers.IO) { CalendarEventUtil.queryBriefEvents(context, calendarIds, showAllDay) }
        } else {
            emptyList()
        }
    }

    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    val time = SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(now))
    val date = SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEMMMd"), Locale.getDefault()).format(Date(now))

    Box(propagateMinConstraints = true) {
        Column(Modifier.fillMaxWidth().padding(spec.expandedOutset).padding(bottom = spec.expandedPadding)) {
            Spacer(Modifier.height(spec.expandedTopPadding))
            scope.CameraRow(
                horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                start = {
                    Text(time, style = IslandTextStyles.title.copy(fontSize = 20.sp))
                    Spacer(Modifier.width(8.dp))
                    Text(date, style = IslandTextStyles.body, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                },
                end = {
                    if (battery >= 0) {
                        if (iconStyle) {
                            BatteryGlyph(battery, accent, showLevel = true)
                        } else {
                            BatteryRing(battery, accent, showLevel = true)
                        }
                    }
                },
            )

            if (calendarEnabled) {
                Spacer(Modifier.height(12.dp))
                val list = events
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when {
                        list == null -> Unit
                        list.isEmpty() -> Text(
                            text = stringResource(R.string.island_brief_no_events),
                            style = IslandTextStyles.body,
                        )
                        else -> list.forEach { event -> BriefEventRow(event, timePattern, accent) { onEventClick(event) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefEventRow(event: UpcomingCalendarEvent, timePattern: String, accent: Color, onClick: () -> Unit) {
    val format = remember(timePattern) { SimpleDateFormat(timePattern, Locale.getDefault()) }
    val whenText = if (event.allDay) {
        stringResource(R.string.island_brief_all_day)
    } else {
        "${format.format(Date(event.startTimeMillis))} – ${format.format(Date(event.endTimeMillis))}"
    }
    val color = event.calendarColor?.let { Color(soften(it)) } ?: accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(32.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = IslandTextStyles.body.copy(color = Color.White, fontSize = 15.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(whenText, event.location).joinToString(" · "),
                style = IslandTextStyles.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun readBattery(context: Context): Int {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    return if (level < 0 || scale <= 0) -1 else level * 100 / scale
}
