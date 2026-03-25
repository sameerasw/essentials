package com.sameerasw.essentials.domain.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class LocationAlarm(
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0,
    @SerializedName("radius") val radius: Int = 1000, // in meters
    @SerializedName("isEnabled") val isEnabled: Boolean = false
)
