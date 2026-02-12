package com.sameerasw.essentials.services.tiles

import android.content.Context
import android.graphics.drawable.Icon
import android.nfc.NfcAdapter
import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.R
import java.lang.reflect.Method

@RequiresApi(Build.VERSION_CODES.N)
class NfcTileService : BaseTileService() {

    private val nfcReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: android.content.Intent?) {
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED == intent?.action) {
                updateTile()
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        val intentFilter = android.content.IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        registerReceiver(nfcReceiver, intentFilter)
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        unregisterReceiver(nfcReceiver)
    }

    override fun onTileClick() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this) ?: return
        val currentlyEnabled = nfcAdapter.isEnabled

        // Toggle the state
        val success = setNfcEnabled(this, !currentlyEnabled)

        if (success) {
            val tile = qsTile
            if (tile != null) {
                updateTile()
            }
        }
    }

    override fun getTileLabel(): String = getString(R.string.nfc_tile_label)

    override fun getTileSubtitle(): String {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        return if (nfcAdapter?.isEnabled == true) getString(R.string.on) else getString(R.string.off)
    }

    override fun hasFeaturePermission(): Boolean {
        // We need WRITE_SECURE_SETTINGS to toggle NFC via reflection
        return checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun getTileIcon(): Icon {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val isEnabled = nfcAdapter?.isEnabled == true
        val resId = if (isEnabled) R.drawable.rounded_nfc_24 else R.drawable.rounded_nfc_24
        return Icon.createWithResource(this, resId)
    }

    override fun getTileState(): Int {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        return if (nfcAdapter?.isEnabled == true) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    private fun setNfcEnabled(context: Context, enable: Boolean): Boolean {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context) ?: return false

        return try {
            val methodName = if (enable) "enable" else "disable"
            // Get the hidden method from the NfcAdapter class
            val method: Method = nfcAdapter.javaClass.getMethod(methodName)
            method.invoke(nfcAdapter) as Boolean
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
