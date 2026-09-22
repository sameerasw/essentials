package com.sameerasw.essentials.island.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandStage
import com.sameerasw.essentials.island.state.IslandUiState
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

interface IslandActions {
    fun onTap(itemKey: String?)
    fun onLongPress(itemKey: String?)
    fun onCollapse()
    fun onDismiss(): Boolean
    fun onOpenFocused()
}

private data class ContentKey(val stage: IslandStage, val itemKey: String?)

private const val COLLAPSE_COMMIT = 0.2f

// The window never moves; the surface is always horizontally centred in it (= on the camera).
// Only the surface's width/height/corner animate, read in layout/draw so frames don't recompose.
@Composable
fun IslandRoot(
    state: IslandUiState,
    spec: IslandLayoutSpec,
    actions: IslandActions,
    onTargetBoundsChanged: (IntRect) -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val stage = state.stage
    val key = ContentKey(stage, state.focusedKey)
    val currentState by rememberUpdatedState(state)

    val lastItems = remember { HashMap<String, IslandItem>() }
    state.items.forEach { (k, v) -> lastItems[k] = v }

    val compactHeightPx = with(density) { spec.compactHeight.toPx() }
    val surfaceTopPx = with(density) { spec.surfaceTop.roundToPx() }
    val expandedCornerPx = with(density) { spec.expandedCorner.toPx() }
    val minSurfaceWidth = with(density) { spec.cameraDiameter.roundToPx() }
    val minSurfaceHeight = with(density) { spec.compactHeight.roundToPx() }
    val fallbackCompact = with(density) {
        IntSize((spec.cameraSlotWidth + spec.compactHeight * 2).roundToPx(), spec.compactHeight.roundToPx())
    }

    var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
    val contentAlpha = remember { Animatable(1f) }
    val collapse = remember { Animatable(0f) }
    val dismissOffset = remember { Animatable(0f) }
    var target by remember { mutableStateOf(IntSize.Zero) }
    var compactSize by remember { mutableStateOf(IntSize.Zero) }
    var windowWidth by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(stage != IslandStage.Hidden) }
    var dragCommitted by remember { mutableStateOf(false) }
    val showPreview by remember { derivedStateOf { collapse.value > 0f } }

    LaunchedEffect(key) {
        if (stage != IslandStage.Hidden) visible = true
        if (dragCommitted) {
            contentAlpha.snapTo(1f)
            delay(IslandMotion.COLLAPSE_MS + 400L)
            if (dragCommitted) {
                dragCommitted = false
                collapse.snapTo(0f)
            }
            return@LaunchedEffect
        }
        contentAlpha.snapTo(0f)
        val collapsing = stage == IslandStage.Compact || stage == IslandStage.Hidden
        contentAlpha.animateTo(1f, tween(durationMillis = if (collapsing) 120 else 160, delayMillis = if (collapsing) 0 else 40))
    }
    LaunchedEffect(target, windowWidth) {
        if (target == IntSize.Zero || windowWidth == 0) return@LaunchedEffect
        val left = (windowWidth - target.width) / 2
        onTargetBoundsChanged(IntRect(left, surfaceTopPx, left + target.width, surfaceTopPx + target.height))
    }

    Box(Modifier.fillMaxSize().onSizeChanged { windowWidth = it.width }) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = spec.surfaceTop)
                .graphicsLayer {
                    alpha = if (visible) 1f else 0f
                    // Corner follows the live height so it can never outrun the size animation.
                    val h = this.size.height
                    val t = ((h - compactHeightPx) / compactHeightPx).coerceIn(0f, 1f)
                    val radius = compactHeightPx / 2f + (expandedCornerPx - compactHeightPx / 2f) * t
                    shape = RoundedCornerShape(radius.coerceAtMost(h / 2f))
                    clip = true
                }
                .background(Color.Black)
                // Finger-driven shrink sits inside the clip/background so the pill itself follows the drag.
                .layout { measurable, constraints ->
                    val child = measurable.measure(constraints)
                    val t = collapse.value.coerceIn(0f, 1f)
                    val end = if (compactSize != IntSize.Zero) compactSize else fallbackCompact
                    // Never smaller than the camera, whatever a spring or fling does.
                    val w = (if (t > 0f) lerp(child.width, end.width, t) else child.width).coerceAtLeast(minSurfaceWidth)
                    val h = (if (t > 0f) lerp(child.height, end.height, t) else child.height).coerceAtLeast(minSurfaceHeight)
                    layout(w, h) { child.place((w - child.width) / 2, 0) }
                }
                .onSizeChanged { surfaceSize = it }
                .animateContentSize(
                    animationSpec = if (stage == IslandStage.Expanded || stage == IslandStage.Line) IslandMotion.size else IslandMotion.collapseSize,
                    alignment = Alignment.TopCenter,
                    finishedListener = { _, _ ->
                        if (currentState.stage == IslandStage.Hidden) visible = false
                        if (dragCommitted) {
                            dragCommitted = false
                            scope.launch { collapse.snapTo(0f) }
                        }
                    },
                )
                .pointerInput(stage) {
                    if (stage == IslandStage.Hidden) return@pointerInput
                    detectTapGestures(
                        onTap = { actions.onTap(null) },
                        onLongPress = {
                            HapticUtil.performHeavyHaptic(view)
                            actions.onLongPress(null)
                        },
                    )
                }
                .pointerInput(stage) {
                    if (stage != IslandStage.Line && stage != IslandStage.Expanded) return@pointerInput
                    val collapseRange = 120.dp.toPx()
                    val dismissThreshold = 48.dp.toPx()
                    val flyOff = view.resources.displayMetrics.widthPixels * 0.85f
                    val tracker = VelocityTracker()
                    var startX = 0f
                    var dx = 0f
                    var dy = 0f
                    var towardCamera = false
                    var crossed = false
                    var progress = 0f
                    fun inwardSign(): Float {
                        val cameraX = size.width / 2f
                        return when {
                            startX < cameraX -> 1f
                            startX > cameraX -> -1f
                            else -> if (dx >= 0f) -1f else 1f
                        }
                    }
                    detectDragGestures(
                        onDragStart = { start ->
                            tracker.resetTracking()
                            startX = start.x
                            dx = 0f
                            dy = 0f
                            crossed = false
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            dx += amount.x
                            dy += amount.y
                            val inward = dx * inwardSign()
                            towardCamera = inward > 0f || -dy > abs(dx)
                            val dismissible = currentState.focused?.dismissible == true
                            if (towardCamera) {
                                progress = (maxOf(inward, -dy) / collapseRange).coerceIn(0f, 1f)
                                scope.launch { collapse.snapTo(progress) }
                                scope.launch { dismissOffset.snapTo(0f) }
                            } else {
                                progress = 0f
                                scope.launch { collapse.snapTo(0f) }
                                scope.launch { dismissOffset.snapTo(if (dismissible) dx else dx * 0.25f) }
                            }
                            val now = if (towardCamera) progress > COLLAPSE_COMMIT else dismissible && abs(dx) > dismissThreshold
                            if (now != crossed) {
                                crossed = now
                                HapticUtil.performGestureThresholdHaptic(view)
                            }
                        },
                        onDragEnd = {
                            val v = tracker.calculateVelocity()
                            if (towardCamera) {
                                // Progress keeps going from where the finger left it, carrying its speed.
                                val inwardVelocity = maxOf(v.x * inwardSign(), -v.y) / collapseRange
                                val commit = crossed || inwardVelocity > 2f
                                scope.launch {
                                    if (commit) {
                                        collapse.animateTo(1f, IslandMotion.fling(), initialVelocity = inwardVelocity.coerceIn(0f, 8f))
                                        dragCommitted = true
                                        actions.onCollapse()
                                    } else {
                                        collapse.animateTo(0f, IslandMotion.fling(), initialVelocity = inwardVelocity)
                                    }
                                }
                            } else {
                                val dismissible = currentState.focused?.dismissible == true
                                val commit = dismissible && (crossed || abs(v.x) > 1500f)
                                scope.launch {
                                    if (commit) {
                                        val dir = if ((if (abs(v.x) > 1500f) v.x else dx) > 0f) 1f else -1f
                                        dismissOffset.animateTo(dir * flyOff, IslandMotion.fling(), initialVelocity = v.x)
                                        actions.onDismiss()
                                        dismissOffset.snapTo(0f)
                                    } else {
                                        dismissOffset.animateTo(0f, IslandMotion.fling(), initialVelocity = v.x)
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { collapse.animateTo(0f, IslandMotion.fling()) }
                            scope.launch { dismissOffset.animateTo(0f, IslandMotion.fling()) }
                        },
                    )
                },
        ) {
            val item = key.itemKey?.let { state.items[it] ?: lastItems[it] }
            val previewing = key.stage == IslandStage.Expanded || key.stage == IslandStage.Line
            Box {
            Box(
                Modifier
                    .onSizeChanged {
                        target = it
                        if (key.stage == IslandStage.Compact) compactSize = it
                    }
                    .graphicsLayer {
                        alpha = contentAlpha.value * (if (previewing) 1f - collapse.value * 1.6f else 1f).coerceIn(0f, 1f) *
                            if (surfaceSize.width > 0) (1f - abs(dismissOffset.value) / surfaceSize.width).coerceIn(0f, 1f) else 1f
                        translationX = dismissOffset.value
                    },
            ) {
                when (key.stage) {
                    IslandStage.Hidden -> Spacer(Modifier.size(spec.cameraDiameter, spec.compactHeight))
                    IslandStage.Compact -> CompactTemplate(
                        state = state,
                        spec = spec,
                        onCellTap = { actions.onTap(it) },
                        onCellLongPress = {
                            HapticUtil.performHeavyHaptic(view)
                            actions.onLongPress(it)
                        },
                    )
                    IslandStage.Line -> item?.line?.let { LineTemplate(it, spec) }
                        ?: Spacer(Modifier.size(spec.lineWidth, spec.compactHeight))
                    IslandStage.Expanded -> item?.let {
                        ExpandedHost(
                            item = it,
                            spec = spec,
                            onCollapse = actions::onCollapse,
                            onDismiss = { actions.onDismiss() },
                            onOpen = actions::onOpenFocused,
                        )
                    }
                }
            }
            if (previewing && showPreview) {
                // Compact content fades in underneath the finger, so release only has to finish the motion.
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = ((collapse.value - 0.3f) / 0.7f).coerceIn(0f, 1f) },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    CompactTemplate(state = state, spec = spec, onCellTap = {}, onCellLongPress = {})
                }
            }
            }
        }
    }
}

private fun lerp(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).roundToInt()
