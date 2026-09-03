/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: KeyboardTouchState.kt
 * Description: State tracking and coordinator for keyboard panel gestures.
 */

package com.sameerasw.essentials.ui.ime.touch

internal enum class PointerMode {
    IDLE,
    PRESSED,
    LONG_PRESS,
    SPACE_DRAG,
    DELETE_SWIPE,
    CANCELLED,
}

internal class KeyboardTouchState(
    private val decoder: TouchDecoder = SpatialTouchDecoder(),
    private val hysteresis: HysteresisSelector = HysteresisSelector(),
) {
    var mode: PointerMode = PointerMode.IDLE
        private set

    var pressedKey: KeySpec? = null
        private set

    var downX: Float = 0f
        private set
    var downY: Float = 0f
        private set
    var lastX: Float = 0f
        private set
    var lastY: Float = 0f
        private set

    fun onDown(x: Float, y: Float, layout: KeyboardLayout): KeySpec? {
        downX = x
        downY = y
        lastX = x
        lastY = y
        hysteresis.reset()

        val decoded = decoder.decode(x, y, layout)
        pressedKey = hysteresis.select(x, y, decoded.selected, layout.letterWidth)
        mode = PointerMode.PRESSED
        return pressedKey
    }

    fun onMove(x: Float, y: Float, layout: KeyboardLayout): KeySpec? {
        if (mode == PointerMode.IDLE || mode == PointerMode.CANCELLED) return null
        lastX = x
        lastY = y

        if (mode == PointerMode.PRESSED) {
            val decoded = decoder.decode(x, y, layout)
            val next = hysteresis.select(x, y, decoded.selected, layout.letterWidth)
            if (next?.id != pressedKey?.id) {
                pressedKey = next
            }
        }
        return pressedKey
    }

    fun setMode(newMode: PointerMode) {
        mode = newMode
    }

    fun onUp(): KeySpec? {
        val key = pressedKey
        reset()
        return key
    }

    fun reset() {
        mode = PointerMode.IDLE
        pressedKey = null
        hysteresis.reset()
    }
}
