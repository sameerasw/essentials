/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Models
 * File: WidgetStackConfig.kt
 * Description: Configuration of a single widget stack placed on the homescreen.
 */

package com.sameerasw.essentials.domain.model

import androidx.annotation.Keep

@Keep
data class WidgetStackConfig(
    val stackWidgetId: Int,
    val hostedWidgetIds: List<Int> = emptyList(),
    val intervalSeconds: Int = DEFAULT_INTERVAL_SECONDS,
    val showControls: Boolean = true,
) {
    companion object {
        const val DEFAULT_INTERVAL_SECONDS = 10
        const val MAX_WIDGETS = 5
        val INTERVAL_OPTIONS = listOf(0, 5, 10, 30, 60)
    }
}
