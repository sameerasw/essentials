/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: AndroidAutoModule.kt
 * Description: Fires automations when Android Auto starts or stops projecting.
 */

package com.sameerasw.essentials.services.automation.modules

import android.content.Context
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.utils.CarConnectionMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class AndroidAutoModule : AutomationModule {
    companion object {
        const val ID = "android_auto_module"
    }

    override val id: String = ID
    private var automations: List<Automation> = emptyList()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var appContext: Context? = null
    private var isProjecting = false

    private val listener: (Boolean) -> Unit = { projecting ->
        if (projecting != isProjecting) {
            isProjecting = projecting
            appContext?.let {
                handleTrigger(it, if (projecting) Trigger.AndroidAutoConnected else Trigger.AndroidAutoDisconnected)
            }
        }
    }

    override fun start(context: Context) {
        appContext = context.applicationContext
        isProjecting = CarConnectionMonitor.isProjecting
        CarConnectionMonitor.addListener(listener)
    }

    override fun stop(context: Context) {
        CarConnectionMonitor.removeListener(listener)
        scope.coroutineContext.cancelChildren()
        appContext = null
    }

    override fun updateAutomations(automations: List<Automation>) {
        this.automations = automations
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
}
