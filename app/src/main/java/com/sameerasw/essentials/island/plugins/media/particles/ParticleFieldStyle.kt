package com.sameerasw.essentials.island.plugins.media.particles

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ParticleShape { Circle, Diamond }

@Immutable
data class ParticleFieldStyle(
    val minDistance: Dp = 6.dp,
    val minRadius: Dp = 0.dp,
    val maxRadius: Dp = 1.5.dp,
    val displacement: Dp = 12.dp,
    val noiseScale: Dp = (1f/DEFAULT_NOISE_FREQUENCY).dp,
    val speed: Float = 1f,
    val outerBand: Dp = 64.dp,
    val innerBand: Dp = 48.dp,
    val shape: ParticleShape = ParticleShape.Circle,
    val falloffExponent: Float = 4f,
    val opacity: Float = 0.35f,
) {
    companion object {
        const val DEFAULT_NOISE_FREQUENCY = 0.012f
    }

    init {
        require(minDistance.value.isFinite() && minDistance.value > 0f)
        require(listOf(minRadius,maxRadius,displacement,innerBand).all { it.value.isFinite() && it.value >= 0f })
        require(minRadius <= maxRadius)
        require(noiseScale.value.isFinite() && noiseScale.value > 0f)
        require(speed.isFinite() && speed >= 0f)
        require(outerBand.value.isFinite() && outerBand.value > 0f)
        require(falloffExponent.isFinite() && falloffExponent >= 0f)
        require(opacity.isFinite() && opacity in 0f..1f)
    }
    internal fun resolve(density: Density): ParticleFieldPixels = with(density) {
        ParticleFieldPixels(minDistance.toPx(),minRadius.toPx(),maxRadius.toPx(),displacement.toPx(),
            noiseScale.toPx(),speed,outerBand.toPx(),innerBand.toPx(),shape,this.density,falloffExponent)
    }
}

internal data class ParticleFieldPixels(val minDistance: Float,val minRadius: Float,val radius: Float,
    val displacement: Float,val noiseScale: Float,val speed: Float,val outerBand: Float,val innerBand: Float,
    val shape: ParticleShape,val density: Float,val falloffExponent: Float)

internal fun particleAlphaEnvelope(distance: Float,inner: Float,outer: Float): Float {
    val factor = if (distance < 0f) {
        if (inner <= 0f) return 0f
        (distance+inner)/inner
    } else 1f-distance/outer
    val x = factor.coerceIn(0f,1f)
    return x*x*(3f-2f*x)
}
