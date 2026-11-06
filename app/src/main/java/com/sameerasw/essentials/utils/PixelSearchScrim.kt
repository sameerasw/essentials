/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities
 * File: PixelSearchScrim.kt
 * Description: Decides how strong the tint behind Pixel Search results is.
 */

package com.sameerasw.essentials.utils

object PixelSearchScrim {
    const val MIN_ALPHA = 0.2f
    const val MAX_ALPHA = 0.95f
    const val DEFAULT_ALPHA = 0.55f

    /**
     * Automatic mode keeps results readable: opaque-ish when blur is unavailable, lighter when the
     * wallpaper is hidden, balanced otherwise. Manual mode uses the user's value within safe bounds.
     */
    fun alpha(
        automatic: Boolean,
        manualAlpha: Float,
        blurEffective: Boolean,
        wallpaperBackground: Boolean,
    ): Float =
        if (automatic) {
            when {
                !blurEffective -> 0.85f
                !wallpaperBackground -> 0.45f
                else -> DEFAULT_ALPHA
            }
        } else {
            manualAlpha.coerceIn(MIN_ALPHA, MAX_ALPHA)
        }
}
