/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: CalendarModule.kt
 * Description: Runs calendar event based state automations.
 */

package com.sameerasw.essentials.services.automation.modules

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
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

    private class Instance(
        val begin: Long,
        val end: Long,
        val allDay: Boolean,
        val status: Int,
        val availability: Int,
        val calendarId: Long,
    )

    override val id: String = ID
    private var automations: List<Automation> = emptyList()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private val activeStateAutomations = mutableSetOf<String>()
    private var appContext: Context? = null

    private val recheck = Runnable { evaluate() }

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            evaluate()
        }
    }

    override fun start(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        try {
            appCtx.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        } catch (_: Exception) {
        }
    }

    override fun stop(context: Context) {
        handler.removeCallbacks(recheck)
        try {
            (appContext ?: context.applicationContext).contentResolver.unregisterContentObserver(observer)
        } catch (_: Exception) {
        }
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
            val instances = queryInstances(context, now, now + LOOKAHEAD_MS)
            var nextBoundary = now + MAX_WAIT_MS

            automations
                .filter { it.type == Automation.Type.STATE && it.isEnabled }
                .forEach { automation ->
                    val state = automation.state as? DIYState.CalendarEvent ?: return@forEach
                    val matching = instances.filter { matches(it, state) }
                    val isActive = matching.any { now in it.begin until it.end }
                    matching.forEach { instance ->
                        if (instance.begin > now) nextBoundary = minOf(nextBoundary, instance.begin)
                        if (instance.end > now) nextBoundary = minOf(nextBoundary, instance.end)
                    }

                    val wasActive = activeStateAutomations.contains(automation.id)
                    if (isActive && !wasActive) {
                        Log.d(ID, "State ${automation.id} became active")
                        activeStateAutomations.add(automation.id)
                        automation.entryAction?.let { CombinedActionExecutor.execute(context, it) }
                    } else if (!isActive && wasActive) {
                        Log.d(ID, "State ${automation.id} became inactive")
                        activeStateAutomations.remove(automation.id)
                        automation.exitAction?.let { CombinedActionExecutor.execute(context, it) }
                    }
                }

            handler.postDelayed(recheck, (nextBoundary - System.currentTimeMillis()).coerceAtLeast(1000L) + 500L)
        }
    }

    private fun matches(instance: Instance, state: DIYState.CalendarEvent): Boolean {
        if (state.calendarIds.isNotEmpty() && instance.calendarId !in state.calendarIds) return false
        if (instance.allDay && !state.includeAllDay) return false
        if (instance.status == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED && !state.includeDeclined) return false
        if (instance.status == CalendarContract.Attendees.ATTENDEE_STATUS_TENTATIVE && !state.includeTentative) return false
        if (instance.availability == CalendarContract.Events.AVAILABILITY_FREE && !state.includeFree) return false
        return true
    }

    private fun queryInstances(context: Context, from: Long, to: Long): List<Instance> {
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from - LOOKAHEAD_MS)
            ContentUris.appendId(it, to)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.AVAILABILITY,
            CalendarContract.Instances.CALENDAR_ID,
        )
        val result = mutableListOf<Instance>()
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val instance = Instance(
                        begin = c.getLong(0),
                        end = c.getLong(1),
                        allDay = c.getInt(2) == 1,
                        status = c.getInt(3),
                        availability = c.getInt(4),
                        calendarId = c.getLong(5),
                    )
                    if (instance.end > from) result += instance
                }
            }
        } catch (e: Exception) {
            Log.e(ID, "Failed to query calendar instances", e)
        }
        return result
    }
}
