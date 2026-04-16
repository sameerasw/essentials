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

    override fun getTileLabel(): String = "Battery Info"

    override fun getTileSubtitle(): String = if (settingsRepository.isBatteryNotificationEnabled()) "On" else "Off"

    override fun hasFeaturePermission(): Boolean = true

    override fun getTileState(): Int {
        return if (settingsRepository.isBatteryNotificationEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun getTileIcon(): Icon {
        return Icon.createWithResource(this, R.drawable.rounded_battery_android_frame_6_24)
    }
}
