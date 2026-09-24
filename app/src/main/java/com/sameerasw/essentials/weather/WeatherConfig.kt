package com.sameerasw.essentials.weather

import com.sameerasw.essentials.weather.model.WeatherLocation

enum class WeatherLocationMode { DEVICE, MANUAL }

interface WeatherConfig {
    val providerId: String
    val apiKey: String?
    val locationMode: WeatherLocationMode
    val manualLocation: WeatherLocation?
    val refreshIntervalMinutes: Int
}
