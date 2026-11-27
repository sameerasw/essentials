/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Data Layer
 * File: CalendarRepository.kt
 * Description: The one place the app reads the calendar from and hears about calendar changes.
 */

package com.sameerasw.essentials.data.repository

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import java.util.TimeZone
import java.util.concurrent.CopyOnWriteArraySet

data class CalendarInstance(
    val eventId: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val rawBegin: Long,
    val rawEnd: Long,
    val allDay: Boolean,
    val selfStatus: Int,
    val availability: Int,
    val calendarId: Long,
    val calendarName: String?,
    val color: Int?,
    val location: String?,
    val description: String?,
    val eventType: String? = null,
) {
    val isDeclined: Boolean get() = selfStatus == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
    val isWorkingLocation: Boolean get() = eventType == CalendarRepository.EVENT_TYPE_WORKING_LOCATION

    fun isOngoing(now: Long): Boolean = now in begin until end
}

data class CalendarInfo(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String?,
    val color: Int?,
    val visible: Boolean,
    val syncEvents: Boolean,
)

object CalendarRepository {
    private const val TAG = "CalendarRepository"

    private val htmlTagRegex = Regex("</?[a-zA-Z][^>]*>|&(nbsp|amp|lt|gt|quot|#\\d+);")
    private val htmlHiddenBlockRegex =
        Regex("<(head|style|script|title)\\b[^>]*>.*?</\\1\\s*>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val htmlCommentRegex = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val blankLinesRegex = Regex("\\n[ \\t\\u00A0]*(\\n[ \\t\\u00A0]*)+")

    private fun plainDescription(raw: String): String {
        if (!htmlTagRegex.containsMatchIn(raw)) return raw.trim()
        val cleaned = raw.replace(htmlCommentRegex, "").replace(htmlHiddenBlockRegex, "")
        return HtmlCompat
            .fromHtml(cleaned, HtmlCompat.FROM_HTML_MODE_COMPACT)
            .toString()
            .replace('\uFFFC', ' ')
            .lines()
            .joinToString("\n") { it.replace('\u00A0', ' ').trimEnd() }
            .replace(blankLinesRegex, "\n\n")
            .trim()
    }

    private const val EVENT_TYPE_PROPERTY = "shared:calendarProviderEventType"
    const val EVENT_TYPE_WORKING_LOCATION = "WORKING_LOCATION"
    private const val CHANGE_DEBOUNCE_MS = 300L
    private const val WORKING_LOCATION_WINDOW_MS = 30L * 24 * 60 * 60 * 1000

    private val handler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<() -> Unit>()
    private var appContext: Context? = null
    private var observer: ContentObserver? = null

    private val dispatch = Runnable { listeners.forEach { runCatching(it).onFailure { e -> Log.e(TAG, "Calendar listener failed", e) } } }

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    // Null means the calendar couldn't be read (no permission or a provider error), which callers must not treat as "no events"
    fun queryInstancesOrNull(
        context: Context,
        from: Long,
        to: Long,
        withEventTypes: Boolean = false,
    ): List<CalendarInstance>? {
        if (!hasPermission(context)) return null
        val uri =
            CalendarContract.Instances.CONTENT_URI
                .buildUpon()
                .also {
                    ContentUris.appendId(it, from)
                    ContentUris.appendId(it, to)
                }.build()
        val projection =
            arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.SELF_ATTENDEE_STATUS,
                CalendarContract.Instances.AVAILABILITY,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
                CalendarContract.Instances.DISPLAY_COLOR,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.DESCRIPTION,
            )
        val instances: List<CalendarInstance> =
            try {
                context.contentResolver
                    .query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")
                    ?.use { c ->
                        buildList<CalendarInstance> {
                            while (c.moveToNext()) {
                                val allDay = c.getInt(4) != 0
                                val rawBegin = c.getLong(2)
                                val rawEnd = c.getLong(3)
                                add(
                                    CalendarInstance(
                                        eventId = c.getLong(0),
                                        title = c.getString(1)?.trim().orEmpty(),
                                        begin = if (allDay) utcDayToLocal(rawBegin) else rawBegin,
                                        end = if (allDay) utcDayToLocal(rawEnd) else rawEnd,
                                        rawBegin = rawBegin,
                                        rawEnd = rawEnd,
                                        allDay = allDay,
                                        selfStatus = c.getInt(5),
                                        availability = c.getInt(6),
                                        calendarId = c.getLong(7),
                                        calendarName = c.getString(8)?.trim()?.takeIf { it.isNotBlank() },
                                        color = if (c.isNull(9)) null else c.getInt(9),
                                        location = c.getString(10)?.trim()?.takeIf { it.isNotBlank() },
                                        description = c.getString(11)?.let(::plainDescription)?.takeIf { it.isNotBlank() },
                                    ),
                                )
                            }
                        }
                    } ?: return null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query calendar instances", e)
                return null
            }
        if (!withEventTypes || instances.isEmpty()) return instances
        val types = queryEventTypes(context, instances.map { it.eventId }.toSet()) ?: return null
        return instances.map { it.copy(eventType = types[it.eventId]) }
    }

