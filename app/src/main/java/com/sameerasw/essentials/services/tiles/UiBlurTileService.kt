package com.sameerasw.essentials.services.tiles

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R

class UiBlurTileService : BaseTileService() {

    override fun getTileLabel(): String = "UI Blur"

    override fun getTileSubtitle(): String {
        return if (isBlurEnabled()) "On" else "Off"
    }

    override fun hasFeaturePermission(): Boolean {
        return checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    override fun getTileIcon(): Icon {
        val iconRes = if (isBlurEnabled()) R.drawable.rounded_blur_on_24 else R.drawable.rounded_blur_off_24
        return Icon.createWithResource(this, iconRes)
    }

    override fun getTileState(): Int {
        return if (isBlurEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        val newState = if (isBlurEnabled()) 1 else 0 // 1 = disable blurs, 0 = enable blurs
        Settings.Global.putInt(contentResolver, "disable_window_blurs", newState)
    }

    private fun isBlurEnabled(): Boolean {
        // "disable_window_blurs": 1 means blurs are RES_DISABLED, so 0 means enabled.
        // Default might be 0 (enabled) or 1 (disabled) depending on device, but usually 0.
        // Let's assume 0 is enabled.
        val disableBlurs = Settings.Global.getInt(contentResolver, "disable_window_blurs", 0)
        return disableBlurs == 0
    }
}
