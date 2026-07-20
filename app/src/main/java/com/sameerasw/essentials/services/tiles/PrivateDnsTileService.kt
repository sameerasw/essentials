package com.sameerasw.essentials.services.tiles

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.PermissionUtils

@RequiresApi(Build.VERSION_CODES.N)
class PrivateDnsTileService : BaseTileService() {

    companion object {
        private const val PRIVATE_DNS_MODE = "private_dns_mode"
        private const val PRIVATE_DNS_SPECIFIER = "private_dns_specifier"

        private const val MODE_OFF = "off"
        private const val MODE_AUTO = "opportunistic"
        private const val MODE_HOSTNAME = "hostname"
    }

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

    override fun getTileLabel(): String = getString(R.string.tile_private_dns)

    override fun getTileSubtitle(): String {
        val mode = getPrivateDnsMode()
        return when (mode) {
            MODE_AUTO -> getString(R.string.tile_private_dns_auto)
            MODE_HOSTNAME -> {
                val hostname = getPrivateDnsHostname()
                if (!hostname.isNullOrEmpty()) {
                    val settingsRepository =
                        SettingsRepository(this)
                    val preset =
                        settingsRepository.getPrivateDnsPresets().find { it.hostname == hostname }
                    preset?.name ?: hostname
                } else {
                    getString(R.string.feat_qs_tiles_title)
                }
            }

            else -> getString(R.string.tile_private_dns_off)
        }
    }

    override fun hasFeaturePermission(): Boolean {
        return PermissionUtils.canWriteSecureSettings(this)
    }

    override fun getTileIcon(): Icon {
        return when (getPrivateDnsMode()) {
            MODE_AUTO -> Icon.createWithResource(this, R.drawable.router_24px)
            MODE_OFF -> Icon.createWithResource(this, R.drawable.router_off_24px)
            else -> Icon.createWithResource(this, R.drawable.router_24px_filled)
        }
    }

    override fun getTileState(): Int {
        return if (getPrivateDnsMode() != MODE_OFF) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        val settingsRepository = SettingsRepository(this)
        val isCycleMode = settingsRepository.getBoolean("private_dns_is_cycle_mode", false)
        val currentMode = getPrivateDnsMode()

        if (isCycleMode) {
            val presets = settingsRepository.getPrivateDnsPresets()
            if (presets.isEmpty()) {
                // If presets are empty, fallback to toggle custom/off
                val nextMode = if (currentMode == MODE_OFF) MODE_AUTO else MODE_OFF
                try {
                    Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, nextMode)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return
            }

            val currentHost = getPrivateDnsHostname() ?: ""
            if (currentMode == MODE_OFF) {
                // Turn on with the first preset
                val firstPreset = presets.first()
                try {
                    Settings.Global.putString(contentResolver, PRIVATE_DNS_SPECIFIER, firstPreset.hostname)
                    Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, MODE_HOSTNAME)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // Find current preset index
                val currentIndex = presets.indexOfFirst { it.hostname == currentHost }
                if (currentIndex == -1 || currentIndex == presets.lastIndex) {
                    // Turn off
                    try {
                        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, MODE_OFF)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // Next preset
                    val nextPreset = presets[currentIndex + 1]
                    try {
                        Settings.Global.putString(contentResolver, PRIVATE_DNS_SPECIFIER, nextPreset.hostname)
                        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, MODE_HOSTNAME)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            val cycleAuto = settingsRepository.getBoolean("private_dns_cycle_auto", true)
            val nextMode = if (cycleAuto) {
                when (currentMode) {
                    MODE_OFF -> MODE_AUTO
                    MODE_AUTO -> {
                        if (getPrivateDnsHostname().isNullOrEmpty()) MODE_OFF else MODE_HOSTNAME
                    }
                    MODE_HOSTNAME -> MODE_OFF
                    else -> MODE_OFF
                }
            } else {
                when (currentMode) {
                    MODE_OFF -> {
                        if (getPrivateDnsHostname().isNullOrEmpty()) MODE_OFF else MODE_HOSTNAME
                    }
                    MODE_AUTO -> {
                        if (getPrivateDnsHostname().isNullOrEmpty()) MODE_OFF else MODE_HOSTNAME
                    }
                    MODE_HOSTNAME -> MODE_OFF
                    else -> MODE_OFF
                }
            }

            try {
                Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, nextMode)
            } catch (e: Exception) {
                // Handle error or permission missing
            }
        }
    }

    private fun getPrivateDnsMode(): String {
        return Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE) ?: MODE_OFF
    }

    private fun getPrivateDnsHostname(): String? {
        return Settings.Global.getString(contentResolver, PRIVATE_DNS_SPECIFIER)
    }
}