    fun queryInstances(
        context: Context,
        from: Long,
        to: Long,
        withEventTypes: Boolean = false,
    ): List<CalendarInstance> = queryInstancesOrNull(context, from, to, withEventTypes).orEmpty()

    fun queryCalendars(context: Context): List<CalendarInfo> {
        if (!hasPermission(context)) return emptyList()
        val projection =
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.CALENDAR_COLOR,
                CalendarContract.Calendars.VISIBLE,
                CalendarContract.Calendars.SYNC_EVENTS,
            )
        return try {
            context.contentResolver
                .query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)
                ?.use { c ->
                    buildList<CalendarInfo> {
                        while (c.moveToNext()) {
                            add(
                                CalendarInfo(
                                    id = c.getLong(0),
                                    name = c.getString(1).orEmpty(),
                                    accountName = c.getString(2).orEmpty(),
                                    accountType = c.getString(3),
                                    color = if (c.isNull(4)) null else c.getInt(4),
                                    visible = c.isNull(5) || c.getInt(5) != 0,
                                    syncEvents = c.isNull(6) || c.getInt(6) != 0,
                                ),
                            )
                        }
                    }
                }.orEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query calendars", e)
            emptyList()
        }
    }

    fun workingLocationNames(context: Context): List<String> {
        val now = System.currentTimeMillis()
        return queryInstances(context, now - WORKING_LOCATION_WINDOW_MS, now + WORKING_LOCATION_WINDOW_MS, withEventTypes = true)
            .filter { it.isWorkingLocation && it.title.isNotBlank() }
            .map { it.title }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    fun addListener(
        context: Context,
        listener: () -> Unit,
    ) {
        appContext = context.applicationContext
        listeners += listener
        ensureObserver()
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
        if (listeners.isEmpty()) releaseObserver()
    }

    // Registering an observer needs the permission, so a grant made later has to start it and wake everyone up
    fun onPermissionChanged(context: Context) {
        appContext = context.applicationContext
        if (!hasPermission(context)) {
            releaseObserver()
            return
        }
        if (observer == null && ensureObserver()) notifyChanged()
    }

    fun notifyChanged() {
        handler.removeCallbacks(dispatch)
        handler.postDelayed(dispatch, CHANGE_DEBOUNCE_MS)
    }

    @Synchronized
    private fun ensureObserver(): Boolean {
        if (observer != null || listeners.isEmpty()) return false
        val context = appContext ?: return false
        if (!hasPermission(context)) return false
        val contentObserver =
            object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) = notifyChanged()
            }
        return try {
            context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, contentObserver)
            observer = contentObserver
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to watch the calendar", e)
            false
        }
    }

    @Synchronized
    private fun releaseObserver() {
        val current = observer ?: return
        runCatching { appContext?.contentResolver?.unregisterContentObserver(current) }
        observer = null
    }

    private fun queryEventTypes(
        context: Context,
        eventIds: Set<Long>,
    ): Map<Long, String>? =
        try {
            buildMap<Long, String> {
                context.contentResolver
                    .query(
                        CalendarContract.ExtendedProperties.CONTENT_URI,
                        arrayOf(CalendarContract.ExtendedProperties.EVENT_ID, CalendarContract.ExtendedProperties.VALUE),
                        "${CalendarContract.ExtendedProperties.NAME} = ? AND ${CalendarContract.ExtendedProperties.EVENT_ID} IN (${eventIds.joinToString(",")})",
                        arrayOf(EVENT_TYPE_PROPERTY),
                        null,
                    )?.use { c ->
                        while (c.moveToNext()) put(c.getLong(0), c.getString(1).orEmpty())
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query calendar event types", e)
            null
        }

    // All-day instances are stored as UTC midnights, so shift them onto the local day
    private fun utcDayToLocal(utcMillis: Long): Long = utcMillis - TimeZone.getDefault().getOffset(utcMillis)
}
