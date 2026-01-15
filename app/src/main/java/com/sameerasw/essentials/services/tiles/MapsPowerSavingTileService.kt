package com.sameerasw.essentials.services.tiles

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.ShellUtils

@RequiresApi(Build.VERSION_CODES.N)
class MapsPowerSavingTileService : BaseTileService() {

    private val settingsRepository by lazy { SettingsRepository(this) }

    override fun onClick() {
        if (!hasFeaturePermission()) {
            val intent = Intent(this, FeatureSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("feature", "Maps power saving mode")
            }
            startActivityAndCollapse(intent)
            return
        }
        super.onClick()
    }

    override fun getTileLabel(): String = getString(R.string.tile_maps_power_saving)

    override fun getTileSubtitle(): String {
        return if (isMapsPowerSavingEnabled()) getString(R.string.tile_active) else getString(R.string.tile_inactive)
    }

    override fun hasFeaturePermission(): Boolean {
        val hasShell = ShellUtils.hasPermission(this)
        val hasNotif = PermissionUtils.hasNotificationListenerPermission(this)
        return hasShell && hasNotif
    }

    override fun getTileIcon(): Icon? {
        return Icon.createWithResource(this, R.drawable.rounded_navigation_24)
    }

    override fun getTileState(): Int {
        return if (isMapsPowerSavingEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        val newState = !isMapsPowerSavingEnabled()
        settingsRepository.putBoolean(SettingsRepository.KEY_MAPS_POWER_SAVING_ENABLED, newState)
        // MapsState.isEnabled is also updated in MainViewModel when preference changes
    }

    private fun isMapsPowerSavingEnabled(): Boolean {
        return settingsRepository.getBoolean(SettingsRepository.KEY_MAPS_POWER_SAVING_ENABLED, false)
    }
}
