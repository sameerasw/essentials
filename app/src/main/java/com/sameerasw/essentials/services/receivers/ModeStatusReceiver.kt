/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeStatusReceiver.kt
 * Description: Follows changes the system makes to Essentials-owned modes.
 */

package com.sameerasw.essentials.services.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sameerasw.essentials.domain.modes.ModesController
import com.sameerasw.essentials.domain.modes.ModesRepository

class ModeStatusReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != NotificationManager.ACTION_AUTOMATIC_ZEN_RULE_STATUS_CHANGED) return
        val ruleId = intent.getStringExtra(NotificationManager.EXTRA_AUTOMATIC_ZEN_RULE_ID) ?: return
        val status = intent.getIntExtra(NotificationManager.EXTRA_AUTOMATIC_ZEN_RULE_STATUS, NotificationManager.AUTOMATIC_RULE_STATUS_UNKNOWN)

        ModesRepository.init(context)
        val modeId = ModesController.modeIdForRule(context, ruleId)
        Log.d(TAG, "Rule $ruleId (mode $modeId) status $status")

        if (modeId != null) {
            when (status) {
                NotificationManager.AUTOMATIC_RULE_STATUS_REMOVED -> ModesRepository.remove(modeId)
                NotificationManager.AUTOMATIC_RULE_STATUS_ENABLED -> ModesRepository.setEnabled(modeId, true)
                NotificationManager.AUTOMATIC_RULE_STATUS_DISABLED -> ModesRepository.setEnabled(modeId, false)
            }
        }
        ModesController.refreshActive(context)
    }

    private companion object {
        const val TAG = "ModeStatusReceiver"
    }
}
