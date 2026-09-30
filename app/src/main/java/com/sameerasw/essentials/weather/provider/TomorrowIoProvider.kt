package com.sameerasw.essentials.weather.provider

import com.sameerasw.essentials.weather.model.CityResult
import com.sameerasw.essentials.weather.model.WeatherExtras
import com.sameerasw.essentials.weather.model.DailyForecast
import com.sameerasw.essentials.weather.model.HourlyForecast
import com.sameerasw.essentials.weather.model.WeatherCondition
import com.sameerasw.essentials.weather.model.WeatherLocation
import com.sameerasw.essentials.weather.model.WeatherSnapshot
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant

class TomorrowIoProvider : WeatherProvider {
    override val id = "tomorrowio"
    override val displayName = "Tomorrow.io"
    override val requiresApiKey = true
    override val signupUrl = "https://app.tomorrow.io/signup"

    override suspend fun fetch(location: WeatherLocation, apiKey: String?): WeatherSnapshot {
        val key = apiKey?.trim().orEmpty()
        if (key.isEmpty()) throw WeatherProviderException(WeatherProviderException.Reason.INVALID_KEY)
        val url = "https://api.tomorrow.io/v4/weather/forecast?location=${location.latitude},${location.longitude}" +
            "&timesteps=1h,1d&units=metric&apikey=${ProviderHttp.encode(key)}"
        val json = JSONObject(ProviderHttp.get(url, mapOf("Accept" to "application/json")))
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
        val timelines = json.getJSONObject("timelines")
        val hours = timelines.getJSONArray("hourly")
        val days = timelines.getJSONArray("daily")

        val suns = (0 until days.length()).mapNotNull { i ->
            val v = days.getJSONObject(i).getJSONObject("values")
            val rise = v.optString("sunriseTime").takeIf { it.isNotBlank() }?.let { Instant.parse(it).toEpochMilli() }
            val set = v.optString("sunsetTime").takeIf { it.isNotBlank() }?.let { Instant.parse(it).toEpochMilli() }
            if (rise != null && set != null) rise to set else null
        }
        fun isDay(time: Long) = suns.isEmpty() || suns.any { (rise, set) -> time in rise until set }

        val currentValues = hours.getJSONObject(0).getJSONObject("values")
        val forecast = buildList {
            for (i in 0 until hours.length()) {
                val h = hours.getJSONObject(i)
                val time = Instant.parse(h.getString("time")).toEpochMilli()
                if (time + HOUR_MS <= now) continue
                val v = h.getJSONObject("values")
                add(
                    HourlyForecast(
                        timeMillis = time,
                        tempC = v.getDouble("temperature"),
                        condition = conditionFor(v.optInt("weatherCode")),
                        isDay = isDay(time),
                        chanceOfRain = v.optDouble("precipitationProbability", 0.0).toInt(),
                    ),
                )
            }
        }.take(HOURLY_COUNT)

        val today = days.getJSONObject(0).getJSONObject("values")
        fun num(o: JSONObject, name: String): Double? = if (o.has(name) && !o.isNull(name)) o.getDouble(name) else null
        val temp = currentValues.getDouble("temperature")
        val code = currentValues.optInt("weatherCode")
        return WeatherSnapshot(
            locationName = place?.first.orEmpty(),
            region = place?.second.orEmpty(),
            tempC = temp,
            feelsLikeC = currentValues.optDouble("temperatureApparent", temp),
            condition = conditionFor(code),
            conditionText = textFor(code),
            isDay = isDay(now),
            humidity = currentValues.optDouble("humidity", 0.0).toInt(),
            windKph = currentValues.optDouble("windSpeed", 0.0) * 3.6,
            chanceOfRain = today.optDouble("precipitationProbabilityMax", today.optDouble("precipitationProbabilityAvg", 0.0)).toInt(),
            highC = today.optDouble("temperatureMax", temp),
            lowC = today.optDouble("temperatureMin", temp),
            hourly = forecast,
            alerts = emptyList(),
            updatedAt = now,
            providerId = id,
            extras = WeatherExtras(
                pressureHpa = num(currentValues, "pressureSurfaceLevel"),
                visibilityKm = num(currentValues, "visibility"),
                dewPointC = num(currentValues, "dewPoint"),
                cloudCover = num(currentValues, "cloudCover")?.toInt(),
                uvIndex = num(currentValues, "uvIndex"),
                windGustKph = num(currentValues, "windGust")?.times(3.6),
                windDirectionDeg = num(currentValues, "windDirection"),
                precipitationMm = num(currentValues, "rainIntensity"),
                sunriseMillis = suns.firstOrNull()?.first,
                sunsetMillis = suns.firstOrNull()?.second,
            ),
            daily = (0 until days.length()).map { i ->
                val d = days.getJSONObject(i)
                val v = d.getJSONObject("values")
                DailyForecast(
                    dayMillis = Instant.parse(d.getString("time")).toEpochMilli(),
                    highC = v.optDouble("temperatureMax", temp),
                    lowC = v.optDouble("temperatureMin", temp),
                    condition = conditionFor(v.optInt("weatherCodeMax", v.optInt("weatherCode"))),
                    chanceOfRain = v.optDouble("precipitationProbabilityMax", 0.0).toInt(),
                )
            },
        )
    }

    // https://docs.tomorrow.io/reference/data-layers-weather-codes
    private fun conditionFor(code: Int): WeatherCondition = when (code) {
        1000, 1100 -> WeatherCondition.CLEAR
        1101 -> WeatherCondition.PARTLY_CLOUDY
        1102, 1001 -> WeatherCondition.CLOUDY
        2000, 2100 -> WeatherCondition.FOG
        4000, 4200 -> WeatherCondition.DRIZZLE
        4001 -> WeatherCondition.RAIN
        4201 -> WeatherCondition.HEAVY_RAIN
        5000, 5001, 5100, 5101 -> WeatherCondition.SNOW
        6000, 6001, 6200, 6201, 7000, 7101, 7102 -> WeatherCondition.SLEET
        8000 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.UNKNOWN
    }

    private fun textFor(code: Int): String = when (code) {
        1000 -> "Clear"
        1100 -> "Mostly clear"
        1101 -> "Partly cloudy"
        1102 -> "Mostly cloudy"
        1001 -> "Cloudy"
        2000 -> "Fog"
        2100 -> "Light fog"
        4000 -> "Drizzle"
        4001 -> "Rain"
        4200 -> "Light rain"
        4201 -> "Heavy rain"
        5000 -> "Snow"
        5001 -> "Flurries"
        5100 -> "Light snow"
        5101 -> "Heavy snow"
        6000, 6200 -> "Freezing drizzle"
        6001, 6201 -> "Freezing rain"
        7000, 7101, 7102 -> "Ice pellets"
        8000 -> "Thunderstorm"
        else -> "Unknown"
    }

    private companion object {
        const val HOUR_MS = 60 * 60 * 1000L
        const val HOURLY_COUNT = 24
    }
}
