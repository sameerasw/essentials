package com.sameerasw.essentials.ui.navigation

import androidx.compose.animation.ContentTransform
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
            targetContentEnter =
                scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = 0.92f) +
                    fadeIn(tween(220)),
            initialContentExit =
                slideOutHorizontally(slideSpec) { it / 3 } +
                    fadeOut(tween(200)),
            targetContentZIndex = -1f,
        )

    fun predictivePop(
        @NavigationEvent.SwipeEdge swipeEdge: Int,
    ): ContentTransform {
        val direction = if (swipeEdge == NavigationEvent.EDGE_RIGHT) -1 else 1
        return ContentTransform(
            targetContentEnter =
                scaleIn(tween(300), initialScale = 0.92f) +
                    fadeIn(tween(300)),
            initialContentExit =
                scaleOut(tween(300), targetScale = 0.85f) +
                    slideOutHorizontally(tween(300)) { direction * it / 4 } +
                    fadeOut(tween(300)),
            targetContentZIndex = -1f,
        )
    }
}
