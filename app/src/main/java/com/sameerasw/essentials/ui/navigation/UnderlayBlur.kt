package com.sameerasw.essentials.ui.navigation

import android.os.Build
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope

private val gestureBlurEasing =
    Easing { t ->
        if (t < 0.8f) {
            0.5f * (t / 0.8f)
        } else {
            0.5f + 0.5f * ((t - 0.8f) / 0.2f)
        }
    }

private val cornerEasing =
    Easing { t ->
        val x = (t / 0.8f).coerceAtMost(1f)
        1f - (1f - x) * (1f - x)
    }

@Composable
fun UnderlayBlur(
    isTop: Boolean,
    content: @Composable () -> Unit,
) {
    val transition = LocalNavAnimatedContentScope.current.transition
    val blurDp by transition.animateFloat(
        transitionSpec = { tween(400, easing = gestureBlurEasing) },
        label = "underlay_blur",
    ) { state ->
        if (isTop || state == EnterExitState.Visible) 0f else 12f
    }
    val cornerDp by transition.animateFloat(
        transitionSpec = { tween(400, easing = cornerEasing) },
        label = "dismiss_corner",
    ) { state ->
        if (isTop && state == EnterExitState.PostExit) 32f else 0f
    }
    val dim by transition.animateFloat(
        transitionSpec = { tween(400, easing = gestureBlurEasing) },
        label = "underlay_dim",
    ) { state ->
        if (isTop || state == EnterExitState.Visible) 0f else 0.25f
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
