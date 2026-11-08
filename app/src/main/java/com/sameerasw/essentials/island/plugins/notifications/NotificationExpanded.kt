package com.sameerasw.essentials.island.plugins.notifications

import com.sameerasw.essentials.island.ui.components.resizableCard
import com.sameerasw.essentials.island.ui.components.rememberExpandedResize
import com.sameerasw.essentials.island.ui.components.ResizeHandle
import com.sameerasw.essentials.island.ui.IslandLiveResize
import androidx.compose.ui.draw.clipToBounds
import com.sameerasw.essentials.data.repository.SettingsRepository
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import kotlinx.coroutines.delay
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.IslandHaptics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import com.sameerasw.essentials.island.ui.components.cameraClearance
import androidx.compose.foundation.layout.Arrangement
import com.sameerasw.essentials.island.ui.components.MarqueeText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
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
import com.sameerasw.essentials.island.ui.islandTextColor
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
    onReply: (NotificationActionItem, String) -> Unit,
    scope: IslandExpandedScope,
) {
    var replyAction by remember(alert.key) { mutableStateOf<NotificationActionItem?>(null) }
    val spec = scope.spec
    val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
    val glowColor = alert.appColor?.let { Color(it) } ?: Color.White
    val resize = scope.rememberExpandedResize(SettingsRepository.KEY_ISLAND_NOTIF_MAX_HEIGHT)
    Box(propagateMinConstraints = true) {
        Box(Modifier.matchParentSize().accentGlow(glowColor, showGlow, scope.cameraClearance))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .resizableCard(resize)
                .padding(spec.expandedOutset),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f, fill = false)) {
                Spacer(Modifier.height(spec.expandedTopPadding))
                scope.CameraRow(
                    horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                    title = sender,
                    start = {
                        IslandBitmap(alert.chatIcon ?: alert.appIcon ?: alert.icon, spec.cellSize, fallbackRes = R.drawable.rounded_notifications_unread_24)
                    },
                    end = {
                        val appIcon = alert.appIcon
                        if (alert.chatIcon != null && appIcon != null) {
                            IslandBitmap(appIcon, spec.cellSize, circle = true)
                        }
                    },
                )
                Column(Modifier.weight(1f, fill = false).clipToBounds()) {
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = message,
                        style = IslandTextStyles.body.copy(color = islandTextColor(), fontSize = 15.sp, lineHeight = 20.sp),
                        maxLines = 20,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                    )
                }
                alert.image?.let { picture ->
                    val image = remember(picture) { picture.asImageBitmap() }
                    val ratio = picture.width.toFloat() / picture.height.coerceAtLeast(1)
                    val maxImageHeight = minOf((LocalConfiguration.current.screenHeightDp * 0.4f).dp, 200.dp)
                    Spacer(Modifier.height(10.dp))
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(horizontal = sidePadding)
                            .layout { measurable, constraints ->
                                val full = constraints.maxWidth
                                val natural = (full / ratio).toInt()
                                val capped = minOf(natural, maxImageHeight.roundToPx())
                                val width = minOf(full, (capped * ratio).toInt()).coerceAtLeast(1)
                                val height = minOf(capped, constraints.maxHeight)
                                if (height < 32.dp.roundToPx()) return@layout layout(full, 0) {}
                                val placeable = measurable.measure(Constraints.fixed(width, height))
                                layout(full, height) { placeable.place((full - width) / 2, 0) }
                            }
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
                }
            }
            Column {
                val replying = replyAction
                if (replying != null) {
                    Spacer(Modifier.height(14.dp))
                    ReplyField(
                        modifier = Modifier.padding(horizontal = sidePadding),
                        accent = glowColor,
                        onSend = { text ->
                            scope.setTextInput(false)
                            replyAction = null
                            onReply(replying, text)
                        },
                        onCancel = {
                            scope.setTextInput(false)
                            replyAction = null
                        },
                        scope = scope,
                    )
                } else if (alert.actions.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    ConnectedButtonRow(
                        height = 36.dp,
                        modifier = Modifier.padding(horizontal = sidePadding),
                        container = Color.White.copy(alpha = 0.2f),
                        items = alert.actions.map { action ->
                            ConnectedItem({ if (action.isQuickReply) replyAction = action else onAction(action) }) {
                                if (action.isQuickReply) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ConnectedTextLabel(action.title)
                                        Icon(painterResource(R.drawable.rounded_reply_24), contentDescription = null, tint = islandTextColor(), modifier = Modifier.padding(end = 8.dp).size(16.dp))
                                    }
                                } else {
                                    ConnectedTextLabel(action.title)
                                }
                            }
                        },
                    )
                }
                ResizeHandle(resize, spec.expandedBottomPadding.coerceAtLeast(18.dp))
            }
        }
    }
}

// Inline reply
@Composable
private fun ReplyField(
    modifier: Modifier,
    accent: Color,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    scope: IslandExpandedScope,
) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    DisposableEffect(Unit) {
        scope.setTextInput(true)
        onDispose { scope.setTextInput(false) }
    }
    LaunchedEffect(Unit) {
        delay(80)
        focus.requestFocus()
        keyboard?.show()
    }
    fun send() {
        val value = text.trim()
        if (value.isEmpty()) return
        IslandHaptics.button(context)
        keyboard?.hide()
        text = ""
        onSend(value)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (text.isEmpty()) {
                Text(stringResource(R.string.island_reply_hint), style = IslandTextStyles.body, maxLines = 1)
            }
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    scope.keepAlive()
                },
                singleLine = true,
                textStyle = IslandTextStyles.body.copy(color = islandTextColor(), fontSize = 15.sp),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send, capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .onKeyEvent { event ->
                        if (event.key == Key.Back && event.type == KeyEventType.KeyUp) {
                            onCancel()
                            true
                        } else {
                            false
                        }
                    },
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (text.isBlank()) Color.White.copy(alpha = 0.12f) else accent)
                .clickable(enabled = text.isNotBlank()) { send() },
            contentAlignment = Alignment.Center,
        ) {
            IslandIcon(R.drawable.rounded_send_24, tint = if (text.isBlank()) Color.White.copy(alpha = 0.5f) else Color.Black, size = 18.dp)
        }
    }
}
