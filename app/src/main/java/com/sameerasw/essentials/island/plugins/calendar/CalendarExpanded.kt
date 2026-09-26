package com.sameerasw.essentials.island.plugins.calendar

import com.sameerasw.essentials.island.ui.squareFit
import androidx.compose.foundation.layout.fillMaxSize
import com.sameerasw.essentials.island.ui.SurfaceBackdrop
import com.sameerasw.essentials.island.ui.components.cameraClearance
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.plugins.soften
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.ConnectedTextLabel
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.MarqueeText
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.essentials.island.ui.components.accentGlow
import com.sameerasw.essentials.utils.UpcomingCalendarEvent
import java.util.Date

@Composable
fun CalendarExpanded(
    event: UpcomingCalendarEvent,
    relative: String,
    countdown: String,
    showGlow: Boolean,
    onView: () -> Unit,
    scope: IslandExpandedScope,
    emoji: String? = null,
    drawBackground: Boolean = true,
) {
    val spec = scope.spec
    val context = LocalContext.current
    val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
    val calendarColor = event.calendarColor?.let { Color(soften(it)) } ?: MaterialTheme.colorScheme.primary
    val timeFormat = DateFormat.getTimeFormat(context)
    val timeRange = when {
        event.allDay -> stringResource(R.string.island_calendar_all_day)
        event.endTimeMillis > event.startTimeMillis ->
            "${timeFormat.format(Date(event.startTimeMillis))} – ${timeFormat.format(Date(event.endTimeMillis))}"
        else -> timeFormat.format(Date(event.startTimeMillis))
    }
    val detailStyle = IslandTextStyles.body.copy(color = Color.White, fontSize = 15.sp, lineHeight = 20.sp)

    Box(propagateMinConstraints = true) {
        if (drawBackground && !SurfaceBackdrop { Box(Modifier.fillMaxSize().accentGlow(calendarColor, showGlow, scope.cameraClearance)) }) {
            Box(Modifier.matchParentSize().accentGlow(calendarColor, showGlow, scope.cameraClearance))
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(spec.expandedOutset),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Spacer(Modifier.height(spec.expandedTopPadding))
                scope.CameraRow(
                    horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                    start = {
                        Box(Modifier.size(spec.cellSize), contentAlignment = Alignment.Center) {
                            CalendarGlyph(emoji, size = 20.dp, tint = calendarColor)
                        }
                        MarqueeText(text = event.title, style = IslandTextStyles.title, modifier = Modifier.weight(1f))
                    },
                    end = { RollingText(countdown) },
                )
                Spacer(Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("$timeRange · $relative", style = detailStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    event.location?.let { DetailRow(R.drawable.rounded_location_on_24, it) }
                    event.calendarName?.let { name ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.padding(horizontal = 3.dp).size(10.dp).background(calendarColor, CircleShape))
                            Text(name, style = IslandTextStyles.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    event.description?.let {
                        Text(it, style = IslandTextStyles.body, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Column {
                Spacer(Modifier.height(14.dp))
                ConnectedButtonRow(
                    height = 36.dp,
                    modifier = Modifier.padding(horizontal = sidePadding),
                    container = Color.White.copy(alpha = 0.2f),
                    items = listOf(ConnectedItem(onView) { ConnectedTextLabel(stringResource(R.string.island_calendar_view_event)) }),
                )
                Spacer(Modifier.height(spec.expandedBottomPadding))
            }
        }
    }
}

@Composable
private fun DetailRow(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IslandIcon(icon, size = 16.dp, tint = Color.White.copy(alpha = 0.72f))
        Text(text, style = IslandTextStyles.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun CalendarGlyph(emoji: String?, size: Dp, tint: Color) {
    if (emoji.isNullOrBlank()) {
        IslandIcon(R.drawable.rounded_calendar_today_24, tint = tint, size = size)
    } else {
        val paint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }
        val bounds = remember { android.graphics.Rect() }
        Canvas(Modifier.squareFit(size)) {
            paint.textSize = this.size.minDimension
            paint.getTextBounds(emoji, 0, emoji.length, bounds)
            val scale = this.size.minDimension * EMOJI_SCALE / maxOf(bounds.width(), bounds.height()).coerceAtLeast(1)
            paint.textSize *= scale
            paint.getTextBounds(emoji, 0, emoji.length, bounds)
            drawContext.canvas.nativeCanvas.drawText(
                emoji,
                center.x - bounds.exactCenterX(),
                center.y - bounds.exactCenterY(),
                paint,
            )
        }
    }
}

private const val EMOJI_SCALE = 1.25f
