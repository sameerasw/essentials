package com.sameerasw.essentials.domain.diy

import com.sameerasw.essentials.R

sealed interface Trigger {
    val title: Int
    val icon: Int
    val permissions: List<String>
        get() = emptyList()
    val isConfigurable: Boolean
        get() = false

    data object ScreenOff : Trigger {
        override val title: Int = R.string.diy_trigger_screen_off
        override val icon: Int = R.drawable.rounded_mobile_lock_portrait_24
    }

    data object ScreenOn : Trigger {
        override val title: Int = R.string.diy_trigger_screen_on
        override val icon: Int = R.drawable.rounded_mobile_text_2_24
    }

    data object DeviceUnlock : Trigger {
        override val title: Int = R.string.diy_trigger_device_unlock
        override val icon: Int = R.drawable.rounded_mobile_unlock_24
    }

    data object ChargerConnected : Trigger {
        override val title: Int = R.string.diy_trigger_charger_connected
        override val icon: Int = R.drawable.rounded_battery_charging_60_24
    }

    data object ChargerDisconnected : Trigger {
        override val title: Int = R.string.diy_trigger_charger_disconnected
        override val icon: Int = R.drawable.rounded_battery_android_frame_3_24
    }
 
    data class Schedule(
        val hour: Int = 0,
        val minute: Int = 0,
        val days: Set<Int> = emptySet()
    ) : Trigger {
        override val title: Int get() = R.string.diy_trigger_schedule
        override val icon: Int get() = R.drawable.rounded_nest_clock_farsight_analog_24
        override val isConfigurable: Boolean get() = true
    }
}
