/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Conscious Gate
 * File: ConsciousGateCountdown.kt
 * Description: Material 3 Expressive countdown treatments shown above the Conscious Gate
 * "Continue" button while the user-configured pause delay elapses.
 */

package com.sameerasw.essentials.ui.features.consciousgate.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.domain.model.ConsciousGateCountdownStyle

/**
 * Renders a Material 3 Expressive visualization of [progress] (0f = "just shown", 1f = "ready to
 * continue") above [content] (the Continue button).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConsciousGateCountdown(
    style: ConsciousGateCountdownStyle,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            ConsciousGateCountdownIndicator(style = style, progress = progress)
        }

        content()
    }
}

/**
 * Just the visual indicator for [style] (no Continue button) — reused both inside
 * [ConsciousGateCountdown] and as a small live preview in the countdown-style picker.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConsciousGateCountdownIndicator(
    style: ConsciousGateCountdownStyle,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    when (style) {
        ConsciousGateCountdownStyle.CIRCULAR_WAVY ->
            CircularWavyProgressIndicator(
                progress = progress,
                modifier = modifier,
                color = indicatorColor,
                trackColor = trackColor,
            )

        ConsciousGateCountdownStyle.LINEAR_WAVY ->
            LinearWavyProgressIndicator(
                progress = progress,
                modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
                color = indicatorColor,
                trackColor = trackColor,
            )

        ConsciousGateCountdownStyle.LOADING_BLOB ->
            ContainedLoadingIndicator(
                progress = progress,
                modifier = modifier,
                containerColor = trackColor,
                indicatorColor = indicatorColor,
            )

        ConsciousGateCountdownStyle.BREATHING_DOT ->
            BreathingDot(modifier = modifier.size(56.dp), color = indicatorColor)
    }
}

/**
 * A large (220dp), prominent version of the countdown animation with [iconResId] centered on
 * top of it — used as the hero visual for every countdown style except the linear wavy bar,
 * which keeps its own compact layout next to the Continue button.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConsciousGateHeroAnimation(
    style: ConsciousGateCountdownStyle,
    progress: () -> Float,
    iconResId: Int,
    modifier: Modifier = Modifier,
) {
    val heroScale = HeroSize / IndicatorNativeSize

    Box(
        modifier = modifier.size(HeroSize),
        contentAlignment = Alignment.Center,
    ) {
        when (style) {
            ConsciousGateCountdownStyle.CIRCULAR_WAVY ->{
                val stroke = Stroke(width = with(LocalDensity.current) { (10f / heroScale).dp.toPx() }, cap = StrokeCap.Round)
                CircularWavyProgressIndicator(
                    progress = progress,
                    modifier = Modifier.scale(heroScale),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    stroke = stroke,
                    trackStroke = stroke,
                )
            }

            ConsciousGateCountdownStyle.LOADING_BLOB ->
                ContainedLoadingIndicator(
                    progress = progress,
                    modifier = Modifier.scale(heroScale),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                )

            ConsciousGateCountdownStyle.BREATHING_DOT ->
                BreathingDot(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                )

            ConsciousGateCountdownStyle.LINEAR_WAVY -> Unit
        }

        val iconTint =
            when (style) {
                // The center stays mostly on the plain page background, so match that surface.
                ConsciousGateCountdownStyle.CIRCULAR_WAVY,
                ConsciousGateCountdownStyle.LINEAR_WAVY,
                -> MaterialTheme.colorScheme.onBackground
                // The center sits on top of a primary-colored fill (blob/dot), so use its
                // contrasting "on" color instead.
                ConsciousGateCountdownStyle.LOADING_BLOB,
                ConsciousGateCountdownStyle.BREATHING_DOT,
                -> MaterialTheme.colorScheme.onPrimary
            }

        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(64.dp),
        )
    }
}

private val HeroSize = 220.dp
private val IndicatorNativeSize = 48.dp

@Composable
private fun BreathingDot(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ConsciousGateBreathingDot")
    val breath by
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "breathScale",
        )
    val alpha by
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.7f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "breathAlpha",
        )
    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = breath
                    scaleY = breath
                    this.alpha = alpha
                }.background(color, CircleShape),
    )
}
