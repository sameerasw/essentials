package com.sameerasw.essentials

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class CaffeinateViewModel : ViewModel() {
    val isActive = mutableStateOf(false)

    fun check(context: Context) {
        isActive.value = isWakeLockServiceRunning(context)
    }

    fun toggle(context: Context) {
        if (isActive.value) {
            // Stop service
            context.stopService(Intent(context, CaffeinateWakeLockService::class.java))
            isActive.value = false
        } else {
            // Start service
            context.startService(Intent(context, CaffeinateWakeLockService::class.java))
            isActive.value = true
        }
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
