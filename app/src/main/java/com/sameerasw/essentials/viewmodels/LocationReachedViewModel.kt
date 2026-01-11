package com.sameerasw.essentials.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.LocationReachedRepository
import com.sameerasw.essentials.domain.model.LocationAlarm
import com.sameerasw.essentials.services.LocationReachedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

class LocationReachedViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocationReachedRepository(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    var alarm = mutableStateOf(repository.getAlarm())
        private set
    
    var isProcessingCoordinates = mutableStateOf(false)
        private set

    var currentDistance = mutableStateOf<Float?>(null)
        private set
    
    var startDistance = mutableStateOf(repository.getStartDistance())
        private set

    init {
        startDistanceTracking()
        
        // Observe shared state for real-time updates across activities
        viewModelScope.launch {
            LocationReachedRepository.isProcessing.collect {
                isProcessingCoordinates.value = it
            }
        }
        
        viewModelScope.launch {
            LocationReachedRepository.alarmFlow.collect { newAlarm ->
                newAlarm?.let {
                    alarm.value = it
                    // Start distance might need refresh if destination changed
                }
            }
        }
    }

    fun clearAlarm() {
        val clearedAlarm = LocationAlarm(0.0, 0.0, 1000, false)
        alarm.value = clearedAlarm
        startDistance.value = 0f
        repository.saveAlarm(clearedAlarm)
        repository.saveStartDistance(0f)
        LocationReachedService.stop(getApplication())
        currentDistance.value = null
    }

    fun startTracking() {
        val currentAlarm = alarm.value
        if (currentAlarm.latitude != 0.0 && currentAlarm.longitude != 0.0) {
            val enabledAlarm = currentAlarm.copy(isEnabled = true)
            alarm.value = enabledAlarm
            repository.saveAlarm(enabledAlarm)
            LocationReachedService.start(getApplication())
            
            // Refreshed start distance logic
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    location?.let {
                        val dist = calculateDistance(
                            it.latitude, it.longitude,
                            enabledAlarm.latitude, enabledAlarm.longitude
                        )
                        startDistance.value = dist
                        repository.saveStartDistance(dist)
                    }
                }
        }
    }

    fun stopTracking() {
        val currentAlarm = alarm.value
        val disabledAlarm = currentAlarm.copy(isEnabled = false)
        alarm.value = disabledAlarm
        repository.saveAlarm(disabledAlarm)
        LocationReachedService.stop(getApplication())
        // Keep start distance for potential restart? Or maybe just keep coordinates.
        // User said "keep last track in memory (only destination)".
    }

    private fun startDistanceTracking() {
        viewModelScope.launch {
            while (true) {
                // Tracking should only happen if enabled? Or just if coordinates exist?
                // Logic: Distance displayed in UI needs coords. Service needs enabled.
                if (alarm.value.latitude != 0.0 && alarm.value.longitude != 0.0) {
                    updateCurrentDistance()
                } else {
                    currentDistance.value = null
                }
                delay(10000) // Update every 10 seconds
            }
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun updateCurrentDistance() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let {
                    val distance = calculateDistance(
                        it.latitude, it.longitude,
                        alarm.value.latitude, alarm.value.longitude
                    )
                    currentDistance.value = distance
                }
            }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
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

    fun updateAlarm(newAlarm: LocationAlarm) {
        alarm.value = newAlarm
        repository.saveAlarm(newAlarm)
        
        // If updating radius while enabled, ensure service stays up or is updated?
        // Service just reads from repo loop, so saving is enough.
    }

    fun handleIntent(intent: android.content.Intent): Boolean {
        val action = intent.action
        val type = intent.type
        val data = intent.data

        android.util.Log.d("LocationReachedVM", "handleIntent: action=$action, type=$type, data=$data")

        val textToParse = when {
            action == android.content.Intent.ACTION_SEND && type == "text/plain" -> {
                intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
            }
            action == android.content.Intent.ACTION_VIEW && data?.scheme == "geo" -> {
                data.toString()
            }
            action == android.content.Intent.ACTION_VIEW && (data?.host?.contains("google.com") == true || data?.host?.contains("goo.gl") == true) -> {
                data.toString()
            }
            else -> null
        }

        if (textToParse == null) return false

        // Check if it's a shortened URL that needs resolution
        if (textToParse.contains("maps.app.goo.gl") || textToParse.contains("goo.gl/maps")) {
            resolveAndParse(textToParse)
            return true // Navigate to settings while resolving
        }

        return tryParseAndSet(textToParse)
    }

    private fun tryParseAndSet(text: String): Boolean {
        // Broad regex for coordinates: looks for two floats separated by a comma
        // Supports: "40.7127, -74.0059", "geo:40.7127,-74.0059", "@40.7127,-74.0059", "q=40.7127,-74.0059"
        val commaRegex = Regex("(-?\\d+\\.\\d+)\\s*,\\s*(-?\\d+\\.\\d+)")
        
        // Pattern for Google Maps data URLs: !3d40.7127!4d-74.0059
        val dataRegex = Regex("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)")

        val match = commaRegex.find(text) ?: dataRegex.find(text)
        
        if (match != null) {
            val lat = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val lng = match.groupValues[2].toDoubleOrNull() ?: 0.0
            
            if (lat != 0.0 && lng != 0.0) {
                android.util.Log.d("LocationReachedVM", "Parsed coordinates: $lat, $lng")
                // Staging mode: don't enable yet
                updateAlarm(alarm.value.copy(latitude = lat, longitude = lng, isEnabled = false))
                android.widget.Toast.makeText(getApplication(), getApplication<Application>().getString(
                    R.string.location_reached_toast_set, lat, lng), android.widget.Toast.LENGTH_SHORT).show()
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
                // Additional check for @lat,lng which might not have spaces or exactly match the above
                val pathRegex = Regex("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)")
                val pathMatch = pathRegex.find(resolvedUrl)
                if (pathMatch != null) {
                    val lat = pathMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                    val lng = pathMatch.groupValues[2].toDoubleOrNull() ?: 0.0
                    if (lat != 0.0 && lng != 0.0) {
                        // Staging mode: don't enable yet
                        updateAlarm(alarm.value.copy(latitude = lat, longitude = lng, isEnabled = false))
                        android.widget.Toast.makeText(getApplication(), getApplication<Application>().getString(R.string.location_reached_toast_set, lat, lng), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                repository.setIsProcessing(false)
            }
        }
    }
}
