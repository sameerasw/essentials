package com.sameerasw.essentials.ui.features.weather

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.sameerasw.essentials.weather.model.WeatherCondition
import com.sameerasw.essentials.weather.model.WeatherSnapshot
import java.util.Calendar

class WeatherPalette(
    val glow: Color,
    val glowSecondary: Color,
    val accent: Color,
    val base: Color = Color.Black,
    val card: Color = Color.White.copy(alpha = 0.09f),
    val onBase: Color = Color.White,
    val onBaseMuted: Color = Color.White.copy(alpha = 0.66f),
) {
    private enum class Phase(val sky: Color, val skyLow: Color, val accent: Color) {
        NIGHT(Color(0xFF0B1230), Color(0xFF070B1C), Color(0xFFB8C4FF)),
        DAWN(Color(0xFF4A3470), Color(0xFF8A4B5E), Color(0xFFFFB38A)),
        DAY(Color(0xFF14609F), Color(0xFF0C3C66), Color(0xFFFFD166)),
        DUSK(Color(0xFF5B2B66), Color(0xFF9A4A3C), Color(0xFFFF9E80)),
    }

    private class Tint(val color: Color, val accent: Color, val amount: Float)

    companion object {
        val Neutral = WeatherPalette(Color(0xFF1E1E1E), Color(0xFF121212), Color.White)

        private const val TWILIGHT_MS = 50 * 60_000L

        fun from(snapshot: WeatherSnapshot?, now: Long, override: String? = null): WeatherPalette {
            if (snapshot == null) return Neutral
            val phase = when (override) {
                "dawn" -> Phase.DAWN
                "day" -> Phase.DAY
                "dusk" -> Phase.DUSK
                "night" -> Phase.NIGHT
                else -> phaseFor(snapshot, now)
            }
            val tint = tintFor(snapshot.condition)
            val glow = lerp(phase.sky, tint.color, tint.amount)
            val secondary = lerp(phase.skyLow, lerp(tint.color, Color.Black, 0.45f), tint.amount)
            val accent = lerp(phase.accent, tint.accent, tint.amount * 0.7f)
            return WeatherPalette(glow, secondary, accent)
        }

        private fun phaseFor(snapshot: WeatherSnapshot, now: Long): Phase {
            val rise = snapshot.extras?.sunriseMillis
            val set = snapshot.extras?.sunsetMillis
            if (rise != null && set != null) {
                val day = 24 * 60 * 60_000L
                val shift = Math.floorDiv(now - rise, day) * day
                val r = rise + shift
                val s = set + shift
                return when {
                    now in (r - TWILIGHT_MS)..(r + TWILIGHT_MS) -> Phase.DAWN
                    now in (s - TWILIGHT_MS)..(s + TWILIGHT_MS) -> Phase.DUSK
                    now in r..s -> Phase.DAY
                    else -> Phase.NIGHT
                }
            }
            val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
            return when {
                !snapshot.isDay -> Phase.NIGHT
                hour in 5..7 -> Phase.DAWN
                hour in 17..19 -> Phase.DUSK
                else -> Phase.DAY
            }
        }

        private fun tintFor(condition: WeatherCondition): Tint = when (condition) {
            WeatherCondition.CLEAR -> Tint(Color(0xFF14609F), Color(0xFFFFD166), 0f)
            WeatherCondition.PARTLY_CLOUDY -> Tint(Color(0xFF3B5A75), Color(0xFFFFE0A0), 0.25f)
            WeatherCondition.CLOUDY -> Tint(Color(0xFF3A434D), Color(0xFFB0BEC5), 0.6f)
            WeatherCondition.FOG -> Tint(Color(0xFF424A50), Color(0xFFCFD8DC), 0.7f)
            WeatherCondition.DRIZZLE -> Tint(Color(0xFF22384A), Color(0xFF7CC4FF), 0.55f)
            WeatherCondition.RAIN -> Tint(Color(0xFF1C3145), Color(0xFF6EC1FF), 0.65f)
            WeatherCondition.HEAVY_RAIN -> Tint(Color(0xFF142234), Color(0xFF4FA3E3), 0.75f)
            WeatherCondition.SLEET -> Tint(Color(0xFF2B3A4A), Color(0xFFBBD9F2), 0.65f)
            WeatherCondition.SNOW -> Tint(Color(0xFF3A4C60), Color(0xFFDCEBFF), 0.6f)
            WeatherCondition.HAIL -> Tint(Color(0xFF26343F), Color(0xFFB7D4EA), 0.7f)
            WeatherCondition.THUNDERSTORM -> Tint(Color(0xFF281E45), Color(0xFFFFE066), 0.8f)
            WeatherCondition.UNKNOWN -> Tint(Color(0xFF2A2A2A), Color.White, 0.5f)
        }
    }
}

@Composable
fun animatedPalette(target: WeatherPalette): WeatherPalette {
    val glow by animateColorAsState(target.glow, tween(900), label = "weatherGlow")
    val secondary by animateColorAsState(target.glowSecondary, tween(900), label = "weatherGlowSecondary")
    val accent by animateColorAsState(target.accent, tween(900), label = "weatherAccent")
    return WeatherPalette(glow, secondary, accent, target.base, target.card, target.onBase, target.onBaseMuted)
}
