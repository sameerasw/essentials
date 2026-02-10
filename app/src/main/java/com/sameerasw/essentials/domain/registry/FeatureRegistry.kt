package com.sameerasw.essentials.domain.registry

import android.content.Context
import android.content.Intent
import com.sameerasw.essentials.EssentialsApp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.Feature
import com.sameerasw.essentials.domain.model.SearchSetting
import com.sameerasw.essentials.ui.activities.WatermarkActivity
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

object FeatureRegistry {
    val ALL_FEATURES = listOf(
        // Sound Group Children
        object : Feature(
            id = "Sound mode tile",
            title = R.string.feat_sound_modes_title, // Renamed
            iconRes = R.drawable.rounded_volume_up_24,
            category = R.string.cat_system,
            description = R.string.feat_sound_modes_desc,
            aboutDescription = R.string.about_desc_sound_mode_tile,
            permissionKeys = listOf("NOTIFICATION_POLICY"),
            searchableSettings = listOf(
                SearchSetting(
                    R.string.search_sound_mode_show_slider_title, 
                    R.string.search_sound_mode_show_slider_desc, 
                    "sound_mode_show_slider"
                ),
                SearchSetting(
                    R.string.search_sound_mode_behavior_title, 
                    R.string.search_sound_mode_behavior_desc, 
                    "sound_mode_cycle_behavior"
                )
            ),
            showToggle = false,
            parentFeatureId = "Sound"
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Ambient music glance",
            title = R.string.feat_ambient_music_title,
            iconRes = R.drawable.rounded_music_note_24,
            category = R.string.cat_interface,
            description = R.string.feat_ambient_music_desc,
            aboutDescription = R.string.about_desc_ambient_music_glance,
            permissionKeys = listOf("ACCESSIBILITY", "NOTIFICATION_LISTENER"),
            showToggle = true,
            isBeta = true,
            parentFeatureId = "Sound"
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isAmbientMusicGlanceEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) = 
                viewModel.isNotificationListenerEnabled.value && viewModel.isAccessibilityEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setAmbientMusicGlanceEnabled(enabled)
        },

