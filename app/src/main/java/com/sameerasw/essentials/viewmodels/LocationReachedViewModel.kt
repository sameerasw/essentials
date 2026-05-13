package com.sameerasw.essentials.viewmodels

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sameerasw.essentials.data.repository.LocationReachedRepository
import com.sameerasw.essentials.domain.model.LocationAlarm
import com.sameerasw.essentials.services.LocationReachedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@androidx.annotation.Keep
class LocationReachedViewModel(application: Application) : AndroidViewModel(application) {
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

    fun setShowBottomSheet(show: Boolean) {
        repository.setShowBottomSheet(show)
    }

    fun setTempAlarm(alarm: LocationAlarm?) {
        repository.setTempAlarm(alarm)
    }

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

    fun deleteAlarm(alarmId: String) {
        if (activeAlarmId.value == alarmId) {
            stopTracking()
        }
        val currentList = savedAlarms.value.filter { it.id != alarmId }
        repository.saveAlarms(currentList)
    }

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
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    val dist = calculateDistance(
                        it.latitude, it.longitude,
                        alarm.latitude, alarm.longitude
                    )
                    startDistance.value = dist
                    repository.saveStartDistance(dist)
                }
            }

        // Clear last trip when starting new
        lastTrip.value = null
        repository.saveLastTrip(null)
    }

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

    fun pauseTracking() {
        val id = activeAlarmId.value ?: return
        val intent = Intent(getApplication(), LocationReachedService::class.java).apply {
            action = LocationReachedService.ACTION_PAUSE
        }
        getApplication<Application>().startService(intent)
        repository.updatePausedState(id, true)
    }

    fun resumeTracking() {
        val id = activeAlarmId.value ?: return
        val intent = Intent(getApplication(), LocationReachedService::class.java).apply {
            action = LocationReachedService.ACTION_RESUME
        }
        getApplication<Application>().startService(intent)
        repository.updatePausedState(id, false)
        updateCurrentDistance()
    }

    private var distanceTrackingJob: kotlinx.coroutines.Job? = null

    fun startUiTracking() {
        if (distanceTrackingJob?.isActive == true) return

        distanceTrackingJob = viewModelScope.launch {
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

    fun stopUiTracking() {
        distanceTrackingJob?.cancel()
        distanceTrackingJob = null
    }

    override fun onCleared() {
        stopUiTracking()
        super.onCleared()
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun updateCurrentDistance() {
        val id = activeAlarmId.value
        val activeAlarm = savedAlarms.value.find { it.id == id } ?: tempAlarm.value ?: return

        if (activeAlarm.isPaused) return

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    val distance = calculateDistance(
                        it.latitude, it.longitude,
                        activeAlarm.latitude, activeAlarm.longitude
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

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371e3 // Earth's radius in meters
        val phi1 = lat1 * PI / 180
        val phi2 = lat2 * PI / 180
        val deltaPhi = (lat2 - lat1) * PI / 180
        val deltaLambda = (lon2 - lon1) * PI / 180

        val a = sin(deltaPhi / 2).pow(2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (r * c).toFloat()
    }

    fun handleIntent(intent: Intent): Boolean {
        val action = intent.action
        val type = intent.type
        val data = intent.data

        android.util.Log.d(
            "LocationReachedVM",
            "handleIntent: action=$action, type=$type, data=$data"
        )

        val textToParse = when {
            action == Intent.ACTION_SEND && type == "text/plain" -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }

            action == Intent.ACTION_VIEW && data?.scheme == "geo" -> {
                data.toString()
            }

            action == Intent.ACTION_VIEW && (data?.host?.contains("google.com") == true || data?.host?.contains(
                "goo.gl"
            ) == true) -> {
                data.toString()
            }

            else -> null
        }

        if (textToParse == null) return false

        // Check if it's a shortened URL that needs resolution
        if (textToParse.contains("maps.app.goo.gl") || textToParse.contains("goo.gl/maps")) {
            repository.setShowBottomSheet(true)
            resolveAndParse(textToParse)
            return true
        }

        return tryParseAndSet(textToParse)
    }

    private fun tryParseAndSet(text: String): Boolean {
        val commaRegex = Regex("(-?\\d+\\.\\d+)\\s*,\\s*(-?\\d+\\.\\d+)")
        val dataRegex = Regex("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)")

        val match = commaRegex.find(text) ?: dataRegex.find(text)

        if (match != null) {
            val lat = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val lng = match.groupValues[2].toDoubleOrNull() ?: 0.0

            if (lat != 0.0 && lng != 0.0) {
                android.util.Log.d("LocationReachedVM", "Parsed coordinates: $lat, $lng")
                repository.setTempAlarm(
                    LocationAlarm(
                        latitude = lat,
                        longitude = lng,
                        name = "New Destination",
                        isEnabled = false
                    )
                )
                repository.setShowBottomSheet(true)
                updateCurrentDistance()
                repository.setIsProcessing(false)
                return true
            }
        }
        android.util.Log.d("LocationReachedVM", "No coordinates found in text: $text")
        repository.setIsProcessing(false)
        return false
    }

    private fun resolveAndParse(shortUrl: String) {
        repository.setIsProcessing(true)
        viewModelScope.launch {
            val resolvedUrl = withContext(Dispatchers.IO) {
                try {
                    val url = URL(shortUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = false
                    connection.connect()
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    location ?: shortUrl
                } catch (e: Exception) {
                    android.util.Log.e("LocationReachedVM", "Error resolving URL", e)
                    shortUrl
                }
            }
            android.util.Log.d("LocationReachedVM", "Resolved URL: $resolvedUrl")
            if (!tryParseAndSet(resolvedUrl)) {
                val pathRegex = Regex("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
                val pathMatch = pathRegex.find(resolvedUrl)
                if (pathMatch != null) {
                    val lat = pathMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                    val lng = pathMatch.groupValues[2].toDoubleOrNull() ?: 0.0
                    if (lat != 0.0 && lng != 0.0) {
                        repository.setTempAlarm(
                            LocationAlarm(
                                latitude = lat,
                                longitude = lng,
                                name = "New Destination",
                                isEnabled = false
                            )
                        )
                        repository.setShowBottomSheet(true)
                        updateCurrentDistance()
                    }
                }
                repository.setIsProcessing(false)
            }
        }
    }
}
