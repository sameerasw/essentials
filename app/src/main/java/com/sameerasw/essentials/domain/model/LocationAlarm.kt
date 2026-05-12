package com.sameerasw.essentials.domain.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class LocationAlarm(
    @SerializedName("id") val id: String = java.util.UUID.randomUUID().toString(),
    @SerializedName("name") val name: String = "",
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("radius") val radius: Int = 1000, // in meters
    @SerializedName("isEnabled") val isEnabled: Boolean = false,
    @SerializedName("lastTravelled") val lastTravelled: Long? = null,
    @SerializedName("isPaused") val isPaused: Boolean = false,
    @SerializedName("iconResName") val iconResName: String = "round_navigation_24",
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis()
)
