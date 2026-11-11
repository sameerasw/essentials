/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: BatteryNotificationTileService.kt
 * Description: Background service component for BatteryNotificationTileService.kt.
 */

package com.sameerasw.essentials.services.tiles

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.services.BatteryNotificationService

@RequiresApi(Build.VERSION_CODES.N)
class BatteryNotificationTileService : BaseTileService() {
    private val settingsRepository by lazy { SettingsRepository(this) }

    override fun onTileClick() {
        val newState = !settingsRepository.isBatteryNotificationEnabled()
        settingsRepository.setBatteryNotificationEnabled(newState)

        val intent = Intent(this, BatteryNotificationService::class.java)
        if (newState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            stopService(intent)
        }
    }

    override fun getTileLabel(): String = getString(R.string.tile_battery_info)

    override fun getTileSubtitle(): String = getString(if (settingsRepository.isBatteryNotificationEnabled()) R.string.on else R.string.off)

    override fun hasFeaturePermission(): Boolean = true

    override fun getTileState(): Int = if (settingsRepository.isBatteryNotificationEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

    override fun getTileIcon(): Icon = Icon.createWithResource(this, R.drawable.rounded_battery_android_frame_6_24)
}
