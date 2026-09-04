/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Conscious Gate
 * File: ConsciousGateIcons.kt
 * Description: Static mapping between the icon names persisted for Conscious Gate and their
 * explicit R.drawable ids, so drawables stay referenced for R8 resource shrinking instead of
 * being looked up by name at runtime.
 */

package com.sameerasw.essentials.ui.features.consciousgate.components

import androidx.annotation.DrawableRes
import com.sameerasw.essentials.R

object ConsciousGateIcons {
    const val DEFAULT_ICON_NAME = "rounded_pause_24"

    val OPTIONS: List<Pair<String, Int>> =
        listOf(
            "rounded_favorite_24" to R.drawable.rounded_favorite_24,
            "rounded_heart_smile_24" to R.drawable.rounded_heart_smile_24,
            "rounded_ecg_heart_24" to R.drawable.rounded_ecg_heart_24,
            "rounded_volunteer_activism_24" to R.drawable.rounded_volunteer_activism_24,
            "rounded_self_improvement_24" to R.drawable.rounded_self_improvement_24,
            "rounded_health_and_safety_24" to R.drawable.rounded_health_and_safety_24,
            "rounded_shield_24" to R.drawable.rounded_shield_24,
            "rounded_wb_sunny_24" to R.drawable.rounded_wb_sunny_24,
            "rounded_nightlight_24" to R.drawable.rounded_nightlight_24,
            "rounded_sentiment_satisfied_24" to R.drawable.rounded_sentiment_satisfied_24,
            "rounded_sentiment_very_satisfied_24" to R.drawable.rounded_sentiment_very_satisfied_24,
            "rounded_spa_24" to R.drawable.rounded_spa_24,
            "rounded_eco_24" to R.drawable.rounded_eco_24,
            "rounded_potted_plant_24" to R.drawable.rounded_potted_plant_24,
            "rounded_pause_24" to R.drawable.rounded_pause_24,
            "rounded_timer_24" to R.drawable.rounded_timer_24,
            "rounded_lock_clock_24" to R.drawable.rounded_lock_clock_24,
        )

    private val byName = OPTIONS.toMap()

    @DrawableRes
    fun resolve(name: String): Int = byName[name] ?: R.drawable.rounded_pause_24
}
