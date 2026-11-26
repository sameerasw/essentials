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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
) {
    val view = LocalView.current
    val context = LocalContext.current
    val holdProgress = remember { Animatable(0f) }
    val holdScope = rememberCoroutineScope()
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val screenWidth = availableWidth ?: LocalConfiguration.current.screenWidthDp.dp
    val titleWidth = (screenWidth - 32.dp - 56.dp - 8.dp - 16.dp - 48.dp - 8.dp).coerceIn(0.dp, 192.dp)
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
                                            .width(titleWidth * (1f - collapse))
                                            .clipToBounds()
                                            .graphicsLayer { alpha = 1f - collapse }
                                            .padding(horizontal = 8.dp)
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
                                            modifier = Modifier.basicMarquee(),
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
                            val itemGap = 2.dp
                            val slots = maxOf(itemSlots, items.size, 1)
                            val maxTabbedWidth =
                                (screenWidth - 32.dp - 8.dp - if (finalFab != null) 64.dp else 0.dp).coerceAtLeast(48.dp)
                            val tabbedWidth = (48.dp * slots + itemGap * (slots - 1)).coerceAtMost(maxTabbedWidth)
                            val fullItemWidth =
                                if (items.isEmpty()) 48.dp else (tabbedWidth - itemGap * (items.size - 1)) / items.size

                            items.forEachIndexed { index, item ->
                                val isSelected = selectedIndex == index

                                val itemWidth by animateDpAsState(
                                    targetValue =
                                        when {
                                            expanded -> fullItemWidth
                                            isSelected -> 48.dp
                                            else -> 0.dp
                                        },
                                    animationSpec =
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow,
                                        ),
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
                                                .then(
                                                    if (isSelected) {
                                                        Modifier.sharedBounds(
                                                            rememberSharedContentState(key = "toolbar_pill"),
                                                            this@AnimatedContent,
                                                            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                                                        )
                                                    } else {
                                                        Modifier
                                                    },
                                                )
                                                .width(shownWidth)
                                                .height(48.dp),
                                        colors =
                                            if (isSelected) {
                                                IconButtonDefaults.filledIconButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary,
                                                    containerColor = MaterialTheme.colorScheme.background,
                                                )
                                            } else {
                                                IconButtonDefaults.iconButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.background,
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                )
                                            },
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            Box {
                                                AnimatedToolbarIcon(
                                                    iconRes = if (isSelected && holdProgress.value > 0.5f) R.drawable.rounded_edit_24 else item.iconRes,
                                                    contentDescription = stringResource(id = item.labelRes),
                                                    tint =
                                                        if (isSelected) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.background
                                                        },
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
        },
    )
}
