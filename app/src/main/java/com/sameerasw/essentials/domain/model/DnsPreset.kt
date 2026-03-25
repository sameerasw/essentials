package com.sameerasw.essentials.domain.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class DnsPreset(
    @SerializedName("id") val id: String = java.util.UUID.randomUUID().toString(),
    @SerializedName("name") val name: String,
    @SerializedName("hostname") val hostname: String,
    @SerializedName("isDefault") val isDefault: Boolean = false
)
