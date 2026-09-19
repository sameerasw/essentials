/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: WatchCallSyncManager.kt
 * Description: Manages phone call status synchronization and remote call control actions for WearOS.
 */

package com.sameerasw.essentials.services

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
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
    ): String? {
        if (number.isNullOrBlank()) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIdx != -1) cursor.getString(nameIdx) else null
                } else {
                    null
                }
            } finally {
                cursor?.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up contact name", e)
            null
        }
    }

    private fun lookupContactPhotoBase64(
        context: Context,
        number: String?,
    ): String? {
        if (number.isNullOrBlank()) return null
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val projection = arrayOf(ContactsContract.PhoneLookup._ID)
            var photoBase64: String? = null
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    if (idIdx != -1) {
                        val contactId = cursor.getLong(idIdx)
                        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
                        val photoStream = ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, contactUri)
                        if (photoStream != null) {
                            val bitmap = BitmapFactory.decodeStream(photoStream)
                            photoStream.close()
                            if (bitmap != null) {
                                val outputStream = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                                photoBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                            }
                        }
                    }
                }
            } finally {
                cursor?.close()
            }
            photoBase64
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up contact photo", e)
            null
        }
    }
}
