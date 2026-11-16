package com.sameerasw.essentials.utils

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.sameerasw.essentials.services.receivers.SecurityDeviceAdminReceiver

object AppResetHelper {
    fun resetAll(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, SecurityDeviceAdminReceiver::class.java)
        runCatching { if (dpm.isAdminActive(admin)) dpm.removeActiveAdmin(admin) }
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.clearApplicationUserData()
    }
}
