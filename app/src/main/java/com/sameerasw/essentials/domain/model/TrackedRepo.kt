package com.sameerasw.essentials.domain.model

data class TrackedRepo(
    val owner: String,
    val name: String,
    val fullName: String,
    val description: String?,
    val stars: Int,
    val avatarUrl: String,
    val latestTagName: String,
    val latestReleaseName: String?,
    val latestReleaseBody: String?,
    val latestReleaseUrl: String?,
    val downloadUrl: String?,
    val publishedAt: String,
    val selectedApkName: String, // "Auto" or actual name
    val mappedPackageName: String?, // Linked local app
    val mappedAppName: String?,
    val isUpdateAvailable: Boolean = false,
    val allowPreReleases: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val lastETag: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
