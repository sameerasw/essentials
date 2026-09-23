/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: QsTileRegistry.kt
 * Description: Background service component for QsTileRegistry.kt.
 */

package com.sameerasw.essentials.services.tiles

import com.sameerasw.essentials.R

object QsTileRegistry {
    data class QsTileEntry(
        val iconRes: Int,
        val serviceClass: Class<*>,
    )

    val ALL_TILES: List<QsTileEntry> =
        listOf(
            QsTileEntry(
                R.drawable.rounded_blur_on_24,
                UiBlurTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_bubble_24,
                BubblesTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_notifications_off_24,
                PrivateNotificationsTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_notifications_unread_24,
                HeadsUpNotificationsTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_touch_app_24,
                TapToWakeTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_mobile_text_2_24,
                AlwaysOnDisplayTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_coffee_24,
                CaffeinateTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_volume_up_24,
                SoundModeTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_blur_linear_24,
                NotificationLightingTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_nightlight_24,
                DynamicNightLightTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_security_24,
                ScreenLockedSecurityTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_shield_lock_24,
                AppLockTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_headphones_24,
                MonoAudioTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_flashlight_on_24,
                FlashlightTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_app_badging_24,
                AppFreezingTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.outline_backlight_high_24,
                FlashlightPulseTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_av_timer_24,
                StayAwakeTileService::class.java,
            ),
            QsTileEntry(R.drawable.rounded_nfc_24, NfcTileService::class.java),
            QsTileEntry(
                R.drawable.rounded_brightness_auto_24,
                AdaptiveBrightnessTileService::class.java,
            ),
            QsTileEntry(R.drawable.rounded_mobile_vibrate_24, HapticsTileService::class.java),
            QsTileEntry(R.drawable.outline_sim_card_24, DataSimTileService::class.java),
            QsTileEntry(
                R.drawable.rounded_front_hand_24,
                ScaleAnimationsTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.reopen_window_24px,
                RestartSystemUiTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_shutter_speed_24,
                RefreshRateTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_navigation_24,
                MapsPowerSavingTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_dns_24,
                PrivateDnsTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_adb_24,
                UsbDebuggingTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_colorize_24,
                ColorPickerTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_mobile_code_24,
                DeveloperOptionsTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_battery_android_frame_shield_24,
                ChargeQuickTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_grain_24,
                SmartPixelsTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_live_tv_24,
                EssentialsOnDisplayTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_lock_24,
                LockdownTileService::class.java,
            ),
            QsTileEntry(
                R.drawable.rounded_link_24,
                UrlShortenerTileService::class.java,
            ),
        )

    private val tilesMapByClassName: Map<String, QsTileEntry> by lazy {
        ALL_TILES.associateBy { it.serviceClass.name }
    }

    fun getTileByClassName(className: String): QsTileEntry? = tilesMapByClassName[className]

    fun isTileActive(
        context: android.content.Context,
        className: String,
    ): Boolean =
        try {
            when (className) {
                DataSimTileService::class.java.name ->
                    DataSimController.lastKnownState?.next != null
                CaffeinateTileService::class.java.name -> {
                    com.sameerasw.essentials.domain.controller.CaffeinateController.isActive.value ||
                            com.sameerasw.essentials.domain.controller.CaffeinateController.isStarting.value
                }

                FlashlightTileService::class.java.name -> {
                    val instance = ScreenOffAccessibilityService.instance
                    if (instance != null) {
                        instance.flashlightHandler.isTorchOn
                    } else {
                        val clazz = Class.forName(className)
                        val tileService =
                            clazz.getDeclaredConstructor().newInstance() as BaseTileService
                        val attachBaseContextMethod =
                            android.content.ContextWrapper::class.java.getDeclaredMethod(
                                "attachBaseContext",
                                android.content.Context::class.java,
                            )
                        attachBaseContextMethod.isAccessible = true
                        attachBaseContextMethod.invoke(tileService, context)
                        val getTileStateMethod =
                            BaseTileService::class.java.getDeclaredMethod("getTileState")
                        getTileStateMethod.isAccessible = true
                        (getTileStateMethod.invoke(tileService) as Int) == android.service.quicksettings.Tile.STATE_ACTIVE
                    }
                }

                else -> {
                    val clazz = Class.forName(className)
                    if (BaseTileService::class.java.isAssignableFrom(clazz)) {
                        val tileService =
                            clazz.getDeclaredConstructor().newInstance() as BaseTileService
                        val attachBaseContextMethod =
                            android.content.ContextWrapper::class.java.getDeclaredMethod(
                                "attachBaseContext",
                                android.content.Context::class.java,
                            )
                        attachBaseContextMethod.isAccessible = true
                        attachBaseContextMethod.invoke(tileService, context)

                        val getTileStateMethod =
                            BaseTileService::class.java.getDeclaredMethod("getTileState")
                        getTileStateMethod.isAccessible = true
                        val state = getTileStateMethod.invoke(tileService) as Int
                        state == android.service.quicksettings.Tile.STATE_ACTIVE
                    } else {
                        false
                    }
                }
            }
        } catch (e: Exception) {
            false
        }

