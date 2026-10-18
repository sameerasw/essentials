/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: Trigger.kt
 * Description: Domain model and business logic entry for Trigger.kt.
 */

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
        @SerializedName("days") val days: Set<Int> = emptySet(),
    ) : Trigger {
        override val title: Int get() = R.string.diy_trigger_schedule
        override val icon: Int get() = R.drawable.rounded_nest_clock_farsight_analog_24
        override val isConfigurable: Boolean get() = true
    }

    @Keep
    data class BluetoothConnected(
        @SerializedName("deviceAddress") val deviceAddress: String = "",
        @SerializedName("deviceName") val deviceName: String = "",
    ) : Trigger {
        override val title: Int get() = R.string.diy_trigger_bluetooth_connected
        override val icon: Int get() = R.drawable.rounded_bluetooth_24
        override val isConfigurable: Boolean get() = true
        override val permissions: List<String>
            get() =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    listOf(android.Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    emptyList()
                }
    }

    @Keep
    data class BluetoothDisconnected(
        @SerializedName("deviceAddress") val deviceAddress: String = "",
        @SerializedName("deviceName") val deviceName: String = "",
    ) : Trigger {
        override val title: Int get() = R.string.diy_trigger_bluetooth_disconnected
        override val icon: Int get() = R.drawable.rounded_bluetooth_24
        override val isConfigurable: Boolean get() = true
        override val permissions: List<String>
            get() =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    listOf(android.Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    emptyList()
                }
    }

    @Keep
    data class WifiConnected(
        @SerializedName("ssid") val ssid: String = "",
    ) : Trigger {
        override val title: Int get() = R.string.diy_trigger_wifi_connected
        override val icon: Int get() = R.drawable.rounded_android_wifi_4_bar_plus_24
        override val isConfigurable: Boolean get() = true
        override val permissions: List<String>
            get() = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Keep
    data class WifiDisconnected(
        @SerializedName("ssid") val ssid: String = "",
    ) : Trigger {
        override val title: Int get() = R.string.diy_trigger_wifi_disconnected
        override val icon: Int get() = R.drawable.rounded_android_wifi_3_bar_24
        override val isConfigurable: Boolean get() = true
        override val permissions: List<String>
            get() = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Keep
    data class ArriveAtLocation(
        @SerializedName("latitude") val latitude: Double = 0.0,
        @SerializedName("longitude") val longitude: Double = 0.0,
        @SerializedName("radiusMeters") val radiusMeters: Int = DEFAULT_LOCATION_RADIUS_METERS,
        @SerializedName("placeName") val placeName: String = "",
    ) : Trigger {
        override val title: Int get() = R.string.diy_trigger_arrive_location
        override val icon: Int get() = R.drawable.rounded_location_on_24
        override val isConfigurable: Boolean get() = true
        override val permissions: List<String> get() = listOf("LOCATION", "BACKGROUND_LOCATION")
    }

    @Keep
    data class LeaveLocation(
        @SerializedName("latitude") val latitude: Double = 0.0,
        @SerializedName("longitude") val longitude: Double = 0.0,
        @SerializedName("radiusMeters") val radiusMeters: Int = DEFAULT_LOCATION_RADIUS_METERS,
        @SerializedName("placeName") val placeName: String = "",
    ) : Trigger {
        override val title: Int get() = R.string.diy_trigger_leave_location
        override val icon: Int get() = R.drawable.rounded_logout_24
        override val isConfigurable: Boolean get() = true
        override val permissions: List<String> get() = listOf("LOCATION", "BACKGROUND_LOCATION")
    }

    @Keep
    data object PowerSavingOn : Trigger {
        override val title: Int = R.string.diy_trigger_power_saving_on
        override val icon: Int = R.drawable.rounded_battery_android_frame_shield_24
    }

    @Keep
    data object PowerSavingOff : Trigger {
        override val title: Int = R.string.diy_trigger_power_saving_off
        override val icon: Int = R.drawable.rounded_battery_android_frame_shield_24
    }

    // A blank app matches every app and blank text matches any content; the editor requires at least one of them
    @Keep
    data class NotificationReceived(
        @SerializedName("packageName") val packageName: String = "",
        @SerializedName("appName") val appName: String = "",
        @SerializedName("textContains") val textContains: String = "",
    ) : Trigger {
        override val title: Int get() = R.string.diy_trigger_notification_received
        override val icon: Int get() = R.drawable.rounded_notifications_unread_24
        override val isConfigurable: Boolean get() = true
        override val permissions: List<String> get() = listOf("NOTIFICATION_LISTENER")

        fun matches(
            alertPackage: String,
            alertTitle: String,
            alertText: String,
        ): Boolean =
            (packageName.isBlank() || packageName == alertPackage) &&
                (
                    textContains.isBlank() ||
                        alertTitle.contains(textContains, ignoreCase = true) ||
                        alertText.contains(textContains, ignoreCase = true)
                )
    }

    @Keep
    data object DndOn : Trigger {
        override val title: Int = R.string.diy_trigger_dnd_on
        override val icon: Int = R.drawable.rounded_do_not_disturb_on_24
    }

    @Keep
    data object DndOff : Trigger {
        override val title: Int = R.string.diy_trigger_dnd_off
        override val icon: Int = R.drawable.rounded_do_not_disturb_on_24
    }

    @Keep
    data object AndroidAutoConnected : Trigger {
        override val title: Int = R.string.diy_trigger_android_auto_connected
        override val icon: Int = R.drawable.rounded_directions_car_24
    }

    @Keep
    data object AndroidAutoDisconnected : Trigger {
        override val title: Int = R.string.diy_trigger_android_auto_disconnected
        override val icon: Int = R.drawable.rounded_directions_car_24
    }

    companion object {
        const val DEFAULT_LOCATION_RADIUS_METERS = 200
        const val MIN_LOCATION_RADIUS_METERS = 100
        const val MAX_LOCATION_RADIUS_METERS = 5000
    }
}
