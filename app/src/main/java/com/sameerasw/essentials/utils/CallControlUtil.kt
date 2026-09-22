/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities
 * File: CallControlUtil.kt
 * Description: Utility functions to programmatically answer, end, and mute phone calls.
 */

package com.sameerasw.essentials.utils

import android.media.AudioDeviceInfo
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.ContextCompat

object CallControlUtil {
    private const val TAG = "CallControlUtil"

    fun acceptCall(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hasPermission =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ANSWER_PHONE_CALLS,
                ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                try {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    if (telecomManager != null) {
                        Log.d(TAG, "Accepting call via TelecomManager")
                        telecomManager.acceptRingingCall()
                        return
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to accept call via TelecomManager", e)
                }
            }
        }
        emulateHeadsetHookClick(context)
    }

    fun endCall(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val hasPermission =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ANSWER_PHONE_CALLS,
                ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                try {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    if (telecomManager != null) {
                        Log.d(TAG, "Ending call via TelecomManager")
                        val success = telecomManager.endCall()
                        if (success) return
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to end call via TelecomManager", e)
                }
            }
        }
        emulateHeadsetHookClick(context)
    }

    fun toggleMute(context: Context): Boolean =
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val currentMute = audioManager.isMicrophoneMute
                val newMute = !currentMute
                audioManager.isMicrophoneMute = newMute
                Log.d(TAG, "Toggled microphone mute: $newMute")
                newMute
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling microphone mute", e)
            false
        }

    fun isMuted(context: Context): Boolean =
        (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.isMicrophoneMute ?: false

    fun isSpeakerOn(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } else {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn
        }
    }

    fun toggleSpeaker(context: Context): Boolean =
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager == null) {
                false
            } else {
                val enable = !isSpeakerOn(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (enable) {
                        audioManager.availableCommunicationDevices
                            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                            ?.let { audioManager.setCommunicationDevice(it) }
                    } else {
                        audioManager.clearCommunicationDevice()
                    }
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = enable
                }
                Log.d(TAG, "Toggled speaker: $enable")
                enable
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling speaker", e)
            false
        }

    fun showInCallScreen(context: Context) {
        try {
            (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.showInCallScreen(false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show in-call screen", e)
        }
    }

    private fun emulateHeadsetHookClick(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK)
                val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK)
                audioManager.dispatchMediaKeyEvent(downEvent)
                audioManager.dispatchMediaKeyEvent(upEvent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error emulating headset hook click", e)
        }
    }
}
