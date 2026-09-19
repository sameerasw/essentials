/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays - Island
 * File: IslandSwipeDirections.kt
 * Description: Shared swipe-direction classification for the island's touch handler.
 */

package com.sameerasw.essentials.utils.island

import kotlin.math.abs

data class IslandSwipeDirections(
    val isSwipeUp: Boolean,
    val isTowardCamera: Boolean,
    val isAwayFromCamera: Boolean,
) {
    companion object {
        fun classify(downX: Float, dx: Float, dy: Float, cameraCenterX: Float, touchSlopPx: Float): IslandSwipeDirections {
            val isSwipeUp = dy < -touchSlopPx * 1.5f && abs(dy) > abs(dx)
            val isTowardCamera = when {
                downX < cameraCenterX -> dx > touchSlopPx * 1.5f && abs(dx) > abs(dy)
                downX > cameraCenterX -> dx < -touchSlopPx * 1.5f && abs(dx) > abs(dy)
                else -> false
            }
            val isAwayFromCamera = when {
                downX < cameraCenterX -> dx < -touchSlopPx * 1.5f && abs(dx) > abs(dy)
                downX > cameraCenterX -> dx > touchSlopPx * 1.5f && abs(dx) > abs(dy)
                else -> abs(dx) > touchSlopPx * 2.0f && abs(dx) > abs(dy)
            }
            return IslandSwipeDirections(isSwipeUp, isTowardCamera, isAwayFromCamera)
        }
    }
}
