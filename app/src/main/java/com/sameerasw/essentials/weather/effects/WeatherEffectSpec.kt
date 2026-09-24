package com.sameerasw.essentials.weather.effects

import com.sameerasw.essentials.weather.model.WeatherCondition
import com.sameerasw.essentials.weather.model.WeatherSnapshot

sealed interface WeatherEffectLayer {
    val intensity: Float

    data class Rain(override val intensity: Float, val slant: Float) : WeatherEffectLayer
    data class Snow(override val intensity: Float) : WeatherEffectLayer
    data class Hail(override val intensity: Float) : WeatherEffectLayer
    data class Clouds(override val intensity: Float) : WeatherEffectLayer
    data class Fog(override val intensity: Float) : WeatherEffectLayer
    data class SunGlow(override val intensity: Float) : WeatherEffectLayer
    data class Stars(override val intensity: Float) : WeatherEffectLayer
    data class Lightning(override val intensity: Float) : WeatherEffectLayer
}

data class WeatherEffectSpec(val layers: List<WeatherEffectLayer>) {
    val isEmpty: Boolean get() = layers.isEmpty()

    companion object {
        val None = WeatherEffectSpec(emptyList())

        fun from(snapshot: WeatherSnapshot): WeatherEffectSpec {
            
            val slant = (snapshot.windKph / 60.0).coerceIn(0.0, 0.6).toFloat()
            val layers = when (snapshot.condition) {
                WeatherCondition.CLEAR ->
                    if (snapshot.isDay) listOf(WeatherEffectLayer.SunGlow(1f)) else listOf(WeatherEffectLayer.Stars(1f))
                WeatherCondition.PARTLY_CLOUDY -> listOfNotNull(
                    WeatherEffectLayer.SunGlow(0.5f).takeIf { snapshot.isDay },
                    WeatherEffectLayer.Stars(0.4f).takeIf { !snapshot.isDay },
                    WeatherEffectLayer.Clouds(0.4f),
                )
                WeatherCondition.CLOUDY -> listOf(WeatherEffectLayer.Clouds(0.85f))
                WeatherCondition.FOG -> listOf(WeatherEffectLayer.Fog(1f))
                WeatherCondition.DRIZZLE -> listOf(WeatherEffectLayer.Clouds(0.5f), WeatherEffectLayer.Rain(0.3f, slant))
                WeatherCondition.RAIN -> listOf(WeatherEffectLayer.Clouds(0.6f), WeatherEffectLayer.Rain(0.6f, slant))
                WeatherCondition.HEAVY_RAIN -> listOf(WeatherEffectLayer.Clouds(0.8f), WeatherEffectLayer.Rain(1f, slant))
                WeatherCondition.SLEET -> listOf(WeatherEffectLayer.Rain(0.45f, slant), WeatherEffectLayer.Snow(0.4f))
                WeatherCondition.SNOW -> listOf(WeatherEffectLayer.Clouds(0.4f), WeatherEffectLayer.Snow(0.75f))
                WeatherCondition.HAIL -> listOf(WeatherEffectLayer.Clouds(0.6f), WeatherEffectLayer.Hail(0.8f))
                WeatherCondition.THUNDERSTORM -> listOf(
                    WeatherEffectLayer.Clouds(0.9f),
                    WeatherEffectLayer.Rain(0.8f, slant),
                    WeatherEffectLayer.Lightning(1f),
                )
                WeatherCondition.UNKNOWN -> emptyList()
            }
            return WeatherEffectSpec(layers)
        }
    }
}
