/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: AutoDismissAppConfig.kt
 * Description: Domain model and business logic entry for AutoDismissAppConfig.kt.
 */

package com.sameerasw.essentials.domain.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class AutoDismissAppConfig(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("minutes") val minutes: Int,
    @SerializedName("excludedChannels") val excludedChannels: List<String>? = null,
)
