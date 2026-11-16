package com.sameerasw.essentials.services.tiles

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository

class AccessibilityTileService : BaseTileService() {
    override val isSensitiveTile: Boolean = true

    override fun getTileLabel(): String = getString(R.string.tile_accessibility)

    override fun getTileSubtitle(): String = getString(if (isOn()) R.string.tile_accessibility_on else R.string.tile_accessibility_off)

    override fun hasFeaturePermission(): Boolean =
        checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    override fun getTileIcon(): Icon = Icon.createWithResource(this, R.drawable.rounded_accessibility_new_24)

    override fun getTileState(): Int = if (isOn()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

    override fun onTileClick() {
        val repository = SettingsRepository(this)
        val current = currentServices()
        val saved = repository.getString(SettingsRepository.KEY_ACCESSIBILITY_TILE_SAVED, "").orEmpty()
        if (current.isNotBlank()) {
            repository.putString(SettingsRepository.KEY_ACCESSIBILITY_TILE_SAVED, merge(saved, current))
            Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "")
            Settings.Secure.putInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        } else if (saved.isNotBlank()) {
            Settings.Secure.putString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, saved)
            Settings.Secure.putInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
            repository.putString(SettingsRepository.KEY_ACCESSIBILITY_TILE_SAVED, "")
        }
    }

    private fun isOn() = currentServices().isNotBlank()

    private fun currentServices(): String =
        Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()

    private fun merge(first: String, second: String): String =
        (first.split(':') + second.split(':')).filter { it.isNotBlank() }.distinct().joinToString(":")
}
