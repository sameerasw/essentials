package com.sameerasw.essentials.domain.diy

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sameerasw.essentials.R

sealed interface Action {
    @get:StringRes
    val title: Int
    @get:DrawableRes
    val icon: Int

    data object HapticVibration : Action {
        override val title: Int = R.string.diy_action_haptic
        override val icon: Int = R.drawable.rounded_mobile_vibrate_24
    }

    data object ShowNotification : Action {
        override val title: Int = R.string.diy_action_notification
        override val icon: Int = R.drawable.rounded_notifications_unread_24
    }
    
    data object RemoveNotification : Action {
        override val title: Int = R.string.diy_action_remove_notification
        override val icon: Int = R.drawable.rounded_notifications_off_24
    }
}
