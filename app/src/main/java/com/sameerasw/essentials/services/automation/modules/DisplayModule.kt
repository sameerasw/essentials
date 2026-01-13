package com.sameerasw.essentials.services.automation.modules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.sameerasw.essentials.domain.diy.State as DIYState

class DisplayModule : AutomationModule {
    companion object {
        const val ID = "display_module"
    }

    override val id: String = ID
    private var automations: List<Automation> = emptyList()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    handleTrigger(context, Trigger.ScreenOn)
                    handleStateChange(context, true)
                }
                Intent.ACTION_SCREEN_OFF -> {
                    handleTrigger(context, Trigger.ScreenOff)
                    handleStateChange(context, false)
                }
                Intent.ACTION_USER_PRESENT -> {
                    handleTrigger(context, Trigger.DeviceUnlock)
                }
            }
        }
    }

    override fun start(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(receiver, filter)
    }

    override fun stop(context: Context) {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Ignore
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
                    if (automation.state is DIYState.ScreenOn) {
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
