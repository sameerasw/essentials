package com.sameerasw.essentials.services.tiles

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.RootUtils
import com.sameerasw.essentials.utils.ShizukuUtils

@RequiresApi(Build.VERSION_CODES.N)
class RestartSystemUiTileService : BaseTileService() {

    override fun onClick() {
        if (!hasFeaturePermission()) {
            val intent = Intent(this, FeatureSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("feature", "Quick settings tiles")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }
        super.onClick()
    }

    override fun getTileLabel(): String = getString(R.string.tile_restart_systemui)

    override fun getTileSubtitle(): String {
        return if (!hasFeaturePermission()) {
            getString(R.string.permission_missing)
        } else {
            getString(R.string.tile_restart_systemui_subtitle_restart)
        }
    }

    override fun hasFeaturePermission(): Boolean {
        val rootOk = RootUtils.isRootAvailable() && RootUtils.isRootPermissionGranted()
        val shizukuOk = ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasPermission()
        return rootOk || shizukuOk
    }

    override fun getTileIcon(): Icon? {
        return Icon.createWithResource(this, R.drawable.reopen_window_24px)
    }

    override fun getTileState(): Int {
        // Tile is kept always in inactive (not disabled) state
        return Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        if (ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasPermission()) {
            ShizukuUtils.runCommand("am crash com.android.systemui")
        } else if (RootUtils.isRootAvailable() && RootUtils.isRootPermissionGranted()) {
            RootUtils.runCommand("am crash com.android.systemui")
        }
    }
}
