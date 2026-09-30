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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

@Composable
fun Modifier.rainOnGlass(intensity: Float, slant: Float, light: () -> Offset): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || intensity <= 0f) return this
    return RainOnGlassApi33.apply(this, intensity.coerceIn(0f, 1f), slant, LocalDensity.current.density, light)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object RainOnGlassApi33 {
    @Composable
    fun apply(modifier: Modifier, intensity: Float, slant: Float, density: Float, light: () -> Offset): Modifier {
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
            shader.setFloatUniform("intensity", intensity)
            shader.setFloatUniform("slant", slant)
            shader.setFloatUniform("density", density)
            val l = light()
            shader.setFloatUniform("lightPos", l.x * size.width, l.y)
            renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        }
    }

    private const val SOURCE = """
uniform shader content;
uniform float2 resolution;
uniform float time;
uniform float intensity;
uniform float slant;
uniform float density;
uniform float2 lightPos;

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float hash12(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

void drop(float2 p, float2 c, float2 radii, inout float h, inout float2 g, inout float3 s, float2 L, inout float sh) {
    float2 d = (p - c) / radii;
    float r2 = dot(d, d);
    if (r2 < 1.0) {
        float w = 1.0 - r2;
        h += w * w;
        g += (-4.0 * w * d) * radii;
        float cw = min(1.0, w * 4.0);
        s += float3(d * cw, cw);
    } else if (r2 < 2.6) {
        float t = 1.0 - (r2 - 1.0) / 1.6;
        sh += t * t * max(dot(-normalize(d), L), 0.0);
    }
}

float mergeY(float sN, float travel, float offset) {
    return travel * (0.2 + 0.5 * hash11(sN * 6.7)) - offset;
}

float pathX(float y, float sN, float col, float colW, float travel, float offset, float density, float slant) {
    float ym = mergeY(sN, travel, offset);
    float dev = (hash11(sN * 3.7) - 0.5) * colW * 0.9;
    float wob = 0.16 * colW * (sin(y * 0.011 / density + sN * 5.0) + 0.5 * sin(y * 0.027 / density + sN * 9.0));
    float kink = smoothstep(ym, ym + 140.0 * density, y) * dev;
    return (col + 0.5 + (hash11(sN * 4.9) - 0.5) * 0.35) * colW + wob + kink + slant * y * 0.12;
}

half4 main(float2 p) {
    float h = 0.0;
    float2 g = float2(0.0);
    float3 s = float3(0.0);
    float sh = 0.0;
    float2 L = normalize(lightPos - p + float2(0.0001, 0.0001));

    float cell = 30.0 * density;
    float2 cid = floor(p / cell);
    float2 hh = float2(hash12(cid), hash12(cid + 17.3));
    if (hh.x < intensity * intensity * 0.3) {
        float2 c = (cid + 0.25 + 0.5 * float2(hash12(cid + 3.1), hash12(cid + 9.7))) * cell;
        float r = (1.8 + 3.0 * hh.y) * density;
        drop(p, c, float2(r, r * 1.1), h, g, s, L, sh);
    }

    float colW = 58.0 * density;
    float colF = p.x / colW;
    float col0 = floor(colF);
    float neighbour = fract(colF) < 0.5 ? -1.0 : 1.0;
    float travel = resolution.y + 320.0 * density;
    float offset = 160.0 * density;
    for (int side = 0; side < 2; side++) {
        float col = col0 + (side == 0 ? 0.0 : neighbour);
        for (int k = 0; k < 3; k++) {
            float seed = col * 7.13 + float(k) * 19.7 + 3.0;
            if (hash11(seed * 3.1) < clamp(intensity * 1.3 - float(k) * 0.35, 0.0, 1.0) * 0.75) {
                float baseSpeed = (0.005 + 0.010 * hash11(seed * 5.3)) * resolution.y;
                float s1 = 6.283 * hash11(seed * 1.3);
                float s2 = 6.283 * hash11(seed * 2.9);
                float w1 = 1.1 + 0.6 * hash11(seed * 6.1);
                float w2 = 2.6 + 1.0 * hash11(seed * 7.7);
                float phase = hash11(seed * 9.7) * travel;

                float raw = phase + baseSpeed * (1.5 * time + 0.4 * sin(w1 * time + s1) + 0.18 * sin(w2 * time + s2));
                float pass = floor(raw / travel);
                float sN = seed + pass * 31.7;
                float y = mod(raw, travel) - offset;
                float big = hash11(sN * 2.3);
                float r = (7.0 + 10.0 * big * big) * density;
                float ym = mergeY(sN, travel, offset);
                float grow = smoothstep(ym - 30.0 * density, ym + 40.0 * density, y);
                float rr = r * mix(0.62, 1.0, grow);
                float x = pathX(y, sN, col, colW, travel, offset, density, slant);

                float v = 1.5 + 0.4 * w1 * cos(w1 * time + s1) + 0.18 * w2 * cos(w2 * time + s2);
                float sn = clamp(v / 2.83, 0.0, 1.0);
                float rx = rr * (1.0 - 0.12 * sn);
                float ry = rr * (1.15 + 0.2 * sn);
                drop(p, float2(x, y - ry), float2(rx, ry), h, g, s, L, sh);
                if (y < ym + 6.0 * density) {
                    float fside = hash11(sN * 1.9) < 0.5 ? -1.0 : 1.0;
                    float fr = r * (0.4 + 0.25 * hash11(sN * 2.1));
                    float fx = pathX(ym, sN, col, colW, travel, offset, density, slant) + fside * (r * 0.7 + fr);
                    drop(p, float2(fx, ym + fr * 0.5), float2(fr, fr * 1.1), h, g, s, L, sh);
                }
            }
        }
    }

    half4 base = content.eval(p);
    base.rgb *= 1.0 - min(sh, 1.0) * 0.07;
    float mask = smoothstep(0.02, 0.14, h);
    if (mask < 0.004) {
        return base;
    }
    float2 sp = p + g * 0.22;
    half4 refracted = (content.eval(sp) + content.eval(sp + float2(1.5, 0.0)) + content.eval(sp + float2(0.0, 1.5)) + content.eval(sp + float2(-1.5, -1.0))) * 0.25;
    float2 nxy = s.xy / max(s.z, 0.0001);
    float tl = length(nxy);
    float2 n = nxy / max(tl, 0.0001);
    float inset = smoothstep(0.2, 1.0, tl);
    float lit = pow(max(dot(n, L), 0.0), 4.0) * inset * 0.34;
    float shadow = pow(max(dot(n, -L), 0.0), 3.0) * inset * 0.3;
    half4 color = mix(base, refracted, mask);
    color.rgb = color.rgb * (1.0 - shadow * mask) + (lit + 0.03) * mask * color.a;
    return color;
}
"""
}
