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
            val now = System.currentTimeMillis()
            val diff = now - date.time

            when {
                diff < 60000 -> context.getString(R.string.time_just_now)
                diff < 3600000 -> context.getString(R.string.time_min_ago, diff / 60000)
                diff < 86400000 -> context.getString(R.string.time_hour_ago, diff / 3600000)
                diff < 2592000000L -> context.getString(R.string.time_day_ago, diff / 86400000)
                diff < 31536000000L -> context.getString(
                    R.string.time_month_ago,
                    diff / 2592000000L
                )

                else -> context.getString(R.string.time_year_ago, diff / 31536000000L)
            }
        } catch (e: Exception) {
            githubDate
        }
    }
}
