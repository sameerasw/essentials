/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Radial Menus
 * File: QuickBallIconPack.kt
 * Description: Icon packs and action icon resolvers for Quick Ball system shortcuts.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action

@Keep
enum class QuickBallIconPack(@field:StringRes val titleRes: Int) {
    MATERIAL(R.string.quick_ball_icon_pack_material),
    CLASSIC(R.string.quick_ball_icon_pack_classic);

    companion object {
        fun fromName(name: String?): QuickBallIconPack {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MATERIAL
        }
    }
}

object QuickBallIconResolver {
    fun resolveIcon(action: Action, iconPack: QuickBallIconPack): Int {
        if (iconPack == QuickBallIconPack.MATERIAL) {
            return action.icon
        }

        return when (action) {
            is Action.Back -> R.drawable.qb_classic_back
            is Action.Home -> R.drawable.qb_classic_home
            is Action.RecentApps -> R.drawable.qb_classic_recent
            is Action.LockScreen -> R.drawable.qb_classic_lock
            is Action.TakeScreenshot -> R.drawable.qb_classic_screenshot
            is Action.OpenNotifications -> R.drawable.qb_classic_notification
            is Action.OpenQuickSettings -> R.drawable.qb_classic_quick_settings
            is Action.ToggleFlashlight -> R.drawable.qb_classic_torch
            is Action.PowerDialog -> R.drawable.qb_classic_power_menu
            is Action.HapticVibration -> R.drawable.qb_classic_vibrate
            is Action.ToggleVibrate -> R.drawable.qb_classic_vibrate
            is Action.ToggleDoNotDisturb -> R.drawable.qb_classic_dnd
            is Action.ToggleMediaVolume -> R.drawable.qb_classic_volume_panel
            is Action.CycleSoundModes -> R.drawable.qb_classic_volume_up
            is Action.ToggleMute -> R.drawable.qb_classic_silent
            is Action.MediaPlayPause -> R.drawable.qb_classic_play_pause
            is Action.MediaNext -> R.drawable.qb_classic_next_track
            is Action.MediaPrevious -> R.drawable.qb_classic_previous_track
            else -> action.icon
        }
    }
}
