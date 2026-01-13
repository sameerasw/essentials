package com.sameerasw.essentials.services.automation.executors

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Build
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.utils.HapticUtil
import android.view.View

object CombinedActionExecutor {

    suspend fun execute(context: Context, action: Action) {
        when (action) {
            is Action.HapticVibration -> {
               val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                   val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                   manager.defaultVibrator
               } else {
                   context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
               }
               
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                   vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
               } else {
                    @Suppress("DEPRECATION")
                   vibrator.vibrate(50)
               }
            }
            
            is Action.TurnOnFlashlight -> toggleFlashlight(context, true)
            is Action.TurnOffFlashlight -> toggleFlashlight(context, false)
            is Action.ToggleFlashlight -> {
                val camManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                try {
                    val cameraId = camManager.cameraIdList[0] 
                     camManager.registerTorchCallback(object : CameraManager.TorchCallback() {
                        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                            super.onTorchModeChanged(cameraId, enabled)
                            camManager.unregisterTorchCallback(this)
                            try {
                                camManager.setTorchMode(cameraId, !enabled)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }, null)
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            is Action.ShowNotification -> {
                // Placeholder
            }
            is Action.RemoveNotification -> {
                // Placeholder
            }
            is Action.DimWallpaper -> {
                com.sameerasw.essentials.utils.ShellUtils.runCommand(context, "cmd wallpaper set-dim-amount ${action.dimAmount}")
            }
        }
    }
    
    private fun toggleFlashlight(context: Context, on: Boolean) {
        val camManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = camManager.cameraIdList[0]
            camManager.setTorchMode(cameraId, on)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
