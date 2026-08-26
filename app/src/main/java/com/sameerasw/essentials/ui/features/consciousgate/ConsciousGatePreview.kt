/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Conscious Gate
 * File: ConsciousGatePreview.kt
 * Description: Full-screen, live-looping preview of the Conscious Gate pause screen, shown
 * as a dialog on top of the settings screen, reusing the real ConsciousGatePauseScreen
 * composable.
 */

package com.sameerasw.essentials.ui.features.consciousgate

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sameerasw.essentials.domain.model.ConsciousGateCountdownStyle
import com.sameerasw.essentials.ui.features.consciousgate.components.ConsciousGateIcons
import com.sameerasw.essentials.ui.theme.EssentialsTheme

@Composable
fun ConsciousGatePreview(
    iconName: String,
    title: String,
    message: String,
    targetAppLabel: String,
    countdownStyle: ConsciousGateCountdownStyle,
    delaySeconds: Int,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconResId = remember(iconName) { ConsciousGateIcons.resolve(iconName) }

    val loopSeconds = delaySeconds.coerceIn(1, 30)
    val infiniteTransition = rememberInfiniteTransition(label = "ConsciousGatePreviewProgress")
    val progress by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = loopSeconds * 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "previewProgress",
        )

    EssentialsTheme {
        ConsciousGatePauseScreen(
            iconResId = iconResId,
            title = title,
            message = message,
            targetAppLabel = targetAppLabel,
            countdownStyle = countdownStyle,
            progress = { progress },
            isContinueEnabled = progress >= 1f,
            onClose = onExit,
            onContinue = onExit,
            modifier = modifier.fillMaxSize(),
        )
    }
}
