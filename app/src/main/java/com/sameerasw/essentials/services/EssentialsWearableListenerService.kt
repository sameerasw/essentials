package com.sameerasw.essentials.services

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

import androidx.core.content.edit

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
                val intent = android.content.Intent(
                    this,
                    com.sameerasw.essentials.services.receivers.FlashlightActionReceiver::class.java
                ).apply {
                    action =
                        com.sameerasw.essentials.services.receivers.FlashlightActionReceiver.ACTION_TOGGLE
                }
                sendBroadcast(intent)
            }

            "/set_flashlight_intensity" -> {
                val intensity = try {
                    String(messageEvent.data).toInt()
                } catch (e: Exception) {
                    1
                }
                val intent = android.content.Intent(
                    this,
                    com.sameerasw.essentials.services.receivers.FlashlightActionReceiver::class.java
                ).apply {
                    action =
                        com.sameerasw.essentials.services.receivers.FlashlightActionReceiver.ACTION_SET_INTENSITY
                    putExtra(
                        com.sameerasw.essentials.services.receivers.FlashlightActionReceiver.EXTRA_INTENSITY,
                        intensity
                    )
                }
                sendBroadcast(intent)
            }

            "/toggle_sound_mode" -> {
                com.sameerasw.essentials.services.handlers.SoundModeHandler(this).cycleNextMode()
            }

            "/lock_device" -> {
                val repository = com.sameerasw.essentials.data.repository.SettingsRepository(this)
                val mode = repository.getInt(
                    com.sameerasw.essentials.data.repository.SettingsRepository.KEY_REMOTE_LOCK_MODE,
                    0
                )

                if (mode == 1) {
                    // Device Admin Lock
                    val dpm =
                        getSystemService(DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                    val adminComponent = android.content.ComponentName(
                        this,
                        com.sameerasw.essentials.services.receivers.SecurityDeviceAdminReceiver::class.java
                    )
                    if (dpm.isAdminActive(adminComponent)) {
                        dpm.lockNow()
                    }
                } else {
                    // Accessibility Lock
                    val intent = android.content.Intent(
                        this,
                        com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService::class.java
                    ).apply {
                        action = "LOCK_SCREEN"
                    }
                    startService(intent)
                }
            }

            "/toggle_flashlight_pulse" -> {
                val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                val enabled = prefs.getBoolean("flashlight_pulse_enabled", false)
                prefs.edit(commit = true) {
                    putBoolean("flashlight_pulse_enabled", !enabled)
                }
            }

            "/toggle_aod" -> {
                val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                val isGlanceEnabled = prefs.getBoolean("notification_glance_enabled", false)
                val isAodEnabled = android.provider.Settings.Secure.getInt(contentResolver, "doze_always_on", 0) == 1

                when {
                    isGlanceEnabled -> {
                        prefs.edit(commit = true) {
                            putBoolean("notification_glance_enabled", false)
                        }
                        try {
                            android.provider.Settings.Secure.putInt(contentResolver, "doze_always_on", 1)
                        } catch (_: Exception) {
                            com.sameerasw.essentials.utils.ShellUtils.runCommand(this, "settings put secure doze_always_on 1")
                        }
                    }
                    isAodEnabled -> {
                        try {
                            android.provider.Settings.Secure.putInt(contentResolver, "doze_always_on", 0)
                        } catch (_: Exception) {
                            com.sameerasw.essentials.utils.ShellUtils.runCommand(this, "settings put secure doze_always_on 0")
                        }
                        prefs.edit(commit = true) {
                            putBoolean("notification_glance_enabled", false)
                        }
                    }
                    else -> {
                        prefs.edit(commit = true) {
                            putBoolean("notification_glance_enabled", true)
                        }
                        try {
                            android.provider.Settings.Secure.putInt(contentResolver, "doze_always_on", 0)
                        } catch (_: Exception) {
                            com.sameerasw.essentials.utils.ShellUtils.runCommand(this, "settings put secure doze_always_on 0")
                        }
                    }
                }
            }

            "/toggle_tap_to_wake" -> {
                val isEnabled = android.provider.Settings.Secure.getInt(contentResolver, "doze_tap_gesture", 1) == 1
                val newState = if (isEnabled) 0 else 1
                try {
                    android.provider.Settings.Secure.putInt(contentResolver, "doze_tap_gesture", newState)
                } catch (_: Exception) {
                    com.sameerasw.essentials.utils.ShellUtils.runCommand(this, "settings put secure doze_tap_gesture $newState")
                }
            }

            "/watch_status_update" -> {
                val data = messageEvent.data
                if (data != null && data.size >= 2) {
                    val adbWifiEnabled = data[0].toInt() == 1
                    val secureSettingsGranted = data[1].toInt() == 1
                    val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                    prefs.edit(commit = true) {
                        putBoolean("watch_adb_wifi_enabled", adbWifiEnabled)
                        putBoolean("watch_write_secure_settings_granted", secureSettingsGranted)
                    }
                }
            }
        }
    }
}
