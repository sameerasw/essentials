/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeConfirmReceiver.kt
 * Description: Handles the answer to a mode confirmation notification.
 */

package com.sameerasw.essentials.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sameerasw.essentials.domain.modes.ModeAutomation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ModeConfirmReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_MODE_ID = "mode_id"
        const val EXTRA_ACCEPT = "accept"
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val modeId = intent.getStringExtra(EXTRA_MODE_ID) ?: return
        val accept = intent.getBooleanExtra(EXTRA_ACCEPT, false)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ModeAutomation.respond(context.applicationContext, modeId, accept)
            } finally {
                pending.finish()
            }
        }
    }
}
