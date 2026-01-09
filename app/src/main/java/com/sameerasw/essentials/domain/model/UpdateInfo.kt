package com.sameerasw.essentials.domain.model

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releaseUrl: String = "",
    val isUpdateAvailable: Boolean = false
)
