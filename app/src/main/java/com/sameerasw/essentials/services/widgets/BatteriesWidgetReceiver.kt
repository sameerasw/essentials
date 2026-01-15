package com.sameerasw.essentials.services.widgets

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.launch

class BatteriesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteriesWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_POWER_CONNECTED ||
            intent.action == Intent.ACTION_POWER_DISCONNECTED ||
            intent.action == Intent.ACTION_BATTERY_LOW ||
            intent.action == Intent.ACTION_BATTERY_OKAY) {
            
            // Trigger update
            val glanceAppWidgetManager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
            kotlinx.coroutines.MainScope().launch {
                 val glanceIds = glanceAppWidgetManager.getGlanceIds(BatteriesWidget::class.java)
                 glanceIds.forEach { glanceId ->
                     glanceAppWidget.update(context, glanceId)
                 }
            }
        }
    }
}
