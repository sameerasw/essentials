package com.sameerasw.essentials.island.ui

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.snapshotFlow
import kotlin.math.hypot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.scale
import com.sameerasw.essentials.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.sameerasw.essentials.island.ui.components.LocalIslandIconColor
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntSizeAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.collectAsState
import com.sameerasw.essentials.island.gestures.CompactGestures
import com.sameerasw.essentials.island.gestures.IslandSlideFeedback
import com.sameerasw.essentials.island.gestures.SlideFeedback
import com.sameerasw.essentials.island.ui.components.SlideFeedbackCompact
import com.sameerasw.essentials.island.model.IslandItem
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.sameerasw.essentials.island.model.SideBubble
import com.sameerasw.essentials.island.model.StackIcon
import com.sameerasw.essentials.island.model.IslandStage
import com.sameerasw.essentials.island.state.IslandUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

interface IslandActions {
    fun onTap(itemKey: String?): Boolean
    fun onLongPress(itemKey: String?)
    fun onCollapse()
    fun onDismiss(): Boolean
    fun onOpenFocused()
    fun onInteraction()
    fun onTextInputChanged(active: Boolean)
    fun onAdvance(): Boolean
    val compactGestures: CompactGestures get() = CompactGestures.None
}

private val LocalLineInset = compositionLocalOf { 0.dp to 0.dp }
private val LocalLineLeading = compositionLocalOf<LineLeading?> { null }

private data class ContentKey(val stage: IslandStage, val itemKey: String?)

private const val COLLAPSE_COMMIT = 0.2f

private enum class SwipeIntent { Hide, Dismiss }

// The window never moves; the surface is always horizontally centred in it (= on the camera).
// Only the surface's width/height/corner animate, read in layout/draw so frames don't recompose.
private fun readableOnDark(color: Color): Color {
    var result = color
    var steps = 0
    while (result.luminance() < 0.4f && steps < 10) {
        result = androidx.compose.ui.graphics.lerp(result, Color.White, 0.12f)
        steps++
    }
    return result
}

@Composable
fun IslandRoot(
    state: IslandUiState,
    targetSpec: IslandLayoutSpec,
    actions: IslandActions,
    onTargetBoundsChanged: (IntRect) -> Unit,
    registerCollapseAnimator: (((() -> Unit) -> Unit)?) -> Unit = {},
    showCameraRing: Boolean = false,
) {
    val primary = MaterialTheme.colorScheme.primary
    val focused = state.items[state.focusedKey]
    val dynamicAccent = (focused?.outlineAccent ?: focused?.accent)
        ?: state.arrangement.visibleItems.firstNotNullOfOrNull { key -> state.items[key]?.let { it.outlineAccent ?: it.accent } }
    val iconBase = if (targetSpec.iconMaterialYou) primary else Color.White
    val iconTarget = if (targetSpec.iconDynamic) {
        dynamicAccent ?: iconBase
    } else {
        targetSpec.iconColorCustom ?: iconBase
    }
    val iconColor by animateColorAsState(readableOnDark(iconTarget), label = "islandIconColor")
    CompositionLocalProvider(
        LocalIslandIconColor provides iconColor,
        LocalIslandTextColor provides if (targetSpec.themedText) pastelOf(iconColor) else null,
    ) {
        IslandRootContent(state, targetSpec, actions, onTargetBoundsChanged, registerCollapseAnimator, showCameraRing)
    }
}

