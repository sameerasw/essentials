/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities
 * File: HilightLights.kt
 * Description: Shizuku-backed access to the lights service for the Pixel Hilight LED array.
 */

package com.sameerasw.essentials.utils.hardware

import android.hardware.lights.Light
import android.hardware.lights.LightState
import android.os.Binder
import android.os.IBinder
import com.sameerasw.essentials.EssentialsApp
import android.content.Context
import com.sameerasw.essentials.utils.PrivilegedMode
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.utils.ShizukuUtils
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method

// CONTROL_DEVICE_LIGHTS is signature|privileged; the shell user Shizuku runs as already holds it
object HilightLights {
    // Light.LIGHT_TYPE_APPLICATION, added in API 37 for the Hilight array
    private const val LIGHT_TYPE_APPLICATION = 10
    private const val DEFAULT_UPDATE_PERIOD_MS = 33L
    private const val SERVICE = "android.hardware.lights.ILightsManager"

    data class LedArray(
        val ids: IntArray,
        val minUpdatePeriodMs: Long,
    )

    private class Service(
        val instance: Any,
        val getLights: Method,
        val openSession: Method,
        val closeSession: Method,
        val setLightStates: Method,
    )

    @Volatile private var cachedArray: LedArray? = null

    @Volatile private var cachedService: Service? = null

    // Root can't hand out the lights binder, only Shizuku and Porter (through its Shizuku bridge) can
    fun isModeSupported(context: Context): Boolean =
        ShellUtils.resolveMode(context).let { it == PrivilegedMode.SHIZUKU || it == PrivilegedMode.PORTER }

    fun isAccessGranted(): Boolean =
        try {
            isModeSupported(EssentialsApp.context) && ShizukuUtils.isShizukuAvailable() && ShizukuUtils.hasPermission()
        } catch (_: Exception) {
            false
        }

    // Ordered by ordinal so patterns sweep across the physical array
    fun array(): LedArray? {
        cachedArray?.let { return it }
        val service = service() ?: return null
        return try {
            @Suppress("UNCHECKED_CAST")
            val lights = (service.getLights.invoke(service.instance) as List<Light>)
                .filter { it.type == LIGHT_TYPE_APPLICATION }
                .sortedBy { it.ordinal }
            if (lights.isEmpty()) return null
            LedArray(
                ids = lights.map { it.id }.toIntArray(),
                minUpdatePeriodMs = lights.maxOf { updatePeriodMs(it) },
            ).also { cachedArray = it }
        } catch (_: Exception) {
            null
        }
    }

    fun openSession(priority: Int = 0): IBinder? {
        val service = service() ?: return null
        return try {
            val token = Binder()
            service.openSession.invoke(service.instance, token, priority)
            token
        } catch (_: Exception) {
            cachedService = null
            null
        }
    }

    fun setColors(
        token: IBinder,
        ids: IntArray,
        colors: IntArray,
    ): Boolean {
        val service = service() ?: return false
        return try {
            val states = Array<LightState>(ids.size) { i -> LightState.Builder().setColor(colors[i % colors.size]).build() }
            service.setLightStates.invoke(service.instance, token, ids, states)
            true
        } catch (_: Exception) {
            cachedService = null
            false
        }
    }

    fun closeSession(token: IBinder) {
        val service = service() ?: return
        try {
            service.closeSession.invoke(service.instance, token)
        } catch (_: Exception) {
            cachedService = null
        }
    }

    // Cached until a call fails, since the lookup is reflection plus a binder round trip per frame otherwise
    private fun service(): Service? {
        cachedService?.let { return it }
        if (!isAccessGranted()) return null
        return try {
            val binder = SystemServiceHelper.getSystemService("lights") ?: return null
            val iface = Class.forName(SERVICE)
            val instance =
                Class
                    .forName("$SERVICE\$Stub")
                    .getMethod("asInterface", IBinder::class.java)
                    .invoke(null, ShizukuBinderWrapper(binder)) ?: return null
            Service(
                instance = instance,
                getLights = iface.getMethod("getLights"),
                openSession = iface.getMethod("openSession", IBinder::class.java, Int::class.javaPrimitiveType),
                closeSession = iface.getMethod("closeSession", IBinder::class.java),
                setLightStates =
                    iface.getMethod(
                        "setLightStates",
                        IBinder::class.java,
                        IntArray::class.java,
                        Array<LightState>::class.java,
                    ),
            ).also { cachedService = it }
        } catch (_: Exception) {
            null
        }
    }

    // Light.getMinUpdatePeriodMillis() is new in API 37
    private fun updatePeriodMs(light: Light): Long =
        try {
            (Light::class.java.getMethod("getMinUpdatePeriodMillis").invoke(light) as Number)
                .toLong()
                .coerceAtLeast(1)
        } catch (_: Throwable) {
            DEFAULT_UPDATE_PERIOD_MS
        }
}
