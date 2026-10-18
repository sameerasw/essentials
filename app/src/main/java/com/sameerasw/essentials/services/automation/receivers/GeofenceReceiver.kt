/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: GeofenceReceiver.kt
 * Description: Hands arrive and leave location transitions from Play services to LocationAutomationWorker.
 */

package com.sameerasw.essentials.services.automation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.location.GeofencingEvent
import com.sameerasw.essentials.services.automation.LocationAutomationWorker

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.e(TAG, "Geofence error ${event.errorCode}")
            return
        }
        val ids = event.triggeringGeofences.orEmpty().map { it.requestId }
        if (ids.isEmpty()) return

        val request =
            OneTimeWorkRequestBuilder<LocationAutomationWorker>()
                .setInputData(
                    workDataOf(
                        LocationAutomationWorker.KEY_AUTOMATION_IDS to ids.toTypedArray(),
                        LocationAutomationWorker.KEY_TRANSITION to event.geofenceTransition,
                    ),
                ).build()
        WorkManager.getInstance(context).enqueue(request)
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
