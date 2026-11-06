/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: Metric.kt
 * Description: Measurable numeric values for value conditions.
 */

package com.sameerasw.essentials.domain.diy

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.sameerasw.essentials.R

@Keep
enum class Metric(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val min: Double,
    val max: Double,
    val step: Double,
    val defaultHysteresis: Double,
) {
    BATTERY_LEVEL(
        R.string.diy_metric_battery_level,
        R.drawable.rounded_battery_android_frame_3_24,
        0.0,
        100.0,
        1.0,
        2.0,
    ),
}
