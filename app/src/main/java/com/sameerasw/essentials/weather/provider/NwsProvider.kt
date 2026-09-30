package com.sameerasw.essentials.weather.provider

import com.sameerasw.essentials.weather.model.AlertSeverity
import com.sameerasw.essentials.weather.model.CityResult
import com.sameerasw.essentials.weather.model.HourlyForecast
import com.sameerasw.essentials.weather.model.WeatherAlert
import com.sameerasw.essentials.weather.model.WeatherCondition
import com.sameerasw.essentials.weather.model.WeatherLocation
import com.sameerasw.essentials.weather.model.WeatherSnapshot
import org.json.JSONException
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.exp

class NwsProvider : WeatherProvider {
    override val id = "nws"
    override val displayName = "US National Weather Service"
    override val requiresApiKey = false
    override val signupUrl: String? = null

    private val headers = mapOf("Accept" to "application/geo+json")

    override suspend fun fetch(location: WeatherLocation, apiKey: String?): WeatherSnapshot {
        val point = String.format(Locale.US, "%.4f,%.4f", location.latitude, location.longitude)
        val points = try {
            JSONObject(ProviderHttp.get("https://api.weather.gov/points/$point", headers))
        } catch (e: WeatherProviderException) {
            if (e.message?.contains("404") == true) {
                throw WeatherProviderException(WeatherProviderException.Reason.BAD_RESPONSE, "NWS only covers the United States")
            }
            throw e
        }
        return try {
            val props = points.getJSONObject("properties")
            val hourlyUrl = props.getString("forecastHourly")
            val hourly = JSONObject(ProviderHttp.get("$hourlyUrl?units=si", headers))
            val alerts = try {
                JSONObject(ProviderHttp.get("https://api.weather.gov/alerts/active?point=$point", headers))
            } catch (_: Exception) {
                null
            }
            val relative = props.optJSONObject("relativeLocation")?.optJSONObject("properties")
            val name = location.name ?: relative?.optString("city").orEmpty()
            val region = relative?.optString("state").orEmpty()
            parse(hourly, alerts, name to region)
        } catch (e: JSONException) {
            throw WeatherProviderException(WeatherProviderException.Reason.BAD_RESPONSE, e.message)
        }
    }

    override suspend fun searchCities(query: String, apiKey: String?): List<CityResult> =
        ProviderHttp.searchCities(query).filter { it.country.equals("United States", ignoreCase = true) }

    private fun parse(hourlyJson: JSONObject, alertsJson: JSONObject?, place: Pair<String, String>): WeatherSnapshot {
        val now = System.currentTimeMillis()
        val periods = hourlyJson.getJSONObject("properties").getJSONArray("periods")
        val first = periods.getJSONObject(0)
        val temp = first.getDouble("temperature")
        val humidity = first.optJSONObject("relativeHumidity")?.optDouble("value", 50.0) ?: 50.0
        val windKph = parseWind(first.optString("windSpeed"))

        val hourly = mutableListOf<HourlyForecast>()
        val next24 = mutableListOf<Double>()
        var rainChance = 0
        for (i in 0 until periods.length()) {
            val p = periods.getJSONObject(i)
            val time = parseTime(p.optString("startTime")) ?: continue
            val t = p.optDouble("temperature", Double.NaN)
            val pop = p.optJSONObject("probabilityOfPrecipitation")?.optDouble("value", 0.0)?.toInt() ?: 0
            if (i < 24 && !t.isNaN()) next24 += t
            if (i < 12) rainChance = maxOf(rainChance, pop)
            if (time + HOUR_MS > now && hourly.size < HOURLY_COUNT && !t.isNaN()) {
                hourly += HourlyForecast(
                    timeMillis = time,
                    tempC = t,
                    condition = conditionFor(p.optString("shortForecast")),
                    isDay = p.optBoolean("isDaytime", true),
                    chanceOfRain = pop,
                )
            }
        }

        val features = alertsJson?.optJSONArray("features")
        val alerts = buildList {
            if (features != null) {
                for (i in 0 until features.length()) {
                    val a = features.getJSONObject(i).optJSONObject("properties") ?: continue
                    val event = a.optString("event")
                    add(
                        WeatherAlert(
                            id = "$id:${a.optString("id")}".hashCode().toString(),
                            event = event,
                            headline = a.optString("headline").ifBlank { event },
                            severity = severityFor(a.optString("severity")),
                            description = a.optString("description").trim(),
                            effectiveMillis = parseTime(a.optString("effective")),
                            expiresMillis = parseTime(a.optString("expires")),
                        ),
                    )
                }
            }
        }.distinctBy { it.id }

        val shortForecast = first.optString("shortForecast")
        return WeatherSnapshot(
            locationName = place.first,
            region = place.second,
            tempC = temp,
            feelsLikeC = apparent(temp, humidity, windKph / 3.6),
            condition = conditionFor(shortForecast),
            conditionText = shortForecast,
            isDay = first.optBoolean("isDaytime", true),
            humidity = humidity.toInt(),
            windKph = windKph,
            chanceOfRain = rainChance,
            highC = next24.maxOrNull() ?: temp,
            lowC = next24.minOrNull() ?: temp,
            hourly = hourly,
            alerts = alerts,
            updatedAt = now,
            providerId = id,
        )
    }

    // "10 km/h" or "5 to 10 km/h": use the upper bound.
    private fun parseWind(value: String): Double =
        Regex("[0-9]+(?:\\.[0-9]+)?").findAll(value).mapNotNull { it.value.toDoubleOrNull() }.maxOrNull() ?: 0.0

    private fun apparent(tempC: Double, humidity: Double, windMs: Double): Double {
        val vapour = humidity / 100.0 * 6.105 * exp(17.27 * tempC / (237.7 + tempC))
        return tempC + 0.33 * vapour - 0.70 * windMs - 4.0
    }

    private fun parseTime(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun severityFor(value: String): AlertSeverity = when (value.trim().lowercase()) {
        "minor" -> AlertSeverity.MINOR
        "moderate" -> AlertSeverity.MODERATE
        "severe" -> AlertSeverity.SEVERE
        "extreme" -> AlertSeverity.EXTREME
        else -> AlertSeverity.UNKNOWN
    }

    private fun conditionFor(text: String): WeatherCondition {
        val s = text.lowercase()
        return when {
            "thunder" in s -> WeatherCondition.THUNDERSTORM
            "sleet" in s || "freezing" in s || "wintry" in s || "ice" in s -> WeatherCondition.SLEET
            "snow" in s || "flurries" in s || "blizzard" in s -> WeatherCondition.SNOW
            "heavy rain" in s -> WeatherCondition.HEAVY_RAIN
            "drizzle" in s -> WeatherCondition.DRIZZLE
            "rain" in s || "showers" in s -> WeatherCondition.RAIN
            "fog" in s || "haze" in s || "smoke" in s || "mist" in s -> WeatherCondition.FOG
            "partly" in s -> WeatherCondition.PARTLY_CLOUDY
            "mostly cloudy" in s || "cloudy" in s || "overcast" in s -> WeatherCondition.CLOUDY
            "sunny" in s || "clear" in s || "fair" in s -> WeatherCondition.CLEAR
            else -> WeatherCondition.UNKNOWN
        }
    }

    private companion object {
        const val HOUR_MS = 60 * 60 * 1000L
        const val HOURLY_COUNT = 24
    }
}
