/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: HysteresisSelector.kt
 * Description: Prevents edge-sliding jitter between keys.
 */

package com.sameerasw.essentials.ui.ime.touch

import kotlin.math.hypot

internal class HysteresisSelector(
    private val fraction: Float = KeyGeometry.HYSTERESIS,
) {
    var current: KeySpec? = null
        private set

    fun reset() {
        current = null
    }

    fun select(
        touchX: Float,
        touchY: Float,
        decoded: KeySpec?,
        letterWidth: Float,
    ): KeySpec? {
        val held = current
        if (held == null) {
            current = decoded
            return current
        }
        if (decoded == null || decoded.id == held.id) return held

        val hysteresisPx = fraction * letterWidth
        val hx = held.geometricCenterX
        val hy = held.geometricCenterY
        val nx = decoded.geometricCenterX
        val ny = decoded.geometricCenterY

        val keep = hypot(touchX - hx, touchY - hy) < hypot(touchX - nx, touchY - ny) + hysteresisPx
        if (!keep) {
            current = decoded
        }
        return current
    }
}
