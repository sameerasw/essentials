package com.sameerasw.essentials.domain.diy

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sameerasw.essentials.R

sealed interface State {
    @get:StringRes
    val title: Int
    @get:DrawableRes
    val icon: Int

    data object Charging : State {
        override val title: Int = R.string.diy_state_charging
        override val icon: Int = R.drawable.rounded_charger_24
    }
}
