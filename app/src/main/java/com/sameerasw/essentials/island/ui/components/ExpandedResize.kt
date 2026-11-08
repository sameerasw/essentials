package com.sameerasw.essentials.island.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.ui.IslandHaptics
import com.sameerasw.essentials.island.ui.IslandLiveResize
import com.sameerasw.essentials.island.ui.islandTextColor

private val limits = HashMap<String, MutableFloatState>()

class ExpandedResize internal constructor(
    val maxHeight: Dp,
    internal val onMeasured: (Int) -> Unit,
    internal val onStart: () -> Unit,
    internal val onDrag: (Float) -> Unit,
    internal val onEnd: () -> Unit,
)

fun Modifier.resizableCard(resize: ExpandedResize): Modifier =
    heightIn(max = resize.maxHeight).onSizeChanged { resize.onMeasured(it.height) }

@Composable
fun IslandExpandedScope.rememberExpandedResize(prefKey: String): ExpandedResize {
    val context = LocalContext.current
    val density = LocalDensity.current
    val settings = remember { SettingsRepository(context) }
    val limit = remember(prefKey) {
        limits.getOrPut(prefKey) { mutableFloatStateOf(settings.getFloat(prefKey, 0f)) }
    }
    val screenCap = (LocalConfiguration.current.screenHeightDp * 0.8f).dp
    val minHeight = spec.compactHeight + spec.expandedTopPadding + 96.dp
    val maxHeight = limit.floatValue.takeIf { it > 0f }?.dp?.coerceIn(minHeight, screenCap) ?: screenCap
    val measured = remember { IntArray(1) }
    val start = remember { FloatArray(1) }
    return ExpandedResize(
        maxHeight = maxHeight,
        onMeasured = { measured[0] = it },
        onStart = {
            start[0] = measured[0].toFloat()
            IslandLiveResize.active = true
            holdOpen(true)
        },
        onDrag = { totalPx ->
            val target = with(density) { (start[0] + totalPx).toDp() }
            limit.floatValue = target.coerceIn(minHeight, screenCap).value
        },
        onEnd = {
            IslandLiveResize.active = false
            holdOpen(false)
            settings.putFloat(prefKey, limit.floatValue)
        },
    )
}

@Composable
fun ResizeHandle(resize: ExpandedResize, height: Dp) {
    val context = LocalContext.current
    var dragging by remember { mutableStateOf(false) }
    val current by androidx.compose.runtime.rememberUpdatedState(resize)
    DisposableEffect(Unit) {
        onDispose { if (dragging) current.onEnd() }
    }
    val width by animateDpAsState(if (dragging) 56.dp else 36.dp, label = "handleWidth")
    val alpha by animateFloatAsState(if (dragging) 0.85f else 0.4f, label = "handleAlpha")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { IslandHaptics.touchDown(context) },
                    onTap = { IslandHaptics.tap(context) },
                )
            }
            .pointerInput(Unit) {
                var total = 0f
                var lastStep = 0
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        total = 0f
                        lastStep = 0
                        dragging = true
                        IslandHaptics.longPress(context)
                        current.onStart()
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        total += delta.y
                        current.onDrag(total)
                        val step = (total / 24.dp.toPx()).toInt()
                        if (step != lastStep) {
                            lastStep = step
                            IslandHaptics.dragStep(context)
                        }
                    },
                    onDragEnd = {
                        dragging = false
                        IslandHaptics.commit(context)
                        current.onEnd()
                    },
                    onDragCancel = {
                        dragging = false
                        current.onEnd()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(width)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(islandTextColor(alpha)),
        )
    }
}
