/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlay
 * File: RippleOverlay.kt
 * Description: Utility helper for RippleOverlay.kt.
 */

package com.sameerasw.essentials.utils.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import com.sameerasw.essentials.domain.model.RippleConfig

object RippleOverlay {
    private const val VIEW_TAG = "ripple_view"

    fun createOverlay(
        context: Context,
        color: Int,
        showBackground: Boolean,
        config: RippleConfig,
    ): FrameLayout {
        val overlay = FrameLayout(context)
        if (showBackground) {
            overlay.setBackgroundColor(Color.BLACK)
        }

        overlay.addView(
            RippleGlitterView(context, color, config).apply {
                tag = VIEW_TAG
                layoutParams =
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            },
        )

        return overlay
    }

    fun pulse(
        view: View,
        config: RippleConfig,
        onAnimationEnd: (() -> Unit)? = null,
    ) {
        view.alpha = 1f
        val rippleView =
            (view as? ViewGroup)?.findViewWithTag<View>(VIEW_TAG) as? RippleGlitterView
        if (rippleView == null) {
            onAnimationEnd?.invoke()
            return
        }

        val count = config.pulses
        val stride = (1f - config.overlapFraction).coerceIn(0.1f, 1f)
        val totalUnits = 1f + (count - 1) * stride

        rippleView.pulses = count
        rippleView.overlapFraction = config.overlapFraction
        rippleView.newSeed()
        rippleView.time = 0f

        ValueAnimator
            .ofFloat(0f, 1f)
            .apply {
                duration = (config.durationMillis * totalUnits).toLong().coerceAtLeast(1L)
                interpolator = LinearInterpolator()
                addUpdateListener { anim -> rippleView.time = anim.animatedValue as Float }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            rippleView.time = 0f
                            onAnimationEnd?.invoke()
                        }
                    },
                )
            }.start()
    }
}
