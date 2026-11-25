/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeFeature.kt
 * Description: Switches a mode can force on or off while it is active.
 */

package com.sameerasw.essentials.domain.modes

import android.content.Context
import android.os.PowerManager
import androidx.annotation.StringRes
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.controller.CaffeinateController
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor

enum class ModeFeature(
    @StringRes val title: Int,
    val onAction: Action,
    val offAction: Action,
) {
    ISLAND(R.string.island_title, Action.TurnOnIsland, Action.TurnOffIsland),
    STATUS_GLANCE(R.string.feat_status_glance_title, Action.TurnOnStatusGlance, Action.TurnOffStatusGlance),
    DUO(R.string.duo_title, Action.TurnOnDuo, Action.TurnOffDuo),
    CAFFEINATE(R.string.feat_caffeinate_title, Action.TurnOnCaffeinate, Action.TurnOffCaffeinate),
    WIFI(R.string.island_network_wifi, Action.TurnOnWifi, Action.TurnOffWifi),
    MOBILE_DATA(R.string.mode_feature_mobile_data, Action.TurnOnCellularData, Action.TurnOffCellularData),
    HOTSPOT(R.string.icon_hotspot, Action.TurnOnHotspot, Action.TurnOffHotspot),
    LOCATION(R.string.perm_location_title, Action.TurnOnLocation, Action.TurnOffLocation),
    AUTO_BRIGHTNESS(R.string.tile_adaptive_brightness, Action.TurnOnAutoBrightness, Action.TurnOffAutoBrightness),
    BATTERY_SAVER(R.string.island_battery_state_saver, Action.TurnOnLowPower, Action.TurnOffLowPower),
    SHOW_TAPS(R.string.mode_feature_show_taps, Action.TurnOnShowTaps, Action.TurnOffShowTaps),
    ;

    val icon: Int get() = onAction.icon

    fun action(on: Boolean): Action = if (on) onAction else offAction

    fun read(context: Context): Boolean? =
        runCatching {
            when (this) {
                ISLAND -> SettingsRepository(context).isIslandEnabled()
                STATUS_GLANCE -> SettingsRepository(context).isStatusGlanceEnabled()
                DUO -> SettingsRepository(context).isDuoEnabled()
                CAFFEINATE -> CaffeinateController.run {
                    check(context)
                    isActive.value
                }
                WIFI -> CombinedActionExecutor.isWifiEnabled(context)
                MOBILE_DATA -> CombinedActionExecutor.isCellularDataEnabled(context)
                HOTSPOT -> CombinedActionExecutor.isHotspotEnabled(context)
                LOCATION -> CombinedActionExecutor.isLocationEnabled(context)
                AUTO_BRIGHTNESS -> CombinedActionExecutor.isAutoBrightnessEnabled(context)
                BATTERY_SAVER -> context.getSystemService(PowerManager::class.java)?.isPowerSaveMode
                SHOW_TAPS -> CombinedActionExecutor.isShowTaps(context)
            }
        }.getOrNull()

    companion object {
        fun fromKey(key: String): ModeFeature? = entries.find { it.name == key }
    }
}
