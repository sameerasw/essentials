/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: Action.kt
 * Description: Domain model and business logic entry for Action.kt.
 */

package com.sameerasw.essentials.domain.diy

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.ScreenOffMethod

@Keep
sealed interface Action {
    @get:StringRes
    val title: Int

    @get:DrawableRes
    val icon: Int
    val permissions: List<String>
        get() = emptyList()
    val isConfigurable: Boolean
        get() = false

    @Keep
    data object HapticVibration : Action {
        override val title: Int = R.string.diy_action_haptic
        override val icon: Int = R.drawable.rounded_mobile_vibrate_24
    }

    @Keep
    data object ShowNotification : Action {
        override val title: Int = R.string.diy_action_notification
        override val icon: Int = R.drawable.rounded_notifications_unread_24
    }

    @Keep
    data object RemoveNotification : Action {
        override val title: Int = R.string.diy_action_remove_notification
        override val icon: Int = R.drawable.rounded_notifications_off_24
    }

    @Keep
    data object TurnOnFlashlight : Action {
        override val title: Int = R.string.diy_action_flashlight_on
        override val icon: Int = R.drawable.round_flashlight_on_24
    }

    @Keep
    data object TurnOffFlashlight : Action {
        override val title: Int = R.string.diy_action_flashlight_off
        override val icon: Int = R.drawable.rounded_flashlight_on_24
    }

    @Keep
    data object ToggleFlashlight : Action {
        override val title: Int = R.string.diy_action_flashlight_toggle
        override val icon: Int = R.drawable.rounded_flashlight_on_24
    }

    @Keep
    data class DimWallpaper(
        @SerializedName("dimAmount") val dimAmount: Float = 0f
    ) : Action {
        override val title: Int get() = R.string.diy_action_dim_wallpaper
        override val icon: Int get() = R.drawable.rounded_mobile_screensaver_24
        override val permissions: List<String> = listOf("SHIZUKU", "ROOT")
        override val isConfigurable: Boolean = true
    }

    @Keep
    data class DeviceEffects(
        @SerializedName("enabled") val enabled: Boolean = true,
        @SerializedName("grayscale") val grayscale: Boolean = false,
        @SerializedName("suppressAmbient") val suppressAmbient: Boolean = false,
        @SerializedName("dimWallpaper") val dimWallpaper: Boolean = false,
        @SerializedName("nightMode") val nightMode: Boolean = false
    ) : Action {
        override val title: Int get() = R.string.diy_action_device_effects
        override val icon: Int get() = R.drawable.rounded_bed_24
        override val permissions: List<String> = listOf("NOTIFICATION_POLICY")
        override val isConfigurable: Boolean = true
    }

    @Keep
    enum class SoundModeType {
        @SerializedName("SOUND")
        SOUND,

        @SerializedName("VIBRATE")
        VIBRATE,

        @SerializedName("SILENT")
        SILENT
    }

    @Keep
    data class SoundMode(
        @SerializedName("mode") val mode: SoundModeType = SoundModeType.SOUND
    ) : Action {
        override val title: Int get() = R.string.diy_action_sound_mode
        override val icon: Int
            get() = when (mode) {
                SoundModeType.SOUND -> R.drawable.rounded_volume_up_24
                SoundModeType.VIBRATE -> R.drawable.rounded_mobile_vibrate_24
                SoundModeType.SILENT -> R.drawable.rounded_volume_off_24
            }
        override val permissions: List<String> = listOf("NOTIFICATION_POLICY")
        override val isConfigurable: Boolean = true
    }

    @Keep
    data object TurnOnLowPower : Action {
        override val title: Int = R.string.diy_action_low_power_on
        override val icon: Int = R.drawable.rounded_battery_android_frame_shield_24
    }

    @Keep
    data object TurnOffLowPower : Action {
        override val title: Int = R.string.diy_action_low_power_off
        override val icon: Int = R.drawable.rounded_battery_android_frame_shield_24
    }

    @Keep
    data class ScreenOff(
        @SerializedName("method") val method: ScreenOffMethod = ScreenOffMethod.ACCESSIBILITY,
        @SerializedName("haptic") val haptic: HapticFeedbackType = HapticFeedbackType.NONE
    ) : Action {
        override val title: Int get() = R.string.diy_action_screen_off
        override val icon: Int get() = R.drawable.rounded_mobile_off_24
        override val isConfigurable: Boolean = true
        override val permissions: List<String>
            get() = if (method == ScreenOffMethod.INPUT) listOf("shizuku", "root") else emptyList()
    }

    @Keep
    data object MediaPlayPause : Action {
        override val title: Int = R.string.diy_action_media_play_pause
        override val icon: Int = R.drawable.round_play_arrow_24
    }

    @Keep
    data object MediaNext : Action {
        override val title: Int = R.string.diy_action_media_next
        override val icon: Int = R.drawable.rounded_skip_next_24
    }

