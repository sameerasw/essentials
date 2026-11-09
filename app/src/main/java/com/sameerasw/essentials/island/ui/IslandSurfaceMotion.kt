package com.sameerasw.essentials.island.ui

import android.view.animation.AnimationUtils
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.google.android.material.R as MaterialR

internal class IslandSurfaceMotion(
    val emphasized: Boolean,
    private val easing: Easing,
    private val expandMs: Int,
    private val collapseMs: Int,
    private val mediumMs: Int,
    private val releaseMs: Int,
    private val shortMs: Int,
    private val fastMs: Int,
) {
    private fun <T> choose(ms: Int, original: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> =
        if (emphasized) motionTween(ms, easing = easing) else original

    fun <T> tween(durationMillis: Int = 300, delayMillis: Int = 0, easing: Easing? = null) =
        motionTween<T>(durationMillis, delayMillis, easing ?: if (emphasized) this.easing else FastOutSlowInEasing)

    val COLLAPSE_MS get() = if (emphasized) IslandMotion.scaled(collapseMs) else IslandMotion.COLLAPSE_MS
    val COMPACT_MS get() = if (emphasized) IslandMotion.scaled(collapseMs) else IslandMotion.COMPACT_MS
    val HIDE_BUBBLE_MS get() = if (emphasized) IslandMotion.scaled(collapseMs) else IslandMotion.HIDE_BUBBLE_MS
    val size get() = choose(expandMs, IslandMotion.size)
    val compactSize get() = choose(collapseMs, IslandMotion.compactSize)
    val compactOffset get() = choose(collapseMs, IslandMotion.compactOffset)
    fun <T> compactFloat() = choose(collapseMs, IslandMotion.compactFloat<T>())
    val collapseSize: FiniteAnimationSpec<IntSize> get() = choose(collapseMs, IslandMotion.collapseSize)
    fun <T> sizeFloat() = choose(expandMs, IslandMotion.sizeFloat<T>())
    fun <T> float() = choose(mediumMs, IslandMotion.float<T>())
    fun <T> collapseFloat() = choose(mediumMs, IslandMotion.collapseFloat<T>())
    fun collapseProgress() = choose(collapseMs, IslandMotion.collapseProgress())
    fun finishCollapse() = choose(collapseMs, IslandMotion.release())
    fun release() = choose(releaseMs, IslandMotion.release())
    fun fling() = choose(shortMs, IslandMotion.fling())
    fun <T> settle() = choose(releaseMs, IslandMotion.settle<T>())
    fun squash() = choose(collapseMs, IslandMotion.squash())
    fun hideBubble() = choose(mediumMs, IslandMotion.hideBubble())
    fun <T> showBubble() = choose(mediumMs, IslandMotion.showBubble<T>())
    fun <T> smoothSpatial() = choose(mediumMs, IslandMotion.smoothSpatial<T>())
    fun contentSpring() = choose(mediumMs, IslandMotion.contentSpring())
    fun <T> contentIn(): FiniteAnimationSpec<T> = if (emphasized) tween(shortMs, 40) else IslandMotion.contentIn()
    fun <T> contentOut(): FiniteAnimationSpec<T> = if (emphasized) tween(fastMs) else IslandMotion.contentOut()
    fun <T> expressiveFastSpatial() = choose(shortMs, IslandMotion.expressiveFastSpatial<T>())
    fun <T> expressiveDefaultSpatial() = choose(mediumMs, IslandMotion.expressiveDefaultSpatial<T>())
    fun <T> expressiveSlowSpatial() = choose(expandMs, IslandMotion.expressiveSlowSpatial<T>())
    fun <T> expressiveFastEffects() = choose(fastMs, IslandMotion.expressiveFastEffects<T>())
    fun <T> expressiveDefaultEffects() = choose(shortMs, IslandMotion.expressiveDefaultEffects<T>())
}

internal val LocalIslandSurfaceMotion = staticCompositionLocalOf<IslandSurfaceMotion?> { null }

@Composable
internal fun currentIslandSurfaceMotion(): IslandSurfaceMotion =
    LocalIslandSurfaceMotion.current ?: rememberIslandSurfaceMotion(false)

@Composable
internal fun rememberIslandSurfaceMotion(emphasized: Boolean): IslandSurfaceMotion {
    val context = LocalContext.current
    return remember(context, emphasized) {
        val interpolator = AnimationUtils.loadInterpolator(context, MaterialR.interpolator.m3_sys_motion_easing_emphasized)
        val resources = context.resources
        IslandSurfaceMotion(emphasized, Easing { interpolator.getInterpolation(it) },
            resources.getInteger(MaterialR.integer.m3_sys_motion_duration_long2),
            resources.getInteger(MaterialR.integer.m3_sys_motion_duration_medium4),
            resources.getInteger(MaterialR.integer.m3_sys_motion_duration_medium2),
            resources.getInteger(MaterialR.integer.m3_sys_motion_duration_medium1),
            resources.getInteger(MaterialR.integer.m3_sys_motion_duration_short4),
            resources.getInteger(MaterialR.integer.m3_sys_motion_duration_short3))
    }
}
