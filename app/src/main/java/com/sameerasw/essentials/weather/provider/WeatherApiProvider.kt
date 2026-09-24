package com.sameerasw.essentials.weather.provider

import com.sameerasw.essentials.weather.model.AlertSeverity
import com.sameerasw.essentials.weather.model.CityResult
import com.sameerasw.essentials.weather.model.HourlyForecast
import com.sameerasw.essentials.weather.model.WeatherAlert
import com.sameerasw.essentials.weather.model.WeatherCondition
import com.sameerasw.essentials.weather.model.WeatherLocation
import com.sameerasw.essentials.weather.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

class WeatherApiProvider : WeatherProvider {
    override val id = "weatherapi"
    override val displayName = "WeatherAPI.com"
    override val requiresApiKey = true
    override val signupUrl = "https://www.weatherapi.com/signup.aspx"

    override suspend fun fetch(location: WeatherLocation, apiKey: String?): WeatherSnapshot {
        val key = apiKey?.trim().orEmpty()
        if (key.isEmpty()) throw WeatherProviderException(WeatherProviderException.Reason.INVALID_KEY)
        val json = JSONObject(get("forecast.json", "key=${encode(key)}&q=${encode(location.query)}&days=2&aqi=no&alerts=yes"))
        return try {
            parseForecast(json, location)
        } catch (e: JSONException) {
            throw WeatherProviderException(WeatherProviderException.Reason.BAD_RESPONSE, e.message)
        }
    }

    override suspend fun searchCities(query: String, apiKey: String?): List<CityResult> {
        val key = apiKey?.trim().orEmpty()
        if (key.isEmpty()) throw WeatherProviderException(WeatherProviderException.Reason.INVALID_KEY)
        if (query.isBlank()) return emptyList()
        val array = JSONArray(get("search.json", "key=${encode(key)}&q=${encode(query.trim())}"))
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            CityResult(
                name = o.optString("name"),
                region = o.optString("region"),
                country = o.optString("country"),
                latitude = o.optDouble("lat"),
                longitude = o.optDouble("lon"),
            )
        }
    }

    private suspend fun get(endpoint: String, query: String): String = withContext(Dispatchers.IO) {
        val connection = try {
            URL("$BASE_URL/$endpoint?$query").openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw WeatherProviderException(WeatherProviderException.Reason.NETWORK, e.message)
        }
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                throw WeatherProviderException(WeatherProviderException.Reason.INVALID_KEY)
            }
            if (code !in 200..299) {
                throw WeatherProviderException(WeatherProviderException.Reason.BAD_RESPONSE, "HTTP $code")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw WeatherProviderException(WeatherProviderException.Reason.NETWORK, e.message)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseForecast(json: JSONObject, requested: WeatherLocation): WeatherSnapshot {
        val location = json.getJSONObject("location")
        val current = json.getJSONObject("current")
        val days = json.getJSONObject("forecast").getJSONArray("forecastday")
        val today = days.getJSONObject(0).getJSONObject("day")
        val now = System.currentTimeMillis()

        val hourly = buildList {
            for (d in 0 until days.length()) {
                val hours = days.getJSONObject(d).getJSONArray("hour")
                for (h in 0 until hours.length()) {
                    val hour = hours.getJSONObject(h)
                    val time = hour.getLong("time_epoch") * 1000L
                    if (time + HOUR_MS <= now) continue
                    add(
                        HourlyForecast(
                            timeMillis = time,
                            tempC = hour.getDouble("temp_c"),
                            condition = conditionFor(hour.getJSONObject("condition").optInt("code")),
                            isDay = hour.optInt("is_day", 1) == 1,
                            chanceOfRain = hour.optInt("chance_of_rain"),
                        ),
                    )
                }
            }
        }.take(HOURLY_COUNT)

        val alertArray = json.optJSONObject("alerts")?.optJSONArray("alert") ?: JSONArray()
        val alerts = (0 until alertArray.length()).map { i ->
            val a = alertArray.getJSONObject(i)
            val event = a.optString("event").ifBlank { a.optString("headline") }
            val effective = parseTime(a.optString("effective"))
            WeatherAlert(
                id = "$id:${a.optString("headline")}:${a.optString("effective")}:$event".hashCode().toString(),
                event = event,
                headline = a.optString("headline").ifBlank { event },
                severity = severityFor(a.optString("severity")),
                description = a.optString("desc").trim(),
                effectiveMillis = effective,
                expiresMillis = parseTime(a.optString("expires")),
            )
        }.distinctBy { it.id }

        val condition = current.getJSONObject("condition")
        return WeatherSnapshot(
            locationName = requested.name ?: location.optString("name"),
            region = location.optString("region").ifBlank { location.optString("country") },
            tempC = current.getDouble("temp_c"),
            feelsLikeC = current.optDouble("feelslike_c", current.getDouble("temp_c")),
            condition = conditionFor(condition.optInt("code")),
            conditionText = condition.optString("text"),
            isDay = current.optInt("is_day", 1) == 1,
            humidity = current.optInt("humidity"),
            windKph = current.optDouble("wind_kph"),
            chanceOfRain = today.optInt("daily_chance_of_rain"),
            highC = today.getDouble("maxtemp_c"),
            lowC = today.getDouble("mintemp_c"),
            hourly = hourly,
            alerts = alerts,
            updatedAt = now,
            providerId = id,
        )
    }

    private fun parseTime(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun severityFor(value: String?): AlertSeverity = when (value?.trim()?.lowercase()) {
        "minor" -> AlertSeverity.MINOR
        "moderate" -> AlertSeverity.MODERATE
        "severe" -> AlertSeverity.SEVERE
        "extreme" -> AlertSeverity.EXTREME
        else -> AlertSeverity.UNKNOWN
    }

    // https://www.weatherapi.com/docs/weather_conditions.json
    private fun conditionFor(code: Int): WeatherCondition = when (code) {
        1000 -> WeatherCondition.CLEAR
        1003 -> WeatherCondition.PARTLY_CLOUDY
        1006, 1009 -> WeatherCondition.CLOUDY
        1030, 1135, 1147 -> WeatherCondition.FOG
        1063, 1150, 1153, 1180, 1183, 1240 -> WeatherCondition.DRIZZLE
        1186, 1189, 1243 -> WeatherCondition.RAIN
        1192, 1195, 1246 -> WeatherCondition.HEAVY_RAIN
        1069, 1072, 1168, 1171, 1198, 1201, 1204, 1207, 1249, 1252 -> WeatherCondition.SLEET
        1066, 1114, 1117, 1210, 1213, 1216, 1219, 1222, 1225, 1255, 1258 -> WeatherCondition.SNOW
        1237, 1261, 1264 -> WeatherCondition.HAIL
        1087, 1273, 1276, 1279, 1282 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.UNKNOWN
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val BASE_URL = "https://api.weatherapi.com/v1"
        const val TIMEOUT_MS = 15_000
        const val HOUR_MS = 60 * 60 * 1000L
        const val HOURLY_COUNT = 8
    }
}