    @Keep
    data object MediaPrevious : Action {
        override val title: Int = R.string.diy_action_media_previous
        override val icon: Int = R.drawable.rounded_skip_previous_24
    }

    @Keep
    data object AIAssistant : Action {
        override val title: Int = R.string.diy_action_ai_assistant
        override val icon: Int = R.drawable.google
    }

    @Keep
    data object TakeScreenshot : Action {
        override val title: Int = R.string.diy_action_take_screenshot
        override val icon: Int = R.drawable.rounded_screenshot_region_24
        override val permissions: List<String> = listOf("accessibility")
    }

    @Keep
    data object ToggleMediaVolume : Action {
        override val title: Int = R.string.diy_action_toggle_media_volume
        override val icon: Int = R.drawable.rounded_mobile_sound_24
    }

    @Keep
    data object LikeCurrentSong : Action {
        override val title: Int = R.string.diy_action_like_current_song
        override val icon: Int = R.drawable.rounded_favorite_24
    }

    @Keep
    data object CircleToSearch : Action {
        override val title: Int = R.string.diy_action_circle_to_search
        override val icon: Int = R.drawable.rounded_search_24
        override val permissions: List<String> = listOf("shizuku", "root")
    }

    @Keep
    data object PinApp : Action {
        override val title: Int = R.string.diy_action_pin_app
        override val icon: Int = R.drawable.rounded_shield_lock_24
        override val permissions: List<String> = listOf("shizuku", "root")
    }

    @Keep
    data class SometimesEssentials(
        @SerializedName("changeNotificationLighting") val changeNotificationLighting: Boolean = false,
        @SerializedName("notificationLightingEnabled") val notificationLightingEnabled: Boolean = true,
        @SerializedName("changeFlashlightPulse") val changeFlashlightPulse: Boolean = false,
        @SerializedName("flashlightPulseEnabled") val flashlightPulseEnabled: Boolean = true,
        @SerializedName("changeBatteryNotification") val changeBatteryNotification: Boolean = false,
        @SerializedName("batteryNotificationEnabled") val batteryNotificationEnabled: Boolean = true,
        @SerializedName("changeEssentialsOnDisplay") val changeEssentialsOnDisplay: Boolean = false,
        @SerializedName("essentialsOnDisplayMode") val essentialsOnDisplayMode: String = "On", // "Off", "On", "Docked"
        @SerializedName("changeAlwaysOnDisplay") val changeAlwaysOnDisplay: Boolean = false,
        @SerializedName("alwaysOnDisplayMode") val alwaysOnDisplayMode: String = "On", // "Off", "Dynamic", "On"
        @SerializedName("changeGloveMode") val changeGloveMode: Boolean = false,
        @SerializedName("gloveModeEnabled") val gloveModeEnabled: Boolean = true,
        @SerializedName("changeLockScreenClock") val changeLockScreenClock: Boolean = false,
        @SerializedName("lockScreenClockStyle") val lockScreenClockStyle: String = "DEFAULT",
        @SerializedName("changeSyncSoundModeWatch") val changeSyncSoundModeWatch: Boolean = false,
        @SerializedName("syncSoundModeWatchEnabled") val syncSoundModeWatchEnabled: Boolean = true,
        @SerializedName("changeSmartPixels") val changeSmartPixels: Boolean = false,
        @SerializedName("smartPixelsEnabled") val smartPixelsEnabled: Boolean = true
    ) : Action {
        override val title: Int get() = R.string.diy_action_sometimes_essentials
        override val icon: Int get() = R.drawable.rounded_settings_24
        override val isConfigurable: Boolean = true
        override val permissions: List<String>
            get() {
                val perms = mutableListOf<String>()
                if (changeNotificationLighting || changeEssentialsOnDisplay || changeAlwaysOnDisplay || changeLockScreenClock || changeGloveMode) {
                    perms.add("write_secure_settings")
                }
                if (changeSmartPixels) {
                    perms.add("accessibility")
                }
                return perms
            }
    }

    @Keep
    data class OpenApp(
        @SerializedName("packageName") val packageName: String = ""
    ) : Action {
        override val title: Int get() = R.string.diy_action_open_app
        override val icon: Int get() = R.drawable.rounded_open_in_new_24
        override val isConfigurable: Boolean = true
    }

    @Keep
    data object TurnOnHotspot : Action {
        override val title: Int = R.string.diy_action_hotspot_on
        override val icon: Int = R.drawable.rounded_wifi_tethering_24
        override val permissions: List<String> = listOf("shizuku", "root")
    }

    @Keep
    data object TurnOffHotspot : Action {
        override val title: Int = R.string.diy_action_hotspot_off
        override val icon: Int = R.drawable.rounded_wifi_tethering_24
        override val permissions: List<String> = listOf("shizuku", "root")
    }

