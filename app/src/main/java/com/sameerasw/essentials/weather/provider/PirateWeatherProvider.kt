package com.sameerasw.essentials.weather.provider

import com.sameerasw.essentials.weather.model.AlertSeverity
import com.sameerasw.essentials.weather.model.CityResult
import com.sameerasw.essentials.weather.model.WeatherExtras
import com.sameerasw.essentials.weather.model.DailyForecast
import com.sameerasw.essentials.weather.model.HourlyForecast
import com.sameerasw.essentials.weather.model.WeatherAlert
import com.sameerasw.essentials.weather.model.WeatherCondition
import com.sameerasw.essentials.weather.model.WeatherLocation
import com.sameerasw.essentials.weather.model.WeatherSnapshot
import org.json.JSONException
import org.json.JSONObject

class PirateWeatherProvider : WeatherProvider {
    override val id = "pirateweather"
    override val displayName = "Pirate Weather"
    override val requiresApiKey = true
    override val signupUrl = "https://pirate-weather.apiable.io/"

    override suspend fun fetch(location: WeatherLocation, apiKey: String?): WeatherSnapshot {
        val key = apiKey?.trim().orEmpty()
        if (key.isEmpty()) throw WeatherProviderException(WeatherProviderException.Reason.INVALID_KEY)
        val url = "https://api.pirateweather.net/forecast/${ProviderHttp.encode(key)}/${location.latitude},${location.longitude}" +
            "?units=si&exclude=minutely"
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
        val current = json.getJSONObject("currently")
        val hourlyData = json.getJSONObject("hourly").getJSONArray("data")
        val dailyArray = json.getJSONObject("daily").getJSONArray("data")
        val dailyToday = dailyArray.getJSONObject(0)
        fun num(o: JSONObject, name: String): Double? = if (o.has(name) && !o.isNull(name)) o.getDouble(name) else null

        val hourly = buildList {
            for (i in 0 until hourlyData.length()) {
                val h = hourlyData.getJSONObject(i)
                val time = h.getLong("time") * 1000L
                if (time + HOUR_MS <= now) continue
                add(
                    HourlyForecast(
                        timeMillis = time,
                        tempC = h.getDouble("temperature"),
                        condition = conditionFor(h.optString("icon"), h.optDouble("precipIntensity", 0.0)),
                        isDay = !h.optString("icon").endsWith("-night"),
                        chanceOfRain = (h.optDouble("precipProbability", 0.0) * 100).toInt(),
                    ),
                )
            }
        }.take(HOURLY_COUNT)

        val alertArray = json.optJSONArray("alerts")
        val alerts = buildList {
            if (alertArray != null) {
                for (i in 0 until alertArray.length()) {
                    val a = alertArray.getJSONObject(i)
                    val title = a.optString("title")
                    add(
                        WeatherAlert(
                            id = "$id:$title:${a.optLong("time")}".hashCode().toString(),
                            event = title,
                            headline = title,
                            severity = severityFor(a.optString("severity")),
                            description = a.optString("description").trim(),
                            effectiveMillis = a.optLong("time", 0L).takeIf { it > 0 }?.times(1000L),
                            expiresMillis = a.optLong("expires", 0L).takeIf { it > 0 }?.times(1000L),
                        ),
                    )
                }
            }
        }.distinctBy { it.id }

        val icon = current.optString("icon")
        val temp = current.getDouble("temperature")
        return WeatherSnapshot(
            locationName = place?.first.orEmpty(),
            region = place?.second.orEmpty(),
            tempC = temp,
            feelsLikeC = current.optDouble("apparentTemperature", temp),
            condition = conditionFor(icon, current.optDouble("precipIntensity", 0.0)),
            conditionText = current.optString("summary"),
            isDay = !icon.endsWith("-night"),
            humidity = (current.optDouble("humidity", 0.0) * 100).toInt(),
            windKph = current.optDouble("windSpeed", 0.0) * 3.6,
            chanceOfRain = (dailyToday.optDouble("precipProbability", 0.0) * 100).toInt(),
            highC = dailyToday.optDouble("temperatureHigh", temp),
            lowC = dailyToday.optDouble("temperatureLow", temp),
            hourly = hourly,
            alerts = alerts,
            updatedAt = now,
            providerId = id,
            extras = WeatherExtras(
                pressureHpa = num(current, "pressure"),
                visibilityKm = num(current, "visibility"),
                dewPointC = num(current, "dewPoint"),
                cloudCover = num(current, "cloudCover")?.times(100)?.toInt(),
                uvIndex = num(current, "uvIndex"),
                windGustKph = num(current, "windGust")?.times(3.6),
                windDirectionDeg = num(current, "windBearing"),
                precipitationMm = num(current, "precipIntensity"),
                sunriseMillis = num(dailyToday, "sunriseTime")?.toLong()?.times(1000L),
                sunsetMillis = num(dailyToday, "sunsetTime")?.toLong()?.times(1000L),
            ),
            daily = (0 until dailyArray.length()).map { i ->
                val d = dailyArray.getJSONObject(i)
                DailyForecast(
                    dayMillis = d.getLong("time") * 1000L,
                    highC = d.optDouble("temperatureHigh", d.optDouble("temperatureMax", temp)),
                    lowC = d.optDouble("temperatureLow", d.optDouble("temperatureMin", temp)),
                    condition = conditionFor(d.optString("icon"), d.optDouble("precipIntensity", 0.0)),
                    chanceOfRain = (d.optDouble("precipProbability", 0.0) * 100).toInt(),
                )
            },
        )
    }

    private fun severityFor(value: String): AlertSeverity = when (value.trim().lowercase()) {
        "minor" -> AlertSeverity.MINOR
        "moderate" -> AlertSeverity.MODERATE
        "severe" -> AlertSeverity.SEVERE
        "extreme" -> AlertSeverity.EXTREME
        else -> AlertSeverity.UNKNOWN
    }

    private fun conditionFor(icon: String, intensityMmH: Double): WeatherCondition = when {
        icon.startsWith("clear") -> WeatherCondition.CLEAR
        icon.startsWith("partly-cloudy") -> WeatherCondition.PARTLY_CLOUDY
        icon == "cloudy" -> WeatherCondition.CLOUDY
        icon == "fog" -> WeatherCondition.FOG
        icon == "rain" -> when {
            intensityMmH >= 4.0 -> WeatherCondition.HEAVY_RAIN
            intensityMmH in 0.0..0.5 -> WeatherCondition.DRIZZLE
            else -> WeatherCondition.RAIN
        }
        icon == "snow" -> WeatherCondition.SNOW
        icon == "sleet" -> WeatherCondition.SLEET
        icon == "hail" -> WeatherCondition.HAIL
        icon == "thunderstorm" -> WeatherCondition.THUNDERSTORM
        icon == "wind" -> WeatherCondition.CLOUDY
        else -> WeatherCondition.UNKNOWN
    }

    private companion object {
        const val HOUR_MS = 60 * 60 * 1000L
        const val HOURLY_COUNT = 24
    }
}
