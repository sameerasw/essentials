package com.sameerasw.essentials.island.plugins.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.ActiveNotificationAlert
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.ConnectedTextLabel
import com.sameerasw.essentials.island.ui.components.IslandBitmap
import com.sameerasw.essentials.island.ui.components.MarqueeText
import com.sameerasw.essentials.island.ui.components.accentGlow
import com.sameerasw.essentials.island.ui.components.cameraClearance

fun formatOtp(code: String): String = when {
    code.length <= 5 -> code
    code.length == 6 -> "${code.take(3)} ${code.drop(3)}"
    code.length == 8 -> "${code.take(4)} ${code.drop(4)}"
    else -> code
}

@Composable
fun OtpExpanded(
    alert: ActiveNotificationAlert,
    code: String,
    sender: String,
    message: String,
    showGlow: Boolean,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
    scope: IslandExpandedScope,
) {
    val spec = scope.spec
    val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
    val accent = alert.appColor?.let { Color(it) } ?: Color.White
    var showMessage by remember(alert.key) { mutableStateOf(false) }
    Box(propagateMinConstraints = true) {
        Box(Modifier.matchParentSize().accentGlow(accent, showGlow, scope.cameraClearance))
        Column(
            modifier = Modifier.fillMaxWidth().padding(spec.expandedOutset),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Spacer(Modifier.height(spec.expandedTopPadding))
                scope.CameraRow(
                    horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                    start = {
                        IslandBitmap(alert.appIcon ?: alert.icon, spec.cellSize, fallbackRes = R.drawable.rounded_notifications_unread_24)
                        MarqueeText(text = sender, style = IslandTextStyles.title, modifier = Modifier.weight(1f))
                    },
                    end = {
                        Text(stringResource(R.string.island_otp_label), style = IslandTextStyles.body, maxLines = 1)
                    },
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = formatOtp(code),
                    style = IslandTextStyles.title.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 32.sp,
                        letterSpacing = 4.sp,
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                )
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    if (!showMessage) {
                        Text(
                            text = stringResource(R.string.island_otp_show_message),
                            style = IslandTextStyles.body,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(interactionSource = null, indication = null) {
                                    showMessage = true
                                    scope.keepAlive()
                                },
                        )
                    }
                    AnimatedVisibility(visible = showMessage) {
                        Text(
                            text = message,
                            style = IslandTextStyles.body.copy(fontSize = 14.sp, lineHeight = 19.sp),
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(14.dp))
                ConnectedButtonRow(
                    height = 36.dp,
                    modifier = Modifier.padding(horizontal = sidePadding),
                    container = Color.White.copy(alpha = 0.2f),
                    items = listOf(
                        ConnectedItem(onDismiss) { ConnectedTextLabel(stringResource(R.string.island_otp_dismiss)) },
                        ConnectedItem(onCopy, container = accent.copy(alpha = 0.55f)) {
                            ConnectedTextLabel(stringResource(R.string.island_otp_copy))
                        },
                    ),
                )
                Spacer(Modifier.height(spec.expandedBottomPadding))
            }
        }
    }
}
