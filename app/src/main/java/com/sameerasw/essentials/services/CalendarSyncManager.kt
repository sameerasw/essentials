/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: CalendarSyncManager.kt
 * Description: Background service component for CalendarSyncManager.kt.
 */

package com.sameerasw.essentials.services

import android.content.Context
import android.content.SharedPreferences
import android.provider.CalendarContract
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.sameerasw.essentials.data.repository.CalendarRepository
import com.sameerasw.essentials.data.repository.SettingsRepository

object CalendarSyncManager {
    private const val TAG = "CalendarSyncManager"
    private const val SYNC_PATH = "/calendar_events"
    private const val PAST_BUFFER_MS = 2 * 24 * 60 * 60 * 1000L
    private const val FUTURE_WINDOW_MS = 10 * 24 * 60 * 60 * 1000L
    private const val MAX_EVENTS = 50

    private var isSyncEnabled = false
    private var listener: (() -> Unit)? = null

    fun forceSync(context: Context) {
        val repo = SettingsRepository(context)
        isSyncEnabled = repo.getBoolean(SettingsRepository.KEY_CALENDAR_SYNC_ENABLED, false)
        Log.d(TAG, "forceSync: Manually triggering sync, isSyncEnabled=$isSyncEnabled")
        syncEvents(context)
    }

    fun init(context: Context) {
        val repo = SettingsRepository(context)
        isSyncEnabled = repo.getBoolean(SettingsRepository.KEY_CALENDAR_SYNC_ENABLED, false)

        if (isSyncEnabled) {
            startSync(context)
        }

        // Listen for preference changes to start/stop sync
        repo.registerOnSharedPreferenceChangeListener(
            object :
                SharedPreferences.OnSharedPreferenceChangeListener {
                override fun onSharedPreferenceChanged(
                    sharedPreferences: SharedPreferences?,
                    key: String?,
                ) {
                    if (key == SettingsRepository.KEY_CALENDAR_SYNC_ENABLED) {
                        val enabled = repo.getBoolean(key, false)
                        if (enabled != isSyncEnabled) {
                            isSyncEnabled = enabled
                            if (isSyncEnabled) {
                                startSync(context)
                                forceSync(context)
                            } else {
                                stopSync(context)
                            }
                        }
                    }
                }
            },
        )
    }

    private fun startSync(context: Context) {
        if (listener != null) return
        syncEvents(context)
        val onChanged: () -> Unit = {
            Log.d(TAG, "Calendar content changed, syncing...")
            syncEvents(context)
        }
        listener = onChanged
        CalendarRepository.addListener(context, onChanged)
    }

    private fun stopSync(context: Context) {
        listener?.let(CalendarRepository::removeListener)
        listener = null
    }

