/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Data & Repository Layer
 * File: HelpMediaRepository.kt
 * Description: Data repository for fetching and caching online help media mappings.
 */

package com.sameerasw.essentials.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class FeatureHelpMedia(
    val type: String, // "video", "gif", "image"
    val url: String,
)

class HelpMediaRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun getHelpMediaForFeature(featureId: String): FeatureHelpMedia? {
        val mapping = getHelpMediaMapping()
        return mapping[featureId]
    }

    private suspend fun getHelpMediaMapping(): Map<String, FeatureHelpMedia> =
        withContext(Dispatchers.IO) {
            val cachedJson = prefs.getString(KEY_MEDIA_MAPPING_CACHE, null)
            val lastFetchTime = prefs.getLong(KEY_MEDIA_MAPPING_LAST_FETCH, 0L)
            val now = System.currentTimeMillis()

            val isCacheStale = (now - lastFetchTime) > CACHE_EXPIRATION_MS

            if (!cachedJson.isNullOrEmpty() && !isCacheStale) {
                try {
                    val type = object : TypeToken<Map<String, FeatureHelpMedia>>() {}.type
                    val cachedMap: Map<String, FeatureHelpMedia>? = gson.fromJson(cachedJson, type)
                    if (cachedMap != null) {
                        return@withContext cachedMap
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fetch remote mapping
            try {
                val url = URL(MAPPING_URL)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val type = object : TypeToken<Map<String, FeatureHelpMedia>>() {}.type
                    val freshMap: Map<String, FeatureHelpMedia> = gson.fromJson(response, type)

                    prefs.edit()
                        .putString(KEY_MEDIA_MAPPING_CACHE, response)
                        .putLong(KEY_MEDIA_MAPPING_LAST_FETCH, now)
                        .apply()

                    return@withContext freshMap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Fallback to cache if available
            if (!cachedJson.isNullOrEmpty()) {
                try {
                    val type = object : TypeToken<Map<String, FeatureHelpMedia>>() {}.type
                    val cachedMap: Map<String, FeatureHelpMedia>? = gson.fromJson(cachedJson, type)
                    if (cachedMap != null) {
                        return@withContext cachedMap
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            emptyMap()
        }

    companion object {
        private const val PREFS_NAME = "help_media_prefs"
        private const val KEY_MEDIA_MAPPING_CACHE = "media_mapping_cache"
        private const val KEY_MEDIA_MAPPING_LAST_FETCH = "media_mapping_last_fetch"
        private const val CACHE_EXPIRATION_MS = 1000 * 60 * 60 // 1 hour
        private const val MAPPING_URL = "https://sameerasw.com/essentials/help/media-mapping.json"

        @Volatile
        private var instance: HelpMediaRepository? = null

        fun getInstance(context: Context): HelpMediaRepository {
            return instance ?: synchronized(this) {
                instance ?: HelpMediaRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
