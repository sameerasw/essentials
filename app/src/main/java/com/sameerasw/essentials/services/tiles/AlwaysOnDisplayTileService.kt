package com.sameerasw.essentials.services.tiles

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository

@RequiresApi(Build.VERSION_CODES.N)
class AlwaysOnDisplayTileService : BaseTileService() {

    override fun getTileLabel(): String = "Always on Display"

    override fun getTileSubtitle(): String {
        return when {
            isGlanceEnabled() -> "Dynamic"
            isAodEnabled() -> "On"
            else -> "Off"
        }
    }

    override fun hasFeaturePermission(): Boolean {
        return checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    override fun getTileIcon(): Icon? {
        return when {
            isGlanceEnabled() -> Icon.createWithResource(this, R.drawable.outline_mobile_chat_24)
            isAodEnabled() -> Icon.createWithResource(this, R.drawable.rounded_mobile_text_2_24)
            else -> Icon.createWithResource(this, R.drawable.rounded_mobile_off_24)
        }
    }

    override fun getTileState(): Int {
        return if (isAodEnabled() || isGlanceEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        when {
            isGlanceEnabled() -> {
                // Dynamic -> On
                setGlanceEnabled(false)
                setAodEnabled(true)
            }
            isAodEnabled() -> {
                // On -> Off
                setAodEnabled(false)
                setGlanceEnabled(false)
            }
            else -> {
                // Off -> Dynamic
                setGlanceEnabled(true)
                setAodEnabled(false)
            }
        }
    }

    private fun isAodEnabled(): Boolean {
        return Settings.Secure.getInt(contentResolver, "doze_always_on", 0) == 1
    }

    private fun setAodEnabled(enabled: Boolean) {
        Settings.Secure.putInt(contentResolver, "doze_always_on", if (enabled) 1 else 0)
    }

    private fun isGlanceEnabled(): Boolean {
        return getSharedPreferences("essentials_prefs", MODE_PRIVATE)
            .getBoolean(SettingsRepository.KEY_NOTIFICATION_GLANCE_ENABLED, false)
    }

    private fun setGlanceEnabled(enabled: Boolean) {
        getSharedPreferences("essentials_prefs", MODE_PRIVATE).edit().apply {
            putBoolean(SettingsRepository.KEY_NOTIFICATION_GLANCE_ENABLED, enabled)
            apply()
        }
    }
}