@Composable
private fun IslandRootContent(
    state: IslandUiState,
    targetSpec: IslandLayoutSpec,
    actions: IslandActions,
    onTargetBoundsChanged: (IntRect) -> Unit,
    registerCollapseAnimator: (((() -> Unit) -> Unit)?) -> Unit = {},
    showCameraRing: Boolean = false,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val stage = state.stage
    val key = ContentKey(stage, state.focusedKey)
    val currentState by rememberUpdatedState(state)
    val inspection = LocalInspectionMode.current

    val bondCandidate = targetSpec.bondEdge && stage == IslandStage.Compact
    var bonded by remember { mutableStateOf(false) }
    LaunchedEffect(bondCandidate) {
        if (bondCandidate) {
            delay(BOND_DELAY_MS)
            bonded = true
        } else {
            bonded = false
        }
    }
    val bond by animateFloatAsState(if (bonded) 1f else 0f, motionTween(560, easing = FastOutSlowInEasing), label = "islandBond")
    val presence by animateFloatAsState(targetSpec.cameraPresence, motionTween(420), label = "cameraPresence")
    val spec = if (presence == 1f && bond == 0f) {
        targetSpec
    } else {
        targetSpec.copy(
            cameraPresence = presence,
            cameraDiameter = targetSpec.cameraDiameter * (1f - 0.3f * bond),
            verticalGap = targetSpec.verticalGap + (3.5.dp - targetSpec.verticalGap) * bond,
            cameraGap = targetSpec.cameraGap * (1f - 0.2f * bond),
        )
    }

    val lastItems = remember { HashMap<String, IslandItem>() }
    state.items.forEach { (k, v) -> lastItems[k] = v }

    val compactHeightPx = with(density) { spec.compactHeight.toPx() }
    val surfaceTopPx = with(density) { spec.surfaceTop.roundToPx() }
    val expandedCornerPx = with(density) { spec.expandedCorner.toPx() }
    val surfaceShape = remember(compactHeightPx, expandedCornerPx) {
        object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                val r = surfaceCornerPx(size.height, compactHeightPx, expandedCornerPx)
                return Outline.Rounded(RoundRect(size.toRect(), CornerRadius(r)))
            }
        }
    }

    val outlineAlpha by animateFloatAsState(
        targetValue = if (spec.outlineHiddenWhenExpanded && stage == IslandStage.Expanded) 0f else 1f,
        label = "islandOutlineAlpha",
    )

    val pulseAccent = state.items[state.focusedKey]?.accent
    val dynamicOutline: Color? = if (spec.outlineDynamic) {
        val focused = state.items[state.focusedKey]
        (focused?.outlineAccent ?: focused?.accent)
            ?: state.arrangement.visibleItems.firstNotNullOfOrNull { key -> state.items[key]?.let { it.outlineAccent ?: it.accent } }
    } else {
        null
    }
    val animatedOutlineColor: Color? = spec.outlineColor?.let { base ->
        animateColorAsState(dynamicOutline ?: base, label = "islandOutlineColor").value
    }
    fun outlineFor(fade: Float): Color? = animatedOutlineColor
        ?.takeIf { outlineAlpha > 0f && fade > 0f }
        ?.let { it.copy(alpha = it.alpha * outlineAlpha * fade) }
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(state.focusedKey, state.items[state.focusedKey]?.contentId, spec.pulseShadow) {
        pulse.snapTo(0f)
        if (!spec.pulseShadow || state.focusedKey == null) return@LaunchedEffect
        pulse.animateTo(1f, motionTween(250, easing = LinearEasing))
        pulse.animateTo(0f, motionTween(1200, easing = LinearEasing))
    }
    val minSurfaceWidth = with(density) { spec.cameraDiameter.roundToPx() }
    val minSurfaceHeight = with(density) { spec.compactHeight.roundToPx() }
    val fallbackCompact = with(density) {
        IntSize((spec.cameraSlotWidth + spec.compactHeight * 2).roundToPx(), spec.compactHeight.roundToPx())
    }

    var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
    val backdropSlot = remember { IslandBackdropSlot() }
    val contentAlpha = remember { Animatable(1f) }
    val contentMotion = remember { Animatable(0f) }
    val wiggle = remember { Animatable(0f) }
    val context = LocalContext.current
    // Written while the expanded content is measured, so the surface reads it in the same frame (no stale height
    // from a previously expanded card).
    val expandedHeight = remember { IntArray(1) }
    val edgeShift = remember { mutableFloatStateOf(0f) }
    var previousStage by remember { mutableStateOf(stage) }
    var lastKey by remember { mutableStateOf(key) }
    var outgoing by remember { mutableStateOf<ContentKey?>(null) }
    val outgoingAlpha = remember { Animatable(0f) }
    val outgoingMotion = remember { Animatable(0f) }
    val contentShiftPx = with(density) { 10.dp.toPx() }
    val collapse = remember { Animatable(0f) }
    // Hidden: the width shrinks to a circle first, then the circle shrinks to nothing, so it never turns oval.
    val squash = remember { Animatable(0f) }
    val holdSize = remember { IntArray(2) }
    val dismissOffset = remember { Animatable(0f) }
    var target by remember { mutableStateOf(IntSize.Zero) }
    var compactSize by remember { mutableStateOf(IntSize.Zero) }
    var windowWidth by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(stage != IslandStage.Hidden) }
    var dragCommitted by remember { mutableStateOf(false) }
    // Set when a swipe threw the card away; the next transition must not replay it as outgoing content.
    var dismissCommitted by remember { mutableStateOf(false) }
    val showPreview by remember { derivedStateOf { collapse.value > 0f } }
    var swipeIntent by remember { mutableStateOf<SwipeIntent?>(null) }
    val showDismissReveal by remember { derivedStateOf { dismissOffset.value != 0f } }
    val revealDirection by remember { derivedStateOf { if (dismissOffset.value >= 0f) 1f else -1f } }
    val dismissThresholdPx = with(density) { 48.dp.toPx() }
    val jelly = rememberCompactJellyState()
    val jellyRangePx = with(density) { 90.dp.toPx() }
    var compactLongPressed by remember { mutableStateOf(false) }
    var compactSettled by remember { mutableStateOf(false) }
    var sizeFromStage by remember { mutableStateOf(stage) }

    LaunchedEffect(stage) {
        compactSettled = false
        if (stage == IslandStage.Compact) {
            if (sizeFromStage == IslandStage.Hidden) compactSettled = true
            sizeFromStage = stage
            delay(IslandMotion.COLLAPSE_MS.toLong())
            compactSettled = true
        } else {
            sizeFromStage = stage
        }
    }

    LaunchedEffect(key) {
        if (inspection) return@LaunchedEffect
        if (stage != IslandStage.Hidden) {
            visible = true
        }
        if (stage != IslandStage.Compact) {
            launch { jelly.press.animateTo(0f, CompactJellyState.JellySpring) }
            launch { jelly.releaseStretch(0f, 0f) }
        }
        if (dismissCommitted) {
            dismissOffset.snapTo(0f)
        } else if (dismissOffset.value != 0f) {
            scope.launch { dismissOffset.animateTo(0f, IslandMotion.release()) }
        }
        if (dragCommitted && (stage == IslandStage.Expanded || stage == IslandStage.Line)) {
            dragCommitted = false
        }
        if (!dragCommitted && collapse.value != 0f) {
            launch { collapse.animateTo(0f, IslandMotion.collapseFloat()) }
        }
        if (dragCommitted) {
            previousStage = stage
            lastKey = key
            outgoing = null
            launch { contentAlpha.animateTo(1f, motionTween(durationMillis = 180)) }
            launch { contentMotion.animateTo(0f, IslandMotion.contentSpring()) }
            delay(IslandMotion.COLLAPSE_MS + 400L)
            if (dragCommitted) {
                dragCommitted = false
                collapse.animateTo(0f, IslandMotion.settle())
            }
            return@LaunchedEffect
        }
        val growing = stage.rank >= previousStage.rank
        previousStage = stage
        val leaving = lastKey
        val thrownAway = dismissCommitted
        dismissCommitted = false
        val handOver = leaving != key && leaving.stage != IslandStage.Hidden && !thrownAway
        // Snap before lastKey is published: until then the pending frame override below keeps drawing the handover.
        if (handOver) {
            outgoingAlpha.snapTo(1f)
            outgoingMotion.snapTo(0f)
        }
        // Shrinking content mirrors the outgoing layer: it rises from below while fading in, once that layer is mostly gone.
        contentMotion.snapTo(if (growing) -1f else 1f)
        contentAlpha.snapTo(0f)
        if (handOver) {
            outgoing = leaving
            launch {
                launch { outgoingMotion.animateTo(if (growing) 1f else -1f, IslandMotion.collapseFloat()) }
                outgoingAlpha.animateTo(0f, motionTween(durationMillis = 140))
                if (outgoing == leaving) outgoing = null
            }
        }
        lastKey = key
        launch { contentMotion.animateTo(0f, if (growing) IslandMotion.contentSpring() else IslandMotion.collapseFloat()) }
        contentAlpha.animateTo(1f, motionTween(durationMillis = if (growing) 180 else 160, delayMillis = if (growing) 30 else 100))
    }
    val outsetPx = with(density) { spec.expandedOutset.toPx() }
    val cameraSlotPx = with(density) { spec.cameraSlotWidth.roundToPx() }
    val layerSizes = remember { HashMap<Any, IntSize>() }
    val contentOriginX = when {
        spec.growDirection > 0 -> 0f
        spec.growDirection < 0 -> 1f
        else -> 0.5f
    }

    fun rejectTap() {
        scope.launch {
            val px = with(density) { 4.dp.toPx() }
            for ((i, x) in listOf(-px, px, -px * 0.5f, 0f).withIndex()) {
                wiggle.animateTo(x, motionTween(durationMillis = if (i == 0) 60 else 80))
                if (x != 0f) IslandHaptics.wiggle(context)
            }
        }
    }

    fun handleTap(itemKey: String?) {
        if (actions.onTap(itemKey)) IslandHaptics.tap(context) else rejectTap()
    }

    fun handleLongPress(itemKey: String?) {
        val gestures = actions.compactGestures
        if (currentState.stage == IslandStage.Compact && gestures.hasLongPress) {
            compactLongPressed = true
            if (gestures.longPressOpensBrief) IslandHaptics.openClick(context) else IslandHaptics.commit(context)
            gestures.longPress()
            return
        }
        IslandHaptics.longPress(context)
        actions.onLongPress(itemKey)
    }

    // Non-gesture collapses replay the swipe path: scrub progress to 1, then hand over to compact.
    DisposableEffect(Unit) {
        registerCollapseAnimator { commit ->
            scope.launch {
                if (dismissOffset.value != 0f) launch { dismissOffset.animateTo(0f, IslandMotion.release()) }
                val finished = try {
                    collapse.animateTo(1f, IslandMotion.collapseProgress())
                    true
                } catch (_: CancellationException) {
                    // Interrupted by another animation (a drag, or the stage already changed): still hand over.
                    false
                }
                dragCommitted = finished
                commit()
            }
        }
        onDispose { registerCollapseAnimator(null) }
    }

    fun edgeCorrection(layerStage: IslandStage): Float =
        edgeShift.floatValue - if (layerStage == IslandStage.Expanded) outsetPx else 0f
    val sideBubble = state.sideBubble
    val catchUpShown = sideBubble != null && stage == IslandStage.Compact
    val knownBubbles = remember { HashMap<String, SideBubble>() }
    var lastBubbleKey by remember { mutableStateOf<String?>(null) }
    if (sideBubble != null && state.sideBubbleKey != null) {
        knownBubbles[state.sideBubbleKey] = sideBubble
        lastBubbleKey = state.sideBubbleKey
    }
    val lastCatchUp = lastBubbleKey?.let { knownBubbles[it] }
    val hasCompactCells = state.arrangement.before.isNotEmpty() || state.arrangement.after.isNotEmpty()
    val bubbleOnlyLayout = !hasCompactCells && spec.cameraPresence <= 0f
    val catchUpAnim = remember { Animatable(0f) }
    val liquidField = remember { LiquidField() }
    liquidField.reach = with(density) { spec.cameraGap.toPx() } * 0.8f
    val surfaceBond = remember(compactHeightPx, expandedCornerPx, bubbleOnlyLayout) {
        LiquidBond(
            id = "surface",
            radius = { surfaceCornerPx(it.height, compactHeightPx, expandedCornerPx) },
            presence = { if (!visible) 0f else if (bubbleOnlyLayout) 1f - catchUpAnim.value.coerceIn(0f, 1f) else 1f },
        )
    }
    val surfaceLiquid = rememberLiquidBody(bond = surfaceBond, field = liquidField)
    LaunchedEffect(catchUpShown) {
        if (inspection) {
            catchUpAnim.snapTo(if (catchUpShown) 1f else 0f)
            return@LaunchedEffect
        }
        if (catchUpShown) {
            withTimeoutOrNull(IslandMotion.COLLAPSE_MS * 2L) {
                snapshotFlow { surfaceSize.width to compactSize.width }
                    .first { (live, target) -> target > 0 && abs(live - target) <= 6 }
            }
            catchUpAnim.animateTo(1f, IslandMotion.smoothSpatial())
        } else {
            catchUpAnim.animateTo(0f, if (stage == IslandStage.Hidden) IslandMotion.hideBubble() else IslandMotion.smoothSpatial())
        }
    }
    // Hiding runs in order: secondary bubbles fold back first, then the main pill closes into the camera.
    LaunchedEffect(stage == IslandStage.Hidden) {
        if (inspection) return@LaunchedEffect
        if (stage != IslandStage.Hidden) {
            squash.animateTo(0f, IslandMotion.smoothSpatial())
            return@LaunchedEffect
        }
        withTimeoutOrNull(IslandMotion.HIDE_BUBBLE_MS + 120L) {
            snapshotFlow { catchUpAnim.value }.first { it <= 0.02f }
        }
        squash.animateTo(1f, IslandMotion.squash())
        visible = false
    }

    val bubbleSizePx = with(density) { spec.compactHeight.roundToPx() }
    val compactSlack = remember { mutableIntStateOf(0) }
    val slideFeedback by IslandSlideFeedback.state.collectAsState()
    val feedbackTakeover = slideFeedback.let {
        it is SlideFeedback.Level || it is SlideFeedback.Sound || (it != null && state.arrangement.visibleItems.isEmpty())
    }
    val slackTarget = if (feedbackTakeover && stage == IslandStage.Compact) 0 else compactSlack.intValue
    val compactBaseWidth = remember { mutableIntStateOf(0) }
    val compactNeedLeft = remember { mutableIntStateOf(-1) }
    val bubbleMaxIcons = lastCatchUp?.maxIcons?.let { chosen ->
        if (chosen > 0) {
            chosen
        } else {
            val sizePx = with(density) { spec.compactHeight.toPx() }
            val budget = with(density) { spec.lineWidth.toPx() }
            val gap = with(density) { spec.cameraGap.toPx() }
            val needLeft = compactNeedLeft.intValue
            // Balanced, the bubble's room grows the pill on the far side, so only the near side's content eats into the budget
            val room = if (needLeft >= 0) {
                (budget - cameraSlotPx) / 2f - needLeft - gap
            } else {
                (budget - maxOf(fallbackCompact.width, compactBaseWidth.intValue)) - gap
            }
            (((room - sizePx) / (sizePx * 0.55f)).toInt() + 1).coerceIn(1, 8)
        }
    } ?: OVERLAP_MAX
    val sideBubbleWidth = lastCatchUp
        ?.takeIf { spec.queueStyle == QueueStyle.Overlap && it.content == null && it.icons.isNotEmpty() }
        ?.let { stackOverlapWidth(it.icons.size, spec.compactHeight, bubbleMaxIcons, it.overflow) }
        ?: spec.compactHeight
    val sideWidthPx = with(density) { sideBubbleWidth.roundToPx() }
    val sideWidthAnim = remember { Animatable(sideWidthPx.toFloat()) }
    LaunchedEffect(sideWidthPx) {
        if (inspection) sideWidthAnim.snapTo(sideWidthPx.toFloat()) else sideWidthAnim.animateTo(sideWidthPx.toFloat(), IslandMotion.smoothSpatial())
    }
    fun sideWidthLive(): Int = sideWidthAnim.value.roundToInt()
    val bubbleGapPx = with(density) { spec.cameraGap.roundToPx() }
    fun sideBubbleProgress(): Float =
        if (spec.growDirection == 0 && (hasCompactCells || stage == IslandStage.Hidden)) catchUpAnim.value.coerceIn(0f, 1f) else 0f
    val slackAnim = remember { Animatable(slackTarget.toFloat()) }
    LaunchedEffect(slackTarget) {
        if (inspection) slackAnim.snapTo(slackTarget.toFloat()) else slackAnim.animateTo(slackTarget.toFloat(), IslandMotion.smoothSpatial())
    }
    fun catchShiftPx(): Float = sideBubbleProgress() * slackAnim.value / 2f
    val lineShift = remember { Animatable(0f) }
    val liveSurfaceHeight = remember { mutableIntStateOf(0) }
    fun surfaceShiftFor(width: Int): Int = spec.growDirection * (width - cameraSlotPx) / 2
    fun surfaceLeft(width: Int): Int = windowWidth / 2 - width / 2 + surfaceShiftFor(width) +
        lineShift.value.roundToInt() + catchShiftPx().roundToInt()
    // Bonded to the edge, the bubble closes the gap and slightly overlaps, so the liquid neck merges both into one body
    fun bondedGapPx(): Int = (bubbleGapPx * (1f - bond) - compactHeightPx * 0.08f * bond).roundToInt()
    fun bubbleX(width: Int): Int =
        if (spec.growDirection > 0) surfaceLeft(width) + width + bondedGapPx() else surfaceLeft(width) - bondedGapPx() - sideWidthLive()
    val focusedItem = state.focused
    
    val foreignStack = if (focusedItem != null && focusedItem.stack.isEmpty()) {
        state.items.values.firstOrNull { it.key != focusedItem.key && it.stack.isNotEmpty() }?.stack.orEmpty()
    } else {
        emptyList()
    }
    val fullStack = focusedItem?.stack?.takeIf { it.isNotEmpty() } ?: foreignStack.map { StackIcon(it.key, current = false, onSelect = it.onSelect, content = it.content) }
    val queuedIcons = when {
        foreignStack.isNotEmpty() -> fullStack
        else -> focusedItem?.queued ?: fullStack.filterNot { it.current }
    }
    val inlineLineQueue = spec.queueInline && stage == IslandStage.Line && queuedIcons.isNotEmpty()
    val queueShown = (stage == IslandStage.Expanded || (stage == IslandStage.Line && !spec.queueInline)) && queuedIcons.isNotEmpty()
    var lastFullStack by remember { mutableStateOf(emptyList<StackIcon>()) }
    if (queuedIcons.isNotEmpty()) lastFullStack = fullStack
    var pillSize by remember { mutableStateOf(IntSize.Zero) }
    var lastQueuedIcons by remember { mutableStateOf(emptyList<StackIcon>()) }
    if (queuedIcons.isNotEmpty()) lastQueuedIcons = queuedIcons
    val queueWidth = spec.compactHeight
    val queueWidthPx = with(density) { queueWidth.roundToPx() }
    val queueWidthAnim = remember { Animatable(queueWidthPx.toFloat()) }
    LaunchedEffect(queueWidthPx) {
        if (inspection) queueWidthAnim.snapTo(queueWidthPx.toFloat()) else queueWidthAnim.animateTo(queueWidthPx.toFloat(), IslandMotion.smoothSpatial())
    }
    fun queueWidthLive(): Int = queueWidthAnim.value.roundToInt()
    fun queueX(width: Int): Int =
        if (spec.growDirection > 0) surfaceLeft(width) + width + bubbleGapPx else surfaceLeft(width) - bubbleGapPx - queueWidthLive()
    val inlineQueueIcons = queuedIcons.ifEmpty { lastQueuedIcons }
    val inlineQueue = LineLeading(
        width = when (spec.queueStyle) {
            QueueStyle.Stack -> spec.cellSize
            QueueStyle.Overlap -> stackOverlapWidth(inlineQueueIcons.size, spec.cellSize)
        },
    ) {
        val tapToAdvance = Modifier.pointerInput(Unit) {
            detectTapGestures {
                IslandHaptics.tap(context)
                if (currentState.focused?.queue != null) actions.onAdvance() else lastQueuedIcons.firstOrNull()?.onSelect?.invoke()
            }
        }
        when (spec.queueStyle) {
            QueueStyle.Stack -> IslandStackBubble(inlineQueueIcons, spec.cellSize, modifier = tapToAdvance)
            QueueStyle.Overlap -> IslandStackOverlap(inlineQueueIcons, spec.cellSize, modifier = tapToAdvance)
        }
    }
    val lineInsetActive = queueShown && stage == IslandStage.Line
    val lineInset = if (lineInsetActive) queueWidth + spec.cameraGap else 0.dp
    val lineInsets = if (spec.growDirection > 0) 0.dp to lineInset else lineInset to 0.dp
    val condensedLineActive = spec.growDirection == 0 && stage == IslandStage.Line && focusedItem?.line?.condensed == true
    LaunchedEffect(lineInsetActive, spec.growDirection, condensedLineActive, queueWidthPx) {
        val shift = if (lineInsetActive && spec.growDirection == 0 && !condensedLineActive) {
            (queueWidthPx + bubbleGapPx) / 2f
        } else {
            0f
        }
        if (inspection) lineShift.snapTo(shift) else lineShift.animateTo(shift, IslandMotion.smoothSpatial())
    }
    val queueBelow = remember { Animatable(0f) }
    
    val pillReveal = remember { Animatable(0f) }
    LaunchedEffect(queueShown) {
        val expandedAlready = stage == IslandStage.Expanded && queueBelow.value >= 0.99f
        when {
            queueShown && expandedAlready -> pillReveal.animateTo(1f, IslandMotion.float())
            queueShown -> pillReveal.animateTo(1f, IslandMotion.float())
            expandedAlready -> pillReveal.animateTo(0f, motionTween(240))
            else -> pillReveal.animateTo(0f, motionTween(180))
        }
    }
    LaunchedEffect(stage) {
        if (inspection) {
            queueBelow.snapTo(if (stage == IslandStage.Expanded) 1f else 0f)
            return@LaunchedEffect
        }
        when (stage) {
            IslandStage.Expanded -> queueBelow.animateTo(1f, IslandMotion.float())
            IslandStage.Line -> queueBelow.animateTo(0f, IslandMotion.float())
            else -> Unit
        }
    }

    LaunchedEffect(
        target, windowWidth, stage, queueShown, pillSize, lineInsetActive, catchUpShown, hasCompactCells,
        bubbleOnlyLayout, condensedLineActive, queueWidthPx, sideWidthPx, slackTarget,
    ) {
        if (target == IntSize.Zero || windowWidth == 0) return@LaunchedEffect
        val g = if (stage == IslandStage.Expanded) outsetPx.roundToInt() else 0
        val shift = when {
            spec.growDirection != 0 || condensedLineActive -> 0
            lineInsetActive -> (queueWidthPx + bubbleGapPx) / 2
            catchUpShown && hasCompactCells -> slackTarget / 2
            else -> 0
        }
        val left = windowWidth / 2 - target.width / 2 + surfaceShiftFor(target.width) + shift
        val top = (surfaceTopPx - g).coerceAtLeast(0)
        var bounds = IntRect(left, top, left + target.width, surfaceTopPx - g + target.height)
        if (queueShown && stage == IslandStage.Line) {
            val bx = if (spec.growDirection > 0) left + target.width + bubbleGapPx else left - bubbleGapPx - queueWidthPx
            bounds = IntRect(minOf(bounds.left, bx), bounds.top, maxOf(bounds.right, bx + queueWidthPx), maxOf(bounds.bottom, surfaceTopPx + bubbleSizePx))
        }
        if (catchUpShown && bubbleOnlyLayout) {
            val bx = windowWidth / 2 - sideWidthPx / 2
            bounds = IntRect(bx, surfaceTopPx, bx + sideWidthPx, surfaceTopPx + bubbleSizePx)
        } else if (catchUpShown) {
            val bx = if (spec.growDirection > 0) left + target.width + bubbleGapPx else left - bubbleGapPx - sideWidthPx
            bounds = IntRect(minOf(bounds.left, bx), bounds.top, maxOf(bounds.right, bx + sideWidthPx), maxOf(bounds.bottom, surfaceTopPx + bubbleSizePx))
        }
        if (queueShown && stage == IslandStage.Expanded && pillSize != IntSize.Zero) {
            val pillLeft = left + (target.width - pillSize.width) / 2
            bounds = IntRect(
                minOf(bounds.left, pillLeft),
                bounds.top,
                maxOf(bounds.right, pillLeft + pillSize.width),
                bounds.bottom + with(density) { 2.dp.roundToPx() } + pillSize.height,
            )
        }
        onTargetBoundsChanged(bounds)
    }

    val queuePresence = animateFloatAsState(if (queueShown) 1f else 0f, IslandMotion.expressiveFastEffects(), label = "queuePresence")
    val queueBond = remember {
        LiquidBond("queue", presence = {
            queuePresence.value * (1f - queueBelow.value * 2f).coerceIn(0f, 1f) * (1f - collapse.value).coerceIn(0f, 1f)
        })
    }
    val pillBond = remember {
        LiquidBond("pill", presence = {
            queuePresence.value * ((queueBelow.value - 0.3f) / 0.7f).coerceIn(0f, 1f) * (1f - collapse.value).coerceIn(0f, 1f)
        })
    }

    CompositionLocalProvider(LocalLiquidField provides liquidField) {
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { windowWidth = it.width }
            .pointerInput(stage) {
                if (stage != IslandStage.Expanded) return@pointerInput
                detectTapGestures { actions.onCollapse() }
            },
    ) {
        LiquidBridges(liquidField)
        val pillGapPx = with(density) { 2.dp.roundToPx() }
        AnimatedVisibility(
            visible = queueShown,
            enter = if (stage == IslandStage.Expanded) EnterTransition.None else fadeIn(IslandMotion.contentIn()),
            exit = if (stage == IslandStage.Expanded) fadeOut(motionTween(1, delayMillis = 260)) else fadeOut(IslandMotion.contentOut()),
        ) {
            
            Box(Modifier.fillMaxSize()) {
                val t = { queueBelow.value.coerceIn(0f, 1f) }
                fun Modifier.queueSlot(fromScale: Float, toScale: Float, slides: Boolean = false, alphaOf: (Float) -> Float) =
                    layout { measurable, constraints ->
                        val p = measurable.measure(Constraints())
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            val w = surfaceSize.width
                            val sideX = queueX(w) + queueWidthLive() / 2f
                            val sideY = surfaceTopPx + bubbleSizePx / 2f
                            val belowX = surfaceLeft(w) + w / 2f
                            val belowY = surfaceTopPx + liveSurfaceHeight.intValue + pillGapPx + p.height / 2f
                            val k = t()
                            val cx = sideX + (belowX - sideX) * k
                            val cy = sideY + (belowY - sideY) * k
                            val tuck = if (slides) (1f - pillReveal.value) * (p.height + pillGapPx) else 0f
                            p.placeWithLayer((cx - p.width / 2f).roundToInt(), (cy - p.height / 2f - tuck).roundToInt()) {
                                val s = fromScale + (toScale - fromScale) * k
                                scaleX = s
                                scaleY = s
                                alpha = alphaOf(k) * (1f - collapse.value).coerceIn(0f, 1f)
                            }
                        }
                    }
                val queueModifier = Modifier
                    .queueSlot(1f, 0.6f) { k -> (1f - k * 2f).coerceIn(0f, 1f) }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (currentState.stage != IslandStage.Line) return@detectTapGestures
                            IslandHaptics.tap(context)
                            if (currentState.focused?.queue != null) {
                                actions.onAdvance()
                            } else {
                                lastQueuedIcons.firstOrNull()?.onSelect?.invoke()
                            }
                        }
                    }
                IslandQueueCount(
                    count = lastQueuedIcons.size,
                    size = spec.compactHeight,
                    outlineColor = outlineFor(1f),
                    outlineThickness = spec.outlineThickness,
                    modifier = queueModifier,
                    bond = queueBond,
                )
                IslandStackPill(
                    icons = lastFullStack,
                    selectedKey = lastFullStack.firstOrNull { it.current }?.key,
                    iconSize = 30.dp,
                    interactive = stage == IslandStage.Expanded,
                    outlineColor = outlineFor(1f),
                    outlineThickness = spec.outlineThickness,
                    bond = pillBond,
                    onInteraction = actions::onInteraction,
                    modifier = Modifier
                        .queueSlot(0.5f, 1f, slides = true) { k -> ((k - 0.3f) / 0.7f).coerceIn(0f, 1f) }
                        .onSizeChanged { pillSize = it },
                )
            }
        }
        if (bond > 0.001f && lastCatchUp != null && catchUpAnim.value > 0.5f && !bubbleOnlyLayout) {
            Box(
                Modifier
                    .layout { measurable, constraints ->
                        val w = surfaceSize.width
                        val surfaceStart = surfaceLeft(w)
                        val bubbleStart = bubbleX(w)
                        val left = minOf(bubbleStart, surfaceStart)
                        val right = maxOf(bubbleStart + sideWidthLive(), surfaceStart + w)
                        val body = measurable.measure(Constraints.fixed((right - left).coerceAtLeast(0), compactHeightPx.roundToInt()))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            body.place(left, surfaceTopPx - (surfaceTopPx * bond).roundToInt())
                        }
                    }
                    .graphicsLayer { alpha = bond }
                    .drawBehind {
                        val r = size.height / 2f
                        drawRoundRect(Color.Black, cornerRadius = CornerRadius(r))
                        drawRect(Color.Black, size = Size(size.width, r))
                        val f = compactHeightPx * 0.4f
                        val leftFillet = Path().apply {
                            moveTo(-f, 0f)
                            lineTo(0f, 0f)
                            lineTo(0f, f)
                            arcTo(Rect(Offset(-f, f), f), 0f, -90f, false)
                            close()
                        }
                        val rightFillet = Path().apply {
                            moveTo(size.width + f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width, f)
                            arcTo(Rect(Offset(size.width + f, f), f), 180f, 90f, false)
                            close()
                        }
                        drawPath(leftFillet, Color.Black)
                        drawPath(rightFillet, Color.Black)
                    },
            )
        }
        val bubbleData = lastCatchUp
        if (bubbleData != null && catchUpAnim.value > 0.001f) {
            val dragX = remember { Animatable(0f) }
            val bubbleBond = remember(bubbleOnlyLayout) {
                LiquidBond("sideBubble", presence = { if (bubbleOnlyLayout) catchUpAnim.value.coerceIn(0f, 1f) else 1f })
            }
            val bubbleLiquid = rememberLiquidBody(LiquidSpec.Droplet, bubbleBond, liquidField)
            val towardCamera = if (spec.growDirection > 0) -1f else 1f
            val maxDrag = with(density) { 56.dp.toPx() }
            val threshold = with(density) { 28.dp.toPx() }
            Box(Modifier.fillMaxSize()) {
                IslandSideBubble(
                    ownerKey = lastBubbleKey,
                    bubbleFor = { knownBubbles[it] },
                    size = spec.compactHeight,
                    style = spec.queueStyle,
                    outlineColor = outlineFor(1f),
                    outlineThickness = spec.outlineThickness,
                    maxIcons = bubbleMaxIcons,
                    overflow = lastCatchUp?.overflow ?: com.sameerasw.essentials.island.model.QueueOverflow.Number,
                    modifier = Modifier
                        .offset {
                            if (bubbleOnlyLayout) {
                                IntOffset((windowWidth / 2 - sideWidthLive() / 2 + dragX.value).roundToInt(), surfaceTopPx)
                            } else {
                                val w = surfaceSize.width
                                val endX = bubbleX(w)
                                val startX = if (spec.growDirection > 0) surfaceLeft(w) + w - sideWidthLive() else surfaceLeft(w)
                                val v = catchUpAnim.value
                                IntOffset((startX + (endX - startX) * v + dragX.value).roundToInt(), surfaceTopPx - (surfaceTopPx * bond).roundToInt())
                            }
                        }
                        .liquid(bubbleLiquid)
                        .graphicsLayer {
                            val appear = if (bubbleOnlyLayout || stage == IslandStage.Hidden) catchUpAnim.value.coerceIn(0f, 1f) else 1f
                            alpha = (1f - (abs(dragX.value) / maxDrag).coerceIn(0f, 0.8f)) * appear
                            val appearScale = if (bubbleOnlyLayout || stage == IslandStage.Hidden) 0.6f + 0.4f * appear else 1f
                            scaleX = appearScale
                            scaleY = appearScale
                        }
                        .pointerInput(Unit) {
                            detectTapGestures {
                                IslandHaptics.tap(context)
                                bubbleData.onOpen()
                            }
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, amount ->
                                    if (bubbleData.onDismiss == null) return@detectHorizontalDragGestures
                                    change.consume()
                                    val next = dragX.value + amount
                                    val lo = if (towardCamera > 0f) -with(density) { 8.dp.toPx() } else -maxDrag
                                    val hi = if (towardCamera > 0f) maxDrag else with(density) { 8.dp.toPx() }
                                    scope.launch { dragX.snapTo(next.coerceIn(lo, hi)) }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        if (dragX.value * towardCamera > threshold) {
                                            IslandHaptics.tap(context)
                                            dragX.animateTo(towardCamera * maxDrag, motionTween(100))
                                            bubbleData.onDismiss?.invoke()
                                        } else {
                                            dragX.animateTo(0f, IslandMotion.float())
                                        }
                                    }
                                },
                                onDragCancel = { scope.launch { dragX.animateTo(0f, IslandMotion.float()) } },
                            )
                        },
                )
            }
        }
        val collapseEndTarget = when {
            state.arrangement.visibleItems.isEmpty() -> IntSize(minSurfaceHeight, minSurfaceHeight)
            compactSize != IntSize.Zero -> compactSize
            else -> fallbackCompact
        }
        val collapseEnd by animateIntSizeAsState(collapseEndTarget, IslandMotion.compactSize, label = "collapseEnd")
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = spec.surfaceTop)
                .layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    layout(p.width, p.height) {
                        val dx = surfaceShiftFor(p.width) + lineShift.value.roundToInt() + catchShiftPx().roundToInt()
                        p.place(dx, 0)
                    }
                }
                .offset { IntOffset(0, (-edgeShift.floatValue - surfaceTopPx * bond).roundToInt()) }
                .compactJelly(jelly, jellyRangePx)
                .liquid(surfaceLiquid, TransformOrigin(0.5f, 0f))
                .drawBehind {
                    if (bond <= 0f) return@drawBehind
                    val f = compactHeightPx * 0.4f * bond
                    val left = Path().apply {
                        moveTo(-f, 0f)
                        lineTo(0f, 0f)
                        lineTo(0f, f)
                        arcTo(Rect(Offset(-f, f), f), 0f, -90f, false)
                        close()
                    }
                    val right = Path().apply {
                        moveTo(size.width + f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, f)
                        arcTo(Rect(Offset(size.width + f, f), f), 180f, 90f, false)
                        close()
                    }
                    drawPath(left, Color.Black)
                    drawPath(right, Color.Black)
                }
                .drawBehind {
                    val p = pulse.value
                    if (pulseAccent == null || p <= 0f || !visible) return@drawBehind
                    val w = size.width * spec.pulseSize
                    val h = size.height * spec.pulseSize
                    val center = Offset(size.width / 2f, size.height / 2f + h * spec.pulseYShift)
                    val radius = h / 2f * spec.pulseSpread
                    scale(scaleX = w / h, scaleY = 1f, pivot = center) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                0f to pulseAccent.copy(alpha = 0.95f * p),
                                0.45f to pulseAccent.copy(alpha = 0.55f * p),
                                1f to Color.Transparent,
                                center = center,
                                radius = radius,
                            ),
                            radius = radius,
                            center = center,
                        )
                    }
                }
                .graphicsLayer {
                    alpha = (if (visible) 1f else 0f) * (1f - if (bubbleOnlyLayout) catchUpAnim.value.coerceIn(0f, 1f) else 0f)
                    // Corner follows the live height so it can never outrun the size animation.
                    shape = if (bond > 0f) bondedShape(bond, compactHeightPx, expandedCornerPx) else surfaceShape
                    clip = true
                }
                .background(Color.Black)
                .then(
                    outlineFor(1f - bond)
                        ?.let { Modifier.border(spec.outlineThickness, it, surfaceShape) }
                        ?: Modifier,
                )
                // Finger-driven shrink sits inside the clip/background so the pill itself follows the drag.
                .layout { measurable, constraints ->
                    val child = measurable.measure(constraints)
                    val t = collapse.value.coerceIn(0f, 1f)
                    val end = collapseEnd
                    // Never smaller than the camera, whatever a spring or fling does.
                    val preW = (if (t > 0f) lerp(child.width, end.width, t) else child.width).coerceAtLeast(minSurfaceWidth)
                    val preH = (if (t > 0f) lerp(child.height, end.height, t) else child.height).coerceAtLeast(minSurfaceHeight)
                    val hiding = currentState.stage == IslandStage.Hidden
                    // While hiding, the pill keeps its last visible size until its turn to close into the camera.
                    val baseW = if (hiding && holdSize[0] > 0) holdSize[0] else preW
                    val baseH = if (hiding && holdSize[1] > 0) holdSize[1] else preH
                    val heightProgress = (squash.value / 0.4f).coerceIn(0f, 1f)
                    val widthProgress = ((squash.value - 0.2f) / 0.8f).coerceIn(0f, 1f)
                    val w = lerp(baseW, minSurfaceWidth, widthProgress)
                    val h = lerp(baseH, minSurfaceHeight, heightProgress).coerceAtLeast(minSurfaceHeight)
                    if (!hiding) {
                        holdSize[0] = preW
                        holdSize[1] = preH
                    }
                    liveSurfaceHeight.intValue = h
                    val range = (expandedHeight[0] - minSurfaceHeight).toFloat()
                    val p = if (range > 0f) ((h - minSurfaceHeight) / range).coerceIn(0f, 1f) else 0f
                    edgeShift.floatValue = outsetPx * p
                    val childX = when {
                        spec.growDirection > 0 -> 0
                        spec.growDirection < 0 -> w - child.width
                        else -> (w - child.width) / 2
                    }
                    layout(w, h) { child.place(childX, 0) }
                }
                .onSizeChanged { surfaceSize = it }
                .layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    val floorW = if (stage == IslandStage.Compact) maxOf(minSurfaceWidth, collapseEnd.width) else minSurfaceWidth
                    val w = p.width.coerceAtLeast(floorW)
                    val h = p.height.coerceAtLeast(minSurfaceHeight)
                    layout(w, h) { p.place((w - p.width) / 2, 0) }
                }
                .animateContentSize(
                    animationSpec = when {
                        stage == IslandStage.Expanded || stage == IslandStage.Line -> IslandMotion.size
                        stage == IslandStage.Compact && compactSettled -> IslandMotion.compactSize
                        else -> IslandMotion.collapseSize
                    },
                    alignment = when {
                        spec.growDirection > 0 -> Alignment.TopStart
                        spec.growDirection < 0 -> Alignment.TopEnd
                        else -> Alignment.TopCenter
                    },
                    finishedListener = { _, _ ->
                        if (dragCommitted && currentState.stage != IslandStage.Expanded && currentState.stage != IslandStage.Line) {
                            dragCommitted = false
                            scope.launch { collapse.animateTo(0f, IslandMotion.settle()) }
                        }
                    },
                )
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        actions.onInteraction()
                        val compact = currentState.stage == IslandStage.Compact
                        val rumble = compact && actions.compactGestures.hasLongPress
                        val holdMs = viewConfiguration.longPressTimeoutMillis
                        compactLongPressed = false
                        if (compact) {
                            IslandHaptics.touchDown(context)
                            scope.launch { jelly.pressDown() }
                        }
                        val ramp = if (rumble) {
                            if (actions.compactGestures.longPressOpensBrief) {
                                IslandHaptics.holdTickRampStart(context, holdMs)
                            } else {
                                IslandHaptics.holdRumbleStart(context, holdMs)
                            }
                            null
                        } else {
                            scope.launch {
                                IslandHaptics.RAMP_DELAYS_MS.forEachIndexed { step, wait ->
                                    delay(wait)
                                    IslandHaptics.longPressRamp(context, step)
                                }
                            }
                        }
                        var holding = true
                        while (true) {
                            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            if (holding && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                holding = false
                                ramp?.cancel()
                                if (rumble && !compactLongPressed) IslandHaptics.holdRumbleStop(context)
                            }
                        }
                        ramp?.cancel()
                        // Lifting early cuts the swell; after the press lands it has already finished.
                        if (rumble && holding && !compactLongPressed) IslandHaptics.holdRumbleStop(context)
                        if (compact) scope.launch { jelly.pressUp() }
                    }
                }
                .pointerInput(stage) {
                    if (stage == IslandStage.Hidden) return@pointerInput
                    detectTapGestures(
                        onTap = { handleTap(null) },
                        onLongPress = { handleLongPress(null) },
                    )
                }
                .pointerInput(stage) {
                    if (stage != IslandStage.Compact) return@pointerInput
                    detectCompactGestures(
                        gestures = { actions.compactGestures },
                        jelly = jelly,
                        scope = scope,
                        context = context,
                        isBlocked = { compactLongPressed },
                    )
                }
                .pointerInput(stage) {
                    if (stage != IslandStage.Line && stage != IslandStage.Expanded) return@pointerInput
                    val collapseRange = 120.dp.toPx()
                    val dismissThreshold = 48.dp.toPx()
                    val flyOff = view.resources.displayMetrics.widthPixels * 0.85f
                    val tracker = VelocityTracker()
                    val peek = stage == IslandStage.Line
                    var startX = 0f
                    var dx = 0f
                    var dy = 0f
                    
                    var sliding = false
                    var crossed = false
                    var progress = 0f
                    var lastStep = 0f
                    val stepPx = 16.dp.toPx()
                    fun inwardSign(): Float {
                        val cameraX = when {
                            spec.growDirection > 0 -> cameraSlotPx / 2f
                            spec.growDirection < 0 -> size.width - cameraSlotPx / 2f
                            else -> size.width / 2f
                        }
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
                            lastStep = 0f
                            crossed = false
                            sliding = false
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            dx += amount.x
                            dy += amount.y
                            val inward = dx * inwardSign()
                            val horizontal = abs(dx) >= abs(dy)
                            val dismissible = currentState.focused?.dismissible == true
                            sliding = if (peek) horizontal && inward <= 0f else horizontal
                            if (sliding) {
                                progress = 0f
                                scope.launch { collapse.snapTo(0f) }
                                scope.launch { dismissOffset.snapTo(if (dismissible) dx else dx * 0.25f) }
                            } else {
                                val pull = if (peek) maxOf(inward, -dy) else -dy
                                progress = (pull / collapseRange).coerceIn(0f, 1f)
                                scope.launch { collapse.snapTo(progress) }
                                scope.launch { dismissOffset.snapTo(0f) }
                            }
                            val now = if (sliding) abs(dx) > dismissThreshold && dismissible else progress > COLLAPSE_COMMIT
                            swipeIntent = when {
                                !now -> null
                                !sliding -> SwipeIntent.Hide
                                else -> SwipeIntent.Dismiss
                            }
                            if (now != crossed) {
                                crossed = now
                                if (now) IslandHaptics.thresholdReached(context) else IslandHaptics.thresholdLeft(context)
                            } else {
                                val dist = hypot(dx, dy)
                                if (abs(dist - lastStep) >= stepPx) {
                                    lastStep = dist
                                    IslandHaptics.dragStep(context)
                                }
                            }
                        },
                        onDragEnd = {
                            swipeIntent = null
                            val v = tracker.calculateVelocity()
                            if (sliding) {
                                val focused = currentState.focused
                                val flung = abs(v.x) > 1500f
                                val dir = if ((if (flung) v.x else dx) > 0f) 1f else -1f
                                val commit = focused?.dismissible == true && (crossed || flung)
                                scope.launch {
                                    if (!commit || focused == null) {
                                        dismissOffset.animateTo(0f, IslandMotion.fling(), initialVelocity = v.x)
                                        return@launch
                                    }
                                    IslandHaptics.commit(context)
                                    val remaining = abs(dir * flyOff - dismissOffset.value)
                                    val speed = maxOf(abs(v.x), 2500f)
                                    val throwMs = (remaining / speed * 1000f).toInt().coerceIn(90, 200)
                                    val throwSpec = tween<Float>(throwMs, easing = LinearEasing)
                                    dismissOffset.animateTo(dir * flyOff, throwSpec)
                                    if (focused.queue != null) {
                                        actions.onDismiss()
                                        withTimeoutOrNull(250L) { snapshotFlow { currentState.focused }.first { it !== focused } }
                                    } else {
                                        dismissCommitted = true
                                        actions.onDismiss()
                                    }
                                    dismissOffset.snapTo(0f)
                                }
                            } else {
                                val pullVelocity = (if (peek) maxOf(v.x * inwardSign(), -v.y) else -v.y) / collapseRange
                                val commit = crossed || pullVelocity > 2f
                                scope.launch {
                                    if (commit) {
                                        IslandHaptics.commit(context)
                                        collapse.animateTo(1f, IslandMotion.release(), initialVelocity = pullVelocity.coerceIn(0f, 8f))
                                        dragCommitted = true
                                        actions.onCollapse()
                                    } else {
                                        collapse.animateTo(0f, IslandMotion.release(), initialVelocity = pullVelocity)
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            swipeIntent = null
                            scope.launch { collapse.animateTo(0f, IslandMotion.release()) }
                            scope.launch { dismissOffset.animateTo(0f, IslandMotion.fling()) }
                        },
                    )
                },
        ) {
            val item = key.itemKey?.let { state.items[it] ?: lastItems[it] }
            // The frame the key changes on is drawn before the effect above runs: draw it as the handover's first frame
            // (old content fully visible, new content transparent) instead of flashing the new content.
            val previewing = key.stage == IslandStage.Expanded || key.stage == IslandStage.Line
            val pending = key != lastKey && !(dragCommitted && !previewing)
            val shownOutgoing = (if (pending && lastKey.stage != IslandStage.Hidden && !dismissCommitted) lastKey else outgoing)
                ?.takeIf { it != key }
            val cardCorner = if (key.stage == IslandStage.Expanded) spec.expandedCorner else spec.compactHeight / 2
            val layerAlign = when {
                spec.growDirection > 0 -> Alignment.TopStart
                spec.growDirection < 0 -> Alignment.TopEnd
                else -> Alignment.TopCenter
            }
            val backdrop = backdropSlot.content
            if (key.stage == IslandStage.Expanded && backdrop != null) {
                Box(
                    Modifier
                        .layout { measurable, _ ->
                            val p = measurable.measure(Constraints.fixed(surfaceSize.width.coerceAtLeast(0), surfaceSize.height.coerceAtLeast(0)))
                            layout(0, 0) { p.place(0, 0) }
                        }
                        .graphicsLayer {
                            alpha = contentAlpha.value * (1f - collapse.value * 1.6f).coerceIn(0f, 1f)
                        },
                ) { backdrop() }
            }
            Box(contentAlignment = layerAlign) {
            val queuedNext = item?.queue?.next
            if (showDismissReveal && queuedNext != null) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val p = (abs(dismissOffset.value) / (surfaceSize.width.coerceAtLeast(1) * 0.6f)).coerceIn(0f, 1f)
                            val s = 0.94f + 0.06f * p
                            scaleX = s
                            scaleY = s
                            alpha = 0.4f + 0.6f * p
                            transformOrigin = TransformOrigin(contentOriginX, 0f)
                        },
                    contentAlignment = layerAlign,
                ) {
                    StageContent(key.stage, queuedNext, state, spec, actions, interactive = false)
                }
            } else if (showDismissReveal && item?.dismissible == true) {
                DismissReveal(
                    fromStart = revealDirection > 0f,
                    progress = { (abs(dismissOffset.value) / dismissThresholdPx).coerceIn(0f, 1f) },
                    corner = cardCorner,
                    modifier = Modifier.matchParentSize(),
                )
            }
            // Keyed, so the leaving layer keeps its composition (a Line marquee doesn't restart when it becomes outgoing).
            for (layerKey in listOfNotNull(shownOutgoing, key)) key(layerKey) {
                val current = layerKey == key
                val providedLeading = inlineQueue.takeIf { current && inlineLineQueue }
                val layerModifier = if (current) {
                    Modifier
                        .layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            if (key.stage == IslandStage.Expanded) expandedHeight[0] = p.height
                            layout(p.width, p.height) { p.place(0, 0) }
                        }
                        .onSizeChanged {
                            target = it
                            layerSizes[layerKey] = it
                            if (key.stage == IslandStage.Compact) compactSize = it
                        }
                        .graphicsLayer {
                            alpha = (if (pending) 0f else contentAlpha.value) *
                                (if (previewing) 1f - collapse.value * 1.6f else 1f).coerceIn(0f, 1f) *
                                if (surfaceSize.width > 0) (1f - abs(dismissOffset.value) / surfaceSize.width * 0.6f).coerceIn(0f, 1f) else 1f
                            translationX = dismissOffset.value + wiggle.value
                            val m = contentMotion.value - if (previewing) collapse.value.coerceIn(0f, 1f) else 0f
                            // Only shrink (growing entry, drag preview); rising in from below keeps its size.
                            val scale = 1f + contentScaleFor(key.stage, spec.bondEdge) * m.coerceAtMost(0f)
                            scaleX = scale
                            scaleY = scale
                            translationY = contentShiftPx * m + edgeCorrection(key.stage)
                            transformOrigin = TransformOrigin(contentOriginX, 0f)
                            val sliding = dismissOffset.value != 0f
                            shape = RoundedCornerShape(if (sliding) cardCorner.toPx() else 0f)
                            clip = sliding
                        }
                        .drawBehind { if (dismissOffset.value != 0f) drawRect(Color.Black) }
                } else {
                    Modifier
                        .layout { measurable, _ ->
                            val last = layerSizes[layerKey]
                            val p = measurable.measure(if (last != null) Constraints.fixed(last.width, last.height) else Constraints())
                            val x = when {
                                spec.growDirection > 0 -> 0
                                spec.growDirection < 0 -> -p.width
                                else -> -p.width / 2
                            }
                            layout(0, 0) { p.place(x, 0) }
                        }
                        .graphicsLayer {
                            val m = if (pending) 0f else outgoingMotion.value
                            alpha = if (pending) 1f else outgoingAlpha.value
                            val scale = 1f + contentScaleFor(layerKey.stage, spec.bondEdge) * m
                            scaleX = scale
                            scaleY = scale
                            translationY = contentShiftPx * m + edgeCorrection(layerKey.stage)
                            transformOrigin = TransformOrigin(contentOriginX, 0f)
                        }
                }
                val layerItem = if (current) item else layerKey.itemKey?.let { state.items[it] ?: lastItems[it] }
                Box(layerModifier) {
                    CompositionLocalProvider(
                        LocalIslandBackdropSlot provides backdropSlot.takeIf { current },
                        LocalIslandCollapse provides { if (current) collapse.value else 0f },
                        LocalLineInset provides if (current) lineInsets else (0.dp to 0.dp),
                        LocalLineLeading provides inlineQueue.takeIf { current && inlineLineQueue },
                    ) {
                        StageContent(
                            layerKey.stage, layerItem, state, spec, actions, interactive = current,
                            onCellTap = ::handleTap,
                            onCellLongPress = ::handleLongPress,
                            sideBubble = ::sideBubbleProgress,
                            bubbleExtent = { bubbleGapPx + sideWidthLive() },
                            onSlack = { if (current) compactSlack.intValue = it },
                            onBaseWidth = { if (current) compactBaseWidth.intValue = it },
                            onNeedLeft = { if (current) compactNeedLeft.intValue = it },
                        )
                    }
                }
            }
            SwipeIntentChip(
                intent = swipeIntent.takeIf { key.stage == IslandStage.Expanded },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = spec.expandedOutset + spec.expandedTopPadding + spec.compactHeight + 4.dp),
            )
            if (previewing && showPreview) {
                // Compact content fades in underneath the finger, so release only has to finish the motion.
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val t = collapse.value.coerceIn(0f, 1f)
                            alpha = ((t - 0.3f) / 0.7f).coerceIn(0f, 1f)
                            val m = 1f - t
                            val scale = 1f + contentScaleFor(IslandStage.Compact) * m
                            scaleX = scale
                            scaleY = scale
                            translationY = contentShiftPx * m + edgeCorrection(IslandStage.Compact)
                            transformOrigin = TransformOrigin(contentOriginX, 0f)
                        },
                    contentAlignment = layerAlign,
                ) {
                    CompactTemplate(
                        state = state,
                        spec = spec,
                        onCellTap = {},
                        onCellLongPress = {},
                        sideBubble = ::sideBubbleProgress,
                        bubbleExtent = { bubbleGapPx + sideWidthLive() },
                    )
                }
            }
            }
        }
        if (showCameraRing) {
            val ringColor = com.sameerasw.essentials.island.ui.components.islandAccent()
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 2.dp.toPx()
                val radius = spec.compactHeight.toPx() / 2f
                drawCircle(
                    color = ringColor,
                    radius = radius + stroke / 2f,
                    center = Offset(size.width / 2f, spec.surfaceTop.toPx() + radius),
                    style = Stroke(width = stroke),
                )
            }
        }
    }
    }
}

