package com.sameerasw.essentials.island.plugins.media

import android.graphics.BlurMaskFilter
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.plugins.media.particles.MediaCameraClearance
import com.sameerasw.essentials.island.plugins.media.particles.MediaCameraGeometry
import com.sameerasw.essentials.island.plugins.media.particles.MediaVisualGeometry
import kotlin.math.max
import kotlin.math.min

private class SurfaceGradient(val center: Offset, val radiusX: Float, val radiusY: Float, val stops: FloatArray)

private fun surfaceGradient(rect: Rect, camera: MediaCameraGeometry?, clearance: Float): SurfaceGradient {
    val blackRadius = (camera?.radius?.coerceAtLeast(0f) ?: 0f) + clearance
    val radiusX = max(rect.width * 1.525f, blackRadius * 1.05f).coerceAtLeast(1f)
    val radiusY = max(rect.height * 1.525f, blackRadius * 1.05f).coerceAtLeast(1f)
    val first = max(0.05f, blackRadius / min(radiusX, radiusY)).coerceAtMost(0.96f)
    val stops = floatArrayOf(0.05f, 0.25f, 0.45f, 0.65f, 0.85f, 1f)
        .map { first + (it - 0.05f) / 0.95f * (1f - first) }.toFloatArray()
    return SurfaceGradient(camera?.center ?: Offset(rect.center.x, rect.top + rect.height * 0.175f), radiusX, radiusY, stops)
}

private class GlowLayer {
    val path = android.graphics.Path().apply { fillType = android.graphics.Path.FillType.EVEN_ODD }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var maskRadius = -1f
}

private fun DrawScope.innerGlow(layer: GlowLayer, rect: Rect, corner: Float, color: Color, radius: Float, spread: Float, dy: Float) {
    val innerCorner = (corner - spread).coerceAtLeast(0f)
    val reach = radius * 2f + abs(dy) + spread
    layer.path.rewind()
    layer.path.addRect(rect.left - reach, rect.top - reach, rect.right + reach, rect.bottom + reach, android.graphics.Path.Direction.CW)
    layer.path.addRoundRect(rect.left + spread, rect.top + spread + dy, rect.right - spread, rect.bottom - spread + dy, innerCorner, innerCorner, android.graphics.Path.Direction.CW)
    if (layer.maskRadius != radius) {
        layer.maskRadius = radius
        layer.paint.maskFilter = if (radius > 0f) BlurMaskFilter(radius / 2f, BlurMaskFilter.Blur.NORMAL) else null
    }
    layer.paint.color = color.toArgb()
    drawContext.canvas.nativeCanvas.drawPath(layer.path, layer.paint)
}

private fun shadowClearUntil(rect: Rect, corner: Float, camera: MediaCameraGeometry?, clearance: Float): Float =
    max(rect.top + corner, camera?.takeIf { it.radius > 0f }?.let { it.center.y + it.radius + clearance } ?: rect.top)

