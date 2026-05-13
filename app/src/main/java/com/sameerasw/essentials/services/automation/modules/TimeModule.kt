package com.sameerasw.essentials.services.automation.modules

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import com.sameerasw.essentials.services.automation.receivers.TimeAutomationReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import com.sameerasw.essentials.domain.diy.State as DIYState

class TimeModule : AutomationModule {
    companion object {
        const val ID = "time_module"
    }

    override val id: String = ID
    private var automations: List<Automation> = emptyList()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var appContext: Context? = null

    override fun start(context: Context) {
        appContext = context.applicationContext
        // We'll run initial check only after we get automations
    }

    override fun stop(context: Context) {
        appContext?.let { cancelAllAlarms(it) }
        appContext = null
    }

    override fun updateAutomations(automations: List<Automation>) {
        this.automations = automations
        appContext?.let {
            checkCurrentStates(it)
            scheduleAllAlarms(it)
        }
    }

    private fun scheduleAllAlarms(context: Context) {
        cancelAllAlarms(context)

        automations.forEach { automation ->
            when (automation.type) {
                Automation.Type.TRIGGER -> {
                    (automation.trigger as? Trigger.Schedule)?.let { schedule ->
                        scheduleAlarm(
                            context,
                            automation.id,
                            schedule.hour,
                            schedule.minute,
                            schedule.days,
                            true
                        )
                    }
                }

                Automation.Type.STATE -> {
                    (automation.state as? DIYState.TimePeriod)?.let { period ->
                        scheduleAlarm(
                            context,
                            automation.id,
                            period.startHour,
                            period.startMinute,
                            period.days,
                            true
                        )
                        scheduleAlarm(
                            context,
                            automation.id,
                            period.endHour,
                            period.endMinute,
                            period.days,
                            false
                        )
                    }
                }

                else -> {}
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        id: String,
        hour: Int,
        minute: Int,
        days: Set<Int>,
        isEntry: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimeAutomationReceiver::class.java).apply {
            action = TimeAutomationReceiver.ACTION_TRIGGER
            putExtra(TimeAutomationReceiver.EXTRA_AUTOMATION_ID, id)
            putExtra(TimeAutomationReceiver.EXTRA_IS_ENTRY, isEntry)
        }

        val requestCode = (id + isEntry.toString()).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = calculateNextOccurrence(hour, minute, days)
        Log.d(
            ID,
            "Scheduling alarm for automation $id (entry=$isEntry) at ${calendar.time}"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        // Fallback to inexact
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        automations.forEach { automation ->
            val intent = Intent(context, TimeAutomationReceiver::class.java).apply {
                action = TimeAutomationReceiver.ACTION_TRIGGER
            }

            val rc1 = (automation.id + "true").hashCode()
            PendingIntent.getBroadcast(
                context,
                rc1,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let {
                alarmManager.cancel(it)
                it.cancel()
            }

            val rc2 = (automation.id + "false").hashCode()
            PendingIntent.getBroadcast(
                context,
                rc2,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    private fun calculateNextOccurrence(hour: Int, minute: Int, days: Set<Int>): Calendar {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (days.isNotEmpty()) {
            while (!days.contains(target.get(Calendar.DAY_OF_WEEK))) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return target
    }

    private val activeStateAutomations = mutableSetOf<String>()

    private fun checkCurrentStates(context: Context) {
        scope.launch {
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)
            val currentMinute = now.get(Calendar.MINUTE)
            val currentDay = now.get(Calendar.DAY_OF_WEEK)

            Log.d(
                ID,
                "Checking current states at $currentHour:$currentMinute on day $currentDay"
            )

            automations.filter { it.type == Automation.Type.STATE && it.isEnabled }
                .forEach { automation ->
                    (automation.state as? DIYState.TimePeriod)?.let { period ->
                        if (period.days.isEmpty() || period.days.contains(currentDay)) {
                            val startTime = period.startHour * 60 + period.startMinute
                            val endTime = period.endHour * 60 + period.endMinute
                            val currentTime = currentHour * 60 + currentMinute

                            val isActive = if (startTime < endTime) {
                                currentTime in startTime until endTime
                            } else {
                                currentTime >= startTime || currentTime < endTime
                            }

                            val wasActive = activeStateAutomations.contains(automation.id)

                            if (isActive && !wasActive) {
                                Log.d(
                                    ID,
                                    "State ${automation.id} became active. Executing entry actions."
                                )
                                activeStateAutomations.add(automation.id)
                                automation.entryAction?.let {
                                    CombinedActionExecutor.execute(
                                        context,
                                        it
                                    )
                                }
                            } else if (!isActive && wasActive) {
                                Log.d(
                                    ID,
                                    "State ${automation.id} became inactive. Executing exit actions."
                                )
                                activeStateAutomations.remove(automation.id)
                                automation.exitAction?.let {
                                    CombinedActionExecutor.execute(
                                        context,
                                        it
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }
}
