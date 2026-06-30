package com.sameerasw.essentials.domain.diy

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.ScreenOffMethod
import com.sameerasw.essentials.domain.HapticFeedbackType

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
        override val permissions: List<String> = listOf("shizuku", "root")
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
        override val permissions: List<String> = listOf("notification_policy")
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
        override val permissions: List<String> = listOf("notification_policy")
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
}