    fun getTileLabel(
        context: android.content.Context,
        className: String,
    ): String =
        try {
            val clazz = Class.forName(className)
            if (BaseTileService::class.java.isAssignableFrom(clazz)) {
                val tileService =
                    clazz.getDeclaredConstructor().newInstance() as BaseTileService
                val attachBaseContextMethod =
                    android.content.ContextWrapper::class.java.getDeclaredMethod(
                        "attachBaseContext",
                        android.content.Context::class.java,
                    )
                attachBaseContextMethod.isAccessible = true
                attachBaseContextMethod.invoke(tileService, context)

                val getTileLabelMethod =
                    BaseTileService::class.java.getDeclaredMethod("getTileLabel")
                getTileLabelMethod.isAccessible = true
                (getTileLabelMethod.invoke(tileService) as? String) ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }

    fun getTileSubtitle(
        context: android.content.Context,
        className: String,
    ): String =
        try {
            when (className) {
                DataSimTileService::class.java.name -> {
                    DataSimController.lastKnownState?.selected?.let { it.name ?: context.getString(R.string.tile_data_sim_slot, it.slot + 1) }
                        ?: context.getString(R.string.tile_data_sim_loading)
                }
                CaffeinateTileService::class.java.name -> {
                    if (com.sameerasw.essentials.domain.controller.CaffeinateController.isActive.value) {
                        "Active"
                    } else if (com.sameerasw.essentials.domain.controller.CaffeinateController.isStarting.value) {
                        "Starting"
                    } else {
                        "Off"
                    }
                }

                FlashlightTileService::class.java.name -> {
                    val instance = ScreenOffAccessibilityService.instance
                    val isTorchOn = instance?.flashlightHandler?.isTorchOn ?: false
                    if (isTorchOn) "On" else "Off"
                }

                else -> {
                    val clazz = Class.forName(className)
                    if (BaseTileService::class.java.isAssignableFrom(clazz)) {
                        val tileService =
                            clazz.getDeclaredConstructor().newInstance() as BaseTileService
                        val attachBaseContextMethod =
                            android.content.ContextWrapper::class.java.getDeclaredMethod(
                                "attachBaseContext",
                                android.content.Context::class.java,
                            )
                        attachBaseContextMethod.isAccessible = true
                        attachBaseContextMethod.invoke(tileService, context)

                        val getTileSubtitleMethod =
                            BaseTileService::class.java.getDeclaredMethod("getTileSubtitle")
                        getTileSubtitleMethod.isAccessible = true
                        (getTileSubtitleMethod.invoke(tileService) as? String) ?: ""
                    } else {
                        ""
                    }
                }
            }
        } catch (e: Exception) {
            ""
        }

    fun getTileIcon(
        context: android.content.Context,
        className: String,
        fallbackIconRes: Int,
    ): Int =
        try {
            when (className) {
                FlashlightTileService::class.java.name -> {
                    val instance = ScreenOffAccessibilityService.instance
                    val isTorchOn =
                        if (instance != null) {
                            instance.flashlightHandler.isTorchOn
                        } else {
                            false
                        }
                    if (isTorchOn) R.drawable.round_flashlight_on_24 else R.drawable.rounded_flashlight_on_24
                }

                else -> {
                    val clazz = Class.forName(className)
                    if (BaseTileService::class.java.isAssignableFrom(clazz)) {
                        val tileService =
                            clazz.getDeclaredConstructor().newInstance() as BaseTileService
                        val attachBaseContextMethod =
                            android.content.ContextWrapper::class.java.getDeclaredMethod(
                                "attachBaseContext",
                                android.content.Context::class.java,
                            )
                        attachBaseContextMethod.isAccessible = true
                        attachBaseContextMethod.invoke(tileService, context)

                        val getTileIconMethod =
                            BaseTileService::class.java.getDeclaredMethod("getTileIcon")
                        getTileIconMethod.isAccessible = true
                        val iconObj =
                            getTileIconMethod.invoke(tileService) as? android.graphics.drawable.Icon
                        if (iconObj != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            val resId = iconObj.resId
                            if (resId != 0) resId else fallbackIconRes
                        } else {
                            fallbackIconRes
                        }
                    } else {
                        fallbackIconRes
                    }
                }
            }
        } catch (e: Exception) {
            fallbackIconRes
        }
}
