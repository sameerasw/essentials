/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: ActionRegistry.kt
 * Description: Central registry providing categorised action lists for both DIY automation and Button Remap.
 */

package com.sameerasw.essentials.domain.diy

import android.os.Build
import com.sameerasw.essentials.R

object ActionRegistry {
    data class ActionCategory(
        val titleRes: Int,
        val actions: List<Action>,
    )

    /**
     * Returns all action categories available for the given context.
     *
     * @param sdkInt Current SDK level, used to gate Android-version-specific actions.
     * @param screenOnOnly When true, actions that only make sense with the screen on (e.g. screenshot) are included;
     *                     when false they are excluded. Used by Button Remap screen-off tab.
     */
    fun getCategories(
        sdkInt: Int = Build.VERSION.SDK_INT,
        screenOnOnly: Boolean? = null,
    ): List<ActionCategory> {
        val connectivityActions =
            listOf(
                Action.TurnOnWifi,
                Action.TurnOffWifi,
                Action.TurnOnCellularData,
                Action.TurnOffCellularData,
                Action.TurnOnHotspot,
                Action.TurnOffHotspot,
                Action.ToggleHotspot,
            )

        val displayActions =
            buildList {
                add(Action.TurnOnAutoBrightness)
                add(Action.TurnOffAutoBrightness)
                add(Action.DimWallpaper())
                add(Action.ScreenOff())
                if (sdkInt >= 35) add(Action.DeviceEffects())
            }

        val appsActions =
            listOf(
                Action.OpenApp(),
                Action.AIAssistant,
                Action.FreezeApps(),
                Action.UnfreezeApps(),
                Action.FreezeTag(),
                Action.PinApp,
                Action.Keyboard(),
            )

        val systemActions =
            buildList {
                add(Action.TurnOnFlashlight)
                add(Action.TurnOffFlashlight)
                add(Action.ToggleFlashlight)
                add(Action.TurnOnLowPower)
                add(Action.TurnOffLowPower)
                add(Action.CustomSettings())
                add(Action.CircleToSearch)
                // TakeScreenshot only available on screen-on context (null means no filter = include always)
                if (screenOnOnly == null || screenOnOnly == true) {
                    add(Action.TakeScreenshot)
                }
            }

        val soundMediaActions =
            listOf(
                Action.SoundMode(),
                Action.CycleSoundModes,
                Action.ToggleMute,
                Action.ToggleVibrate,
                Action.HapticVibration,
                Action.ToggleMediaVolume,
                Action.SetVolume(),
                Action.MediaPlayPause,
                Action.MediaNext,
                Action.MediaPrevious,
                Action.LikeCurrentSong,
                Action.OpenNowPlayingApp,
            )

        val essentialsActions =
            listOf(
                Action.SometimesEssentials(),
            )

        return listOf(
            ActionCategory(R.string.diy_category_connectivity, connectivityActions),
            ActionCategory(R.string.diy_category_display, displayActions),
            ActionCategory(R.string.diy_category_apps, appsActions),
            ActionCategory(R.string.diy_category_system, systemActions),
            ActionCategory(R.string.diy_category_sound_media, soundMediaActions),
            ActionCategory(R.string.diy_category_essentials, essentialsActions),
        )
    }
}
