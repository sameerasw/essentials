package com.sameerasw.essentials.utils

import android.content.Context
import android.os.IBinder
import android.os.Parcel
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object SurfaceFlingerControl {

    private const val SURFACE_COMPOSER_INTERFACE_KEY = "android.ui.ISurfaceComposer"
    private const val SURFACE_FLINGER_DISABLE_OVERLAYS_CODE = 1008
    private const val SURFACE_FLINGER_READ_CODE = 1010

    fun setDisableHwOverlays(disable: Boolean, isRoot: Boolean = false): Boolean {
        val state = if (disable) 1 else 0

        if (isRoot) {
            return RootUtils.runCommand("service call SurfaceFlinger 1008 i32 $state")
        }

        val rawBinder: IBinder? = try {
            SystemServiceHelper.getSystemService("SurfaceFlinger")
        } catch (e: Exception) {
            null
        }

        if (rawBinder != null) {
            val binder = ShizukuBinderWrapper(rawBinder)
            val data = Parcel.obtain()
            return try {
                data.writeInterfaceToken(SURFACE_COMPOSER_INTERFACE_KEY)
                data.writeInt(state)
                binder.transact(
                    SURFACE_FLINGER_DISABLE_OVERLAYS_CODE,
                    data,
                    null,
                    0
                )
                true
            } catch (e: Exception) {
                ShizukuUtils.runCommand("service call SurfaceFlinger 1008 i32 $state")
                true
            } finally {
                data.recycle()
            }
        } else {
            ShizukuUtils.runCommand("service call SurfaceFlinger 1008 i32 $state")
            return true
        }
    }

    fun isHwOverlaysDisabled(context: Context? = null, isRoot: Boolean = false): Boolean {
        val rawBinder: IBinder? = try {
            SystemServiceHelper.getSystemService("SurfaceFlinger")
        } catch (e: Exception) {
            null
        }

        if (rawBinder != null) {
            val binder = ShizukuBinderWrapper(rawBinder)
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken(SURFACE_COMPOSER_INTERFACE_KEY)
                binder.transact(SURFACE_FLINGER_READ_CODE, data, reply, 0)
                reply.readInt() // showCpu
                reply.readInt() // enableGL
                reply.readInt() // showUpdates
                reply.readInt() // showBackground
                val disableOverlays = reply.readInt() // 5th integer
                return disableOverlays != 0
            } catch (e: Exception) {
                // Fallthrough to shell command output parsing
            } finally {
                data.recycle()
                reply.recycle()
            }
        }

        if (context != null) {
            val output = ShellUtils.runCommandWithOutput(context, "service call SurfaceFlinger 1010")
            if (!output.isNullOrEmpty()) {
                val hexValues = Regex("0x[0-9a-fA-F]+").findAll(output).map { it.value }.toList()
                if (hexValues.size >= 5) {
                    return hexValues[4].substringAfter("0x").toIntOrNull(16) != 0
                }
            }
        }

        return false
    }
}
