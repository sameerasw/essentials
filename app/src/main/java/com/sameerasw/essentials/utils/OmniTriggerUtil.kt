package com.sameerasw.essentials.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.shizuku.ShizukuPermissionHelper
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Method

object OmniTriggerUtil {
    private const val TAG = "OmniTriggerUtil"

    private var iVimsClass: Class<*>? = null
    private var vimsInterfaceMethod: Method? = null
    private var serviceManagerClass: Class<*>? = null
    private var getServiceMethod: Method? = null

    @SuppressLint("PrivateApi")
    private fun ensureReflection() {
        if (iVimsClass != null) return
        runCatching {
            iVimsClass = Class.forName("com.android.internal.app.IVoiceInteractionManagerService")
            vimsInterfaceMethod = Class.forName("com.android.internal.app.IVoiceInteractionManagerService\$Stub")
                .getMethod("asInterface", IBinder::class.java)
            serviceManagerClass = Class.forName("android.os.ServiceManager")
            getServiceMethod = serviceManagerClass?.getMethod("getService", String::class.java)
        }
    }

    fun trigger(context: Context): Boolean {
        ensureReflection()
        
        val bundle = Bundle().apply {
            putLong("invocation_time_ms", SystemClock.elapsedRealtime())
            putInt("omni.entry_point", 1)
            putBoolean("micts_trigger", true)
        }

        // 1. Try Shizuku logic
        val shizukuHelper = ShizukuPermissionHelper(context)
        if (shizukuHelper.isReady() && shizukuHelper.hasPermission()) {
            val result = runCatching {
                val vis = ShizukuUtils.getSystemBinder("voiceinteraction")
                val vims = vimsInterfaceMethod?.invoke(null, vis)
                val clazz = iVimsClass
                if (vims != null && clazz != null) {
                    invokeShowSession(clazz, vims, bundle)
                } else false
            }.getOrDefault(false)

            if (result) return true
        }

        // 2. Fallback to Non-Root Reflection
        return runCatching {
            val vis = getServiceMethod?.invoke(null, "voiceinteraction") as IBinder?
            val vims = vimsInterfaceMethod?.invoke(null, vis)
            val clazz = iVimsClass
            if (vims != null && clazz != null) {
                invokeShowSession(clazz, vims, bundle)
            } else false
        }.onFailure { e ->
            Log.e(TAG, "Trigger failed", e)
        }.getOrDefault(false)
    }

    private fun invokeShowSession(clazz: Class<*>, vims: Any, bundle: Bundle): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HiddenApiBypass.invoke(clazz, vims, "showSessionFromSession", null, bundle, 7, "hyperOS_home") as Boolean? ?: false
            } else {
                HiddenApiBypass.invoke(clazz, vims, "showSessionFromSession", null, bundle, 7) as Boolean? ?: false
            }
        }.getOrDefault(false)
    }
}
