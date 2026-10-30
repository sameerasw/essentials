package com.sameerasw.essentials.island.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

fun <T> motionTween(durationMillis: Int = 300, delayMillis: Int = 0, easing: androidx.compose.animation.core.Easing = androidx.compose.animation.core.FastOutSlowInEasing): androidx.compose.animation.core.TweenSpec<T> =
    tween(IslandMotion.scaled(durationMillis), IslandMotion.scaled(delayMillis), easing)

fun <T> motionSpring(dampingRatio: Float = Spring.DampingRatioNoBouncy, stiffness: Float = Spring.StiffnessMedium, visibilityThreshold: T? = null): androidx.compose.animation.core.SpringSpec<T> =
    spring(dampingRatio, stiffness * IslandMotion.speed * IslandMotion.speed, visibilityThreshold)

object IslandMotion {
    @Volatile
    var speed = 1f

    @Volatile
    var expressive = false

    private const val X_SPATIAL_DAMPING = 0.7f
    private const val X_SPATIAL_FAST = 420f
    private const val X_SPATIAL_DEFAULT = 200f
    private const val X_SPATIAL_SLOW = 120f
    private const val X_EFFECTS_FAST = 2000f
    private const val X_EFFECTS_DEFAULT = 900f
    private const val X_EFFECTS_SLOW = 450f
    private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun scaled(ms: Int) = (ms / speed).toInt()

    const val DAMPING = 0.86f
    const val STIFFNESS = 260f

    private const val EXPAND_MS_BASE = 460
    val EXPAND_MS get() = scaled(EXPAND_MS_BASE)
    private val ExpandEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val size: FiniteAnimationSpec<IntSize> get() =
        if (expressive) motionSpring(X_SPATIAL_DAMPING, X_SPATIAL_DEFAULT, IntSize(1, 1)) else motionTween(EXPAND_MS_BASE, easing = ExpandEasing)
    const val COMPACT_DAMPING = 0.78f
    const val COMPACT_STIFFNESS = 230f
    private const val COMPACT_MS_BASE = 380
    val COMPACT_MS get() = scaled(COMPACT_MS_BASE)
    val compactSize: FiniteAnimationSpec<IntSize> get() =
        if (expressive) motionSpring(X_SPATIAL_DAMPING, X_SPATIAL_FAST, IntSize(1, 1)) else motionTween(COMPACT_MS_BASE, easing = ExpandEasing)
    val compactOffset: FiniteAnimationSpec<IntOffset> get() =
        if (expressive) motionSpring(X_SPATIAL_DAMPING, X_SPATIAL_FAST, IntOffset(1, 1)) else motionTween(COMPACT_MS_BASE, easing = ExpandEasing)
    fun <T> compactFloat(): FiniteAnimationSpec<T> =
        if (expressive) motionSpring(X_SPATIAL_DAMPING, X_SPATIAL_FAST) else motionSpring(COMPACT_DAMPING, COMPACT_STIFFNESS)
    private const val COLLAPSE_MS_BASE = 480
    val COLLAPSE_MS get() = scaled(COLLAPSE_MS_BASE)
    private val CollapseEasing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)
    val collapseSize: FiniteAnimationSpec<IntSize> get() =
        if (expressive) motionSpring(X_SPATIAL_DAMPING, X_SPATIAL_DEFAULT, IntSize(1, 1)) else motionSpring(0.72f, 320f, IntSize(1, 1))
    val offset: FiniteAnimationSpec<IntOffset> get() = motionSpring(Spring.DampingRatioNoBouncy, 300f, IntOffset(1, 1))
    fun <T> float(): FiniteAnimationSpec<T> =
        if (expressive) motionSpring(X_SPATIAL_DAMPING, X_SPATIAL_DEFAULT) else motionSpring(DAMPING, STIFFNESS)
    fun <T> collapseFloat(): FiniteAnimationSpec<T> =
        if (expressive) motionSpring(1f, X_SPATIAL_DEFAULT) else motionTween(COLLAPSE_MS_BASE, easing = CollapseEasing)
    fun collapseProgress(): FiniteAnimationSpec<Float> =
        if (expressive) motionSpring(1f, X_SPATIAL_DEFAULT) else motionTween(COLLAPSE_MS_BASE, easing = CollapseEasing)

    private const val HIDE_BUBBLE_MS_BASE = 320
    val HIDE_BUBBLE_MS get() = scaled(HIDE_BUBBLE_MS_BASE)
    fun hideBubble(): FiniteAnimationSpec<Float> =
        if (expressive) motionSpring(1f, X_EFFECTS_DEFAULT) else motionTween(HIDE_BUBBLE_MS_BASE, easing = ExpandEasing)

    private const val SQUASH_MS_BASE = 560
    val SQUASH_MS get() = scaled(SQUASH_MS_BASE)
    private val SquashEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    fun squash(): FiniteAnimationSpec<Float> =
        if (expressive) motionSpring(1f, X_EFFECTS_SLOW) else motionTween(SQUASH_MS_BASE, easing = SquashEasing)

    fun release(): FiniteAnimationSpec<Float> =
        motionSpring(if (expressive) X_SPATIAL_DAMPING else Spring.DampingRatioNoBouncy, 380f)

    fun fling(): FiniteAnimationSpec<Float> =
        if (expressive) motionSpring(X_SPATIAL_DAMPING, X_SPATIAL_FAST) else motionSpring(Spring.DampingRatioNoBouncy, 700f)
    fun <T> settle(): FiniteAnimationSpec<T> =
        if (expressive) motionSpring(X_SPATIAL_DAMPING, X_SPATIAL_SLOW) else motionSpring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)

    fun <T> contentIn(): FiniteAnimationSpec<T> =
        if (expressive) motionTween(320, 80, EmphasizedDecelerate) else motionTween(durationMillis = 280, delayMillis = 90)
    fun <T> contentOut(): FiniteAnimationSpec<T> =
        if (expressive) motionTween(180, easing = EmphasizedAccelerate) else motionTween(durationMillis = 150)

    const val CONTENT_SCALE = 0.06f
    fun contentSpring(): FiniteAnimationSpec<Float> =
        if (expressive) motionSpring(X_SPATIAL_DAMPING, X_SPATIAL_DEFAULT) else motionSpring(0.9f, 300f)

    fun <T> smoothSpatial(): FiniteAnimationSpec<T> =
        if (expressive) motionSpring(X_SPATIAL_DAMPING, X_SPATIAL_DEFAULT) else motionTween(EXPAND_MS_BASE, easing = ExpandEasing)
    fun <T> expressiveFastSpatial(): FiniteAnimationSpec<T> = motionSpring(0.7f, 650f)
    fun <T> expressiveDefaultSpatial(): FiniteAnimationSpec<T> = motionSpring(0.82f, 300f)
    fun <T> expressiveSlowSpatial(): FiniteAnimationSpec<T> = motionSpring(0.82f, 160f)
    fun <T> expressiveFastEffects(): FiniteAnimationSpec<T> = motionSpring(1f, 3000f)
    fun <T> expressiveDefaultEffects(): FiniteAnimationSpec<T> = motionSpring(1f, 1300f)
    fun <T> expressiveSlowEffects(): FiniteAnimationSpec<T> = motionSpring(1f, 650f)

    const val COLLAPSE_THRESHOLD = 0.3f
    const val DISMISS_THRESHOLD = 0.3f
}
