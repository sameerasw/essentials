/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Registries
 * File: QSTileRegistry.kt
 * Description: Canonical registry and metadata for all Quick Settings tiles.
 */

package com.sameerasw.essentials.domain.registry

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.tiles.AdaptiveBrightnessTileService
import com.sameerasw.essentials.services.tiles.AlwaysOnDisplayTileService
import com.sameerasw.essentials.services.tiles.AppFreezingTileService
import com.sameerasw.essentials.services.tiles.AppLockTileService
import com.sameerasw.essentials.services.tiles.BubblesTileService
import com.sameerasw.essentials.services.tiles.CaffeinateTileService
import com.sameerasw.essentials.services.tiles.ChargeQuickTileService
import com.sameerasw.essentials.services.tiles.ColorPickerTileService
import com.sameerasw.essentials.services.tiles.DeveloperOptionsTileService
import com.sameerasw.essentials.services.tiles.DynamicNightLightTileService
import com.sameerasw.essentials.services.tiles.EssentialsOnDisplayTileService
import com.sameerasw.essentials.services.tiles.FlashlightPulseTileService
import com.sameerasw.essentials.services.tiles.FlashlightTileService
import com.sameerasw.essentials.services.tiles.LockdownTileService
import com.sameerasw.essentials.services.tiles.MapsPowerSavingTileService
import com.sameerasw.essentials.services.tiles.MonoAudioTileService
import com.sameerasw.essentials.services.tiles.NfcTileService
import com.sameerasw.essentials.services.tiles.NotificationLightingTileService
import com.sameerasw.essentials.services.tiles.PrivateDnsTileService
import com.sameerasw.essentials.services.tiles.PrivateNotificationsTileService
import com.sameerasw.essentials.services.tiles.RefreshRateTileService
import com.sameerasw.essentials.services.tiles.RestartSystemUiTileService
import com.sameerasw.essentials.services.tiles.ScaleAnimationsTileService
import com.sameerasw.essentials.services.tiles.ScreenLockedSecurityTileService
import com.sameerasw.essentials.services.tiles.SmartPixelsTileService
import com.sameerasw.essentials.services.tiles.SoundModeTileService
import com.sameerasw.essentials.services.tiles.StayAwakeTileService
import com.sameerasw.essentials.services.tiles.TapToWakeTileService
import com.sameerasw.essentials.services.tiles.UiBlurTileService
import com.sameerasw.essentials.services.tiles.UrlShortenerTileService
import com.sameerasw.essentials.services.tiles.UsbDebuggingTileService
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.ShellUtils

data class QSTileInfo(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val serviceClass: Class<*>,
    val permissionKeys: List<String> = emptyList(),
    @StringRes val aboutDescription: Int? = null,
    @StringRes val categoryRes: Int,
    val isSupported: (Context) -> Boolean = { true },
)

