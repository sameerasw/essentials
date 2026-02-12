package com.sameerasw.essentials.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.domain.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class UpdateRepository {

    suspend fun checkForUpdates(
        isPreReleaseCheckEnabled: Boolean,
        currentVersion: String
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val urlString = if (isPreReleaseCheckEnabled) {
                "https://api.github.com/repos/sameerasw/essentials/releases"
            } else {
                "https://api.github.com/repos/sameerasw/essentials/releases/latest"
            }

            val url = URL(urlString)
            val connection = url.openConnection() as java.net.HttpURLConnection

            if (connection.responseCode != 200) {
                return@withContext null
            }

            val releaseData = connection.inputStream.bufferedReader().readText()

            val release: Map<String, Any>? = if (isPreReleaseCheckEnabled) {
                val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val releases: List<Map<String, Any>> = Gson().fromJson(releaseData, listType)

                // Find the true latest release using SemanticVersion comparison
                releases.maxByOrNull { rel ->
                    val tagName = (rel["tag_name"] as? String)?.removePrefix("v") ?: "0.0.0"
                    SemanticVersion.parse(tagName)
                }
            } else {
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                Gson().fromJson(releaseData, mapType)
            }

            if (release == null) return@withContext null

            val latestVersion = (release["tag_name"] as? String)?.removePrefix("v") ?: "0.0"
            val body = release["body"] as? String ?: ""
            val releaseUrl = release["html_url"] as? String ?: ""
            val assets = (release["assets"] as? List<*>)?.filterIsInstance<Map<String, Any>>()
            val downloadUrl = assets?.firstOrNull { it["name"].toString() == "app-release.apk" }
                ?.get("browser_download_url") as? String
                ?: assets?.firstOrNull { it["name"].toString().endsWith(".apk") }
                    ?.get("browser_download_url") as? String
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
            val currentVer = SemanticVersion.parse(current)
            val latestVer = SemanticVersion.parse(latest)
            latestVer > currentVer
        } catch (e: Exception) {
            latest != current
        }
    }

    private data class SemanticVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val preRelease: String? = null
    ) : Comparable<SemanticVersion> {

        override fun compareTo(other: SemanticVersion): Int {
            if (major != other.major) return major - other.major
            if (minor != other.minor) return minor - other.minor
            if (patch != other.patch) return patch - other.patch

            // Version with pre-release is LOWER than version without (e.g. 1.0.0-beta < 1.0.0)
            if (preRelease == null && other.preRelease != null) return 1
            if (preRelease != null && other.preRelease == null) return -1

            // Compare pre-release strings naturally if both exist 
            // (e.g. beta.1 < beta.2) - String comparison matches lexical order
            if (preRelease != null && other.preRelease != null) {
                return preRelease.compareTo(other.preRelease)
            }

            return 0
        }

        companion object {
            fun parse(versionInfo: String): SemanticVersion {
                try {
                    // Remove "v" prefix if present
                    val version = versionInfo.removePrefix("v")

                    // Split into main parts and pre-release parts (e.g. "8.1-beta.1")
                    val parts = version.split("-", limit = 2)
                    val baseParts = parts[0].split(".")

                    val major = baseParts.getOrNull(0)?.toIntOrNull() ?: 0
                    val minor = baseParts.getOrNull(1)?.toIntOrNull() ?: 0
                    val patch = baseParts.getOrNull(2)?.toIntOrNull() ?: 0

                    val preRelease = if (parts.size > 1) parts[1] else null

                    return SemanticVersion(major, minor, patch, preRelease)
                } catch (e: Exception) {
                    return SemanticVersion(0, 0, 0)
                }
            }
        }
    }
}
