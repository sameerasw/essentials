package com.sameerasw.essentials.island.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = icons.take(BUBBLE_MAX_MINIS),
            contentKey = { list -> list.map { it.key } },
            transitionSpec = {
                (fadeIn(IslandMotion.contentIn()) + scaleIn(IslandMotion.compactFloat(), initialScale = 0.7f)) togetherWith
                    (fadeOut(IslandMotion.contentOut()) + scaleOut(IslandMotion.compactFloat(), targetScale = 0.7f))
            },
            label = "islandBubble",
        ) { shown ->
            Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                when (shown.size) {
                    0 -> Unit
                    1 -> shown[0].content(size * 0.62f)
                    else -> {
                        val mini = size * if (shown.size == 2) 0.42f else 0.36f
                        val r = size * 0.19f
                        val offsets = if (shown.size == 2) {
                            listOf(-r to 0.dp, r to 0.dp)
                        } else {
                            listOf(0.dp to -r, -r to r * 0.7f, r to r * 0.7f)
                        }
                        shown.forEachIndexed { i, icon ->
                            val (x, y) = offsets[i]
                            Box(Modifier.offset(x, y)) { icon.content(mini) }
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
                Box(
                    Modifier
                        .animatePlacement()
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
