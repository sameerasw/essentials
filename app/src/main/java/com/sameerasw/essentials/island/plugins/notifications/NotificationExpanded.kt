package com.sameerasw.essentials.island.plugins.notifications

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.ActiveNotificationAlert
import com.sameerasw.essentials.domain.model.NotificationActionItem
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.ConnectedTextLabel
import com.sameerasw.essentials.island.ui.components.IslandBitmap
import com.sameerasw.essentials.island.ui.components.accentGlow

@Composable
fun NotificationExpanded(
    alert: ActiveNotificationAlert,
    sender: String,
    message: String,
    showGlow: Boolean,
    onAction: (NotificationActionItem) -> Unit,
    scope: IslandExpandedScope,
) {
    val spec = scope.spec
    val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
    val glowColor = alert.appColor?.let { Color(it) } ?: Color.White
    Box {
        Box(Modifier.matchParentSize().accentGlow(glowColor, showGlow))
        Column(Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(spec.expandedTopPadding))
            scope.CameraRow(
                horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                start = {
                    IslandBitmap(alert.icon ?: alert.appIcon, spec.cellSize, fallbackRes = R.drawable.rounded_notifications_unread_24)
                    Text(
                        text = sender,
                        style = IslandTextStyles.title,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 1200),
                    )
                },
            )
            if (message.isNotBlank()) {
                Spacer(Modifier.height(if (alert.actions.isEmpty()) 4.dp else 0.dp))
                Text(
                    text = message,
                    style = IslandTextStyles.body.copy(color = Color.White, fontSize = 15.sp, lineHeight = 20.sp),
                    maxLines = 7,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                )
            }
            if (alert.actions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ConnectedButtonRow(
                    height = 36.dp,
                    modifier = Modifier.padding(horizontal = sidePadding),
                    container = Color.White.copy(alpha = 0.2f),
                    items = alert.actions.map { action ->
                        ConnectedItem({ onAction(action) }) {
                            ConnectedTextLabel(if (action.isQuickReply) "${action.title} ↩" else action.title)
                        }
                    },
                )
            }
            Spacer(Modifier.height(if (alert.actions.isEmpty()) spec.expandedPadding * 0.9f else spec.expandedPadding * 0.7f))
        }
    }
}
