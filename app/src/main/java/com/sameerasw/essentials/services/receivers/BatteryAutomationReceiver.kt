/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: BatteryAutomationReceiver.kt
 * Description: Background service component for BatteryAutomationReceiver.kt.
 */

package com.sameerasw.essentials.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.BatteryStatsUtil

class BatteryAutomationReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action ?: return
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isPlugged =
            action == Intent.ACTION_POWER_CONNECTED ||
                (
                    action == Intent.ACTION_BATTERY_CHANGED &&
                        intent.getIntExtra(
                            BatteryManager.EXTRA_PLUGGED,
                            0,
                        ) > 0
                )

        if (level >= 0) {
            val force =
                action == Intent.ACTION_POWER_CONNECTED || action == Intent.ACTION_POWER_DISCONNECTED
            com.sameerasw.essentials.utils.BatteryHistoryManager.recordPoint(
                context,
                level,
                isPlugged,
                forceRecord = force,
            )
        }

        if (action == Intent.ACTION_POWER_DISCONNECTED) {
            checkAutoResetBatteryStats(context)
        }
    }

    private fun checkAutoResetBatteryStats(context: Context) {
        val repo = SettingsRepository(context)
        val autoResetEnabled = repo.getBoolean("auto_reset_battery_stats", false)
        if (autoResetEnabled) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (level >= 90) {
                BatteryStatsUtil.resetStats(context)
            }
        }
    }
}
