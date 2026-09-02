/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: EssentialsWearableListenerService.kt
 * Description: Background service component for EssentialsWearableListenerService.kt.
 */

package com.sameerasw.essentials.services

import android.content.Context
import androidx.core.content.edit
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.ScreenOffMethod
import com.sameerasw.essentials.utils.DeviceLockUtils

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
                val intent =
                    android.content
                        .Intent(
                            this,
                            com.sameerasw.essentials.services.receivers.FlashlightActionReceiver::class.java,
                        ).apply {
                            action =
                                com.sameerasw.essentials.services.receivers.FlashlightActionReceiver.ACTION_TOGGLE
                        }
                sendBroadcast(intent)
            }

            "/set_flashlight_intensity" -> {
                val intensity =
                    try {
                        String(messageEvent.data).toInt()
                    } catch (e: Exception) {
                        1
                    }
                val intent =
                    android.content
                        .Intent(
                            this,
                            com.sameerasw.essentials.services.receivers.FlashlightActionReceiver::class.java,
                        ).apply {
                            action =
                                com.sameerasw.essentials.services.receivers.FlashlightActionReceiver.ACTION_SET_INTENSITY
                            putExtra(
                                com.sameerasw.essentials.services.receivers.FlashlightActionReceiver.EXTRA_INTENSITY,
                                intensity,
                            )
                        }
                sendBroadcast(intent)
            }

            "/toggle_sound_mode" -> {
                com.sameerasw.essentials.services.handlers
                    .SoundModeHandler(this)
                    .cycleNextMode()
            }

            "/lock_device" -> {
                val repository = SettingsRepository(this)
                val mode =
                    repository.getInt(
                        SettingsRepository.KEY_REMOTE_LOCK_MODE,
                        0,
                    )
                val method =
                    if (mode == 1) ScreenOffMethod.DEVICE_ADMIN else ScreenOffMethod.ACCESSIBILITY
                DeviceLockUtils.lockDevice(this, method)
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
                val isAodEnabled =
                    android.provider.Settings.Secure.getInt(
                        contentResolver,
                        "doze_always_on",
                        0,
                    ) == 1

                when {
                    isGlanceEnabled -> {
                        prefs.edit(commit = true) {
                            putBoolean("notification_glance_enabled", false)
                        }
                        try {
                            android.provider.Settings.Secure.putInt(
                                contentResolver,
                                "doze_always_on",
                                1,
                            )
                        } catch (_: Exception) {
                            com.sameerasw.essentials.utils.ShellUtils.runCommand(
                                this,
                                "settings put secure doze_always_on 1",
                            )
                        }
                    }

                    isAodEnabled -> {
                        try {
                            android.provider.Settings.Secure.putInt(
                                contentResolver,
                                "doze_always_on",
                                0,
                            )
                        } catch (_: Exception) {
                            com.sameerasw.essentials.utils.ShellUtils.runCommand(
                                this,
                                "settings put secure doze_always_on 0",
                            )
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
                            android.provider.Settings.Secure.putInt(
                                contentResolver,
                                "doze_always_on",
                                0,
                            )
                        } catch (_: Exception) {
                            com.sameerasw.essentials.utils.ShellUtils.runCommand(
                                this,
                                "settings put secure doze_always_on 0",
                            )
                        }
                    }
                }
            }

            "/toggle_tap_to_wake" -> {
                val isEnabled =
                    android.provider.Settings.Secure.getInt(
                        contentResolver,
                        "doze_tap_gesture",
                        1,
                    ) == 1
                val newState = if (isEnabled) 0 else 1
                try {
                    android.provider.Settings.Secure.putInt(
                        contentResolver,
                        "doze_tap_gesture",
                        newState,
                    )
                } catch (_: Exception) {
                    com.sameerasw.essentials.utils.ShellUtils.runCommand(
                        this,
                        "settings put secure doze_tap_gesture $newState",
                    )
                }
            }

            "/watch_status_update" -> {
                val data = messageEvent.data
                if (data != null && data.isNotEmpty()) {
                    val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                    try {
                        val jsonStr = String(data, Charsets.UTF_8)
                        val jsonObj = org.json.JSONObject(jsonStr)
                        val adbWifiEnabled = jsonObj.optBoolean("adb_wifi_enabled", false)
                        val secureSettingsGranted = jsonObj.optBoolean("write_secure_settings_granted", false)
                        val version = jsonObj.optInt("version_code", 0)
                        val ipAddress = jsonObj.optString("ip_address", "")
                        val adbPort = jsonObj.optInt("adb_port", -1)
                        prefs.edit(commit = true) {
                            putBoolean("watch_adb_wifi_enabled", adbWifiEnabled)
                            putBoolean("watch_write_secure_settings_granted", secureSettingsGranted)
                            if (version > 0) {
                                putInt("watch_version_code", version)
                            }
                            putString("watch_adb_wifi_ip", ipAddress)
                            putInt("watch_adb_wifi_port", adbPort)
                        }
                    } catch (_: Exception) {
                        if (data.size >= 2) {
                            val adbWifiEnabled = data[0].toInt() == 1
                            val secureSettingsGranted = data[1].toInt() == 1
                            val version = if (data.size >= 3) data[2].toInt() else 0
                            prefs.edit(commit = true) {
                                putBoolean("watch_adb_wifi_enabled", adbWifiEnabled)
                                putBoolean("watch_write_secure_settings_granted", secureSettingsGranted)
                                if (version > 0) {
                                    putInt("watch_version_code", version)
                                }
                            }
                        }
                    }
                }
            }

            "/set_sync_sound_mode" -> {
                val data = messageEvent.data
                if (data != null && data.isNotEmpty()) {
                    val enabled = data[0].toInt() == 1
                    val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                    prefs.edit(commit = true) {
                        putBoolean("watch_sync_sound_mode_enabled", enabled)
                    }
                    // Immediately push device info to let watch know
                    DeviceInfoSyncManager.forceSync(this)
                }
            }

            "/set_phone_ringer_mode" -> {
                val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                val isSyncEnabled = prefs.getBoolean("watch_sync_sound_mode_enabled", false)
                if (isSyncEnabled) {
                    val ringerMode = messageEvent.data?.firstOrNull()?.toInt() ?: 2
                    val audioManager =
                        getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                    try {
                        audioManager?.ringerMode = ringerMode
                    } catch (e: Exception) {
                        // ignore permission issue
                    }
                }
            }
            "/dismiss_phone_notification" -> {
                val key = String(messageEvent.data ?: byteArrayOf())
                if (key.isNotBlank()) {
                    try {
                        val instance = NotificationListener.instance
                        if (instance != null) {
                            instance.cancelNotification(key)
                        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            android.service.notification.NotificationListenerService.requestRebind(
                                android.content.ComponentName(this, NotificationListener::class.java),
                            )
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("EssentialsWearable", "Error cancelling notification: $key", e)
                    }
                }
            }
            "/reply_phone_notification" -> {
                val jsonStr = String(messageEvent.data ?: byteArrayOf())
                if (jsonStr.isNotBlank()) {
                    WatchNotificationSyncManager.handleReplyFromWatch(this, jsonStr)
                }
            }
            WatchCallSyncManager.PATH_WATCH_CALL_ACTION -> {
                val action = String(messageEvent.data ?: byteArrayOf())
                if (action.isNotBlank()) {
                    WatchCallSyncManager.handleCallAction(this, action)
                }
            }
        }
    }
}
