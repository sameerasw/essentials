/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models
 * File: AppPermission.kt
 * Description: Enum defining all app permissions with keys, localized titles, and icons.
 */

package com.sameerasw.essentials.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sameerasw.essentials.R

enum class AppPermission(
    val key: String,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val aliases: List<String> = emptyList(),
) {
    ACCESSIBILITY(
        key = "ACCESSIBILITY",
        titleRes = R.string.perm_accessibility_title,
        iconRes = R.drawable.rounded_settings_accessibility_24,
    ),
    WRITE_SECURE_SETTINGS(
        key = "WRITE_SECURE_SETTINGS",
        titleRes = R.string.perm_write_secure_title,
        iconRes = R.drawable.rounded_security_24,
    ),
    NOTIFICATION_LISTENER(
        key = "NOTIFICATION_LISTENER",
        titleRes = R.string.perm_notif_listener_title,
        iconRes = R.drawable.rounded_notifications_unread_24,
    ),
    DRAW_OVERLAYS(
        key = "DRAW_OVERLAYS",
        titleRes = R.string.perm_overlay_title,
        iconRes = R.drawable.rounded_magnify_fullscreen_24,
        aliases = listOf("DRAW_OVER_OTHER_APPS"),
    ),
    WRITE_SETTINGS(
        key = "WRITE_SETTINGS",
        titleRes = R.string.perm_write_settings_title,
        iconRes = R.drawable.rounded_security_24,
    ),
    NOTIFICATION_POLICY(
        key = "NOTIFICATION_POLICY",
        titleRes = R.string.perm_notif_policy_title,
        iconRes = R.drawable.rounded_volume_up_24,
    ),
    POST_NOTIFICATIONS(
        key = "POST_NOTIFICATIONS",
        titleRes = R.string.permission_post_notifications_title,
        iconRes = R.drawable.rounded_notifications_unread_24,
    ),
    READ_PHONE_STATE(
        key = "READ_PHONE_STATE",
        titleRes = R.string.permission_read_phone_state_title,
        iconRes = R.drawable.rounded_android_cell_dual_4_bar_24,
    ),
    LOCATION(
        key = "LOCATION",
        titleRes = R.string.perm_location_title,
        iconRes = R.drawable.rounded_location_on_24,
    ),
    BACKGROUND_LOCATION(
        key = "BACKGROUND_LOCATION",
        titleRes = R.string.perm_bg_location_title,
        iconRes = R.drawable.rounded_location_on_24,
    ),
    DEVICE_ADMIN(
        key = "DEVICE_ADMIN",
        titleRes = R.string.perm_device_admin_title,
        iconRes = R.drawable.rounded_admin_panel_settings_24,
    ),
    ROOT(
        key = "ROOT",
        titleRes = R.string.perm_root_title,
        iconRes = R.drawable.rounded_numbers_24,
    ),
    SHIZUKU(
        key = "SHIZUKU",
        titleRes = R.string.perm_shizuku_title,
        iconRes = R.drawable.rounded_adb_24,
    ),
    READ_CALENDAR(
        key = "READ_CALENDAR",
        titleRes = R.string.perm_calendar_title,
        iconRes = R.drawable.rounded_calendar_today_24,
    ),
    USAGE_STATS(
        key = "USAGE_STATS",
        titleRes = R.string.perm_usage_stats_title,
        iconRes = R.drawable.rounded_data_usage_24,
    ),
    DEFAULT_BROWSER(
        key = "DEFAULT_BROWSER",
        titleRes = R.string.perm_default_browser_title,
        iconRes = R.drawable.rounded_open_in_browser_24,
    ),
    BLUETOOTH(
        key = "BLUETOOTH_CONNECT",
        titleRes = R.string.perm_nearby_devices_title,
        iconRes = R.drawable.rounded_bluetooth_24,
        aliases = listOf("BLUETOOTH_SCAN", "BLUETOOTH"),
    ),
    REQUEST_INSTALL_PACKAGES(
        key = "REQUEST_INSTALL_PACKAGES",
        titleRes = R.string.perm_install_packages_title,
        iconRes = R.drawable.rounded_mobile_arrow_down_24,
    ),
    READ_CONTACTS(
        key = "READ_CONTACTS",
        titleRes = R.string.perm_contacts_title,
        iconRes = R.drawable.rounded_call_24,
    ),
    WATCH_CALL_SYNC(
        key = "WATCH_CALL_SYNC",
        titleRes = R.string.watch_call_sync_title,
        iconRes = R.drawable.rounded_mobile_sound_24,
        aliases = listOf("ANSWER_PHONE_CALLS", "READ_CALL_LOG"),
    ),
    STORAGE(
        key = "STORAGE",
        titleRes = R.string.perm_storage_title,
        iconRes = R.drawable.rounded_image_24,
        aliases = listOf("READ_MEDIA_IMAGES", "READ_EXTERNAL_STORAGE", "MANAGE_EXTERNAL_STORAGE"),
    );

    companion object {
        fun fromKey(key: String): AppPermission? {
            val upper = key.uppercase()
            return entries.firstOrNull { it.name == upper || it.key.equals(upper, ignoreCase = true) || it.aliases.any { alias -> alias.equals(upper, ignoreCase = true) } }
        }
    }
}
