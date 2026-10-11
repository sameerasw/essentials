/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Data & Repository Layer
 * File: WallpaperStagingRepository.kt
 * Description: Repository for staging tomorrow's wallpaper and synchronizing with GitHub & Unsplash.
 */

package com.sameerasw.essentials.data.repository

import com.google.gson.Gson
import com.sameerasw.essentials.domain.model.WallpaperInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64

class WallpaperStagingRepository(
    private val settingsRepository: SettingsRepository,
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val collectionId = "LqO9knU9z2A"

    suspend fun fetchStagedWallpaper(): WallpaperInfo? =
        withContext(Dispatchers.IO) {
            val urls =
                listOf(
                    "https://sameerasw.com/unsplash-next.json",
                    "https://raw.githubusercontent.com/sameerasw/sameerasw.com/main/public/unsplash-next.json",
                )
            for (urlString in urls) {
                try {
                    val request =
                        Request.Builder()
                            .url(urlString)
                            .header("Cache-Control", "no-cache, no-store, must-revalidate")
                            .header("Pragma", "no-cache")
                            .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) continue

                    val body = response.body?.string() ?: continue
                    if (!body.trim().startsWith("{")) continue

                    val rawMap = gson.fromJson(body, Map::class.java) as? Map<*, *> ?: continue
                    val mobileMap = rawMap["mobile"] as? Map<*, *> ?: continue

                    val id = mobileMap["id"] as? String ?: continue
                    if (id.isBlank()) continue

                    val urlMobile = mobileMap["url"] as? String ?: ""
                    val urlFull = mobileMap["url_full"] as? String ?: ""
                    val author = mobileMap["author"] as? Map<*, *>
                    val authorName = author?.get("name") as? String ?: ""
                    val authorUsername = author?.get("username") as? String ?: ""
                    val authorLink = author?.get("link") as? String ?: ""
                    val link = mobileMap["link"] as? String ?: ""
                    val updatedAt = mobileMap["updatedAt"] as? String ?: ""

                    return@withContext WallpaperInfo(
                        id = id,
                        url = urlMobile,
                        urlMobile = urlMobile,
                        urlFull = urlFull,
                        authorName = authorName,
                        authorUsername = authorUsername,
                        authorLink = authorLink,
                        photoLink = link,
                        updatedAt = updatedAt,
                    )
                } catch (e: Exception) {
                    // Continue to next URL fallback
                }
            }
            null
        }

    suspend fun fetchMobileHistory(): Set<String> =
        withContext(Dispatchers.IO) {
            val urls =
                listOf(
                    "https://sameerasw.com/unsplash-mobile-history.json",
                    "https://raw.githubusercontent.com/sameerasw/sameerasw.com/main/public/unsplash-mobile-history.json",
                    "https://raw.githubusercontent.com/sameerasw/sameerasw.com/main/.github/scripts/unsplash-mobile-history.json",
                )
            for (urlString in urls) {
                try {
                    val request =
                        Request.Builder()
                            .url(urlString)
                            .header("Cache-Control", "no-cache, no-store, must-revalidate")
                            .header("Pragma", "no-cache")
                            .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) continue

                    val body = response.body?.string() ?: continue
                    if (!body.trim().startsWith("[")) continue

                    val list = gson.fromJson(body, List::class.java) as? List<*> ?: continue
                    val result = list.filterIsInstance<String>().toSet()
                    if (result.isNotEmpty()) {
                        return@withContext result
                    }
                } catch (e: Exception) {
                    // Continue to next URL fallback
                }
            }
            emptySet()
        }

    suspend fun fetchCollectionTotalPages(accessKey: String): Int =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.unsplash.com/collections/$collectionId"
                val request =
                    Request.Builder()
                        .url(url)
                        .header("Authorization", "Client-ID $accessKey")
                        .header("Accept-Version", "v1")
                        .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext 1

                val body = response.body?.string() ?: return@withContext 1
                val collection = gson.fromJson(body, Map::class.java) as? Map<*, *> ?: return@withContext 1
                val totalPhotos = (collection["total_photos"] as? Number)?.toInt() ?: 0
                val totalPages = Math.ceil(totalPhotos / 30.0).toInt().coerceAtLeast(1)
                totalPages
            } catch (e: Exception) {
                e.printStackTrace()
                1
            }
        }

    suspend fun fetchCollectionPhotos(accessKey: String, page: Int = 1): List<WallpaperInfo> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.unsplash.com/collections/$collectionId/photos?per_page=30&page=$page"
                val request =
                    Request.Builder()
                        .url(url)
                        .header("Authorization", "Client-ID $accessKey")
                        .header("Accept-Version", "v1")
                        .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) return@withContext emptyList()

                val body = response.body?.string() ?: return@withContext emptyList()
                val photos = gson.fromJson(body, List::class.java) as? List<*> ?: return@withContext emptyList()

                photos.filterIsInstance<Map<*, *>>().mapNotNull { item ->
                    val id = item["id"] as? String ?: return@mapNotNull null
                    val width = (item["width"] as? Number)?.toInt() ?: 0
                    val height = (item["height"] as? Number)?.toInt() ?: 0

                    // Only select portrait (or square) photos for mobile wallpaper, identical to daily-unsplash.js
                    if (height < width) return@mapNotNull null

                    val urls = item["urls"] as? Map<*, *>
                    val rawUrl = urls?.get("raw") as? String ?: return@mapNotNull null
                    val fullUrl = urls?.get("full") as? String ?: rawUrl

                    val user = item["user"] as? Map<*, *>
                    val userName = user?.get("name") as? String ?: ""
                    val userUsername = user?.get("username") as? String ?: ""
                    val userLinks = user?.get("links") as? Map<*, *>
                    val userHtml = userLinks?.get("html") as? String ?: ""

                    val links = item["links"] as? Map<*, *>
                    val photoHtml = links?.get("html") as? String ?: ""

                    val mobileUrl = "$rawUrl&w=1080&q=90"

                    WallpaperInfo(
                        id = id,
                        url = mobileUrl,
                        urlMobile = mobileUrl,
                        urlFull = fullUrl,
                        authorName = userName,
                        authorUsername = userUsername,
                        authorLink = "$userHtml?utm_source=Glance&utm_medium=referral",
                        photoLink = "$photoHtml?utm_source=Glance&utm_medium=referral",
                        updatedAt = "",
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    suspend fun stageTomorrowWallpaper(
        info: WallpaperInfo?,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val token = settingsRepository.getGitHubWorkflowToken() ?: settingsRepository.getGitHubToken()
            if (token.isNullOrBlank()) {
                return@withContext false
            }

            try {
                val owner = "sameerasw"
                val repo = "sameerasw.com"
                val path = "public/unsplash-next.json"
                val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/$path"

                // 1. Get current SHA if file exists
                var currentSha: String? = null
                val getRequest =
                    Request.Builder()
                        .url(apiUrl)
                        .header("Authorization", "Bearer $token")
                        .header("Accept", "application/vnd.github.v3+json")
                        .build()

                val getResponse = client.newCall(getRequest).execute()
                if (getResponse.isSuccessful) {
                    val getBody = getResponse.body?.string()
                    if (getBody != null) {
                        val map = gson.fromJson(getBody, Map::class.java) as? Map<*, *>
                        currentSha = map?.get("sha") as? String
                    }
                }

                // 2. Build content payload
                val payloadMap: Map<String, Any?> =
                    if (info == null) {
                        emptyMap()
                    } else {
                        mapOf(
                            "mobile" to
                                mapOf(
                                    "id" to info.id,
                                    "url" to info.urlMobile,
                                    "url_full" to info.urlFull,
                                    "author" to
                                        mapOf(
                                            "name" to info.authorName,
                                            "username" to info.authorUsername,
                                            "link" to info.authorLink,
                                        ),
                                    "link" to info.photoLink,
                                    "updatedAt" to java.time.Instant.now().toString(),
                                ),
                        )
                    }

                val jsonContent = gson.toJson(payloadMap)
                val base64Content = Base64.getEncoder().encodeToString(jsonContent.toByteArray(Charsets.UTF_8))

                val commitMessage =
                    if (info == null) {
                        "chore: clear staged wallpaper for tomorrow"
                    } else {
                        "chore: stage tomorrow mobile wallpaper (${info.id})"
                    }

                val putBodyMap =
                    mutableMapOf<String, Any>(
                        "message" to commitMessage,
                        "content" to base64Content,
                    ).apply {
                        currentSha?.let { put("sha", it) }
                    }

                val putRequest =
                    Request.Builder()
                        .url(apiUrl)
                        .header("Authorization", "Bearer $token")
                        .header("Accept", "application/vnd.github.v3+json")
                        .put(gson.toJson(putBodyMap).toRequestBody("application/json".toMediaType()))
                        .build()

                val putResponse = client.newCall(putRequest).execute()
                putResponse.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
}
