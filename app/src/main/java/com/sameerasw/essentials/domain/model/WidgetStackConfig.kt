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
    // Stored by name; Gson leaves it null for stacks saved before it existed.
    private val controlsMode: String? = ControlsMode.ALWAYS.name,
) {
    /** How the arrows and dots below the stack are shown. */
    val controls: ControlsMode
        get() = ControlsMode.entries.firstOrNull { it.name == controlsMode } ?: ControlsMode.ALWAYS

    fun withControls(mode: ControlsMode) = copy(controlsMode = mode.name)

    enum class ControlsMode {
        ALWAYS,

        /** Hidden until the strip below the stack is tapped, then hidden again after a few seconds. */
        AUTO_HIDE,
        HIDDEN,
    }

    companion object {
        const val DEFAULT_INTERVAL_SECONDS = 10
        const val MAX_WIDGETS = 5
        val INTERVAL_OPTIONS = listOf(0, 5, 10, 30, 60)
    }
}