    @Keep
    data object ToggleHotspot : Action {
        override val title: Int = R.string.diy_action_hotspot_toggle
        override val icon: Int = R.drawable.rounded_wifi_tethering_24
        override val permissions: List<String> = listOf("shizuku", "root")
    }

    @Keep
    data class FreezeTag(
        @SerializedName("mode") val mode: String = "Freeze", // "Freeze", "Unfreeze"
        @SerializedName("tagIds") val tagIds: List<String> = emptyList()
    ) : Action {
        override val title: Int get() = R.string.diy_action_freeze_tag
        override val icon: Int get() = R.drawable.rounded_mode_cool_24
        override val isConfigurable: Boolean = true
        override val permissions: List<String> = listOf("shizuku", "root")
    }

    @Keep
    data object TurnOnWifi : Action {
        override val title: Int = R.string.diy_action_wifi_on
        override val icon: Int = R.drawable.rounded_android_wifi_4_bar_plus_24
        override val permissions: List<String> = listOf("SHIZUKU", "ROOT")
    }

    @Keep
    data object TurnOffWifi : Action {
        override val title: Int = R.string.diy_action_wifi_off
        override val icon: Int = R.drawable.rounded_android_wifi_4_bar_plus_24
        override val permissions: List<String> = listOf("SHIZUKU", "ROOT")
    }

    @Keep
    data object TurnOnCellularData : Action {
        override val title: Int = R.string.diy_action_cellular_on
        override val icon: Int = R.drawable.rounded_signal_cellular_alt_24
        override val permissions: List<String> = listOf("SHIZUKU", "ROOT")
    }

    @Keep
    data object TurnOffCellularData : Action {
        override val title: Int = R.string.diy_action_cellular_off
        override val icon: Int = R.drawable.rounded_signal_cellular_alt_24
        override val permissions: List<String> = listOf("SHIZUKU", "ROOT")
    }

    @Keep
    data object TurnOnAutoBrightness : Action {
        override val title: Int = R.string.diy_action_auto_brightness_on
        override val icon: Int = R.drawable.rounded_brightness_auto_24
        override val permissions: List<String> = listOf("WRITE_SETTINGS")
    }

    @Keep
    data object TurnOffAutoBrightness : Action {
        override val title: Int = R.string.diy_action_auto_brightness_off
        override val icon: Int = R.drawable.rounded_brightness_auto_24
        override val permissions: List<String> = listOf("WRITE_SETTINGS")
    }

    @Keep
    data class FreezeApps(
        @SerializedName("packageNames") val packageNames: List<String> = emptyList()
    ) : Action {
        override val title: Int get() = R.string.diy_action_freeze_apps
        override val icon: Int get() = R.drawable.rounded_mode_cool_24
        override val permissions: List<String> = listOf("SHIZUKU", "ROOT")
        override val isConfigurable: Boolean = true
    }

    @Keep
    data class UnfreezeApps(
        @SerializedName("packageNames") val packageNames: List<String> = emptyList()
    ) : Action {
        override val title: Int get() = R.string.diy_action_unfreeze_apps
        override val icon: Int get() = R.drawable.rounded_mode_cool_off_24
        override val permissions: List<String> = listOf("SHIZUKU", "ROOT")
        override val isConfigurable: Boolean = true
    }

    @Keep
    data class Keyboard(
        @SerializedName("packageName") val packageName: String? = null
    ) : Action {
        override val title: Int = R.string.feat_system_keyboard_title
        override val icon: Int = R.drawable.rounded_keyboard_24
        override val permissions: List<String> = listOf("WRITE_SECURE_SETTINGS")
        override val isConfigurable: Boolean = true
    }

    @Keep
    enum class SettingsTable {
        @SerializedName("SYSTEM")
        SYSTEM,

        @SerializedName("SECURE")
        SECURE,

        @SerializedName("GLOBAL")
        GLOBAL
    }

    @Keep
    data class SettingsEntry(
        @SerializedName("table") val table: SettingsTable = SettingsTable.SYSTEM,
        @SerializedName("key") val key: String = "",
        @SerializedName("value") val value: String = ""
    )

    @Keep
    data class CustomSettings(
        @SerializedName("entries") val entries: List<SettingsEntry> = emptyList()
    ) : Action {
        override val title: Int get() = R.string.diy_action_custom_settings
        override val icon: Int get() = R.drawable.rounded_settings_24
        override val isConfigurable: Boolean = true
        override val permissions: List<String>
            get() {
                val perms = mutableSetOf<String>()
                for (entry in entries) {
                    when (entry.table) {
                        SettingsTable.SYSTEM -> perms.add("WRITE_SETTINGS")
                        SettingsTable.SECURE -> perms.add("WRITE_SECURE_SETTINGS")
                        SettingsTable.GLOBAL -> perms.add("WRITE_SECURE_SETTINGS")
                    }
                }
                return perms.toList()
            }
    }
}


