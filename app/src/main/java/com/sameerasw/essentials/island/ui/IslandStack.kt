package com.sameerasw.essentials.island.ui

import com.sameerasw.essentials.island.ui.components.islandAccent

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
import com.sameerasw.essentials.island.ui.components.LocalIslandBitmapFlat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalContext
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
import com.sameerasw.essentials.island.model.QueueOverflow

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

private fun queueVisible(icons: List<StackIcon>, max: Int, overflow: QueueOverflow = QueueOverflow.Number): List<StackIcon> {
    if (icons.size <= max) return icons
    if (overflow == QueueOverflow.None) return icons.take(max)
    val drawn = icons.take(max - 1)
    return drawn + queueOverflowIcon(icons.size - drawn.size, overflow == QueueOverflow.Dot)
}

private const val OVERFLOW_KEY = "queue_overflow"

@Composable
private fun overflowBackground(): Color = lerp(LocalIslandIconColor.current ?: Color.White, Color.Black, 0.65f)

private fun queueOverflowIcon(count: Int, dot: Boolean) = StackIcon(OVERFLOW_KEY) { size ->
    if (dot) QueueOverflowDot(size) else QueueOverflowCounter(count, size)
}

@Composable
private fun QueueOverflowDot(size: Dp) {
    Box(Modifier.size(size * 0.7f).clip(CircleShape).background(com.sameerasw.essentials.island.ui.components.islandAccent()))
}

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

private fun dotBox(size: Dp) = size * 0.42f

private fun dotBase(size: Dp) = dotBox(size) - size * 0.08f

// The +N counter gets a slot of its own so the next icon doesn't sit on top of it
fun stackOverlapWidth(count: Int, size: Dp, maxIcons: Int = OVERLAP_MAX, overflow: QueueOverflow = QueueOverflow.Number): Dp {
    if (count > maxIcons && overflow == QueueOverflow.Dot) {
        return dotBase(size) + if (maxIcons > 1) size + size * OVERLAP_STEP * (maxIcons - 2) else 0.dp
    }
    val gap = if (count > maxIcons && overflow != QueueOverflow.None) size * (OVERFLOW_STEP - OVERLAP_STEP) else 0.dp
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
    overflow: QueueOverflow = QueueOverflow.Number,
) {
    val shown = queueVisible(icons, maxIcons, overflow)
    val width by animateDpAsState(stackOverlapWidth(icons.size, size, maxIcons, overflow), IslandMotion.expressiveDefaultSpatial(), label = "overlapWidth")
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
                    val counterGap = if (icons.size > maxIcons && overflow != QueueOverflow.None && entry.icon.key != OVERFLOW_KEY) size * (OVERFLOW_STEP - OVERLAP_STEP) else 0.dp
                    val dotMode = icons.size > maxIcons && overflow == QueueOverflow.Dot
                    val isDot = dotMode && entry.icon.key == OVERFLOW_KEY
                    val boxSize = if (isDot) dotBox(size) else size
                    val target = if (present) {
                        (if (dotMode) (if (isDot) 0.dp else dotBase(size) + step * (index - 1)) else step * index + counterGap).also { held[0] = it }
                    } else {
                        step * (order.size - 1) / 2
                    }
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
                            .offset { IntOffset(x.value.roundToPx(), if (isDot) ((size - boxSize) / 2).roundToPx() else 0) }
                            .liquid(entryLiquid),
                    ) {
                        val ringColor = islandAccent()
                        val ringAlpha by animateFloatAsState(if (entry.icon.highlight) 1f else 0f, IslandMotion.expressiveDefaultEffects(), label = "overlapRing")
                        Box(
                            Modifier
                                .size(boxSize)
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
                            val iconSize = boxSize * 0.72f
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
    val ring = islandAccent()
    val liquid = rememberLiquidBody(bond = bond)
    val itemWidth = iconSize + 8.dp
    val spacing = 2.dp
    val carouselState = rememberCarouselState { icons.size }
    val hapticContext = LocalContext.current
    val selectedIndex = icons.indexOfFirst { it.key == selectedKey }
    val programmatic = remember { booleanArrayOf(false) }
    LaunchedEffect(selectedKey) {
        if (selectedIndex < 0) return@LaunchedEffect
        programmatic[0] = true
        try {
            carouselState.animateScrollToItem(selectedIndex)
        } finally {
            programmatic[0] = false
        }
    }
    LaunchedEffect(carouselState) {
        var last = carouselState.currentItem
        snapshotFlow { carouselState.currentItem }.collect {
            if (it != last) {
                last = it
                if (!programmatic[0]) {
                    IslandHaptics.scrollTick(hapticContext)
                    onInteraction()
                }
            }
        }
    }
    LaunchedEffect(carouselState) {
        snapshotFlow { carouselState.isScrollInProgress }.collect { scrolling ->
            while (scrolling && carouselState.isScrollInProgress && !programmatic[0]) {
                onInteraction()
                delay(300)
            }
        }
    }
    val visibleSlots = icons.size.coerceAtMost(PILL_MAX_ICONS)
    val carouselWidth by animateDpAsState((itemWidth + spacing) * visibleSlots + 12.dp, IslandMotion.expressiveDefaultSpatial(), label = "pillWidth")
    Box(
        modifier = modifier
            .liquid(liquid)
            .clip(CircleShape)
            .background(Color.Black)
            .islandOutline(outlineColor, outlineThickness, CircleShape)
            .animateContentSize(IslandMotion.compactSize)
            .padding(vertical = 5.dp),
    ) {
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = itemWidth,
            minSmallItemWidth = 10.dp,
            maxSmallItemWidth = itemWidth,
            itemSpacing = spacing,
            contentPadding = PaddingValues(horizontal = 6.dp),
            modifier = Modifier
                .width(carouselWidth)
                .height(itemWidth),
        ) { index ->
            val icon = icons[index]
            val selected by animateFloatAsState(if (icon.key == selectedKey) 1f else 0f, IslandMotion.float(), label = "stackSelected")
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .fillMaxSize()
                    .maskClip(shape)
                    .maskBorder(BorderStroke(2.dp, ring.copy(alpha = selected)), shape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .then(
                        if (interactive && icon.onSelect != null) {
                            Modifier.pointerInput(icon.key) { detectTapGestures { IslandHaptics.button(hapticContext); onInteraction(); icon.onSelect.invoke() } }
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalIslandBitmapFlat provides true) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { icon.content(itemWidth) }
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
    overflow: QueueOverflow = QueueOverflow.Number,
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
                    IslandStackOverlap(bubble.icons, size, outlineColor = outlineColor, outlineThickness = outlineThickness, maxIcons = maxIcons, overflow = overflow)
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
