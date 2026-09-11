/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: ProgressiveBlurModifier.kt
 * Description: UI layout element for ProgressiveBlurModifier.kt.
 */

package com.sameerasw.essentials.ui.modifiers

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.utils.DeviceUtils

enum class BlurDirection {
    TOP,
    BOTTOM,
}

/**
 * Applies native progressive blur to the specified edge of the element.
 */
fun Modifier.progressiveBlur(
    blurRadius: Float,
    height: Float,
    direction: BlurDirection = BlurDirection.TOP,
    showGradientOverlay: Boolean = true,
): Modifier =
    composed {
        val overlayColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f)
        val context = LocalContext.current
        val density = LocalDensity.current
        val isPowerSave = remember(context) { DeviceUtils.isPowerSaveMode(context) }

        val blurRadiusDp = with(density) { blurRadius.toDp() }

        val blurModifier =
            if (blurRadius > 0f && !isPowerSave) {
                Modifier.blur {
                    val sizeHeightPx = size.height.toPx()
                    if (sizeHeightPx > 0f && height > 0f) {
                        val fraction = (height / sizeHeightPx).coerceIn(0f, 1f)
                        radius =
                            when (direction) {
                                BlurDirection.TOP ->
                                    BlurRadiusSpec.verticalGradient(
                                        listOf(
                                            BlurStop(0.0f, blurRadiusDp),
                                            BlurStop(fraction, 0.dp),
                                            BlurStop(1.0f, 0.dp),
                                        ),
                                    )
                                BlurDirection.BOTTOM ->
                                    BlurRadiusSpec.verticalGradient(
                                        listOf(
                                            BlurStop(0.0f, 0.dp),
                                            BlurStop(1.0f - fraction, 0.dp),
                                            BlurStop(1.0f, blurRadiusDp),
                                        ),
                                    )
                            }
                    } else {
                        radius =
                            when (direction) {
                                BlurDirection.TOP ->
                                    BlurRadiusSpec.verticalGradient(
                                        startRadius = blurRadiusDp,
                                        endRadius = 0.dp,
                                    )
                                BlurDirection.BOTTOM ->
                                    BlurRadiusSpec.verticalGradient(
                                        startRadius = 0.dp,
                                        endRadius = blurRadiusDp,
                                    )
                            }
                    }
                }
            } else {
                Modifier
            }

        val gradientModifier =
            if (showGradientOverlay) {
                Modifier.drawWithContent {
                    drawContent()
                    val brush =
                        when (direction) {
                            BlurDirection.TOP -> {
                                Brush.verticalGradient(
                                    colors = listOf(overlayColor, Color.Transparent),
                                    endY = height,
                                )
                            }
                            BlurDirection.BOTTOM -> {
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, overlayColor),
                                    startY = size.height - height,
                                )
                            }
                        }
                    drawRect(brush = brush)
                }
            } else {
                Modifier
            }

        this
            .then(blurModifier)
            .then(gradientModifier)
    }

fun Modifier.progressiveBlur(
    maxRadius: Dp = 24.dp,
    direction: BlurDirection = BlurDirection.TOP,
    showGradientOverlay: Boolean = false,
): Modifier =
    composed {
        val overlayColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f)
        val context = LocalContext.current
        val isPowerSave = remember(context) { DeviceUtils.isPowerSaveMode(context) }

        val blurModifier =
            if (maxRadius > 0.dp && !isPowerSave) {
                Modifier.blur {
                    radius =
                        when (direction) {
                            BlurDirection.TOP ->
                                BlurRadiusSpec.verticalGradient(
                                    startRadius = maxRadius,
                                    endRadius = 0.dp,
                                )
                            BlurDirection.BOTTOM ->
                                BlurRadiusSpec.verticalGradient(
                                    startRadius = 0.dp,
                                    endRadius = maxRadius,
                                )
                        }
                }
            } else {
                Modifier
            }

        val gradientModifier =
            if (showGradientOverlay) {
                Modifier.drawWithContent {
                    drawContent()
                    val brush =
                        when (direction) {
                            BlurDirection.TOP ->
                                Brush.verticalGradient(
                                    colors = listOf(overlayColor, Color.Transparent),
                                )
                            BlurDirection.BOTTOM ->
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, overlayColor),
                                )
                        }
                    drawRect(brush = brush)
                }
            } else {
                Modifier
            }

        this
            .then(blurModifier)
            .then(gradientModifier)
    }