        object : Feature(
            id = "Call vibrations",
            title = R.string.feat_call_vibrations_title,
            iconRes = R.drawable.rounded_mobile_vibrate_24,
            category = R.string.cat_system,
            description = R.string.feat_call_vibrations_desc,
            aboutDescription = R.string.about_desc_call_vibrations,
            permissionKeys = listOf("READ_PHONE_STATE", "NOTIFICATION_LISTENER"),
            hasMoreSettings = false,
            parentFeatureId = "Sound"
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isCallVibrationsEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) = 
                viewModel.isReadPhoneStateEnabled.value && viewModel.isNotificationListenerEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setCallVibrationsEnabled(enabled)
            override fun onClick(context: Context, viewModel: MainViewModel) {}
        },
        object : Feature(
            id = "Sound",
            title = R.string.feat_sound_haptics_title,
            iconRes = R.drawable.rounded_mobile_sound_24,
            category = R.string.cat_system,
            description = R.string.feat_sound_haptics_desc,
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },
        object : Feature(
            id = "Security",
            title = R.string.feat_security_privacy_title,
            iconRes = R.drawable.rounded_security_24,
            category = R.string.cat_system,
            description = R.string.feat_security_privacy_desc,
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },
        object : Feature(
            id = "Notifications",
            title = R.string.feat_notifications_alerts_title,
            iconRes = R.drawable.rounded_notification_sound_24,
            category = R.string.cat_system,
            description = R.string.feat_notifications_alerts_desc,
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },
        object : Feature(
            id = "Input",
            title = R.string.feat_input_actions_title,
            iconRes = R.drawable.rounded_mobile_hand_24,
            category = R.string.cat_interaction,
            description = R.string.feat_input_actions_desc,
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },
        object : Feature(
            id = "Widgets",
            title = R.string.feat_widgets_title,
            iconRes = R.drawable.rounded_widgets_24,
            category = R.string.cat_interface,
            description = R.string.feat_widgets_desc,
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },
        object : Feature(
            id = "Display",
            title = R.string.feat_display_visuals_title,
            iconRes = R.drawable.rounded_mobile_layout_24,
            category = R.string.cat_interface,
            description = R.string.feat_display_visuals_desc,
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },
        object : Feature(
            id = "Watch",
            title = R.string.feat_watch_title,
            iconRes = R.drawable.rounded_watch_24,
            category = R.string.cat_tools,
            description = R.string.feat_watch_desc,
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Screen off widget",
            title = R.string.feat_screen_off_widget_title,
            iconRes = R.drawable.rounded_widgets_24,
            category = R.string.cat_interface,
            description = R.string.feat_screen_off_widget_desc,
            aboutDescription = R.string.about_desc_screen_off_widget,
            permissionKeys = listOf("ACCESSIBILITY"),
            searchableSettings = listOf(
                SearchSetting(
                    R.string.search_haptic_title,
                    R.string.search_haptic_desc,
                    "haptic_picker",
                    R.array.keywords_haptic
                )
            ),

            showToggle = false,
            parentFeatureId = "Widgets"
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Statusbar icons",
            title = R.string.feat_statusbar_icons_title,
            iconRes = R.drawable.rounded_signal_cellular_alt_24, // Use requested icon
            category = R.string.cat_system,
            description = R.string.feat_statusbar_icons_desc,
            aboutDescription = R.string.about_desc_statusbar_icons,
            permissionKeys = listOf("WRITE_SECURE_SETTINGS", "WRITE_SETTINGS"),
            searchableSettings = listOf(
                SearchSetting(R.string.search_smart_wifi_title, R.string.search_smart_wifi_desc, "smart_wifi", R.array.keywords_network_visibility),
                SearchSetting(R.string.search_smart_data_title, R.string.search_smart_data_desc, "smart_data", R.array.keywords_network_visibility),
                SearchSetting(R.string.search_reset_icons_title, R.string.search_reset_icons_desc, "reset_icons", R.array.keywords_restore_default),
                SearchSetting(R.string.search_clock_seconds_title, R.string.search_clock_seconds_desc, "clock_seconds"),
                SearchSetting(R.string.search_battery_percentage_title, R.string.search_battery_percentage_desc, "battery_percentage"),
                SearchSetting(R.string.search_privacy_chip_title, R.string.search_privacy_chip_desc, "privacy_chip")
            ),
            showToggle = false,
            parentFeatureId = "Display"
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
            aboutDescription = R.string.about_desc_caffeinate,
            permissionKeys = listOf("POST_NOTIFICATIONS"),
            searchableSettings = listOf(
                SearchSetting(
                    title = R.string.search_caffeinate_abort_screen_off_title,
                    description = R.string.search_caffeinate_abort_screen_off_desc,
                    targetSettingHighlightKey = "abort_screen_off"
                )
            ),
            showToggle = true,
            parentFeatureId = "Display"
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
            aboutDescription = R.string.about_desc_maps_power_saving,
            permissionKeys = if (ShellUtils.isRootEnabled(EssentialsApp.context)) listOf("ROOT", "NOTIFICATION_LISTENER") else listOf("SHIZUKU", "NOTIFICATION_LISTENER"),
            hasMoreSettings = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isMapsPowerSavingEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) =
                ShellUtils.hasPermission(context) && viewModel.isNotificationListenerEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setMapsPowerSavingEnabled(enabled, context)
            override fun onClick(context: Context, viewModel: MainViewModel) {}
        },

        object : Feature(
            id = "Notification lighting",
            title = R.string.feat_notification_lighting_title,
            iconRes = R.drawable.rounded_magnify_fullscreen_24,
            category = R.string.cat_interface,
            description = R.string.feat_notification_lighting_desc,
            permissionKeys = listOf("DRAW_OVERLAYS", "ACCESSIBILITY", "NOTIFICATION_LISTENER"),
            aboutDescription = R.string.about_desc_notification_lighting,
            searchableSettings = listOf(
                SearchSetting(R.string.search_lighting_style_title, R.string.search_lighting_style_desc, "style", R.array.keywords_visual_style),
                SearchSetting(R.string.search_corner_radius_title, R.string.search_corner_radius_desc, "corner_radius", R.array.keywords_round_shape),
                SearchSetting(R.string.search_skip_silent_title, R.string.search_skip_silent_desc, "skip_silent_notifications", R.array.keywords_quiet_filter)
            ),
            showToggle = true,
            parentFeatureId = "Notifications"
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isNotificationLightingEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) =
                viewModel.isOverlayPermissionGranted.value && viewModel.isNotificationLightingAccessibilityEnabled.value && viewModel.isNotificationListenerEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setNotificationLightingEnabled(enabled, context)
        },

        object : Feature(
            id = "Flashlight pulse",
            title = R.string.flashlight_pulse_title,
            iconRes = R.drawable.rounded_flashlight_on_24,
            category = R.string.cat_system,
            description = R.string.feat_flashlight_pulse_desc,
            aboutDescription = R.string.about_desc_flashlight_pulse,
            permissionKeys = listOf("NOTIFICATION_LISTENER"),
            searchableSettings = listOf(
                SearchSetting(R.string.search_flashlight_pulse_title, R.string.search_flashlight_pulse_desc, "flashlight_pulse", R.array.keywords_flashlight_pulse),
                SearchSetting(R.string.search_only_facing_down_title, R.string.search_only_facing_down_desc, "flashlight_pulse_facedown", R.array.keywords_proximity_sensor)
            ),
            showToggle = true,
            parentFeatureId = "Notifications"
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isFlashlightPulseEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) =
                viewModel.isNotificationListenerEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setFlashlightPulseEnabled(enabled, context)
        },




        object : Feature(
            id = "Link actions",
            title = R.string.feat_link_actions_title,
            iconRes = R.drawable.rounded_link_24,
            category = R.string.cat_interaction,
            description = R.string.feat_link_actions_desc,
            showToggle = false,
            parentFeatureId = "Input"
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Snooze system notifications",
            title = R.string.feat_snooze_notifications_title,
            iconRes = R.drawable.rounded_snooze_24,
            category = R.string.cat_interface,
            description = R.string.feat_snooze_notifications_desc,
            aboutDescription = R.string.about_desc_snooze_notifications,
            permissionKeys = listOf("NOTIFICATION_LISTENER"),
            showToggle = true,
            searchableSettings = emptyList(),
            parentFeatureId = "Notifications"
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isSnoozeHeadsUpEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) = viewModel.isNotificationListenerEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setSnoozeHeadsUpEnabled(
                enabled,
                context = context
            )
        },

        object : Feature(
            id = "Quick settings tiles",
            title = R.string.feat_qs_tiles_title,
            iconRes = R.drawable.rounded_tile_small_24,
            category = R.string.cat_system,
            description = R.string.feat_qs_tiles_desc,
            aboutDescription = R.string.about_desc_quick_settings_tiles,
            permissionKeys = listOf("WRITE_SETTINGS"),
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
                ),
                SearchSetting(
                    R.string.search_qs_private_dns_title,
                    R.string.search_qs_private_dns_desc,
                    "Private DNS",
                    R.array.keywords_network_visibility,
                    R.string.feat_qs_tiles_title
                ),
                SearchSetting(
                    R.string.search_qs_usb_debugging_title,
                    R.string.search_qs_usb_debugging_desc,
                    "USB Debugging",
                    R.array.keywords_adb_debug,
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
            category = R.string.cat_interaction,
            description = R.string.feat_button_remap_desc,
            aboutDescription = R.string.about_desc_button_remap,
            permissionKeys = if (ShellUtils.isRootEnabled(EssentialsApp.context)) listOf("ACCESSIBILITY", "ROOT") else listOf("ACCESSIBILITY", "SHIZUKU"),
            showToggle = true,
            searchableSettings = listOf(
                SearchSetting(R.string.search_remap_enable_title, R.string.search_remap_enable_desc, "enable_remap", R.array.keywords_switch_master),
                SearchSetting(R.string.search_remap_haptic_title, R.string.search_remap_haptic_desc, "remap_haptic", R.array.keywords_vibration),
                SearchSetting(R.string.search_remap_flashlight_title, R.string.search_remap_flashlight_desc, "flashlight_toggle", R.array.keywords_flashlight)
            ),
            parentFeatureId = "Input"
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isButtonRemapEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) = viewModel.isAccessibilityEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setButtonRemapEnabled(enabled, context)
        },

        object : Feature(
            id = "Dynamic night light",
            title = R.string.feat_dynamic_night_light_title,
            iconRes = R.drawable.rounded_nightlight_24,
            category = R.string.cat_display,
            description = R.string.feat_dynamic_night_light_desc,
            aboutDescription = R.string.about_desc_dynamic_night_light,
            permissionKeys = listOf("ACCESSIBILITY", "WRITE_SECURE_SETTINGS"),
            searchableSettings = listOf(
                SearchSetting(R.string.search_night_light_enable_title, R.string.search_night_light_enable_desc, "dynamic_night_light_toggle", R.array.keywords_switch_master)
            ),
            showToggle = true,
            parentFeatureId = "Display"
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
            category = R.string.cat_protection,
            description = R.string.feat_screen_locked_security_desc,
            aboutDescription = R.string.about_desc_screen_locked_security,
            permissionKeys = listOf("ACCESSIBILITY", "WRITE_SECURE_SETTINGS", "DEVICE_ADMIN"),
            parentFeatureId = "Security"
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isScreenLockedSecurityEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) =
                viewModel.isAccessibilityEnabled.value && viewModel.isWriteSecureSettingsEnabled.value && viewModel.isDeviceAdminEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setScreenLockedSecurityEnabled(enabled, context)
        },

        object : Feature(
            id = "App lock",
            title = R.string.feat_app_lock_title,
            iconRes = R.drawable.rounded_apps_24,
            category = R.string.cat_protection,
            description = R.string.feat_app_lock_desc,
            aboutDescription = R.string.about_desc_app_lock,
            permissionKeys = listOf("ACCESSIBILITY"),
            searchableSettings = listOf(
                SearchSetting(R.string.search_app_lock_enable_title, R.string.search_app_lock_enable_desc, "app_lock_enabled", R.array.keywords_privacy),
                SearchSetting(R.string.search_app_lock_pick_title, R.string.search_app_lock_pick_desc, "app_lock_selected_apps", R.array.keywords_selection)
            ),
            parentFeatureId = "Security"
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
            aboutDescription = R.string.about_desc_location_reached,
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
            aboutDescription = R.string.about_desc_freeze,
            permissionKeys = if (ShellUtils.isRootEnabled(EssentialsApp.context)) listOf("ROOT", "USAGE_STATS", "NOTIFICATION_LISTENER") else listOf("SHIZUKU", "USAGE_STATS", "NOTIFICATION_LISTENER"),
            searchableSettings = listOf(
                SearchSetting(R.string.search_freeze_pick_title, R.string.search_freeze_pick_desc, "freeze_selected_apps", R.array.keywords_selection),
                SearchSetting(R.string.search_freeze_all_title, R.string.search_freeze_all_desc, "freeze_all_manual", R.array.keywords_manual_now),
                SearchSetting(R.string.search_freeze_locked_title, R.string.search_freeze_locked_desc, "freeze_when_locked_enabled", R.array.keywords_automation_lock),
                SearchSetting(R.string.search_freeze_delay_title, R.string.search_freeze_delay_desc, "freeze_lock_delay_index", R.array.keywords_timer)
            ),
            showToggle = false,
            isVisibleInMain = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) =
                ShellUtils.hasPermission(context)
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "System Keyboard",
            title = R.string.feat_system_keyboard_title,
            iconRes = R.drawable.rounded_keyboard_24,
            category = R.string.cat_system,
            description = R.string.feat_system_keyboard_desc,
            aboutDescription = R.string.about_desc_system_keyboard,
            hasMoreSettings = true,
            showToggle = false,
            searchableSettings = listOf(
                SearchSetting(R.string.search_keyboard_height_title, R.string.search_keyboard_height_desc, "keyboard_height", R.array.keywords_keyboard),
                SearchSetting(R.string.search_keyboard_padding_title, R.string.search_keyboard_padding_desc, "keyboard_bottom_padding", R.array.keywords_keyboard),
                SearchSetting(R.string.search_keyboard_haptics_title, R.string.search_keyboard_haptics_desc, "keyboard_haptics", R.array.keywords_vibration)
            ),
            parentFeatureId = "Input"
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Batteries",
            title = R.string.feat_batteries_title,
            iconRes = R.drawable.rounded_battery_charging_60_24,
            // "Batteries"
            category = R.string.cat_tools,
            description = R.string.feat_batteries_desc,
            aboutDescription = R.string.about_desc_batteries,
            permissionKeys = listOf("BLUETOOTH_CONNECT", "BLUETOOTH_SCAN"),
            showToggle = false,
            hasMoreSettings = true,
            isBeta = true,
            parentFeatureId = "Widgets"
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Watermark",
            title = R.string.feat_watermark_title,
            iconRes = R.drawable.rounded_draw_24,
            category = R.string.cat_tools,
            description = R.string.feat_watermark_desc,
            aboutDescription = R.string.about_desc_watermark,
            showToggle = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
            override fun onClick(context: Context, viewModel: MainViewModel) {
                context.startActivity(Intent(context, WatermarkActivity::class.java))
            }
        },

        object : Feature(
            id = "Calendar Sync",
            title = R.string.feat_calendar_sync_title,
            iconRes = R.drawable.rounded_sync_24, // Use sync icon
            category = R.string.cat_tools,
            description = R.string.feat_calendar_sync_desc,
            aboutDescription = R.string.about_desc_calendar_sync,
            permissionKeys = listOf("READ_CALENDAR"),
            parentFeatureId = "Watch"
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isCalendarSyncEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setCalendarSyncEnabled(enabled, context)
        },
        

        object : Feature(
            id = "Ambient music glance",
            title = R.string.feat_ambient_music_glance_title,
            iconRes = R.drawable.rounded_music_video_24,
            category = R.string.cat_visuals,
            description = R.string.feat_ambient_music_glance_desc,
            permissionKeys = listOf("ACCESSIBILITY", "NOTIFICATION_LISTENER"),
            hasMoreSettings = true,
            showToggle = true,
            isBeta = true,
            isVisibleInMain = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = viewModel.isAmbientMusicGlanceEnabled.value
            override fun isToggleEnabled(viewModel: MainViewModel, context: Context) = 
                viewModel.isNotificationListenerEnabled.value && viewModel.isAccessibilityEnabled.value
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) = viewModel.setAmbientMusicGlanceEnabled(enabled)
        },
        
        object : Feature(
            id = "App updates",
            title = R.string.feat_app_updates_title,
            iconRes = R.drawable.rounded_downloading_24,
            category = R.string.cat_tools,
            description = R.string.feat_app_updates_desc,
            aboutDescription = R.string.about_desc_app_updates,
            showToggle = false,
            isVisibleInMain = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = true
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
            override fun onClick(context: Context, viewModel: MainViewModel) {
                context.startActivity(android.content.Intent(context, com.sameerasw.essentials.AppUpdatesActivity::class.java))
            }
        },

        // QS specific features for permission tracking
        object : Feature(
            id = "UI Blur tile",
            title = R.string.tile_ui_blur,
            iconRes = R.drawable.rounded_blur_on_24,
            category = R.string.cat_system,
            description = R.string.feat_qs_tiles_desc,
            permissionKeys = listOf("WRITE_SECURE_SETTINGS"),
            showToggle = false,
            isVisibleInMain = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "AOD tile",
            title = R.string.tile_aod,
            iconRes = R.drawable.rounded_mobile_text_2_24,
            category = R.string.cat_system,
            description = R.string.feat_qs_tiles_desc,
            permissionKeys = listOf("WRITE_SECURE_SETTINGS"),
            showToggle = false,
            isVisibleInMain = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Mono Audio tile",
            title = R.string.tile_mono_audio,
            iconRes = R.drawable.rounded_headphones_24,
            category = R.string.cat_system,
            description = R.string.feat_qs_tiles_desc,
            permissionKeys = if (ShellUtils.isRootEnabled(EssentialsApp.context)) listOf("ROOT") else listOf("SHIZUKU"),
            showToggle = false,
            isVisibleInMain = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Adaptive Brightness tile",
            title = R.string.tile_adaptive_brightness,
            iconRes = R.drawable.rounded_brightness_auto_24,
            category = R.string.cat_system,
            description = R.string.feat_qs_tiles_desc,
            permissionKeys = listOf("WRITE_SETTINGS"),
            showToggle = false,
            isVisibleInMain = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Private DNS tile",
            title = R.string.tile_private_dns,
            iconRes = R.drawable.rounded_dns_24,
            category = R.string.cat_system,
            description = R.string.feat_qs_tiles_desc,
            permissionKeys = listOf("WRITE_SECURE_SETTINGS"),
            showToggle = false,
            isVisibleInMain = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "USB Debugging tile",
            title = R.string.tile_usb_debugging,
            iconRes = R.drawable.rounded_adb_24,
            category = R.string.cat_system,
            description = R.string.feat_qs_tiles_desc,
            permissionKeys = listOf("WRITE_SECURE_SETTINGS"),
            showToggle = false,
            isVisibleInMain = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        },

        object : Feature(
            id = "Developer Options tile",
            title = R.string.tile_developer_options,
            iconRes = R.drawable.rounded_mobile_code_24,
            category = R.string.cat_system,
            description = R.string.feat_qs_tiles_desc,
            aboutDescription = R.string.about_desc_developer_options,
            permissionKeys = listOf("WRITE_SECURE_SETTINGS"),
            showToggle = false,
            isVisibleInMain = false
        ) {
            override fun isEnabled(viewModel: MainViewModel) = false
            override fun onToggle(viewModel: MainViewModel, context: Context, enabled: Boolean) {}
        }
    )
}