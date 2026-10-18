/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: BackGestureAppConfig.kt
 * Description: Per-app back gesture edge widths.
 */

package com.sameerasw.essentials.domain.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class BackGestureAppConfig(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("leftScale") val leftScale: Float = 0f,
    @SerializedName("rightScale") val rightScale: Float = 0f,
)