object QSTileRegistry {
    fun getAllTiles(context: Context, isUseUsageStats: Boolean = false): List<QSTileInfo> =
        listOf(
            QSTileInfo(
                R.string.tile_ui_blur,
                R.drawable.rounded_blur_on_24,
                UiBlurTileService::class.java,
                listOf("WRITE_SECURE_SETTINGS"),
                R.string.about_desc_ui_blur,
                R.string.cat_visuals,
            ),
            QSTileInfo(
                R.string.tile_bubbles,
                R.drawable.rounded_bubble_24,
                BubblesTileService::class.java,
                listOf("WRITE_SECURE_SETTINGS"),
                R.string.about_desc_bubbles,
                R.string.cat_utils,
            ),
            QSTileInfo(
                R.string.tile_sensitive_content,
                R.drawable.rounded_notifications_off_24,
                PrivateNotificationsTileService::class.java,
                listOf("WRITE_SECURE_SETTINGS"),
                R.string.about_desc_sensitive_content,
                R.string.cat_privacy,
            ),
            QSTileInfo(
                R.string.tile_tap_to_wake,
                R.drawable.rounded_touch_app_24,
                TapToWakeTileService::class.java,
                listOf("WRITE_SECURE_SETTINGS"),
                R.string.about_desc_tap_to_wake,
                R.string.cat_visuals,
            ),
            QSTileInfo(
                R.string.tile_aod,
                R.drawable.rounded_mobile_text_2_24,
                AlwaysOnDisplayTileService::class.java,
                if (ShellUtils.isRootEnabled(context)) {
                    listOf("ROOT")
                } else if (PermissionUtils.canWriteSecureSettings(context)) {
                    listOf("WRITE_SECURE_SETTINGS")
                } else {
                    listOf("SHIZUKU")
                },
                R.string.about_desc_aod,
                R.string.cat_visuals,
            ),
            QSTileInfo(
                R.string.tile_caffeinate,
                R.drawable.rounded_coffee_24,
                CaffeinateTileService::class.java,
                listOf("POST_NOTIFICATIONS"),
                R.string.about_desc_caffeinate,
                R.string.cat_utils,
            ),
            QSTileInfo(
                R.string.tile_sound_mode,
                R.drawable.rounded_volume_up_24,
                SoundModeTileService::class.java,
                listOf("NOTIFICATION_POLICY"),
                R.string.about_desc_sound_mode_tile,
                R.string.cat_utils,
            ),
            QSTileInfo(
                R.string.tile_notification_lighting,
                R.drawable.rounded_blur_linear_24,
                NotificationLightingTileService::class.java,
                listOf("DRAW_OVERLAYS", "ACCESSIBILITY", "NOTIFICATION_LISTENER"),
                R.string.about_desc_notification_lighting,
                R.string.cat_utils,
            ),
            QSTileInfo(
                R.string.tile_dynamic_night_light,
                R.drawable.rounded_nightlight_24,
                DynamicNightLightTileService::class.java,
                listOf("ACCESSIBILITY", "WRITE_SECURE_SETTINGS"),
                R.string.about_desc_dynamic_night_light,
                R.string.cat_visuals,
            ),
            QSTileInfo(
                R.string.tile_locked_security,
                R.drawable.rounded_security_24,
                ScreenLockedSecurityTileService::class.java,
                if (ShellUtils.isRootEnabled(context)) listOf("ROOT") else listOf("SHIZUKU"),
                R.string.about_desc_screen_locked_security,
                R.string.cat_privacy,
            ),
            QSTileInfo(
                R.string.tile_app_lock,
                R.drawable.rounded_shield_lock_24,
                AppLockTileService::class.java,
                if (isUseUsageStats) listOf("USAGE_STATS") else listOf("ACCESSIBILITY"),
                R.string.about_desc_app_lock,
                R.string.cat_privacy,
            ),
            QSTileInfo(
                R.string.tile_mono_audio,
                R.drawable.rounded_headphones_24,
                MonoAudioTileService::class.java,
                if (ShellUtils.isRootEnabled(context)) {
                    listOf("ROOT")
                } else if (PermissionUtils.canWriteSecureSettings(context)) {
                    listOf("WRITE_SECURE_SETTINGS")
                } else {
                    listOf("SHIZUKU")
                },
                R.string.about_desc_mono_audio,
                R.string.cat_accessibility,
            ),
            QSTileInfo(
                R.string.tile_flashlight,
                R.drawable.rounded_flashlight_on_24,
                FlashlightTileService::class.java,
                emptyList(),
                R.string.about_desc_flashlight_tile,
                R.string.cat_utils,
            ),
            QSTileInfo(
                R.string.tile_app_freezing,
                R.drawable.rounded_app_badging_24,
                AppFreezingTileService::class.java,
                if (ShellUtils.isRootEnabled(context)) {
                    listOf(
                        "ROOT",
                        "USAGE_STATS",
                        "NOTIFICATION_LISTENER",
                    )
                } else {
                    listOf("SHIZUKU", "USAGE_STATS", "NOTIFICATION_LISTENER")
                },
                R.string.about_desc_freeze,
                R.string.cat_utils,
            ),
            QSTileInfo(
                R.string.tile_flashlight_pulse,
                R.drawable.outline_backlight_high_24,
                FlashlightPulseTileService::class.java,
                listOf("NOTIFICATION_LISTENER"),
                R.string.about_desc_flashlight_pulse,
                R.string.cat_utils,
            ),
            QSTileInfo(
                R.string.tile_stay_awake,
                R.drawable.rounded_av_timer_24,
                StayAwakeTileService::class.java,
                listOf("WRITE_SECURE_SETTINGS"),
                R.string.about_desc_stay_awake,
                R.string.cat_visuals,
            ),
            QSTileInfo(
                R.string.nfc_tile_label,
                R.drawable.rounded_nfc_24,
                NfcTileService::class.java,
                if (ShellUtils.isRootEnabled(context)) {
                    listOf("ROOT")
                } else if (PermissionUtils.canWriteSecureSettings(context)) {
                    listOf("WRITE_SECURE_SETTINGS")
                } else {
                    listOf("SHIZUKU")
                },
                R.string.about_desc_nfc,
                R.string.cat_connectivity,
            ),
            QSTileInfo(
                R.string.tile_adaptive_brightness,
                R.drawable.rounded_brightness_auto_24,
                AdaptiveBrightnessTileService::class.java,
                listOf("WRITE_SETTINGS"),
                R.string.about_desc_adaptive_brightness,
                R.string.cat_visuals,
            ),
            QSTileInfo(
                R.string.tile_scale_animations,
                R.drawable.rounded_front_hand_24,
                ScaleAnimationsTileService::class.java,
                listOf("WRITE_SECURE_SETTINGS"),
                R.string.about_desc_scale_animations_tile,
                R.string.cat_visuals,
            ),
            QSTileInfo(
                R.string.tile_restart_systemui,
                R.drawable.reopen_window_24px,
                RestartSystemUiTileService::class.java,
                if (ShellUtils.isRootEnabled(context)) listOf("ROOT") else listOf("SHIZUKU"),
                R.string.about_desc_restart_systemui_tile,
                R.string.cat_utils,
            ),
            QSTileInfo(
                R.string.tile_refresh_rate,
                R.drawable.rounded_shutter_speed_24,
                RefreshRateTileService::class.java,
                listOf("SHIZUKU"),
                R.string.about_desc_refresh_rate_tile,
                R.string.cat_visuals,
                isSupported = { _ -> DeviceUtils.isGoogleDevice() },
            ),
            QSTileInfo(
                R.string.feat_maps_power_saving_title,
                R.drawable.rounded_navigation_24,
                MapsPowerSavingTileService::class.java,
                if (ShellUtils.isRootEnabled(context)) {
                    listOf(
                        "ROOT",
                        "NOTIFICATION_LISTENER",
                    )
                } else {
                    listOf("SHIZUKU", "NOTIFICATION_LISTENER")
                },
                R.string.about_desc_maps_power_saving,
                R.string.cat_utils,
            ),
            QSTileInfo(
                R.string.tile_private_dns,
                R.drawable.rounded_dns_24,
                PrivateDnsTileService::class.java,
                listOf("WRITE_SECURE_SETTINGS"),
                R.string.about_desc_private_dns,
                R.string.cat_connectivity,
            ),
            QSTileInfo(
                R.string.tile_usb_debugging,
                R.drawable.rounded_adb_24,
                UsbDebuggingTileService::class.java,
                listOf("WRITE_SECURE_SETTINGS"),
                R.string.about_desc_usb_debugging,
                R.string.cat_utils,
            ),
            QSTileInfo(
                R.string.tile_color_picker,
                R.drawable.rounded_colorize_24,
                ColorPickerTileService::class.java,
                emptyList(),
                R.string.about_desc_color_picker,
                R.string.cat_visuals,
            ),
            QSTileInfo(
                R.string.tile_developer_options,
                R.drawable.rounded_mobile_code_24,
                DeveloperOptionsTileService::class.java,
                listOf("WRITE_SECURE_SETTINGS"),
                R.string.about_desc_developer_options,
                R.string.cat_utils,
            ),
            QSTileInfo(
                R.string.tile_charge_optimization,
                R.drawable.rounded_battery_android_frame_shield_24,
                ChargeQuickTileService::class.java,
                if (ShellUtils.isRootEnabled(context)) listOf("ROOT") else listOf("SHIZUKU"),
                R.string.about_desc_charge_optimization,
                R.string.cat_utils,
                isSupported = { _ -> DeviceUtils.isGoogleDevice() },
            ),
            QSTileInfo(
                R.string.tile_lock,
                R.drawable.rounded_lock_24,
                LockdownTileService::class.java,
                listOf("ACCESSIBILITY", "DEVICE_ADMIN"),
                R.string.tile_lockdown_mode_about_desc,
                R.string.cat_privacy,
            ),
            QSTileInfo(
                R.string.feat_smart_pixels_title,
                R.drawable.rounded_grain_24,
                SmartPixelsTileService::class.java,
                listOf("ACCESSIBILITY"),
                R.string.about_desc_smart_pixels,
                R.string.cat_visuals,
            ),
            QSTileInfo(
                R.string.feat_essentials_on_display_title,
                R.drawable.rounded_live_tv_24,
                EssentialsOnDisplayTileService::class.java,
                listOf("ACCESSIBILITY", "NOTIFICATION_LISTENER"),
                R.string.feat_essentials_on_display_desc,
                R.string.cat_visuals,
            ),
            QSTileInfo(
                R.string.tile_url_shortener,
                R.drawable.rounded_link_24,
                UrlShortenerTileService::class.java,
                emptyList(),
                R.string.tile_url_shortener_subtitle,
                R.string.cat_utils,
            ),
        )

    fun searchTiles(
        context: Context,
        query: String,
        includeUnsupported: Boolean = false,
        isUseUsageStats: Boolean = false,
    ): List<QSTileInfo> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        val tiles = getAllTiles(context, isUseUsageStats).filter { tile ->
            includeUnsupported || tile.isSupported(context)
        }

        return tiles.filter { tile ->
            val title = context.getString(tile.titleRes).lowercase()
            val category = context.getString(tile.categoryRes).lowercase()
            val desc = tile.aboutDescription?.let { context.getString(it).lowercase() } ?: ""
            title.contains(q) || category.contains(q) || desc.contains(q)
        }
    }
}
