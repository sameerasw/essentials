/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Unit Tests - Services
 * File: StackPositionsTest.kt
 * Description: Unit tests for StackPositions restore logic.
 */

package com.sameerasw.essentials.services

import com.sameerasw.essentials.domain.model.WidgetStackConfig
import com.sameerasw.essentials.services.widgets.StackPositions
import org.junit.Assert.assertEquals
import org.junit.Test

class StackPositionsTest {
    @Test
    fun keepsSavedPositionInRange() {
        val stacks = listOf(WidgetStackConfig(stackWidgetId = 1, hostedWidgetIds = listOf(10, 11, 12)))
        val saved = mapOf(1 to 1)
        assertEquals(mapOf(1 to 1), StackPositions.restore(saved, stacks))
    }

    @Test
    fun clampsSavedPositionAboveLastIndexToLastIndex() {
        val stacks = listOf(WidgetStackConfig(stackWidgetId = 1, hostedWidgetIds = listOf(10, 11, 12)))
        val saved = mapOf(1 to 5)
        assertEquals(mapOf(1 to 2), StackPositions.restore(saved, stacks))
    }

    @Test
    fun clampsNegativeSavedPositionToZero() {
        val stacks = listOf(WidgetStackConfig(stackWidgetId = 1, hostedWidgetIds = listOf(10, 11, 12)))
        val saved = mapOf(1 to -3)
        assertEquals(mapOf(1 to 0), StackPositions.restore(saved, stacks))
    }

    @Test
    fun dropsPositionsForStackIdsNotInStacks() {
        val stacks = listOf(WidgetStackConfig(stackWidgetId = 1, hostedWidgetIds = listOf(10, 11)))
        val saved = mapOf(1 to 1, 2 to 0, 99 to 3)
        assertEquals(mapOf(1 to 1), StackPositions.restore(saved, stacks))
    }

    @Test
    fun leavesOutStacksWithNoHostedWidgets() {
        val stacks =
            listOf(
                WidgetStackConfig(stackWidgetId = 1, hostedWidgetIds = emptyList()),
                WidgetStackConfig(stackWidgetId = 2, hostedWidgetIds = listOf(20)),
            )
        val saved = mapOf(1 to 0, 2 to 0)
        assertEquals(mapOf(2 to 0), StackPositions.restore(saved, stacks))
    }

    @Test
    fun leavesOutStacksWithNoSavedPosition() {
        val stacks =
            listOf(
                WidgetStackConfig(stackWidgetId = 1, hostedWidgetIds = listOf(10, 11)),
                WidgetStackConfig(stackWidgetId = 2, hostedWidgetIds = listOf(20, 21)),
            )
        val saved = mapOf(1 to 1)
        assertEquals(mapOf(1 to 1), StackPositions.restore(saved, stacks))
    }
}
