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
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.domain.diy.DIYRuntimeStore
import com.sameerasw.essentials.domain.diy.Metric
import com.sameerasw.essentials.domain.diy.SystemMetricTracker
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.utils.AutomationFailureNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.sameerasw.essentials.domain.diy.State as DIYState

class PowerModule : AutomationModule {
    companion object {
        const val ID = "power_module"
    }

    override val id: String = ID
    private var automations: List<Automation> = emptyList()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var tracker: SystemMetricTracker? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val automationMutexes = mutableMapOf<String, Mutex>()

    // State tracking
    private var isCharging = false
    private var isPowerSaving = false
    private var appContext: Context? = null
    private var batteryLevel = -1

    private fun getMutex(id: String): Mutex = automationMutexes.getOrPut(id) { Mutex() }

    private fun runOrdered(
        automation: Automation,
        actions: List<Action>,
    ) {
        if (actions.isEmpty()) return
        val context = appContext ?: return
        val mutex = getMutex(automation.id)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.withLock {
                CombinedActionExecutor.executeAll(context, actions)
            }
        }
    }

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
                                // The broadcast fires on any battery field change, so only forward level changes.
                                tracker?.onReading(Metric.BATTERY_LEVEL, percent.toDouble())
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
        DIYRuntimeStore.init(context)
        tracker =
            SystemMetricTracker(
                clock = { SystemClock.elapsedRealtime() },
                scheduler = { delayMs, block ->
                    val runnable = Runnable { block() }
                    mainHandler.postDelayed(runnable, delayMs)
                    val cancel: () -> Unit = { mainHandler.removeCallbacks(runnable) }
                    cancel
                },
                openStore =
                    object : SystemMetricTracker.OpenStateStore {
                        override fun load(): Set<String> = DIYRuntimeStore.openStateIds(ID)

                        override fun save(ids: Set<String>) = DIYRuntimeStore.setOpenStateIds(ID, ids)
                    },
                listener =
                    object : SystemMetricTracker.Listener {
                        override fun onFire(automation: Automation) {
                            runOrdered(automation, automation.actionList)
                        }

                        override fun onEnter(automation: Automation) {
                            runOrdered(automation, automation.entryActionList)
                        }

                        override fun onExit(automation: Automation) {
                            runOrdered(automation, automation.exitActionList)
                        }

                        override fun onLoopBreak(automation: Automation) {
                            DIYRepository.pauseAutomation(automation.id)
                            appContext?.let {
                                AutomationFailureNotifier.notifyLoopBreak(it, automation)
                            }
                        }
                    },
            )
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
        tracker?.closeAll()
        tracker = null
        mainHandler.removeCallbacksAndMessages(null)
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        batteryLevel = -1
        appContext = null
    }

    override fun updateAutomations(automations: List<Automation>) {
        this.automations = automations
        tracker?.updateAutomations(automations)
    }

    private fun handleTrigger(
        context: Context,
        trigger: Trigger,
    ) {
        scope.launch {
            automations
                .filter { it.type == Automation.Type.TRIGGER && it.trigger == trigger }
                .forEach { automation ->
                    CombinedActionExecutor.executeAll(context, automation.actionList)
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
                            CombinedActionExecutor.executeAll(context, automation.entryActionList)
                        } else {
                            // Exit
                            CombinedActionExecutor.executeAll(context, automation.exitActionList)
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
                            CombinedActionExecutor.executeAll(context, automation.entryActionList)
                        } else {
                            // Exit
                            CombinedActionExecutor.executeAll(context, automation.exitActionList)
                        }
                    }
                }
        }
    }
}
