package com.sameerasw.essentials.services.automation.modules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    if (!isCharging) {
                        isCharging = true
                        handleTrigger(context, Trigger.ChargerConnected)
                        handleStateChange(context, true)
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    if (isCharging) {
                        isCharging = false
                        handleTrigger(context, Trigger.ChargerDisconnected)
                        handleStateChange(context, false)
                    }
                }
            }
        }
    }

    override fun start(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter)

        // Initial check
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                     status == BatteryManager.BATTERY_STATUS_FULL
                     
        if (isCharging) {
            handleStateChange(context, true)
        }
    }

    override fun stop(context: Context) {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }

    override fun updateAutomations(automations: List<Automation>) {
        this.automations = automations
    }

    private fun handleTrigger(context: Context, trigger: Trigger) {
        scope.launch {
            automations.filter { it.type == Automation.Type.TRIGGER && it.trigger == trigger }
                .forEach { automation ->
                    automation.actions.forEach { action ->
                        CombinedActionExecutor.execute(context, action)
                    }
                }
        }
    }

    private fun handleStateChange(context: Context, isActive: Boolean) {
        scope.launch {
            automations.filter { it.type == Automation.Type.STATE }
                .forEach { automation ->
                    if (automation.state is DIYState.Charging) {
                         if (isActive) {
                             // Entry
                             automation.entryAction?.let { CombinedActionExecutor.execute(context, it) }
                         } else {
                             // Exit
                             automation.exitAction?.let { CombinedActionExecutor.execute(context, it) }
                         }
                    }
                }
        }
    }
}
