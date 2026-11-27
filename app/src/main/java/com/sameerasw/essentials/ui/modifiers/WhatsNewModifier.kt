/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Modifiers
 * File: WhatsNewModifier.kt
 * Description: Accent edge glow marking new features and settings.
 */

package com.sameerasw.essentials.ui.modifiers

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.sameerasw.essentials.ui.state.WhatsNewState

val LocalWhatsNewScope = staticCompositionLocalOf<String?> { null }

fun Modifier.whatsNewGlow(active: Boolean): Modifier =
    if (!active) {
        this
    } else {
        composed {
            val color = MaterialTheme.colorScheme.primary
            graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush =
                            Brush.horizontalGradient(
                                0f to color.copy(alpha = 0.2f),
                                1f to color.copy(alpha = 0f),
                                startX = 0f,
                                endX = size.width * 0.4f,
                            ),
                        blendMode = BlendMode.SrcAtop,
                    )
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
