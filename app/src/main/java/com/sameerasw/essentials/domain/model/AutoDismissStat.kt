/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: AutoDismissStat.kt
 * Description: Domain model and business logic entry for AutoDismissStat.kt.
 */

package com.sameerasw.essentials.domain.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class AutoDismissStat(
    @SerializedName("count") val count: Int,
    @SerializedName("since") val since: Long,
)
