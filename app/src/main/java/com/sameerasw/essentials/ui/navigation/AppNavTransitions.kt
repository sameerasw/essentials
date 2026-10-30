package com.sameerasw.essentials.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.navigationevent.NavigationEvent

object AppNavTransitions {
    private val slideSpec = spring<IntOffset>(dampingRatio = 0.9f, stiffness = 500f)

    fun push(): ContentTransform =
        (
            slideInHorizontally(slideSpec) { it / 3 } +
                fadeIn(tween(220, delayMillis = 40))
        ) togetherWith (
            scaleOut(tween(300, easing = FastOutSlowInEasing), targetScale = 0.92f) +
                fadeOut(tween(200))
        )

    fun pop(): ContentTransform =
        ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit =
                slideOutHorizontally(slideSpec) { it } +
                    scaleOut(tween(300), targetScale = 0.95f),
            targetContentZIndex = -1f,
            sizeTransform = null,
        )

    const val BACK_THRESHOLD = 0.05f
    const val GESTURE_PHASE = 0.8f
    val thresholdProgress = BACK_THRESHOLD * GESTURE_PHASE

    private const val GESTURE_SLIDE = 0.12f
    private const val THRESHOLD_BOOST = 0.14f

    private val elasticSlide =
        Easing { t ->
            if (t < GESTURE_PHASE) {
                val x = t / GESTURE_PHASE
                val elastic = GESTURE_SLIDE * (1f - (1f - x) * (1f - x) * (1f - x))
                val b = ((x - BACK_THRESHOLD) / 0.05f).coerceIn(0f, 1f)
                elastic + THRESHOLD_BOOST * b * b * (3f - 2f * b)
            } else {
                val start = GESTURE_SLIDE + THRESHOLD_BOOST
                start + (1f - start) * ((t - GESTURE_PHASE) / (1f - GESTURE_PHASE))
            }
        }

    private val gestureScale =
        Easing { t ->
            val x = (t / 0.8f).coerceAtMost(1f)
            1f - (1f - x) * (1f - x)
        }

    fun predictivePop(
        @NavigationEvent.SwipeEdge swipeEdge: Int,
    ): ContentTransform {
        val direction = if (swipeEdge == NavigationEvent.EDGE_RIGHT) -1 else 1
        return ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit =
                slideOutHorizontally(tween(400, easing = elasticSlide)) { direction * it } +
                    scaleOut(tween(400, easing = gestureScale), targetScale = 0.82f),
            targetContentZIndex = -1f,
            sizeTransform = null,
        )
    }
}
