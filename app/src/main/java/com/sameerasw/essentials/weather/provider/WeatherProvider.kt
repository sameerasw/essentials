package com.sameerasw.essentials.weather.provider

import com.sameerasw.essentials.weather.model.CityResult
import com.sameerasw.essentials.weather.model.WeatherLocation
import com.sameerasw.essentials.weather.model.WeatherSnapshot

interface WeatherProvider {
    val id: String
    val displayName: String
    val requiresApiKey: Boolean
    val signupUrl: String?

    suspend fun fetch(location: WeatherLocation, apiKey: String?): WeatherSnapshot

    suspend fun searchCities(query: String, apiKey: String?): List<CityResult>
}

class WeatherProviderException(val reason: Reason, message: String? = null) : Exception(message) {
    enum class Reason { INVALID_KEY, NETWORK, BAD_RESPONSE }
}

object WeatherProviders {
    val all: List<WeatherProvider> = listOf(WeatherApiProvider())

    val default: WeatherProvider get() = all.first()

    fun byId(id: String?): WeatherProvider = all.firstOrNull { it.id == id } ?: default
}
