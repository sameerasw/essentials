package com.sameerasw.essentials.domain.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class UpdateInfo(
    @SerializedName("versionName") val versionName: String,
    @SerializedName("releaseNotes") val releaseNotes: String,
    @SerializedName("downloadUrl") val downloadUrl: String,
    @SerializedName("releaseUrl") val releaseUrl: String = "",
    @SerializedName("isUpdateAvailable") val isUpdateAvailable: Boolean = false
)
