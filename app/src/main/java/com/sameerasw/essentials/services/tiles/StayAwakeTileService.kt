package com.sameerasw.essentials.services.tiles

import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R


class StayAwakeTileService : BaseTileService() {

    override fun onTileClick() {
        val isActive = qsTile.state == Tile.STATE_ACTIVE
        val newValue = if (isActive) 0 else 3 // 3 = Battery + USB + Wireless (Stay awake on all)
        
        try {
            Settings.Global.putInt(contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, newValue)
        } catch (e: SecurityException) {
            // Should be handled by BaseTileService permission check usually, but just in case
        }
        updateTile()
    }

    override fun getTileLabel(): String = getString(R.string.tile_stay_awake)

    override fun getTileSubtitle(): String {
        return if (qsTile.state == Tile.STATE_ACTIVE) {
            getString(R.string.tile_active)
        } else {
            getString(R.string.tile_inactive)
        }
    }

    override fun hasFeaturePermission(): Boolean {
        return com.sameerasw.essentials.utils.PermissionUtils.canWriteSecureSettings(this)
    }

    override fun getTileState(): Int {
        val stayAwakeValue = try {
            Settings.Global.getInt(contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        return if (stayAwakeValue > 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun getTileIcon(): Icon? {
        return Icon.createWithResource(this, R.drawable.rounded_av_timer_24)
    }
}
