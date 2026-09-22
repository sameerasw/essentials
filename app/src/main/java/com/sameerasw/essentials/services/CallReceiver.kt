/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: CallReceiver.kt
 * Description: BroadcastReceiver that listens for telephony state changes and passes call info to WatchCallSyncManager.
 */

package com.sameerasw.essentials.services

import com.sameerasw.essentials.utils.call.CallStateRepository
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CallReceiver"
        private var savedNumber: String? = null
    }

    @Suppress("DEPRECATION")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Log.d(TAG, "Broadcast received: ${intent.action}")

        if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            savedNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            Log.d(TAG, "New outgoing call to: $savedNumber")
            return
        }

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val state =
            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
                TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
                TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
                else -> TelephonyManager.CALL_STATE_IDLE
            }

        val numberToUse =
            when {
                state == TelephonyManager.CALL_STATE_RINGING -> incomingNumber ?: savedNumber
                savedNumber != null -> savedNumber
                else -> incomingNumber
            }

        if (state == TelephonyManager.CALL_STATE_IDLE) {
            savedNumber = null
        }

        try {
            CallStateRepository.onCallStateChanged(context, state, numberToUse)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update call state", e)
        }
        WatchCallSyncManager.onCallStateChanged(context, state, numberToUse)
    }
}
