package com.sameerasw.essentials.services

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.RemoteViews
import android.widget.Toast
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService

class ScreenOffWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "WIDGET_CLICK") {
            if (isAccessibilityEnabled(context)) {
                val serviceIntent =
                    Intent(context, ScreenOffAccessibilityService::class.java).apply {
                        action = "LOCK_SCREEN"
                    }
                context.startService(serviceIntent)
            } else {
                Toast.makeText(context, "Missing permissions, Check the app", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains("com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService") == true
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.screen_off_widget)

        val intent = Intent(context, ScreenOffWidgetProvider::class.java).apply {
            action = "WIDGET_CLICK"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}