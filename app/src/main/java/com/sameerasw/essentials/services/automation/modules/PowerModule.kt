/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: PowerModule.kt
 * Description: Background service component for PowerModule.kt.
 */

package com.sameerasw.essentials.services.automation.modules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.sameerasw.essentials.domain.diy.State as DIYState

class PowerModule : AutomationModule {
    companion object {
        const val ID = "power_module"
    }

    override val id: String = ID
    private var automations: List<Automation> = emptyList()
    private val scope = CoroutineScope(Dispatchers.IO)

    // State tracking
    private var isCharging = false
    private var isPowerSaving = false
    private var appContext: Context? = null
    private var batteryLevel = -1
    private val batteryStateActive = mutableMapOf<String, Boolean>()

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                when (intent.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        if (!isCharging) {
                            isCharging = true
                            handleTrigger(context, Trigger.ChargerConnected)
                            handleChargingStateChange(context, true)
                        }
                    }

                    Intent.ACTION_POWER_DISCONNECTED -> {
                        if (isCharging) {
                            isCharging = false
                            handleTrigger(context, Trigger.ChargerDisconnected)
                            handleChargingStateChange(context, false)
                        }
                    }

                    Intent.ACTION_BATTERY_CHANGED -> {
                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                        if (level >= 0 && scale > 0) {
                            val percent = level * 100 / scale
                            if (percent != batteryLevel) {
                                batteryLevel = percent
                                evaluateBatteryLevel(context)
                            }
                        }
                    }

                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                        val currentPowerSave = powerManager?.isPowerSaveMode == true
                        if (currentPowerSave != isPowerSaving) {
                            isPowerSaving = currentPowerSave
                            if (isPowerSaving) {
                                handleTrigger(context, Trigger.PowerSavingOn)
                                handlePowerSavingStateChange(context, true)
                            } else {
                                handleTrigger(context, Trigger.PowerSavingOff)
                                handlePowerSavingStateChange(context, false)
                            }
                        }
                    }
                }
            }
        }

    override fun start(context: Context) {
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
            }
        appContext = context.applicationContext
        context.registerReceiver(receiver, filter)

        // Initial check for charging
        val batteryStatus: Intent? =
            IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                context.registerReceiver(null, ifilter)
            }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        if (isCharging) {
            handleChargingStateChange(context, true)
        }

        // Initial check for power saving
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isPowerSaving = powerManager?.isPowerSaveMode == true
        if (isPowerSaving) {
            handlePowerSavingStateChange(context, true)
        }
    }

    override fun stop(context: Context) {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        batteryStateActive.clear()
        batteryLevel = -1
        appContext = null
    }

    override fun updateAutomations(automations: List<Automation>) {
        this.automations = automations
        batteryStateActive.keys.retainAll(automations.map { it.id }.toSet())
        appContext?.let { evaluateBatteryLevel(it) }
    }

    private fun evaluateBatteryLevel(context: Context) {
        val level = batteryLevel
        if (level < 0) return
        automations
            .filter { it.type == Automation.Type.STATE && it.isEnabled }
            .forEach { automation ->
                val state = automation.state as? DIYState.BatteryLevel ?: return@forEach
                val inRange = level in state.minLevel..state.maxLevel
                val previous = batteryStateActive.put(automation.id, inRange)
                if (inRange == (previous ?: false)) return@forEach
                val action = if (inRange) automation.entryAction else automation.exitAction
                if (action == null) return@forEach
                scope.launch { CombinedActionExecutor.execute(context, action) }
            }
    }

    private fun handleTrigger(
        context: Context,
        trigger: Trigger,
    ) {
        scope.launch {
            automations
                .filter { it.type == Automation.Type.TRIGGER && it.trigger == trigger }
                .forEach { automation ->
                    automation.actions.forEach { action ->
                        CombinedActionExecutor.execute(context, action)
                    }
                }
        }
    }

    private fun handleChargingStateChange(
        context: Context,
        isActive: Boolean,
    ) {
        scope.launch {
            automations
                .filter { it.type == Automation.Type.STATE }
                .forEach { automation ->
                    if (automation.state is DIYState.Charging) {
                        if (isActive) {
                            // Entry
                            automation.entryAction?.let {
                                CombinedActionExecutor.execute(
                                    context,
                                    it,
                                )
                            }
                        } else {
                            // Exit
                            automation.exitAction?.let {
                                CombinedActionExecutor.execute(
                                    context,
                                    it,
                                )
                            }
                        }
                    }
                }
        }
    }

    private fun handlePowerSavingStateChange(
        context: Context,
        isActive: Boolean,
    ) {
        scope.launch {
            automations
                .filter { it.type == Automation.Type.STATE }
                .forEach { automation ->
                    if (automation.state is DIYState.PowerSaving) {
                        if (isActive) {
                            // Entry
                            automation.entryAction?.let {
                                CombinedActionExecutor.execute(
                                    context,
                                    it,
                                )
                            }
                        } else {
                            // Exit
                            automation.exitAction?.let {
                                CombinedActionExecutor.execute(
                                    context,
                                    it,
                                )
                            }
                        }
                    }
                }
        }
    }
}
