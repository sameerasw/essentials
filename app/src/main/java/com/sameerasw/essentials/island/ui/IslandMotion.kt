package com.sameerasw.essentials.island.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

object IslandMotion {
    const val DAMPING = 0.78f
    const val STIFFNESS = 380f

    val size: FiniteAnimationSpec<IntSize> = spring(DAMPING, STIFFNESS, IntSize(1, 1))
    const val COLLAPSE_MS = 220
    private val CollapseEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val collapseSize: FiniteAnimationSpec<IntSize> = tween(COLLAPSE_MS, easing = CollapseEasing)
    val offset: FiniteAnimationSpec<IntOffset> = spring(DAMPING, STIFFNESS, IntOffset(1, 1))
    fun <T> float(): FiniteAnimationSpec<T> = spring(DAMPING, STIFFNESS)
    fun <T> collapseFloat(): FiniteAnimationSpec<T> = tween(COLLAPSE_MS, easing = CollapseEasing)
    // Finishes a finger-driven gesture
    fun fling(): FiniteAnimationSpec<Float> = spring(Spring.DampingRatioNoBouncy, 700f)
    fun <T> settle(): FiniteAnimationSpec<T> = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)

    fun <T> contentIn(): FiniteAnimationSpec<T> = tween(durationMillis = 220, delayMillis = 70)
    fun <T> contentOut(): FiniteAnimationSpec<T> = tween(durationMillis = 110)

    const val CONTENT_SCALE = 0.06f
    fun contentSpring(): FiniteAnimationSpec<Float> = spring(0.85f, 420f)

    const val COLLAPSE_THRESHOLD = 0.3f
    const val DISMISS_THRESHOLD = 0.3f
}
