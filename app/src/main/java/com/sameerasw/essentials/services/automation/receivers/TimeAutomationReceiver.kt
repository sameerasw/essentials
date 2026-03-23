package com.sameerasw.essentials.services.automation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimeAutomationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TRIGGER = "com.sameerasw.essentials.ACTION_TIME_AUTOMATION_TRIGGER"
        const val EXTRA_AUTOMATION_ID = "automation_id"
        const val EXTRA_IS_ENTRY = "is_entry"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TRIGGER) return

        val automationId = intent.getStringExtra(EXTRA_AUTOMATION_ID) ?: return
        val isEntry = intent.getBooleanExtra(EXTRA_IS_ENTRY, true)

        val pendingResult = goAsync()
        scope.launch {
            try {
                DIYRepository.init(context)
                val automation = DIYRepository.getAutomation(automationId) ?: return@launch
                if (!automation.isEnabled) {
                    return@launch
                }

                when (automation.type) {
                    Automation.Type.TRIGGER -> {
                        automation.actions.forEach { action ->
                            CombinedActionExecutor.execute(context, action)
                        }
                    }
                    Automation.Type.STATE -> {
                        val action = if (isEntry) automation.entryAction else automation.exitAction
                        action?.let { CombinedActionExecutor.execute(context, it) }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
