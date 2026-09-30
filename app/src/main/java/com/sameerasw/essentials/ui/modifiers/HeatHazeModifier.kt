package com.sameerasw.essentials.ui.modifiers

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

@Composable
fun Modifier.heatHaze(strength: Float): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || strength <= 0f) return this
    return HeatHazeApi33.apply(this, strength.coerceIn(0f, 1f), LocalDensity.current.density)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object HeatHazeApi33 {
    @Composable
    fun apply(modifier: Modifier, strength: Float, density: Float): Modifier {
        val shader = remember { runCatching { RuntimeShader(SOURCE) }.getOrNull() } ?: return modifier
        val time = remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            while (true) {
                time.floatValue = withFrameNanos { (it - start) / 1_000_000_000f }
            }
        }
        return modifier.graphicsLayer {
            shader.setFloatUniform("resolution", size.width, size.height)
            shader.setFloatUniform("time", time.floatValue)
            shader.setFloatUniform("strength", strength)
            shader.setFloatUniform("density", density)
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        }
    }

    private const val SOURCE = """
uniform shader content;
uniform float2 resolution;
uniform float time;
uniform float strength;
uniform float density;

half4 main(float2 p) {
    float rise = p.y / density * 0.045 - time * 2.4;
    float wobble = sin(rise) + 0.6 * sin(rise * 2.3 + p.x / density * 0.03 + time * 0.7);
    float sway = sin(p.x / density * 0.05 + time * 1.3);
    float lower = 0.35 + 0.65 * clamp(p.y / resolution.y, 0.0, 1.0);
    float2 offset = float2(wobble * 1.5, sway * 0.7) * density * strength * lower;
    return content.eval(p + offset);
}
"""
}
