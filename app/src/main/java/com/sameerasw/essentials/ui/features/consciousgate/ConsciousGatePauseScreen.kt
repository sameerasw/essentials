/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Conscious Gate
 * File: ConsciousGatePauseScreen.kt
 * Description: Shared full-screen Conscious Gate screen UI, used both by the real
 * ConsciousGateActivity and by the live preview shown in the settings screen.
 */

package com.sameerasw.essentials.ui.features.consciousgate

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.ConsciousGateCountdownStyle
import com.sameerasw.essentials.ui.features.consciousgate.components.ConsciousGateCountdown
import com.sameerasw.essentials.ui.features.consciousgate.components.ConsciousGateHeroAnimation
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val FadeDurationMillis = 400

@Composable
fun ConsciousGatePauseScreen(
    iconResId: Int,
    title: String,
    message: String,
    targetAppLabel: String,
    countdownStyle: ConsciousGateCountdownStyle,
    progress: () -> Float,
    isContinueEnabled: Boolean,
    onClose: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = FadeDurationMillis),
        label = "ConsciousGateFade",
    )
    val scope = rememberCoroutineScope()
    fun fadeOutThen(action: () -> Unit) {
        scope.launch {
            visible = false
            delay(FadeDurationMillis.toLong())
            action()
        }
    }
    val fadeOutClose = { fadeOutThen(onClose) }
    val fadeOutContinue = { fadeOutThen(onContinue) }

    BackHandler(onBack = fadeOutClose)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha }
                .background(MaterialTheme.colorScheme.background),
    ) {
        if (countdownStyle == ConsciousGateCountdownStyle.LINEAR_WAVY) {
            LinearStyleLayout(
                iconResId = iconResId,
                title = title,
                message = message,
                targetAppLabel = targetAppLabel,
                progress = progress,
                isContinueEnabled = isContinueEnabled,
                onClose = fadeOutClose,
                onContinue = fadeOutContinue,
            )
        } else {
            HeroStyleLayout(
                iconResId = iconResId,
                title = title,
                message = message,
                targetAppLabel = targetAppLabel,
                countdownStyle = countdownStyle,
                progress = progress,
                isContinueEnabled = isContinueEnabled,
                onClose = fadeOutClose,
                onContinue = fadeOutContinue,
            )
        }
    }
}

/** Unchanged, original layout: small icon badge up top, countdown next to the Continue button. */
@Composable
private fun LinearStyleLayout(
    iconResId: Int,
    title: String,
    message: String,
    targetAppLabel: String,
    progress: () -> Float,
    isContinueEnabled: Boolean,
    onClose: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 120.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        IconBadge(iconResId)

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(1f))

        ConsciousGateCountdown(
            style = ConsciousGateCountdownStyle.LINEAR_WAVY,
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ContinueButton(targetAppLabel, isContinueEnabled, onContinue)
        }

        Spacer(modifier = Modifier.height(16.dp))

        CloseButton(onClose)
    }
}

/**
 * Big centered animation (with the icon at its center) up top, title and message between the
 * animation and the buttons. Used for every countdown style except the linear wavy bar.
 */
@Composable
private fun HeroStyleLayout(
    iconResId: Int,
    title: String,
    message: String,
    targetAppLabel: String,
    countdownStyle: ConsciousGateCountdownStyle,
    progress: () -> Float,
    isContinueEnabled: Boolean,
    onClose: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 96.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        ConsciousGateHeroAnimation(
            style = countdownStyle,
            progress = progress,
            iconResId = iconResId,
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(1f))

        ContinueButton(targetAppLabel, isContinueEnabled, onContinue)

        Spacer(modifier = Modifier.height(16.dp))

        CloseButton(onClose)
    }
}

@Composable
private fun IconBadge(iconResId: Int) {
    Icon(
        painter = painterResource(id = iconResId),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.size(96.dp),
    )
}

@Composable
private fun ContinueButton(
    targetAppLabel: String,
    isContinueEnabled: Boolean,
    onContinue: () -> Unit,
) {
    val view = LocalView.current
    OutlinedButton(
        onClick = {
            HapticUtil.performUIHaptic(view)
            onContinue()
        },
        enabled = isContinueEnabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(),
    ) {
        Text(stringResource(R.string.conscious_gate_continue_on_app, targetAppLabel))
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    val view = LocalView.current
    Button(
        onClick = {
            HapticUtil.performUIHaptic(view)
            onClose()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.conscious_gate_close_button))
    }
}
