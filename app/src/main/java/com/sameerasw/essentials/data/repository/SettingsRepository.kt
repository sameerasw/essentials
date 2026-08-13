/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Data & Repository Layer
 * File: SettingsRepository.kt
 * Description: Data repository and storage component for SettingsRepository.kt.
 */

package com.sameerasw.essentials.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.AppTag
import com.sameerasw.essentials.domain.model.DnsPreset
import com.sameerasw.essentials.domain.model.NotificationLightingColorMode
import com.sameerasw.essentials.domain.model.NotificationLightingSide
import com.sameerasw.essentials.domain.model.NotificationLightingStyle
import com.sameerasw.essentials.domain.model.NotificationLightingSweepPosition
import com.sameerasw.essentials.domain.model.ScaleAnimationsProfile
import com.sameerasw.essentials.domain.model.TrackedRepo
import com.sameerasw.essentials.domain.model.github.GitHubUser
import com.sameerasw.essentials.utils.RootUtils
import com.sameerasw.essentials.utils.ShizukuUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SettingsRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        migrateUsageAccessKey()
    }

    private fun migrateUsageAccessKey() {
        val oldKey = "app_lock_use_usage_access"
        if (prefs.contains(oldKey)) {
            val value = prefs.getBoolean(oldKey, false)
            if (!prefs.contains(KEY_USE_USAGE_ACCESS)) {
                putBoolean(KEY_USE_USAGE_ACCESS, value)
            }
            remove(oldKey)
        }
    }

    companion object {
        const val PREFS_NAME = "essentials_prefs"

        // Keys
        const val KEY_GENAI_AUTOMATION_ENABLED = "genai_automation_enabled"
        const val KEY_SMART_PIXELS_ENABLED = "smart_pixels_enabled"
        const val KEY_SMART_PIXELS_INTENSITY = "smart_pixels_intensity"
        const val KEY_DAILY_WALLPAPER_LAST_ID = "daily_wallpaper_last_id"

        const val KEY_DAILY_WALLPAPER_LAST_URL_MOBILE = "daily_wallpaper_last_url_mobile"
        const val KEY_DAILY_WALLPAPER_LAST_URL = "daily_wallpaper_last_url"
        const val KEY_DAILY_WALLPAPER_AUTHOR_NAME = "daily_wallpaper_author_name"
        const val KEY_DAILY_WALLPAPER_AUTHOR_LINK = "daily_wallpaper_author_link"
        const val KEY_DAILY_WALLPAPER_PHOTO_LINK = "daily_wallpaper_photo_link"
        const val KEY_DAILY_WALLPAPER_UPDATED_AT = "daily_wallpaper_updated_at"
        const val KEY_DAILY_WALLPAPER_AUTO_UPDATE = "daily_wallpaper_auto_update"
        const val KEY_DAILY_WALLPAPER_AUTO_UPDATE_TIME = "daily_wallpaper_auto_update_time"
        const val KEY_DAILY_WALLPAPER_SHOW_LAST_TIME = "daily_wallpaper_show_last_time"
        const val KEY_DAILY_WALLPAPER_APPLY_HOME = "daily_wallpaper_apply_home"
        const val KEY_DAILY_WALLPAPER_APPLY_LOCK = "daily_wallpaper_apply_lock"
        const val KEY_DAILY_WALLPAPER_RETRY_COUNT = "daily_wallpaper_retry_count"


        const val KEY_WIDGET_ENABLED = "widget_enabled"
        const val KEY_STATUS_BAR_ICON_CONTROL_ENABLED = "status_bar_icon_control_enabled"
        const val KEY_MAPS_POWER_SAVING_ENABLED = "maps_power_saving_enabled"
        const val KEY_MAPS_DISCOVERED_CHANNELS = "maps_discovered_channels"
        const val KEY_MAPS_DETECTION_CHANNELS = "maps_detection_channels"
        const val KEY_EDGE_LIGHTING_ENABLED = "edge_lighting_enabled"
        const val KEY_EDGE_LIGHTING_ONLY_SCREEN_OFF = "edge_lighting_only_screen_off"
        const val KEY_EDGE_LIGHTING_AMBIENT_DISPLAY = "edge_lighting_ambient_display"
        const val KEY_EDGE_LIGHTING_AMBIENT_SHOW_LOCK_SCREEN =
            "edge_lighting_ambient_show_lock_screen"
        const val KEY_EDGE_LIGHTING_SKIP_SILENT = "edge_lighting_skip_silent"
        const val KEY_EDGE_LIGHTING_SKIP_PERSISTENT = "edge_lighting_skip_persistent"
        const val KEY_EDGE_LIGHTING_STYLE = "edge_lighting_style"
        const val KEY_EDGE_LIGHTING_COLOR_MODE = "edge_lighting_color_mode"
        const val KEY_EDGE_LIGHTING_CUSTOM_COLOR = "edge_lighting_custom_color"
        const val KEY_EDGE_LIGHTING_PULSE_COUNT = "edge_lighting_pulse_count"
        const val KEY_EDGE_LIGHTING_PULSE_DURATION = "edge_lighting_pulse_duration"
        const val KEY_EDGE_LIGHTING_INDICATOR_X = "edge_lighting_indicator_x"
        const val KEY_EDGE_LIGHTING_INDICATOR_Y = "edge_lighting_indicator_y"
        const val KEY_EDGE_LIGHTING_INDICATOR_SCALE = "edge_lighting_indicator_scale"
        const val KEY_EDGE_LIGHTING_GLOW_SIDES = "edge_lighting_glow_sides"
        const val KEY_EDGE_LIGHTING_CORNER_RADIUS = "edge_lighting_corner_radius"
        const val KEY_EDGE_LIGHTING_STROKE_THICKNESS = "edge_lighting_stroke_thickness"
        const val KEY_EDGE_LIGHTING_SELECTED_APPS = "edge_lighting_selected_apps"
        const val KEY_EDGE_LIGHTING_SWEEP_POSITION = "edge_lighting_sweep_position"
        const val KEY_EDGE_LIGHTING_SWEEP_THICKNESS = "edge_lighting_sweep_thickness"
        const val KEY_EDGE_LIGHTING_SWEEP_RANDOM_SHAPES = "edge_lighting_sweep_random_shapes"
        const val KEY_EDGE_LIGHTING_SYSTEM_MODE = "edge_lighting_system_mode"
        const val KEY_LOCK_SCREEN_WALLPAPER_SOURCE = "lock_screen_wallpaper_source"

        const val KEY_CALL_VIBRATIONS_ENABLED = "call_vibrations_enabled"
        const val KEY_LAST_CALL_STATE = "last_call_state"

        const val KEY_BUTTON_REMAP_ENABLED = "button_remap_enabled"
        const val KEY_FLASHLIGHT_VOLUME_TOGGLE_ENABLED =
            "flashlight_volume_toggle_enabled" // Legacy
        const val KEY_BUTTON_REMAP_USE_SHIZUKU = "button_remap_use_shizuku"
        const val KEY_SHIZUKU_DETECTED_DEVICE_PATH = "shizuku_detected_device_path"
        const val KEY_FLASHLIGHT_TRIGGER_BUTTON = "flashlight_trigger_button" // Legacy
        const val KEY_BUTTON_REMAP_VOL_UP_ACTION_OFF = "button_remap_vol_up_action_off"
        const val KEY_BUTTON_REMAP_VOL_UP_ACTION = "button_remap_vol_up_action" // Legacy
        const val KEY_BUTTON_REMAP_VOL_DOWN_ACTION_OFF = "button_remap_vol_down_action_off"
        const val KEY_BUTTON_REMAP_VOL_DOWN_ACTION = "button_remap_vol_down_action" // Legacy
        const val KEY_BUTTON_REMAP_VOL_UP_ACTION_ON = "button_remap_vol_up_action_on"
        const val KEY_BUTTON_REMAP_VOL_DOWN_ACTION_ON = "button_remap_vol_down_action_on"
        const val KEY_BUTTON_REMAP_HAPTIC_TYPE = "button_remap_haptic_type"
        const val KEY_FLASHLIGHT_HAPTIC_TYPE = "flashlight_haptic_type" // Legacy

        const val KEY_DYNAMIC_NIGHT_LIGHT_ENABLED = "dynamic_night_light_enabled"
        const val KEY_DYNAMIC_NIGHT_LIGHT_SELECTED_APPS = "dynamic_night_light_selected_apps"

        const val KEY_SNOOZE_DISCOVERED_CHANNELS = "snooze_discovered_channels"
        const val KEY_SNOOZE_BLOCKED_CHANNELS = "snooze_blocked_channels"
        const val KEY_SNOOZE_HEADS_UP_ENABLED = "snooze_heads_up_enabled"

        const val KEY_FLASHLIGHT_ALWAYS_TURN_OFF_ENABLED = "flashlight_always_turn_off_enabled"
        const val KEY_FLASHLIGHT_FADE_ENABLED = "flashlight_fade_enabled"
        const val KEY_FLASHLIGHT_ADJUST_INTENSITY_ENABLED = "flashlight_adjust_intensity_enabled"
        const val KEY_FLASHLIGHT_GLOBAL_ENABLED = "flashlight_global_enabled"
        const val KEY_FLASHLIGHT_LIVE_UPDATE_ENABLED = "flashlight_live_update_enabled"
        const val KEY_FLASHLIGHT_LAST_INTENSITY = "flashlight_last_intensity"
        const val KEY_FLASHLIGHT_PULSE_ENABLED = "flashlight_pulse_enabled"
        const val KEY_FLASHLIGHT_PULSE_FACEDOWN_ONLY = "flashlight_pulse_facedown_only"
        const val KEY_FLASHLIGHT_PULSE_MAX_INTENSITY = "flashlight_pulse_max_intensity"
        const val KEY_FLASHLIGHT_PULSE_DISABLE_ON_DND = "flashlight_pulse_disable_on_dnd"
        const val KEY_FLASHLIGHT_POCKET_TURN_OFF_ENABLED = "flashlight_pocket_turn_off_enabled"
        const val KEY_FLASHLIGHT_OVERHEAT_PREVENTION_ENABLED =
            "flashlight_overheat_prevention_enabled"

        const val KEY_SCREEN_LOCKED_SECURITY_ENABLED = "screen_locked_security_enabled"
        const val KEY_HIDE_SYSTEM_ICONS = "hide_system_icons"
        const val KEY_HIDE_SYSTEM_ICONS_LOCKED_ONLY = "hide_system_icons_locked_only"
        const val KEY_HIDE_GESTURE_BAR_ENABLED = "hide_gesture_bar_enabled"
        const val KEY_HIDE_GESTURE_BAR_ON_LAUNCHER_ENABLED = "hide_gesture_bar_on_launcher_enabled"
        const val KEY_CIRCLE_TO_SEARCH_GESTURE_ENABLED = "circle_to_search_gesture_enabled"
        const val KEY_CIRCLE_TO_SEARCH_GESTURE_HEIGHT = "circle_to_search_gesture_height"
        const val KEY_CIRCLE_TO_SEARCH_GESTURE_WIDTH = "circle_to_search_gesture_width"
        const val KEY_CIRCLE_TO_SEARCH_PREVIEW_ENABLED = "circle_to_search_preview_enabled"
        const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"
        const val KEY_UPDATE_NOTIFICATION_ENABLED = "update_notification_enabled"
        const val KEY_LAST_UPDATE_CHECK_TIME = "last_update_check_time"
        const val KEY_CHECK_PRE_RELEASES_ENABLED = "check_pre_releases_enabled"

        const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        const val KEY_APP_LOCK_SELECTED_APPS = "app_lock_selected_apps"
        const val KEY_APP_LOCK_AUTO_LOCK_DELAY_INDEX = "app_lock_auto_lock_delay_index"
        const val KEY_USE_USAGE_ACCESS = "use_usage_access"

        const val KEY_FREEZE_WHEN_LOCKED_ENABLED = "freeze_when_locked_enabled"
        const val KEY_FREEZE_LOCK_DELAY_INDEX = "freeze_lock_delay_index"
        const val KEY_FREEZE_AUTO_EXCLUDED_APPS = "freeze_auto_excluded_apps"
        const val KEY_FREEZE_SELECTED_APPS = "freeze_selected_apps"
        const val KEY_FREEZE_DONT_FREEZE_ACTIVE_APPS = "freeze_dont_freeze_active_apps"
        const val KEY_FREEZE_MODE = "freeze_mode"
        const val KEY_FREEZE_SHOW_IN_LAUNCHER = "freeze_show_in_launcher"
        const val KEY_FREEZE_TAGS = "freeze_tags"
        const val KEY_FREEZE_APP_TAG_MAP = "freeze_app_tag_map"
        const val KEY_FREEZE_TAG_COLOR_CODED_ENABLED = "freeze_tag_color_coded_enabled"

        const val KEY_DEVELOPER_MODE_ENABLED = "developer_mode_enabled"
        const val KEY_HAPTIC_FEEDBACK_TYPE = "haptic_feedback_type"
        const val KEY_DEFAULT_TAB = "default_tab"
        const val KEY_USE_ROOT = "use_root"
        const val KEY_PITCH_BLACK_THEME_ENABLED = "pitch_black_theme_enabled"
        const val KEY_ENABLE_UNSUPPORTED_FEATURES = "enable_unsupported_features"

        const val KEY_KEYBOARD_HEIGHT = "keyboard_height"
        const val KEY_TRACKED_REPOS = "tracked_repos"
        const val KEY_KEYBOARD_BOTTOM_PADDING = "keyboard_bottom_padding"
        const val KEY_KEYBOARD_HAPTICS_ENABLED = "keyboard_haptics_enabled"
        const val KEY_KEYBOARD_ROUNDNESS = "keyboard_roundness"
        const val KEY_KEYBOARD_SHAPE = "keyboard_shape" // 0=Round, 1=Flat, 2=Inverse
        const val KEY_KEYBOARD_FUNCTIONS_BOTTOM = "keyboard_functions_bottom"
        const val KEY_KEYBOARD_FUNCTIONS_PADDING = "keyboard_functions_padding"
        const val KEY_KEYBOARD_HAPTIC_STRENGTH = "keyboard_haptic_strength"
        const val KEY_KEYBOARD_ALWAYS_DARK = "keyboard_always_dark"
        const val KEY_KEYBOARD_PITCH_BLACK = "keyboard_pitch_black"
        const val KEY_KEYBOARD_CLIPBOARD_ENABLED = "keyboard_clipboard_enabled"
        const val KEY_KEYBOARD_LONG_PRESS_SYMBOLS = "keyboard_long_press_symbols"
        const val KEY_KEYBOARD_ACCENTED_CHARACTERS = "keyboard_accented_characters"

        // Essentials-AirSync Bridge
        const val KEY_AIRSYNC_CONNECTION_ENABLED = "airsync_connection_enabled"
        const val KEY_MAC_BATTERY_LEVEL = "mac_battery_level"
        const val KEY_MAC_BATTERY_IS_CHARGING = "mac_battery_is_charging"
        const val KEY_MAC_BATTERY_LAST_UPDATED = "mac_battery_last_updated"
        const val KEY_AIRSYNC_MAC_CONNECTED = "airsync_mac_connected"

        const val KEY_BLUETOOTH_DEVICES_BATTERY = "bluetooth_devices_battery"
        const val KEY_SHOW_BLUETOOTH_DEVICES = "show_bluetooth_devices"
        const val KEY_BATTERY_WIDGET_MAX_DEVICES = "battery_widget_max_devices"
        const val KEY_BATTERY_WIDGET_BACKGROUND_ENABLED = "battery_widget_background_enabled"

        const val KEY_PINNED_FEATURES = "pinned_features"
        const val KEY_PINNED_QS_TILES = "pinned_qs_tiles"
        const val KEY_LIKE_SONG_TOAST_ENABLED = "like_song_toast_enabled"
        const val KEY_LIKE_SONG_AOD_OVERLAY_ENABLED = "like_song_aod_overlay_enabled"
        const val KEY_AMBIENT_MUSIC_GLANCE_ENABLED = "ambient_music_glance_enabled"
        const val KEY_AMBIENT_MUSIC_GLANCE_DOCKED_MODE = "ambient_music_glance_docked_mode"
        const val KEY_AMBIENT_MUSIC_GLANCE_RANDOM_SHAPES = "ambient_music_glance_random_shapes"
        const val KEY_AMBIENT_MUSIC_GLANCE_ALBUM_ART_MODE = "ambient_music_glance_album_art_mode"
        const val KEY_AMBIENT_MUSIC_GLANCE_CLOCK_SIZE = "ambient_music_glance_clock_size"
        const val KEY_AMBIENT_MUSIC_GLANCE_CLOCK_WEIGHT = "ambient_music_glance_clock_weight"
        const val KEY_AMBIENT_MUSIC_GLANCE_CLOCK_WIDTH = "ambient_music_glance_clock_width"
        const val KEY_AMBIENT_MUSIC_GLANCE_CLOCK_ROUNDNESS = "ambient_music_glance_clock_roundness"
        const val KEY_AMBIENT_MUSIC_GLANCE_FORCE_FILL_WHILE_CHARGING =
            "ambient_music_glance_force_fill_while_charging"
        const val KEY_AMBIENT_MUSIC_GLANCE_RESPECT_NOTIFICATIONS =
            "ambient_music_glance_respect_notifications"
        const val KEY_CALENDAR_SYNC_ENABLED = "calendar_sync_enabled"
        const val KEY_CALENDAR_SYNC_SELECTED_CALENDARS = "calendar_sync_selected_calendars"
        const val KEY_CALENDAR_SYNC_PERIODIC_ENABLED = "calendar_sync_periodic_enabled"
        const val KEY_REMOTE_LOCK_MODE = "remote_lock_mode" // 0: Screen off, 1: Lock

        const val KEY_GITHUB_ACCESS_TOKEN = "github_access_token"
        const val KEY_GITHUB_WORKFLOW_TOKEN = "github_workflow_token"
        const val KEY_GITHUB_USER_PROFILE = "github_user_profile"

        const val KEY_FLASHLIGHT_PULSE_SELECTED_APPS = "flashlight_pulse_selected_apps"
        const val KEY_FLASHLIGHT_PULSE_SAME_AS_LIGHTING = "flashlight_pulse_same_as_lighting"

        const val KEY_BATTERY_NOTIFICATION_ENABLED = "battery_notification_enabled"
        const val KEY_USER_DICTIONARY_ENABLED = "user_dictionary_enabled"
        const val KEY_USER_DICT_LAST_UPDATE = "user_dict_last_update"

        const val KEY_FONT_SCALE = "font_scale"
        const val KEY_FONT_WEIGHT = "font_weight"
        const val KEY_ANIMATOR_DURATION_SCALE = "animator_duration_scale"
        const val KEY_TRANSITION_ANIMATION_SCALE = "transition_animation_scale"
        const val KEY_WINDOW_ANIMATION_SCALE = "window_animation_scale"
        const val KEY_SMALLEST_WIDTH = "smallest_width"
        const val KEY_NOTIFICATION_GLANCE_ENABLED = "notification_glance_enabled"
        const val KEY_NOTIFICATION_GLANCE_SAME_AS_LIGHTING = "notification_glance_same_as_lighting"
        const val KEY_NOTIFICATION_GLANCE_SELECTED_APPS = "notification_glance_selected_apps"
        const val KEY_AOD_FORCE_TURN_OFF_ENABLED = "aod_force_turn_off_enabled"
        const val KEY_AUTO_ACCESSIBILITY_ENABLED = "auto_accessibility_enabled"
        const val KEY_USE_BLUR = "use_blur"
        const val KEY_SWIPE_TABS = "swipe_tabs"
        const val KEY_SENTRY_REPORT_MODE = "sentry_report_mode"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_PRIVATE_DNS_PRESETS = "private_dns_presets"
        const val KEY_APRIL_FOOLS_SHOWN = "april_fools_shown"
        const val KEY_WHATS_NEW_LAST_SHOWN_COUNTER = "whats_new_last_shown_counter"
        const val KEY_SCALE_ANIMATIONS_MODE = "scale_animations_mode"
        const val KEY_SCALE_ANIMATIONS_DEFAULT_PROFILE = "scale_animations_default_profile"
        const val KEY_SCALE_ANIMATIONS_GLOVE_PROFILE = "scale_animations_glove_profile"
        const val KEY_REFRESH_RATE_MODE = "refresh_rate_mode"
        const val KEY_REFRESH_RATE_FIXED = "refresh_rate_fixed"
        const val KEY_REFRESH_RATE_MIN = "refresh_rate_min"
        const val KEY_REFRESH_RATE_PEAK = "refresh_rate_peak"
        const val KEY_REFRESH_RATE_DEFAULT_PEAK_INFINITY = "refresh_rate_default_peak_infinity"

        // Live Wallpaper
        const val LIVE_WALLPAPER_PREFS_NAME = "live_wallpaper_prefs"
        const val KEY_LIVE_WALLPAPER_SELECTED_VIDEO = "selected_video"
        const val KEY_LIVE_WALLPAPER_PLAYBACK_TRIGGER = "playback_trigger"
        const val KEY_LIVE_WALLPAPER_CUSTOM_VIDEOS = "custom_videos"
        const val LIVE_WALLPAPER_DEFAULT_VIDEO = "my_video"
        const val LIVE_WALLPAPER_TRIGGER_UNLOCK = "unlock"
        const val LIVE_WALLPAPER_TRIGGER_SCREEN_ON = "screen_on"

        const val KEY_SHUT_UP_SELECTED_APPS = "shut_up_selected_apps"
        const val KEY_SHUT_UP_ORIGINAL_SETTINGS = "shut_up_original_settings"
        const val KEY_SHUT_UP_ATTEMPT_SHIZUKU_RESTART = "shut_up_attempt_shizuku_restart"
        const val KEY_SHUT_UP_RESTORE_DELAY = "shut_up_restore_delay"
        const val KEY_SHUT_UP_RESTORE_MODE = "shut_up_restore_mode"
        const val KEY_SHIZUKU_AUTH_TOKEN = "shizuku_auth_token"
        const val KEY_EDGE_LIGHTING_SWEEP_SELECTED_SHAPES = "edge_lighting_sweep_selected_shapes"
        const val KEY_DISABLE_ROTATION_SUGGESTION = "disable_rotation_suggestion"
        const val KEY_ALLOW_OVERLAYS_IN_SETTINGS = "allow_overlays_in_settings"
        const val KEY_NETWORK_DOWNLOAD_RATE_LIMIT = "network_download_rate_limit"
        const val KEY_MOBILE_DATA_ALWAYS_ON = "mobile_data_always_on"
        const val KEY_WIRELESS_DISPLAY_CERTIFICATION = "wireless_display_certification"
        const val KEY_PREFER_GPU_COMPOSING = "prefer_gpu_composing"
        const val KEY_TRANSPARENT_NAVIGATION_BAR = "transparent_navigation_bar"
        const val KEY_STANDBY_APPS = "standby_apps"
        const val KEY_PIXEL_SEARCHBAR = "pixel_searchbar"
        const val KEY_PIXEL_SEARCHBAR_TYPE = "pixel_searchbar_type"
        const val KEY_PIXEL_SEARCHBAR_DATE_FORMAT = "pixel_searchbar_date_format"
        const val KEY_PIXEL_SEARCHBAR_BACKGROUND_PILL = "pixel_searchbar_background_pill"
        const val KEY_PIXEL_SEARCHBAR_WIDGET_ID = "pixel_searchbar_widget_id"
        const val KEY_PIXEL_SEARCHBAR_WIDGET_PROVIDER = "pixel_searchbar_widget_provider"
        const val KEY_PIXEL_SEARCHBAR_SCRAPED_LINE1 = "pixel_searchbar_scraped_line1"
        const val KEY_PIXEL_SEARCHBAR_SCRAPED_LINE2 = "pixel_searchbar_scraped_line2"
        const val KEY_PIXEL_SEARCHBAR_WIDGET_PADDING_H = "pixel_searchbar_widget_padding_h"
        const val KEY_PIXEL_SEARCHBAR_WIDGET_PADDING_V = "pixel_searchbar_widget_padding_v"
        const val KEY_PIXEL_SEARCHBAR_TAP_ACTION_ENABLED = "pixel_searchbar_tap_action_enabled"
        const val KEY_PIXEL_SEARCHBAR_WIDGET_REVISION = "pixel_searchbar_widget_revision"
        const val KEY_PIXEL_SEARCHBAR_MUSIC_TITLE = "pixel_searchbar_music_title"
        const val KEY_PIXEL_SEARCHBAR_MUSIC_ARTIST = "pixel_searchbar_music_artist"
        const val KEY_PIXEL_SEARCHBAR_MUSIC_PACKAGE = "pixel_searchbar_music_package"

        const val KEY_LOCK_SCREEN_CLOCK_WEIGHT = "lock_screen_clock_weight"
        const val KEY_LOCK_SCREEN_CLOCK_WIDTH = "lock_screen_clock_width"
        const val KEY_LOCK_SCREEN_CLOCK_GRADE = "lock_screen_clock_grade"
        const val KEY_LOCK_SCREEN_CLOCK_ROUNDNESS = "lock_screen_clock_roundness"
        const val KEY_LOCK_SCREEN_CLOCK_COLOR_TONE = "lock_screen_clock_color_tone"
        const val KEY_LOCK_SCREEN_CLOCK_SELECTED_COLOR_ID = "lock_screen_clock_selected_color_id"
        const val KEY_LOCK_SCREEN_CLOCK_SEED_COLOR = "lock_screen_clock_seed_color"
        const val KEY_RECENT_SEARCHES = "recent_searches"
        const val KEY_POCKET_MODE_ENABLED = "pocket_mode_enabled"
        const val KEY_POCKET_MODE_USE_LIGHT_SENSOR = "pocket_mode_use_light_sensor"
        const val KEY_POCKET_MODE_EXCLUDED_APPS = "pocket_mode_excluded_apps"
        const val KEY_POCKET_MODE_TRIGGER_DELAY = "pocket_mode_trigger_delay"
        const val KEY_POCKET_MODE_LOCK_SCREEN_ONLY = "pocket_mode_lock_screen_only"
        const val KEY_KEEP_PREFS = "keep_prefs"
        const val KEY_TRANSLATION_MODE_DO_NOT_SHOW_WARNING = "translation_mode_do_not_show_warning"

        const val KEY_LOCKDOWN_MODE = "lockdown_mode"
    }

    /**
     * Executes the is translation mode warning suppressed operation.
     * @return The resulting Boolean data.
     */
    fun isTranslationModeWarningSuppressed(): Boolean =
        getBoolean(KEY_TRANSLATION_MODE_DO_NOT_SHOW_WARNING, false)

    /**
     * Executes the set translation mode warning suppressed operation.
     *
     * @param suppressed [Boolean] Target suppressed.
     */
    fun setTranslationModeWarningSuppressed(suppressed: Boolean) =
        putBoolean(KEY_TRANSLATION_MODE_DO_NOT_SHOW_WARNING, suppressed)


    // Observe changes
    fun observeKeyChanges(): Flow<String?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            trySend(key)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val isPitchBlackThemeEnabled: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_PITCH_BLACK_THEME_ENABLED) {
                trySend(getBoolean(KEY_PITCH_BLACK_THEME_ENABLED))
            }
        }
        trySend(getBoolean(KEY_PITCH_BLACK_THEME_ENABLED))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /**
     * Registers a listener to be notified when shared preference values change.
     *
     * @param listener [SharedPreferences.OnSharedPreferenceChangeListener] The listener instance to register.
     */
    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Unregisters a previously registered shared preference change listener.
     *
     * @param listener [SharedPreferences.OnSharedPreferenceChangeListener] The listener instance to unregister.
     */
    fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    // General Getters

    /**
     * Retrieves a boolean preference value by key.
     *
     * @param key [String] Preference key name.
     * @param default [Boolean] Fallback boolean value if key is absent.
     * @return The stored boolean value or default.
     */
    fun getBoolean(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)

    /**
     * Retrieves a string preference value by key.
     *
     * @param key [String] Preference key name.
     * @param default [String?] Fallback string value if key is absent.
     * @return The stored string value or default.
     */
    fun getString(key: String, default: String? = null): String? = prefs.getString(key, default)

    /**
     * Retrieves an integer preference value by key.
     *
     * @param key [String] Preference key name.
     * @param default [Int] Fallback integer value if key is absent.
     * @return The stored integer value or default.
     */
    fun getInt(key: String, default: Int = 0): Int = prefs.getInt(key, default)

    /**
     * Retrieves a float preference value by key, with legacy integer migration fallback.
     *
     * @param key [String] Preference key name.
     * @param default [Float] Fallback float value if key is absent.
     * @return The stored float value or default.
     */
    fun getFloat(key: String, default: Float = 0f): Float {
        return try {
            prefs.getFloat(key, default)
        } catch (e: ClassCastException) {
            try {
                // Migrate from Int to Float if necessary
                val intValue = prefs.getInt(key, default.toInt())
                val floatValue = intValue.toFloat()
                putFloat(key, floatValue)
                floatValue
            } catch (e2: Exception) {
                default
            }
        }
    }

    /**
     * Retrieves a long integer preference value by key.
     *
     * @param key [String] Preference key name.
     * @param default [Long] Fallback long value if key is absent.
     * @return The stored long value or default.
     */
    fun getLong(key: String, default: Long = 0L): Long = prefs.getLong(key, default)

    // General Setters

    /**
     * Checks if a preference key exists in persistent storage.
     *
     * @param key [String] Preference key name.
     * @return True if key exists in shared preferences.
     */
    fun contains(key: String): Boolean = prefs.contains(key)

    /**
     * Asynchronously stores a boolean preference value.
     *
     * @param key [String] Preference key name.
     * @param value [Boolean] Boolean value to store.
     */
    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    /**
     * Asynchronously stores a string preference value.
     *
     * @param key [String] Preference key name.
     * @param value [String?] String value to store.
     */
    fun putString(key: String, value: String?) = prefs.edit().putString(key, value).apply()

    /**
     * Asynchronously stores an integer preference value.
     *
     * @param key [String] Preference key name.
     * @param value [Int] Integer value to store.
     */
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()

    /**
     * Asynchronously stores a float preference value.
     *
     * @param key [String] Preference key name.
     * @param value [Float] Float value to store.
     */
    fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()

    /**
     * Asynchronously stores a long integer preference value.
     *
     * @param key [String] Preference key name.
     * @param value [Long] Long value to store.
     */
    fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()

    /**
     * Asynchronously removes a preference entry by key.
     *
     * @param key [String] Preference key name to remove.
     */
    fun remove(key: String) = prefs.edit().remove(key).apply()

    // Specific Getters with logic from ViewModel

    fun getNotificationLightingStyle(): NotificationLightingStyle {
        val styleName =
            prefs.getString(KEY_EDGE_LIGHTING_STYLE, NotificationLightingStyle.STROKE.name)
        return try {
            NotificationLightingStyle.valueOf(styleName ?: NotificationLightingStyle.STROKE.name)
        } catch (e: Exception) {
            NotificationLightingStyle.STROKE
        }
    }

    /**
     * Executes the get notification lighting color mode operation.
     * @return The resulting NotificationLightingColorMode data.
     */
    fun getNotificationLightingColorMode(): NotificationLightingColorMode {
        val colorModeName =
            prefs.getString(KEY_EDGE_LIGHTING_COLOR_MODE, NotificationLightingColorMode.SYSTEM.name)
        return try {
            NotificationLightingColorMode.valueOf(
                colorModeName ?: NotificationLightingColorMode.SYSTEM.name
            )
        } catch (e: Exception) {
            NotificationLightingColorMode.SYSTEM
        }
    }

    /**
     * Executes the get notification lighting glow sides operation.
     * @return The resulting Set<NotificationLightingSide> data.
     */
    fun getNotificationLightingGlowSides(): Set<NotificationLightingSide> {
        val json = prefs.getString(KEY_EDGE_LIGHTING_GLOW_SIDES, null)
        return if (json != null) {
            try {
                gson.fromJson(json, Array<NotificationLightingSide>::class.java).toSet()
            } catch (e: Exception) {
                setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
            }
        } else {
            setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
        }
    }


    /**
     * Executes the save notification lighting glow sides operation.
     *
     * @param sides [Set<NotificationLightingSide>] Target sides.
     */
    fun saveNotificationLightingGlowSides(sides: Set<NotificationLightingSide>) {
        val json = gson.toJson(sides)
        putString(KEY_EDGE_LIGHTING_GLOW_SIDES, json)
    }

    /**
     * Executes the get notification lighting sweep position operation.
     * @return The resulting NotificationLightingSweepPosition data.
     */
    fun getNotificationLightingSweepPosition(): NotificationLightingSweepPosition {
        val posName = prefs.getString(
            KEY_EDGE_LIGHTING_SWEEP_POSITION,
            NotificationLightingSweepPosition.CENTER.name
        )
        return try {
            NotificationLightingSweepPosition.valueOf(
                posName ?: NotificationLightingSweepPosition.CENTER.name
            )
        } catch (e: Exception) {
            NotificationLightingSweepPosition.CENTER
        }
    }

    /**
     * Executes the save notification lighting sweep position operation.
     *
     * @param position [NotificationLightingSweepPosition] Target position.
     */
    fun saveNotificationLightingSweepPosition(position: NotificationLightingSweepPosition) {
        putString(KEY_EDGE_LIGHTING_SWEEP_POSITION, position.name)
    }

    /**
     * Executes the get notification lighting system mode operation.
     * @return The resulting Int data.
     */
    fun getNotificationLightingSystemMode(): Int = getInt(KEY_EDGE_LIGHTING_SYSTEM_MODE, 0)

    /**
     * Executes the save notification lighting system mode operation.
     *
     * @param mode [Int] Target mode.
     */
    fun saveNotificationLightingSystemMode(mode: Int) = putInt(KEY_EDGE_LIGHTING_SYSTEM_MODE, mode)

    /**
     * Executes the get freeze auto excluded apps operation.
     * @return The resulting Set<String> data.
     */
    fun getFreezeAutoExcludedApps(): Set<String> {
        val json = prefs.getString(KEY_FREEZE_AUTO_EXCLUDED_APPS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, Array<String>::class.java).toSet()
            } catch (e: Exception) {
                emptySet()
            }
        } else emptySet()
    }

    /**
     * Executes the save freeze auto excluded apps operation.
     *
     * @param apps [Set<String>] Target apps.
     */
    fun saveFreezeAutoExcludedApps(apps: Set<String>) {
        val json = gson.toJson(apps)
        putString(KEY_FREEZE_AUTO_EXCLUDED_APPS, json)
    }

    /**
     * Executes the get freeze tags operation.
     * @return The resulting List<AppTag> data.
     */
    fun getFreezeTags(): List<AppTag> {
        val json = prefs.getString(KEY_FREEZE_TAGS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AppTag>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Executes the save freeze tags operation.
     *
     * @param tags [List<AppTag>] Target tags.
     */
    fun saveFreezeTags(tags: List<AppTag>) {
        val json = gson.toJson(tags)
        putString(KEY_FREEZE_TAGS, json)
    }

    /**
     * Executes the get freeze app tag map operation.
     * @return The resulting Map<String, List<String>> data.
     */
    fun getFreezeAppTagMap(): Map<String, List<String>> {
        val json = prefs.getString(KEY_FREEZE_APP_TAG_MAP, null) ?: return emptyMap()
        return try {
            val type = object : TypeToken<Map<String, List<String>>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * Executes the save freeze app tag map operation.
     *
     * @param map [Map<String] Target map.
     * @param List<String>> Target list string.
     */
    fun saveFreezeAppTagMap(map: Map<String, List<String>>) {
        val json = gson.toJson(map)
        putString(KEY_FREEZE_APP_TAG_MAP, json)
    }

    /**
     * Executes the get freeze mode operation.
     * @return The resulting Int data.
     */
    fun getFreezeMode(): Int = getInt(KEY_FREEZE_MODE, 0)

    /**
     * Executes the get haptic feedback type operation.
     * @return The resulting HapticFeedbackType data.
     */
    fun getHapticFeedbackType(): HapticFeedbackType {
        val typeName = prefs.getString(KEY_HAPTIC_FEEDBACK_TYPE, HapticFeedbackType.SUBTLE.name)
        return try {
            HapticFeedbackType.valueOf(typeName ?: HapticFeedbackType.SUBTLE.name)
        } catch (e: Exception) {
            HapticFeedbackType.SUBTLE
        }
    }

    /**
     * Executes the get diy tab operation.
     * @return The resulting com data.
     */
    fun getDIYTab(): com.sameerasw.essentials.domain.DIYTabs {
        val tabName = prefs.getString(
            KEY_DEFAULT_TAB,
            com.sameerasw.essentials.domain.DIYTabs.ESSENTIALS.name
        )
        return try {
            com.sameerasw.essentials.domain.DIYTabs.valueOf(
                tabName ?: com.sameerasw.essentials.domain.DIYTabs.ESSENTIALS.name
            )
        } catch (e: Exception) {
            com.sameerasw.essentials.domain.DIYTabs.ESSENTIALS
        }
    }

    /**
     * Executes the save diy tab operation.
     *
     * @param tab [com.sameerasw.essentials.domain.DIYTabs] Target tab.
     */
    fun saveDIYTab(tab: com.sameerasw.essentials.domain.DIYTabs) {
        putString(KEY_DEFAULT_TAB, tab.name)
    }

    /**
     * Executes the get calendar sync selected calendars operation.
     * @return The resulting Set<String> data.
     */
    fun getCalendarSyncSelectedCalendars(): Set<String> {
        val json = prefs.getString(KEY_CALENDAR_SYNC_SELECTED_CALENDARS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, Array<String>::class.java).toSet()
            } catch (e: Exception) {
                emptySet()
            }
        } else emptySet()
    }

    /**
     * Executes the save calendar sync selected calendars operation.
     *
     * @param calendarIds [Set<String>] Target calendar ids.
     */
    fun saveCalendarSyncSelectedCalendars(calendarIds: Set<String>) {
        val json = gson.toJson(calendarIds)
        putString(KEY_CALENDAR_SYNC_SELECTED_CALENDARS, json)
    }

    /**
     * Executes the is calendar sync periodic enabled operation.
     * @return The resulting Boolean data.
     */
    fun isCalendarSyncPeriodicEnabled(): Boolean =
        getBoolean(KEY_CALENDAR_SYNC_PERIODIC_ENABLED, false)

    /**
     * Executes the set calendar sync periodic enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setCalendarSyncPeriodicEnabled(enabled: Boolean) =
        putBoolean(KEY_CALENDAR_SYNC_PERIODIC_ENABLED, enabled)

    // App Selection Helper Generic
    private fun loadAppSelection(key: String): List<AppSelection> {
        val json = prefs.getString(key, null)
        return if (json != null) {
            try {
                gson.fromJson(json, Array<AppSelection>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    private fun saveAppSelection(key: String, apps: List<AppSelection>) {
        val json = gson.toJson(apps)
        putString(key, json)
    }

    // Feature specific App selections

    fun loadNotificationLightingSelectedApps() = loadAppSelection(KEY_EDGE_LIGHTING_SELECTED_APPS)

    /**
     * Executes the save notification lighting selected apps operation.
     *
     * @param apps [List<AppSelection>] Target apps.
     */
    fun saveNotificationLightingSelectedApps(apps: List<AppSelection>) =
        saveAppSelection(KEY_EDGE_LIGHTING_SELECTED_APPS, apps)

    /**
     * Executes the update notification lighting app selection operation.
     *
     * @param packageName [String] Target package name.
     * @param enabled [Boolean] Target enabled.
     */
    fun updateNotificationLightingAppSelection(packageName: String, enabled: Boolean) =
        updateAppSelection(KEY_EDGE_LIGHTING_SELECTED_APPS, packageName, enabled)

    /**
     * Executes the load dynamic night light selected apps operation.
     */
    fun loadDynamicNightLightSelectedApps() =
        loadAppSelection(KEY_DYNAMIC_NIGHT_LIGHT_SELECTED_APPS)

    /**
     * Executes the save dynamic night light selected apps operation.
     *
     * @param apps [List<AppSelection>] Target apps.
     */
    fun saveDynamicNightLightSelectedApps(apps: List<AppSelection>) =
        saveAppSelection(KEY_DYNAMIC_NIGHT_LIGHT_SELECTED_APPS, apps)

    /**
     * Executes the update dynamic night light app selection operation.
     *
     * @param packageName [String] Target package name.
     * @param enabled [Boolean] Target enabled.
     */
    fun updateDynamicNightLightAppSelection(packageName: String, enabled: Boolean) =
        updateAppSelection(KEY_DYNAMIC_NIGHT_LIGHT_SELECTED_APPS, packageName, enabled)

    /**
     * Executes the load app lock selected apps operation.
     */
    fun loadAppLockSelectedApps() = loadAppSelection(KEY_APP_LOCK_SELECTED_APPS)

    /**
     * Executes the save app lock selected apps operation.
     *
     * @param apps [List<AppSelection>] Target apps.
     */
    fun saveAppLockSelectedApps(apps: List<AppSelection>) =
        saveAppSelection(KEY_APP_LOCK_SELECTED_APPS, apps)

    /**
     * Executes the update app lock app selection operation.
     *
     * @param packageName [String] Target package name.
     * @param enabled [Boolean] Target enabled.
     */
    fun updateAppLockAppSelection(packageName: String, enabled: Boolean) =
        updateAppSelection(KEY_APP_LOCK_SELECTED_APPS, packageName, enabled)

    /**
     * Executes the load freeze selected apps operation.
     */
    fun loadFreezeSelectedApps() = loadAppSelection(KEY_FREEZE_SELECTED_APPS)

    /**
     * Executes the save freeze selected apps operation.
     *
     * @param apps [List<AppSelection>] Target apps.
     */
    fun saveFreezeSelectedApps(apps: List<AppSelection>) =
        saveAppSelection(KEY_FREEZE_SELECTED_APPS, apps.filter { it.isEnabled })

    /**
     * Executes the update freeze app selection operation.
     *
     * @param packageName [String] Target package name.
     * @param enabled [Boolean] Target enabled.
     */
    fun updateFreezeAppSelection(packageName: String, enabled: Boolean) =
        updateAppSelection(KEY_FREEZE_SELECTED_APPS, packageName, enabled)

    /**
     * Executes the load flashlight pulse selected apps operation.
     */
    fun loadFlashlightPulseSelectedApps() = loadAppSelection(KEY_FLASHLIGHT_PULSE_SELECTED_APPS)

    /**
     * Executes the save flashlight pulse selected apps operation.
     *
     * @param apps [List<AppSelection>] Target apps.
     */
    fun saveFlashlightPulseSelectedApps(apps: List<AppSelection>) =
        saveAppSelection(KEY_FLASHLIGHT_PULSE_SELECTED_APPS, apps)

    /**
     * Executes the update flashlight pulse app selection operation.
     *
     * @param packageName [String] Target package name.
     * @param enabled [Boolean] Target enabled.
     */
    fun updateFlashlightPulseAppSelection(packageName: String, enabled: Boolean) =
        updateAppSelection(KEY_FLASHLIGHT_PULSE_SELECTED_APPS, packageName, enabled)

    /**
     * Executes the load notification glance selected apps operation.
     */
    fun loadNotificationGlanceSelectedApps() =
        loadAppSelection(KEY_NOTIFICATION_GLANCE_SELECTED_APPS)

    /**
     * Executes the save notification glance selected apps operation.
     *
     * @param apps [List<AppSelection>] Target apps.
     */
    fun saveNotificationGlanceSelectedApps(apps: List<AppSelection>) =
        saveAppSelection(KEY_NOTIFICATION_GLANCE_SELECTED_APPS, apps)

    /**
     * Executes the update notification glance app selection operation.
     *
     * @param packageName [String] Target package name.
     * @param enabled [Boolean] Target enabled.
     */
    fun updateNotificationGlanceAppSelection(packageName: String, enabled: Boolean) =
        updateAppSelection(KEY_NOTIFICATION_GLANCE_SELECTED_APPS, packageName, enabled)

    /**
     * Executes the load pocket mode excluded apps operation.
     */
    fun loadPocketModeExcludedApps() = loadAppSelection(KEY_POCKET_MODE_EXCLUDED_APPS)

    /**
     * Executes the save pocket mode excluded apps operation.
     *
     * @param apps [List<AppSelection>] Target apps.
     */
    fun savePocketModeExcludedApps(apps: List<AppSelection>) =
        saveAppSelection(KEY_POCKET_MODE_EXCLUDED_APPS, apps)

    /**
     * Executes the update pocket mode excluded app selection operation.
     *
     * @param packageName [String] Target package name.
     * @param enabled [Boolean] Target enabled.
     */
    fun updatePocketModeExcludedAppSelection(packageName: String, enabled: Boolean) =
        updateAppSelection(KEY_POCKET_MODE_EXCLUDED_APPS, packageName, enabled)

    /**
     * Executes the load shut up configs operation.
     * @return The resulting List<com data.
     */
    fun loadShutUpConfigs(): List<com.sameerasw.essentials.domain.model.ShutUpAppConfig> {
        val json = prefs.getString(KEY_SHUT_UP_SELECTED_APPS, null)
        return if (json != null) {
            try {
                gson.fromJson(
                    json,
                    Array<com.sameerasw.essentials.domain.model.ShutUpAppConfig>::class.java
                ).toList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * Executes the save shut up configs operation.
     *
     * @param configs [List<com.sameerasw.essentials.domain.model.ShutUpAppConfig>] Target configs.
     */
    fun saveShutUpConfigs(configs: List<com.sameerasw.essentials.domain.model.ShutUpAppConfig>) {
        val json = gson.toJson(configs)
        putString(KEY_SHUT_UP_SELECTED_APPS, json)
    }

    /**
     * Executes the update shut up config operation.
     *
     * @param config [com.sameerasw.essentials.domain.model.ShutUpAppConfig] Target config.
     */
    fun updateShutUpConfig(config: com.sameerasw.essentials.domain.model.ShutUpAppConfig) {
        val current = loadShutUpConfigs().toMutableList()
        val index = current.indexOfFirst { it.packageName == config.packageName }
        if (index != -1) {
            current[index] = config
        } else {
            current.add(config)
        }
        saveShutUpConfigs(current)
    }

    /**
     * Executes the save shut up original settings operation.
     *
     * @param settings [Map<String] Target settings.
     * @param String> Target string.
     */
    fun saveShutUpOriginalSettings(settings: Map<String, String>) {
        val json = gson.toJson(settings)
        putString(KEY_SHUT_UP_ORIGINAL_SETTINGS, json)
    }

    /**
     * Executes the get shut up original settings operation.
     * @return The resulting Map<String, String> data.
     */
    fun getShutUpOriginalSettings(): Map<String, String> {
        val json = prefs.getString(KEY_SHUT_UP_ORIGINAL_SETTINGS, null) ?: return emptyMap()
        return try {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson(json, Map::class.java) as Map<String, String>
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun updateAppSelection(key: String, packageName: String, enabled: Boolean) {
        val current = loadAppSelection(key).toMutableList()
        val index = current.indexOfFirst { it.packageName == packageName }
        if (index != -1) {
            current[index] = current[index].copy(isEnabled = enabled)
        } else {
            current.add(AppSelection(packageName, enabled))
        }
        // Special case for freeze apps to only save enabled ones is handled in saveFreezeSelectedApps
        // But here we are using generic generic save for key?
        // Wait, saveFreezeSelectedApps filters. 
        // My generic updateAppSelection calls... wait, no.
        // I should call the specific save method or generic save method?
        // If I use generic saveAppSelection(key, current), for freeze apps, I might save disabled apps if I don't filter.
        // Let's look at saveFreezeSelectedApps: it calls saveAppSelection(KEY..., apps.filter { it.isEnabled })

        if (key == KEY_FREEZE_SELECTED_APPS) {
            saveAppSelection(key, current.filter { it.isEnabled })
        } else {
            saveAppSelection(key, current)
        }
    }

    // Snooze Notifications Helper
    fun loadSnoozeDiscoveredChannels(): List<com.sameerasw.essentials.domain.model.SnoozeChannel> {
        val json = prefs.getString(KEY_SNOOZE_DISCOVERED_CHANNELS, null)
        return if (json != null) {
            try {
                gson.fromJson(
                    json,
                    Array<com.sameerasw.essentials.domain.model.SnoozeChannel>::class.java
                ).toList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * Executes the save snooze discovered channels operation.
     *
     * @param channels [List<com.sameerasw.essentials.domain.model.SnoozeChannel>] Target channels.
     */
    fun saveSnoozeDiscoveredChannels(channels: List<com.sameerasw.essentials.domain.model.SnoozeChannel>) {
        val json = gson.toJson(channels)
        putString(KEY_SNOOZE_DISCOVERED_CHANNELS, json)
    }

    /**
     * Executes the load snooze blocked channels operation.
     * @return The resulting Set<String> data.
     */
    fun loadSnoozeBlockedChannels(): Set<String> {
        val json = prefs.getString(KEY_SNOOZE_BLOCKED_CHANNELS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, Array<String>::class.java).toSet()
            } catch (e: Exception) {
                emptySet()
            }
        } else {
            emptySet()
        }
    }

    /**
     * Executes the save snooze blocked channels operation.
     *
     * @param blockedChannels [Set<String>] Target blocked channels.
     */
    fun saveSnoozeBlockedChannels(blockedChannels: Set<String>) {
        val json = gson.toJson(blockedChannels)
        putString(KEY_SNOOZE_BLOCKED_CHANNELS, json)
    }

    // Maps Channels Helper
    fun loadMapsDiscoveredChannels(): List<com.sameerasw.essentials.domain.model.MapsChannel> {
        val json = prefs.getString(KEY_MAPS_DISCOVERED_CHANNELS, null)
        return if (json != null) {
            try {
                gson.fromJson(
                    json,
                    Array<com.sameerasw.essentials.domain.model.MapsChannel>::class.java
                ).toList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * Executes the save maps discovered channels operation.
     *
     * @param channels [List<com.sameerasw.essentials.domain.model.MapsChannel>] Target channels.
     */
    fun saveMapsDiscoveredChannels(channels: List<com.sameerasw.essentials.domain.model.MapsChannel>) {
        val json = gson.toJson(channels)
        putString(KEY_MAPS_DISCOVERED_CHANNELS, json)
    }

    /**
     * Executes the load maps detection channels operation.
     * @return The resulting Set<String> data.
     */
    fun loadMapsDetectionChannels(): Set<String> {
        val json = prefs.getString(KEY_MAPS_DETECTION_CHANNELS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, Array<String>::class.java).toSet()
            } catch (e: Exception) {
                emptySet()
            }
        } else {
            // Default to navigation related channel IDs if none are selected yet
            setOf(
                "navigation_notification_channel",
                "primary_navigation_channel_v1",
                "primary_navigation_channel_v2"
            )
        }
    }

    /**
     * Executes the save maps detection channels operation.
     *
     * @param channels [Set<String>] Target channels.
     */
    fun saveMapsDetectionChannels(channels: Set<String>) {
        val json = gson.toJson(channels)
        putString(KEY_MAPS_DETECTION_CHANNELS, json)
    }

    // Config Export/Import
    fun getAllConfigsAsJsonString(): String {
        return try {
            val allConfigs = mutableMapOf<String, Map<String, Map<String, Any>>>()
            val prefFiles = listOf(
                "essentials_prefs",
                "caffeinate_prefs",
                "link_prefs",
                "diy_automations_prefs",
                "live_wallpaper_prefs"
            )

            prefFiles.forEach { fileName ->
                val p = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
                val wrapperMap = mutableMapOf<String, Map<String, Any>>()

                p.all.forEach { (key, value) ->
                    if (key == "freeze_auto_excluded_apps" || key.endsWith("_selected_apps")) {
                    }
                    if (key == KEY_GITHUB_ACCESS_TOKEN || key == KEY_GITHUB_WORKFLOW_TOKEN ||
                        key == KEY_SHIZUKU_AUTH_TOKEN || key.startsWith("mac_battery_") ||
                        key == "airsync_mac_connected" || key == KEY_SNOOZE_DISCOVERED_CHANNELS ||
                        key == KEY_MAPS_DISCOVERED_CHANNELS || key == KEY_SHUT_UP_ORIGINAL_SETTINGS ||
                        key == "battery_history_points"
                    ) {
                        return@forEach
                    }

                    val type = when (value) {
                        is Boolean -> "Boolean"
                        is Int -> "Int"
                        is Long -> "Long"
                        is Float -> "Float"
                        is String -> "String"
                        is Set<*> -> "StringSet"
                        else -> "Unknown"
                    }
                    if (value != null && type != "Unknown") {
                        wrapperMap[key] = mapOf("type" to type, "value" to value)
                    }
                }
                allConfigs[fileName] = wrapperMap
            }

            gson.toJson(allConfigs)
        } catch (e: Exception) {
            "{}"
        }
    }

    /**
     * Executes the export configs operation.
     *
     * @param outputStream [java.io.OutputStream] Target output stream.
     */
    fun exportConfigs(outputStream: java.io.OutputStream) {
        try {
            val json = getAllConfigsAsJsonString()
            outputStream.write(json.toByteArray())
            outputStream.flush()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the import configs operation.
     *
     * @param inputStream [java.io.InputStream] Target input stream.
     * @param keepPrefs [Boolean] Target keep prefs.
     * @return The resulting Boolean data.
     */
    fun importConfigs(inputStream: java.io.InputStream, keepPrefs: Boolean): Boolean {
        return try {
            val json = inputStream.bufferedReader().use { it.readText() }
            val allConfigs: Map<String, Map<String, Map<String, Any>>> =
                gson.fromJson(json, Map::class.java) as Map<String, Map<String, Map<String, Any>>>

            allConfigs.forEach { (fileName, prefWrapper) ->
                val p = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)

                // Preserve sensitive or volatile local state not present in backups
                val preservedValues = mutableMapOf<String, Any?>()
                val keysToPreserve = listOf(
                    KEY_GITHUB_ACCESS_TOKEN,
                    KEY_GITHUB_WORKFLOW_TOKEN,
                    KEY_SHIZUKU_AUTH_TOKEN,
                    "airsync_mac_connected",
                    KEY_SNOOZE_DISCOVERED_CHANNELS,
                    KEY_MAPS_DISCOVERED_CHANNELS,
                    KEY_SHUT_UP_ORIGINAL_SETTINGS
                )
                val macBatteryKeys = p.all.keys.filter { it.startsWith("mac_battery_") }
                (keysToPreserve + macBatteryKeys).forEach { key ->
                    if (p.contains(key)) {
                        preservedValues[key] = p.all[key]
                    }
                }

                p.edit().apply {
                    if (!keepPrefs) clear()

                    // Restore preserved values
                    preservedValues.forEach { (key, value) ->
                        if (value != null) {
                            when (value) {
                                is Boolean -> putBoolean(key, value)
                                is Int -> putInt(key, value)
                                is Long -> putLong(key, value)
                                is Float -> putFloat(key, value)
                                is String -> putString(key, value)
                                is Set<*> -> {
                                    @Suppress("UNCHECKED_CAST")
                                    putStringSet(key, value as Set<String>)
                                }
                            }
                        }
                    }

                    prefWrapper.forEach { (key, item) ->
                        // Do not import sensitive keys from the file even if they exist there
                        if (key == KEY_GITHUB_ACCESS_TOKEN || key == KEY_GITHUB_WORKFLOW_TOKEN ||
                            key == KEY_SHIZUKU_AUTH_TOKEN
                        ) {
                            return@forEach
                        }
                        val itemType = item["type"] as? String
                        val itemValue = item["value"]

                        if (itemType != null && itemValue != null) {
                            try {
                                when (itemType) {
                                    "Boolean" -> putBoolean(key, itemValue as Boolean)
                                    "Int" -> putInt(key, (itemValue as Double).toInt())
                                    "Long" -> putLong(key, (itemValue as Double).toLong())
                                    "Float" -> putFloat(key, (itemValue as Double).toFloat())
                                    "String" -> putString(key, itemValue as String)
                                    "StringSet" -> {
                                        @Suppress("UNCHECKED_CAST")
                                        putStringSet(key, (itemValue as List<String>).toSet())
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }.apply()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                inputStream.close()
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Executes the get bluetooth devices battery operation.
     * @return The resulting List<com data.
     */
    fun getBluetoothDevicesBattery(): List<com.sameerasw.essentials.utils.BluetoothBatteryUtils.BluetoothDeviceBattery> {
        val json = prefs.getString(KEY_BLUETOOTH_DEVICES_BATTERY, null) ?: return emptyList()
        return try {
            gson.fromJson(
                json,
                Array<com.sameerasw.essentials.utils.BluetoothBatteryUtils.BluetoothDeviceBattery>::class.java
            ).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Executes the save bluetooth devices battery operation.
     *
     * @param devices [List<com.sameerasw.essentials.utils.BluetoothBatteryUtils.BluetoothDeviceBattery>] Target devices.
     */
    fun saveBluetoothDevicesBattery(devices: List<com.sameerasw.essentials.utils.BluetoothBatteryUtils.BluetoothDeviceBattery>) {
        val json = gson.toJson(devices)
        putString(KEY_BLUETOOTH_DEVICES_BATTERY, json)
    }

    /**
     * Executes the is bluetooth devices enabled operation.
     * @return The resulting Boolean data.
     */
    fun isBluetoothDevicesEnabled(): Boolean = getBoolean(KEY_SHOW_BLUETOOTH_DEVICES, false)

    /**
     * Executes the set bluetooth devices enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setBluetoothDevicesEnabled(enabled: Boolean) =
        putBoolean(KEY_SHOW_BLUETOOTH_DEVICES, enabled)

    /**
     * Executes the get battery widget max devices operation.
     * @return The resulting Int data.
     */
    fun getBatteryWidgetMaxDevices(): Int = getInt(KEY_BATTERY_WIDGET_MAX_DEVICES, 8)

    /**
     * Executes the set battery widget max devices operation.
     *
     * @param count [Int] Target count.
     */
    fun setBatteryWidgetMaxDevices(count: Int) = putInt(KEY_BATTERY_WIDGET_MAX_DEVICES, count)

    /**
     * Executes the is battery widget background enabled operation.
     * @return The resulting Boolean data.
     */
    fun isBatteryWidgetBackgroundEnabled(): Boolean =
        getBoolean(KEY_BATTERY_WIDGET_BACKGROUND_ENABLED, true)

    /**
     * Executes the set battery widget background enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setBatteryWidgetBackgroundEnabled(enabled: Boolean) =
        putBoolean(KEY_BATTERY_WIDGET_BACKGROUND_ENABLED, enabled)

    /**
     * Executes the get pinned features operation.
     * @return The resulting List<String> data.
     */
    fun getPinnedFeatures(): List<String> {
        val json = prefs.getString(KEY_PINNED_FEATURES, null)
        return if (json != null) {
            try {
                gson.fromJson(json, Array<String>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    }

    /**
     * Executes the save pinned features operation.
     *
     * @param features [List<String>] Target features.
     */
    fun savePinnedFeatures(features: List<String>) {
        val json = gson.toJson(features)
        putString(KEY_PINNED_FEATURES, json)
    }

    /**
     * Executes the get pinned qs tiles operation.
     * @return The resulting List<String> data.
     */
    fun getPinnedQsTiles(): List<String> {
        val json = prefs.getString(KEY_PINNED_QS_TILES, null)
        return if (json != null) {
            try {
                gson.fromJson(json, Array<String>::class.java).toList()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    }

    /**
     * Executes the save pinned qs tiles operation.
     *
     * @param tiles [List<String>] Target tiles.
     */
    fun savePinnedQsTiles(tiles: List<String>) {
        val json = gson.toJson(tiles)
        putString(KEY_PINNED_QS_TILES, json)
    }

    /**
     * Executes the get recent searches operation.
     * @return The resulting List<com data.
     */
    fun getRecentSearches(): List<com.sameerasw.essentials.domain.model.SearchableItem> {
        val json = prefs.getString(KEY_RECENT_SEARCHES, null)
        return if (json != null) {
            try {
                gson.fromJson(
                    json,
                    Array<com.sameerasw.essentials.domain.model.SearchableItem>::class.java
                ).toList()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    }

    /**
     * Executes the save recent searches operation.
     *
     * @param items [List<com.sameerasw.essentials.domain.model.SearchableItem>] Target items.
     */
    fun saveRecentSearches(items: List<com.sameerasw.essentials.domain.model.SearchableItem>) {
        val json = gson.toJson(items)
        putString(KEY_RECENT_SEARCHES, json)
    }

    /**
     * Executes the get tracked repos operation.
     * @return The resulting List<TrackedRepo> data.
     */
    fun getTrackedRepos(): List<TrackedRepo> {
        val json = prefs.getString(KEY_TRACKED_REPOS, null) ?: return emptyList()
        return try {
            gson.fromJson(json, Array<TrackedRepo>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Executes the save tracked repos operation.
     *
     * @param repos [List<TrackedRepo>] Target repos.
     */
    fun saveTrackedRepos(repos: List<TrackedRepo>) {
        val json = gson.toJson(repos)
        prefs.edit().putString(KEY_TRACKED_REPOS, json).apply()
    }

    /**
     * Executes the add or update tracked repo operation.
     *
     * @param repo [TrackedRepo] Target repo.
     */
    fun addOrUpdateTrackedRepo(repo: TrackedRepo) {
        val current = getTrackedRepos().toMutableList()
        val index = current.indexOfFirst { it.fullName == repo.fullName }
        if (index != -1) {
            current[index] = repo
        } else {
            current.add(repo)
        }
        saveTrackedRepos(current)
    }

    /**
     * Executes the is shut up attempt shizuku restart enabled operation.
     * @return The resulting Boolean data.
     */
    fun isShutUpAttemptShizukuRestartEnabled(): Boolean =
        getBoolean(KEY_SHUT_UP_ATTEMPT_SHIZUKU_RESTART, true)

    /**
     * Executes the set shut up attempt shizuku restart enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setShutUpAttemptShizukuRestartEnabled(enabled: Boolean) =
        putBoolean(KEY_SHUT_UP_ATTEMPT_SHIZUKU_RESTART, enabled)

    /**
     * Executes the get shut up restore delay operation.
     * @return The resulting Int data.
     */
    fun getShutUpRestoreDelay(): Int =
        getInt(KEY_SHUT_UP_RESTORE_DELAY, 10)

    /**
     * Executes the set shut up restore delay operation.
     *
     * @param delaySeconds [Int] Target delay seconds.
     */
    fun setShutUpRestoreDelay(delaySeconds: Int) =
        putInt(KEY_SHUT_UP_RESTORE_DELAY, delaySeconds)

    /**
     * Executes the get shut up restore mode operation.
     * @return The resulting String data.
     */
    fun getShutUpRestoreMode(): String =
        prefs.getString(KEY_SHUT_UP_RESTORE_MODE, "Auto") ?: "Auto"

    /**
     * Executes the set shut up restore mode operation.
     *
     * @param mode [String] Target mode.
     */
    fun setShutUpRestoreMode(mode: String) =
        putString(KEY_SHUT_UP_RESTORE_MODE, mode)

    /**
     * Executes the get shizuku auth token operation.
     * @return The resulting String data.
     */
    fun getShizukuAuthToken(): String =
        prefs.getString(KEY_SHIZUKU_AUTH_TOKEN, "") ?: ""

    /**
     * Executes the set shizuku auth token operation.
     *
     * @param token [String] Target token.
     */
    fun setShizukuAuthToken(token: String) =
        putString(KEY_SHIZUKU_AUTH_TOKEN, token)

    /**
     * Executes the get pixel searchbar type operation.
     * @return The resulting String data.
     */
    fun getPixelSearchbarType(): String =
        prefs.getString(KEY_PIXEL_SEARCHBAR_TYPE, "empty") ?: "empty"

    /**
     * Executes the set pixel searchbar type operation.
     *
     * @param type [String] Target type.
     */
    fun setPixelSearchbarType(type: String) =
        putString(KEY_PIXEL_SEARCHBAR_TYPE, type)

    /**
     * Executes the get pixel searchbar date format operation.
     * @return The resulting String data.
     */
    fun getPixelSearchbarDateFormat(): String =
        prefs.getString(KEY_PIXEL_SEARCHBAR_DATE_FORMAT, "EEEE, MMMM d") ?: "EEEE, MMMM d"

    /**
     * Executes the set pixel searchbar date format operation.
     *
     * @param format [String] Target format.
     */
    fun setPixelSearchbarDateFormat(format: String) =
        putString(KEY_PIXEL_SEARCHBAR_DATE_FORMAT, format)

    /**
     * Executes the get pixel searchbar background pill operation.
     * @return The resulting Boolean data.
     */
    fun getPixelSearchbarBackgroundPill(): Boolean =
        prefs.getBoolean(KEY_PIXEL_SEARCHBAR_BACKGROUND_PILL, false)

    /**
     * Executes the set pixel searchbar background pill operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setPixelSearchbarBackgroundPill(enabled: Boolean) =
        putBoolean(KEY_PIXEL_SEARCHBAR_BACKGROUND_PILL, enabled)

    /**
     * Executes the get pixel searchbar widget id operation.
     * @return The resulting Int data.
     */
    fun getPixelSearchbarWidgetId(): Int =
        prefs.getInt(
            KEY_PIXEL_SEARCHBAR_WIDGET_ID,
            android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
        )

    /**
     * Executes the set pixel searchbar widget id operation.
     *
     * @param id [Int] Target id.
     */
    fun setPixelSearchbarWidgetId(id: Int) =
        prefs.edit().putInt(KEY_PIXEL_SEARCHBAR_WIDGET_ID, id).apply()

    /**
     * Executes the get pixel searchbar widget provider operation.
     * @return The resulting String? data.
     */
    fun getPixelSearchbarWidgetProvider(): String? =
        prefs.getString(KEY_PIXEL_SEARCHBAR_WIDGET_PROVIDER, null)

    /**
     * Executes the set pixel searchbar widget provider operation.
     *
     * @param provider [String?] Target provider.
     */
    fun setPixelSearchbarWidgetProvider(provider: String?) =
        if (provider == null) prefs.edit().remove(KEY_PIXEL_SEARCHBAR_WIDGET_PROVIDER).apply()
        else putString(KEY_PIXEL_SEARCHBAR_WIDGET_PROVIDER, provider)

    /**
     * Executes the get pixel searchbar scraped line1 operation.
     * @return The resulting String data.
     */
    fun getPixelSearchbarScrapedLine1(): String =
        prefs.getString(KEY_PIXEL_SEARCHBAR_SCRAPED_LINE1, "") ?: ""

    /**
     * Executes the set pixel searchbar scraped line1 operation.
     *
     * @param text [String] Target text.
     */
    fun setPixelSearchbarScrapedLine1(text: String) =
        putString(KEY_PIXEL_SEARCHBAR_SCRAPED_LINE1, text)

    /**
     * Executes the get pixel searchbar scraped line2 operation.
     * @return The resulting String data.
     */
    fun getPixelSearchbarScrapedLine2(): String =
        prefs.getString(KEY_PIXEL_SEARCHBAR_SCRAPED_LINE2, "") ?: ""

    /**
     * Executes the set pixel searchbar scraped line2 operation.
     *
     * @param text [String] Target text.
     */
    fun setPixelSearchbarScrapedLine2(text: String) =
        putString(KEY_PIXEL_SEARCHBAR_SCRAPED_LINE2, text)

    /**
     * Executes the get pixel searchbar widget padding h operation.
     * @return The resulting Int data.
     */
    fun getPixelSearchbarWidgetPaddingH(): Int =
        prefs.getInt(KEY_PIXEL_SEARCHBAR_WIDGET_PADDING_H, 0)

    /**
     * Executes the set pixel searchbar widget padding h operation.
     *
     * @param value [Int] Target value.
     */
    fun setPixelSearchbarWidgetPaddingH(value: Int) =
        prefs.edit().putInt(KEY_PIXEL_SEARCHBAR_WIDGET_PADDING_H, value).apply()

    /**
     * Executes the get pixel searchbar widget padding v operation.
     * @return The resulting Int data.
     */
    fun getPixelSearchbarWidgetPaddingV(): Int =
        prefs.getInt(KEY_PIXEL_SEARCHBAR_WIDGET_PADDING_V, 0)

    /**
     * Executes the set pixel searchbar widget padding v operation.
     *
     * @param value [Int] Target value.
     */
    fun setPixelSearchbarWidgetPaddingV(value: Int) =
        prefs.edit().putInt(KEY_PIXEL_SEARCHBAR_WIDGET_PADDING_V, value).apply()

    /**
     * Executes the get pixel searchbar tap action enabled operation.
     * @return The resulting Boolean data.
     */
    fun getPixelSearchbarTapActionEnabled(): Boolean =
        prefs.getBoolean(KEY_PIXEL_SEARCHBAR_TAP_ACTION_ENABLED, true)

    /**
     * Executes the set pixel searchbar tap action enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setPixelSearchbarTapActionEnabled(enabled: Boolean) =
        putBoolean(KEY_PIXEL_SEARCHBAR_TAP_ACTION_ENABLED, enabled)

    /**
     * Executes the get pixel searchbar widget revision operation.
     * @return The resulting Int data.
     */
    fun getPixelSearchbarWidgetRevision(): Int =
        prefs.getInt(KEY_PIXEL_SEARCHBAR_WIDGET_REVISION, 0)

    /**
     * Executes the increment pixel searchbar widget revision operation.
     */
    fun incrementPixelSearchbarWidgetRevision() {
        val current = getPixelSearchbarWidgetRevision()
        prefs.edit().putInt(KEY_PIXEL_SEARCHBAR_WIDGET_REVISION, current + 1).apply()
    }

    /**
     * Executes the get pixel searchbar music title operation.
     * @return The resulting String data.
     */
    fun getPixelSearchbarMusicTitle(): String =
        prefs.getString(KEY_PIXEL_SEARCHBAR_MUSIC_TITLE, "") ?: ""

    /**
     * Executes the set pixel searchbar music title operation.
     *
     * @param value [String] Target value.
     */
    fun setPixelSearchbarMusicTitle(value: String) =
        putString(KEY_PIXEL_SEARCHBAR_MUSIC_TITLE, value)

    /**
     * Executes the get pixel searchbar music artist operation.
     * @return The resulting String data.
     */
    fun getPixelSearchbarMusicArtist(): String =
        prefs.getString(KEY_PIXEL_SEARCHBAR_MUSIC_ARTIST, "") ?: ""

    /**
     * Executes the set pixel searchbar music artist operation.
     *
     * @param value [String] Target value.
     */
    fun setPixelSearchbarMusicArtist(value: String) =
        putString(KEY_PIXEL_SEARCHBAR_MUSIC_ARTIST, value)

    /**
     * Executes the get pixel searchbar music package operation.
     * @return The resulting String data.
     */
    fun getPixelSearchbarMusicPackage(): String =
        prefs.getString(KEY_PIXEL_SEARCHBAR_MUSIC_PACKAGE, "") ?: ""

    /**
     * Executes the set pixel searchbar music package operation.
     *
     * @param value [String] Target value.
     */
    fun setPixelSearchbarMusicPackage(value: String) =
        putString(KEY_PIXEL_SEARCHBAR_MUSIC_PACKAGE, value)

    /**
     * Executes the get edge lighting sweep selected shapes operation.
     * @return The resulting Set<String> data.
     */
    fun getEdgeLightingSweepSelectedShapes(): Set<String> {
        val defaultShapes =
            com.sameerasw.essentials.utils.AmbientMusicShapeHelper.allShapesWithNames.map { it.first }
                .toSet()
        val json = prefs.getString(KEY_EDGE_LIGHTING_SWEEP_SELECTED_SHAPES, null)
        return if (json != null) {
            try {
                gson.fromJson(json, Array<String>::class.java).toSet()
            } catch (e: Exception) {
                defaultShapes
            }
        } else {
            defaultShapes
        }
    }

    /**
     * Executes the save edge lighting sweep selected shapes operation.
     *
     * @param shapes [Set<String>] Target shapes.
     */
    fun saveEdgeLightingSweepSelectedShapes(shapes: Set<String>) {
        val json = gson.toJson(shapes)
        putString(KEY_EDGE_LIGHTING_SWEEP_SELECTED_SHAPES, json)
    }

    /**
     * Executes the remove tracked repo operation.
     *
     * @param fullName [String] Target full name.
     */
    fun removeTrackedRepo(fullName: String) {
        val current = getTrackedRepos().toMutableList()
        current.removeAll { it.fullName == fullName }
        saveTrackedRepos(current)
    }

    /**
     * Executes the get git hub token operation.
     * @return The resulting String? data.
     */
    fun getGitHubToken(): String? {
        return prefs.getString(KEY_GITHUB_ACCESS_TOKEN, null)
    }

    /**
     * Executes the save git hub token operation.
     *
     * @param token [String?] Target token.
     */
    fun saveGitHubToken(token: String?) {
        prefs.edit().putString(KEY_GITHUB_ACCESS_TOKEN, token).apply()
    }

    // observe token changes
    val gitHubToken: Flow<String?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_GITHUB_ACCESS_TOKEN) {
                trySend(getString(KEY_GITHUB_ACCESS_TOKEN))
            }
        }
        trySend(getString(KEY_GITHUB_ACCESS_TOKEN))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /**
     * Executes the get git hub workflow token operation.
     * @return The resulting String? data.
     */
    fun getGitHubWorkflowToken(): String? {
        return prefs.getString(KEY_GITHUB_WORKFLOW_TOKEN, null)
    }

    /**
     * Executes the save git hub workflow token operation.
     *
     * @param token [String?] Target token.
     */
    fun saveGitHubWorkflowToken(token: String?) {
        prefs.edit().putString(KEY_GITHUB_WORKFLOW_TOKEN, token).apply()
    }

    val gitHubWorkflowToken: Flow<String?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_GITHUB_WORKFLOW_TOKEN) {
                trySend(getString(KEY_GITHUB_WORKFLOW_TOKEN))
            }
        }
        trySend(getString(KEY_GITHUB_WORKFLOW_TOKEN))
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /**
     * Executes the save git hub user operation.
     *
     * @param user [GitHubUser?] Target user.
     */
    fun saveGitHubUser(user: GitHubUser?) {
        if (user == null) {
            prefs.edit().remove(KEY_GITHUB_USER_PROFILE).apply()
        } else {
            val json = gson.toJson(user)
            prefs.edit().putString(KEY_GITHUB_USER_PROFILE, json).apply()
        }
    }

    /**
     * Executes the get git hub user operation.
     * @return The resulting GitHubUser? data.
     */
    fun getGitHubUser(): GitHubUser? {
        val json = prefs.getString(KEY_GITHUB_USER_PROFILE, null) ?: return null
        return try {
            gson.fromJson(json, GitHubUser::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Executes the is user dictionary enabled operation.
     * @return The resulting Boolean data.
     */
    fun isUserDictionaryEnabled(): Boolean = getBoolean(KEY_USER_DICTIONARY_ENABLED, false)

    /**
     * Executes the set user dictionary enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setUserDictionaryEnabled(enabled: Boolean) =
        putBoolean(KEY_USER_DICTIONARY_ENABLED, enabled)

    /**
     * Executes the is accented characters enabled operation.
     * @return The resulting Boolean data.
     */
    fun isAccentedCharactersEnabled(): Boolean = getBoolean(KEY_KEYBOARD_ACCENTED_CHARACTERS, false)

    /**
     * Executes the set accented characters enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setAccentedCharactersEnabled(enabled: Boolean) =
        putBoolean(KEY_KEYBOARD_ACCENTED_CHARACTERS, enabled)

    /**
     * Executes the is battery notification enabled operation.
     * @return The resulting Boolean data.
     */
    fun isBatteryNotificationEnabled(): Boolean =
        getBoolean(KEY_BATTERY_NOTIFICATION_ENABLED, false)

    /**
     * Executes the set battery notification enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setBatteryNotificationEnabled(enabled: Boolean) =
        putBoolean(KEY_BATTERY_NOTIFICATION_ENABLED, enabled)

    /**
     * Executes the is enable unsupported features operation.
     * @return The resulting Boolean data.
     */
    fun isEnableUnsupportedFeatures(): Boolean =
        getBoolean(KEY_ENABLE_UNSUPPORTED_FEATURES, false)

    /**
     * Executes the set enable unsupported features operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setEnableUnsupportedFeatures(enabled: Boolean) =
        putBoolean(KEY_ENABLE_UNSUPPORTED_FEATURES, enabled)

    // Live Wallpaper Helpers
    private val liveWallpaperPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(LIVE_WALLPAPER_PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Executes the get live wallpaper selected video operation.
     * @return The resulting String data.
     */
    fun getLiveWallpaperSelectedVideo(): String =
        liveWallpaperPrefs.getString(
            KEY_LIVE_WALLPAPER_SELECTED_VIDEO,
            LIVE_WALLPAPER_DEFAULT_VIDEO
        )
            ?: LIVE_WALLPAPER_DEFAULT_VIDEO

    /**
     * Executes the save live wallpaper selected video operation.
     *
     * @param video [String] Target video.
     */
    fun saveLiveWallpaperSelectedVideo(video: String) =
        liveWallpaperPrefs.edit().putString(KEY_LIVE_WALLPAPER_SELECTED_VIDEO, video).apply()

    /**
     * Executes the get live wallpaper playback trigger operation.
     * @return The resulting String data.
     */
    fun getLiveWallpaperPlaybackTrigger(): String =
        liveWallpaperPrefs.getString(
            KEY_LIVE_WALLPAPER_PLAYBACK_TRIGGER,
            LIVE_WALLPAPER_TRIGGER_UNLOCK
        )
            ?: LIVE_WALLPAPER_TRIGGER_UNLOCK

    /**
     * Executes the save live wallpaper playback trigger operation.
     *
     * @param trigger [String] Target trigger.
     */
    fun saveLiveWallpaperPlaybackTrigger(trigger: String) =
        liveWallpaperPrefs.edit().putString(KEY_LIVE_WALLPAPER_PLAYBACK_TRIGGER, trigger).apply()

    /**
     * Executes the get live wallpaper custom videos operation.
     * @return The resulting List<String> data.
     */
    fun getLiveWallpaperCustomVideos(): List<String> {
        val stored = liveWallpaperPrefs.getString(KEY_LIVE_WALLPAPER_CUSTOM_VIDEOS, "") ?: ""
        val delimiter = if (!stored.contains("\n") && stored.contains(",")) "," else "\n"
        return stored.split(delimiter)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    /**
     * Executes the save live wallpaper custom videos operation.
     *
     * @param videos [List<String>] Target videos.
     */
    fun saveLiveWallpaperCustomVideos(videos: List<String>) =
        liveWallpaperPrefs.edit()
            .putString(
                KEY_LIVE_WALLPAPER_CUSTOM_VIDEOS,
                videos.filter { it.isNotEmpty() }.distinct().joinToString("\n")
            ).apply()

    /**
     * Executes the add live wallpaper custom video operation.
     *
     * @param uri [String] Target uri.
     */
    fun addLiveWallpaperCustomVideo(uri: String) {
        val current = getLiveWallpaperCustomVideos().toMutableList()
        if (current.contains(uri)) {
            current.remove(uri)
        }
        current.add(0, uri)
        saveLiveWallpaperCustomVideos(if (current.size > 5) current.take(5) else current)
    }

    /**
     * Executes the get live wallpaper available videos operation.
     * @return The resulting List<String> data.
     */
    fun getLiveWallpaperAvailableVideos(): List<String> {
        val raws = com.sameerasw.essentials.R.raw::class.java.fields.mapNotNull { field ->
            try {
                if (field.name.startsWith("loop_")) field.name else null
            } catch (e: Exception) {
                null
            }
        }
        return (raws + getLiveWallpaperCustomVideos()).filter { it.isNotBlank() }.distinct()
    }

    /**
     * Executes the remove live wallpaper custom video operation.
     *
     * @param videoUri [String] Target video uri.
     */
    fun removeLiveWallpaperCustomVideo(videoUri: String) {
        val current = getLiveWallpaperCustomVideos().toMutableList()
        val removed = current.removeAll { it == videoUri || it.isBlank() }
        if (removed) {
            saveLiveWallpaperCustomVideos(current)
            // If the removed video was selected, revert to default
            if (getLiveWallpaperSelectedVideo() == videoUri) {
                saveLiveWallpaperSelectedVideo(LIVE_WALLPAPER_DEFAULT_VIDEO)
            }
        }
    }

    /**
     * Executes the get daily wallpaper apply home operation.
     * @return The resulting Boolean data.
     */
    fun getDailyWallpaperApplyHome(): Boolean =
        getBoolean(KEY_DAILY_WALLPAPER_APPLY_HOME, true)

    /**
     * Executes the set daily wallpaper apply home operation.
     *
     * @param value [Boolean] Target value.
     */
    fun setDailyWallpaperApplyHome(value: Boolean) =
        putBoolean(KEY_DAILY_WALLPAPER_APPLY_HOME, value)

    /**
     * Executes the get daily wallpaper apply lock operation.
     * @return The resulting Boolean data.
     */
    fun getDailyWallpaperApplyLock(): Boolean =
        getBoolean(KEY_DAILY_WALLPAPER_APPLY_LOCK, true)

    /**
     * Executes the set daily wallpaper apply lock operation.
     *
     * @param value [Boolean] Target value.
     */
    fun setDailyWallpaperApplyLock(value: Boolean) =
        putBoolean(KEY_DAILY_WALLPAPER_APPLY_LOCK, value)

    /**
     * Executes the get font scale operation.
     * @return The resulting Float data.
     */
    fun getFontScale(): Float {
        return try {
            android.provider.Settings.System.getFloat(
                context.contentResolver,
                android.provider.Settings.System.FONT_SCALE
            )
        } catch (e: Exception) {
            1.0f
        }
    }

    /**
     * Executes the set font scale operation.
     *
     * @param scale [Float] Target scale.
     */
    fun setFontScale(scale: Float) {
        putFloat(KEY_FONT_SCALE, scale)
        try {
            android.provider.Settings.System.putFloat(
                context.contentResolver,
                android.provider.Settings.System.FONT_SCALE,
                scale
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the get font weight operation.
     * @return The resulting Int data.
     */
    fun getFontWeight(): Int {
        return try {
            android.provider.Settings.Secure.getInt(
                context.contentResolver,
                "font_weight_adjustment"
            )
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Executes the set font weight operation.
     *
     * @param weight [Int] Target weight.
     */
    fun setFontWeight(weight: Int) {
        putInt(KEY_FONT_WEIGHT, weight)
        try {
            android.provider.Settings.Secure.putInt(
                context.contentResolver,
                "font_weight_adjustment",
                weight
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the get smallest width operation.
     * @return The resulting Int data.
     */
    fun getSmallestWidth(): Int {
        val forcedDensity = try {
            android.provider.Settings.Secure.getInt(
                context.contentResolver,
                "display_density_forced"
            )
        } catch (e: Exception) {
            0
        }
        if (forcedDensity > 0) {
            val metrics = context.resources.displayMetrics
            val widthPx = Math.min(metrics.widthPixels, metrics.heightPixels)
            return (widthPx * 160) / forcedDensity
        }
        return context.resources.configuration.smallestScreenWidthDp
    }

    /**
     * Executes the set smallest width operation.
     *
     * @param widthDp [Int] Target width dp.
     */
    fun setSmallestWidth(widthDp: Int) {
        putInt(KEY_SMALLEST_WIDTH, widthDp)
        val metrics = context.resources.displayMetrics
        val widthPx = Math.min(metrics.widthPixels, metrics.heightPixels)
        val density = (widthPx * 160) / widthDp

        val command = "wm density $density"
        if (ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasPermission()) {
            ShizukuUtils.runCommand(command)
        } else if (RootUtils.isRootAvailable()) {
            RootUtils.runCommand(command)
        } else {
            try {
                android.provider.Settings.Secure.putInt(
                    context.contentResolver,
                    "display_density_forced",
                    density
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Executes the reset smallest width operation.
     */
    fun resetSmallestWidth() {
        val command = "wm density reset"
        if (ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasPermission()) {
            ShizukuUtils.runCommand(command)
        } else if (RootUtils.isRootAvailable()) {
            RootUtils.runCommand(command)
        } else {
            try {
                android.provider.Settings.Secure.putString(
                    context.contentResolver,
                    "display_density_forced",
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        remove(KEY_SMALLEST_WIDTH)
    }

    /**
     * Executes the get animation scale operation.
     *
     * @param key [String] Target key.
     * @return The resulting Float data.
     */
    fun getAnimationScale(key: String): Float {
        return try {
            android.provider.Settings.Global.getFloat(context.contentResolver, key)
        } catch (e: Exception) {
            1.0f
        }
    }

    /**
     * Executes the set animation scale operation.
     *
     * @param key [String] Target key.
     * @param scale [Float] Target scale.
     */
    fun setAnimationScale(key: String, scale: Float) {
        when (key) {
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE -> putFloat(
                KEY_ANIMATOR_DURATION_SCALE,
                scale
            )

            android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE -> putFloat(
                KEY_TRANSITION_ANIMATION_SCALE,
                scale
            )

            android.provider.Settings.Global.WINDOW_ANIMATION_SCALE -> putFloat(
                KEY_WINDOW_ANIMATION_SCALE,
                scale
            )
        }
        try {
            android.provider.Settings.Global.putFloat(context.contentResolver, key, scale)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the sync system settings with saved operation.
     */
    fun syncSystemSettingsWithSaved() {
        try {
            if (contains(KEY_FONT_SCALE)) {
                setFontScale(getFloat(KEY_FONT_SCALE, 1.0f))
            }
            if (contains(KEY_FONT_WEIGHT)) {
                setFontWeight(getInt(KEY_FONT_WEIGHT, 0))
            }
            if (contains(KEY_ANIMATOR_DURATION_SCALE)) {
                setAnimationScale(
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                    getFloat(KEY_ANIMATOR_DURATION_SCALE, 1.0f)
                )
            }
            if (contains(KEY_TRANSITION_ANIMATION_SCALE)) {
                setAnimationScale(
                    android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
                    getFloat(KEY_TRANSITION_ANIMATION_SCALE, 1.0f)
                )
            }
            if (contains(KEY_WINDOW_ANIMATION_SCALE)) {
                setAnimationScale(
                    android.provider.Settings.Global.WINDOW_ANIMATION_SCALE,
                    getFloat(KEY_WINDOW_ANIMATION_SCALE, 1.0f)
                )
            }
            if (contains(KEY_REFRESH_RATE_FIXED) || contains(KEY_REFRESH_RATE_MIN) || contains(
                    KEY_REFRESH_RATE_PEAK
                )
            ) {
                val mode = getRefreshRateMode()
                val fixed = getFloat(KEY_REFRESH_RATE_FIXED, 0f)
                val min = getFloat(KEY_REFRESH_RATE_MIN, 0f)
                val peak = getFloat(KEY_REFRESH_RATE_PEAK, 0f)

                if (fixed <= 0f && min <= 0f && peak <= 0f) {
                    com.sameerasw.essentials.utils.RefreshRateUtils.resetRefreshRate(
                        context,
                        shouldRestoreInfinityPeakOnRefreshRateReset()
                    )
                } else if (mode == com.sameerasw.essentials.utils.RefreshRateUtils.MODE_RANGE && min > 0f && peak > 0f) {
                    com.sameerasw.essentials.utils.RefreshRateUtils.applyRangeRefreshRate(
                        context,
                        min,
                        peak
                    )
                } else if (fixed > 0f || peak > 0f) {
                    com.sameerasw.essentials.utils.RefreshRateUtils.applyFixedRefreshRate(
                        context,
                        if (fixed > 0f) fixed else peak
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the is aod enabled operation.
     * @return The resulting Boolean data.
     */
    fun isAodEnabled(): Boolean {
        return android.provider.Settings.Secure.getInt(
            context.contentResolver,
            "doze_always_on",
            1
        ) == 1
    }

    /**
     * Updates System Secure setting for Doze Always-On-Display (AOD).
     *
     * @param enabled [Boolean] True to enable Always-On-Display, false to disable.
     */
    fun setAodEnabled(enabled: Boolean) {
        try {
            android.provider.Settings.Secure.putInt(
                context.contentResolver,
                "doze_always_on",
                if (enabled) 1 else 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the get private dns presets operation.
     * @return The resulting List<DnsPreset> data.
     */
    fun getPrivateDnsPresets(): List<DnsPreset> {
        val json = prefs.getString(KEY_PRIVATE_DNS_PRESETS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, Array<DnsPreset>::class.java).toList()
            } catch (e: Exception) {
                getDefaultDnsPresets()
            }
        } else {
            getDefaultDnsPresets().also { savePrivateDnsPresets(it) }
        }
    }

    private fun getDefaultDnsPresets(): List<DnsPreset> {
        return listOf(
            DnsPreset(
                name = context.getString(com.sameerasw.essentials.R.string.dns_preset_adguard),
                hostname = "dns.adguard.com",
                isDefault = true
            ),
            DnsPreset(
                name = context.getString(com.sameerasw.essentials.R.string.dns_preset_google),
                hostname = "dns.google",
                isDefault = true
            ),
            DnsPreset(
                name = context.getString(com.sameerasw.essentials.R.string.dns_preset_cloudflare),
                hostname = "1dot1dot1dot1.cloudflare-dns.com",
                isDefault = true
            ),
            DnsPreset(
                name = context.getString(com.sameerasw.essentials.R.string.dns_preset_quad9),
                hostname = "dns.quad9.net",
                isDefault = true
            ),
            DnsPreset(
                name = context.getString(com.sameerasw.essentials.R.string.dns_preset_cleanbrowsing),
                hostname = "adult-filter-dns.cleanbrowsing.org",
                isDefault = true
            )
        )
    }

    /**
     * Executes the save private dns presets operation.
     *
     * @param presets [List<DnsPreset>] Target presets.
     */
    fun savePrivateDnsPresets(presets: List<DnsPreset>) {
        val json = gson.toJson(presets)
        putString(KEY_PRIVATE_DNS_PRESETS, json)
    }

    /**
     * Executes the reset private dns presets operation.
     */
    fun resetPrivateDnsPresets() {
        savePrivateDnsPresets(getDefaultDnsPresets())
    }

    /**
     * Executes the get ambient music glance album art mode operation.
     * @return The resulting String data.
     */
    fun getAmbientMusicGlanceAlbumArtMode(): String =
        prefs.getString(KEY_AMBIENT_MUSIC_GLANCE_ALBUM_ART_MODE, "default") ?: "default"

    /**
     * Executes the set ambient music glance album art mode operation.
     *
     * @param mode [String] Target mode.
     */
    fun setAmbientMusicGlanceAlbumArtMode(mode: String) =
        prefs.edit().putString(KEY_AMBIENT_MUSIC_GLANCE_ALBUM_ART_MODE, mode).apply()

    /**
     * Executes the get ambient music glance clock size operation.
     * @return The resulting Int data.
     */
    fun getAmbientMusicGlanceClockSize(): Int =
        prefs.getInt(KEY_AMBIENT_MUSIC_GLANCE_CLOCK_SIZE, 80)

    /**
     * Executes the set ambient music glance clock size operation.
     *
     * @param size [Int] Target size.
     */
    fun setAmbientMusicGlanceClockSize(size: Int) =
        prefs.edit().putInt(KEY_AMBIENT_MUSIC_GLANCE_CLOCK_SIZE, size).apply()

    /**
     * Executes the get ambient music glance clock weight operation.
     * @return The resulting Int data.
     */
    fun getAmbientMusicGlanceClockWeight(): Int =
        prefs.getInt(KEY_AMBIENT_MUSIC_GLANCE_CLOCK_WEIGHT, 400)

    /**
     * Executes the set ambient music glance clock weight operation.
     *
     * @param weight [Int] Target weight.
     */
    fun setAmbientMusicGlanceClockWeight(weight: Int) =
        prefs.edit().putInt(KEY_AMBIENT_MUSIC_GLANCE_CLOCK_WEIGHT, weight).apply()

    /**
     * Executes the get ambient music glance clock width operation.
     * @return The resulting Int data.
     */
    fun getAmbientMusicGlanceClockWidth(): Int =
        prefs.getInt(KEY_AMBIENT_MUSIC_GLANCE_CLOCK_WIDTH, 100)

    /**
     * Executes the set ambient music glance clock width operation.
     *
     * @param width [Int] Target width.
     */
    fun setAmbientMusicGlanceClockWidth(width: Int) =
        prefs.edit().putInt(KEY_AMBIENT_MUSIC_GLANCE_CLOCK_WIDTH, width).apply()

    /**
     * Executes the get ambient music glance clock roundness operation.
     * @return The resulting Int data.
     */
    fun getAmbientMusicGlanceClockRoundness(): Int =
        prefs.getInt(KEY_AMBIENT_MUSIC_GLANCE_CLOCK_ROUNDNESS, 50)

    /**
     * Executes the set ambient music glance clock roundness operation.
     *
     * @param roundness [Int] Target roundness.
     */
    fun setAmbientMusicGlanceClockRoundness(roundness: Int) =
        prefs.edit().putInt(KEY_AMBIENT_MUSIC_GLANCE_CLOCK_ROUNDNESS, roundness).apply()

    /**
     * Executes the is ambient music glance force fill while charging enabled operation.
     * @return The resulting Boolean data.
     */
    fun isAmbientMusicGlanceForceFillWhileChargingEnabled(): Boolean =
        prefs.getBoolean(KEY_AMBIENT_MUSIC_GLANCE_FORCE_FILL_WHILE_CHARGING, false)

    /**
     * Executes the set ambient music glance force fill while charging enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setAmbientMusicGlanceForceFillWhileChargingEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_AMBIENT_MUSIC_GLANCE_FORCE_FILL_WHILE_CHARGING, enabled).apply()

    /**
     * Executes the is ambient music glance respect notifications enabled operation.
     * @return The resulting Boolean data.
     */
    fun isAmbientMusicGlanceRespectNotificationsEnabled(): Boolean =
        prefs.getBoolean(KEY_AMBIENT_MUSIC_GLANCE_RESPECT_NOTIFICATIONS, true)

    /**
     * Executes the set ambient music glance respect notifications enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setAmbientMusicGlanceRespectNotificationsEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_AMBIENT_MUSIC_GLANCE_RESPECT_NOTIFICATIONS, enabled).apply()

    // Notification Glance Settings

    fun getScaleAnimationsMode(): String =
        getString(KEY_SCALE_ANIMATIONS_MODE, "default") ?: "default"

    /**
     * Executes the set scale animations mode operation.
     *
     * @param mode [String] Target mode.
     */
    fun setScaleAnimationsMode(mode: String) = putString(KEY_SCALE_ANIMATIONS_MODE, mode)

    /**
     * Executes the get refresh rate mode operation.
     * @return The resulting String data.
     */
    fun getRefreshRateMode(): String =
        getString(KEY_REFRESH_RATE_MODE, com.sameerasw.essentials.utils.RefreshRateUtils.MODE_FIXED)
            ?: com.sameerasw.essentials.utils.RefreshRateUtils.MODE_FIXED

    /**
     * Executes the set refresh rate mode operation.
     *
     * @param mode [String] Target mode.
     */
    fun setRefreshRateMode(mode: String) = putString(KEY_REFRESH_RATE_MODE, mode)

    /**
     * Executes the save refresh rate state operation.
     *
     * @param mode [String] Target mode.
     * @param fixed [Float] Target fixed.
     * @param min [Float] Target min.
     * @param peak [Float] Target peak.
     */
    fun saveRefreshRateState(mode: String, fixed: Float, min: Float, peak: Float) {
        putString(KEY_REFRESH_RATE_MODE, mode)
        putFloat(KEY_REFRESH_RATE_FIXED, fixed)
        putFloat(KEY_REFRESH_RATE_MIN, min)
        putFloat(KEY_REFRESH_RATE_PEAK, peak)
    }

    /**
     * Executes the should restore infinity peak on refresh rate reset operation.
     * @return The resulting Boolean data.
     */
    fun shouldRestoreInfinityPeakOnRefreshRateReset(): Boolean =
        getBoolean(KEY_REFRESH_RATE_DEFAULT_PEAK_INFINITY, false)

    /**
     * Executes the set restore infinity peak on refresh rate reset operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setRestoreInfinityPeakOnRefreshRateReset(enabled: Boolean) =
        putBoolean(KEY_REFRESH_RATE_DEFAULT_PEAK_INFINITY, enabled)

    /**
     * Executes the get scale animations profile operation.
     *
     * @param mode [String] Target mode.
     * @return The resulting ScaleAnimationsProfile data.
     */
    fun getScaleAnimationsProfile(mode: String): ScaleAnimationsProfile {
        val key =
            if (mode == "glove") KEY_SCALE_ANIMATIONS_GLOVE_PROFILE else KEY_SCALE_ANIMATIONS_DEFAULT_PROFILE
        val json = prefs.getString(key, null)
        return if (json != null) {
            try {
                gson.fromJson(json, ScaleAnimationsProfile::class.java)
            } catch (e: Exception) {
                getDefaultScaleAnimationsProfile(mode)
            }
        } else {
            getDefaultScaleAnimationsProfile(mode)
        }
    }

    private fun getDefaultScaleAnimationsProfile(mode: String): ScaleAnimationsProfile {
        return if (mode == "glove") {
            ScaleAnimationsProfile(
                fontScale = 1.25f,
                smallestWidth = 385,
                touchSensitivityEnabled = true,
                autoRotateEnabled = true,
                screenTimeout = 60000L
            )
        } else {
            ScaleAnimationsProfile()
        }
    }

    /**
     * Executes the save scale animations profile operation.
     *
     * @param mode [String] Target mode.
     * @param profile [ScaleAnimationsProfile] Target profile.
     */
    fun saveScaleAnimationsProfile(mode: String, profile: ScaleAnimationsProfile) {
        val key =
            if (mode == "glove") KEY_SCALE_ANIMATIONS_GLOVE_PROFILE else KEY_SCALE_ANIMATIONS_DEFAULT_PROFILE
        val json = gson.toJson(profile)
        putString(key, json)
    }

    /**
     * Executes the get touch sensitivity enabled operation.
     * @return The resulting Boolean data.
     */
    fun getTouchSensitivityEnabled(): Boolean {
        return try {
            android.provider.Settings.Secure.getInt(
                context.contentResolver,
                "touch_sensitivity_enabled",
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Executes the set touch sensitivity enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setTouchSensitivityEnabled(enabled: Boolean) {
        try {
            android.provider.Settings.Secure.putInt(
                context.contentResolver,
                "touch_sensitivity_enabled",
                if (enabled) 1 else 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the get auto rotate enabled operation.
     * @return The resulting Boolean data.
     */
    fun getAutoRotateEnabled(): Boolean {
        return try {
            android.provider.Settings.System.getInt(
                context.contentResolver,
                android.provider.Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Executes the set auto rotate enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setAutoRotateEnabled(enabled: Boolean) {
        try {
            android.provider.Settings.System.putInt(
                context.contentResolver,
                android.provider.Settings.System.ACCELEROMETER_ROTATION,
                if (enabled) 1 else 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the get screen timeout operation.
     * @return The resulting Long data.
     */
    fun getScreenTimeout(): Long {
        return try {
            android.provider.Settings.System.getLong(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_OFF_TIMEOUT,
                30000L
            )
        } catch (e: Exception) {
            30000L
        }
    }

    /**
     * Executes the set screen timeout operation.
     *
     * @param timeoutMs [Long] Target timeout ms.
     */
    fun setScreenTimeout(timeoutMs: Long) {
        try {
            android.provider.Settings.System.putLong(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_OFF_TIMEOUT,
                timeoutMs
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Executes the get lock screen clock weight operation.
     * @return The resulting Int data.
     */
    fun getLockScreenClockWeight(): Int = getInt(KEY_LOCK_SCREEN_CLOCK_WEIGHT, 300)

    /**
     * Executes the set lock screen clock weight operation.
     *
     * @param value [Int] Target value.
     */
    fun setLockScreenClockWeight(value: Int) = putInt(KEY_LOCK_SCREEN_CLOCK_WEIGHT, value)

    /**
     * Executes the get lock screen clock width operation.
     * @return The resulting Int data.
     */
    fun getLockScreenClockWidth(): Int = getInt(KEY_LOCK_SCREEN_CLOCK_WIDTH, 116)

    /**
     * Executes the set lock screen clock width operation.
     *
     * @param value [Int] Target value.
     */
    fun setLockScreenClockWidth(value: Int) = putInt(KEY_LOCK_SCREEN_CLOCK_WIDTH, value)

    /**
     * Executes the get lock screen clock grade operation.
     * @return The resulting Int data.
     */
    fun getLockScreenClockGrade(): Int = getInt(KEY_LOCK_SCREEN_CLOCK_GRADE, 0)

    /**
     * Executes the set lock screen clock grade operation.
     *
     * @param value [Int] Target value.
     */
    fun setLockScreenClockGrade(value: Int) = putInt(KEY_LOCK_SCREEN_CLOCK_GRADE, value)

    /**
     * Executes the get lock screen clock roundness operation.
     * @return The resulting Int data.
     */
    fun getLockScreenClockRoundness(): Int = getInt(KEY_LOCK_SCREEN_CLOCK_ROUNDNESS, 100)

    /**
     * Executes the set lock screen clock roundness operation.
     *
     * @param value [Int] Target value.
     */
    fun setLockScreenClockRoundness(value: Int) = putInt(KEY_LOCK_SCREEN_CLOCK_ROUNDNESS, value)

    /**
     * Executes the get lock screen clock color tone operation.
     * @return The resulting Int data.
     */
    fun getLockScreenClockColorTone(): Int = getInt(KEY_LOCK_SCREEN_CLOCK_COLOR_TONE, 75)

    /**
     * Executes the set lock screen clock color tone operation.
     *
     * @param value [Int] Target value.
     */
    fun setLockScreenClockColorTone(value: Int) = putInt(KEY_LOCK_SCREEN_CLOCK_COLOR_TONE, value)

    /**
     * Executes the get lock screen clock selected color id operation.
     * @return The resulting String data.
     */
    fun getLockScreenClockSelectedColorId(): String =
        getString(KEY_LOCK_SCREEN_CLOCK_SELECTED_COLOR_ID, "DEFAULT") ?: "DEFAULT"

    /**
     * Executes the set lock screen clock selected color id operation.
     *
     * @param value [String] Target value.
     */
    fun setLockScreenClockSelectedColorId(value: String) =
        putString(KEY_LOCK_SCREEN_CLOCK_SELECTED_COLOR_ID, value)

    /**
     * Executes the get lock screen clock seed color operation.
     * @return The resulting Int data.
     */
    fun getLockScreenClockSeedColor(): Int = getInt(KEY_LOCK_SCREEN_CLOCK_SEED_COLOR, 0)

    /**
     * Executes the set lock screen clock seed color operation.
     *
     * @param value [Int] Target value.
     */
    fun setLockScreenClockSeedColor(value: Int) = putInt(KEY_LOCK_SCREEN_CLOCK_SEED_COLOR, value)
}