    private val syncExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "calendar-sync") }

    private fun syncEvents(context: Context) {
        syncExecutor.execute { syncEventsBlocking(context) }
    }

    private fun syncEventsBlocking(context: Context) {
        if (!isSyncEnabled) {
            Log.d(TAG, "Sync disabled, skipping")
            return
        }

        if (!CalendarRepository.hasPermission(context)) {
            Log.w(TAG, "READ_CALENDAR permission not granted, skipping sync")
            return
        }

        // Trigger system sync and set VISIBLE/SYNC_EVENTS for calendars like Day app
        try {
            val repo = SettingsRepository(context)
            val selectedIds = repo.getCalendarSyncSelectedCalendars().mapNotNull { it.toLongOrNull() }.toSet()

            CalendarRepository.queryCalendars(context).forEach { calendar ->
                val calId = calendar.id
                val accountName = calendar.accountName
                val accountType = calendar.accountType
                val currentVisible = if (calendar.visible) 1 else 0
                val currentSync = if (calendar.syncEvents) 1 else 0
                val isEnabled = selectedIds.isEmpty() || selectedIds.contains(calId)
                val targetValue = if (isEnabled) 1 else 0

                if (currentVisible != targetValue || currentSync != targetValue) {
                    val hasWritePermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.WRITE_CALENDAR,
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (hasWritePermission) {
                        try {
                            val values = android.content.ContentValues().apply {
                                put(CalendarContract.Calendars.VISIBLE, targetValue)
                                put(CalendarContract.Calendars.SYNC_EVENTS, targetValue)
                            }

                            var rows = 0
                            if (!accountName.isNullOrEmpty() && !accountType.isNullOrEmpty()) {
                                val syncAdapterUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
                                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
                                    .build()
                                rows = context.contentResolver.update(
                                    syncAdapterUri,
                                    values,
                                    "${CalendarContract.Calendars._ID} = ?",
                                    arrayOf(calId.toString()),
                                )
                            }

                            if (rows == 0) {
                                context.contentResolver.update(
                                    android.content.ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calId),
                                    values,
                                    null,
                                    null,
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not update calendar $calId visibility: ${e.message}")
                        }
                    }
                }

                if (isEnabled && !accountName.isNullOrEmpty() && !accountType.isNullOrEmpty()) {
                    try {
                        val account = android.accounts.Account(accountName, accountType)
                        android.content.ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1)
                        android.content.ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true)
                        val extras = android.os.Bundle().apply {
                            putBoolean(android.content.ContentResolver.SYNC_EXTRAS_MANUAL, true)
                            putBoolean(android.content.ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                        }
                        android.content.ContentResolver.requestSync(account, CalendarContract.AUTHORITY, extras)
                    } catch (_: Exception) { }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring calendars visibility", e)
        }

        Log.d(TAG, "Starting sync...")
        val events = queryUpcomingEvents(context)

        // Get Material You theme colors
        var primaryColor: Int? = null
        var secondaryColor: Int? = null
        var tertiaryColor: Int? = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            primaryColor = context.getColor(android.R.color.system_accent1_600)
            secondaryColor = context.getColor(android.R.color.system_accent2_600)
            tertiaryColor = context.getColor(android.R.color.system_accent3_600)
        }

        Log.d(
            TAG,
            "Found ${events.size} upcoming events across all calendars, colors: P=$primaryColor, S=$secondaryColor, T=$tertiaryColor",
        )
        sendToWearable(context, events, primaryColor, secondaryColor, tertiaryColor)
    }

    // The watch expects the provider's own all-day convention, so this keeps the raw times
    private fun queryUpcomingEvents(context: Context): List<CalendarEvent> {
        val nowMillis = System.currentTimeMillis()
        val selectedIds = SettingsRepository(context).getCalendarSyncSelectedCalendars().mapNotNull { it.toLongOrNull() }.toSet()
        return CalendarRepository
            .queryInstances(context, nowMillis - PAST_BUFFER_MS, nowMillis + FUTURE_WINDOW_MS)
            .asSequence()
            .filter { selectedIds.isEmpty() || it.calendarId in selectedIds }
            .filterNot { it.isDeclined }
            .filter { it.title.isNotBlank() }
            .map { CalendarEvent(it.eventId, it.title, it.rawBegin, it.rawEnd, it.allDay, it.location) }
            .filter { it.end >= nowMillis || it.begin >= nowMillis }
            .sortedBy { it.begin }
            .distinctBy { "${it.id}_${it.begin}" }
            .take(MAX_EVENTS)
            .toList()
    }

    private fun sendToWearable(
        context: Context,
        events: List<CalendarEvent>,
        primaryColor: Int?,
        secondaryColor: Int?,
        tertiaryColor: Int?,
    ) {
        val putDataMapReq = PutDataMapRequest.create(SYNC_PATH)
        val dataMap = putDataMapReq.dataMap

        val eventList = ArrayList<DataMap>()
        for (event in events) {
            val map = DataMap()
            map.putLong("id", event.id)
            map.putString("title", event.title ?: "No Title")
            map.putLong("begin", event.begin)
            map.putLong("end", event.end)
            map.putBoolean("allDay", event.allDay)
            map.putString("location", event.location ?: "")
            eventList.add(map)
        }

        dataMap.putDataMapArrayList("events", eventList)
        primaryColor?.let { dataMap.putInt("theme_primary_color", it) }
        secondaryColor?.let { dataMap.putInt("theme_secondary_color", it) }
        tertiaryColor?.let { dataMap.putInt("theme_tertiary_color", it) }
        dataMap.putLong("timestamp", System.currentTimeMillis())

        val putDataReq = putDataMapReq.asPutDataRequest()
        putDataReq.setUrgent()

        Wearable
            .getDataClient(context)
            .putDataItem(putDataReq)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully synced ${events.size} events to wearable")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync events to wearable", e)
            }
    }

    data class CalendarEvent(
        val id: Long,
        val title: String?,
        val begin: Long,
        val end: Long,
        val allDay: Boolean,
        val location: String?,
    )
}
