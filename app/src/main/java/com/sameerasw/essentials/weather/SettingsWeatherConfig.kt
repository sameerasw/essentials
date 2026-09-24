package com.sameerasw.essentials.weather

import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.weather.model.WeatherLocation
import com.sameerasw.essentials.weather.provider.WeatherProviders

class SettingsWeatherConfig(private val settings: SettingsRepository) : WeatherConfig {
    override val providerId: String get() = WeatherProviders.byId(settings.getWeatherProvider()).id
    override val apiKey: String? get() = settings.getWeatherApiKey()
    override val locationMode: WeatherLocationMode
        get() = if (settings.getWeatherLocationMode() == "manual") WeatherLocationMode.MANUAL else WeatherLocationMode.DEVICE
    override val manualLocation: WeatherLocation?
        get() = settings.getWeatherManualLocation()?.let { (lat, lon, name) -> WeatherLocation(lat, lon, name) }
    override val refreshIntervalMinutes: Int get() = settings.getWeatherRefreshMinutes()
}
