/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: ScrollMotionBlurModifier.kt
 * Description: AGSL directional motion blur modifier for scrollable content.
 */

package com.sameerasw.essentials.ui.modifiers

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.isActive
import org.intellij.lang.annotations.Language
import kotlin.math.abs

@Language("AGSL")
private const val DIRECTIONAL_BLUR_AGSL = """
    uniform shader composable;
    uniform float2 resolution;
    uniform float scrollVelocity;

    half4 main(float2 fragCoord) {
        const int SAMPLES = 10;
        half4 color = half4(0.0);
        float totalWeight = 0.0;
        
        // Scale velocity to pixel blur magnitude with max clamping
        float blurMagnitude = clamp(scrollVelocity * 22.0, -40.0, 40.0);

        for (int i = 0; i < SAMPLES; i++) {
            float offset = (float(i) / float(SAMPLES - 1) - 0.5) * blurMagnitude;
            
            // Sample along Y-axis with boundary clamping to avoid edge artifacts
            float clampedY = clamp(fragCoord.y + offset, 0.0, resolution.y);
            float2 sampleCoord = float2(fragCoord.x, clampedY);
            
            // Linear decay weight for trailing effect
            float weight = 1.0 - abs(offset / (abs(blurMagnitude) + 0.001)) * 0.5;
            
            color += composable.eval(sampleCoord) * weight;
            totalWeight += weight;
        }

        return color / totalWeight;
    }
"""

fun Modifier.scrollMotionBlur(
    scrollState: ScrollState,
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed Modifier
    }

    val animatedVelocity = remember { Animatable(0f) }

    LaunchedEffect(scrollState) {
        var prevValue = scrollState.value
        var prevTimeNanos = 0L

        while (isActive) {
            var newVelocityToSnap = 0f
            withFrameNanos { frameTimeNanos ->
                val currentValue = scrollState.value
                if (prevTimeNanos != 0L) {
                    val dtMs = (frameTimeNanos - prevTimeNanos) / 1_000_000.0f
                    if (dtMs in 1f..100f) {
                        val delta = (currentValue - prevValue).toFloat()
                        val targetVelocity = (delta / dtMs).coerceIn(-3f, 3f)
                        if (abs(delta) > 0.1f && scrollState.isScrollInProgress) {
                            newVelocityToSnap = animatedVelocity.value * 0.35f + targetVelocity * 0.65f
                        } else {
                            // Decay rapidly to 0 when finger holds or stops movement
                            val decayed = animatedVelocity.value * 0.45f
                            newVelocityToSnap = if (abs(decayed) < 0.01f) 0f else decayed
                        }
                    } else {
                        newVelocityToSnap = 0f
                    }
                }
                prevValue = currentValue
                prevTimeNanos = frameTimeNanos
            }
            animatedVelocity.snapTo(newVelocityToSnap)
        }
    }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (!scrollState.isScrollInProgress) {
            animatedVelocity.animateTo(0f, animationSpec = tween(60))
        }
    }

    val shader = remember { RuntimeShader(DIRECTIONAL_BLUR_AGSL) }

    Modifier.graphicsLayer {
        val vel = animatedVelocity.value
        if (abs(vel) > 0.05f) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("scrollVelocity", vel)

            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "composable")
                .asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

fun Modifier.scrollMotionBlur(
    lazyListState: LazyListState,
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed Modifier
    }

    val animatedVelocity = remember { Animatable(0f) }

    LaunchedEffect(lazyListState) {
        var prevIndex = lazyListState.firstVisibleItemIndex
        var prevOffset = lazyListState.firstVisibleItemScrollOffset
        var prevTimeNanos = 0L

        while (isActive) {
            var newVelocityToSnap = 0f
            withFrameNanos { frameTimeNanos ->
                val currentIndex = lazyListState.firstVisibleItemIndex
                val currentOffset = lazyListState.firstVisibleItemScrollOffset
                if (prevTimeNanos != 0L) {
                    val dtMs = (frameTimeNanos - prevTimeNanos) / 1_000_000.0f
                    if (dtMs in 1f..100f) {
                        val indexDelta = currentIndex - prevIndex
                        val offsetDelta = currentOffset - prevOffset
                        val totalDelta = (indexDelta * 80f) + offsetDelta
                        val targetVelocity = (totalDelta / dtMs).coerceIn(-3f, 3f)
                        if (abs(totalDelta) > 0.1f && lazyListState.isScrollInProgress) {
                            newVelocityToSnap = animatedVelocity.value * 0.35f + targetVelocity * 0.65f
                        } else {
                            // Decay rapidly to 0 when finger holds or stops movement
                            val decayed = animatedVelocity.value * 0.45f
                            newVelocityToSnap = if (abs(decayed) < 0.01f) 0f else decayed
                        }
                    } else {
                        newVelocityToSnap = 0f
                    }
                }
                prevIndex = currentIndex
                prevOffset = currentOffset
                prevTimeNanos = frameTimeNanos
            }
            animatedVelocity.snapTo(newVelocityToSnap)
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            animatedVelocity.animateTo(0f, animationSpec = tween(60))
        }
    }

    val shader = remember { RuntimeShader(DIRECTIONAL_BLUR_AGSL) }

    Modifier.graphicsLayer {
        val vel = animatedVelocity.value
        if (abs(vel) > 0.05f) {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("scrollVelocity", vel)

            renderEffect = RenderEffect
                .createRuntimeShaderEffect(shader, "composable")
                .asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}
