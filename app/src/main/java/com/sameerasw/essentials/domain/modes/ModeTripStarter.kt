/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeTripStarter.kt
 * Description: Starts an "Are we there yet" trip when a mode ends.
 */

package com.sameerasw.essentials.domain.modes

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import com.sameerasw.essentials.data.repository.LocationReachedRepository
import com.sameerasw.essentials.domain.model.AppPermission
import com.sameerasw.essentials.services.LocationReachedService
import java.util.concurrent.TimeUnit

object ModeTripStarter {
    private const val TAG = "ModeTripStarter"
    private const val LOCATION_TIMEOUT_SECONDS = 15L

    val requiredPermissions = listOf(AppPermission.LOCATION, AppPermission.BACKGROUND_LOCATION, AppPermission.POST_NOTIFICATIONS)

    fun start(
        context: Context,
        alarmId: String,
    ) {
        val repository = LocationReachedRepository(context)
        val alarm = repository.getAlarms().find { it.id == alarmId } ?: return
        if (repository.getActiveAlarmId() == alarmId) return
        // The location service only gets location from the background with this granted
        if (!AppPermission.LOCATION.isGranted(context) || !AppPermission.BACKGROUND_LOCATION.isGranted(context)) {
            Log.w(TAG, "Skipping trip to ${alarm.name}: location access missing")
            return
        }

        val now = System.currentTimeMillis()
        repository.saveActiveAlarmId(alarmId)
        repository.saveStartTime(now)
        repository.updateLastTravelled(alarmId, now)
        repository.saveLastTrip(null)
        currentLocation(context)?.let { here ->
            val distance = FloatArray(1)
            Location.distanceBetween(here.latitude, here.longitude, alarm.latitude, alarm.longitude, distance)
            repository.saveStartDistance(distance[0])
        }

        runCatching { LocationReachedService.start(context) }
            .onFailure {
                Log.e(TAG, "Failed to start trip to ${alarm.name}", it)
                repository.saveActiveAlarmId(null)
            }
    }

    @SuppressLint("MissingPermission")
    private fun currentLocation(context: Context): Location? =
        runCatching {
            Tasks.await(
                LocationServices.getFusedLocationProviderClient(context).getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null),
                LOCATION_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
        }.getOrNull()
}
