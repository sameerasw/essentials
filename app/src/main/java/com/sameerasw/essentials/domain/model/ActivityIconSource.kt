/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: ActivityIconSource.kt
 * Description: Where the icon of an Open Activity shortcut comes from.
 */

package com.sameerasw.essentials.domain.model

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.sameerasw.essentials.R

enum class ActivityIconSource(
    @StringRes val title: Int,
) {
    @SerializedName("ACTIVITY")
    ACTIVITY(R.string.activity_icon_source_activity),

    @SerializedName("APP")
    APP(R.string.activity_icon_source_app),

    @SerializedName("CUSTOM")
    CUSTOM(R.string.activity_icon_source_custom),
}
