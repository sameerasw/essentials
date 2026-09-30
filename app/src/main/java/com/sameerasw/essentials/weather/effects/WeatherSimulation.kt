package com.sameerasw.essentials.weather.effects

import com.sameerasw.essentials.weather.effects.WeatherEffectLayer.Clouds
import com.sameerasw.essentials.weather.model.WeatherCondition
import com.sameerasw.essentials.weather.model.WeatherSnapshot
import com.sameerasw.essentials.weather.effects.WeatherEffectLayer.Fog
import com.sameerasw.essentials.weather.effects.WeatherEffectLayer.Hail
import com.sameerasw.essentials.weather.effects.WeatherEffectLayer.Lightning
import com.sameerasw.essentials.weather.effects.WeatherEffectLayer.Rain
import com.sameerasw.essentials.weather.effects.WeatherEffectLayer.Snow
import com.sameerasw.essentials.weather.effects.WeatherEffectLayer.Stars
import com.sameerasw.essentials.weather.effects.WeatherEffectLayer.SunGlow

class WeatherSimulationPreset(val id: String, val label: String, val spec: WeatherEffectSpec)

object WeatherSimulation {
    const val OFF = "off"

    val presets = listOf(
        WeatherSimulationPreset(OFF, "Off", WeatherEffectSpec.None),
        preset("sunny", "Sunny", SunGlow(1f)),
        preset("clear_night", "Clear night", Stars(1f)),
        preset("partly_cloudy", "Partly cloudy", SunGlow(0.5f), Clouds(0.4f)),
        preset("partly_cloudy_night", "Partly cloudy night", Stars(0.4f), Clouds(0.4f)),
        preset("cloudy", "Cloudy", Clouds(0.85f)),
        preset("overcast", "Overcast", Clouds(1f)),
        preset("light_fog", "Light fog", Fog(0.5f)),
        preset("fog", "Dense fog", Fog(1f)),
        preset("drizzle", "Drizzle", Clouds(0.5f), Rain(0.3f, 0f)),
        preset("rain", "Rain", Clouds(0.6f), Rain(0.6f, 0.1f)),
        preset("heavy_rain", "Heavy rain", Clouds(0.8f), Rain(1f, 0.2f)),
        preset("windy_rain", "Windy rain", Clouds(0.7f), Rain(0.8f, 0.6f)),
        preset("sleet", "Sleet", Rain(0.45f, 0.1f), Snow(0.4f)),
        preset("light_snow", "Light snow", Clouds(0.4f), Snow(0.35f)),
        preset("snow", "Snow", Clouds(0.4f), Snow(0.75f)),
        preset("blizzard", "Blizzard", Clouds(0.9f), Snow(1f), Fog(0.5f)),
        preset("hail", "Hail", Clouds(0.6f), Hail(0.8f)),
        preset("thunderstorm", "Thunderstorm", Clouds(0.9f), Rain(0.8f, 0.2f), Lightning(1f)),
        preset("dry_lightning", "Dry lightning", Clouds(0.7f), Lightning(1f)),
        preset("everything", "Everything", Clouds(1f), Rain(1f, 0.4f), Snow(1f), Hail(1f), Lightning(1f)),
    )

    fun apply(snapshot: WeatherSnapshot, preset: WeatherSimulationPreset): WeatherSnapshot =
        snapshot.copy(
            condition = conditionOf(preset.spec),
            conditionText = preset.label,
            isDay = preset.spec.layers.none { it is Stars },
        )

    fun withTimeOfDay(snapshot: WeatherSnapshot, override: String?): WeatherSnapshot =
        if (override == null) snapshot else snapshot.copy(isDay = override != "night")

    fun timeFor(snapshot: WeatherSnapshot?, override: String?, clock: Long): Long {
        val rise = snapshot?.extras?.sunriseMillis
        val set = snapshot?.extras?.sunsetMillis
        if (override == null || rise == null || set == null) return clock
        val day = 24 * 60 * 60_000L
        val shift = Math.floorDiv(clock - rise, day) * day
        val r = rise + shift
        val s = set + shift
        return when (override) {
            "dawn" -> r
            "day" -> (r + s) / 2
            "dusk" -> s
            else -> (s + r + day) / 2
        }
    }

    private fun conditionOf(spec: WeatherEffectSpec): WeatherCondition {
        val layers = spec.layers
        val rain = layers.filterIsInstance<Rain>().firstOrNull()
        return when {
            layers.any { it is Lightning } -> WeatherCondition.THUNDERSTORM
            layers.any { it is Hail } -> WeatherCondition.HAIL
            layers.any { it is Snow } && rain != null -> WeatherCondition.SLEET
            layers.any { it is Snow } -> WeatherCondition.SNOW
            rain != null -> if (rain.intensity >= 0.9f) WeatherCondition.HEAVY_RAIN else if (rain.intensity > 0.4f) WeatherCondition.RAIN else WeatherCondition.DRIZZLE
            layers.any { it is Fog } -> WeatherCondition.FOG
            layers.any { it is Clouds && it.intensity > 0.7f } -> WeatherCondition.CLOUDY
            layers.any { it is Clouds } -> WeatherCondition.PARTLY_CLOUDY
            else -> WeatherCondition.CLEAR
        }
    }

    fun find(id: String?): WeatherSimulationPreset? = presets.firstOrNull { it.id == id && it.id != OFF }

    private fun preset(id: String, label: String, vararg layers: WeatherEffectLayer) =
        WeatherSimulationPreset(id, label, WeatherEffectSpec(layers.toList()))
}
