package com.sameerasw.essentials.island.ui

import android.content.Context
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.utils.HapticUtil

object IslandHaptics {
    fun tap(context: Context) = HapticUtil.performHapticForService(context, HapticFeedbackType.CLICK)
    fun dragStep(context: Context) = HapticUtil.performCustomHaptic(context, 0.20f)
    fun thresholdReached(context: Context) = HapticUtil.performCustomHaptic(context, 0.85f)
    fun thresholdLeft(context: Context) = HapticUtil.performCustomHaptic(context, 0.12f)
    fun longPressRamp(context: Context, step: Int) = HapticUtil.performCustomHaptic(context, RAMP[step])
    fun longPress(context: Context) = HapticUtil.performStrongTickHaptic(context)
    fun commit(context: Context) {
        HapticUtil.performRumbleHaptic(context)
        HapticUtil.performStrongTickHaptic(context)
    }
    fun wiggle(context: Context) = HapticUtil.performCustomHaptic(context, 0.15f)
    fun button(context: Context) = HapticUtil.performCustomHaptic(context, 0.5f)
    fun sliderStep(context: Context) = HapticUtil.performCustomHaptic(context, 0.2f)

    private val RAMP = floatArrayOf(0.18f, 0.42f, 0.70f)
    val RAMP_DELAYS_MS = longArrayOf(110L, 100L, 90L)
}
