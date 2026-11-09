package com.sameerasw.essentials.island.plugins.media

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.plugins.media.particles.MediaCameraClearance
import com.sameerasw.essentials.island.plugins.media.particles.MediaCameraGeometry
import com.sameerasw.essentials.island.plugins.media.particles.MediaVisualGeometry
import com.sameerasw.essentials.island.ui.LocalIslandCamera
import com.sameerasw.essentials.island.ui.LocalIslandPulse
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

private fun shadowClearUntil(rect: Rect, corner: Float, camera: MediaCameraGeometry?, clearance: Float): Float =
    max(rect.top + corner, camera?.takeIf { it.radius > 0f }?.let { it.center.y + it.radius + clearance } ?: rect.top)

@Composable
fun MediaSurfaceBackdrop(accent: Color, corner: Dp, modifier: Modifier = Modifier) {
    val pulse = LocalIslandPulse.current
    val camera = LocalIslandCamera.current
    MediaSurface(
        accent = accent,
        corner = corner,
        pulse = pulse,
        geometry = null,
        camera = { camera()?.let { MediaCameraGeometry(it.center, it.radius) } },
        modifier = modifier,
    )
}

@Composable
fun MediaSurface(
    accent: Color,
    corner: Dp,
    pulse: () -> Float,
    geometry: (() -> MediaVisualGeometry)?,
    camera: () -> MediaCameraGeometry?,
    modifier: Modifier = Modifier,
) {
    var viewportHeight by remember { mutableIntStateOf(1) }
    val shape = remember(geometry, corner) {
        object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                val radius = (geometry?.invoke()?.cornerRadius ?: with(density) { corner.toPx() })
                    .coerceIn(0f, minOf(size.width, size.height) / 2f)
                return Outline.Rounded(RoundRect(Rect(Offset.Zero, size), CornerRadius(radius)))
            }
        }
    }
    val progressiveBlur = Modifier.blur {
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
        Box(Modifier.matchParentSize().then(progressiveBlur).drawWithCache {
            val (rect, geometryCorner) = surfaceRect(size)
            val cornerPx = (if (geometry != null) geometryCorner else corner.toPx())
                .coerceIn(0f, minOf(rect.width, rect.height).coerceAtLeast(0f) / 2f)
            val gradient = surfaceGradient(rect, camera(), MediaCameraClearance.toPx())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    0f, 0f, 1f,
                    intArrayOf(0xFF000000.toInt(), 0xF2000000.toInt(), 0xE6000000.toInt(), 0xCC000000.toInt(), 0x8C000000.toInt(), 0x80000000.toInt()),
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
                .then(progressiveBlur),
        ) {
            Box(
                Modifier
                    .layout { measurable, constraints ->
                        val rect = geometry?.invoke()?.bounds
                            ?: Rect(0f, 0f, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                        val child = measurable.measure(
                            Constraints.fixed(rect.width.roundToInt().coerceAtLeast(0), rect.height.roundToInt().coerceAtLeast(0)),
                        )
                        val left = rect.left.roundToInt()
                        val top = rect.top.roundToInt()
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            child.placeWithLayer(left, top) {
                                translationX = rect.left - left
                                translationY = rect.top - top
                            }
                        }
                    }
                    .innerShadow(shape) {
                        radius = 40.dp.toPx()
                        spread = 4.dp.toPx()
                        color = accent.copy(alpha = 0.6f)
                        offset = Offset(0f, (-14).dp.toPx())
                    }
                    .innerShadow(shape) {
                        radius = 3.dp.toPx()
                        color = accent.copy(alpha = 0.5f)
                        offset = Offset.Zero
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
