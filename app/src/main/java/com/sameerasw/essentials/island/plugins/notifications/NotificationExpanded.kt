package com.sameerasw.essentials.island.plugins.notifications

import com.sameerasw.essentials.island.ui.IslandSurfaceAccent
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
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.IslandHaptics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
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
import com.sameerasw.essentials.translation.TranslatableText
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalWindowInfo
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.lerp
import com.sameerasw.essentials.island.ui.IslandMotion
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
import com.sameerasw.essentials.island.ui.components.ConnectedTextLabel
import com.sameerasw.essentials.island.ui.components.IslandBitmap

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
    IslandSurfaceAccent(glowColor)
    val resize = scope.rememberExpandedResize(SettingsRepository.KEY_ISLAND_NOTIF_MAX_HEIGHT)
    val density = LocalDensity.current
    val imeInset = WindowInsets.ime.getBottom(density)
    var cardTop by remember { mutableIntStateOf(0) }
    var rootHeight by remember { mutableIntStateOf(0) }
    val imeRoom = if (replyAction != null && imeInset > 0 && rootHeight > 0) {
        (rootHeight - imeInset - cardTop - with(density) { IME_MARGIN.roundToPx() }).coerceAtLeast(0)
    } else {
        null
    }
    Box(propagateMinConstraints = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    cardTop = coordinates.positionInRoot().y.toInt()
                    rootHeight = coordinates.findRootCoordinates().size.height
                }
                .then(if (imeRoom != null) Modifier.heightIn(max = with(density) { imeRoom.toDp() }) else Modifier)
                .resizableCard(resize)
                .padding(spec.expandedOutset),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f, fill = false)) {
                Spacer(Modifier.height(spec.expandedTopPadding))
                scope.CameraRow(
                    horizontalPadding = spec.contentGap + spec.expandedCorner * 0.35f,
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
                if (alert.actions.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    ActionBar(
                        actions = alert.actions,
                        replyAction = replyAction,
                        accent = glowColor,
                        scope = scope,
                        modifier = Modifier.padding(horizontal = sidePadding),
                        onAction = { action -> if (action.isQuickReply) replyAction = action else onAction(action) },
                        onSend = { action, text ->
                            scope.setTextInput(false)
                            replyAction = null
                            onReply(action, text)
                        },
                        onCancel = {
                            scope.setTextInput(false)
                            replyAction = null
                        },
                    )
                }
                ResizeHandle(resize, spec.expandedBottomPadding.coerceAtLeast(18.dp))
            }
        }
    }
}

@Composable
private fun ActionBar(
    actions: List<NotificationActionItem>,
    replyAction: NotificationActionItem?,
    accent: Color,
    scope: IslandExpandedScope,
    modifier: Modifier,
    onAction: (NotificationActionItem) -> Unit,
    onSend: (NotificationActionItem, String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var morphKey by remember { mutableStateOf<String?>(null) }
    if (replyAction != null) morphKey = replyAction.actionKey
    val animated by animateFloatAsState(if (replyAction != null) 1f else 0f, IslandMotion.float(), label = "replyMorph")
    val progress = animated.coerceIn(0f, 1f)
    val height = lerp(ACTION_HEIGHT, REPLY_HEIGHT, progress)
    val outer = height / 2
    Row(
        modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(4.dp * (1f - progress)),
    ) {
        actions.forEachIndexed { index, action ->
            val morphing = action.actionKey == morphKey && action.isQuickReply
            if (!morphing && progress >= 0.999f) return@forEachIndexed
            val start = if (index == 0 || actions.size == 1) outer else 4.dp
            val end = if (index == actions.lastIndex || actions.size == 1) outer else 4.dp
            val shape = if (morphing) {
                RoundedCornerShape(lerp(start, outer, progress), lerp(end, outer, progress), lerp(end, outer, progress), lerp(start, outer, progress))
            } else {
                RoundedCornerShape(start, end, end, start)
            }
            Box(
                modifier = Modifier
                    .weight(if (morphing) 1f + progress * 40f else (1f - progress).coerceAtLeast(0.001f))
                    .height(height)
                    .graphicsLayer { if (!morphing) alpha = 1f - progress }
                    .clip(shape)
                    .background(Color.White.copy(alpha = if (morphing) 0.2f - 0.06f * progress else 0.2f))
                    .clickable(
                        enabled = replyAction == null,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White),
                    ) {
                        IslandHaptics.button(context)
                        onAction(action)
                    },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = morphing && replyAction != null,
                    transitionSpec = { fadeIn(IslandMotion.contentIn()) togetherWith fadeOut(IslandMotion.contentOut()) },
                    contentAlignment = Alignment.Center,
                    label = "replyContent",
                ) { input ->
                    if (input) {
                        ReplyInput(
                            accent = accent,
                            scope = scope,
                            onSend = { text -> onSend(action, text) },
                            onCancel = onCancel,
                        )
                    } else if (action.isQuickReply) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ConnectedTextLabel(action.title)
                            Icon(painterResource(R.drawable.rounded_reply_24), contentDescription = null, tint = islandTextColor(), modifier = Modifier.padding(end = 8.dp).size(18.dp))
                        }
                    } else {
                        ConnectedTextLabel(action.title)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyInput(
    accent: Color,
    scope: IslandExpandedScope,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    DisposableEffect(Unit) {
        scope.setTextInput(true)
        onDispose { scope.setTextInput(false) }
    }
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(Unit) {
        snapshotFlow { windowInfo.isWindowFocused }.first { it }
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
    fun cancel() {
        IslandHaptics.button(context)
        keyboard?.hide()
        onCancel()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(REPLY_HEIGHT)
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (text.isEmpty()) {
                TranslatableText(R.string.island_reply_hint, style = IslandTextStyles.body, maxLines = 1)
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
        val empty = text.isBlank()
        val buttonColor by animateColorAsState(if (empty) Color.White.copy(alpha = 0.12f) else accent, label = "replyButton")
        Box(
            modifier = Modifier
                .size(REPLY_HEIGHT - 12.dp)
                .clip(CircleShape)
                .background(buttonColor)
                .clickable { if (empty) cancel() else send() },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = empty,
                transitionSpec = {
                    (fadeIn(IslandMotion.contentIn()) + scaleIn(IslandMotion.contentIn(), initialScale = 0.6f))
                        .togetherWith(fadeOut(IslandMotion.contentOut()) + scaleOut(IslandMotion.contentOut(), targetScale = 0.6f))
                },
                label = "replyButtonIcon",
            ) { isEmpty ->
                if (isEmpty) {
                    IslandIcon(R.drawable.rounded_close_24, tint = Color.White.copy(alpha = 0.8f), size = 20.dp)
                } else {
                    IslandIcon(R.drawable.rounded_send_24, tint = Color.Black, size = 20.dp)
                }
            }
        }
    }
}

private val ACTION_HEIGHT = 44.dp
private val IME_MARGIN = 12.dp
private val REPLY_HEIGHT = 52.dp
