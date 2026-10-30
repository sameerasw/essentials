package com.sameerasw.essentials.ui.navigation

import android.os.Build
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope

internal val gestureBlurEasing =
    Easing { t ->
        if (t < 0.8f) {
            0.5f * (t / 0.8f)
        } else {
            0.5f + 0.5f * ((t - 0.8f) / 0.2f)
        }
    }

internal val cornerEasing =
    Easing { t ->
        val x = (t / 0.8f).coerceAtMost(1f)
        1f - (1f - x) * (1f - x)
    }

@Composable
fun UnderlayBlur(
    route: AppRoute,
    backStack: List<AppRoute>,
    lastNavWasPop: Boolean,
    content: @Composable () -> Unit,
) {
    val transition = LocalNavAnimatedContentScope.current.transition
    val isTop = backStack.lastOrNull() == route
    val isDismissing = isTop || route !in backStack
    val isRevealed = !isTop || lastNavWasPop
    val underlayFactor: (EnterExitState) -> Float = { state ->
        when (state) {
            EnterExitState.Visible -> 0f
            EnterExitState.PreEnter -> if (isRevealed) 1f else 0f
            EnterExitState.PostExit -> if (isDismissing) 0f else 1f
        }
    }
    val blurDp by transition.animateFloat(
        transitionSpec = { tween(400, easing = gestureBlurEasing) },
        label = "underlay_blur",
    ) { state ->
        12f * underlayFactor(state)
    }
    val cornerDp by transition.animateFloat(
        transitionSpec = { tween(400, easing = cornerEasing) },
        label = "dismiss_corner",
    ) { state ->
        if (isDismissing && state == EnterExitState.PostExit) 32f else 0f
    }
    val dismissProgress by transition.animateFloat(
        transitionSpec = { tween(400, easing = LinearEasing) },
        label = "dismiss_progress",
    ) { state ->
        if (isDismissing && state == EnterExitState.PostExit) 1f else 0f
    }
    val barHost = LocalBottomBarHost.current
    if (barHost != null) {
        LaunchedEffect(barHost, isDismissing) {
            if (isDismissing) {
                snapshotFlow { dismissProgress }.collect { barHost.dismissProgress = it }
            }
        }
        DisposableEffect(barHost) {
            onDispose { barHost.dismissProgress = 0f }
        }
    }
    val dim by transition.animateFloat(
        transitionSpec = { tween(400, easing = gestureBlurEasing) },
        label = "underlay_dim",
    ) { state ->
        0.25f * underlayFactor(state)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (cornerDp > 0.5f) {
                        shape = RoundedCornerShape(cornerDp.dp)
                        clip = true
                    } else {
                        clip = false
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurDp > 0.5f) {
                        val radius = blurDp.dp.toPx()
                        renderEffect = BlurEffect(radius, radius, TileMode.Decal)
                    } else {
                        renderEffect = null
                    }
                }.drawWithContent {
                    drawContent()
                    if (dim > 0.005f) drawRect(Color.Black.copy(alpha = dim))
                },
    ) {
        content()
    }
}
