package com.sameerasw.essentials.domain.diy

import androidx.annotation.StringRes
import com.sameerasw.essentials.R

sealed interface Trigger {
    val title: Int
    val icon: Int

    data object ScreenOff : Trigger {
        override val title: Int = R.string.diy_trigger_screen_off
        override val icon: Int = R.drawable.rounded_mobile_lock_portrait_24
    }
}
