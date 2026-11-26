/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: EssentialsFloatingToolbar.kt
 * Description: UI layout element for EssentialsFloatingToolbar.kt.
 */

package com.sameerasw.essentials.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.layout
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.utils.HapticUtil

private const val HOLD_START_MS = 350L
private const val HOLD_ARM_MS = 650L

private val TITLE_BLEED_START = 56.dp
private val TITLE_BLEED_END = 4.dp
private val TITLE_FADE = 12.dp
private val LABEL_START_PADDING = 12.dp
private val LABEL_END_PADDING = 4.dp
private val LABEL_FADE = 8.dp

private fun pillCoverage(
    bounds: Pair<Float, Float>?,
    pillLeft: Float,
    pillRight: Float,
): Float {
    if (bounds == null) return 0f
    val width = bounds.second - bounds.first
    if (width <= 0f) return 0f
    val overlap = (minOf(bounds.second, pillRight) - maxOf(bounds.first, pillLeft)).coerceAtLeast(0f)
    return (overlap / width).coerceIn(0f, 1f)
}

data class ToolbarItem(
    val iconRes: Int,
    val labelRes: Int,
    val onClick: () -> Unit,
    val badge: (@Composable () -> Unit)? = null,
    val iconScale: Float = 1f,
    val effect: ToolbarIconEffect = ToolbarIconEffect.POP,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun EssentialsFloatingToolbar(
    modifier: Modifier = Modifier,
    // Tabbed Mode
    items: List<ToolbarItem> = emptyList(),
    selectedIndex: Int = -1,
    itemSlots: Int = items.size,
    // Standard Mode
    title: String? = null,
    isBeta: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null,
    // FAB / Action
    fabAction: (() -> Unit)? = null,
    fabIconRes: Int? = null,
    fabContentDescription: String? = null,
    // Slots for custom logic
    floatingActionButton: (@Composable () -> Unit)? = null,
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    expanded: Boolean = true,
    backCollapsed: Boolean = false,
    availableWidth: Dp? = null,
    onLongClick: (() -> Unit)? = null,
    showLabels: Boolean = false,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val holdProgress = remember { Animatable(0f) }
    val itemBounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val pillLeft = remember { Animatable(0f) }
    val pillRight = remember { Animatable(0f) }
    var pillPlaced by remember { mutableStateOf(false) }
    val holdScope = rememberCoroutineScope()
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val screenWidth = availableWidth ?: LocalConfiguration.current.screenWidthDp.dp
    val itemGap = 2.dp
    val slots = maxOf(itemSlots, items.size, 1)
    val hasFab = floatingActionButton != null || onHelpClick != null || (fabAction != null && fabIconRes != null)
    val maxTabbedWidth =
        (screenWidth - 32.dp - 8.dp - if (hasFab) 64.dp else 0.dp).coerceAtLeast(48.dp)
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge
    val selectedItem = items.getOrNull(selectedIndex)
    val selectedLabel = selectedItem?.let { stringResource(it.labelRes) }
    val labelsActive = showLabels && selectedItem != null && selectedLabel != null
    val iconSlotsWidth = (48.dp + itemGap) * (items.size - 1).coerceAtLeast(0)
    val selectedLabelTextWidth =
        remember(selectedLabel, labelStyle, density) {
            if (selectedLabel == null) {
                0.dp
            } else {
                with(density) { textMeasurer.measure(selectedLabel, labelStyle, maxLines = 1, softWrap = false).size.width.toDp() }
            }
        }
    val selectedLabeledWidth =
        if (labelsActive) {
            (LABEL_START_PADDING + 24.dp * selectedItem!!.iconScale + LABEL_FADE * 2 + selectedLabelTextWidth + LABEL_END_PADDING)
                .coerceIn(48.dp, (maxTabbedWidth - iconSlotsWidth).coerceAtLeast(48.dp))
        } else {
            48.dp
        }
    val tabbedWidth =
        if (labelsActive) {
            iconSlotsWidth + selectedLabeledWidth
        } else {
            (48.dp * slots + itemGap * (slots - 1)).coerceAtMost(maxTabbedWidth)
        }
    val titleWidth =
        if (itemSlots > 0 || items.isNotEmpty()) {
            (tabbedWidth - 48.dp - 8.dp).coerceAtLeast(0.dp)
        } else {
            (screenWidth - 32.dp - 56.dp - 8.dp - 16.dp - 48.dp - 8.dp).coerceIn(0.dp, 192.dp)
        }
    val collapse by animateFloatAsState(
        targetValue = if (backCollapsed) 1f else 0f,
        animationSpec = tween(380, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        label = "back_collapse",
    )
    var latestTitle by remember { mutableStateOf(title) }
    var latestOnBack by remember { mutableStateOf(onBackClick) }
    if (title != null) latestTitle = title
    if (onBackClick != null) latestOnBack = onBackClick

    val finalFab: (@Composable () -> Unit)? =
        when {
            floatingActionButton != null -> floatingActionButton
            onHelpClick != null && fabAction == null -> {
                {
                    FloatingActionButton(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            onHelpClick()
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.large,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_help_24),
                            contentDescription = stringResource(R.string.action_help_guide),
                        )
                    }
                }
            }

            fabAction != null && fabIconRes != null -> {
                {
                    FloatingActionButton(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            fabAction()
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = MaterialTheme.shapes.large,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = fabIconRes),
                            contentDescription = fabContentDescription,
                        )
                    }
                }
            }

            else -> null
        }

    HorizontalFloatingToolbar(
        modifier =
            modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, bottom = 0.dp)
                .height(56.dp),
        expanded = expanded,
        contentPadding = PaddingValues(4.dp),
        floatingActionButton = finalFab ?: {},
        scrollBehavior = scrollBehavior,
        colors =
            FloatingToolbarDefaults.vibrantFloatingToolbarColors(
                toolbarContentColor = MaterialTheme.colorScheme.onSurface,
                toolbarContainerColor = MaterialTheme.colorScheme.primary,
            ),
        content = {
            SharedTransitionLayout {
                AnimatedContent(
                    targetState = onBackClick != null,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.85f)) togetherWith
                            (fadeOut() + scaleOut(targetScale = 0.85f)) using SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.Center,
                    label = "toolbar_mode",
                ) { isBackMode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            if (!isBackMode && onLongClick != null) {
                                Modifier.pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                        val early =
                                            withTimeoutOrNull(HOLD_START_MS) {
                                                waitForUpOrCancellation(PointerEventPass.Initial)
                                            }
                                        if (early != null) return@awaitEachGesture
                                        HapticUtil.startTickRampHaptic(context, HOLD_ARM_MS)
                                        val armJob =
                                            holdScope.launch {
                                                holdProgress.animateTo(1f, tween(HOLD_ARM_MS.toInt(), easing = FastOutSlowInEasing))
                                            }
                                        val released =
                                            withTimeoutOrNull(HOLD_ARM_MS) {
                                                while (true) {
                                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                                    if (event.changes.none { it.pressed }) {
                                                        event.changes.forEach { it.consume() }
                                                        break
                                                    }
                                                }
                                            }
                                        if (released != null) {
                                            armJob.cancel()
                                            HapticUtil.stopHoldHaptic(context)
                                            holdScope.launch { holdProgress.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                            return@awaitEachGesture
                                        }
                                        HapticUtil.performOpenClickHaptic(context)
                                        currentOnLongClick?.invoke()
                                        do {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            event.changes.forEach { it.consume() }
                                        } while (event.changes.any { it.pressed })
                                        holdScope.launch { holdProgress.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                    }
                                }
                            } else {
                                Modifier
                            },
                    ) {
                        if (isBackMode) {
                            // BACK BUTTON - Unified with Tabbed style (pop-out effect)
                            IconButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    latestOnBack?.invoke()
                                },
                                modifier =
                                    Modifier
                                        .zIndex(1f)
                                        .sharedBounds(
                                            rememberSharedContentState(key = "toolbar_pill"),
                                            this@AnimatedContent,
                                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                        )
                                        .height(48.dp)
                                        .width(48.dp + (8.dp + titleWidth) * if (latestTitle != null) collapse else 0f),
                                colors =
                                    IconButtonDefaults.filledIconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary,
                                        containerColor = MaterialTheme.colorScheme.background,
                                    ),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_arrow_back_24),
                                    contentDescription = stringResource(R.string.content_desc_back),
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            if (latestTitle != null) {
                                Spacer(modifier = Modifier.width(8.dp * (1f - collapse)))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    modifier =
                                        Modifier
                                            .layout { measurable, constraints ->
                                                val visible = (titleWidth * (1f - collapse)).roundToPx()
                                                val start = TITLE_BLEED_START.roundToPx()
                                                val end = TITLE_BLEED_END.roundToPx()
                                                val placeable =
                                                    measurable.measure(
                                                        constraints.copy(minWidth = visible + start + end, maxWidth = visible + start + end),
                                                    )
                                                layout(visible, placeable.height) {
                                                    placeable.place(-start, 0)
                                                }
                                            }
                                            .graphicsLayer {
                                                alpha = 1f - collapse
                                                compositingStrategy = CompositingStrategy.Offscreen
                                            }
                                            .drawWithContent {
                                                drawContent()
                                                val fade = TITLE_FADE.toPx() / size.width
                                                drawRect(
                                                    brush =
                                                        Brush.horizontalGradient(
                                                            0f to Color.Transparent,
                                                            fade to Color.Black,
                                                            1f - fade to Color.Black,
                                                            1f to Color.Transparent,
                                                        ),
                                                    blendMode = BlendMode.DstIn,
                                                )
                                            }
                                            .align(Alignment.CenterVertically),
                                ) {
                                    AnimatedContent(
                                        targetState = latestTitle,
                                        transitionSpec = {
                                            (fadeIn() + slideInVertically { it / 2 }) togetherWith
                                                (fadeOut() + slideOutVertically { -it / 2 }) using SizeTransform(clip = false)
                                        },
                                        modifier = Modifier.weight(1f, fill = false),
                                        contentAlignment = Alignment.Center,
                                        label = "toolbar_title",
                                    ) { shownTitle ->
                                        TranslatableText(
                                            text = shownTitle ?: "",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.background,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            modifier =
                                                Modifier
                                                    .basicMarquee()
                                                    .padding(start = TITLE_BLEED_START - TITLE_BLEED_END),
                                        )
                                    }
                                    if (isBeta) {
                                        com.sameerasw.essentials.ui.core.cards.FeatureTagIcon(
                                            iconRes = R.drawable.rounded_science_24,
                                            containerColor = MaterialTheme.colorScheme.background,
                                            contentColor = MaterialTheme.colorScheme.primary,
                                            contentDescription = stringResource(R.string.label_beta),
                                        )
                                    }
                                }
                            }
                        } else {
                            // TABBED MODE
                            val fullItemWidth =
                                if (items.isEmpty()) 48.dp else (tabbedWidth - itemGap * (items.size - 1)) / items.size

                            val selectedBounds = itemBounds[selectedIndex]
                            val targetLeft = selectedBounds?.first
                            val targetRight = selectedBounds?.second
                            val snapPill = holdProgress.value > 0f

                            LaunchedEffect(targetLeft, targetRight, snapPill, labelsActive) {
                                if (targetLeft == null || targetRight == null) return@LaunchedEffect
                                if (!pillPlaced || snapPill || labelsActive) {
                                    pillLeft.snapTo(targetLeft)
                                    pillRight.snapTo(targetRight)
                                    pillPlaced = true
                                    return@LaunchedEffect
                                }
                                val movingRight = targetLeft > pillLeft.value
                                val lead = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                val trail = spring<Float>(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
                                launch { pillLeft.animateTo(targetLeft, if (movingRight) trail else lead) }
                                launch { pillRight.animateTo(targetRight, if (movingRight) lead else trail) }
                            }

                            val pillColor = MaterialTheme.colorScheme.background
                            var rowSize by remember { mutableStateOf(IntSize.Zero) }
                            var pillPosition by remember { mutableStateOf(Offset.Zero) }
                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier.onSizeChanged { rowSize = it },
                            ) {
                                if (pillPlaced) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .offset { IntOffset(pillLeft.value.roundToInt(), 0) }
                                                .sharedBounds(
                                                    rememberSharedContentState(key = "toolbar_pill"),
                                                    this@AnimatedContent,
                                                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                                    renderInOverlayDuringTransition = false,
                                                )
                                                .width(with(density) { (pillRight.value - pillLeft.value).coerceAtLeast(0f).toDp() })
                                                .height(48.dp)
                                                .onPlaced { pillPosition = it.positionInParent() }
                                                .drawBehind {
                                                    val pill =
                                                        Path().apply {
                                                            addRoundRect(RoundRect(size.toRect(), CornerRadius(size.height / 2f)))
                                                        }
                                                    val rowRect =
                                                        Rect(
                                                            -pillPosition.x,
                                                            -pillPosition.y,
                                                            rowSize.width - pillPosition.x,
                                                            rowSize.height - pillPosition.y,
                                                        )
                                                    val row =
                                                        Path().apply {
                                                            addRoundRect(RoundRect(rowRect, CornerRadius(rowRect.height / 2f)))
                                                        }
                                                    drawPath(Path.combine(PathOperation.Intersect, pill, row), pillColor)
                                                },
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                items.forEachIndexed { index, item ->
                                    val isSelected = selectedIndex == index

                                    val itemWidth by animateDpAsState(
                                        targetValue =
                                            when {
                                                expanded && labelsActive -> if (isSelected) selectedLabeledWidth else 48.dp
                                                expanded -> fullItemWidth
                                                isSelected -> 48.dp
                                                else -> 0.dp
                                            },
                                        animationSpec =
                                            if (labelsActive) {
                                                tween(durationMillis = 300, easing = LinearOutSlowInEasing)
                                            } else {
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow,
                                                )
                                            },
                                        label = "item_width_$index",
                                    )

                                    val spacerWidth by animateDpAsState(
                                        targetValue = if (index < items.size - 1) itemGap else 0.dp,
                                        animationSpec =
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow,
                                            ),
                                        label = "spacer_width_$index",
                                    )

                                    val hold = holdProgress.value
                                    val shownWidth =
                                        if (isSelected) {
                                            itemWidth + (tabbedWidth - itemWidth) * hold
                                        } else {
                                            itemWidth * (1f - hold)
                                        }

                                    if (itemWidth > 0.dp || isSelected) {
                                        IconButton(
                                            onClick = {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                item.onClick()
                                            },
                                            modifier =
                                                Modifier
                                                    .onPlaced { coords ->
                                                        val x = coords.positionInParent().x
                                                        itemBounds[index] = x to x + coords.size.width
                                                    }
                                                    .width(shownWidth)
                                                    .height(48.dp),
                                            colors =
                                                IconButtonDefaults.iconButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.background,
                                                    containerColor = Color.Transparent,
                                                ),
                                        ) {
                                            val tint =
                                                lerp(
                                                    MaterialTheme.colorScheme.background,
                                                    MaterialTheme.colorScheme.primary,
                                                    if (pillPlaced) pillCoverage(itemBounds[index], pillLeft.value, pillRight.value) else if (isSelected) 1f else 0f,
                                                )
                                            val labelShown = labelsActive && expanded && isSelected
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier =
                                                    if (labelShown) {
                                                        Modifier.padding(start = LABEL_START_PADDING, end = LABEL_END_PADDING)
                                                    } else {
                                                        Modifier
                                                    },
                                            ) {
                                                Box {
                                                    AnimatedToolbarIcon(
                                                        iconRes = if (isSelected && holdProgress.value > 0.5f) R.drawable.rounded_edit_24 else item.iconRes,
                                                        contentDescription = stringResource(id = item.labelRes),
                                                        tint = tint,
                                                        selected = isSelected,
                                                        effect = if (isSelected && holdProgress.value > 0.5f) null else item.effect,
                                                        modifier = Modifier.size(24.dp * item.iconScale),
                                                    )
                                                    if (item.badge != null) {
                                                        Box(
                                                            modifier =
                                                                Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .offset(x = 7.dp, y = (-7).dp),
                                                        ) {
                                                            item.badge.invoke()
                                                        }
                                                    }
                                                }
                                                if (labelShown) {
                                                    Box(
                                                        modifier =
                                                            Modifier
                                                                .weight(1f, fill = false)
                                                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                                                .drawWithContent {
                                                                    drawContent()
                                                                    val fade = LABEL_FADE.toPx() / size.width
                                                                    drawRect(
                                                                        brush =
                                                                            Brush.horizontalGradient(
                                                                                0f to Color.Transparent,
                                                                                fade to Color.Black,
                                                                                1f - fade to Color.Black,
                                                                                1f to Color.Transparent,
                                                                            ),
                                                                        blendMode = BlendMode.DstIn,
                                                                    )
                                                                },
                                                    ) {
                                                        Text(
                                                            text = stringResource(id = item.labelRes),
                                                            style = labelStyle,
                                                            color = tint,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            modifier =
                                                                Modifier
                                                                    .basicMarquee(iterations = Int.MAX_VALUE)
                                                                    .padding(horizontal = LABEL_FADE),
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Animated spacing between buttons
                                        if (index < items.size - 1) {
                                            Spacer(modifier = Modifier.width(spacerWidth * (1f - holdProgress.value)))
                                        }
                                    }
                                }
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}
