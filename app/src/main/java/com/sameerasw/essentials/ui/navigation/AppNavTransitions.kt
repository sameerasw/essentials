package com.sameerasw.essentials.ui.navigation

import androidx.compose.animation.ContentTransform
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
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

object AppNavTransitions {
    private val slideSpec = spring<IntOffset>(dampingRatio = 0.9f, stiffness = 500f)

    private enum class UnderlayMode { None, Pop, Predictive }

    @Volatile
    private var underlayMode = UnderlayMode.None

    fun push(): ContentTransform {
        underlayMode = UnderlayMode.None
        return (
            slideInHorizontally(slideSpec) { it / 3 } +
                fadeIn(tween(220, delayMillis = 40))
        ) togetherWith (
            scaleOut(tween(300, easing = FastOutSlowInEasing), targetScale = 0.92f) +
                fadeOut(tween(200))
        )
    }

    fun paneFade(): ContentTransform {
        underlayMode = UnderlayMode.None
        return ContentTransform(
            targetContentEnter = fadeIn(tween(180)),
            initialContentExit = fadeOut(tween(180)),
            sizeTransform = null,
        )
    }

    fun pop(): ContentTransform {
        underlayMode = UnderlayMode.Pop
        return ContentTransform(
            targetContentEnter =
                scaleIn(tween(300, easing = FastOutSlowInEasing), initialScale = UNDERLAY_SCALE) +
                    slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -(it * UNDERLAY_SHIFT).toInt() },
            initialContentExit =
                slideOutHorizontally(slideSpec) { it } +
                    scaleOut(tween(300), targetScale = 0.95f),
            targetContentZIndex = -1f,
            sizeTransform = null,
        )
    }

    const val BACK_THRESHOLD = 0.05f
    const val GESTURE_PHASE = 0.8f
    val thresholdProgress = BACK_THRESHOLD * GESTURE_PHASE

    private const val GESTURE_SLIDE = 0.12f
    private const val THRESHOLD_BOOST = 0.14f

    val elasticSlide =
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

    val gestureScale =
        Easing { t ->
            val x = (t / 0.8f).coerceAtMost(1f)
            1f - (1f - x) * (1f - x)
        }

    @Volatile
    private var releasedAt = -1f

    private fun afterRelease(t: Float, during: (Float) -> Float): Float {
        val p = releasedAt
        if (p < 0f || t <= p || p >= 1f) return during(t)
        val from = during(p)
        val u = ((t - p) / (1f - p)).coerceIn(0f, 1f)
        return from + (1f - from) * FastOutSlowInEasing.transform(u)
    }

    private val predictiveSlide = Easing { t -> afterRelease(t) { elasticSlide.transform(it * GESTURE_PHASE) } }

    private val predictiveScale = Easing { t -> afterRelease(t) { gestureScale.transform(it * GESTURE_PHASE) } }

    private val underlayScale = Easing { t -> afterRelease(t) { UNDERLAY_GESTURE_SHARE * gestureScale.transform(it * GESTURE_PHASE) } }

    const val UNDERLAY_SCALE = 0.88f
    const val UNDERLAY_SHIFT = 0.12f
    const val UNDERLAY_CORNER = 32f
    private const val UNDERLAY_GESTURE_SHARE = 0.35f

    fun commitFade(u: Float): Float {
        val c = ((u - FADE_START) / (FADE_END - FADE_START)).coerceIn(0f, 1f)
        return c * c * (3f - 2f * c)
    }

    private val predictiveFade =
        Easing { t ->
            val p = releasedAt
            if (p < 0f || t <= p || p >= 1f) 0f else commitFade((t - p) / (1f - p))
        }

    private const val FADE_START = 0.45f
    private const val FADE_END = 0.85f

    @Composable
    fun TrackPredictiveBackRelease() {
        val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return
        LaunchedEffect(dispatcher) {
            var lastProgress = 0f
            var inGesture = false
            dispatcher.transitionState.collect { state ->
                if (state is NavigationEventTransitionState.InProgress) {
                    if (!inGesture) releasedAt = -1f
                    inGesture = true
                    lastProgress = state.latestEvent.progress
                } else if (inGesture) {
                    inGesture = false
                    releasedAt = lastProgress
                }
            }
        }
    }

    fun predictivePop(
        @NavigationEvent.SwipeEdge swipeEdge: Int,
    ): ContentTransform {
        underlayMode = UnderlayMode.Predictive
        val direction = if (swipeEdge == NavigationEvent.EDGE_RIGHT) -1 else 1
        return ContentTransform(
            targetContentEnter =
                scaleIn(tween(400, easing = underlayScale), initialScale = UNDERLAY_SCALE) +
                    slideInHorizontally(tween(400, easing = underlayScale)) { -direction * (it * UNDERLAY_SHIFT).toInt() },
            initialContentExit =
                slideOutHorizontally(tween(400, easing = predictiveSlide)) { direction * it } +
                    scaleOut(tween(400, easing = predictiveScale), targetScale = 0.82f) +
                    fadeOut(tween(400, easing = predictiveFade)),
            targetContentZIndex = -1f,
            sizeTransform = null,
        )
    }

    fun <T : Any> underlayCornerDecorator(): NavEntryDecorator<T> =
        NavEntryDecorator { entry ->
            val scope = LocalNavAnimatedContentScope.current
            val corner by scope.transition.animateFloat(
                transitionSpec = {
                    when (underlayMode) {
                        UnderlayMode.Predictive -> tween(400, easing = underlayScale)
                        else -> tween(300, easing = FastOutSlowInEasing)
                    }
                },
                label = "underlayCorner",
            ) { state ->
                if (state == EnterExitState.PreEnter && underlayMode != UnderlayMode.None) UNDERLAY_CORNER else 0f
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (corner > 0f) {
                                shape = RoundedCornerShape(corner.dp)
                                clip = true
                            }
                        },
            ) {
                entry.Content()
            }
        }
}
