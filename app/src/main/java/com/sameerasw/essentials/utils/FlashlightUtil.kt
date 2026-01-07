package com.sameerasw.essentials.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.delay

object FlashlightUtil {
    private const val TAG = "FlashlightUtil"

    fun isIntensitySupported(context: Context, cameraId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val maxLevel = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 0
            maxLevel > 1
        } catch (e: Exception) {
            Log.e(TAG, "Error checking intensity support", e)
            false
        }
    }

    fun getMaxLevel(context: Context, cameraId: String): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return 1
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
        } catch (e: Exception) {
            1
        }
    }

    fun getDefaultLevel(context: Context, cameraId: String): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return 1
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL) ?: 1
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Smoothly transitions the flashlight intensity.
     */
    suspend fun fadeFlashlight(
        context: Context,
        cameraId: String,
        targetOn: Boolean,
        maxLevel: Int = getMaxLevel(context, cameraId),
        durationMs: Long = 400L,
        steps: Int = 20
    ) {
        Log.d(TAG, "fadeFlashlight: targetOn=$targetOn, maxLevel=$maxLevel, steps=$steps")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || maxLevel <= 1) {
            Log.d(TAG, "Intensity not supported, basic toggle")
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.setTorchMode(cameraId, targetOn)
            return
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val delayPerStep = durationMs / steps

        try {
            if (targetOn) {
                // Fade In
                for (i in 1..steps) {
                    val level = (maxLevel * i) / steps
                    val finalLevel = if (level < 1) 1 else level
                    cameraManager.turnOnTorchWithStrengthLevel(cameraId, finalLevel)
                    delay(delayPerStep)
                }
            } else {
                // Fade Out
                for (i in steps downTo 1) {
                    val level = (maxLevel * i) / steps
                    if (level < 1) break
                    cameraManager.turnOnTorchWithStrengthLevel(cameraId, level)
                    delay(delayPerStep)
                }
                cameraManager.setTorchMode(cameraId, false)
            }
            Log.d(TAG, "fadeFlashlight completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during flashlight fade", e)
            cameraManager.setTorchMode(cameraId, targetOn)
        }
    }

}
