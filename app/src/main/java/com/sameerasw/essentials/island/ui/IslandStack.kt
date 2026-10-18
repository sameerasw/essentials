package com.sameerasw.essentials.island.ui

import com.sameerasw.essentials.island.model.SideBubble
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.model.StackIcon

private const val BUBBLE_MAX_MINIS = 3
private const val PILL_MAX_ICONS = 5

private class QueueEntry(
    val icon: StackIcon,
    val visibility: MutableTransitionState<Boolean>,
    val leaving: Boolean,
    val onExited: () -> Unit = {},
)

@Composable
private fun rememberQueueEntries(icons: List<StackIcon>): List<QueueEntry> {
    val transitions = remember { HashMap<String, MutableTransitionState<Boolean>>() }
    val previous = remember { arrayListOf<StackIcon>() }
    val settled = remember { booleanArrayOf(false) }
    val exits = remember { mutableIntStateOf(0) }
    exits.intValue
    val currentKeys = icons.mapTo(HashSet()) { it.key }
    val leaving = previous.mapNotNull { icon ->
        if (icon.key in currentKeys) return@mapNotNull null
        val state = transitions[icon.key] ?: return@mapNotNull null
        if (state.isIdle && !state.currentState) {
            transitions.remove(icon.key)
            return@mapNotNull null
        }
        state.targetState = false
        QueueEntry(icon, state, leaving = true, onExited = { exits.intValue++ })
    }
    val current = icons.map { icon ->
        val state = transitions.getOrPut(icon.key) { MutableTransitionState(!settled[0]) }
        state.targetState = true
        QueueEntry(icon, state, leaving = false)
    }
    previous.clear()
    previous.addAll(icons + leaving.map { it.icon })
    SideEffect { settled[0] = true }
    return leaving + current
}

@Composable
private fun QueueExitWatcher(entry: QueueEntry) {
    if (!entry.leaving) return
    LaunchedEffect(entry.visibility) {
        snapshotFlow { entry.visibility.isIdle && !entry.visibility.currentState }.first { it }
        entry.onExited()
    }
}

private val QueueEnter = scaleIn(IslandMotion.expressiveFastSpatial(), initialScale = 0.4f) + fadeIn(IslandMotion.expressiveFastEffects())
private val QueueExit = scaleOut(IslandMotion.expressiveFastEffects(), targetScale = 0.6f) + fadeOut(IslandMotion.expressiveFastEffects())

@Composable
fun IslandStackBubble(
    icons: List<StackIcon>,
    size: Dp,
    modifier: Modifier = Modifier,
    outlineColor: Color? = null,
    outlineThickness: Dp = 1.dp,
) {
    val shown = queueVisible(icons, BUBBLE_MAX_MINIS)
    val count = shown.size
    val iconSize = when (count) {
        1 -> size * 0.62f
        2 -> size * 0.42f
        else -> size * 0.36f
    }
    val r = size * 0.19f
    fun slot(i: Int): Pair<Dp, Dp> = when (count) {
        1 -> 0.dp to 0.dp
        2 -> listOf(-r to 0.dp, r to 0.dp)[i]
        else -> listOf(0.dp to -r, -r to r * 0.7f, r to r * 0.7f)[i]
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black)
            .islandOutline(outlineColor, outlineThickness, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        rememberQueueEntries(shown).forEach { entry ->
            key(entry.icon.key) {
                QueueExitWatcher(entry)
                val held = remember { arrayOfNulls<Pair<Dp, Dp>>(1) }
                val index = shown.indexOfFirst { it.key == entry.icon.key }
                val target = if (index >= 0) slot(index).also { held[0] = it } else held[0] ?: (0.dp to 0.dp)
                val x by animateDpAsState(target.first, IslandMotion.expressiveDefaultSpatial(), label = "bubbleX")
                val y by animateDpAsState(target.second, IslandMotion.expressiveDefaultSpatial(), label = "bubbleY")
                val s by animateDpAsState(iconSize, IslandMotion.expressiveDefaultSpatial(), label = "bubbleSize")
                AnimatedVisibility(
                    visibleState = entry.visibility,
                    enter = QueueEnter,
                    exit = QueueExit,
                    modifier = Modifier.offset(x, y),
                ) {
                    Box(Modifier.size(s), contentAlignment = Alignment.Center) { entry.icon.content(s) }
                }
            }
        }
    }
}

private const val OVERLAP_MAX = 3

private fun queueVisible(icons: List<StackIcon>, max: Int): List<StackIcon> {
    if (icons.size <= max) return icons
    val drawn = icons.take(max - 1)
    return drawn + queueOverflowIcon(icons.size - drawn.size)
}

private fun queueOverflowIcon(count: Int) = StackIcon("queue_overflow") { size -> QueueOverflowCounter(count, size) }

@Composable
private fun QueueOverflowCounter(count: Int, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically(IslandMotion.expressiveFastSpatial()) { if (up) it else -it } + fadeIn(IslandMotion.expressiveFastEffects()))
                    .togetherWith(slideOutVertically(IslandMotion.expressiveFastEffects()) { if (up) -it else it } + fadeOut(IslandMotion.expressiveFastEffects()))
            },
            label = "queueOverflow",
        ) { shownCount ->
            Text(
                text = "+$shownCount",
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                fontWeight = FontWeight.Bold,
                fontSize = with(LocalDensity.current) { (size * if (shownCount > 9) 0.4f else 0.5f).toSp() },
            )
        }
    }
}

private const val OVERLAP_STEP = 0.55f

