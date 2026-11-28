package com.sameerasw.essentials.ui.components

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Rect
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreenViewProvider

// Splash AVD: logo spans 53.9 of a 108 viewport, drawn into 1.5x the icon view (1 unit = view / 72).
// LayeredLogoIcon: same logo spans 135.74 of a 192 viewport, drawn at minDimension / 140 per unit.
private const val SPLASH_UNITS_PER_VIEW = 72f
private const val TOOLBAR_UNITS_PER_ICON = 140f
private const val WELCOME_UNITS_PER_ICON = 192f
private const val TOOLBAR_PER_SPLASH_UNIT = 135.74f / 53.9f

class SplashLogoHandoff {
    var targetBounds: Rect? = null
    var welcomeBounds: Rect? = null
    val welcomeLogoAlpha = mutableFloatStateOf(1f)
    val toolbarLogoAlpha = mutableFloatStateOf(1f)
    val toolbarLogoSlot = mutableFloatStateOf(1f)

    fun animate(
        provider: SplashScreenViewProvider,
        iconView: View,
        durationMs: Long,
        onEnd: () -> Unit,
    ): Boolean {
        val toWelcome = welcomeBounds?.isEmpty == false
        val initialTarget = (if (toWelcome) welcomeBounds else targetBounds) ?: return false
        val unitsPerIcon = if (toWelcome) WELCOME_UNITS_PER_ICON else TOOLBAR_UNITS_PER_ICON
        if (initialTarget.isEmpty || iconView.width == 0) return false
        val splashView = provider.view

        val iconLocation = IntArray(2)
        iconView.getLocationInWindow(iconLocation)
        val iconCenterX = iconLocation[0] + iconView.width / 2f
        val iconCenterY = iconLocation[1] + iconView.height / 2f
        val splashPxPerUnit = iconView.width / SPLASH_UNITS_PER_VIEW

        val fadingViews =
            if (splashView is ViewGroup) {
                (0 until splashView.childCount)
                    .map { splashView.getChildAt(it) }
                    .filterNot { it === iconView || (it is ViewGroup && it.containsDescendant(iconView)) }
            } else {
                emptyList()
            }
        val background = splashView.background?.mutate()

        if (toWelcome) {
            welcomeLogoAlpha.floatValue = 0f
        } else {
            toolbarLogoAlpha.floatValue = 0f
            toolbarLogoSlot.floatValue = 0f
        }

        val move = PathInterpolator(0.55f, 0f, 0.15f, 1f)
        val fade = PathInterpolator(0f, 0f, 0.2f, 1f)
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            addUpdateListener { animator ->
                val t = animator.animatedFraction
                val target = (if (toWelcome) welcomeBounds else targetBounds) ?: initialTarget
                val targetPxPerUnit = target.minDimension / unitsPerIcon * TOOLBAR_PER_SPLASH_UNIT
                val targetScale = targetPxPerUnit / splashPxPerUnit
                val m = move.getInterpolation(t)
                if (!toWelcome) toolbarLogoSlot.floatValue = m

                iconView.translationX = (target.center.x - iconCenterX) * m
                iconView.translationY = (target.center.y - iconCenterY) * m
                val scale = 1f + (targetScale - 1f) * m
                iconView.scaleX = scale
                iconView.scaleY = scale

                val backdrop = 1f - fade.getInterpolation((t / 0.45f).coerceAtMost(1f))
                background?.alpha = (backdrop * 255).toInt()
                fadingViews.forEach { it.alpha = backdrop }

                val handoff = ((t - 0.85f) / 0.15f).coerceIn(0f, 1f)
                iconView.alpha = 1f - handoff
                if (toWelcome) welcomeLogoAlpha.floatValue = handoff else toolbarLogoAlpha.floatValue = handoff
            }
            doOnEnd {
                welcomeLogoAlpha.floatValue = 1f
                toolbarLogoAlpha.floatValue = 1f
                toolbarLogoSlot.floatValue = 1f
                onEnd()
            }
            start()
        }
        return true
    }
}

private fun ViewGroup.containsDescendant(view: View): Boolean {
    var parent = view.parent
    while (parent != null) {
        if (parent === this) return true
        parent = parent.parent
    }
    return false
}
