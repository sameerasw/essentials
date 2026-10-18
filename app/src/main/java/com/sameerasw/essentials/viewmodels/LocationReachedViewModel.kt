/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Location Alarms & Geofencing
 * File: LocationReachedViewModel.kt
 * Description: ViewModel managing destination geofencing alarms, GPS location tracking,
 * and background alarm service triggers.
 */

package com.sameerasw.essentials.viewmodels

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.LocationReachedRepository
import com.sameerasw.essentials.domain.model.LocationAlarm
import com.sameerasw.essentials.services.LocationReachedService
import com.sameerasw.essentials.utils.MapsLinkParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@androidx.annotation.Keep
class LocationReachedViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = LocationReachedRepository(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    var savedAlarms = mutableStateOf<List<LocationAlarm>>(emptyList())
        private set

    var activeAlarmId = mutableStateOf<String?>(null)
        private set

    var lastTrip = mutableStateOf<LocationAlarm?>(repository.getLastTrip())
        private set

    var tempAlarm = mutableStateOf<LocationAlarm?>(null)
        private set

    var showBottomSheet = mutableStateOf(false)
        private set

    var isProcessingCoordinates = mutableStateOf(false)
        private set

    var currentDistance = mutableStateOf<Float?>(null)
        private set

    var startDistance = mutableStateOf(repository.getStartDistance())
        private set

    var remainingTimeMinutes = mutableStateOf<Int?>(null)
        private set

    var startTime = mutableStateOf(repository.getStartTime())
        private set

    init {
        // Observe shared state for real-time updates across activities
        viewModelScope.launch {
            LocationReachedRepository.isProcessing.collect {
                isProcessingCoordinates.value = it
            }
        }

        viewModelScope.launch {
            LocationReachedRepository.tempAlarm.collect {
                tempAlarm.value = it
            }
        }

        viewModelScope.launch {
            LocationReachedRepository.showBottomSheet.collect {
                showBottomSheet.value = it
            }
        }

        viewModelScope.launch {
            LocationReachedRepository.alarmsFlow.collect { alarms ->
                savedAlarms.value = alarms
            }
        }

        viewModelScope.launch {
            LocationReachedRepository.activeAlarmId.collect { id ->
                activeAlarmId.value = id
                if (id != null) {
                    updateCurrentDistance()
                } else {
                    currentDistance.value = null
                }
            }
        }
    }

    /**
     * Executes the set show bottom sheet operation.
     *
     * @param show [Boolean] Target show.
     */
    fun setShowBottomSheet(show: Boolean) {
        repository.setShowBottomSheet(show)
    }

    /**
     * Executes the set temp alarm operation.
     *
     * @param alarm [LocationAlarm?] Target alarm.
     */
    fun setTempAlarm(alarm: LocationAlarm?) {
        repository.setTempAlarm(alarm)
    }

    /**
     * Executes the save alarm operation.
     *
     * @param alarm [LocationAlarm] Target alarm.
     */
    fun saveAlarm(alarm: LocationAlarm) {
        val currentList = savedAlarms.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == alarm.id }
        if (index != -1) {
            currentList[index] = alarm
        } else {
            currentList.add(alarm)
        }
        repository.saveAlarms(currentList)
        repository.setShowBottomSheet(false)
        repository.setTempAlarm(null)
    }

    /**
     * Executes the delete alarm operation.
     *
     * @param alarmId [String] Target alarm id.
     */
    fun deleteAlarm(alarmId: String) {
        if (activeAlarmId.value == alarmId) {
            stopTracking()
        }
        val currentList = savedAlarms.value.filter { it.id != alarmId }
        repository.saveAlarms(currentList)
    }

    /**
     * Executes the start tracking operation.
     *
     * @param alarmId [String] Target alarm id.
     */
    fun startTracking(alarmId: String) {
        val alarm = savedAlarms.value.find { it.id == alarmId } ?: return

        // Stop any previous tracking
        if (activeAlarmId.value != null && activeAlarmId.value != alarmId) {
            stopTracking()
        }

        repository.saveActiveAlarmId(alarmId)
        LocationReachedService.start(getApplication())

        val now = System.currentTimeMillis()
        repository.saveStartTime(now)
        startTime.value = now
        repository.updateLastTravelled(alarmId, now)

        // Refreshed start distance logic
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    val dist =
                        calculateDistance(
                            it.latitude,
                            it.longitude,
                            alarm.latitude,
                            alarm.longitude,
                        )
                    startDistance.value = dist
                    repository.saveStartDistance(dist)
                }
            }

        // Clear last trip when starting new
        lastTrip.value = null
        repository.saveLastTrip(null)
    }

    /**
     * Executes the stop tracking operation.
     */
    fun stopTracking() {
        val id = activeAlarmId.value ?: return
        val alarm = savedAlarms.value.find { it.id == id }

        if (alarm != null) {
            // Save as last trip
            lastTrip.value = alarm
            repository.saveLastTrip(alarm)
            repository.updatePausedState(id, false)
        }

        repository.saveActiveAlarmId(null)
        LocationReachedService.stop(getApplication())
        currentDistance.value = null
        remainingTimeMinutes.value = null
        startDistance.value = 0f
        repository.saveStartDistance(0f)
        repository.saveStartTime(0L)
        startTime.value = 0L
    }

    /**
     * Executes the pause tracking operation.
     */
    fun pauseTracking() {
        val id = activeAlarmId.value ?: return
        val intent =
            Intent(getApplication(), LocationReachedService::class.java).apply {
                action = LocationReachedService.ACTION_PAUSE
            }
        getApplication<Application>().startService(intent)
        repository.updatePausedState(id, true)
    }

    /**
     * Executes the resume tracking operation.
     */
    fun resumeTracking() {
        val id = activeAlarmId.value ?: return
        val intent =
            Intent(getApplication(), LocationReachedService::class.java).apply {
                action = LocationReachedService.ACTION_RESUME
            }
        getApplication<Application>().startService(intent)
        repository.updatePausedState(id, false)
        updateCurrentDistance()
    }

    private var distanceTrackingJob: kotlinx.coroutines.Job? = null

    /**
     * Executes the start ui tracking operation.
     */
    fun startUiTracking() {
        if (distanceTrackingJob?.isActive == true) return

        distanceTrackingJob =
            viewModelScope.launch {
                while (true) {
                    if (activeAlarmId.value != null) {
                        updateCurrentDistance()
                    } else {
                        currentDistance.value = null
                    }
                    delay(10000) // Update every 10 seconds while UI is active
                }
            }
    }

    /**
     * Executes the stop ui tracking operation.
     */
    fun stopUiTracking() {
        distanceTrackingJob?.cancel()
        distanceTrackingJob = null
    }

    override fun onCleared() {
        stopUiTracking()
        super.onCleared()
    }

    /**
     * Executes the update current distance operation.
     */
    @android.annotation.SuppressLint("MissingPermission")
    fun updateCurrentDistance() {
        val id = activeAlarmId.value
        val activeAlarm = savedAlarms.value.find { it.id == id } ?: tempAlarm.value ?: return

        if (activeAlarm.isPaused) return

        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    val distance =
                        calculateDistance(
                            it.latitude,
                            it.longitude,
                            activeAlarm.latitude,
                            activeAlarm.longitude,
                        )
                    currentDistance.value = distance
                    calculateEta(distance)
                }
            }
    }

    private fun calculateEta(currentDistMeters: Float) {
        val startDistMeters = startDistance.value
        val startT = startTime.value
        if (startDistMeters <= 0 || startT <= 0L) {
            remainingTimeMinutes.value = null
            return
        }

        val elapsedMillis = System.currentTimeMillis() - startT
        val distanceTravelled = startDistMeters - currentDistMeters

        if (distanceTravelled <= 0 || elapsedMillis <= 0) {
            remainingTimeMinutes.value = null
            return
        }

        val remainingMillis = (currentDistMeters * elapsedMillis / distanceTravelled).toLong()
        remainingTimeMinutes.value = (remainingMillis / 60000).toInt().coerceAtLeast(1)
    }

    /**
     * Executes the calculate distance operation.
     *
     * @param lat1 [Double] Target lat1.
     * @param lon1 [Double] Target lon1.
     * @param lat2 [Double] Target lat2.
     * @param lon2 [Double] Target lon2.
     * @return The resulting Float data.
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Float {
        val r = 6371e3 // Earth's radius in meters
        val phi1 = lat1 * PI / 180
        val phi2 = lat2 * PI / 180
        val deltaPhi = (lat2 - lat1) * PI / 180
        val deltaLambda = (lon2 - lon1) * PI / 180

        val a =
            sin(deltaPhi / 2).pow(2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (r * c).toFloat()
    }

    /**
     * Executes the handle intent operation.
     *
     * @param intent [Intent] Target intent.
     * @return The resulting Boolean data.
     */
    fun handleIntent(intent: Intent): Boolean {
        val action = intent.action
        val type = intent.type
        val data = intent.data

        Log.d(
            "LocationReachedVM",
            "handleIntent: action=$action, type=$type, data=$data",
        )

        val textToParse =
            when {
                action == Intent.ACTION_SEND && type == "text/plain" -> {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                }

                action == Intent.ACTION_VIEW && data?.scheme == "geo" -> {
                    data.toString()
                }

                action == Intent.ACTION_VIEW &&
                    (
                        data?.host?.contains("google.com") == true ||
                            data?.host?.contains(
                                "goo.gl",
                            ) == true
                    ) -> {
                    data.toString()
                }

                else -> null
            }

        if (textToParse == null) return false

        // Check if it's a shortened URL that needs resolution
        if (MapsLinkParser.isShortLink(textToParse)) {
            repository.setShowBottomSheet(true)
            resolveAndParse(textToParse)
            return true
        }

        return tryParseAndSet(textToParse)
    }

    private fun tryParseAndSet(text: String): Boolean {
        val place = MapsLinkParser.parse(text)
        if (place == null) {
            Log.d("LocationReachedVM", "No coordinates found in text: $text")
            repository.setIsProcessing(false)
            return false
        }
        setPlace(place)
        return true
    }

    private fun setPlace(place: MapsLinkParser.Place) {
        Log.d("LocationReachedVM", "Parsed coordinates: ${place.latitude}, ${place.longitude}")
        repository.setTempAlarm(
            LocationAlarm(
                latitude = place.latitude,
                longitude = place.longitude,
                name = place.name ?: getApplication<Application>().getString(R.string.location_reached_new_destination),
                isEnabled = false,
            ),
        )
        repository.setShowBottomSheet(true)
        updateCurrentDistance()
        repository.setIsProcessing(false)
    }

    private fun resolveAndParse(shortUrl: String) {
        repository.setIsProcessing(true)
        viewModelScope.launch {
            val place = withContext(Dispatchers.IO) { MapsLinkParser.resolve(getApplication(), shortUrl) }
            if (place != null) setPlace(place) else repository.setIsProcessing(false)
        }
    }
}
