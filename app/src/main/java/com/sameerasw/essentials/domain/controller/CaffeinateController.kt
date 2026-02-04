package com.sameerasw.essentials.domain.controller

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.services.CaffeinateWakeLockService
import com.sameerasw.essentials.services.tiles.CaffeinateTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object CaffeinateController {
    val isActive = mutableStateOf(false)
    val isStarting = mutableStateOf(false)
    val startingTimeLeft = mutableStateOf(0)
    val selectedTimeout = mutableStateOf(5)

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var startingJob: Job? = null
    private var screenOffReceiver: android.content.BroadcastReceiver? = null

    val timeoutPresets = listOf(5, 10, 30, 60, -1)

    fun check(context: Context) {
        isActive.value = isWakeLockServiceRunning(context)
    }

    fun toggle(context: Context) {
        if (isActive.value || isStarting.value) {
            cancelAll(context)
        } else {
            startSelection(context)
        }
        refreshTile(context)
    }

    private fun refreshTile(context: Context) {
        TileService.requestListeningState(context, ComponentName(context, CaffeinateTileService::class.java))
    }

    private fun startSelection(context: Context) {
        val enabledPresets = getEnabledPresets(context)
        if (enabledPresets.isEmpty()) return

        isStarting.value = true
        startingTimeLeft.value = 5
        
        val sortedPresets = timeoutPresets.filter { enabledPresets.contains(it) }
        selectedTimeout.value = sortedPresets.firstOrNull() ?: timeoutPresets.first()
        
        registerScreenOffReceiver(context)
        
        if (isSkipCountdownEnabled(context)) {
            startService(context)
            isStarting.value = true
        }
        
        resetSelectionTimer(context)
    }

    private fun registerScreenOffReceiver(context: Context) {
        if (screenOffReceiver != null) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("caffeinate_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("abort_screen_off", true)) return

        screenOffReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    cancelAll(appContext)
                }
            }
        }
        appContext.registerReceiver(screenOffReceiver, android.content.IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    private fun unregisterScreenOffReceiver(context: Context) {
        val appContext = context.applicationContext
        screenOffReceiver?.let {
            try { appContext.unregisterReceiver(it) } catch (_: Exception) {}
        }
        screenOffReceiver = null
    }

    fun cycleTimeout(context: Context) {
        if (!isStarting.value) return
        
        val enabledPresets = getEnabledPresets(context)
        val sortedPresets = timeoutPresets.filter { enabledPresets.contains(it) }
        val currentIndex = sortedPresets.indexOf(selectedTimeout.value)
        val nextIndex = (currentIndex + 1) % sortedPresets.size
        
        selectedTimeout.value = sortedPresets[nextIndex]
        
        if (isActive.value) {
            startService(context)
            isStarting.value = true
        }
        
        resetSelectionTimer(context)
        refreshTile(context)
    }

    private fun resetSelectionTimer(context: Context) {
        startingJob?.cancel()
        startingTimeLeft.value = 5
        startingJob = scope.launch {
            while (startingTimeLeft.value > 0) {
                delay(1000)
                startingTimeLeft.value -= 1
                refreshTile(context)
            }
            startService(context)
        }
    }

    private fun startService(context: Context) {
        unregisterScreenOffReceiver(context)
        isStarting.value = false
        val intent = Intent(context, CaffeinateWakeLockService::class.java).apply {
            putExtra("timeout_minutes", selectedTimeout.value)
        }
        ContextCompat.startForegroundService(context, intent)
        isActive.value = true
        refreshTile(context)
    }

    fun cancelAll(context: Context) {
        unregisterScreenOffReceiver(context)
        startingJob?.cancel()
        isStarting.value = false
        isActive.value = false
        context.stopService(Intent(context, CaffeinateWakeLockService::class.java))
        refreshTile(context)
    }

    private fun getEnabledPresets(context: Context): Set<Int> {
        val prefs = context.getSharedPreferences("caffeinate_prefs", Context.MODE_PRIVATE)
        val savedPresets = prefs.getStringSet("enabled_presets", timeoutPresets.map { it.toString() }.toSet())
        return savedPresets?.map { it.toInt() }?.toSet() ?: timeoutPresets.toSet()
    }

    private fun isSkipCountdownEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("caffeinate_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("skip_countdown", false)
    }

    private fun isWakeLockServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (CaffeinateWakeLockService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
