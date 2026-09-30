package com.sameerasw.essentials.weather.provider

import com.sameerasw.essentials.weather.model.CityResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal object ProviderHttp {
    const val USER_AGENT = "Essentials/1.0 github.com/sameerasw/essentials"
    private const val TIMEOUT_MS = 15_000

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw WeatherProviderException(WeatherProviderException.Reason.NETWORK, e.message)
        }
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
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

    suspend fun searchCities(query: String): List<CityResult> {
        if (query.isBlank()) return emptyList()
        val json = try {
            JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=${encode(query.trim())}&count=8&language=en&format=json"))
        } catch (e: JSONException) {
            throw WeatherProviderException(WeatherProviderException.Reason.BAD_RESPONSE, e.message)
        }
        val results = json.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val o = results.getJSONObject(i)
            CityResult(
                name = o.optString("name"),
                region = o.optString("admin1"),
                country = o.optString("country"),
                latitude = o.optDouble("latitude"),
                longitude = o.optDouble("longitude"),
            )
        }
    }

    // Best effort; providers without their own geocoder fall back to an empty name when this fails.
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Pair<String, String>? = try {
        val json = JSONObject(
            get("https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$latitude&longitude=$longitude&localityLanguage=en"),
        )
        val name = json.optString("city").ifBlank { json.optString("locality") }
        val region = json.optString("principalSubdivision").ifBlank { json.optString("countryName") }
        if (name.isBlank()) null else name to region
    } catch (_: Exception) {
        null
    }
}
