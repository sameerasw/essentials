package com.sameerasw.essentials.island.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val FLING_VELOCITY = 800f

// Material 3 only ships horizontal carousels, so the real one is laid out sideways and rotated a quarter turn; each item is turned back upright
@Composable
fun IslandVerticalCarousel(
    itemCount: Int,
    itemHeight: Dp,
    maxHeight: Dp,
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 8.dp,
    itemShape: RoundedCornerShape = RoundedCornerShape(28.dp),
    content: @Composable CarouselItemScope.(index: Int) -> Unit,
) {
    val state = rememberCarouselState { itemCount }
    val scope = rememberCoroutineScope()
    Layout(
        modifier = modifier.carouselDrag(state, scope) { itemCount },
        content = {
            HorizontalMultiBrowseCarousel(
                state = state,
                preferredItemWidth = itemHeight,
                itemSpacing = itemSpacing,
                userScrollEnabled = false,
            ) { index ->
                Box(Modifier.maskClip(itemShape)) { Upright { content(index) } }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val wanted = ((itemHeight + itemSpacing).toPx() * itemCount).roundToInt()
        val height = minOf(maxHeight.roundToPx(), wanted).coerceAtLeast(itemHeight.roundToPx())
        val inner = measurables.first().measure(Constraints.fixed(height, width))
        layout(width, height) {
            inner.placeWithLayer((width - height) / 2, (height - width) / 2) {
                rotationZ = 90f
                transformOrigin = TransformOrigin.Center
            }
        }
    }
}

@Composable
private fun Upright(content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val placeable = measurables.first().measure(Constraints.fixed(h, w))
        layout(w, h) {
            placeable.placeWithLayer((w - h) / 2, (h - w) / 2) {
                rotationZ = -90f
                transformOrigin = TransformOrigin.Center
            }
        }
    }
}

// Scrolls only while there is room in the drag direction, so a swipe at either end reaches the island's own gestures.
private fun Modifier.carouselDrag(state: CarouselState, scope: CoroutineScope, count: () -> Int): Modifier = pointerInput(state) {
    var snap: Job? = null
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        snap?.cancel()
        val tracker = VelocityTracker()
        var total = Offset.Zero
        var decided = false
        var handle = false
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull() ?: break
            if (change.isConsumed || !change.pressed) break
            tracker.addPosition(change.uptimeMillis, change.position)
            val delta = change.positionChange()
            total += delta
            if (!decided && total.getDistance() > viewConfiguration.touchSlop) {
                decided = true
                handle = abs(total.y) > abs(total.x) &&
                    ((total.y < 0f && state.currentItem < count() - 1) || (total.y > 0f && state.currentItem > 0))
            }
            if (decided && handle) {
                state.dispatchRawDelta(-delta.y)
                change.consume()
            }
        }
        if (handle) {
            val velocity = tracker.calculateVelocity().y
            val target = when {
                velocity < -FLING_VELOCITY -> state.currentItem + 1
                velocity > FLING_VELOCITY -> state.currentItem - 1
                else -> state.currentItem
            }
            snap = scope.launch { state.animateScrollToItem(target.coerceIn(0, (count() - 1).coerceAtLeast(0))) }
        }
    }
}
