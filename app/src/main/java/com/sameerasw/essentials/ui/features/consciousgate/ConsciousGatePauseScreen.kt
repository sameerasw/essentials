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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val FadeDurationMillis = 400
private const val HoldDurationMillis = 2000L

@Composable
fun ConsciousGatePauseScreen(
    title: String,
    message: String,
    targetAppLabel: String,
    targetAppPackage: String?,
    progress: () -> Float,
    onClose: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = FadeDurationMillis),
        label = "ConsciousGateFade",
    )

    val scope = rememberCoroutineScope()
    var isActionInvoked by remember { mutableStateOf(false) }
    var isHolding by remember { mutableStateOf(false) }

    val view = LocalView.current
    LaunchedEffect(isHolding) {
        while (isActive) {
            delay(1000L)
            if (!isHolding && !isActionInvoked) {
                HapticUtil.performLightHaptic(view)
            }
        }
    }

    fun fadeOutThen(action: () -> Unit) {
        if (isActionInvoked) return
        isActionInvoked = true
        scope.launch {
            visible = false
            delay(FadeDurationMillis.toLong())
            action()
        }
    }

    val fadeOutClose = { fadeOutThen(onClose) }
    val fadeOutContinue = { fadeOutThen(onContinue) }

    BackHandler(onBack = fadeOutClose)

    val appIconBitmap =
        remember(targetAppPackage) {
            targetAppPackage?.let { pkg ->
                try {
                    AppUtil.getShortcutIcon(context, pkg).asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
        }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha }
                .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .padding(top = 72.dp, bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = targetAppLabel,
                        modifier =
                            Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.rounded_pause_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Centered 2 rows of text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            ) {
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
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons at bottom
            HoldToContinueButton(
                waitTimeProgress = progress(),
                onContinue = fadeOutContinue,
                onHoldingChange = { isHolding = it },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            CloseButton(
                onClose = fadeOutClose,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HoldToContinueButton(
    waitTimeProgress: Float,
    onContinue: () -> Unit,
    onHoldingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val isWaitTimeFinished = waitTimeProgress >= 1f

    val holdProgress = remember { Animatable(0f) }
    val wiggleOffsetX = remember { Animatable(0f) }

    suspend fun triggerWiggle() {
        HapticUtil.performLightHaptic(view)
        wiggleOffsetX.snapTo(0f)
        wiggleOffsetX.animateTo(
            targetValue = 0f,
            animationSpec =
                keyframes {
                    durationMillis = 350
                    -14f at 50
                    14f at 100
                    -10f at 150
                    10f at 200
                    -5f at 250
                    5f at 300
                    0f at 350
                },
        )
    }

    Box(
        modifier =
            modifier
                .offset { IntOffset(wiggleOffsetX.value.roundToInt(), 0) }
                .height(58.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .pointerInput(isWaitTimeFinished) {
                    detectTapGestures(
                        onTap = {
                            scope.launch { triggerWiggle() }
                        },
                        onPress = {
                            if (!isWaitTimeFinished) {
                                scope.launch { triggerWiggle() }
                                return@detectTapGestures
                            }

                            val startTime = System.currentTimeMillis()
                            var completed = false

                            onHoldingChange(true)
                            HapticUtil.startRampingHoldHaptic(context, HoldDurationMillis)

                            val animJob =
                                scope.launch {
                                    holdProgress.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = HoldDurationMillis.toInt(), easing = androidx.compose.animation.core.LinearEasing),
                                    )
                                    if (holdProgress.value >= 1f) {
                                        completed = true
                                    }
                                }

                            tryAwaitRelease()
                            onHoldingChange(false)
                            animJob.cancel()
                            HapticUtil.stopHoldHaptic(context)

                            if (completed || holdProgress.value >= 0.99f) {
                                HapticUtil.performCustomHaptic(view, 1.0f)
                                onContinue()
                            } else {
                                val elapsed = System.currentTimeMillis() - startTime
                                scope.launch {
                                    holdProgress.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 180, easing = androidx.compose.animation.core.LinearEasing),
                                    )
                                }
                                if (elapsed < 200L) {
                                    scope.launch { triggerWiggle() }
                                } else {
                                    HapticUtil.performLightHaptic(view)
                                }
                            }
                        },
                    )
                },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Wait time progress filling in secondary accent color
        if (!isWaitTimeFinished) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(waitTimeProgress.coerceIn(0f, 1f))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
            )
        }

        // Hold-to-continue progress fill in secondary accent color
        if (isWaitTimeFinished && holdProgress.value > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(holdProgress.value.coerceIn(0f, 1f))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
            )
        }

        // Button label centered
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text =
                    if (isWaitTimeFinished) {
                        stringResource(R.string.conscious_gate_hold_to_continue)
                    } else {
                        stringResource(R.string.conscious_gate_please_wait)
                    },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color =
                    if (isWaitTimeFinished) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Button(
        onClick = {
            HapticUtil.performUIHaptic(view)
            onClose()
        },
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(24.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        Text(
            text = stringResource(R.string.conscious_gate_close_button),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}
