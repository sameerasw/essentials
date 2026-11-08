/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - System Quick Ball
 * File: QuickBallView.kt
 * Description: Floating assistive touch ball and expanding radial menu built with Jetpack Compose.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QuickBallFloatingBall(
    sizeDp: Float,
    idleAlpha: Float,
    isIdle: Boolean,
    isAutoHide: Boolean,
    isDockedLeft: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val currentAlpha by animateFloatAsState(
        targetValue = if (isIdle) idleAlpha else 1.0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "ballAlpha",
    )

    val shape =
        if (isIdle && isAutoHide) {
            if (isDockedLeft) {
                RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            } else {
                RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
            }
        } else {
            CircleShape
        }

    val widthDp = if (isIdle && isAutoHide) (sizeDp * 0.45f).dp else sizeDp.dp
    val heightDp = sizeDp.dp

    Box(
        modifier =
            modifier
                .size(width = widthDp, height = heightDp)
                .alpha(currentAlpha)
                .shadow(elevation = 6.dp, shape = shape)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = shape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (!isIdle || !isAutoHide) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_touch_app_24),
                contentDescription = stringResource(id = R.string.feat_quick_ball_title),
                modifier = Modifier.size((sizeDp * 0.5f).dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            // Sleek docked edge accent line
            Box(
                modifier =
                    Modifier
                        .size(width = 4.dp, height = (sizeDp * 0.4f).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
            )
        }
    }
}

@Composable
fun QuickBallExpandedOverlay(
    ballCenterX: Float,
    ballCenterY: Float,
    isDockedLeft: Boolean,
    actions: List<Action>,
    onActionClick: (Action) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { 108.dp.toPx() }

    // Arc angles based on screen border docking
    // Left edge curves to the right (-60° to +60°)
    // Right edge curves to the left (120° to 240°)
    val angles =
        if (isDockedLeft) {
            listOf(-60.0, -30.0, 0.0, 30.0, 60.0)
        } else {
            listOf(240.0, 210.0, 180.0, 150.0, 120.0)
        }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismissRequest() })
                },
    ) {
        // Center dismiss button located directly over the ball origin
        val centerBtnSize = 48.dp
        Box(
            modifier =
                Modifier
                    .offset {
                        IntOffset(
                            x = (ballCenterX - with(density) { (centerBtnSize / 2).toPx() }).roundToInt(),
                            y = (ballCenterY - with(density) { (centerBtnSize / 2).toPx() }).roundToInt(),
                        )
                    }.size(centerBtnSize)
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        shape = CircleShape,
                    ).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_close_24),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Radial action buttons
        actions.take(5).forEachIndexed { index, action ->
            val angleDeg = angles.getOrElse(index) { 0.0 }
            val angleRad = Math.toRadians(angleDeg)
            val targetOffsetX = (radiusPx * cos(angleRad)).toFloat()
            val targetOffsetY = (radiusPx * sin(angleRad)).toFloat()

            val animProgress by animateFloatAsState(
                targetValue = 1f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                label = "radialItem_$index",
            )

            val currentX = ballCenterX + (targetOffsetX * animProgress)
            val currentY = ballCenterY + (targetOffsetY * animProgress)

            val itemSize = 48.dp
            val halfItemPx = with(density) { (itemSize / 2).toPx() }

            Box(
                modifier =
                    Modifier
                        .offset {
                            IntOffset(
                                x = (currentX - halfItemPx).roundToInt(),
                                y = (currentY - halfItemPx).roundToInt(),
                            )
                        }.scale(animProgress)
                        .alpha(animProgress.coerceIn(0f, 1f))
                        .size(itemSize)
                        .shadow(elevation = 8.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = CircleShape,
                        ).clickable {
                            onActionClick(action)
                        },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = action.icon),
                    contentDescription = stringResource(id = action.title),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
