/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: EssentialsOnDisplayTileService.kt
 * Description: Quick Settings Tile Service to toggle and cycle through Essentials on Display modes (Off -> On -> Docked).
 */

package com.sameerasw.essentials.services.tiles

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.PermissionUtils

@RequiresApi(Build.VERSION_CODES.N)
class EssentialsOnDisplayTileService : BaseTileService() {

    override fun getTileLabel(): String = getString(R.string.tile_essentials_on_display)

    override fun getTileSubtitle(): String {
        val enabled = isGlanceEnabled()
        val docked = isDockedModeEnabled()
        return when {
            enabled && docked -> "Docked"
            enabled -> getString(R.string.tile_active)
            else -> getString(R.string.tile_inactive)
        }
    }

    override fun hasFeaturePermission(): Boolean {
        return PermissionUtils.isAccessibilityServiceEnabled(this) &&
                PermissionUtils.hasNotificationListenerPermission(this)
    }

    override fun getTileIcon(): Icon? {
        val enabled = isGlanceEnabled()
        val docked = isDockedModeEnabled()
        return when {
            enabled && docked -> Icon.createWithResource(this, R.drawable.rounded_live_tv_24)
            enabled -> Icon.createWithResource(this, R.drawable.rounded_tv_next_24)
            else -> Icon.createWithResource(this, R.drawable.rounded_tv_off_24)
        }
    }

    override fun getTileState(): Int {
        return if (isGlanceEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        val enabled = isGlanceEnabled()
        val docked = isDockedModeEnabled()

        when {
            !enabled -> {
                // Off -> On
                setGlanceEnabled(true)
                setDockedModeEnabled(false)
            }
            enabled && !docked -> {
                // On -> Docked (Both On and Docked enabled)
                setGlanceEnabled(true)
                setDockedModeEnabled(true)
            }
            else -> {
                // Docked -> Off
                setGlanceEnabled(false)
                setDockedModeEnabled(false)
            }
        }
    }

    private fun isGlanceEnabled(): Boolean {
        return getSharedPreferences(SettingsRepository.PREFS_NAME, MODE_PRIVATE)
            .getBoolean(SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_ENABLED, false)
    }

    private fun setGlanceEnabled(enabled: Boolean) {
        getSharedPreferences(SettingsRepository.PREFS_NAME, MODE_PRIVATE).edit().apply {
            putBoolean(SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_ENABLED, enabled)
            apply()
        }
    }

    private fun isDockedModeEnabled(): Boolean {
        return getSharedPreferences(SettingsRepository.PREFS_NAME, MODE_PRIVATE)
            .getBoolean(SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_DOCKED_MODE, false)
    }

    private fun setDockedModeEnabled(enabled: Boolean) {
        getSharedPreferences(SettingsRepository.PREFS_NAME, MODE_PRIVATE).edit().apply {
            putBoolean(SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_DOCKED_MODE, enabled)
            apply()
        }
    }
}
