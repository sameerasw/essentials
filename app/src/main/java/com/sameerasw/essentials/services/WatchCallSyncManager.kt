/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: WatchCallSyncManager.kt
 * Description: Manages phone call status synchronization and remote call control actions for WearOS.
 */

package com.sameerasw.essentials.services

import com.sameerasw.essentials.utils.call.CallerLookup
import android.content.Context
import android.graphics.Bitmap
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.sameerasw.essentials.utils.CallControlUtil
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object WatchCallSyncManager {
    private const val TAG = "WatchCallSyncManager"
    const val PATH_WATCH_CALL_STATE = "/watch_call_state"
    const val PATH_WATCH_CALL_ACTION = "/watch_call_action"

    fun isCallSyncEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("watch_call_sync_enabled", false)
    }

    fun onCallStateChanged(
        context: Context,
        state: Int,
        phoneNumber: String?,
    ) {
        if (!isCallSyncEnabled(context)) return

        val stateStr =
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> "RINGING"
                TelephonyManager.CALL_STATE_OFFHOOK -> "OFFHOOK"
                TelephonyManager.CALL_STATE_IDLE -> "IDLE"
                else -> "IDLE"
            }

        val contactName = lookupContactName(context, phoneNumber)
        val contactPhoto = lookupContactPhotoBase64(context, phoneNumber)

        val json =
            JSONObject().apply {
                put("state", stateStr)
                put("number", phoneNumber ?: "")
                put("contactName", contactName ?: "")
                put("contactPhoto", contactPhoto ?: "")
                put("isIncoming", state == TelephonyManager.CALL_STATE_RINGING)
                put("timestamp", System.currentTimeMillis())
            }

        Log.d(TAG, "Sending call state to watch: state=$stateStr, number=$phoneNumber, name=$contactName")
        sendMessageToWatch(context, PATH_WATCH_CALL_STATE, json.toString().toByteArray())
    }

    fun handleCallAction(
        context: Context,
        action: String,
    ) {
        Log.d(TAG, "Handling call action from watch: $action")
        when (action.uppercase()) {
            "ANSWER" -> CallControlUtil.acceptCall(context)
            "REJECT", "END" -> CallControlUtil.endCall(context)
            "MUTE" -> CallControlUtil.toggleMute(context)
            else -> Log.w(TAG, "Unknown call action: $action")
        }
    }

    private fun sendMessageToWatch(
        context: Context,
        path: String,
        data: ByteArray,
    ) {
        try {
            Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                for (node in nodes) {
                    Wearable
                        .getMessageClient(context)
                        .sendMessage(node.id, path, data)
                        .addOnSuccessListener {
                            Log.d(TAG, "Sent message $path to watch node: ${node.displayName}")
                        }.addOnFailureListener { e ->
                            Log.e(TAG, "Failed to send message $path to watch node", e)
                        }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message to watch", e)
        }
    }

    private fun lookupContactName(
        context: Context,
        number: String?,
    ): String? = CallerLookup.name(context, number)

    private fun lookupContactPhotoBase64(
        context: Context,
        number: String?,
    ): String? {
        val bitmap = CallerLookup.photo(context, number) ?: return null
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding contact photo", e)
            null
        }
    }
}
