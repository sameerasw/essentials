package com.sameerasw.essentials.services

import android.content.Context
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.PermissionUtils
import androidx.core.content.edit

class ScreenLockedSecurityTileService : BaseTileService() {

    override fun onTileClick() {
        val prefs = getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("screen_locked_security_enabled", false)

        val intent = android.content.Intent(this, com.sameerasw.essentials.ui.activities.TileAuthActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("feature_pref_key", "screen_locked_security_enabled")
            putExtra("auth_title", "Screen Locked Security")
            putExtra("auth_subtitle", if (isEnabled) "Authenticate to disable screen locked security" else "Authenticate to enable screen locked security")
        }

        if (android.os.Build.VERSION.SDK_INT >= 34) {
            val pendingIntent = android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            startActivityAndCollapse(intent)
        }
    }

    override fun getTileLabel(): String = "Locked Security"

    override fun getTileSubtitle(): String {
        return if (qsTile.state == Tile.STATE_ACTIVE) "Enabled" else "Disabled"
    }

    override fun hasFeaturePermission(): Boolean {
        // Accessibility, Device Admin and Write Secure Settings are required
        return PermissionUtils.isAccessibilityServiceEnabled(this) &&
               PermissionUtils.isDeviceAdminActive(this) &&
               PermissionUtils.canWriteSecureSettings(this)
    }

    override fun getTileIcon(): Icon = Icon.createWithResource(this, R.drawable.rounded_security_24)

    override fun getTileState(): Int {
        val enabled = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
            .getBoolean("screen_locked_security_enabled", false)
        return if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }
}
