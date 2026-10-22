/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: AutoDismissChannel.kt
 * Description: Domain model and business logic entry for AutoDismissChannel.kt.
 */

package com.sameerasw.essentials.domain.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class AutoDismissChannel(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
)
