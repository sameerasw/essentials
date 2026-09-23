package com.sameerasw.essentials.island.plugins.brief

import androidx.compose.foundation.layout.fillMaxSize
import com.sameerasw.essentials.island.ui.SurfaceBackdrop
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.ContentUris
import androidx.compose.foundation.Image
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.sameerasw.essentials.island.plugins.media.IslandMediaState
import com.sameerasw.essentials.island.plugins.media.MediaExpanded
import com.sameerasw.essentials.island.plugins.media.MediaSnapshot
import com.sameerasw.essentials.island.ui.components.ArtworkBackdrop
import com.sameerasw.essentials.island.ui.components.accentGlow
import com.sameerasw.essentials.island.ui.components.cameraClearance
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.MarqueeText
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
import androidx.compose.ui.unit.Dp
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
    var page by remember { mutableStateOf<BriefPage>(BriefPage.Overview) }
    val media by IslandMediaState.current.collectAsState()
    LaunchedEffect(media == null) {
        if (media == null && page == BriefPage.Player) page = BriefPage.Overview
    }
    
    val pageShape = RoundedCornerShape(scope.spec.expandedCorner)
    Box(propagateMinConstraints = true) {
        if (!SurfaceBackdrop { BriefBackground(page, media, showGlow, scope, Modifier.fillMaxSize()) }) {
            BriefBackground(page, media, showGlow, scope, Modifier.matchParentSize())
        }
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val forward = targetState != BriefPage.Overview
                (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut()) using
                    SizeTransform(clip = false) { _, _ -> spring(stiffness = IslandMotion.STIFFNESS, dampingRatio = IslandMotion.DAMPING) }
            },
            label = "brief",
        ) { target ->
            Box(propagateMinConstraints = true, modifier = Modifier.clip(pageShape)) {
                when (target) {
                    BriefPage.Overview -> BriefOverview(
                        scope = scope,
                        iconStyle = iconStyle,
                        calendarEnabled = calendarEnabled,
                        calendarIds = calendarIds,
                        showAllDay = showAllDay,
                        media = media,
                        onEventClick = { page = BriefPage.Event(it) },
                        onPlayerClick = { page = BriefPage.Player },
                    )
                    is BriefPage.Event -> SwipeBackPage(scope, onBack = { page = BriefPage.Overview }) {
                        BriefEventDetail(target.event, showGlow, scope)
                    }
                    BriefPage.Player -> SwipeBackPage(scope, onBack = { page = BriefPage.Overview }) {
                        media?.let { m ->
                            MediaExpanded(m.title, m.artist, m.artwork, m.accent, m.playing, m.liked, m.actions, scope, drawBackground = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefBackground(page: BriefPage, media: MediaSnapshot?, showGlow: Boolean, scope: IslandExpandedScope, modifier: Modifier) {
    val artwork = remember(media?.artwork) { media?.artwork?.asImageBitmap() }
    val artAlpha by animateFloatAsState(
        when (page) {
            BriefPage.Player -> 1f
            BriefPage.Overview -> if (media?.playing == true) 1f else 0f
            is BriefPage.Event -> 0f
        },
        tween(400),
        label = "briefArt",
    )
    val event = (page as? BriefPage.Event)?.event
    val glowColor = event?.calendarColor?.let { Color(soften(it)) } ?: MaterialTheme.colorScheme.primary
    val glowAlpha by animateFloatAsState(if (event != null) 1f else 0f, tween(400), label = "briefGlow")
    Box(modifier) {
        if (artAlpha > 0f) {
            scope.ArtworkBackdrop(artwork, Modifier.matchParentSize().graphicsLayer { alpha = artAlpha })
        }
        if (glowAlpha > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = glowAlpha }
                    .accentGlow(glowColor, showGlow, scope.cameraClearance),
            )
        }
    }
}

private sealed interface BriefPage {
    data object Overview : BriefPage
    data class Event(val event: UpcomingCalendarEvent) : BriefPage
    data object Player : BriefPage
}

// Swipe left-to-right returns to the overview
@Composable
private fun SwipeBackPage(scope: IslandExpandedScope, onBack: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val backThreshold = with(density) { 64.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    Box(
        propagateMinConstraints = true,
        modifier = Modifier
            .graphicsLayer {
                translationX = offset.value
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
        content()
    }
}

@Composable
private fun BriefEventDetail(event: UpcomingCalendarEvent, showGlow: Boolean, scope: IslandExpandedScope) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    Box(propagateMinConstraints = true) {
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
            drawBackground = false,
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
    media: MediaSnapshot?,
    onEventClick: (UpcomingCalendarEvent) -> Unit,
    onPlayerClick: () -> Unit,
) {
    val context = LocalContext.current
    val spec = scope.spec
    val sidePadding = spec.expandedPadding * 0.6f + spec.expandedCorner * 0.25f
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

    val artwork = remember(media?.artwork) { media?.artwork?.asImageBitmap() }
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
                        else -> list.forEach { event -> BriefEventRow(event, now, accent) { onEventClick(event) } }
                    }
                }
            }

            if (media != null) {
                Spacer(Modifier.height(8.dp))
                BriefPlayer(media, artwork, onPlayerClick, Modifier.padding(horizontal = sidePadding))
            }
        }
    }
}

@Composable
private fun BriefEventRow(event: UpcomingCalendarEvent, now: Long, accent: Color, onClick: () -> Unit) {
    val context = LocalContext.current
    val whenText = remember(event, now) { briefWhen(context, event, now) }
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

@Composable
private fun BriefPlayer(media: MediaSnapshot, artwork: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.1f)),
        ) {
            if (artwork != null) {
                Image(artwork, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            MarqueeText(text = media.title, style = IslandTextStyles.body.copy(color = Color.White, fontSize = 15.sp))
            MarqueeText(text = media.artist, style = IslandTextStyles.body)
        }
        BriefPlayerButton(
            icon = if (media.liked) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24,
            tint = if (media.liked) media.accent else Color.White,
        ) {
            IslandHaptics.button(context)
            media.actions.like()
        }
        BriefPlayerButton(
            icon = if (media.playing) R.drawable.rounded_pause_24 else R.drawable.rounded_play_arrow_24,
            tint = if (media.accent.luminance() > 0.5f) Color.Black else Color.White,
            container = media.accent,
            width = 64.dp,
            iconSize = 26.dp,
        ) {
            IslandHaptics.button(context)
            media.actions.playPause()
        }
        BriefPlayerButton(R.drawable.rounded_skip_next_24) {
            IslandHaptics.button(context)
            media.actions.next()
        }
    }
}

