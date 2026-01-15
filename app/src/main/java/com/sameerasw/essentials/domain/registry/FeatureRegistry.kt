package com.sameerasw.essentials.domain.registry

import android.content.Context
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.Feature
import com.sameerasw.essentials.domain.model.SearchSetting
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

object FeatureRegistry {
    val ALL_FEATURES = listOf(
        object : Feature(
            id = "Screen off widget",
            title = R.string.feat_screen_off_widget_title,
            iconRes = R.drawable.rounded_settings_power_24,
            category = R.string.cat_tools,
            description = R.string.feat_screen_off_widget_desc,
            permissionKeys = listOf("ACCESSIBILITY"),
            searchableSettings = listOf(
                SearchSetting(
                    R.string.search_haptic_title,
                    R.string.search_haptic_desc,
                    "haptic_picker",
                    R.array.keywords_haptic
                )
            ),
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Statusbar icons",
            title = R.string.feat_statusbar_icons_title,
            iconRes = R.drawable.rounded_interests_24,
            category = R.string.cat_visuals,
            description = R.string.feat_statusbar_icons_desc,
            permissionKeys = listOf("WRITE_SECURE_SETTINGS"),
            searchableSettings = listOf(
                SearchSetting(
                    R.string.search_smart_wifi_title,
                    R.string.search_smart_wifi_desc,
                    "smart_wifi",
                    R.array.keywords_network_visibility
                ),
                SearchSetting(
                    R.string.search_smart_data_title,
                    R.string.search_smart_data_desc,
                    "smart_data",
                    R.array.keywords_network_visibility
                ),
                SearchSetting(
                    R.string.search_reset_icons_title,
                    R.string.search_reset_icons_desc,
                    "reset_icons",
                    R.array.keywords_restore_default
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isStatusBarIconControlEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) = viewModel.isWriteSecureSettingsEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setStatusBarIconControlEnabled(enabled, context)
        },

        object : Feature(
            id = "Caffeinate",
            title = R.string.feat_caffeinate_title,
            iconRes = R.drawable.rounded_coffee_24,
            category = R.string.cat_tools,
            description = R.string.feat_caffeinate_desc,
            permissionKeys = listOf("POST_NOTIFICATIONS"),
            searchableSettings = listOf(
                SearchSetting(
                    title = R.string.search_caffeinate_abort_screen_off_title,
                    description = R.string.search_caffeinate_abort_screen_off_desc,
                    targetSettingHighlightKey = "abort_screen_off"
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
            title = R.string.feat_maps_power_saving_title,
            iconRes = R.drawable.rounded_navigation_24,
            category = R.string.cat_tools,
            description = R.string.feat_maps_power_saving_desc,
            permissionKeys = if (ShellUtils.isRootEnabled(com.sameerasw.essentials.EssentialsApp.context)) listOf("ROOT", "NOTIFICATION_LISTENER") else listOf("SHIZUKU", "NOTIFICATION_LISTENER"),
            hasMoreSettings = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isMapsPowerSavingEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) =
                com.sameerasw.essentials.utils.ShellUtils.hasPermission(context) && viewModel.isNotificationListenerEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setMapsPowerSavingEnabled(enabled, context)
            override fun onClick(context: Context, viewModel: MainViewModel) {}
        },

        object : Feature(
            id = "Notification lighting",
            title = R.string.feat_notification_lighting_title,
            iconRes = R.drawable.rounded_magnify_fullscreen_24,
            category = R.string.cat_visuals,
            description = R.string.feat_notification_lighting_desc,
            permissionKeys = listOf("DRAW_OVERLAYS", "ACCESSIBILITY", "NOTIFICATION_LISTENER"),
            searchableSettings = listOf(
                SearchSetting(
                    R.string.search_lighting_style_title,
                    R.string.search_lighting_style_desc,
                    "style",
                    R.array.keywords_visual_style
                ),
                SearchSetting(
                    R.string.search_corner_radius_title,
                    R.string.search_corner_radius_desc,
                    "corner_radius",
                    R.array.keywords_round_shape
                ),
                SearchSetting(
                    R.string.search_skip_silent_title,
                    R.string.search_skip_silent_desc,
                    "skip_silent_notifications",
                    R.array.keywords_quiet_filter
                ),
                SearchSetting(
                    R.string.search_flashlight_pulse_title,
                    R.string.search_flashlight_pulse_desc,
                    "flashlight_pulse",
                    R.array.keywords_flashlight_pulse
                ),
                SearchSetting(
                    R.string.search_only_facing_down_title,
                    R.string.search_only_facing_down_desc,
                    "flashlight_pulse_facedown",
                    R.array.keywords_proximity_sensor
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
            title = R.string.feat_sound_mode_tile_title,
            iconRes = R.drawable.rounded_volume_up_24,
            category = R.string.cat_tools,
            description = R.string.feat_sound_mode_tile_desc,
            permissionKeys = listOf("WRITE_SECURE_SETTINGS"),
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },



        object : Feature(
            id = "Link actions",
            title = R.string.feat_link_actions_title,
            iconRes = R.drawable.rounded_link_24,
            category = R.string.cat_tools,
            description = R.string.feat_link_actions_desc,
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Snooze system notifications",
            title = R.string.feat_snooze_notifications_title,
            iconRes = R.drawable.rounded_snooze_24,
            category = R.string.cat_tools,
            description = R.string.feat_snooze_notifications_desc,
            permissionKeys = listOf("NOTIFICATION_LISTENER"),
            showToggle = false,
            searchableSettings = listOf(
                SearchSetting(
                    R.string.search_snooze_debug_title,
                    R.string.search_snooze_debug_desc,
                    "snooze_debugging",
                    R.array.keywords_adb_debug
                ),
                SearchSetting(
                    R.string.search_snooze_file_title,
                    R.string.search_snooze_file_desc,
                    "snooze_file_transfer",
                    R.array.keywords_mtp
                ),
                SearchSetting(
                    R.string.search_snooze_charge_title,
                    R.string.search_snooze_charge_desc,
                    "snooze_charging",
                    R.array.keywords_battery_charge
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) = viewModel.isNotificationListenerEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Quick settings tiles",
            title = R.string.feat_qs_tiles_title,
            iconRes = R.drawable.rounded_tile_small_24,
            category = R.string.cat_system,
            description = R.string.feat_qs_tiles_desc,
            showToggle = false,
            searchableSettings = listOf(
                SearchSetting(
                    R.string.search_qs_blur_title,
                    R.string.search_qs_blur_desc,
                    "UI Blur",
                    R.array.keywords_blur_glass,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_bubbles_title,
                    R.string.search_qs_bubbles_desc,
                    "Bubbles",
                    R.array.keywords_float_window,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_sensitive_title,
                    R.string.search_qs_sensitive_desc,
                    "Sensitive Content",
                    R.array.keywords_privacy,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_wake_title,
                    R.string.search_qs_wake_desc,
                    "Tap to Wake",
                    R.array.keywords_wake_display,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_aod_title,
                    R.string.search_qs_aod_desc,
                    "AOD",
                    R.array.keywords_always_display,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_caffeinate_title,
                    R.string.search_qs_caffeinate_desc,
                    "Caffeinate",
                    R.array.keywords_timeout,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_sound_title,
                    R.string.search_qs_sound_desc,
                    "Sound Mode",
                    R.array.keywords_audio_mute,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_lighting_title,
                    R.string.search_qs_lighting_desc,
                    "Notification Lighting",
                    R.array.keywords_notification_lighting,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_night_light_title,
                    R.string.search_qs_night_light_desc,
                    "Dynamic Night Light",
                    R.array.keywords_blue_filter,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_locked_sec_title,
                    R.string.search_qs_locked_sec_desc,
                    "Locked Security",
                    R.array.keywords_network_visibility,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_mono_title,
                    R.string.search_qs_mono_desc,
                    "Mono Audio",
                    R.array.keywords_sound_accessibility,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_flashlight_title,
                    R.string.search_qs_flashlight_desc,
                    "Flashlight",
                    R.array.keywords_flashlight,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_freeze_title,
                    R.string.search_qs_freeze_desc,
                    "App Freezing",
                    R.array.keywords_app_freezing,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_pulse_title,
                    R.string.search_qs_pulse_desc,
                    "Flashlight Pulse",
                    R.array.keywords_flashlight_pulse,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.tile_stay_awake,
                    R.string.search_qs_stay_awake_desc,
                    "Stay awake",
                    R.array.keywords_qs_stay_awake,
                    R.string.feat_qs_tiles_title
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Button remap",
            title = R.string.feat_button_remap_title,
            iconRes = R.drawable.rounded_switch_access_3_24,
            category = R.string.cat_system,
            description = R.string.feat_button_remap_desc,
            permissionKeys = if (ShellUtils.isRootEnabled(com.sameerasw.essentials.EssentialsApp.context)) listOf("ACCESSIBILITY", "ROOT") else listOf("ACCESSIBILITY", "SHIZUKU"),
            showToggle = false,
            searchableSettings = listOf(
                SearchSetting(
                    R.string.search_remap_enable_title,
                    R.string.search_remap_enable_desc,
                    "enable_remap",
                    R.array.keywords_switch_master
                ),
                SearchSetting(
                    R.string.search_remap_haptic_title,
                    R.string.search_remap_haptic_desc,
                    "remap_haptic",
                    R.array.keywords_vibration
                ),
                SearchSetting(
                    R.string.search_remap_flashlight_title,
                    R.string.search_remap_flashlight_desc,
                    "flashlight_toggle",
                    R.array.keywords_flashlight
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) = viewModel.isAccessibilityEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setButtonRemapEnabled(enabled, context)
        },

        object : Feature(
            id = "Dynamic night light",
            title = R.string.feat_dynamic_night_light_title,
            iconRes = R.drawable.rounded_nightlight_24,
            category = R.string.cat_visuals,
            description = R.string.feat_dynamic_night_light_desc,
            permissionKeys = listOf("ACCESSIBILITY", "WRITE_SECURE_SETTINGS"),
            searchableSettings = listOf(
                SearchSetting(
                    R.string.search_night_light_enable_title,
                    R.string.search_night_light_enable_desc,
                    "dynamic_night_light_toggle",
                    R.array.keywords_switch_master
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
            title = R.string.feat_screen_locked_security_title,
            iconRes = R.drawable.rounded_security_24,
            category = R.string.cat_security,
            description = R.string.feat_screen_locked_security_desc,
            permissionKeys = listOf("ACCESSIBILITY", "WRITE_SECURE_SETTINGS", "DEVICE_ADMIN")
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isScreenLockedSecurityEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) =
                viewModel.isAccessibilityEnabled.value && viewModel.isWriteSecureSettingsEnabled.value && viewModel.isDeviceAdminEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setScreenLockedSecurityEnabled(enabled, context)
        },

        object : Feature(
            id = "App lock",
            title = R.string.feat_app_lock_title,
            iconRes = R.drawable.rounded_shield_lock_24,
            category = R.string.cat_security,
            description = R.string.feat_app_lock_desc,
            permissionKeys = listOf("ACCESSIBILITY"),
            searchableSettings = listOf(
                SearchSetting(
                    R.string.search_app_lock_enable_title,
                    R.string.search_app_lock_enable_desc,
                    "app_lock_enabled",
                    R.array.keywords_privacy
                ),
                SearchSetting(
                    R.string.search_app_lock_pick_title,
                    R.string.search_app_lock_pick_desc,
                    "app_lock_selected_apps",
                    R.array.keywords_selection
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isAppLockEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) = viewModel.isAccessibilityEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setAppLockEnabled(enabled, context)
        },

        object : Feature(
            id = "Location reached",
            title = R.string.feat_location_reached_title,
            iconRes = R.drawable.rounded_navigation_24,
            category = R.string.cat_tools,
            description = R.string.feat_location_reached_desc,
            permissionKeys = listOf("LOCATION", "BACKGROUND_LOCATION", "USE_FULL_SCREEN_INTENT"),
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Freeze",
            title = R.string.feat_freeze_title,
            iconRes = R.drawable.rounded_mode_cool_24,
            category = R.string.cat_tools,
            description = R.string.feat_freeze_desc,
            permissionKeys = if (ShellUtils.isRootEnabled(com.sameerasw.essentials.EssentialsApp.context)) listOf("ROOT") else listOf("SHIZUKU"),
            searchableSettings = listOf(
                SearchSetting(
                    R.string.search_freeze_pick_title,
                    R.string.search_freeze_pick_desc,
                    "freeze_selected_apps",
                    R.array.keywords_selection
                ),
                SearchSetting(
                    R.string.search_freeze_all_title,
                    R.string.search_freeze_all_desc,
                    "freeze_all_manual",
                    R.array.keywords_manual_now
                ),
                SearchSetting(
                    R.string.search_freeze_locked_title,
                    R.string.search_freeze_locked_desc,
                    "freeze_when_locked_enabled",
                    R.array.keywords_automation_lock
                ),
                SearchSetting(
                    R.string.search_freeze_delay_title,
                    R.string.search_freeze_delay_desc,
                    "freeze_lock_delay_index",
                    R.array.keywords_timer
                )
            )
,
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) =
                com.sameerasw.essentials.utils.ShellUtils.hasPermission(context)
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "System Keyboard",
            title = R.string.feat_system_keyboard_title,
            iconRes = R.drawable.rounded_keyboard_24,
            category = R.string.cat_system,
            description = R.string.feat_system_keyboard_desc,
            hasMoreSettings = true,
            showToggle = false,
            searchableSettings = listOf(
                SearchSetting(
                    R.string.search_keyboard_height_title,
                    R.string.search_keyboard_height_desc,
                    "keyboard_height",
                    R.array.keywords_keyboard
                ),
                SearchSetting(
                    R.string.search_keyboard_padding_title,
                    R.string.search_keyboard_padding_desc,
                    "keyboard_bottom_padding",
                    R.array.keywords_keyboard
                ),
                SearchSetting(
                    R.string.search_keyboard_haptics_title,
                    R.string.search_keyboard_haptics_desc,
                    "keyboard_haptics",
                    R.array.keywords_vibration
                )
            )
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        }
    )
}