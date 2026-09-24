package com.sameerasw.essentials.weather.model

import androidx.annotation.Keep
import java.util.Locale

@Keep
enum class WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    HEAVY_RAIN,
    SLEET,
    SNOW,
    HAIL,
    THUNDERSTORM,
    UNKNOWN,
}

@Keep
enum class AlertSeverity {
    MINOR,
    MODERATE,
    SEVERE,
    EXTREME,
    UNKNOWN,
    ;

    val isSevere: Boolean get() = this == SEVERE || this == EXTREME
}

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

@Keep
data class WeatherLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
) {
    // ~1km precision is plenty for weather and keeps the exact position off the wire.
    val query: String get() = "%.2f,%.2f".format(Locale.US, latitude, longitude)
}

data class CityResult(
    val name: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
) {
    val label: String get() = listOf(name, region, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

@Keep
data class HourlyForecast(
    val timeMillis: Long,
    val tempC: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val chanceOfRain: Int,
)

@Keep
data class WeatherAlert(
    val id: String,
    val event: String,
    val headline: String,
    val severity: AlertSeverity,
    val description: String,
    val effectiveMillis: Long?,
    val expiresMillis: Long?,
) {
    fun isActive(now: Long): Boolean = expiresMillis == null || expiresMillis > now
}

@Keep
data class WeatherSnapshot(
    val locationName: String,
    val region: String,
    val tempC: Double,
    val feelsLikeC: Double,
    val condition: WeatherCondition,
    val conditionText: String,
    val isDay: Boolean,
    val humidity: Int,
    val windKph: Double,
    val chanceOfRain: Int,
    val highC: Double,
    val lowC: Double,
    val hourly: List<HourlyForecast>,
    val alerts: List<WeatherAlert>,
    val updatedAt: Long,
    val providerId: String,
) {
    fun activeAlerts(now: Long = System.currentTimeMillis()): List<WeatherAlert> = alerts.filter { it.isActive(now) }
}

sealed interface WeatherError {
    data object MissingApiKey : WeatherError
    data object InvalidApiKey : WeatherError
    data object NoLocation : WeatherError
    data object LocationPermission : WeatherError
    data object Network : WeatherError
    data class Unknown(val message: String?) : WeatherError
}

data class WeatherState(
    val snapshot: WeatherSnapshot? = null,
    val loading: Boolean = false,
    val error: WeatherError? = null,
)
