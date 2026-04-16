package com.sameerasw.essentials.services.tiles

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.handlers.SoundModeHandler

@RequiresApi(Build.VERSION_CODES.N)
class SoundModeTileService : BaseTileService() {

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                updateTile()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        this.registerReceiver(
            broadcastReceiver,
            IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        )
    }

    override fun onDestroy() {
        try {
            this.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    override fun onTileClick() {
        SoundModeHandler(this).cycleNextMode()
    }

    override fun getTileLabel(): String {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "Sound"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            else -> "Sound"
        }
    }

    override fun getTileSubtitle(): String = if (hasFeaturePermission()) "Mode" else getString(R.string.permission_missing)

    override fun hasFeaturePermission(): Boolean {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    override fun getTileIcon(): Icon {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val resId = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> R.drawable.rounded_volume_up_24
            AudioManager.RINGER_MODE_VIBRATE -> R.drawable.rounded_mobile_vibrate_24
            AudioManager.RINGER_MODE_SILENT -> R.drawable.rounded_volume_off_24
            else -> R.drawable.rounded_volume_up_24
        }
        return Icon.createWithResource(this, resId)
    }

    override fun getTileState(): Int {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        return if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            Tile.STATE_INACTIVE
        } else {
            Tile.STATE_ACTIVE
        }
    }
}
