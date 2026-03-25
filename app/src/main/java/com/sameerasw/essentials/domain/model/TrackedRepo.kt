package com.sameerasw.essentials.domain.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class TrackedRepo(
    @SerializedName("owner") val owner: String,
    @SerializedName("name") val name: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("description") val description: String?,
    @SerializedName("stars") val stars: Int,
    @SerializedName("avatarUrl") val avatarUrl: String,
    @SerializedName("latestTagName") val latestTagName: String,
    @SerializedName("latestReleaseName") val latestReleaseName: String?,
    @SerializedName("latestReleaseBody") val latestReleaseBody: String?,
    @SerializedName("latestReleaseUrl") val latestReleaseUrl: String?,
    @SerializedName("downloadUrl") val downloadUrl: String?,
    @SerializedName("publishedAt") val publishedAt: String,
    @SerializedName("selectedApkName") val selectedApkName: String, // "Auto" or actual name
    @SerializedName("mappedPackageName") val mappedPackageName: String?, // Linked local app
    @SerializedName("mappedAppName") val mappedAppName: String?,
    @SerializedName("isUpdateAvailable") val isUpdateAvailable: Boolean = false,
    @SerializedName("allowPreReleases") val allowPreReleases: Boolean = false,
    @SerializedName("notificationsEnabled") val notificationsEnabled: Boolean = true,
    @SerializedName("lastETag") val lastETag: String? = null,
    @SerializedName("addedAt") val addedAt: Long = System.currentTimeMillis()
)
