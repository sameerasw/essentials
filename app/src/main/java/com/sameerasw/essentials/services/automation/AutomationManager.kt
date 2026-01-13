package com.sameerasw.essentials.services.automation

import android.content.Context
import android.content.Intent
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.domain.diy.State as DIYState
import com.sameerasw.essentials.services.automation.modules.AutomationModule
import com.sameerasw.essentials.services.automation.modules.DisplayModule
import com.sameerasw.essentials.services.automation.modules.PowerModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object AutomationManager {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var service: AutomationService? = null
    private var applicationContext: Context? = null
    
    // Active modules map: ModuleID -> Module Instance
    private val activeModules = ConcurrentHashMap<String, AutomationModule>()

    fun init(context: Context) {
        applicationContext = context.applicationContext
        
        // Observe repository
        scope.launch {
            DIYRepository.automations.collect { automations ->
                refreshModules(automations)
            }
        }
    }

    fun onServiceConnected(serviceInstance: AutomationService) {
        service = serviceInstance
        // Re-apply modules if needed
        val automations = DIYRepository.automations.value
        refreshModules(automations)
    }

    fun onServiceDisconnected(serviceInstance: AutomationService) {
        if (service == serviceInstance) {
            service = null
            stopAllModules()
        }
    }

    private fun stopAllModules() {
        val context = applicationContext ?: return
        activeModules.values.forEach { it.stop(context) }
        activeModules.clear()
    }

    private fun refreshModules(automations: List<Automation>) {
        val context = applicationContext ?: return
        val enabledAutomations = automations.filter { it.isEnabled }

        // Determine required modules
        val requiredModuleIds = mutableSetOf<String>()
        val powerAutomations = mutableListOf<Automation>()
        val displayAutomations = mutableListOf<Automation>()

        enabledAutomations.forEach { automation ->
            when (automation.type) {
                Automation.Type.TRIGGER -> {
                    when (automation.trigger) {
                        is Trigger.ChargerConnected, is Trigger.ChargerDisconnected -> {
                            requiredModuleIds.add(PowerModule.ID)
                            powerAutomations.add(automation)
                        }
                        is Trigger.ScreenOn, is Trigger.ScreenOff, is Trigger.ScreenUnlock -> {
                            requiredModuleIds.add(DisplayModule.ID)
                            displayAutomations.add(automation)
                        }
                        else -> {}
                    }
                }
                Automation.Type.STATE -> {
                    when (automation.state) {
                        is DIYState.Charging -> {
                            requiredModuleIds.add(PowerModule.ID)
                            powerAutomations.add(automation)
                        }
                        is DIYState.ScreenOn -> {
                            requiredModuleIds.add(DisplayModule.ID)
                            displayAutomations.add(automation)
                        }
                        else -> {}
                    }
                }
            }
        }

        // Service Lifecycle Management
        if (requiredModuleIds.isNotEmpty()) {
            startService(context)
        } else {
            stopService(context)
            stopAllModules()
            return
        }

        // Module Management
        
        // Power Module
        if (requiredModuleIds.contains(PowerModule.ID)) {
            val module = activeModules.getOrPut(PowerModule.ID) {
                PowerModule().also { it.start(context) }
            }
            module.updateAutomations(powerAutomations)
        } else {
            activeModules.remove(PowerModule.ID)?.stop(context)
        }

        // Display Module
        if (requiredModuleIds.contains(DisplayModule.ID)) {
            val module = activeModules.getOrPut(DisplayModule.ID) {
                DisplayModule().also { it.start(context) }
            }
            module.updateAutomations(displayAutomations)
        } else {
            activeModules.remove(DisplayModule.ID)?.stop(context)
        }
    }

    private fun startService(context: Context) {
        if (!AutomationService.isRunning) {
            val intent = Intent(context, AutomationService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private fun stopService(context: Context) {
        if (AutomationService.isRunning) {
            context.stopService(Intent(context, AutomationService::class.java))
        }
    }
}
