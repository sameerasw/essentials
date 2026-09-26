package com.sameerasw.essentials.island.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
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

@Composable
fun IslandStackBubble(icons: List<StackIcon>, size: Dp, modifier: Modifier = Modifier) {
    val shown = icons.takeLast(BUBBLE_MAX_MINIS)
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
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        shown.forEachIndexed { i, icon ->
            key(icon.key) {
                val (tx, ty) = slot(i)
                val x by animateDpAsState(tx, IslandMotion.compactFloat(), label = "bubbleX")
                val y by animateDpAsState(ty, IslandMotion.compactFloat(), label = "bubbleY")
                val s by animateDpAsState(iconSize, IslandMotion.compactFloat(), label = "bubbleSize")
                val enter = remember { Animatable(0f) }
                LaunchedEffect(Unit) { enter.animateTo(1f, IslandMotion.compactFloat()) }
                Box(
                    Modifier
                        .offset(x, y)
                        .graphicsLayer {
                            scaleX = enter.value
                            scaleY = enter.value
                            alpha = enter.value.coerceIn(0f, 1f)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(s), contentAlignment = Alignment.Center) { icon.content(s) }
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
) {
    val ring = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black)
            .animateContentSize(IslandMotion.compactSize)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icons.forEach { icon ->
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
    }
}
