package com.sameerasw.essentials.island.plugins.media

import com.sameerasw.essentials.island.ui.components.islandStill
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.sameerasw.essentials.island.plugins.progress.ProgressRing
import com.sameerasw.essentials.island.ui.IslandMotion
import com.sameerasw.essentials.island.ui.components.EqualizerBars
import kotlinx.coroutines.delay

const val MEDIA_PILL_WIDTH = 2.2f

private const val STILL_PROGRESS_POLL_MS = 30_000L

@Composable
private fun rememberPlaybackProgress(playing: Boolean, read: () -> Float): Float {
    var value by remember { mutableFloatStateOf(read()) }
    val still = islandStill()
    LaunchedEffect(playing, still) {
        while (true) {
            value = read()
            if (!playing) break
            delay(if (still) STILL_PROGRESS_POLL_MS else 500L)
        }
    }
    return value
}

@Composable
fun MediaBubbleRing(art: ImageBitmap?, playing: Boolean, progress: () -> Float, accent: Color, size: Dp) {
    ProgressRing(rememberPlaybackProgress(playing, progress), false, art, accent, size)
}

@Composable
fun MediaBubblePill(art: ImageBitmap?, playing: Boolean, progress: () -> Float, accent: Color, height: Dp, fillArt: Boolean) {
    val fraction by animateFloatAsState(rememberPlaybackProgress(playing, progress).coerceIn(0f, 1f), IslandMotion.float(), label = "mediaPillProgress")
    Box(Modifier.fillMaxSize()) {
        if (fillArt && art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().padding(height * 0.1f).clip(RoundedCornerShape(50)),
            )
        } else {
            Row(
                Modifier.fillMaxSize().padding(horizontal = height * 0.19f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (art != null) {
                    Image(
                        bitmap = art,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(height * 0.62f).clip(CircleShape),
                    )
                }
                EqualizerBars(playing, accent, size = height * 0.5f)
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.height * 0.1f
            val inset = stroke / 2f
            val w = this.size.width - stroke
            val h = this.size.height - stroke
            val r = h / 2f
            val path = Path().apply {
                moveTo(inset + w / 2f, inset)
                lineTo(inset + w - r, inset)
                arcTo(Rect(inset + w - 2 * r, inset, inset + w, inset + h), -90f, 180f, false)
                lineTo(inset + r, inset + h)
                arcTo(Rect(inset, inset, inset + 2 * r, inset + h), 90f, 180f, false)
                close()
            }
            drawPath(path, accent.copy(alpha = 0.25f), style = Stroke(stroke))
            val measure = PathMeasure().apply { setPath(path, false) }
            val segment = Path()
            measure.getSegment(0f, measure.length * fraction, segment, true)
            drawPath(segment, accent, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}
