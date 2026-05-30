package com.sameerasw.essentials.services.widgets

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.launch

class FavoritesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FavoritesWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action
        if (action == Intent.ACTION_CONFIGURATION_CHANGED ||
            action == android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            action == "com.sameerasw.essentials.action.FAVORITES_WIDGET_UPDATE"
        ) {
            kotlinx.coroutines.MainScope().launch {
                try {
                    val glanceAppWidgetManager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
                    if (action == Intent.ACTION_CONFIGURATION_CHANGED) {
                        kotlinx.coroutines.delay(500)
                    }

                    val glanceIds = glanceAppWidgetManager.getGlanceIds(FavoritesWidget::class.java)
                    glanceIds.forEach { glanceId ->
                        glanceAppWidget.update(context, glanceId)
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                        "FavoritesWidget",
                        "Error updating favorites widget on broadcast",
                        e
                    )
                }
            }
        }
    }
}
