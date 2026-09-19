/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: RippleConfig.kt
 * Description: Domain model and business logic entry for RippleConfig.kt.
 */

package com.sameerasw.essentials.domain.model

data class RippleConfig(
    val speed: Float = DEFAULT_SPEED,
    val repeatCount: Int = DEFAULT_REPEAT_COUNT,
    val waveSize: Float = DEFAULT_WAVE_SIZE,
    val opacity: Float = DEFAULT_OPACITY,
    val sparklesEnabled: Boolean = true,
    val sparkleCount: Int = DEFAULT_SPARKLE_COUNT,
    val sparkleSize: Float = DEFAULT_SPARKLE_SIZE,
    val position: NotificationLightingRipplePosition = NotificationLightingRipplePosition.AUTO,
    val positionX: Float = DEFAULT_POSITION_PERCENT,
    val positionY: Float = DEFAULT_POSITION_PERCENT,
    val overlapPercent: Float = DEFAULT_OVERLAP_PERCENT,
) {
    val durationMillis: Long
        get() = (BASE_DURATION_MS / speed.coerceAtLeast(MIN_SPEED)).toLong()

    val pulses: Int
        get() = repeatCount.coerceAtLeast(1)

    val overlapFraction: Float
        get() = (overlapPercent / 100f).coerceIn(0f, 0.9f)

    val effectiveSparkleCount: Int
        get() = if (sparklesEnabled) sparkleCount.coerceAtLeast(0) else 0

    companion object {
        const val BASE_DURATION_MS = 1800L
        const val MIN_SPEED = 0.1f

        const val DEFAULT_SPEED = 1f
        const val DEFAULT_REPEAT_COUNT = 1
        const val DEFAULT_WAVE_SIZE = 1f
        const val DEFAULT_OPACITY = 1f
        const val DEFAULT_SPARKLE_COUNT = 110
        const val DEFAULT_SPARKLE_SIZE = 1f
        const val DEFAULT_POSITION_PERCENT = 50f
        const val DEFAULT_OVERLAP_PERCENT = 0f
    }
}
