/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Modifiers
 * File: WhatsNewModifier.kt
 * Description: Accent edge glow marking new features and settings.
 */

package com.sameerasw.essentials.ui.modifiers

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.sameerasw.essentials.ui.state.WhatsNewState

val LocalWhatsNewScope = staticCompositionLocalOf<String?> { null }

private const val FLOW_PERIOD_MS = 4200
private const val FLOW_ACTIVE_FRACTION = 0.65f
private const val STAGGER_PX = 2400f

fun Modifier.whatsNewGlow(active: Boolean): Modifier =
    if (!active) {
        this
    } else {
        composed {
            val color = MaterialTheme.colorScheme.primary
            val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
            val baseAlpha = if (isLight) 0.24f else 0.16f
            val waveAlpha = if (isLight) 0.32f else 0.24f
            val time by rememberInfiniteTransition(label = "whatsNewFlow").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(FLOW_PERIOD_MS, easing = LinearEasing)),
                label = "whatsNewFlowTime",
            )
            val offsetY = remember { floatArrayOf(0f) }
            onGloballyPositioned { offsetY[0] = it.positionInRoot().y }
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush =
                            Brush.horizontalGradient(
                                0f to color.copy(alpha = baseAlpha),
                                1f to color.copy(alpha = 0f),
                                startX = 0f,
                                endX = size.width * 0.35f,
                            ),
                        blendMode = BlendMode.SrcAtop,
                    )
                    val phase = ((time - offsetY[0] / STAGGER_PX) % 1f + 1f) % 1f
                    val wave = phase / FLOW_ACTIVE_FRACTION
                    if (wave < 1f) {
                        val eased = FastOutSlowInEasing.transform(wave)
                        val half = size.width * 0.22f
                        val center = -half + eased * (size.width * 0.75f)
                        val alpha = waveAlpha * (1f - wave)
                        drawRect(
                            brush =
                                Brush.horizontalGradient(
                                    0f to color.copy(alpha = 0f),
                                    0.5f to color.copy(alpha = alpha),
                                    1f to color.copy(alpha = 0f),
                                    startX = center - half,
                                    endX = center + half,
                                ),
                            blendMode = BlendMode.SrcAtop,
                        )
                    }
                }
        }
    }

fun Modifier.whatsNewSetting(
    key: String,
    featureId: String? = null,
): Modifier =
    composed {
        whatsNewGlow(WhatsNewState.isNewSetting(featureId ?: LocalWhatsNewScope.current, key))
    }

fun Modifier.whatsNewFeature(featureId: String): Modifier =
    composed {
        whatsNewGlow(WhatsNewState.isNewFeature(featureId))
    }
