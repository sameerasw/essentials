package com.sameerasw.essentials.services

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.sameerasw.essentials.data.repository.SettingsRepository
import java.util.Calendar

object CalendarSyncManager {
    private const val TAG = "CalendarSyncManager"
    private const val SYNC_PATH = "/calendar_events"
    
    private var isSyncEnabled = false
    private var observer: ContentObserver? = null

    fun forceSync(context: Context) {
        Log.d(TAG, "forceSync: Manually triggering sync")
        syncEvents(context)
    }

    fun init(context: Context) {
        val repo = SettingsRepository(context)
        isSyncEnabled = repo.getBoolean(SettingsRepository.KEY_CALENDAR_SYNC_ENABLED, false)
        
        if (isSyncEnabled) {
            startSync(context)
        }
        
        // Listen for preference changes to start/stop sync
        repo.registerOnSharedPreferenceChangeListener { _, key ->
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
    }

    private fun startSync(context: Context) {
        if (observer != null) return
        
        // Initial sync
        syncEvents(context)
        
        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                Log.d(TAG, "Calendar content changed, syncing...")
                syncEvents(context)
            }
        }
        
        try {
            context.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                observer!!
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
        
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CALENDAR permission not granted, skipping sync")
            return
        }
        
        Log.d(TAG, "Starting sync...")
        val events = queryUpcomingEvents(context)
        
        // Get Material You theme color
        var themeColor: Int? = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            themeColor = context.getColor(android.R.color.system_accent1_600)
        }

        Log.d(TAG, "Found ${events.size} upcoming events, themeColor=$themeColor")
        sendToWearable(context, events, themeColor)
    }

    private fun queryUpcomingEvents(context: Context): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val startTime = System.currentTimeMillis()
        val endTime = startTime + 24 * 60 * 60 * 1000 * 7 // Next 7 days
        
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION
        )
        
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, startTime)
        android.content.ContentUris.appendId(builder, endTime)
        
        Log.d(TAG, "queryUpcomingEvents: range=$startTime to $endTime, URI=${builder.build()}")
        
        val repo = SettingsRepository(context)
        val selectedIds = repo.getCalendarSyncSelectedCalendars()
        
        val selection = if (selectedIds.isNotEmpty()) {
            "${CalendarContract.Instances.CALENDAR_ID} IN (${selectedIds.joinToString(",")})"
        } else null

        val cursor = context.contentResolver.query(
            builder.build(),
            projection,
            selection,
            null,
            CalendarContract.Instances.BEGIN + " ASC"
        )
        
        Log.d(TAG, "queryUpcomingEvents: selection=$selection, cursorCount=${cursor?.count ?: 0}")
        
        cursor?.use {
            while (it.moveToNext()) {
                val idValue = it.getLong(0)
                val title = it.getString(1)
                val begin = it.getLong(2)
                val end = it.getLong(3)
                val allDay = it.getInt(4) != 0
                val location = it.getString(5)
                
                events.add(CalendarEvent(idValue, title, begin, end, allDay, location))
                if (events.size >= 10) break // Limit to top 10 events for Wear OS
            }
        }
        
        return events
    }

    private fun sendToWearable(context: Context, events: List<CalendarEvent>, themeColor: Int?) {
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
        themeColor?.let { dataMap.putInt("theme_color", it) }
        dataMap.putLong("timestamp", System.currentTimeMillis())
        
        val putDataReq = putDataMapReq.asPutDataRequest()
        putDataReq.setUrgent()
        
        Wearable.getDataClient(context).putDataItem(putDataReq)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully synced ${events.size} events to wearable")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to sync events to wearable", e)
            }
    }

    data class CalendarEvent(
        val id: Long,
        val title: String?,
        val begin: Long,
        val end: Long,
        val allDay: Boolean,
        val location: String?
    )
}
