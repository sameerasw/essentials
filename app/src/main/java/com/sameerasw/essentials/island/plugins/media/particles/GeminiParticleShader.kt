package com.sameerasw.essentials.island.plugins.media.particles

internal const val GEMINI_PARTICLE_AGSL = """
uniform float2 uRadiusEncoding;
uniform float uAlpha;
uniform int uShapeType;
layout(color) uniform half4 uColor;

half4 main(float2 coord) {
    float index = floor(coord.y/uRadiusEncoding.x);
    float radius = index*uRadiusEncoding.y;
    float2 p = float2(coord.x,coord.y-(index+0.5)*uRadiusEncoding.x);
    float metric = uShapeType == 1 ? abs(p.x)+abs(p.y) : length(p);
    float sd = (metric-radius)*(uShapeType == 1 ? 0.70710678 : 1.0);
    half alpha = half((1.0-smoothstep(-0.5,0.5,sd))*min(2.0*radius,1.0)*uAlpha)*uColor.a;
    return half4(uColor.rgb*alpha,alpha);
}
"""
