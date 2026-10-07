package com.sameerasw.essentials.island.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.model.IslandItem

@Composable
fun ExpandedHost(
    item: IslandItem,
    spec: IslandLayoutSpec,
    onCollapse: () -> Unit,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onTextInput: (Boolean) -> Unit = {},
    onKeepAlive: () -> Unit = {},
) {
    val content = item.expanded ?: return
    val scope = remember(item, spec, onCollapse, onDismiss, onOpen, onTextInput, onKeepAlive) {
        object : IslandExpandedScope {
            override val spec: IslandLayoutSpec = spec
            override val accent: Color = item.accent ?: Color.White
            override fun collapse() = onCollapse()
            override fun dismiss() = onDismiss()
            override fun openApp() = onOpen()
            override fun setTextInput(active: Boolean) = onTextInput(active)
            override fun keepAlive() = onKeepAlive()
        }
    }
    
    val capped = spec.maxExpandedHeight != Dp.Unspecified
    Box(
        Modifier
            .width(spec.expandedWidth + spec.expandedOutset * 2)
            .then(if (capped) Modifier.heightIn(max = spec.maxExpandedHeight) else Modifier)
            .heightIn(min = spec.expandedCorner * 2 + spec.compactHeight + spec.expandedOutset * 2),
        contentAlignment = Alignment.TopStart,
        propagateMinConstraints = true,
    ) {
        val scrollState = rememberScrollState()
        Box(
            if (capped) Modifier.edgeAwareScroll(scrollState) else Modifier,
            propagateMinConstraints = true,
        ) { content.content(scope) }
    }
}

// Scrolls only while there is room in the drag direction, so a swipe at either end reaches the island's own gestures.
private fun Modifier.edgeAwareScroll(state: ScrollState): Modifier =
    pointerInput(state) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var total = Offset.Zero
            var decided = false
            var handle = false
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull() ?: break
                if (change.isConsumed || !change.pressed) break
                val delta = change.positionChange()
                total += delta
                if (!decided && total.getDistance() > viewConfiguration.touchSlop) {
                    decided = true
                    handle = abs(total.y) > abs(total.x) &&
                        ((total.y < 0f && state.canScrollForward) || (total.y > 0f && state.canScrollBackward))
                }
                if (decided && handle) {
                    state.dispatchRawDelta(-delta.y)
                    change.consume()
                }
            }
        }
    }.verticalScroll(state, enabled = false)
