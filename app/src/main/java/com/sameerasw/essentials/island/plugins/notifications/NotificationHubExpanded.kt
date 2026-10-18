package com.sameerasw.essentials.island.plugins.notifications

import android.text.format.DateUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.ActiveNotificationAlert
import com.sameerasw.essentials.domain.model.NotificationActionItem
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.ui.IslandHaptics
import com.sameerasw.essentials.island.ui.IslandMotion
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.IslandBitmap
import com.sameerasw.essentials.island.ui.components.IslandVerticalCarousel
import com.sameerasw.essentials.island.plugins.soften
import kotlinx.coroutines.launch
import kotlin.math.abs

private val HubListMaxHeight = 400.dp
private val HubItemHeight = 118.dp
private val HubItemCorner = 28.dp

@Composable
fun NotificationHubExpanded(
    alerts: List<ActiveNotificationAlert>,
    onOpen: (ActiveNotificationAlert) -> Unit,
    onSelect: (ActiveNotificationAlert) -> Unit,
    onDismiss: (ActiveNotificationAlert) -> Unit,
    onAction: (ActiveNotificationAlert, NotificationActionItem) -> Unit,
    onClearAll: () -> Unit,
    scope: IslandExpandedScope,
) {
    val context = LocalContext.current
    val spec = scope.spec
    val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
    Column(
        modifier = Modifier.fillMaxWidth().padding(spec.expandedOutset),
    ) {
        Spacer(Modifier.height(spec.expandedTopPadding))
        scope.CameraRow(
            horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
            start = {
                Text(stringResource(R.string.island_section_notifications), style = IslandTextStyles.title)
                Text("${alerts.size}", style = IslandTextStyles.body)
            },
            end = {
                if (alerts.any { !it.ongoing }) {
                    Text(
                        text = stringResource(R.string.label_clear_all),
                        style = IslandTextStyles.line,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.16f))
                            .combinedClickable(onClick = {
                                IslandHaptics.button(context)
                                onClearAll()
                            })
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            },
        )
        IslandVerticalCarousel(
            itemCount = alerts.size,
            itemHeight = HubItemHeight,
            maxHeight = HubListMaxHeight,
            itemShape = RoundedCornerShape(HubItemCorner),
            modifier = Modifier.padding(horizontal = sidePadding - 6.dp),
        ) { index ->
            val alert = alerts[index]
            HubRow(
                alert = alert,
                onOpen = { onOpen(alert) },
                onSelect = { onSelect(alert) },
                onDismiss = { onDismiss(alert) },
                onAction = { onAction(alert, it) },
            )
        }
        Spacer(Modifier.height(spec.expandedBottomPadding))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HubRow(
    alert: ActiveNotificationAlert,
    onOpen: () -> Unit,
    onSelect: () -> Unit,
    onDismiss: () -> Unit,
    onAction: (NotificationActionItem) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val actions = remember(alert.key, alert.actions) { alert.actions.filter { !it.isQuickReply }.take(2) }
    val hasActions = actions.isNotEmpty()
    val (sender, message) = remember(alert.key, alert.title, alert.text) { NotificationsPlugin.senderAndMessage(context, alert) }
    val time = remember(alert.timestamp) {
        DateUtils.getRelativeTimeSpanString(alert.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = offset.value
                alpha = (1f - abs(offset.value) / size.width).coerceIn(0f, 1f)
            }
            .background(alert.appColor?.let { Color(soften(it)).copy(alpha = 0.3f) } ?: Color.White.copy(alpha = 0.1f))
            .then(
                if (alert.ongoing) {
                    Modifier
                } else {
                    Modifier.pointerInput(alert.key) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                scope.launch { offset.snapTo(offset.value + amount) }
                            },
                            onDragEnd = {
                                val width = size.width.toFloat()
                                scope.launch {
                                    if (abs(offset.value) > width * 0.35f) {
                                        IslandHaptics.commit(context)
                                        offset.animateTo(if (offset.value > 0f) width else -width, IslandMotion.fling())
                                        onDismiss()
                                    } else {
                                        offset.animateTo(0f, IslandMotion.fling())
                                    }
                                }
                            },
                            onDragCancel = { scope.launch { offset.animateTo(0f, IslandMotion.fling()) } },
                        )
                    }
                },
            )
            .combinedClickable(
                onClick = {
                    IslandHaptics.button(context)
                    onOpen()
                },
                onLongClick = {
                    IslandHaptics.longPress(context)
                    onSelect()
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IslandBitmap(
                alert.chatIcon ?: alert.appIcon ?: alert.icon,
                44.dp,
                circle = true,
                fallbackRes = R.drawable.rounded_notifications_unread_24,
            )
            Column(Modifier.weight(1f)) {
                Text(sender, style = IslandTextStyles.title.copy(fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (message.isNotBlank()) {
                    Text(
                        message,
                        style = IslandTextStyles.body.copy(fontSize = 14.sp, lineHeight = 18.sp, color = Color.White.copy(alpha = 0.8f)),
                        maxLines = if (hasActions) 1 else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = if (alert.ongoing) stringResource(R.string.island_hub_ongoing) else time,
                style = IslandTextStyles.body.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
        if (hasActions) {
            Row(Modifier.padding(top = 8.dp, start = 56.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                actions.forEach { action ->
                    Text(
                        text = action.title,
                        style = IslandTextStyles.line.copy(fontSize = 12.sp),
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.16f))
                            .combinedClickable(onClick = { onAction(action) })
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}
