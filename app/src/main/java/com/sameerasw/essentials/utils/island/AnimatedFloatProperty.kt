/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays - Island
 * File: AnimatedFloatProperty.kt
 * Description: Reusable single-value float animator, replacing the hand-rolled
 *              cancel/ValueAnimator.ofFloat/addListener blocks in IslandOverlayView.
 */

package com.sameerasw.essentials.utils.island

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator

class AnimatedFloatProperty {
    private var animator: ValueAnimator? = null

    fun animateTo(
        from: Float,
        to: Float,
        spec: IslandTransitionSpec,
        onUpdate: (Float) -> Unit,
        onEnd: (() -> Unit)? = null,
    ) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(from, to).apply {
            duration = spec.durationMs
            interpolator = spec.interpolator
            addUpdateListener { onUpdate(it.animatedValue as Float) }
            if (onEnd != null) {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onEnd()
                    }
                })
            }
            start()
        }
    }

    fun cancel() {
        animator?.cancel()
    }
}
