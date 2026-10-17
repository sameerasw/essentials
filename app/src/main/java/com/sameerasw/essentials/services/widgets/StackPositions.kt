/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: StackPositions.kt
 * Description: Restores and validates widget stack positions.
 */

package com.sameerasw.essentials.services.widgets

import com.sameerasw.essentials.domain.model.WidgetStackConfig

object StackPositions {
    fun restore(
        saved: Map<Int, Int>,
        stacks: List<WidgetStackConfig>,
    ): Map<Int, Int> =
        buildMap {
            for (stack in stacks) {
                if (stack.hostedWidgetIds.isEmpty()) continue
                val pos = saved[stack.stackWidgetId] ?: continue
                put(stack.stackWidgetId, pos.coerceIn(0, stack.hostedWidgetIds.lastIndex))
            }
        }
}
