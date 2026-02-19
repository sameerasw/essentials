package com.sameerasw.essentials.services.tiles

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.services.BatteryNotificationService

class BatteryNotificationTileService : TileService() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
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
        
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val isEnabled = settingsRepository.isBatteryNotificationEnabled()
        
        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
