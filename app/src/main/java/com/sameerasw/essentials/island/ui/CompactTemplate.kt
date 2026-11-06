package com.sameerasw.essentials.island.ui

import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.ui.components.LocalIslandChipInk
import com.sameerasw.essentials.island.ui.components.LocalIslandIconColor
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.state.IslandUiState

@Composable
private fun ChipUnit(color: Color, spec: IslandLayoutSpec, startNeighbor: Boolean, endNeighbor: Boolean, content: @Composable () -> Unit) {
    val container by animateColorAsState(color, label = "islandChip")
    val ink = Color.Black
    val full = spec.cellSize / 2
    val startRadius by animateDpAsState(if (startNeighbor) 6.dp else full, IslandMotion.float(), label = "chipStart")
    val endRadius by animateDpAsState(if (endNeighbor) 6.dp else full, IslandMotion.float(), label = "chipEnd")
    CompositionLocalProvider(
        LocalIslandIconColor provides ink,
        LocalIslandTextColor provides ink,
        LocalIslandChipInk provides ink,
    ) {
        Row(
            modifier = Modifier
                .height(spec.cellSize)
                .defaultMinSize(minWidth = spec.cellSize)
                .clip(RoundedCornerShape(topStart = startRadius, bottomStart = startRadius, topEnd = endRadius, bottomEnd = endRadius))
                .background(container)
                .padding(horizontal = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(spec.cellSpacing / 2),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}

private fun slotChipped(i: Int, beforeSlots: Int, beforeUnits: List<SlotUnit>, afterUnits: List<SlotUnit>): Boolean {
    val unit = if (i < beforeSlots) beforeUnits.getOrNull(beforeSlots - 1 - i) else afterUnits.getOrNull(i - beforeSlots)
    return unit?.chipColor != null
}

private class CellEntry(val itemKey: String, val cell: CompactCell)

private class SlotUnit(
    val key: String,
    val itemKey: String,
    val priority: Int,
    val before: Boolean,
    val cells: List<CellEntry>,
    val chipColor: Color?,
    val sideKeys: List<String>,
)

@Composable
fun CompactTemplate(
    state: IslandUiState,
    spec: IslandLayoutSpec,
    onCellTap: (String) -> Unit,
    onCellLongPress: (String) -> Unit,
    sideBubble: () -> Float = { 0f },
    bubbleExtent: () -> Int = { 0 },
    onSlack: (Int) -> Unit = {},
    onBaseWidth: (Int) -> Unit = {},
    onNeedLeft: (Int) -> Unit = {},
) {
    val iconColor = LocalIslandIconColor.current
    val primary = MaterialTheme.colorScheme.primary
    val cells = buildMap {
        state.items.values.forEach { item -> item.compact.forEach { put(it.key, item.key to it) } }
    }
    val splitAcrossCamera = spec.growDirection == 0 && state.arrangement.visibleItems.size == 1 &&
        state.arrangement.before.isNotEmpty() && state.arrangement.after.isNotEmpty()
    fun chipColorOf(itemKey: String): Color? =
        state.items[itemKey]?.takeIf { spec.chips && it.chip && !splitAcrossCamera }?.let { it.accent ?: iconColor ?: primary }

    fun group(keys: List<String>, before: Boolean): List<SlotUnit> {
        val units = mutableListOf<MutableList<CellEntry>>()
        keys.forEach { k ->
            val (itemKey, cell) = cells[k] ?: return@forEach
            val last = units.lastOrNull()
            if (chipColorOf(itemKey) != null && last != null && last[0].itemKey == itemKey) {
                last.add(CellEntry(itemKey, cell))
            } else {
                units.add(mutableListOf(CellEntry(itemKey, cell)))
            }
        }
        val ids = units.map { it[0].cell.key }
        return units.map {
            SlotUnit(it[0].cell.key, it[0].itemKey, state.items[it[0].itemKey]?.effectivePriority ?: 0, before, it, chipColorOf(it[0].itemKey), ids)
        }
    }

    val beforeUnits = group(state.arrangement.before.reversed(), true).reversed()
    val afterUnits = group(state.arrangement.after, false)

    val known = remember { HashMap<String, SlotUnit>() }
    (beforeUnits + afterUnits).forEach { known[it.key] = it }
    val settled = remember { booleanArrayOf(false) }
    val lingerBefore = remember { intArrayOf(0) }
    val lingerAfter = remember { intArrayOf(0) }
    val trim = remember { androidx.compose.runtime.mutableIntStateOf(0) }
    trim.intValue
    lingerBefore[0] = maxOf(lingerBefore[0], beforeUnits.size)
    lingerAfter[0] = maxOf(lingerAfter[0], afterUnits.size)
    val beforeSlots = lingerBefore[0]
    val afterSlots = lingerAfter[0]
    val beforeCount = beforeSlots

    @Composable
    fun Slot(before: Boolean, index: Int, units: List<SlotUnit>, linger: IntArray) {
        val target = units.getOrNull(index)?.key
        var shown by remember { mutableStateOf(if (settled[0]) null else target) }
        androidx.compose.runtime.LaunchedEffect(target) { shown = target }
        androidx.compose.runtime.LaunchedEffect(index >= units.size, units.size) {
            if (index >= units.size) {
                kotlinx.coroutines.delay(IslandMotion.COMPACT_MS + 80L)
                if (linger[0] > units.size) {
                    linger[0] = units.size
                    trim.intValue++
                }
            }
        }
        val cameraOnEnd = if (spec.growDirection != 0) spec.growDirection < 0 else before
        val towardCamera: (Int) -> Int = { if (cameraOnEnd) it / 2 else -it / 2 }
        val edgeAnchored = spec.growDirection != 0
        AnimatedContent(
            targetState = shown,
            transitionSpec = {
                val from = initialState?.let { known[it] }
                val to = targetState?.let { known[it] }
                val swap = from != null && to != null && from.key !in to.sideKeys && to.key !in from.sideKeys
                val up = (to?.priority ?: 0) < (from?.priority ?: 0)
                val morph = from != null && to != null && from.itemKey == to.itemKey
                val spec2 = when {
                    morph -> fadeIn(motionTween(240, delayMillis = 40)) + scaleIn(IslandMotion.compactFloat(), initialScale = 0.9f) togetherWith
                        fadeOut(motionTween(180)) + scaleOut(IslandMotion.compactFloat(), targetScale = 0.9f)
                    swap -> (fadeIn(IslandMotion.contentIn()) + slideInVertically(IslandMotion.compactOffset) { if (up) it else -it })
                        .togetherWith(fadeOut(IslandMotion.contentOut()) + slideOutVertically(IslandMotion.compactOffset) { if (up) -it else it })
                    else -> (
                        fadeIn(motionTween(260, delayMillis = 60)) +
                            (if (edgeAnchored) EnterTransition.None else slideInHorizontally(IslandMotion.compactOffset, towardCamera)) +
                            scaleIn(IslandMotion.compactFloat(), initialScale = 0.5f)
                        ).togetherWith(
                            fadeOut(motionTween(220)) +
                                (if (edgeAnchored) ExitTransition.None else slideOutHorizontally(IslandMotion.compactOffset, towardCamera)) +
                                scaleOut(IslandMotion.compactFloat(), targetScale = 0.5f),
                        )
                }
                spec2.using(SizeTransform(clip = true) { _, _ -> IslandMotion.compactSize })
            },
            contentAlignment = Alignment.Center,
            label = "compactSlot",
            modifier = Modifier
                .animatePlacement(fromEnd = spec.growDirection == 0 && !before)
                .height(spec.cellSize),
        ) { k ->
            val unit = k?.let { known[it] }
            if (unit != null) {
                val tap = Modifier.pointerInput(unit.itemKey) {
                    detectTapGestures(
                        onTap = { onCellTap(unit.itemKey) },
                        onLongPress = { onCellLongPress(unit.itemKey) },
                    )
                }
                if (unit.chipColor != null) {
                    Box(tap) {
                        val at = units.indexOfFirst { it.key == unit.key }
                        val innerNeighbor = at > 0 && units[at - 1].chipColor != null
                        val outerNeighbor = at >= 0 && units.getOrNull(at + 1)?.chipColor != null
                        ChipUnit(unit.chipColor, spec, startNeighbor = if (before) outerNeighbor else innerNeighbor, endNeighbor = if (before) innerNeighbor else outerNeighbor) {
                            unit.cells.forEach { member ->
                                Box(
                                    modifier = Modifier
                                        .height(spec.cellSize)
                                        .then(
                                            if (member.cell.key.endsWith(".art") || member.cell.key.endsWith(".avatar")) {
                                                Modifier
                                            } else {
                                                Modifier.defaultMinSize(minWidth = spec.cellSize).padding(horizontal = 4.dp)
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) { member.cell.content() }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = tap.height(spec.cellSize).defaultMinSize(minWidth = spec.cellSize),
                        contentAlignment = Alignment.Center,
                    ) { unit.cells[0].cell.content() }
                }
            }
        }
    }

    Layout(
        modifier = Modifier.height(spec.compactHeight),
        content = {
            for (i in beforeSlots - 1 downTo 0) key("b$i") { Slot(true, i, beforeUnits, lingerBefore) }
            for (i in 0 until afterSlots) key("a$i") { Slot(false, i, afterUnits, lingerAfter) }
            SideEffect { settled[0] = true }
        },
    ) { measurables, constraints ->
        val loose = Constraints(maxHeight = constraints.maxHeight)
        val placeables = measurables.map { it.measure(loose) }
        val chipPlaceables = placeables.filterIndexed { i, _ -> slotChipped(i, beforeSlots, beforeUnits, afterUnits) }.toSet()
        val spacing = spec.cellSpacing.roundToPx()
        val cameraSlot = spec.cameraSlotWidth.roundToPx()
        val left = placeables.take(beforeCount).filter { it.width > 0 }
        val right = placeables.drop(beforeCount).filter { it.width > 0 }
        val chipGap = 2.dp.roundToPx()
        fun gapAfter(list: List<Placeable>, i: Int) =
            if (i < list.lastIndex && list[i] in chipPlaceables && list[i + 1] in chipPlaceables) chipGap else spacing
        fun content(list: List<Placeable>) = if (list.isEmpty()) 0 else list.sumOf { it.width } + list.indices.sumOf { if (it < list.lastIndex) gapAfter(list, it) else 0 }
        val height = spec.compactHeight.roundToPx()
        
        val squareCell = spec.cellSize.roundToPx()
        val opticalInset = (height * OPTICAL_INSET_RATIO).toInt()
        fun edgeInset(outer: Placeable?) = if (outer != null && outer.width > squareCell && outer !in chipPlaceables) opticalInset else 0
        if (cameraSlot == 0) {
            val cells = left + right
            onSlack(if (spec.balanceCompact) bubbleExtent().coerceAtLeast(0) else 0)
            if (cells.isEmpty()) return@Layout layout(0, height) {}
            val pad = ((height - squareCell) / 2).coerceAtLeast(0)
            val startInset = edgeInset(cells.first())
            val endInset = edgeInset(cells.last())
            val width = pad * 2 + startInset + endInset + content(cells)
            onBaseWidth(width)
            onNeedLeft(-1)
            return@Layout layout(width, height) {
                var x = pad + startInset
                cells.forEachIndexed { index, it ->
                    it.place(x, (height - it.height) / 2)
                    x += it.width + gapAfter(cells, index)
                }
            }
        }
        if (spec.growDirection != 0) {
            val cells = left + right
            val inset = edgeInset(if (spec.growDirection > 0) cells.lastOrNull() else cells.firstOrNull())
            val body = content(cells) + if (cells.isEmpty()) 0 else spacing + inset
            val width = cameraSlot + body
            onBaseWidth(width)
            onNeedLeft(-1)
            return@Layout layout(width, height) {
                var x = if (spec.growDirection > 0) cameraSlot else spacing + inset
                cells.forEachIndexed { index, it ->
                    it.place(x, (height - it.height) / 2)
                    x += it.width + gapAfter(cells, index)
                }
            }
        }
        // Cells hug the outer ends with the same edge padding on both sides; the slack sits around the camera.
        val leftInset = edgeInset(left.firstOrNull())
        val rightInset = edgeInset(right.lastOrNull())
        val hasCells = left.isNotEmpty() || right.isNotEmpty()
        val edge = if (hasCells) spacing else 0
        val needLeft = content(left) + leftInset + edge
        val needRight = content(right) + rightInset + edge
        val t = if (hasCells) sideBubble().coerceIn(0f, 1f) else 0f
        val mirrored = maxOf(needLeft, needRight)
        val extent = if (spec.balanceCompact) bubbleExtent().coerceAtLeast(0) else 0
        val balancedLeft = if (spec.balanceCompact) maxOf(needLeft, needRight - extent) else needLeft
        val balancedRight = if (spec.balanceCompact) balancedLeft + extent else needRight
        onSlack(if (spec.balanceCompact) extent else needRight - needLeft)
        val leftSide = (mirrored + (balancedLeft - mirrored) * t).roundToInt()
        val rightSide = (mirrored + (balancedRight - mirrored) * t).roundToInt()
        onBaseWidth(mirrored * 2 + cameraSlot)
        onNeedLeft(needLeft)
        val width = leftSide + rightSide + cameraSlot
        layout(width, height) {
            var x = spacing + leftInset
            left.forEachIndexed { index, it ->
                it.place(x, (height - it.height) / 2)
                x += it.width + gapAfter(left, index)
            }
            x = width - spacing - rightInset - content(right)
            right.forEachIndexed { index, it ->
                it.place(x, (height - it.height) / 2)
                x += it.width + gapAfter(right, index)
            }
        }
    }
}

internal const val OPTICAL_INSET_RATIO = 0.15f
