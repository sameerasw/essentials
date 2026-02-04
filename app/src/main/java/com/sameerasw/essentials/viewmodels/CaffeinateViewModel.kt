package com.sameerasw.essentials.viewmodels

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.sameerasw.essentials.domain.controller.CaffeinateController
import com.sameerasw.essentials.services.CaffeinateWakeLockService

class CaffeinateViewModel : ViewModel() {
    val isActive get() = CaffeinateController.isActive
    val isStarting get() = CaffeinateController.isStarting
    val startingTimeLeft get() = CaffeinateController.startingTimeLeft
    val selectedTimeout get() = CaffeinateController.selectedTimeout
    val timeoutPresets = CaffeinateController.timeoutPresets
    
    val postNotificationsGranted = mutableStateOf(false)
    val batteryOptimizationGranted = mutableStateOf(false)
    val abortWithScreenOff = mutableStateOf(true)
    val skipCountdown = mutableStateOf(false)
    val enabledPresets = mutableStateOf(setOf<Int>())

    fun check(context: Context) {
        CaffeinateController.check(context)
        postNotificationsGranted.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        batteryOptimizationGranted.value = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        val prefs = context.getSharedPreferences("caffeinate_prefs", Context.MODE_PRIVATE)
        abortWithScreenOff.value = prefs.getBoolean("abort_screen_off", true)
        skipCountdown.value = prefs.getBoolean("skip_countdown", false)
        
        val savedPresets = prefs.getStringSet("enabled_presets", timeoutPresets.map { it.toString() }.toSet())
        enabledPresets.value = savedPresets?.map { it.toInt() }?.toSet() ?: timeoutPresets.toSet()
    }

    fun togglePreset(preset: Int, context: Context) {
        val current = enabledPresets.value.toMutableSet()
        if (current.contains(preset)) {
            if (current.size > 1) current.remove(preset)
        } else {
            current.add(preset)
        }
        enabledPresets.value = current
        val prefs = context.getSharedPreferences("caffeinate_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("enabled_presets", current.map { it.toString() }.toSet()).apply()
    }

    fun setAbortWithScreenOff(value: Boolean, context: Context) {
        val prefs = context.getSharedPreferences("caffeinate_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("abort_screen_off", value).apply()
        abortWithScreenOff.value = value
        
        if (isActive.value) {
            val intent = Intent(context, CaffeinateWakeLockService::class.java).apply {
                action = "UPDATE_PREFS"
            }
            context.startService(intent)
        }
    }

    fun setSkipCountdown(value: Boolean, context: Context) {
        val prefs = context.getSharedPreferences("caffeinate_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("skip_countdown", value).apply()
        skipCountdown.value = value
    }

    fun toggle(context: Context) {
        CaffeinateController.toggle(context)
    }
}