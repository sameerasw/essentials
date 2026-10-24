package com.sameerasw.essentials.island.ui

import com.sameerasw.essentials.island.model.SideBubble
import androidx.compose.ui.graphics.lerp
import com.sameerasw.essentials.island.ui.components.LocalIslandIconColor
import com.sameerasw.essentials.island.model.CompactCell
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
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
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
    bond: LiquidBond? = null,
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

    val liquid = rememberLiquidBody(LiquidSpec.Droplet, bond)
    Box(
        modifier = modifier
            .liquid(liquid)
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
                val miniLiquid = rememberLiquidBody(LiquidSpec.Droplet)
                AnimatedVisibility(
                    visibleState = entry.visibility,
                    enter = QueueEnter,
                    exit = QueueExit,
                    modifier = Modifier.offset(x, y).liquid(miniLiquid),
                ) {
                    Box(Modifier.size(s), contentAlignment = Alignment.Center) { entry.icon.content(s) }
                }
            }
        }
    }
}

@Composable
fun IslandQueueCount(
    count: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    outlineColor: Color? = null,
    outlineThickness: Dp = 1.dp,
    bond: LiquidBond? = null,
) {
    val liquid = rememberLiquidBody(LiquidSpec.Droplet, bond)
    Box(
        modifier = modifier
            .liquid(liquid)
            .size(size)
            .clip(CircleShape)
            .background(Color.Black)
            .islandOutline(outlineColor, outlineThickness, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        QueueOverflowCounter(count, size * 0.7f)
    }
}

const val OVERLAP_MAX = 3

private fun queueVisible(icons: List<StackIcon>, max: Int): List<StackIcon> {
    if (icons.size <= max) return icons
    val drawn = icons.take(max - 1)
    return drawn + queueOverflowIcon(icons.size - drawn.size)
}

private const val OVERFLOW_KEY = "queue_overflow"

@Composable
private fun overflowBackground(): Color = lerp(LocalIslandIconColor.current ?: Color.White, Color.Black, 0.65f)

private fun queueOverflowIcon(count: Int) = StackIcon(OVERFLOW_KEY) { size -> QueueOverflowCounter(count, size) }

@Composable
private fun QueueOverflowCounter(count: Int, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(overflowBackground()),
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
                fontSize = with(LocalDensity.current) { (size * if (shownCount > 9) 0.32f else 0.4f).toSp() },
            )
        }
    }
}

private const val OVERLAP_STEP = 0.55f

// Slightly overlapped, so the liquid neck to the counter holds through a bounce
private const val OVERFLOW_STEP = 0.85f

// The +N counter gets a slot of its own so the next icon doesn't sit on top of it
fun stackOverlapWidth(count: Int, size: Dp, maxIcons: Int = OVERLAP_MAX): Dp {
    val gap = if (count > maxIcons) size * (OVERFLOW_STEP - OVERLAP_STEP) else 0.dp
    return size + size * OVERLAP_STEP * (count.coerceIn(1, maxIcons) - 1) + gap
}

