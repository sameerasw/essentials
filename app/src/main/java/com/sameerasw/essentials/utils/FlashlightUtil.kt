package com.sameerasw.essentials.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException

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

    fun getCurrentLevel(context: Context, cameraId: String): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return 1
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            cameraManager.getTorchStrengthLevel(cameraId)
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Smoothly transitions the flashlight intensity between two levels.
     * Level 0 means the torch is OFF.
     */
    suspend fun fadeFlashlight(
        context: Context,
        cameraId: String,
        fromLevel: Int,
        toLevel: Int,
        durationMs: Long = 250L,
        steps: Int = 10
    ) {
        Log.d(TAG, "fadeFlashlight: from=$fromLevel, to=$toLevel, duration=${durationMs}ms, steps=$steps")
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            cameraManager.setTorchMode(cameraId, toLevel > 0)
            return
        }

        val delayPerStep = durationMs / steps
        try {
            for (i in 1..steps) {
                val level = fromLevel + ((toLevel - fromLevel) * i / steps)
                if (level > 0) {
                    cameraManager.turnOnTorchWithStrengthLevel(cameraId, level)
                } else if (i == steps) {
                    // Final step and target is 0, so turn off
                    cameraManager.setTorchMode(cameraId, false)
                }
                delay(delayPerStep)
            }
            
            if (toLevel > 0) {
                cameraManager.turnOnTorchWithStrengthLevel(cameraId, toLevel)
            } else {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during flashlight fade", e)
            cameraManager.setTorchMode(cameraId, toLevel > 0)
        }
    }

    /**
     * Legacy wrapper for backward compatibility or simpler calls.
     */
    suspend fun fadeFlashlight(
        context: Context,
        cameraId: String,
        targetOn: Boolean,
        maxLevel: Int = getMaxLevel(context, cameraId),
        durationMs: Long = 400L,
        steps: Int = 20
    ) {
        val currentLevel = if (targetOn) 0 else getCurrentLevel(context, cameraId)
        val targetLevel = if (targetOn) maxLevel else 0
        fadeFlashlight(context, cameraId, currentLevel, targetLevel, durationMs, steps)
    }

}
