package com.sameerasw.essentials.services.tiles

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.PermissionUtils

@RequiresApi(Build.VERSION_CODES.N)
class DeveloperOptionsTileService : BaseTileService() {

    override fun onClick() {
        if (!hasFeaturePermission()) {
            val intent = Intent(this, FeatureSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("feature", "Quick settings tiles")
            }
            startActivityAndCollapse(intent)
            return
        }
        super.onClick()
    }

    override fun getTileLabel(): String = getString(R.string.tile_developer_options)

    override fun getTileSubtitle(): String {
        return if (isDevOptionsEnabled()) getString(R.string.tile_active) else getString(R.string.tile_inactive)
    }

    override fun hasFeaturePermission(): Boolean {
        return PermissionUtils.canWriteSecureSettings(this)
    }

    override fun getTileIcon(): Icon {
        return Icon.createWithResource(this, R.drawable.rounded_mobile_code_24)
    }

    override fun getTileState(): Int {
        return if (isDevOptionsEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        val newState = if (isDevOptionsEnabled()) 0 else 1
        try {
            Settings.Global.putInt(
                contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                newState
            )
        } catch (e: Exception) {
            // Permission check in BaseTileService handles this
        }
    }

    private fun isDevOptionsEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
}
