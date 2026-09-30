package com.sameerasw.essentials.weather.provider

import com.sameerasw.essentials.weather.model.CityResult
import com.sameerasw.essentials.weather.model.WeatherExtras
import com.sameerasw.essentials.weather.model.HourlyForecast
import com.sameerasw.essentials.weather.model.WeatherCondition
import com.sameerasw.essentials.weather.model.WeatherLocation
import com.sameerasw.essentials.weather.model.WeatherSnapshot
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class OpenWeatherMapProvider : WeatherProvider {
    override val id = "openweathermap"
    override val displayName = "OpenWeatherMap"
    override val requiresApiKey = true
    override val signupUrl = "https://home.openweathermap.org/users/sign_up"

    override suspend fun fetch(location: WeatherLocation, apiKey: String?): WeatherSnapshot {
        val key = apiKey?.trim().orEmpty()
        if (key.isEmpty()) throw WeatherProviderException(WeatherProviderException.Reason.INVALID_KEY)
        val common = "lat=${location.latitude}&lon=${location.longitude}&units=metric&appid=${ProviderHttp.encode(key)}"
        val current = JSONObject(ProviderHttp.get("$BASE_URL/data/2.5/weather?$common"))
        val forecast = JSONObject(ProviderHttp.get("$BASE_URL/data/2.5/forecast?$common&cnt=10"))
        return try {
            parse(current, forecast, location)
        } catch (e: JSONException) {
            throw WeatherProviderException(WeatherProviderException.Reason.BAD_RESPONSE, e.message)
        }
    }

    override suspend fun searchCities(query: String, apiKey: String?): List<CityResult> {
        val key = apiKey?.trim().orEmpty()
        if (key.isEmpty()) throw WeatherProviderException(WeatherProviderException.Reason.INVALID_KEY)
        if (query.isBlank()) return emptyList()
        val array = try {
            JSONArray(ProviderHttp.get("$BASE_URL/geo/1.0/direct?q=${ProviderHttp.encode(query.trim())}&limit=8&appid=${ProviderHttp.encode(key)}"))
        } catch (e: JSONException) {
            throw WeatherProviderException(WeatherProviderException.Reason.BAD_RESPONSE, e.message)
        }
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            CityResult(
                name = o.optString("name"),
                region = o.optString("state"),
                country = o.optString("country"),
                latitude = o.optDouble("lat"),
                longitude = o.optDouble("lon"),
            )
        }
    }

    private fun parse(current: JSONObject, forecast: JSONObject, requested: WeatherLocation): WeatherSnapshot {
        val now = System.currentTimeMillis()
        val main = current.getJSONObject("main")
        val weather = current.getJSONArray("weather").getJSONObject(0)
        val sys = current.optJSONObject("sys")
        val dt = current.optLong("dt", now / 1000L)
        val sunrise = sys?.optLong("sunrise", 0L) ?: 0L
        val sunset = sys?.optLong("sunset", 0L) ?: 0L
        val isDay = if (sunrise > 0L && sunset > 0L) dt in sunrise until sunset else true
        val temp = main.getDouble("temp")

        val list = forecast.getJSONArray("list")
        val hourly = mutableListOf<HourlyForecast>()
        val temps = mutableListOf(temp)
        var rainChance = 0
        for (i in 0 until list.length()) {
            val entry = list.getJSONObject(i)
            val time = entry.getLong("dt") * 1000L
            val entryMain = entry.getJSONObject("main")
            temps += entryMain.optDouble("temp_max", entryMain.getDouble("temp"))
            temps += entryMain.optDouble("temp_min", entryMain.getDouble("temp"))
            val pop = (entry.optDouble("pop", 0.0) * 100).toInt()
            if (time + THREE_HOURS_MS > now && hourly.size < HOURLY_COUNT) {
                hourly += HourlyForecast(
                    timeMillis = time,
                    tempC = entryMain.getDouble("temp"),
                    condition = conditionFor(entry.getJSONArray("weather").getJSONObject(0).optInt("id")),
                    isDay = entry.optJSONObject("sys")?.optString("pod") != "n",
                    chanceOfRain = pop,
                )
            }
            if (i < 4) rainChance = maxOf(rainChance, pop)
        }

        val description = weather.optString("description")
        return WeatherSnapshot(
            locationName = requested.name ?: current.optString("name"),
            region = sys?.optString("country").orEmpty(),
            tempC = temp,
            feelsLikeC = main.optDouble("feels_like", temp),
            condition = conditionFor(weather.optInt("id")),
            conditionText = description.replaceFirstChar { it.titlecase(Locale.getDefault()) },
            isDay = isDay,
            humidity = main.optInt("humidity"),
            windKph = current.optJSONObject("wind")?.optDouble("speed", 0.0)?.times(3.6) ?: 0.0,
            chanceOfRain = rainChance,
            highC = temps.max(),
            lowC = temps.min(),
            hourly = hourly,
            alerts = emptyList(),
            updatedAt = now,
            providerId = id,
            extras = WeatherExtras(
                pressureHpa = main.optDouble("pressure").takeUnless { it.isNaN() },
                visibilityKm = current.optDouble("visibility").takeUnless { it.isNaN() }?.div(1000.0),
                cloudCover = current.optJSONObject("clouds")?.optInt("all", -1)?.takeIf { it >= 0 },
                windGustKph = current.optJSONObject("wind")?.optDouble("gust")?.takeUnless { it.isNaN() }?.times(3.6),
                windDirectionDeg = current.optJSONObject("wind")?.optDouble("deg")?.takeUnless { it.isNaN() },
                sunriseMillis = sunrise.takeIf { it > 0 }?.times(1000L),
                sunsetMillis = sunset.takeIf { it > 0 }?.times(1000L),
            ),
        )
    }

    // https://openweathermap.org/weather-conditions
    private fun conditionFor(code: Int): WeatherCondition = when (code) {
        in 200..232 -> WeatherCondition.THUNDERSTORM
        in 300..321 -> WeatherCondition.DRIZZLE
        500, 501, 520, 521 -> WeatherCondition.RAIN
        502, 503, 504, 522, 531 -> WeatherCondition.HEAVY_RAIN
        511, in 611..616 -> WeatherCondition.SLEET
        in 600..602, 620, 621, 622 -> WeatherCondition.SNOW
        701, 711, 721, 741 -> WeatherCondition.FOG
        800 -> WeatherCondition.CLEAR
        801, 802 -> WeatherCondition.PARTLY_CLOUDY
        803, 804 -> WeatherCondition.CLOUDY
        else -> WeatherCondition.UNKNOWN
    }

    private companion object {
        const val BASE_URL = "https://api.openweathermap.org"
        const val THREE_HOURS_MS = 3 * 60 * 60 * 1000L
        const val HOURLY_COUNT = 24
    }
}
