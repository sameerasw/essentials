package com.sameerasw.essentials.data.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class DeviceSpecItem(
    @SerializedName("name") val name: String,
    @SerializedName("value") val value: String
)

@Keep
data class DeviceSpecCategory(
    @SerializedName("category") val category: String,
    @SerializedName("specifications") val specifications: List<DeviceSpecItem>
)

@Keep
data class DeviceSpecs(
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("detailSpec") val detailSpec: List<DeviceSpecCategory>,
    @SerializedName("imageUrls") val imageUrls: List<String> = emptyList(),
    @SerializedName("localImagePaths") val localImagePaths: List<String> = emptyList(),
    @SerializedName("lastUpdated") val lastUpdated: Long = System.currentTimeMillis()
)
