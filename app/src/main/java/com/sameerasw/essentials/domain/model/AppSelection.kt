package com.sameerasw.essentials.domain.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class AppSelection(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("isEnabled") val isEnabled: Boolean
)
