/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - General
 * File: MapsLinkParser.kt
 * Description: Reads coordinates and a place name from Google Maps links and shared text.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.location.Geocoder
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

object MapsLinkParser {
    data class Place(
        val latitude: Double,
        val longitude: Double,
        val name: String?,
        val isApproximate: Boolean = false,
        val address: String? = null,
    )

    private val pinRegex = Regex("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)")
    private val coordinatesRegex = Regex("(?<![@\\d.-])(-?\\d+\\.\\d+)\\s*,\\s*(-?\\d+\\.\\d+)")
    private val viewportRegex = Regex("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
    private val placePathRegex = Regex("/maps/place/([^/@?]+)")
    private val urlRegex = Regex("https?://\\S+")

    fun isShortLink(text: String): Boolean = text.contains("maps.app.goo.gl") || text.contains("goo.gl/maps")

    fun findUrl(text: String): String? = urlRegex.find(text)?.value

    fun parse(text: String): Place? {
        val match = pinRegex.find(text) ?: coordinatesRegex.find(text) ?: return null
        return placeFrom(match, text)
    }

    private fun placeFrom(
        match: MatchResult,
        text: String,
        isApproximate: Boolean = false,
    ): Place? {
        val latitude = match.groupValues[1].toDoubleOrNull() ?: return null
        val longitude = match.groupValues[2].toDoubleOrNull() ?: return null
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0 || (latitude == 0.0 && longitude == 0.0)) return null
        return Place(latitude, longitude, placeName(text), isApproximate)
    }

    fun placeName(text: String): String? {
        val firstLine =
            text
                .lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() && !urlRegex.containsMatchIn(it) && !coordinatesRegex.matches(it) }
        if (firstLine != null) return firstLine
        val fromPath = placePathRegex.find(text)?.groupValues?.get(1) ?: return null
        return runCatching { URLDecoder.decode(fromPath, "UTF-8") }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    fun resolve(
        context: Context,
        text: String,
    ): Place? {
        parse(text)?.let { return it }
        val link = findUrl(text) ?: text
        val resolved = if (isShortLink(link)) resolveShortLink(link) else link
        parse(resolved)?.let { return it }
        val address = placeName(resolved)
        val located = address?.let { geocode(context, it) }
        if (located != null) return located.copy(name = placeName(text) ?: address.substringBefore(','))
        return viewportRegex.find(resolved)?.let { placeFrom(it, text, isApproximate = true) }
    }

    @Suppress("DEPRECATION")
    private fun geocode(
        context: Context,
        address: String,
    ): Place? {
        if (!Geocoder.isPresent()) return null
        return try {
            Geocoder(context)
                .getFromLocationName(address, 1)
                ?.firstOrNull()
                ?.let { Place(it.latitude, it.longitude, null, isApproximate = true, address = it.getAddressLine(0)) }
        } catch (e: Exception) {
            Log.e("MapsLinkParser", "Geocoding failed", e)
            null
        }
    }

    fun resolveShortLink(shortUrl: String): String =
        try {
            val connection = URL(shortUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.connect()
            val location = connection.getHeaderField("Location")
            connection.disconnect()
            location ?: shortUrl
        } catch (e: Exception) {
            Log.e("MapsLinkParser", "Error resolving URL", e)
            shortUrl
        }

    private const val TIMEOUT_MS = 10_000
}
