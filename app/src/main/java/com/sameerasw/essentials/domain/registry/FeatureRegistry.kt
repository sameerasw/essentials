package com.sameerasw.essentials.domain.registry

import android.content.Context
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.Feature
import com.sameerasw.essentials.domain.model.SearchSetting
import com.sameerasw.essentials.viewmodels.MainViewModel

object FeatureRegistry {
    val ALL_FEATURES = listOf(
        object : Feature(
            id = "Screen off widget",
            title = "Screen off widget",
            iconRes = R.drawable.rounded_settings_power_24,
            category = "Tools",
            description = "Invisible widget to turn the screen off",
            permissionKeys = listOf("ACCESSIBILITY"),
            searchableSettings = listOf(
                SearchSetting(
                    "Widget Haptic feedback",
                    "Pick haptic feedback for widget taps",
                    "haptic_picker",
                    listOf("vibration", "touch", "feel")
                )
            ),
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Statusbar icons",
            title = "Statusbar icons",
            iconRes = R.drawable.rounded_interests_24,
            category = "Visuals",
            description = "Control statusbar icons visibility",
            permissionKeys = listOf("WRITE_SECURE_SETTINGS"),
            searchableSettings = listOf(
                SearchSetting(
                    "Smart WiFi",
                    "Hide mobile data when WiFi is connected",
                    "smart_wifi",
                    listOf("network", "visibility", "auto", "hide")
                ),
                SearchSetting(
                    "Smart Data",
                    "Hide mobile data in certain modes",
                    "smart_data",
                    listOf("network", "visibility", "auto", "hide")
                ),
                SearchSetting(
                    "Reset All Icons",
                    "Reset status bar icon visibility to default",
                    "reset_icons",
                    listOf("restore", "default", "icon")
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isStatusBarIconControlEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) = viewModel.isWriteSecureSettingsEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setStatusBarIconControlEnabled(enabled, context)
        },

        object : Feature(
            id = "Caffeinate",
            title = "Caffeinate",
            iconRes = R.drawable.rounded_coffee_24,
            category = "Tools",
            description = "Keep the screen awake",
            permissionKeys = listOf("POST_NOTIFICATIONS"),
            searchableSettings = listOf(
                SearchSetting(
                    "Show notification",
                    "Show a persistent notification when Caffeinate is active",
                    "show_notification",
                    listOf("visible", "alert")
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isCaffeinateActive.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {
                if (enabled) viewModel.startCaffeinate(context) else viewModel.stopCaffeinate(context)
            }
        },

        object : Feature(
            id = "Maps power saving mode",
            title = "Maps power saving mode",
            iconRes = R.drawable.rounded_navigation_24,
            category = "Tools",
            description = "For any Android device",
            permissionKeys = listOf("SHIZUKU", "NOTIFICATION_LISTENER"),
            hasMoreSettings = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isMapsPowerSavingEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) =
                viewModel.isShizukuAvailable.value && viewModel.isShizukuPermissionGranted.value && viewModel.isNotificationListenerEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setMapsPowerSavingEnabled(enabled, context)
            override fun onClick(context: Context, viewModel: MainViewModel) {}
        },

        object : Feature(
            id = "Notification lighting",
            title = "Notification lighting",
            iconRes = R.drawable.rounded_magnify_fullscreen_24,
            category = "Visuals",
            description = "Light up for notifications",
            permissionKeys = listOf("DRAW_OVERLAYS", "ACCESSIBILITY", "NOTIFICATION_LISTENER"),
            searchableSettings = listOf(
                SearchSetting(
                    "Lighting Style",
                    "Choose between Stroke, Glow, Spinner, and more",
                    "style",
                    listOf("animation", "visual", "look")
                ),
                SearchSetting(
                    "Corner radius",
                    "Adjust the corner radius of the notification lighting",
                    "corner_radius",
                    listOf("round", "shape", "edge")
                ),
                SearchSetting(
                    "Skip silent notifications",
                    "Do not show lighting for silent notifications",
                    "skip_silent_notifications",
                    listOf("quiet", "ignore", "filter")
                ),
                SearchSetting(
                    "Flashlight pulse",
                    "Slowly pulse flashlight for new notifications",
                    "flashlight_pulse",
                    listOf("light", "torch", "pulse", "notification")
                ),
                SearchSetting(
                    "Only while facing down",
                    "Pulse flashlight only when device is face down",
                    "flashlight_pulse_facedown",
                    listOf("proximity", "sensor", "face", "down")
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isNotificationLightingEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) =
                viewModel.isOverlayPermissionGranted.value && viewModel.isNotificationLightingAccessibilityEnabled.value && viewModel.isNotificationListenerEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setNotificationLightingEnabled(enabled, context)
        },

        object : Feature(
            id = "Sound mode tile",
            title = "Sound mode tile",
            iconRes = R.drawable.rounded_volume_up_24,
            category = "Tools",
            description = "QS tile to toggle sound mode",
            permissionKeys = listOf("WRITE_SECURE_SETTINGS"),
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Link actions",
            title = "Link actions",
            iconRes = R.drawable.rounded_link_24,
            category = "Tools",
            description = "Handle links with multiple apps",
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Snooze system notifications",
            title = "Snooze system notifications",
            iconRes = R.drawable.rounded_snooze_24,
            category = "Tools",
            description = "Snooze persistent notifications",
            permissionKeys = listOf("NOTIFICATION_LISTENER"),
            showToggle = false,
            searchableSettings = listOf(
                SearchSetting(
                    "Disable debugging notifications",
                    "Hide persistent ADB/USB debugging notifications",
                    "snooze_debugging",
                    listOf("adb", "usb", "debug")
                ),
                SearchSetting(
                    "Disable file transfer notification",
                    "Hide persistent USB file transfer notifications",
                    "snooze_file_transfer",
                    listOf("usb", "file", "transfer", "mtp")
                ),
                SearchSetting(
                    "Disable charging notification",
                    "Hide system charging notifications",
                    "snooze_charging",
                    listOf("battery", "charge", "power")
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) = viewModel.isNotificationListenerEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Quick settings tiles",
            title = "Quick settings tiles",
            iconRes = R.drawable.rounded_tile_small_24,
            category = "System",
            description = "View all",
            showToggle = false,
            searchableSettings = listOf(
                SearchSetting(
                    "UI Blur",
                    "Toggle system-wide UI blur",
                    "UI Blur",
                    listOf("blur", "glass", "vignette", "tile", "qs"),
                    "Quick Settings"
                ),
                SearchSetting(
                    "Bubbles",
                    "Enable floating window bubbles",
                    "Bubbles",
                    listOf("float", "window", "overlay", "tile", "qs"),
                    "Quick Settings"
                ),
                SearchSetting(
                    "Sensitive Content",
                    "Hide notification details on lockscreen",
                    "Sensitive Content",
                    listOf("privacy", "lock", "secure", "tile", "qs"),
                    "Quick Settings"
                ),
                SearchSetting(
                    "Tap to Wake",
                    "Double tap to wake control",
                    "Tap to Wake",
                    listOf("touch", "wake", "display", "tile", "qs"),
                    "Quick Settings"
                ),
                SearchSetting(
                    "AOD",
                    "Always On Display toggle",
                    "AOD",
                    listOf("always", "display", "clock", "tile", "qs"),
                    "Quick Settings"
                ),
                SearchSetting(
                    "Caffeinate",
                    "Keep screen awake toggle",
                    "Caffeinate",
                    listOf("stay", "on", "timeout", "tile", "qs"),
                    "Quick Settings"
                ),
                SearchSetting(
                    "Sound Mode",
                    "Cycle sound modes (Ring/Vibrate/Silent)",
                    "Sound Mode",
                    listOf("audio", "mute", "volume", "tile", "qs"),
                    "Quick Settings"
                ),
                SearchSetting(
                    "Notification Lighting",
                    "Toggle notification lighting service",
                    "Notification Lighting",
                    listOf("glow", "notification", "led", "tile", "qs"),
                    "Quick Settings"
                ),
                SearchSetting(
                    "Dynamic Night Light",
                    "Night light automation toggle",
                    "Dynamic Night Light",
                    listOf("blue", "filter", "auto", "tile", "qs"),
                    "Quick Settings"
                ),
                SearchSetting(
                    "Locked Security",
                    "Network security on lockscreen toggle",
                    "Locked Security",
                    listOf("wifi", "data", "lock", "tile", "qs"),
                    "Quick Settings"
                ),
                SearchSetting(
                    "Mono Audio",
                    "Force mono audio output toggle",
                    "Mono Audio",
                    listOf("sound", "accessibility", "hear", "tile", "qs"),
                    "Quick Settings"
                ),
                SearchSetting(
                    "Flashlight",
                    "Dedicated flashlight toggle",
                    "Flashlight",
                    listOf("light", "torch", "tile", "qs"),
                    "Quick Settings"
                ),
                SearchSetting(
                    "App Freezing",
                    "Launch app freezing grid",
                    "App Freezing",
                    listOf("freeze", "shizuku", "tile", "qs"),
                    "Quick Settings"
                ),
                SearchSetting(
                    "Flashlight Pulse",
                    "Toggle notification flashlight pulse",
                    "Flashlight Pulse",
                    listOf("light", "torch", "pulse", "notification", "tile", "qs"),
                    "Quick Settings"
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Button remap",
            title = "Button remap",
            iconRes = R.drawable.rounded_switch_access_3_24,
            category = "System",
            description = "Remap hardware button actions",
            permissionKeys = listOf("ACCESSIBILITY"),
            showToggle = false,
            searchableSettings = listOf(
                SearchSetting(
                    "Enable Button Remap",
                    "Master toggle for volume button remapping",
                    "enable_remap",
                    listOf("switch", "master")
                ),
                SearchSetting(
                    "Remap Haptic Feedback",
                    "Vibration feedback when remapped button is pressed",
                    "remap_haptic",
                    listOf("vibration", "feel")
                ),
                SearchSetting(
                    "Flashlight toggle",
                    "Toggle flashlight with volume buttons",
                    "flashlight_toggle",
                    listOf("light", "torch")
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) = viewModel.isAccessibilityEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setButtonRemapEnabled(enabled, context)
        },

        object : Feature(
            id = "Dynamic night light",
            title = "Dynamic night light",
            iconRes = R.drawable.rounded_nightlight_24,
            category = "Visuals",
            description = "Toggle night light based on app",
            permissionKeys = listOf("ACCESSIBILITY", "WRITE_SECURE_SETTINGS"),
            searchableSettings = listOf(
                SearchSetting(
                    "Enable Dynamic Night Light",
                    "Master switch for dynamic night light",
                    "dynamic_night_light_toggle",
                    listOf("switch", "master")
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isDynamicNightLightEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) =
                viewModel.isAccessibilityEnabled.value && viewModel.isWriteSecureSettingsEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setDynamicNightLightEnabled(enabled, context)
        },



        object : Feature(
            id = "Screen locked security",
            title = "Screen locked security",
            iconRes = R.drawable.rounded_security_24,
            category = "Security and Privacy",
            description = "Prevent network controls",
            permissionKeys = listOf("ACCESSIBILITY", "WRITE_SECURE_SETTINGS", "DEVICE_ADMIN")
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isScreenLockedSecurityEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) =
                viewModel.isAccessibilityEnabled.value && viewModel.isWriteSecureSettingsEnabled.value && viewModel.isDeviceAdminEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setScreenLockedSecurityEnabled(enabled, context)
        },

        object : Feature(
            id = "App lock",
            title = "App lock",
            iconRes = R.drawable.rounded_shield_lock_24,
            category = "Security and Privacy",
            description = "Secure apps with biometrics",
            permissionKeys = listOf("ACCESSIBILITY"),
            searchableSettings = listOf(
                SearchSetting(
                    "Enable app lock",
                    "Master toggle for app locking",
                    "app_lock_enabled",
                    listOf("secure", "privacy", "biometric", "face", "fingerprint")
                ),
                SearchSetting(
                    "Select locked apps",
                    "Choose which apps require authentication",
                    "app_lock_selected_apps",
                    listOf("list", "picker", "selection")
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isAppLockEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) = viewModel.isAccessibilityEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setAppLockEnabled(enabled, context)
        },

        object : Feature(
            id = "Freeze",
            title = "Freeze",
            iconRes = R.drawable.rounded_mode_cool_24,
            category = "Tools",
            description = "Disable rarely used apps",
            permissionKeys = listOf("SHIZUKU"),
            searchableSettings = listOf(
                SearchSetting(
                    "Pick apps to freeze",
                    "Choose which apps can be frozen",
                    "freeze_selected_apps",
                    listOf("list", "picker", "selection")
                ),
                SearchSetting(
                    "Freeze all apps",
                    "Immediately freeze all picked apps",
                    "freeze_all_manual",
                    listOf("manual", "now", "shizuku")
                ),
                SearchSetting(
                    "Freeze when locked",
                    "Freeze selected apps when device locks",
                    "freeze_when_locked_enabled",
                    listOf("automation", "auto", "lock")
                ),
                SearchSetting(
                    "Freeze delay",
                    "Delay before freezing after locking",
                    "freeze_lock_delay_index",
                    listOf("timer", "wait", "timeout")
                )
            ),
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) =
                viewModel.isShizukuAvailable.value && viewModel.isShizukuPermissionGranted.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        }
    )
}