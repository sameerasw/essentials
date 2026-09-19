/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: DashConfig.kt
 * Description: Domain model and business logic entry for DashConfig.kt.
 */

package com.sameerasw.essentials.domain.model

data class DashConfig(
    val thickness: Float = DEFAULT_THICKNESS,
    val lengthPercent: Float = DEFAULT_LENGTH_PERCENT,
    val glow: Float = DEFAULT_GLOW,
    val glowLengthRatio: Float = DEFAULT_GLOW_LENGTH_RATIO,
) {
    val lengthFraction: Float
        get() = (lengthPercent / 100f).coerceIn(0.02f, 1f)

    val glowLength: Float
        get() = glowLengthRatio.coerceIn(1f, 4f)

    companion object {
        const val DEFAULT_THICKNESS = 8f
        const val DEFAULT_LENGTH_PERCENT = 32f
        const val DEFAULT_GLOW = 1f
        const val DEFAULT_GLOW_LENGTH_RATIO = 1.2f
    }
}
