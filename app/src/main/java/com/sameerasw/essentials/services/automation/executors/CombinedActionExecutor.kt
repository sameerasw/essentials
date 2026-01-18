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
            is Action.DeviceEffects -> {
                 if (Build.VERSION.SDK_INT >= 35) { // Android 15+
                     val nm = context.getSystemService(android.app.NotificationManager::class.java)
                     if (nm.isNotificationPolicyAccessGranted) {
                         try {
                              if (action.enabled) {
                                  // ENABLE/UPDATE EFFECTS
                                  val effectsBuilder = android.service.notification.ZenDeviceEffects.Builder()
                                      .setShouldDisplayGrayscale(action.grayscale)
                                      .setShouldSuppressAmbientDisplay(action.suppressAmbient)
                                      .setShouldDimWallpaper(action.dimWallpaper)
                                      .setShouldUseNightMode(action.nightMode)
                                 
                                  val effects = effectsBuilder.build()
                                  
                                  val ruleId = "essentials_focus_mode"
                                  val existingRule = nm.automaticZenRules.values.find { it.name == "Essentials Focus" }
                                  val ruleKey = existingRule?.let { nm.automaticZenRules.entries.find { entry -> entry.value == it }?.key }
                                  
                                  val componentName = android.content.ComponentName(context, com.sameerasw.essentials.services.EssentialsConditionProvider::class.java)
                                  val conditionUri = com.sameerasw.essentials.services.EssentialsConditionProvider.CONDITION_URI
                                  
                                  val ruleBuilder = android.app.AutomaticZenRule.Builder("Essentials Focus", conditionUri)
                                      .setOwner(componentName)
                                      .setDeviceEffects(effects)
                                      .setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                                      .setZenPolicy(android.service.notification.ZenPolicy.Builder().allowAlarms(true).build())
                                      .setConditionId(conditionUri)
                                      .setConfigurationActivity(android.content.ComponentName(context, com.sameerasw.essentials.MainActivity::class.java))
    
                                  if (ruleKey != null) {
                                      nm.updateAutomaticZenRule(ruleKey, ruleBuilder.build())
                                  } else {
                                      nm.addAutomaticZenRule(ruleBuilder.build())
                                  }
                                  
                                  // Trigger the condition to be TRUE
                                  com.sameerasw.essentials.services.EssentialsConditionProvider.setConditionState(context, true)
                                  
                                  android.util.Log.d("DeviceEffects", "Updated ZenRule for Device Effects")
                                  
                              } else {
                                  // DISABLE EFFECTS
                                  val existingRuleEntry = nm.automaticZenRules.entries.find { it.value.name == "Essentials Focus" }
                                  existingRuleEntry?.let { entry ->
                                      val rule = entry.value
                                      rule.isEnabled = false
                                      nm.updateAutomaticZenRule(entry.key, rule)
                                  }
                                  // Also notify condition false just in case
                                  com.sameerasw.essentials.services.EssentialsConditionProvider.setConditionState(context, false)
                                  
                                  android.util.Log.d("DeviceEffects", "Disabled ZenRule for Device Effects")
                              }
                              
                         } catch (e: Exception) {
                             e.printStackTrace()
                         }
                     }
                 }
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
