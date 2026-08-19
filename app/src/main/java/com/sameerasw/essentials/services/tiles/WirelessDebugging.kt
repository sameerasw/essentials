package com.sameerasw.essentials.services.tiles

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.PermissionUtils

class WirelessDebugging : BaseTileService() {
    override fun onClick() {
        if (!hasFeaturePermission()) {
            val intent = Intent(this, FeatureSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("feature", "Quick settings tiles")
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(intent)
                return
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
            return
        }
        super.onClick()
    }

    override fun onTileClick() = setWifiDebuggingEnabled(!isWifiDebuggingEnabled())

    override fun getTileLabel() = getString(R.string.tile_wifi_adb_title_short)

    override fun getTileSubtitle(): String {
        return if (isWifiDebuggingEnabled()) getString(R.string.on) else getString(R.string.off)
    }

    override fun hasFeaturePermission() = PermissionUtils.canWriteSecureSettings(this)

    override fun getTileIcon(): Icon {
        return Icon.createWithResource(this, R.drawable.rounded_android_24)
    }

    override fun getTileState(): Int {
        return if (isWifiDebuggingEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    private fun isWifiDebuggingEnabled(): Boolean {
        return try {
            Settings.Global.getInt(contentResolver, "adb_wifi_enabled", 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    private fun setWifiDebuggingEnabled(enabled: Boolean) {
        try {
            Settings.Global.putInt(contentResolver, "adb_wifi_enabled", if (enabled) 1 else 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}