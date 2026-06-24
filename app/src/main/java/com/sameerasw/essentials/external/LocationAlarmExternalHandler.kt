package com.sameerasw.essentials.external

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import com.sameerasw.essentials.data.repository.LocationReachedRepository
import com.sameerasw.essentials.services.LocationReachedService

class LocationAlarmExternalHandler : ExternalHandler {
    override val path: String = "location_alarm"

    override fun onQuery(context: Context, remainingPath: String, extras: Bundle?): Cursor? {
        val repository = LocationReachedRepository(context)
        if (remainingPath == "list") {
            val alarms = repository.getAlarms()
            val activeId = repository.getActiveAlarmId()

            val cursor = MatrixCursor(arrayOf(
                "id",
                "name",
                "latitude",
                "longitude",
                "radius",
                "isEnabled",
                "isPaused",
                "lastTravelled",
                "isActive",
                "iconResName"
            ))

            for (alarm in alarms) {
                cursor.addRow(arrayOf<Any?>(
                    alarm.id,
                    alarm.name,
                    alarm.latitude,
                    alarm.longitude,
                    alarm.radius,
                    if (alarm.isEnabled) 1 else 0,
                    if (alarm.isPaused) 1 else 0,
                    alarm.lastTravelled ?: 0L,
                    if (alarm.id == activeId) 1 else 0,
                    alarm.iconResName
                ))
            }
            return cursor
        }
        return null
    }

    override fun onUpdate(context: Context, remainingPath: String, value: String?, extras: Bundle?): Boolean {
        return false
    }

    override fun onAction(context: Context, remainingPath: String, action: String?, extras: Bundle?): Bundle? {
        val repository = LocationReachedRepository(context)
        val targetAction = action ?: remainingPath
        when (targetAction) {
            "start" -> {
                val alarmId = extras?.getString("id") ?: return null
                repository.saveActiveAlarmId(alarmId)
                LocationReachedService.start(context)
                return Bundle().apply { putBoolean("success", true) }
            }
            "stop" -> {
                repository.saveActiveAlarmId(null)
                val intent = Intent(context, LocationReachedService::class.java).apply {
                    this.action = LocationReachedService.ACTION_STOP
                }
                context.startService(intent)
                return Bundle().apply { putBoolean("success", true) }
            }
        }
        return null
    }
}
