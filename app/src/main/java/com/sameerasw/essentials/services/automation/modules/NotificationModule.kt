package com.sameerasw.essentials.services.automation.modules

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.domain.model.ActiveNotificationAlert
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class NotificationModule : AutomationModule {
    companion object {
        const val ID = "notification_module"
    }

    override val id: String = ID

    @Volatile
    private var automations: List<Automation> = emptyList()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var appContext: Context? = null
    private var dndOn = false

    private val alertListener =
        object : NotificationListener.NotificationAlertListener {
            override fun onNotificationAlertPosted(alert: ActiveNotificationAlert) {
                val context = appContext ?: return
                run(context) { trigger ->
                    trigger is Trigger.NotificationReceived && trigger.matches(alert.packageName, alert.title, alert.text)
                }
            }

            override fun onNotificationAlertRemoved(key: String) {}
        }

    private val dndReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                // The filter also changes between DND levels (priority, alarms, total silence) only on/off counts
                val on = isDndOn(context)
                if (on == dndOn) return
                dndOn = on
                val fired = if (on) Trigger.DndOn else Trigger.DndOff
                run(context) { it == fired }
            }
        }

    override fun start(context: Context) {
        appContext = context.applicationContext
        dndOn = isDndOn(context)
        context.registerReceiver(dndReceiver, IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED))
        NotificationListener.addNotificationAlertListener(alertListener)
    }

    override fun stop(context: Context) {
        NotificationListener.removeNotificationAlertListener(alertListener)
        try {
            context.unregisterReceiver(dndReceiver)
        } catch (e: Exception) { }
        scope.coroutineContext.cancelChildren()
    }

    override fun updateAutomations(automations: List<Automation>) {
        this.automations = automations
    }

    private fun run(
        context: Context,
        matches: (Trigger) -> Boolean,
    ) {
        scope.launch {
            automations
                .filter { it.type == Automation.Type.TRIGGER && it.trigger?.let(matches) == true }
                .forEach { automation ->
                    CombinedActionExecutor.executeAll(context, automation.actionList)
                }
        }
    }

    private fun isDndOn(context: Context): Boolean {
        val filter = context.getSystemService(NotificationManager::class.java).currentInterruptionFilter
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }
}
