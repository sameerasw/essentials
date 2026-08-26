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
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.sameerasw.essentials.data.repository.SettingsRepository

object CalendarSyncManager {
    private const val TAG = "CalendarSyncManager"
    private const val SYNC_PATH = "/calendar_events"

    private var isSyncEnabled = false
    private var observer: ContentObserver? = null

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
        if (observer != null) return

        // Initial sync
        syncEvents(context)

        observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    Log.d(TAG, "Calendar content changed, syncing...")
                    syncEvents(context)
                }
            }

        try {
            context.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                observer!!,
            )
            Log.d(TAG, "Calendar observer registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register calendar observer", e)
        }
    }

    private fun stopSync(context: Context) {
        observer?.let {
            context.contentResolver.unregisterContentObserver(it)
            observer = null
            Log.d(TAG, "Calendar observer unregistered")
        }
    }

    private fun syncEvents(context: Context) {
        if (!isSyncEnabled) {
            Log.d(TAG, "Sync disabled, skipping")
            return
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CALENDAR,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "READ_CALENDAR permission not granted, skipping sync")
            return
        }

        // Trigger system sync and set VISIBLE/SYNC_EVENTS for calendars like Day app
        try {
            val repo = SettingsRepository(context)
            val selectedIds = repo.getCalendarSyncSelectedCalendars().mapNotNull { it.toLongOrNull() }.toSet()

            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.VISIBLE,
                CalendarContract.Calendars.SYNC_EVENTS,
            )
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null,
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val displayNameIndex = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountNameIndex = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val accountTypeIndex = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val visibleIndex = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)
                val syncIndex = cursor.getColumnIndex(CalendarContract.Calendars.SYNC_EVENTS)

                while (cursor.moveToNext()) {
                    val calId = cursor.getLong(idIndex)
                    val accountName = cursor.getString(accountNameIndex)
                    val accountType = cursor.getString(accountTypeIndex)
                    val currentVisible = if (visibleIndex != -1) cursor.getInt(visibleIndex) else 1
                    val currentSync = if (syncIndex != -1) cursor.getInt(syncIndex) else 1
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

    private fun queryUpcomingEvents(context: Context): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val nowMillis = System.currentTimeMillis()
        val queryStartMillis = nowMillis - (2 * 24 * 60 * 60 * 1000L) // 2 days past buffer for ongoing/all-day events
        val queryEndMillis = nowMillis + (10 * 24 * 60 * 60 * 1000L) // Next 10 days

        val repo = SettingsRepository(context)
        val selectedIds = repo.getCalendarSyncSelectedCalendars().mapNotNull { it.toLongOrNull() }.toSet()

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, queryStartMillis)
        android.content.ContentUris.appendId(builder, queryEndMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            "account_name",
            "organizer",
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.CALENDAR_ID,
        )

        try {
            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                CalendarContract.Instances.BEGIN + " ASC",
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val locIndex = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val beginIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIndex = it.getColumnIndex(CalendarContract.Instances.END)
                val allDayIndex = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val statusIndex = it.getColumnIndex(CalendarContract.Instances.SELF_ATTENDEE_STATUS)
                val calIdIndex = it.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)

                while (it.moveToNext()) {
                    val calId = it.getLong(calIdIndex)
                    if (selectedIds.isNotEmpty() && !selectedIds.contains(calId)) {
                        continue
                    }

                    if (statusIndex != -1 && it.getInt(statusIndex) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) {
                        continue
                    }

                    val rawTitle = it.getString(titleIndex)
                    if (rawTitle.isNullOrBlank()) {
                        continue
                    }

                    val idValue = it.getLong(idIndex)
                    val title = rawTitle.trim()
                    val begin = it.getLong(beginIndex)
                    val end = it.getLong(endIndex)
                    val isAllDay = it.getInt(allDayIndex) != 0
                    val location = it.getString(locIndex)

                    events.add(CalendarEvent(idValue, title, begin, end, isAllDay, location))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying calendar instances", e)
        }

        return events
            .filter { it.end >= nowMillis || it.begin >= nowMillis }
            .sortedBy { it.begin }
            .distinctBy { "${it.id}_${it.begin}" }
            .take(50)
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
