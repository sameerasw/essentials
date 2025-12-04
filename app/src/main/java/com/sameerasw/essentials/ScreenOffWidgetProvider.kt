package com.sameerasw.essentials

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class ScreenOffWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.screen_off_widget)

        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("widget_enabled", false)

        if (enabled) {
            val intent = Intent(context, ScreenOffAccessibilityService::class.java).apply {
                action = "LOCK_SCREEN"
            }
            val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
        }
        // If not enabled, do not set the pending intent, so tapping does nothing

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
