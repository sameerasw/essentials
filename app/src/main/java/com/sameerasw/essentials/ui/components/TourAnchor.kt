/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: TourAnchor.kt
 * Description: Anchors guided tour tooltips to UI elements.
 */

package com.sameerasw.essentials.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.sameerasw.essentials.ui.state.AppTour
import com.sameerasw.essentials.ui.state.TourStep
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourAnchor(
    vararg steps: Pair<TourStep, Boolean>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    steps.forEach { (step, eligible) ->
        if (eligible) {
            DisposableEffect(step) {
                AppTour.attach(step)
                onDispose { AppTour.detach(step) }
            }
        }
    }

    val active = AppTour.current?.takeIf { current -> steps.any { it.first == current && it.second } }
    val tooltipState = rememberTooltipState(isPersistent = true)
    var shownStep by remember { mutableStateOf<TourStep?>(null) }
    if (active != null) shownStep = active
    val dismiss = {
        if (active != null) {
            HapticUtil.performUIHaptic(view)
            AppTour.markSeen(active)
        }
    }

    LaunchedEffect(active) {
        if (active != null) {
            delay(700)
            tooltipState.show()
        } else {
            tooltipState.dismiss()
        }
    }

    val glowColor = MaterialTheme.colorScheme.primary
    TooltipBox(
        positionProvider = rememberTourPositionProvider(),
        tooltip = {
            val step = shownStep ?: return@TooltipBox
            Box(
                modifier =
                    Modifier
                        .padding(TourGlowPadding)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = dismiss,
                        ),
            ) {
                RichTooltip(
                    modifier =
                        Modifier.dropShadow(
                            shape = TourTooltipShape,
                            shadow = Shadow(radius = 20.dp, color = glowColor, alpha = 0.55f),
                        ),
                    shape = TourTooltipShape,
                    caretShape = RoundedCaretShape,
                    colors =
                        TooltipDefaults.richTooltipColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    shadowElevation = 0.dp,
                ) {
                    Text(
                        text = stringResource(step.textRes),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
            }
        },
        state = tooltipState,
        modifier = modifier,
        onDismissRequest = dismiss,
        focusable = true,
        enableUserInput = false,
        content = content,
    )
}

private val TourGlowPadding = 24.dp
private val TourCaretWidth = 24.dp
private val TourCaretHeight = 10.dp
private val TourTooltipShape = RoundedCornerShape(16.dp)

private val RoundedCaretShape =
    object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Outline {
            val w = with(density) { TourCaretWidth.toPx() }
            val h = with(density) { TourCaretHeight.toPx() }
            val half = w / 2
            val fillet = half * 0.3f
            val tipRound = 0.35f
            val path =
                Path().apply {
                    moveTo(-half - fillet, 0f)
                    quadraticTo(-half, 0f, -half * (1 - 0.2f), h * 0.2f)
                    lineTo(-half * tipRound, h * (1 - tipRound))
                    quadraticTo(0f, h, half * tipRound, h * (1 - tipRound))
                    lineTo(half * (1 - 0.2f), h * 0.2f)
                    quadraticTo(half, 0f, half + fillet, 0f)
                    close()
                }
            return Outline.Generic(path)
        }
    }

// The popup is padded for the glow, and the tooltip is edge-aligned to the anchor (not the screen)
// so M3's caret math still lands on the anchor center.
@Composable
private fun rememberTourPositionProvider(): PopupPositionProvider {
    val density = LocalDensity.current
    val pad = with(density) { TourGlowPadding.roundToPx() }
    val spacing = with(density) { (TourCaretHeight + 4.dp).roundToPx() }
    return remember(pad, spacing) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val width = popupContentSize.width - pad * 2
                val height = popupContentSize.height - pad * 2
                var x = anchorBounds.center.x - width / 2
                if (x < 0) {
                    x = anchorBounds.left
                } else if (x + width > windowSize.width) {
                    x = anchorBounds.right - width
                }
                var y = anchorBounds.top - height - spacing
                if (y < 0) y = anchorBounds.bottom + spacing
                return IntOffset(x - pad, y - pad)
            }
        }
    }
}
