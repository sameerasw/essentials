/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: Mode.kt
 * Description: An Essentials mode, backed by a system mode the app owns.
 */

package com.sameerasw.essentials.domain.modes

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.sameerasw.essentials.R
import java.util.UUID

@Keep
data class Mode(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("name") val name: String = "",
    @SerializedName("iconName") val iconName: String = ModeIcons.DEFAULT,
    @SerializedName("colorHex") val colorHex: String = "#BAE1FF",
    @SerializedName("isEnabled") val isEnabled: Boolean = true,
)

object ModeIcons {
    const val DEFAULT = "rounded_work_24"

    private val icons =
        linkedMapOf(
            "rounded_work_24" to R.drawable.rounded_work_24,
            "rounded_home_24" to R.drawable.rounded_home_24,
            "rounded_bedtime_24" to R.drawable.rounded_bedtime_24,
            "rounded_directions_car_24" to R.drawable.rounded_directions_car_24,
            "rounded_fitness_center_24" to R.drawable.rounded_fitness_center_24,
            "rounded_school_24" to R.drawable.rounded_school_24,
            "rounded_self_improvement_24" to R.drawable.rounded_self_improvement_24,
            "rounded_spa_24" to R.drawable.rounded_spa_24,
            "rounded_flight_24" to R.drawable.rounded_flight_24,
            "rounded_videogame_asset_24" to R.drawable.rounded_videogame_asset_24,
            "rounded_music_note_24" to R.drawable.rounded_music_note_24,
            "rounded_local_pizza_24" to R.drawable.rounded_local_pizza_24,
        )

    val names: List<String> = icons.keys.toList()

    @DrawableRes
    fun resolve(name: String): Int = icons[name] ?: R.drawable.rounded_work_24
}
