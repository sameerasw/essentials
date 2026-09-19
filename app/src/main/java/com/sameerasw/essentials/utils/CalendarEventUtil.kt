/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities
 * File: CalendarEventUtil.kt
 * Description: Shared calendar-provider query for the next upcoming event.
 */

package com.sameerasw.essentials.utils

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.R
import java.util.Calendar

data class UpcomingCalendarEvent(
    val title: String,
    val startTimeMillis: Long,
    val location: String?,
)

object CalendarEventUtil {
    fun formatRelativeTime(context: Context, eventTimeMillis: Long, now: Long): String {
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
        val diffMillis = (eventTimeMillis - now).coerceAtLeast(0L)
        val minutes = (diffMillis / (60 * 1000L)).toInt()
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "1m"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            else -> "${days}d"
        }
    }

    fun queryNextUpcomingEvent(context: Context, timeframe: String, selectedCalendarIds: Set<Long>, showAllDay: Boolean = false): UpcomingCalendarEvent? {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return null
        }

        val now = System.currentTimeMillis()
        val maxTimeMillis: Long = when (timeframe) {
            "15m" -> now + (15 * 60 * 1000L)
            "30m" -> now + (30 * 60 * 1000L)
            "1h" -> now + (60 * 60 * 1000L)
            "2h" -> now + (2 * 60 * 60 * 1000L)
            "6h" -> now + (6 * 60 * 60 * 1000L)
            "24h" -> now + (24 * 60 * 60 * 1000L)
            "next" -> now + (30 * 24 * 60 * 60 * 1000L)
            else -> Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, maxTimeMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.EVENT_LOCATION,
        )

        return try {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                val titleIndex = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIndex = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val statusIndex = cursor.getColumnIndex(CalendarContract.Instances.SELF_ATTENDEE_STATUS)
                val calIdIndex = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
                val locationIndex = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val allDayIndex = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)

                while (cursor.moveToNext()) {
                    val calId = cursor.getLong(calIdIndex)
                    if (selectedCalendarIds.isNotEmpty() && !selectedCalendarIds.contains(calId)) continue
                    if (statusIndex != -1 && cursor.getInt(statusIndex) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) continue
                    if (!showAllDay && allDayIndex != -1 && cursor.getInt(allDayIndex) != 0) continue

                    val rawTitle = cursor.getString(titleIndex)
                    if (rawTitle.isNullOrBlank()) continue

                    val begin = cursor.getLong(beginIndex)
                    if (begin in now..maxTimeMillis) {
                        val location = if (locationIndex != -1) cursor.getString(locationIndex)?.trim()?.takeIf { it.isNotBlank() } else null
                        return@use UpcomingCalendarEvent(rawTitle.trim(), begin, location)
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
