/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Unit Tests - Utilities
 * File: PixelSearchScrimTest.kt
 * Description: Unit tests for the Pixel Search background tint rules.
 */

package com.sameerasw.essentials.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PixelSearchScrimTest {
    @Test
    fun automatic_withoutBlur_isMostOpaque() {
        assertEquals(0.85f, PixelSearchScrim.alpha(true, 0.3f, blurEffective = false, wallpaperBackground = true))
    }

    @Test
    fun automatic_withBlurAndNoWallpaper_isLighter() {
        assertEquals(0.45f, PixelSearchScrim.alpha(true, 0.3f, blurEffective = true, wallpaperBackground = false))
    }

    @Test
    fun automatic_withBlurAndWallpaper_isDefault() {
        assertEquals(
            PixelSearchScrim.DEFAULT_ALPHA,
            PixelSearchScrim.alpha(true, 0.3f, blurEffective = true, wallpaperBackground = true),
        )
    }

    @Test
    fun manual_usesUserValue() {
        assertEquals(0.7f, PixelSearchScrim.alpha(false, 0.7f, blurEffective = false, wallpaperBackground = false))
    }

    @Test
    fun manual_isClampedToReadableBounds() {
        assertEquals(PixelSearchScrim.MIN_ALPHA, PixelSearchScrim.alpha(false, 0f, true, true))
        assertEquals(PixelSearchScrim.MAX_ALPHA, PixelSearchScrim.alpha(false, 5f, true, true))
    }
}
