package com.sameerasw.essentials.island.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
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

private class CellEntry(val itemKey: String, val cell: CompactCell, val before: Boolean)

@Composable
fun CompactTemplate(
    state: IslandUiState,
    spec: IslandLayoutSpec,
    onCellTap: (String) -> Unit,
    onCellLongPress: (String) -> Unit,
) {
    val cells = buildMap {
        state.items.values.forEach { item -> item.compact.forEach { put(it.key, item.key to it) } }
    }
    val current = state.arrangement.before.reversed().mapNotNull { k -> cells[k]?.let { CellEntry(it.first, it.second, true) } } +
        state.arrangement.after.mapNotNull { k -> cells[k]?.let { CellEntry(it.first, it.second, false) } }

    // Cells that just left stay composed until their exit animation finishes.
    val transitions = remember { HashMap<String, MutableTransitionState<Boolean>>() }
    val previous = remember { arrayListOf<CellEntry>() }
    // Cells present when the template first appears show immediately; only later arrivals animate in.
    val settled = remember { booleanArrayOf(false) }
    val currentKeys = current.map { it.cell.key }.toSet()
    val merged = current.toMutableList()
    previous.forEachIndexed { index, entry ->
        if (entry.cell.key in currentKeys) return@forEachIndexed
        val exitState = transitions[entry.cell.key] ?: return@forEachIndexed
        if (exitState.isIdle && !exitState.currentState) {
            transitions.remove(entry.cell.key)
            return@forEachIndexed
        }
        exitState.targetState = false
        merged.add(index.coerceAtMost(merged.size), entry)
    }
    current.forEach { entry ->
        transitions.getOrPut(entry.cell.key) { MutableTransitionState(!settled[0]) }.targetState = true
    }
    previous.clear()
    previous.addAll(merged)
    SideEffect { settled[0] = true }
    val beforeCount = merged.count { it.before }
    val ordered = merged.filter { it.before } + merged.filterNot { it.before }

    Layout(
        modifier = Modifier.height(spec.compactHeight),
        content = {
            ordered.forEach { entry ->
                key(entry.cell.key) {
                    val visibleState = transitions[entry.cell.key] ?: remember { MutableTransitionState(true) }
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = fadeIn(IslandMotion.contentIn()) + expandHorizontally(IslandMotion.size, expandFrom = Alignment.CenterHorizontally),
                        exit = fadeOut(IslandMotion.contentOut()) + shrinkHorizontally(IslandMotion.size, shrinkTowards = Alignment.CenterHorizontally),
                        modifier = Modifier
                            .animatePlacement()
                            .pointerInput(entry.itemKey) {
                                detectTapGestures(
                                    onTap = { onCellTap(entry.itemKey) },
                                    onLongPress = { onCellLongPress(entry.itemKey) },
                                )
                            },
                    ) {
                        Box(
                            modifier = Modifier.height(spec.cellSize).defaultMinSize(minWidth = spec.cellSize),
                            contentAlignment = Alignment.Center,
                        ) { entry.cell.content() }
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val loose = Constraints(maxHeight = constraints.maxHeight)
        val placeables = measurables.map { it.measure(loose) }
        val spacing = spec.cellSpacing.roundToPx()
        val cameraSlot = spec.cameraSlotWidth.roundToPx()
        val left = placeables.take(beforeCount).filter { it.width > 0 }
        val right = placeables.drop(beforeCount).filter { it.width > 0 }
        fun content(list: List<Placeable>) = if (list.isEmpty()) 0 else list.sumOf { it.width } + spacing * (list.size - 1)
        // Cells hug the outer ends with the same edge padding on both sides; the slack sits around the camera.
        val side = maxOf(content(left), content(right)) + if (left.isEmpty() && right.isEmpty()) 0 else spacing
        val width = side * 2 + cameraSlot
        val height = spec.compactHeight.roundToPx()
        layout(width, height) {
            var x = spacing
            left.forEach {
                it.place(x, (height - it.height) / 2)
                x += it.width + spacing
            }
            x = width - spacing - content(right)
            right.forEach {
                it.place(x, (height - it.height) / 2)
                x += it.width + spacing
            }
        }
    }
}
