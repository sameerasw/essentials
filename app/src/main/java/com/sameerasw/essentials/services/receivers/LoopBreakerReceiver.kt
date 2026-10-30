/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: LoopBreakerReceiver.kt
 * Description: Receiver to re-enable automations paused by the loop breaker.
 */

package com.sameerasw.essentials.services.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sameerasw.essentials.domain.diy.DIYRepository

class LoopBreakerReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val automationId = intent.getStringExtra("automation_id") ?: return
        DIYRepository.init(context)
        val automation = DIYRepository.getAutomation(automationId) ?: return
        DIYRepository.updateAutomation(automation.copy(isEnabled = true))
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(automationId.hashCode())
    }
}
