package com.sameerasw.essentials.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.utils.HapticUtil

class CallVibrationReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CallVibrationReceiver"
        private var isOutgoing = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean(SettingsRepository.KEY_CALL_VIBRATIONS_ENABLED, false)) return

        if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            isOutgoing = true
            return
        }

        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

        val currentState = when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            else -> TelephonyManager.CALL_STATE_IDLE
        }

        val lastState = prefs.getInt("vibration_last_call_state", TelephonyManager.CALL_STATE_IDLE)

        if (currentState == lastState) return

        when (currentState) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isOutgoing = false
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    // Incoming call answered
                    HapticUtil.performHapticForService(context, HapticFeedbackType.TICK)
                } else if (lastState == TelephonyManager.CALL_STATE_IDLE) {
                    // Outgoing call started (dialing)
                    isOutgoing = true
                    HapticUtil.performHapticForService(context, HapticFeedbackType.TICK)
                }
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                // Call ended or declined
                HapticUtil.performHapticForService(context, HapticFeedbackType.DOUBLE)
                isOutgoing = false
            }
        }

        // Persist state for next transition
        prefs.edit().putInt("vibration_last_call_state", currentState).apply()
    }
}
