package com.sameerasw.essentials.island.plugins.media

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.QuantizerCelebi
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.Score
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class MediaArtworkColors(
    val primary: Color,
    val onPrimary: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
)

internal fun mediaColorsFromSeed(seed: Int): MediaArtworkColors {
    val scheme = SchemeTonalSpot(Hct.fromInt(seed), true, 0.0)
    val roles = MaterialDynamicColors()
    return MediaArtworkColors(
        Color(roles.primary().getArgb(scheme)), Color(roles.onPrimary().getArgb(scheme)),
        Color(roles.onSurface().getArgb(scheme)), Color(roles.onSurfaceVariant().getArgb(scheme)),
    )
}

@Composable
internal fun rememberMediaArtworkColors(artwork: Bitmap?): MediaArtworkColors {
    val theme = MaterialTheme.colorScheme
    val fallback = MediaArtworkColors(theme.primary, theme.onPrimary, theme.onSurface, theme.onSurfaceVariant)
    val fallbackSeed = theme.primary.toArgb()
    val result by produceState(fallback, artwork, fallbackSeed) {
        if (artwork == null) {
            value = fallback
        } else {
            value = withContext(Dispatchers.Default) {
                runCatching {
                    val readable = if (artwork.config == Bitmap.Config.HARDWARE) artwork.copy(Bitmap.Config.ARGB_8888, false) else artwork
                    try {
                        val sample = Bitmap.createScaledBitmap(readable, 64, 64, true)
                        try {
                            val pixels = IntArray(sample.width * sample.height)
                            sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
                            val seed = Score.score(QuantizerCelebi.quantize(pixels, 128)).firstOrNull() ?: fallbackSeed
                            mediaColorsFromSeed(seed)
                        } finally {
                            if (sample !== readable) sample.recycle()
                        }
                    } finally {
                        if (readable !== artwork) readable.recycle()
                    }
                }.getOrDefault(fallback)
            }
        }
    }
    return result
}