private const val BOND_DELAY_MS = 3000L

private fun surfaceCornerPx(height: Float, compactHeightPx: Float, expandedCornerPx: Float): Float {
    val t = ((height - compactHeightPx) / compactHeightPx).coerceIn(0f, 1f)
    return (compactHeightPx / 2f + (expandedCornerPx - compactHeightPx / 2f) * t).coerceAtMost(height / 2f)
}

private fun bondedShape(progress: Float, compactHeightPx: Float, expandedCornerPx: Float): Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = ((size.height - compactHeightPx) / compactHeightPx).coerceIn(0f, 1f)
        val r = (compactHeightPx / 2f + (expandedCornerPx - compactHeightPx / 2f) * t).coerceAtMost(size.height / 2f)
        val top = CornerRadius(r * (1f - progress))
        val bottom = CornerRadius(r)
        return Outline.Rounded(RoundRect(size.toRect(), top, top, bottom, bottom))
    }
}

private fun lerp(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).roundToInt()

@Composable
private fun DismissReveal(fromStart: Boolean, progress: () -> Float, corner: Dp, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 20.dp),
        contentAlignment = if (fromStart) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.graphicsLayer {
                val p = progress()
                alpha = p
                translationX = (if (fromStart) -1f else 1f) * (1f - p) * 12.dp.toPx()
            },
        ) {
            Icon(painterResource(R.drawable.rounded_close_24), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.action_dismiss), style = IslandTextStyles.title)
        }
    }
}

