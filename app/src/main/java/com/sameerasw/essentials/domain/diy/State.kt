/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: State.kt
 * Description: Domain model and business logic entry for State.kt.
 */

package com.sameerasw.essentials.domain.diy

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.sameerasw.essentials.R

@Keep
sealed interface State {
    @get:StringRes
    val title: Int

    @get:DrawableRes
    val icon: Int

    @Keep
    data object Charging : State {
        override val title: Int = R.string.diy_state_charging
        override val icon: Int = R.drawable.rounded_charger_24
    }

    @Keep
    data object ScreenOn : State {
        override val title: Int = R.string.diy_state_screen_on
        override val icon: Int = R.drawable.rounded_mobile_text_2_24
    }

    @Keep
    data object PowerSaving : State {
        override val title: Int = R.string.diy_state_power_saving
        override val icon: Int = R.drawable.rounded_battery_android_frame_shield_24
    }

    @Keep
    data class TimePeriod(
        @SerializedName("startHour") val startHour: Int = 0,
        @SerializedName("startMinute") val startMinute: Int = 0,
        @SerializedName("endHour") val endHour: Int = 0,
        @SerializedName("endMinute") val endMinute: Int = 0,
        @SerializedName("days") val days: Set<Int> = emptySet()
    ) : State {
        override val title: Int get() = R.string.diy_state_time_period
        override val icon: Int get() = R.drawable.rounded_timelapse_24
    }
}
