package com.sameerasw.essentials.domain.diy

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sameerasw.essentials.R

sealed interface Action {
    @get:StringRes
    val title: Int

    @get:DrawableRes
    val icon: Int
    val permissions: List<String>
        get() = emptyList()
    val isConfigurable: Boolean
        get() = false

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

    data object TurnOnFlashlight : Action {
        override val title: Int = R.string.diy_action_flashlight_on
        override val icon: Int = R.drawable.round_flashlight_on_24
    }

    data object TurnOffFlashlight : Action {
        override val title: Int = R.string.diy_action_flashlight_off
        override val icon: Int = R.drawable.rounded_flashlight_on_24
    }

    data object ToggleFlashlight : Action {
        override val title: Int = R.string.diy_action_flashlight_toggle
        override val icon: Int = R.drawable.rounded_flashlight_on_24
    }

    data class DimWallpaper(val dimAmount: Float = 0f) : Action {
        override val title: Int get() = R.string.diy_action_dim_wallpaper
        override val icon: Int get() = R.drawable.rounded_mobile_screensaver_24
        override val permissions: List<String> = listOf("shizuku", "root")
        override val isConfigurable: Boolean = true
    }

    data class DeviceEffects(
        val enabled: Boolean = true,
        val grayscale: Boolean = false,
        val suppressAmbient: Boolean = false,
        val dimWallpaper: Boolean = false,
        val nightMode: Boolean = false
    ) : Action {
        override val title: Int get() = R.string.diy_action_device_effects
        override val icon: Int get() = R.drawable.rounded_bed_24
        override val permissions: List<String> = listOf("notification_policy")
        override val isConfigurable: Boolean = true
    }

    enum class SoundModeType {
        SOUND, VIBRATE, SILENT
    }

    data class SoundMode(val mode: SoundModeType = SoundModeType.SOUND) : Action {
        override val title: Int get() = R.string.diy_action_sound_mode
        override val icon: Int get() = when (mode) {
            SoundModeType.SOUND -> R.drawable.rounded_volume_up_24
            SoundModeType.VIBRATE -> R.drawable.rounded_mobile_vibrate_24
            SoundModeType.SILENT -> R.drawable.rounded_volume_off_24
        }
        override val permissions: List<String> = listOf("notification_policy")
        override val isConfigurable: Boolean = true
    }
}
