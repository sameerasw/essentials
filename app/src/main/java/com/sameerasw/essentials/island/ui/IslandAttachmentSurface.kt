package com.sameerasw.essentials.island.ui

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import com.sameerasw.essentials.island.model.IslandBeat
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

@Composable
internal fun IslandAttachmentSurface(
    accent: Color,
    corner: Dp,
    spec: IslandLayoutSpec,
    active: Boolean,
    beat: IslandBeat?,
    positionMs: (() -> Long)?,
    modifier: Modifier = Modifier,
) {
    val accentState = rememberUpdatedState(accent)
    val beatState = rememberUpdatedState(beat)
    val positionState = rememberUpdatedState(positionMs)
    val activeState = rememberUpdatedState(active)
    val phaseState = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val step = (now - last) / 1_000_000f / LOOP_MS * TWO_PI
                    phaseState.floatValue = (phaseState.floatValue + step) % TWO_PI
                }
                last = now
            }
        }
    }
    val phase by phaseState
    val rimPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE } }
    val hsv = remember { FloatArray(3) }
    Box(
        modifier
            .drawWithCache {
                val w = size.width
                val h = size.height
                val cornerPx = corner.toPx().coerceAtMost(minOf(w, h) / 2f)
                val body = if (spec.transparency) {
                    fun a(base: Int) = (255 - (255 - base) * spec.transparencyIntensity).toInt()
                        .coerceIn(0, 255) / 255f
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = a(0x80)),
                        0.45f to Color.Black.copy(alpha = a(0xCC)),
                        1f to Color.Black.copy(alpha = a(0xF2)),
                    )
                } else {
                    Brush.verticalGradient(listOf(Color.Black, Color.Black))
                }
                val lightMask = Brush.verticalGradient(
                    0f to Color.White,
                    0.6f to Color.White.copy(alpha = 0.75f),
                    1f to Color.White.copy(alpha = 0.2f)
                )
                val rimWidth = 2.dp.toPx()
                rimPaint.strokeWidth = rimWidth
                rimPaint.maskFilter = BlurMaskFilter(3.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                onDrawBehind {
                    if (w <= 0f || h <= 0f) return@onDrawBehind
                    drawRect(body)
                    val color = accentState.value
                    val tame = if (color.luminance() > 0.8f) 0.4f else 1f
                    val strength = (spec.glowIntensity * tame).coerceIn(0f, 1.5f)
                    if (strength > 0f) {
                        val grid = beatState.value
                        val position = positionState.value
                        val onBeat = grid != null && position != null
                        var beatIndex = 0L
                        var pulse = 0f
                        var drift = phase
                        if (onBeat) {
                            val beats = (position!!() - grid!!.offsetMs) / grid.periodMs
                            val whole = floor(beats)
                            beatIndex = whole.toLong()
                            val accent = if (Math.floorMod(
                                    beatIndex,
                                    BEATS_PER_BAR
                                ) == 0L
                            ) 1f else OFF_BEAT_ACCENT
                            pulse =
                                if (activeState.value) exp(-(beats - whole) * KICK_DECAY) * accent else 0f
                            drift = (beats / DRIFT_BEATS % 1f) * TWO_PI
                        }
                        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                        val baseHue = hsv[0]
                        val baseSat = if (hsv[1] < 0.08f) hsv[1] else max(hsv[1], 0.6f)
                        val baseVal = max(hsv[2], 0.75f)
                        val canvas = drawContext.canvas.nativeCanvas
                        val layer = canvas.saveLayer(0f, 0f, w, h, null)
                        drawRect(
                            Brush.verticalGradient(
                                0f to color.copy(
                                    alpha = ((if (onBeat) 0.3f + 0.35f * pulse else 0.45f) * strength).coerceAtMost(
                                        1f
                                    )
                                ),
                                1f to color.copy(alpha = (0.18f * strength).coerceAtMost(1f)),
                            ),
                        )
                        LIGHTS.forEachIndexed { i, light ->
                            val t = drift * light.speed + light.offset
                            hsv[0] = (baseHue + light.hueShift + 360f) % 360f
                            val spot = if (onBeat && Math.floorMod(
                                    beatIndex,
                                    LIGHTS.size.toLong()
                                ) == i.toLong()
                            ) pulse else 0f
                            hsv[1] =
                                (baseSat * (light.saturation + 0.3f * sin(t * 2f + i)) + 0.25f * pulse + 0.15f * spot).coerceIn(
                                    0f,
                                    1f
                                )
                            hsv[2] = baseVal
                            val level =
                                if (onBeat) 0.4f + 0.45f * pulse + 0.3f * spot else 0.6f + 0.4f * sin(
                                    t * 3f + i * 2f
                                )
                            val alpha = (0.9f * strength * level).coerceIn(0f, 1f)
                            val c =
                                Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = alpha)
                            val center = Offset(
                                w * (light.x + 0.18f * sin(t)),
                                h * (0.1f + 0.15f * sin(t + i)),
                            )
                            val swell =
                                if (onBeat) 0.8f + 0.3f * pulse + 0.1f * spot else 0.8f + 0.2f * sin(
                                    t * 2f
                                )
                            val radius =
                                (max(w * 0.3f, h * 1.1f) * spec.glowSize * swell).coerceAtLeast(1f)
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(c, c.copy(alpha = 0f)),
                                    center,
                                    radius
                                ), radius, center
                            )
                        }
                        drawRect(
                            lightMask,
                            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                        )
                        canvas.restoreToCount(layer)
                        if (spec.glowRim) {
                            rimPaint.shader = android.graphics.LinearGradient(
                                0f, 0f, 0f, h * 0.6f,
                                color.copy(
                                    alpha = ((if (onBeat) 0.45f + 0.45f * pulse else 0.7f) * strength).coerceAtMost(
                                        1f
                                    )
                                ).toArgb(),
                                0,
                                android.graphics.Shader.TileMode.CLAMP,
                            )
                            val inset = rimWidth / 2f
                            val rimCorner = (cornerPx - inset).coerceAtLeast(0f)
                            canvas.drawRoundRect(
                                inset,
                                inset,
                                w - inset,
                                h - inset,
                                rimCorner,
                                rimCorner,
                                rimPaint
                            )
                        }
                    }
                }
            },
    )
}

private class Light(
    val x: Float,
    val hueShift: Float,
    val saturation: Float,
    val speed: Float,
    val offset: Float
)

private val LIGHTS = listOf(
    Light(x = 0.15f, hueShift = -24f, saturation = 1f, speed = 1f, offset = 0f),
    Light(x = 0.4f, hueShift = 0f, saturation = 0.75f, speed = 2f, offset = 2.1f),
    Light(x = 0.62f, hueShift = 30f, saturation = 1.2f, speed = 3f, offset = 4.2f),
    Light(x = 0.85f, hueShift = -10f, saturation = 0.9f, speed = 2f, offset = 5.3f),
)

private const val LOOP_MS = 9_000
private const val TWO_PI = (2 * PI).toFloat()
private const val KICK_DECAY = 3f
private const val BEATS_PER_BAR = 4L
private const val OFF_BEAT_ACCENT = 0.65f
private const val DRIFT_BEATS = 16f
