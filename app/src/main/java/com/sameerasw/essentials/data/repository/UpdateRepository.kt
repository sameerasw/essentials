package com.sameerasw.essentials.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.domain.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class UpdateRepository {

    suspend fun checkForUpdates(isPreReleaseCheckEnabled: Boolean, currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = if (isPreReleaseCheckEnabled) {
                URL("https://api.github.com/repos/sameerasw/essentials/releases")
            } else {
                URL("https://api.github.com/repos/sameerasw/essentials/releases/latest")
            }
            
            val releaseData = url.readText()

            val release: Map<String, Any>? = if (isPreReleaseCheckEnabled) {
                val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val releases: List<Map<String, Any>> = Gson().fromJson(releaseData, listType)
                releases.firstOrNull()
            } else {
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                Gson().fromJson(releaseData, mapType)
            }

            if (release == null) return@withContext null

            val latestVersion = (release["tag_name"] as? String)?.removePrefix("v") ?: "0.0"
            val body = release["body"] as? String ?: ""
            val releaseUrl = release["html_url"] as? String ?: ""
            val assets = release["assets"] as? List<Map<String, Any>>
            val downloadUrl = assets?.firstOrNull { it["name"].toString() == "app-release.apk" }?.get("browser_download_url") as? String 
                ?: assets?.firstOrNull { it["name"].toString().endsWith(".apk") }?.get("browser_download_url") as? String 
                ?: ""

            val hasUpdate = isNewerVersion(currentVersion, latestVersion)
            
            UpdateInfo(
                versionName = latestVersion,
                releaseNotes = body,
                downloadUrl = downloadUrl,
                releaseUrl = releaseUrl,
                isUpdateAvailable = hasUpdate
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        return try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLength = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until maxLength) {
                val v1 = if (i < currentParts.size) currentParts[i] else 0
                val v2 = if (i < latestParts.size) latestParts[i] else 0
                if (v2 > v1) return true
                if (v1 > v2) return false
            }
            false
        } catch (e: Exception) {
            latest != current
        }
    }
}
