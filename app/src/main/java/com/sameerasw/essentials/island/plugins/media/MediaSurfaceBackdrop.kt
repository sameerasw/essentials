package com.sameerasw.essentials.island.plugins.media

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.blur.BlurRadiusSpec
import androidx.compose.ui.graphics.blur.BlurStop
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.ui.IslandCameraSpot
import com.sameerasw.essentials.island.ui.LocalIslandCamera
import com.sameerasw.essentials.island.ui.LocalIslandPulse
import kotlin.math.max
import kotlin.math.min

private val CameraClearance = 4.dp

private class SurfaceGradient(val center: Offset, val radiusX: Float, val radiusY: Float, val stops: FloatArray)

private fun surfaceGradient(rect: Rect, camera: IslandCameraSpot?, clearance: Float): SurfaceGradient {
    val blackRadius = (camera?.radius?.coerceAtLeast(0f) ?: 0f) + clearance
    val radiusX = max(rect.width * 1.525f, blackRadius * 1.05f).coerceAtLeast(1f)
    val radiusY = max(rect.height * 1.525f, blackRadius * 1.05f).coerceAtLeast(1f)
    val first = max(0.05f, blackRadius / min(radiusX, radiusY)).coerceAtMost(0.96f)
    val stops = floatArrayOf(0.05f, 0.25f, 0.45f, 0.65f, 0.85f, 1f)
        .map { first + (it - 0.05f) / 0.95f * (1f - first) }.toFloatArray()
    return SurfaceGradient(camera?.center ?: Offset(rect.center.x, rect.top + rect.height * 0.175f), radiusX, radiusY, stops)
}

private fun shadowClearUntil(rect: Rect, corner: Float, camera: IslandCameraSpot?, clearance: Float): Float =
    max(rect.top + corner, camera?.takeIf { it.radius > 0f }?.let { it.center.y + it.radius + clearance } ?: rect.top)

@Composable
fun MediaSurfaceBackdrop(accent: Color, corner: Dp, modifier: Modifier = Modifier) {
    val pulse = LocalIslandPulse.current
    val camera = LocalIslandCamera.current
    val shape = RoundedCornerShape(corner)
    val progressiveBlur = Modifier.blur {
        edgeTreatment = BlurredEdgeTreatment.Rectangle
        val p = pulse().coerceIn(0f, 1f)
        radius = BlurRadiusSpec.verticalGradient(listOf(BlurStop(0f, 2.dp * p), BlurStop(1f, 16.dp * p)))
    }
    Box(modifier) {
        Box(Modifier.matchParentSize().then(progressiveBlur).drawWithCache {
            val rect = Rect(0f, 0f, size.width, size.height)
            val gradient = surfaceGradient(rect, camera(), CameraClearance.toPx())
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
            onDrawBehind { drawContext.canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint) }
        })
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithCache {
                    val rect = Rect(0f, 0f, size.width, size.height)
                    val clearUntil = shadowClearUntil(rect, corner.toPx(), camera(), CameraClearance.toPx())
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
                    .matchParentSize()
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
                val clearance = CameraClearance.toPx()
                onDrawBehind {
                    if (spot != null && spot.radius > 0f) drawCircle(Color.Black, radius = spot.radius + clearance, center = spot.center)
                }
            },
        )
    }
}
