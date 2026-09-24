package com.sameerasw.essentials.weather

import androidx.core.text.util.LocalePreferences
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.weather.model.TemperatureUnit
import com.sameerasw.essentials.weather.model.WeatherCondition
import kotlin.math.roundToInt

object WeatherFormat {
    fun unitFor(setting: String): TemperatureUnit = when (setting) {
        SettingsRepository.WEATHER_UNITS_CELSIUS -> TemperatureUnit.CELSIUS
        SettingsRepository.WEATHER_UNITS_FAHRENHEIT -> TemperatureUnit.FAHRENHEIT
        else -> systemUnit()
    }

    fun systemUnit(): TemperatureUnit =
        if (LocalePreferences.getTemperatureUnit() == LocalePreferences.TemperatureUnit.FAHRENHEIT) {
            TemperatureUnit.FAHRENHEIT
        } else {
            TemperatureUnit.CELSIUS
        }

    fun temperature(celsius: Double, unit: TemperatureUnit): String {
        val value = if (unit == TemperatureUnit.FAHRENHEIT) celsius * 9.0 / 5.0 + 32.0 else celsius
        return "${value.roundToInt()}°"
    }

    fun wind(kph: Double, unit: TemperatureUnit): String =
        if (unit == TemperatureUnit.FAHRENHEIT) "${(kph / 1.609344).roundToInt()} mph" else "${kph.roundToInt()} km/h"

    fun icon(condition: WeatherCondition, isDay: Boolean): Int = when (condition) {
        WeatherCondition.CLEAR -> if (isDay) R.drawable.rounded_sunny_24 else R.drawable.rounded_bedtime_24
        WeatherCondition.PARTLY_CLOUDY -> if (isDay) R.drawable.rounded_partly_cloudy_day_24 else R.drawable.rounded_partly_cloudy_night_24
        WeatherCondition.CLOUDY -> R.drawable.rounded_cloud_24
        WeatherCondition.FOG -> R.drawable.rounded_foggy_24
        WeatherCondition.DRIZZLE -> R.drawable.rounded_rainy_light_24
        WeatherCondition.RAIN -> R.drawable.rounded_rainy_24
        WeatherCondition.HEAVY_RAIN -> R.drawable.rounded_rainy_heavy_24
        WeatherCondition.SLEET -> R.drawable.rounded_rainy_snow_24
        WeatherCondition.SNOW -> R.drawable.rounded_weather_snowy_24
        WeatherCondition.HAIL -> R.drawable.rounded_weather_hail_24
        WeatherCondition.THUNDERSTORM -> R.drawable.rounded_thunderstorm_24
        WeatherCondition.UNKNOWN -> R.drawable.rounded_cloud_24
    }
}
