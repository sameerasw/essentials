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
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.sameerasw.essentials.domain.model.RippleConfig

object RippleOverlay {
    private const val VIEW_TAG = "ripple_view"
    private const val EXPANSION_TENSION = 1.6f

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

        val maxPulses = config.pulses
        val duration = config.durationMillis
        var completed = 0

        fun startPulse() {
            if (completed >= maxPulses) {
                onAnimationEnd?.invoke()
                return
            }
            completed++

            rippleView.newSeed()
            rippleView.progress = 0f

            ValueAnimator
                .ofFloat(0f, 1f)
                .apply {
                    this.duration = duration
                    interpolator = DecelerateInterpolator(EXPANSION_TENSION)
                    addUpdateListener { anim -> rippleView.progress = anim.animatedValue as Float }
                    addListener(
                        object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                rippleView.progress = 0f
                                startPulse()
                            }
                        },
                    )
                }.start()
        }

        startPulse()
    }
}
