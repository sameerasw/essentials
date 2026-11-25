/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities
 * File: CalendarEventUtil.kt
 * Description: Shared calendar-provider query for the next upcoming event.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.CalendarInstance
import com.sameerasw.essentials.data.repository.CalendarRepository
import java.util.Calendar

data class UpcomingCalendarEvent(
    val title: String,
    val startTimeMillis: Long,
    val location: String?,
    val eventId: Long = -1L,
    val endTimeMillis: Long = 0L,
    val allDay: Boolean = false,
    val calendarName: String? = null,
    val calendarColor: Int? = null,
    val description: String? = null,
    val calendarId: Long = -1L,
)

object CalendarEventUtil {
    fun formatRelativeTime(context: Context, eventTimeMillis: Long, now: Long): String {
        if (eventTimeMillis <= now) return context.getString(R.string.status_glance_calendar_event_now)
        val diffMillis = (eventTimeMillis - now).coerceAtLeast(0L)
        val minutes = (diffMillis / (60 * 1000L)).toInt()
        val hours = minutes / 60
        val days = hours / 24

        val timeStr = when {
            minutes < 1 -> "1m"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> {
                val remMins = minutes % 60
                if (remMins > 0) "${hours}h ${remMins}m" else "${hours}h"
            }
            else -> {
                val remHours = hours % 24
                if (remHours > 0) "${days}d ${remHours}h" else "${days}d"
            }
        }
        return context.getString(R.string.status_glance_calendar_event_in_time, timeStr)
    }

    fun formatRelativeTimeCompact(eventTimeMillis: Long, now: Long): String {
        if (eventTimeMillis <= now) return "Now"
        val diffMillis = (eventTimeMillis - now).coerceAtLeast(0L)
        val minutes = (diffMillis / (60 * 1000L)).toInt()
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "1m"
            minutes < 60 -> "${minutes}m"
            hours < 2 -> (minutes % 60).let { if (it > 0) "${hours}h ${it}m" else "${hours}h" }
            hours < 24 -> "${hours}h"
            else -> "${days}d"
        }
    }

    fun queryNextUpcomingEvent(context: Context, timeframe: String, selectedCalendarIds: Set<Long>, showAllDay: Boolean = false, includeOngoing: Boolean = false): UpcomingCalendarEvent? =
        queryUpcomingEvents(context, timeframe, selectedCalendarIds, showAllDay, includeOngoing, limit = 1).firstOrNull()

    fun queryUpcomingEvents(
        context: Context,
        timeframe: String,
        selectedCalendarIds: Set<Long>,
        showAllDay: Boolean = false,
        includeOngoing: Boolean = false,
        limit: Int = Int.MAX_VALUE,
    ): List<UpcomingCalendarEvent> {
        val now = System.currentTimeMillis()
        val maxTimeMillis: Long = when (timeframe) {
            "15m" -> now + (15 * 60 * 1000L)
            "30m" -> now + (30 * 60 * 1000L)
            "1h" -> now + (60 * 60 * 1000L)
            "2h" -> now + (2 * 60 * 60 * 1000L)
            "6h" -> now + (6 * 60 * 60 * 1000L)
            "24h" -> now + (24 * 60 * 60 * 1000L)
            "next" -> now + (30 * 24 * 60 * 60 * 1000L)
            else -> endOfDay(0)
        }

        return CalendarRepository
            .queryInstances(context, now - ONGOING_LOOKBACK_MS, maxTimeMillis)
            .asSequence()
            .filter { selectedCalendarIds.isEmpty() || it.calendarId in selectedCalendarIds }
            .filterNot { it.isDeclined }
            .filter { showAllDay || !it.allDay }
            .filter { it.title.isNotBlank() }
            .filter { (includeOngoing && it.begin < now && it.end > now) || it.begin in now..maxTimeMillis }
            .sortedBy { it.begin }
            .take(limit)
            .map { it.toUpcoming() }
            .toList()
    }

    fun queryBriefEvents(context: Context, selectedCalendarIds: Set<Long>, showAllDay: Boolean, limit: Int = 1): List<UpcomingCalendarEvent> {
        val now = System.currentTimeMillis()
        val endOfTomorrow = endOfDay(1)
        return CalendarRepository
            .queryInstances(context, now - ONGOING_LOOKBACK_MS, endOfTomorrow)
            .asSequence()
            .filter { selectedCalendarIds.isEmpty() || it.calendarId in selectedCalendarIds }
            .filterNot { it.isDeclined }
            .filter { showAllDay || !it.allDay }
            .filter { it.title.isNotBlank() && it.end > now && it.begin <= endOfTomorrow }
            .sortedBy { it.begin }
            .take(limit)
            .map { it.toUpcoming() }
            .toList()
    }

    private const val ONGOING_LOOKBACK_MS = 24 * 60 * 60 * 1000L

    private fun endOfDay(daysAhead: Int): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysAhead)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

    private fun CalendarInstance.toUpcoming() =
        UpcomingCalendarEvent(
            title = title,
            startTimeMillis = begin,
            location = location,
            eventId = eventId,
            endTimeMillis = end,
            allDay = allDay,
            calendarName = calendarName,
            calendarColor = color,
            description = description,
            calendarId = calendarId,
        )
}