fun stackOverlapWidth(count: Int, size: Dp): Dp = size + size * OVERLAP_STEP * (count.coerceIn(1, OVERLAP_MAX) - 1)

@Composable
fun IslandStackOverlap(
    icons: List<StackIcon>,
    size: Dp,
    modifier: Modifier = Modifier,
    outlineColor: Color? = null,
    outlineThickness: Dp = 1.dp,
) {
    val shown = queueVisible(icons, OVERLAP_MAX)
    val width by animateDpAsState(stackOverlapWidth(shown.size, size), IslandMotion.expressiveDefaultSpatial(), label = "overlapWidth")
    Box(modifier.size(width, size)) {
        val order = shown.asReversed()
        rememberQueueEntries(order).forEach { entry ->
            key(entry.icon.key) {
                QueueExitWatcher(entry)
                val held = remember { arrayOf(0.dp) }
                val index = order.indexOfFirst { it.key == entry.icon.key }
                val target = if (index >= 0) (size * OVERLAP_STEP * index).also { held[0] = it } else held[0]
                val x by animateDpAsState(target, IslandMotion.expressiveDefaultSpatial(), label = "overlapX")
                AnimatedVisibility(
                    visibleState = entry.visibility,
                    enter = QueueEnter,
                    exit = QueueExit,
                    modifier = Modifier.offset(x = x),
                ) {
                    Box(
                        Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .islandOutline(outlineColor, outlineThickness, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        val iconSize = size * 0.72f
                        Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) { entry.icon.content(iconSize) }
                    }
                }
            }
        }
    }
}

@Composable
fun IslandStackPill(
    icons: List<StackIcon>,
    selectedKey: String?,
    iconSize: Dp,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    outlineColor: Color? = null,
    outlineThickness: Dp = 1.dp,
) {
    val ring = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black)
            .islandOutline(outlineColor, outlineThickness, CircleShape)
            .animateContentSize(IslandMotion.compactSize)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val selectedIndex = icons.indexOfFirst { it.key == selectedKey }
        val limit = if (icons.size == PILL_MAX_ICONS + 1) icons.size else PILL_MAX_ICONS
        val start = if (selectedIndex >= limit) selectedIndex - limit + 1 else 0
        val visible = icons.drop(start).take(limit)
        val overflow = icons.size - visible.size
        visible.forEach { icon ->
            key(icon.key) {
                val selected by animateFloatAsState(if (icon.key == selectedKey) 1f else 0f, IslandMotion.float(), label = "stackSelected")
                val enter = remember { Animatable(0f) }
                LaunchedEffect(Unit) { enter.animateTo(1f, IslandMotion.compactFloat()) }
                Box(
                    Modifier
                        .animatePlacement()
                        .graphicsLayer {
                            scaleX = enter.value
                            scaleY = enter.value
                            alpha = enter.value.coerceIn(0f, 1f)
                        }
                        .clip(CircleShape)
                        .drawBehind {
                            val stroke = 2.dp.toPx()
                            drawCircle(ring.copy(alpha = selected), radius = size.minDimension / 2f - stroke / 2f, style = Stroke(stroke))
                        }
                        .then(
                            if (interactive && icon.onSelect != null) {
                                Modifier.pointerInput(icon.key) { detectTapGestures { icon.onSelect.invoke() } }
                            } else {
                                Modifier
                            },
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) { icon.content(iconSize) }
                }
            }
        }
        if (overflow > 0) {
            key("stack_overflow") {
                val enter = remember { Animatable(0f) }
                LaunchedEffect(Unit) { enter.animateTo(1f, IslandMotion.compactFloat()) }
                Box(
                    Modifier
                        .animatePlacement()
                        .graphicsLayer {
                            scaleX = enter.value
                            scaleY = enter.value
                            alpha = enter.value.coerceIn(0f, 1f)
                        }
                        .padding(4.dp)
                        .size(iconSize)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+$overflow",
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = FontWeight.Bold,
                        fontSize = with(LocalDensity.current) { (iconSize * if (overflow > 9) 0.4f else 0.5f).toSp() },
                    )
                }
            }
        }
    }
}

@Composable
fun IslandSideBubble(
    ownerKey: String?,
    bubbleFor: (String) -> SideBubble?,
    size: Dp,
    modifier: Modifier = Modifier,
    style: QueueStyle = QueueStyle.Stack,
    outlineColor: Color? = null,
    outlineThickness: Dp = 1.dp,
) {
    Box(modifier.height(size)) {
        AnimatedContent(
            targetState = ownerKey,
            transitionSpec = {
                (fadeIn(IslandMotion.expressiveDefaultEffects()) + scaleIn(IslandMotion.expressiveFastSpatial(), initialScale = 0.7f))
                    .togetherWith(fadeOut(IslandMotion.expressiveFastEffects()) + scaleOut(IslandMotion.expressiveFastEffects(), targetScale = 0.8f))
            },
            label = "sideBubble",
        ) { key ->
            val bubble = key?.let(bubbleFor)
            if (bubble != null) {
                val content = bubble.content
                if (content != null) {
                    Box(
                        Modifier
                            .size(size)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .islandOutline(outlineColor, outlineThickness, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { content(size * 0.72f) }
                } else if (style == QueueStyle.Overlap) {
                    IslandStackOverlap(bubble.icons, size, outlineColor = outlineColor, outlineThickness = outlineThickness)
                } else {
                    IslandStackBubble(bubble.icons, size, outlineColor = outlineColor, outlineThickness = outlineThickness)
                }
            }
        }
    }
}
