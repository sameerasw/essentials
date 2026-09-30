package com.sameerasw.essentials.weather.provider

import com.sameerasw.essentials.weather.model.CityResult
import com.sameerasw.essentials.weather.model.WeatherExtras
import com.sameerasw.essentials.weather.model.HourlyForecast
import com.sameerasw.essentials.weather.model.WeatherCondition
import com.sameerasw.essentials.weather.model.WeatherLocation
import com.sameerasw.essentials.weather.model.WeatherSnapshot
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.util.Locale
import kotlin.math.exp

class MetNorwayProvider : WeatherProvider {
    override val id = "metno"
    override val displayName = "MET Norway (Yr)"
    override val requiresApiKey = false
    override val signupUrl: String? = null

    override suspend fun fetch(location: WeatherLocation, apiKey: String?): WeatherSnapshot {
        val url = "https://api.met.no/weatherapi/locationforecast/2.0/compact?" +
            String.format(Locale.US, "lat=%.4f&lon=%.4f", location.latitude, location.longitude)
        val json = JSONObject(ProviderHttp.get(url))
        val place = location.name?.let { it to "" } ?: ProviderHttp.reverseGeocode(location.latitude, location.longitude)
        return try {
            parse(json, place)
        } catch (e: JSONException) {
            throw WeatherProviderException(WeatherProviderException.Reason.BAD_RESPONSE, e.message)
        }
    }

    override suspend fun searchCities(query: String, apiKey: String?): List<CityResult> = ProviderHttp.searchCities(query)

    private fun parse(json: JSONObject, place: Pair<String, String>?): WeatherSnapshot {
        val now = System.currentTimeMillis()
        val series = json.getJSONObject("properties").getJSONArray("timeseries")
        val first = series.getJSONObject(0)
        val details = first.getJSONObject("data").getJSONObject("instant").getJSONObject("details")
        val temp = details.getDouble("air_temperature")
        val humidity = details.optDouble("relative_humidity", 50.0)
        val windMs = details.optDouble("wind_speed", 0.0)

        val hourly = mutableListOf<HourlyForecast>()
        val next24 = mutableListOf<Double>()
        var rainChance = 0
        for (i in 0 until series.length()) {
            val entry = series.getJSONObject(i)
            val time = Instant.parse(entry.getString("time")).toEpochMilli()
            if (time > now + 24 * HOUR_MS) break
            val data = entry.getJSONObject("data")
            val t = data.getJSONObject("instant").getJSONObject("details").optDouble("air_temperature", Double.NaN)
            if (!t.isNaN()) next24 += t
            val next = data.optJSONObject("next_1_hours") ?: continue
            if (time + HOUR_MS <= now) continue
            val symbol = next.optJSONObject("summary")?.optString("symbol_code").orEmpty()
            val precip = next.optJSONObject("details")?.optDouble("precipitation_amount", 0.0) ?: 0.0
            val chance = chanceFor(precip)
            if (hourly.size < HOURLY_COUNT) {
                hourly += HourlyForecast(
                    timeMillis = time,
                    tempC = t.takeUnless { it.isNaN() } ?: temp,
                    condition = conditionFor(symbol),
                    isDay = !symbol.endsWith("_night"),
                    chanceOfRain = chance,
                )
            }
            if (i < 12) rainChance = maxOf(rainChance, chance)
        }

        val currentSymbol = first.getJSONObject("data").let {
            (it.optJSONObject("next_1_hours") ?: it.optJSONObject("next_6_hours"))
                ?.optJSONObject("summary")?.optString("symbol_code").orEmpty()
        }
        val condition = conditionFor(currentSymbol)
        return WeatherSnapshot(
            locationName = place?.first.orEmpty(),
            region = place?.second.orEmpty(),
            tempC = temp,
            feelsLikeC = apparent(temp, humidity, windMs),
            condition = condition,
            conditionText = textFor(condition),
            isDay = !currentSymbol.endsWith("_night"),
            humidity = humidity.toInt(),
            windKph = windMs * 3.6,
            chanceOfRain = rainChance,
            highC = next24.maxOrNull() ?: temp,
            lowC = next24.minOrNull() ?: temp,
            hourly = hourly,
            alerts = emptyList(),
            updatedAt = now,
            providerId = id,
            extras = WeatherExtras(
                pressureHpa = details.optDouble("air_pressure_at_sea_level").takeUnless { it.isNaN() },
                dewPointC = details.optDouble("dew_point_temperature").takeUnless { it.isNaN() },
                cloudCover = details.optDouble("cloud_area_fraction").takeUnless { it.isNaN() }?.toInt(),
                windGustKph = details.optDouble("wind_speed_of_gust").takeUnless { it.isNaN() }?.times(3.6),
                windDirectionDeg = details.optDouble("wind_from_direction").takeUnless { it.isNaN() },
                uvIndex = details.optDouble("ultraviolet_index_clear_sky").takeUnless { it.isNaN() },
            ),
        )
    }

    // MET only reports amounts, so the chance is derived from how much rain is expected that hour.
    private fun chanceFor(mm: Double): Int = when {
        mm >= 1.0 -> 90
        mm >= 0.3 -> 70
        mm > 0.0 -> 40
        else -> 0
    }

    // Australian Bureau of Meteorology apparent temperature.
    private fun apparent(tempC: Double, humidity: Double, windMs: Double): Double {
        val vapour = humidity / 100.0 * 6.105 * exp(17.27 * tempC / (237.7 + tempC))
        return tempC + 0.33 * vapour - 0.70 * windMs - 4.0
    }

    // https://api.met.no/weatherapi/weathericon/2.0/documentation
    private fun conditionFor(symbol: String): WeatherCondition {
        val s = symbol.substringBefore('_')
        return when {
            s.contains("thunder") -> WeatherCondition.THUNDERSTORM
            s.contains("sleet") -> WeatherCondition.SLEET
            s.contains("snow") -> WeatherCondition.SNOW
            s.startsWith("heavyrain") -> WeatherCondition.HEAVY_RAIN
            s.startsWith("lightrain") -> WeatherCondition.DRIZZLE
            s.contains("rain") -> WeatherCondition.RAIN
            s == "fog" -> WeatherCondition.FOG
            s == "clearsky" || s == "fair" -> WeatherCondition.CLEAR
            s == "partlycloudy" -> WeatherCondition.PARTLY_CLOUDY
            s == "cloudy" -> WeatherCondition.CLOUDY
            else -> WeatherCondition.UNKNOWN
        }
    }

    private fun textFor(condition: WeatherCondition): String = when (condition) {
        WeatherCondition.CLEAR -> "Clear"
        WeatherCondition.PARTLY_CLOUDY -> "Partly cloudy"
        WeatherCondition.CLOUDY -> "Cloudy"
        WeatherCondition.FOG -> "Fog"
        WeatherCondition.DRIZZLE -> "Light rain"
        WeatherCondition.RAIN -> "Rain"
        WeatherCondition.HEAVY_RAIN -> "Heavy rain"
        WeatherCondition.SLEET -> "Sleet"
        WeatherCondition.SNOW -> "Snow"
        WeatherCondition.HAIL -> "Hail"
        WeatherCondition.THUNDERSTORM -> "Thunderstorm"
        WeatherCondition.UNKNOWN -> "Unknown"
    }

    private companion object {
        const val HOUR_MS = 60 * 60 * 1000L
        const val HOURLY_COUNT = 24
    }
}
