/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: CalendarModule.kt
 * Description: Runs calendar event based state automations.
 */

package com.sameerasw.essentials.services.automation.modules

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import com.sameerasw.essentials.data.repository.CalendarInstance
import com.sameerasw.essentials.data.repository.CalendarRepository
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.sameerasw.essentials.domain.diy.State as DIYState

class CalendarModule : AutomationModule {
    companion object {
        const val ID = "calendar_module"
        private const val LOOKAHEAD_MS = 2 * 24 * 60 * 60 * 1000L
        private const val MAX_WAIT_MS = 15 * 60 * 1000L
    }

    override val id: String = ID
    private var automations: List<Automation> = emptyList()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private val activeStateAutomations = mutableSetOf<String>()
    private var appContext: Context? = null

    private val recheck = Runnable { evaluate() }
    private val onCalendarChanged: () -> Unit = { evaluate() }

    override fun start(context: Context) {
        appContext = context.applicationContext
        CalendarRepository.addListener(context, onCalendarChanged)
    }

    override fun stop(context: Context) {
        handler.removeCallbacks(recheck)
        CalendarRepository.removeListener(onCalendarChanged)
        appContext = null
    }

    override fun updateAutomations(automations: List<Automation>) {
        this.automations = automations
        evaluate()
    }

    private fun evaluate() {
        val context = appContext ?: return
        handler.removeCallbacks(recheck)
        scope.launch {
            val now = System.currentTimeMillis()
            val states = automations.filter { it.type == Automation.Type.STATE && it.isEnabled && it.state is DIYState.CalendarEvent }
            val needsTypes = states.any { (it.state as DIYState.CalendarEvent).workingLocationOnly }
            val instances = CalendarRepository.queryInstancesOrNull(context, now - LOOKAHEAD_MS, now + LOOKAHEAD_MS, needsTypes)
            if (instances == null) {
                // Unreadable is not the same as "no events", so leave every state as it is and try again later
                Log.w(ID, "Calendar unavailable, keeping current states")
                handler.postDelayed(recheck, MAX_WAIT_MS)
                return@launch
            }
            var nextBoundary = now + MAX_WAIT_MS

            states.forEach { automation ->
                val state = automation.state as DIYState.CalendarEvent
                val matching =
                    if (state.workingLocationOnly) {
                        instances.filter { isWorkingLocation(it, state) }
                    } else {
                        instances.filter { matches(it, state) }
                    }
                val isActive =
                    if (state.workingLocationOnly) {
                        effectiveWorkingLocation(matching, now)?.let { titleMatches(it, state) } ?: false
                    } else {
                        matching.any { it.isOngoing(now) }
                    }
                matching.forEach { instance ->
                    if (instance.begin > now) nextBoundary = minOf(nextBoundary, instance.begin)
                    if (instance.end > now) nextBoundary = minOf(nextBoundary, instance.end)
                }

                val wasActive = activeStateAutomations.contains(automation.id)
                if (isActive && !wasActive) {
                    Log.d(ID, "State ${automation.id} became active")
                    activeStateAutomations.add(automation.id)
                    CombinedActionExecutor.executeAll(context, automation.entryActionList)
                } else if (!isActive && wasActive) {
                    Log.d(ID, "State ${automation.id} became inactive")
                    activeStateAutomations.remove(automation.id)
                    CombinedActionExecutor.executeAll(context, automation.exitActionList)
                }
            }

            handler.postDelayed(recheck, (nextBoundary - System.currentTimeMillis()).coerceAtLeast(1000L) + 500L)
        }
    }

    private fun matches(instance: CalendarInstance, state: DIYState.CalendarEvent): Boolean {
        if (state.calendarIds.isNotEmpty() && instance.calendarId !in state.calendarIds) return false
        if (!titleMatches(instance, state)) return false
        if (instance.allDay && !state.includeAllDay) return false
        if (instance.isDeclined && !state.includeDeclined) return false
        if (instance.selfStatus == CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE && !state.includeTentative) return false
        if (instance.availability == CalendarContract.Events.AVAILABILITY_FREE && !state.includeFree) return false
        return true
    }

    private fun titleMatches(instance: CalendarInstance, state: DIYState.CalendarEvent): Boolean =
        state.titleContains.isBlank() || instance.title.contains(state.titleContains.trim(), ignoreCase = true)

    // Working location entries are free and often all day, so they skip the usual event filters
    private fun isWorkingLocation(instance: CalendarInstance, state: DIYState.CalendarEvent): Boolean =
        instance.isWorkingLocation && (state.calendarIds.isEmpty() || instance.calendarId in state.calendarIds)

    // A timed working location overrides the all-day one for its span, the way Google Calendar shows it
    private fun effectiveWorkingLocation(instances: List<CalendarInstance>, now: Long): CalendarInstance? {
        val current = instances.filter { it.isOngoing(now) }
        return current.filterNot { it.allDay }.maxByOrNull { it.begin } ?: current.maxByOrNull { it.begin }
    }
}