@Composable
fun IslandStackOverlap(
    icons: List<StackIcon>,
    size: Dp,
    modifier: Modifier = Modifier,
    outlineColor: Color? = null,
    outlineThickness: Dp = 1.dp,
    bond: LiquidBond? = null,
    maxIcons: Int = OVERLAP_MAX,
) {
    val shown = queueVisible(icons, maxIcons)
    val width by animateDpAsState(stackOverlapWidth(icons.size, size, maxIcons), IslandMotion.expressiveDefaultSpatial(), label = "overlapWidth")
    val liquid = rememberLiquidBody(LiquidSpec.Droplet, bond)
    val field = remember { LiquidField() }
    field.reach = with(LocalDensity.current) { size.toPx() } * 0.3f
    Box(modifier.liquid(liquid).size(width, size)) {
        LiquidBridges(field)
        CompositionLocalProvider(LocalLiquidField provides field) {
            val order = shown.asReversed()
            val step = size * OVERLAP_STEP
            rememberQueueEntries(order).forEach { entry ->
                key(entry.icon.key) {
                    QueueExitWatcher(entry)
                    val held = remember { arrayOf(0.dp) }
                    val index = order.indexOfFirst { it.key == entry.icon.key }
                    val present = index >= 0
                    val counterGap = if (icons.size > maxIcons && entry.icon.key != OVERFLOW_KEY) size * (OVERFLOW_STEP - OVERLAP_STEP) else 0.dp
                    val target = if (present) (step * index + counterGap).also { held[0] = it } else step * (order.size - 1) / 2
                    val spawn = remember { if (entry.visibility.currentState) target else step * (index - 1).coerceAtLeast(0) }
                    val x = remember { Animatable(spawn, Dp.VectorConverter) }
                    LaunchedEffect(target) { x.animateTo(target, IslandMotion.expressiveDefaultSpatial()) }
                    val presence = animateFloatAsState(if (present) 1f else 0f, IslandMotion.expressiveFastEffects(), label = "overlapPresence")
                    val entryBond = remember { LiquidBond("overlap_${entry.icon.key}", presence = { presence.value }) }
                    val entryLiquid = rememberLiquidBody(LiquidSpec.Droplet, entryBond)
                    AnimatedVisibility(
                        visibleState = entry.visibility,
                        enter = QueueEnter,
                        exit = QueueExit,
                        modifier = Modifier
                            .offset { IntOffset(x.value.roundToPx(), 0) }
                            .liquid(entryLiquid),
                    ) {
                        val ringColor = MaterialTheme.colorScheme.primary
                        val ringAlpha by animateFloatAsState(if (entry.icon.highlight) 1f else 0f, IslandMotion.expressiveDefaultEffects(), label = "overlapRing")
                        Box(
                            Modifier
                                .size(size)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .islandOutline(outlineColor, outlineThickness, CircleShape)
                                .then(
                                    if (entry.icon.onSelect != null && !entry.leaving) {
                                        Modifier.pointerInput(entry.icon.key) { detectTapGestures { entry.icon.onSelect.invoke() } }
                                    } else {
                                        Modifier
                                    },
                                )
                                .drawWithContent {
                                    drawContent()
                                    if (ringAlpha > 0f) {
                                        val stroke = 2.5.dp.toPx()
                                        drawCircle(ringColor.copy(alpha = ringAlpha), radius = this.size.minDimension / 2f - stroke / 2f, style = Stroke(stroke))
                                    }
                                },
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
    bond: LiquidBond? = null,
    onInteraction: () -> Unit = {},
) {
    val ring = MaterialTheme.colorScheme.primary
    val liquid = rememberLiquidBody(bond = bond)
    val limit = if (icons.size == PILL_MAX_ICONS + 1) icons.size else PILL_MAX_ICONS
    val maxStart = (icons.size - limit).coerceAtLeast(0)
    var scrollStart by remember { mutableIntStateOf(0) }
    val stepPx = with(LocalDensity.current) { (iconSize + 10.dp).toPx() }
    val selectedIndex = icons.indexOfFirst { it.key == selectedKey }
    LaunchedEffect(selectedKey) {
        if (selectedIndex >= 0) scrollStart = scrollStart.coerceIn((selectedIndex - limit + 1).coerceAtLeast(0), selectedIndex)
    }
    val start = scrollStart.coerceIn(0, maxStart)
    var dragAcc by remember { mutableStateOf(0f) }
    val hapticContext = LocalContext.current
    Row(
        modifier = modifier
            .pointerInput(maxStart, stepPx) {
                detectHorizontalDragGestures(onDragEnd = { dragAcc = 0f }, onDragCancel = { dragAcc = 0f }) { change, amount ->
                    if (maxStart == 0) return@detectHorizontalDragGestures
                    change.consume()
                    onInteraction()
                    dragAcc -= amount
                    while (dragAcc >= stepPx && scrollStart < maxStart) { scrollStart++; dragAcc -= stepPx; IslandHaptics.sliderStep(hapticContext) }
                    while (dragAcc <= -stepPx && scrollStart > 0) { scrollStart--; dragAcc += stepPx; IslandHaptics.sliderStep(hapticContext) }
                    dragAcc = dragAcc.coerceIn(-stepPx, stepPx)
                }
            }
            .liquid(liquid)
            .clip(CircleShape)
            .background(Color.Black)
            .islandOutline(outlineColor, outlineThickness, CircleShape)
            .animateContentSize(IslandMotion.compactSize)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val visible = icons.drop(start).take(limit)
        val overflow = icons.size - start - visible.size
        visible.forEach { icon ->
            key(icon.key) {
                val selected by animateFloatAsState(if (icon.key == selectedKey) 1f else 0f, IslandMotion.float(), label = "stackSelected")
                val enter = remember { Animatable(0f) }
                val iconLiquid = rememberLiquidBody(LiquidSpec.Droplet)
                LaunchedEffect(Unit) { enter.animateTo(1f, IslandMotion.compactFloat()) }
                Box(
                    Modifier
                        .animatePlacement()
                        .liquid(iconLiquid)
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
                                Modifier.pointerInput(icon.key) { detectTapGestures { onInteraction(); icon.onSelect.invoke() } }
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
                        .background(overflowBackground()),
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
    maxIcons: Int = OVERLAP_MAX,
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
                    IslandStackOverlap(bubble.icons, size, outlineColor = outlineColor, outlineThickness = outlineThickness, maxIcons = maxIcons)
                } else {
                    IslandStackBubble(bubble.icons, size, outlineColor = outlineColor, outlineThickness = outlineThickness)
                }
            }
        }
    }
}

fun inlineBubbleCell(bubble: SideBubble, overlap: Boolean): CompactCell = CompactCell("bubble.inline") {
    val tap = Modifier.pointerInput(bubble) { detectTapGestures { bubble.onOpen() } }
    val content = bubble.content
    when {
        content != null -> Box(tap.size(22.dp).clip(CircleShape).background(Color.Black), contentAlignment = Alignment.Center) {
            content(16.dp)
        }
        overlap -> IslandStackOverlap(bubble.icons, 22.dp, modifier = tap)
        else -> IslandStackBubble(bubble.icons, 26.dp, modifier = tap)
    }
}
