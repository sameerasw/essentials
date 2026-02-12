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
class AdaptiveBrightnessTileService : BaseTileService() {

    override fun onClick() {
        if (!hasFeaturePermission()) {
            val intent = Intent(this, FeatureSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("feature", "Quick settings tiles")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }
        super.onClick()
    }

    override fun getTileLabel(): String = getString(R.string.tile_adaptive_brightness)

    override fun getTileSubtitle(): String {
        return if (isAdaptiveBrightnessEnabled()) getString(R.string.tile_active) else getString(R.string.tile_inactive)
    }

    override fun hasFeaturePermission(): Boolean {
        return PermissionUtils.canWriteSystemSettings(this)
    }

    override fun getTileIcon(): Icon? {
        return if (isAdaptiveBrightnessEnabled()) {
            Icon.createWithResource(this, R.drawable.rounded_brightness_auto_24)
        } else {
            Icon.createWithResource(this, R.drawable.rounded_brightness_medium_24)
        }
    }

    override fun getTileState(): Int {
        return if (isAdaptiveBrightnessEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        val newState = if (isAdaptiveBrightnessEnabled()) 0 else 1
        try {
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, newState)
        } catch (e: SecurityException) {
            // Permission check in BaseTileService should handle this
        }
    }

    private fun isAdaptiveBrightnessEnabled(): Boolean {
        return try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) == 1
        } catch (e: Exception) {
            false
        }
    }
}
