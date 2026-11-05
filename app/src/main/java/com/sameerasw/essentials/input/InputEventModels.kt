/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Input
 * File: InputEventModels.kt
 * Description: Component file for InputEventModels.kt.
 */

package com.sameerasw.essentials.input

data class InputEvent(
    val timeSecond: Long,
    val timeMicro: Long,
    val type: Int,
    val code: Int,
    val value: Int,
)

data class InputDevice(
    val path: String,
    val name: String,
    val bus: Int,
    val vendor: Int,
    val product: Int,
)

sealed class VolumePressEvent {
    data class ShortPress(
        val direction: VolumeDirection,
        val timestamp: Long,
    ) : VolumePressEvent()

    data class LongPress(
        val direction: VolumeDirection,
        val durationMs: Long,
        val timestamp: Long,
    ) : VolumePressEvent()
}

enum class VolumeDirection { UP, DOWN }

enum class PowerGesture { DOUBLE_PRESS, HOLD }

data class PowerPressEvent(
    val gesture: PowerGesture,
    val screenOnBefore: Boolean,
    val firstPressAt: Long = 0L,
)
