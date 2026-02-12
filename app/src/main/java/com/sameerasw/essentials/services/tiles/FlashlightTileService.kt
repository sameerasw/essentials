package com.sameerasw.essentials.services.tiles

import android.content.Intent
import android.graphics.drawable.Icon
import android.hardware.camera2.CameraManager
import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.receivers.FlashlightActionReceiver

@RequiresApi(Build.VERSION_CODES.N)
class FlashlightTileService : BaseTileService() {

    private var isTorchOn = false
    private val cameraManager by lazy { getSystemService(CAMERA_SERVICE) as CameraManager }
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            isTorchOn = enabled
            updateTile()
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            cameraManager.registerTorchCallback(torchCallback, null)
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onDestroy() {
        try {
            cameraManager.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {
            // ignore
        }
        super.onDestroy()
    }

    override fun onTileClick() {
        val intent = Intent(this, FlashlightActionReceiver::class.java).apply {
            action = FlashlightActionReceiver.ACTION_TOGGLE
        }
        sendBroadcast(intent)
    }

    override fun getTileLabel(): String = "Flashlight"

    override fun getTileSubtitle(): String = if (isTorchOn) "On" else "Off"

    override fun hasFeaturePermission(): Boolean = true

    override fun getTileIcon(): Icon {
        val resId =
            if (isTorchOn) R.drawable.round_flashlight_on_24 else R.drawable.rounded_flashlight_on_24
        return Icon.createWithResource(this, resId)
    }

    override fun getTileState(): Int = if (isTorchOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
}
