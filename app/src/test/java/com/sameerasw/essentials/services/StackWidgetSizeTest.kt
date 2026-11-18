/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Unit Tests - Services
 * File: StackWidgetSizeTest.kt
 * Description: Unit tests for stack widget size calculations and natural size config.
 */

package com.sameerasw.essentials.services

import com.sameerasw.essentials.domain.model.WidgetStackConfig
import com.sameerasw.essentials.services.widgets.StackWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StackWidgetSizeTest {
    @Test
    fun innerSizesCalculatesPortraitAndLandscapeFromLauncherOptions() {
        val (portrait, landscape) =
            StackWidgetProvider.innerSizes(
                minWidth = 100,
                maxWidth = 200,
                minHeight = 80,
                maxHeight = 160,
                reserved = 32,
            )
        assertEquals(100 to 128, portrait)
        assertEquals(200 to 48, landscape)
    }

    @Test
    fun innerSizesWithZeroReservedLeavesHeightsUnchanged() {
        val (portrait, landscape) =
            StackWidgetProvider.innerSizes(
                minWidth = 120,
                maxWidth = 250,
                minHeight = 90,
                maxHeight = 180,
                reserved = 0,
            )
        assertEquals(120 to 180, portrait)
        assertEquals(250 to 90, landscape)
    }

    @Test
    fun innerSizesCoercesNegativeHeightToZero() {
        val (portrait, landscape) =
            StackWidgetProvider.innerSizes(
                minWidth = 100,
                maxWidth = 200,
                minHeight = 20,
                maxHeight = 30,
                reserved = 32,
            )
        assertEquals(100 to 0, portrait)
        assertEquals(200 to 0, landscape)
    }

    @Test
    fun naturalSizeConvertsProviderDefaultPixelsToDp() {
        val result = StackWidgetProvider.naturalSize(defaultPx = 300, density = 2.0f, stackDp = 200)
        assertEquals(150, result)
    }

    @Test
    fun naturalSizeClampsLargerDefaultToStackSize() {
        val result = StackWidgetProvider.naturalSize(defaultPx = 500, density = 2.0f, stackDp = 200)
        assertEquals(200, result)
    }

    @Test
    fun naturalSizePairConvertsBothDimensionsAndClamps() {
        val result =
            StackWidgetProvider.naturalSize(
                defaultWidthPx = 400,
                defaultHeightPx = 600,
                density = 2.0f,
                stackWidthDp = 300,
                stackHeightDp = 200,
            )
        assertEquals(200 to 200, result)
    }

    @Test
    fun naturalSizeHandlesZeroDimensionsSafely() {
        assertEquals(0, StackWidgetProvider.naturalSize(defaultPx = 0, density = 2.0f, stackDp = 100))
        assertEquals(0, StackWidgetProvider.naturalSize(defaultPx = 100, density = 2.0f, stackDp = 0))
    }

    @Test
    fun naturalSizeDefaultsToNullAndFillForAll() {
        val config = WidgetStackConfig(stackWidgetId = 1, hostedWidgetIds = listOf(10, 20))
        assertFalse(config.isNaturalSize(10))
        assertFalse(config.isNaturalSize(20))
    }

    @Test
    fun withNaturalSizeEnablesNaturalSizeForOneWidget() {
        val config = WidgetStackConfig(stackWidgetId = 1, hostedWidgetIds = listOf(10, 20))
        val updated = config.withNaturalSize(10, true)
        assertTrue(updated.isNaturalSize(10))
        assertFalse(updated.isNaturalSize(20))
    }

    @Test
    fun withNaturalSizeDoesNotDuplicateId() {
        val config = WidgetStackConfig(stackWidgetId = 1, hostedWidgetIds = listOf(10, 20))
        val updated = config.withNaturalSize(10, true).withNaturalSize(10, true)
        assertTrue(updated.isNaturalSize(10))
        val removed = updated.withNaturalSize(10, false)
        assertFalse(removed.isNaturalSize(10))
    }

    @Test
    fun withNaturalSizeDisablesNaturalSize() {
        val config = WidgetStackConfig(stackWidgetId = 1, hostedWidgetIds = listOf(10, 20))
        val enabled = config.withNaturalSize(10, true)
        assertTrue(enabled.isNaturalSize(10))
        val disabled = enabled.withNaturalSize(10, false)
        assertFalse(disabled.isNaturalSize(10))
    }

    @Test
    fun withNaturalSizeFalseOnNullConfigLeavesItFill() {
        val config = WidgetStackConfig(stackWidgetId = 1, hostedWidgetIds = listOf(10, 20))
        val updated = config.withNaturalSize(10, false)
        assertFalse(updated.isNaturalSize(10))
    }
}