@Composable
private fun BriefPlayerButton(
    icon: Int,
    tint: Color = Color.White,
    container: Color? = null,
    width: Dp = 44.dp,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(container ?: Color.Transparent, label = "briefButton")
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .size(width = width, height = 44.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        IslandIcon(icon, tint = tint, size = iconSize)
    }
}

private fun briefWhen(context: Context, event: UpcomingCalendarEvent, now: Long): String {
    val timeFormat = DateFormat.getTimeFormat(context)
    val start = event.startTimeMillis
    val tomorrow = !isSameDay(start, now) && start > now
    if (event.allDay) {
        val all = context.getString(R.string.island_brief_all_day)
        return if (tomorrow) context.getString(R.string.island_brief_tomorrow, all) else all
    }
    if (start <= now) {
        return context.getString(R.string.island_brief_now_until, timeFormat.format(Date(event.endTimeMillis)))
    }
    val minutes = ((start - now + 59_999L) / 60_000L).toInt()
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        minutes < 60 -> context.getString(R.string.island_brief_in_minutes, minutes)
        minutes < 3 * 60 -> if (rest == 0) {
            context.getString(R.string.island_brief_in_hours, hours)
        } else {
            context.getString(R.string.island_brief_in_hours_minutes, hours, rest)
        }
        minutes < 12 * 60 -> context.getString(R.string.island_brief_in_hours, if (rest >= 30) hours + 1 else hours)
        tomorrow -> context.getString(R.string.island_brief_tomorrow, timeFormat.format(Date(start)))
        else -> context.getString(R.string.island_brief_today, timeFormat.format(Date(start)))
    }
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val ca = java.util.Calendar.getInstance().apply { timeInMillis = a }
    val cb = java.util.Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(java.util.Calendar.YEAR) == cb.get(java.util.Calendar.YEAR) &&
        ca.get(java.util.Calendar.DAY_OF_YEAR) == cb.get(java.util.Calendar.DAY_OF_YEAR)
}