private val IslandStage.rank: Int
    get() = when (this) {
        IslandStage.Hidden -> 0
        IslandStage.Compact -> 1
        IslandStage.Line -> 2
        IslandStage.Expanded -> 3
    }

private fun contentScaleFor(stage: IslandStage, flat: Boolean = false): Float = when (stage) {
    IslandStage.Hidden, IslandStage.Compact -> 0.25f
    IslandStage.Line -> 0.1f
    IslandStage.Expanded -> if (flat) 0f else IslandMotion.CONTENT_SCALE
}

private val NoActions = object : IslandActions {
    override fun onTap(itemKey: String?): Boolean = false
    override fun onLongPress(itemKey: String?) {}
    override fun onCollapse() {}
    override fun onDismiss(): Boolean = false
    override fun onOpenFocused() {}
    override fun onInteraction() {}
    override fun onTextInputChanged(active: Boolean) {}
    override fun onAdvance(): Boolean = false
}

@Composable
private fun StageContent(
    stage: IslandStage,
    item: IslandItem?,
    state: IslandUiState,
    spec: IslandLayoutSpec,
    actions: IslandActions,
    interactive: Boolean,
    onCellTap: (String) -> Unit = {},
    onCellLongPress: (String) -> Unit = {},
    sideBubble: () -> Float = { 0f },
    bubbleExtent: () -> Int = { 0 },
    onSlack: (Int) -> Unit = {},
    onBaseWidth: (Int) -> Unit = {},
    onNeedLeft: (Int) -> Unit = {},
) {
    val a = if (interactive) actions else NoActions
    when (stage) {
        IslandStage.Hidden -> Spacer(Modifier.size(spec.compactHeight, spec.compactHeight))
        IslandStage.Compact -> {
            val feedback by IslandSlideFeedback.state.collectAsState()
            val takeover = feedback
            if (takeover is SlideFeedback.Level || takeover is SlideFeedback.Sound || (takeover != null && state.arrangement.visibleItems.isEmpty())) {
                SlideFeedbackCompact(takeover, spec)
            } else {
                CompactTemplate(
                    state = state,
                    spec = spec,
                    onCellTap = { if (interactive) onCellTap(it) },
                    onCellLongPress = { if (interactive) onCellLongPress(it) },
                    sideBubble = sideBubble,
                    bubbleExtent = bubbleExtent,
                    onSlack = onSlack,
                    onBaseWidth = onBaseWidth,
                    onNeedLeft = onNeedLeft,
                )
            }
        }
        IslandStage.Line -> item?.line?.let {
            val inset = LocalLineInset.current
            LineTemplate(it, spec, inset.first, inset.second, leading = LocalLineLeading.current)
        }
            ?: Spacer(Modifier.size(spec.lineWidth, spec.compactHeight))
        IslandStage.Expanded -> item?.let {
            ExpandedHost(
                item = it,
                spec = spec,
                onCollapse = a::onCollapse,
                onDismiss = { a.onDismiss() },
                onOpen = a::onOpenFocused,
                onTextInput = a::onTextInputChanged,
                onKeepAlive = a::onInteraction,
                current = interactive,
            )
        }
    }
}

@Composable
private fun SwipeIntentChip(intent: SwipeIntent?, modifier: Modifier) {
    var shown by remember { mutableStateOf(SwipeIntent.Dismiss) }
    if (intent != null) shown = intent
    AnimatedVisibility(
        visible = intent != null,
        modifier = modifier,
        enter = fadeIn(motionTween(120)) + scaleIn(motionSpring(dampingRatio = 0.55f, stiffness = 600f), initialScale = 0.6f),
        exit = fadeOut(motionTween(100)) + scaleOut(motionTween(100), targetScale = 0.8f),
    ) {
        val hide = shown == SwipeIntent.Hide
        val container = if (hide) Color(0xFFAECBFA) else Color(0xFFF6AEA9)
        val ink = if (hide) Color(0xFF0B2A5B) else Color(0xFF5C1210)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(container)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painterResource(if (hide) R.drawable.rounded_visibility_off_24 else R.drawable.rounded_close_24),
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(16.dp),
            )
            Text(
                stringResource(if (hide) R.string.island_action_hide else R.string.action_dismiss),
                style = IslandTextStyles.line.copy(color = ink, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}
