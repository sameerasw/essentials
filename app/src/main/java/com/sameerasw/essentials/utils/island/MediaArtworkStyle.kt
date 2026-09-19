/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Overlays - Island
 * File: MediaArtworkStyle.kt
 * Description: Derives the blurred backdrop and palette accent color from media artwork.
 */

package com.sameerasw.essentials.utils.island

import android.graphics.Bitmap
import android.graphics.Color
import androidx.palette.graphics.Palette

class MediaArtworkStyle(private val onAccentReady: () -> Unit) {
    var accentColor: Int? = null
        private set

    private var accentSource: Bitmap? = null
    private var blurSource: Bitmap? = null
    private var blurred: Bitmap? = null

    fun update(artwork: Bitmap?) {
        if (artwork == null) {
            clear()
            return
        }
        if (accentSource === artwork) return
        accentSource = artwork
        try {
            Palette.from(artwork).generate { palette ->
                if (accentSource !== artwork) return@generate
                val swatch = palette?.vibrantSwatch
                    ?: palette?.lightVibrantSwatch
                    ?: palette?.dominantSwatch
                    ?: palette?.mutedSwatch
                val raw = swatch?.rgb ?: return@generate
                val hsv = FloatArray(3)
                Color.colorToHSV(raw, hsv)
                hsv[1] = (hsv[1] * 0.75f).coerceIn(0.25f, 0.90f)
                hsv[2] = (hsv[2] * 1.25f).coerceIn(0.85f, 1.0f)
                accentColor = Color.HSVToColor(hsv)
                onAccentReady()
            }
        } catch (_: Exception) {
        }
    }

    // Down/up-scaling is a cheap blur that only runs once per artwork.
    fun blurredFor(artwork: Bitmap?): Bitmap? {
        artwork ?: return null
        if (blurSource !== artwork) {
            blurSource = artwork
            blurred = try {
                val small = Bitmap.createScaledBitmap(artwork, 8, 8, true)
                Bitmap.createScaledBitmap(small, artwork.width.coerceAtLeast(1), artwork.height.coerceAtLeast(1), true)
            } catch (_: Exception) {
                artwork
            }
        }
        return blurred
    }

    fun clear() {
        accentSource = null
        accentColor = null
        blurSource = null
        blurred = null
    }
}
