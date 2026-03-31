package com.sameerasw.essentials.utils

import android.content.Context
import com.sameerasw.essentials.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object TimeUtil {
    fun formatRelativeDate(githubDate: String, context: Context): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(githubDate) ?: return githubDate
            formatRelativeDate(date.time, context)
        } catch (e: Exception) {
            githubDate
        }
    }

    fun formatRelativeDate(timestamp: Long, context: Context): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val days = diff / 86400000L
        return when {
            diff < 60000L -> context.getString(R.string.time_just_now)
            diff < 3600000L -> context.getString(R.string.time_min_ago, (diff / 60000).toInt())
            diff < 86400000L && isSameDay(now, timestamp) -> {
                if (diff < 3600000L * 24) {
                    context.getString(R.string.time_hour_ago, (diff / 3600000L).toInt())
                } else {
                    context.getString(R.string.today)
                }
            }
            days == 1L || (days == 0L && !isSameDay(now, timestamp)) -> context.getString(R.string.yesterday)
            days < 7L -> context.getString(R.string.time_days_ago, days.toInt())
            days < 30L -> context.getString(R.string.time_weeks_ago, (days / 7).toInt())
            days < 365L -> context.getString(R.string.time_months_ago, (days / 30).toInt())
            else -> context.getString(R.string.time_year_ago, (days / 365).toInt())
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
}
