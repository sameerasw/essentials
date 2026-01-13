package com.sameerasw.essentials.services.automation.modules

import android.content.Context
import com.sameerasw.essentials.domain.diy.Automation

interface AutomationModule {
    val id: String
    fun start(context: Context)
    fun stop(context: Context)
    fun updateAutomations(automations: List<Automation>)
}