@Composable
fun MediaSurface(
    accent: Color,
    corner: Dp,
    pulse: () -> Float,
    geometry: (() -> MediaVisualGeometry)?,
    camera: () -> MediaCameraGeometry?,
    modifier: Modifier = Modifier,
    transparent: Boolean = true,
    transparencyIntensity: Float = 1f,
    progressiveBlur: Boolean = true,
    glowIntensity: Float = 1f,
) {
    val accentState = rememberUpdatedState(accent)
    var viewportHeight by remember { mutableIntStateOf(1) }
    val softGlow = remember { GlowLayer() }
    val rimGlow = remember { GlowLayer() }
    val cardPath = remember { Path() }
    val blurModifier = if (!transparent || !progressiveBlur) Modifier else Modifier.blur {
        edgeTreatment = if (geometry != null) BlurredEdgeTreatment.Unbounded else BlurredEdgeTreatment.Rectangle
        val p = pulse().coerceIn(0f, 1f)
        val rect = geometry?.invoke()?.bounds
        val top = ((rect?.top ?: 0f) / viewportHeight).coerceIn(0f, 1f)
        val bottom = ((rect?.bottom ?: viewportHeight.toFloat()) / viewportHeight).coerceIn(top, 1f)
        radius = BlurRadiusSpec.verticalGradient(
            listOf(0f to 2f, top to 2f, bottom to 16f, 1f to 16f)
                .distinctBy { it.first }
                .map { (position, value) -> BlurStop(position, value.dp * p) },
        )
    }
    fun surfaceRect(size: Size): Pair<Rect, Float> {
        val g = geometry?.invoke()
        val rect = g?.bounds ?: Rect(0f, 0f, size.width, size.height)
        return rect to (g?.cornerRadius ?: 0f)
    }
    Box(modifier.onSizeChanged { viewportHeight = it.height.coerceAtLeast(1) }) {
        Box(Modifier.matchParentSize().then(blurModifier)) {
            Box(Modifier.matchParentSize().drawWithCache {
                val (rect, geometryCorner) = surfaceRect(size)
                val cornerPx = (if (geometry != null) geometryCorner else corner.toPx())
                    .coerceIn(0f, minOf(rect.width, rect.height).coerceAtLeast(0f) / 2f)
                val gradient = surfaceGradient(rect, camera(), MediaCameraClearance.toPx())
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        0f, 0f, 1f,
                        if (transparent) {
                            intArrayOf(0xFF, 0xF2, 0xE6, 0xCC, 0x8C, 0x80).mapIndexed { index, base ->
                                val alpha = if (index == 0) base else (255 - (255 - base) * transparencyIntensity).toInt().coerceIn(0, 255)
                                alpha shl 24
                            }.toIntArray()
                        } else {
                            IntArray(6) { 0xFF000000.toInt() }
                        },
                        gradient.stops,
                        Shader.TileMode.CLAMP,
                    ).apply {
                        setLocalMatrix(Matrix().apply {
                            setScale(gradient.radiusX, gradient.radiusY)
                            postTranslate(gradient.center.x, gradient.center.y)
                        })
                    }
                }
                onDrawBehind {
                    if (rect.width > 0f && rect.height > 0f) {
                        drawContext.canvas.nativeCanvas.drawRoundRect(rect.left, rect.top, rect.right, rect.bottom, cornerPx, cornerPx, paint)
                    }
                }
            })
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithCache {
                        val (rect, geometryCorner) = surfaceRect(size)
                        val cornerPx = if (geometry != null) geometryCorner else corner.toPx()
                        val clearUntil = shadowClearUntil(rect, cornerPx, camera(), MediaCameraClearance.toPx())
                        val mask = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.25f to Color.White.copy(alpha = 0.08f),
                            0.5f to Color.White.copy(alpha = 0.3f),
                            0.75f to Color.White.copy(alpha = 0.7f),
                            1f to Color.White,
                            startY = clearUntil,
                            endY = clearUntil + 72.dp.toPx(),
                        )
                        onDrawWithContent {
                            drawContent()
                            drawRect(mask, blendMode = BlendMode.DstIn)
                        }
                    }
                    .drawBehind {
                        if (glowIntensity <= 0f) return@drawBehind
                        val (rect, geometryCorner) = surfaceRect(size)
                        if (rect.width <= 0f || rect.height <= 0f) return@drawBehind
                        val cornerPx = (if (geometry != null) geometryCorner else corner.toPx())
                            .coerceIn(0f, minOf(rect.width, rect.height) / 2f)
                        cardPath.rewind()
                        cardPath.addRoundRect(RoundRect(rect, CornerRadius(cornerPx)))
                        clipPath(cardPath) {
                            innerGlow(softGlow, rect, cornerPx, accentState.value.copy(alpha = (0.6f * glowIntensity).coerceAtMost(1f)), 40.dp.toPx() * glowIntensity.coerceAtLeast(1f), 4.dp.toPx(), (-14).dp.toPx())
                            innerGlow(rimGlow, rect, cornerPx, accentState.value.copy(alpha = (0.5f * glowIntensity).coerceAtMost(1f)), 3.dp.toPx(), 0f, 0f)
                        }
                    },
            )
        }
        Box(
            Modifier.matchParentSize().drawWithCache {
                val spot = camera()
                val (rect, geometryCorner) = surfaceRect(size)
                val cornerPx = (if (geometry != null) geometryCorner else corner.toPx())
                    .coerceIn(0f, minOf(rect.width, rect.height).coerceAtLeast(0f) / 2f)
                val clip = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(cornerPx))) }
                val clearance = MediaCameraClearance.toPx()
                onDrawBehind {
                    if (spot != null && spot.radius > 0f && rect.width > 0f && rect.height > 0f) {
                        clipPath(clip) { drawCircle(Color.Black, radius = spot.radius + clearance, center = spot.center) }
                    }
                }
            },
        )
    }
}
