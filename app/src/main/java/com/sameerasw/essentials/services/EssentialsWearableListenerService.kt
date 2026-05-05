package com.sameerasw.essentials.services

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class EssentialsWearableListenerService : WearableListenerService() {
    companion object {
        private const val TAG = "EssentialsWearableListener"
        private const val PATH_REQUEST_SYNC = "/request_device_info_sync"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        
        when (messageEvent.path) {
            PATH_REQUEST_SYNC -> {
                DeviceInfoSyncManager.forceSync(this)
            }
            "/toggle_flashlight" -> {
                val intent = android.content.Intent(this, com.sameerasw.essentials.services.receivers.FlashlightActionReceiver::class.java).apply {
                    action = com.sameerasw.essentials.services.receivers.FlashlightActionReceiver.ACTION_TOGGLE
                }
                sendBroadcast(intent)
            }
            "/set_flashlight_intensity" -> {
                val intensity = try {
                    String(messageEvent.data).toInt()
                } catch (e: Exception) {
                    1
                }
                val intent = android.content.Intent(this, com.sameerasw.essentials.services.receivers.FlashlightActionReceiver::class.java).apply {
                    action = com.sameerasw.essentials.services.receivers.FlashlightActionReceiver.ACTION_SET_INTENSITY
                    putExtra(com.sameerasw.essentials.services.receivers.FlashlightActionReceiver.EXTRA_INTENSITY, intensity)
                }
                sendBroadcast(intent)
            }
            "/toggle_sound_mode" -> {
                com.sameerasw.essentials.services.handlers.SoundModeHandler(this).cycleNextMode()
            }
            "/lock_device" -> {
                val repository = com.sameerasw.essentials.data.repository.SettingsRepository(this)
                val mode = repository.getInt(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_REMOTE_LOCK_MODE, 0)
                
                if (mode == 1) {
                    // Device Admin Lock
                    val dpm = getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                    val adminComponent = android.content.ComponentName(this, com.sameerasw.essentials.services.receivers.SecurityDeviceAdminReceiver::class.java)
                    if (dpm.isAdminActive(adminComponent)) {
                        dpm.lockNow()
                    }
                } else {
                    // Accessibility Lock
                    val intent = android.content.Intent(this, com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService::class.java).apply {
                        action = "LOCK_SCREEN"
                    }
                    startService(intent)
                }
            }
        }
    }
}
