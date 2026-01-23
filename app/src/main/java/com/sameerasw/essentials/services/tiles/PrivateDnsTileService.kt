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
        return when (getPrivateDnsMode()) {
            MODE_AUTO -> getString(R.string.tile_private_dns_auto)
            MODE_HOSTNAME -> getPrivateDnsHostname() ?: getString(R.string.feat_qs_tiles_title)
            else -> getString(R.string.tile_private_dns_off)
        }
    }

    override fun hasFeaturePermission(): Boolean {
        return PermissionUtils.canWriteSecureSettings(this)
    }

    override fun getTileIcon(): Icon? {
        return Icon.createWithResource(this, R.drawable.rounded_dns_24)
    }

    override fun getTileState(): Int {
        return if (getPrivateDnsMode() != MODE_OFF) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        val currentMode = getPrivateDnsMode()
        val nextMode = when (currentMode) {
            MODE_OFF -> MODE_AUTO
            MODE_AUTO -> {
                if (getPrivateDnsHostname().isNullOrEmpty()) MODE_OFF else MODE_HOSTNAME
            }
            MODE_HOSTNAME -> MODE_OFF
            else -> MODE_OFF
        }
        
        try {
            Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, nextMode)
        } catch (e: Exception) {
            // Handle error or permission missing
        }
    }

    private fun getPrivateDnsMode(): String {
        return Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE) ?: MODE_OFF
    }

    private fun getPrivateDnsHostname(): String? {
        return Settings.Global.getString(contentResolver, PRIVATE_DNS_SPECIFIER)
    }
}
