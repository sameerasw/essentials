/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: MonoAudioTileService.kt
 * Description: Background service component for MonoAudioTileService.kt.
 */

package com.sameerasw.essentials.services.tiles

import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R

class MonoAudioTileService : BaseTileService() {
    override fun getTileLabel(): String = getString(R.string.tile_mono_audio)

    override fun getTileSubtitle(): String = getString(if (isMonoAudioEnabled()) R.string.on else R.string.off)

    override fun hasFeaturePermission(): Boolean =
        com.sameerasw.essentials.utils.PermissionUtils
            .canWriteSecureSettings(this) ||
            (
                com.sameerasw.essentials.utils.ShellUtils
                    .isAvailable(this) &&
                    com.sameerasw.essentials.utils.ShellUtils
                        .hasPermission(this)
            )

    override fun getTileIcon(): Icon = Icon.createWithResource(this, R.drawable.rounded_headphones_24)

    override fun getTileState(): Int = if (isMonoAudioEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

    override fun onTileClick() {
        val newState = if (isMonoAudioEnabled()) 0 else 1
        com.sameerasw.essentials.utils.ShellUtils.runCommand(
            this,
            "settings put system master_mono $newState",
            featureName = getTileLabel(),
        )
    }

    private fun isMonoAudioEnabled(): Boolean = Settings.System.getInt(contentResolver, "master_mono", 0) == 1
}
