package com.sameerasw.essentials.domain.diy

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.sameerasw.essentials.R

@Keep
sealed interface Trigger {
    val title: Int
    val icon: Int
    val permissions: List<String>
        get() = emptyList()
    val isConfigurable: Boolean
        get() = false

    @Keep
    data object ScreenOff : Trigger {
        override val title: Int = R.string.diy_trigger_screen_off
        override val icon: Int = R.drawable.rounded_mobile_lock_portrait_24
    }

    @Keep
    data object ScreenOn : Trigger {
        override val title: Int = R.string.diy_trigger_screen_on
        override val icon: Int = R.drawable.rounded_mobile_text_2_24
    }

    @Keep
    data object DeviceUnlock : Trigger {
        override val title: Int = R.string.diy_trigger_device_unlock
        override val icon: Int = R.drawable.rounded_mobile_unlock_24
    }

    @Keep
    data object ChargerConnected : Trigger {
        override val title: Int = R.string.diy_trigger_charger_connected
        override val icon: Int = R.drawable.rounded_battery_charging_60_24
    }

    @Keep
    data object ChargerDisconnected : Trigger {
        override val title: Int = R.string.diy_trigger_charger_disconnected
        override val icon: Int = R.drawable.rounded_battery_android_frame_3_24
    }
 
    @Keep
    data class Schedule(
        @SerializedName("hour") val hour: Int = 0,
        @SerializedName("minute") val minute: Int = 0,
        @SerializedName("days") val days: Set<Int> = emptySet()
    ) : Trigger {
        override val title: Int get() = R.string.diy_trigger_schedule
        override val icon: Int get() = R.drawable.rounded_nest_clock_farsight_analog_24
        override val isConfigurable: Boolean get() = true
    }
}
