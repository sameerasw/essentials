package com.sameerasw.essentials.services.automation.executors

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.ScreenOffMethod
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.utils.performHapticFeedback
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object CombinedActionExecutor {

    suspend fun execute(context: Context, action: Action) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            when (action) {
                is Action.TurnOnLowPower -> setLowPowerMode(context, true)
                is Action.TurnOffLowPower -> setLowPowerMode(context, false)
                is Action.HapticVibration -> {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val manager =
                            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                        manager.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            android.os.VibrationEffect.createOneShot(
                                50,
                                android.os.VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(50)
                    }
                }

                is Action.TurnOnFlashlight -> toggleFlashlight(context, true)
                is Action.TurnOffFlashlight -> toggleFlashlight(context, false)
                is Action.ToggleFlashlight -> {
                    val camManager =
                        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    try {
                        camManager.cameraIdList[0]
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
                    com.sameerasw.essentials.utils.ShellUtils.runCommand(
                        context,
                        "cmd wallpaper set-dim-amount ${action.dimAmount}"
                    )
                }

                is Action.DeviceEffects -> {
                    if (Build.VERSION.SDK_INT >= 35) { // Android 15+
                        val nm =
                            context.getSystemService(android.app.NotificationManager::class.java)
                        if (nm.isNotificationPolicyAccessGranted) {
                            try {
                                if (action.enabled) {
                                    // ENABLE/UPDATE EFFECTS
                                    val effectsBuilder = try {
                                        android.service.notification.ZenDeviceEffects.Builder()
                                    } catch (e: NoSuchMethodError) {
                                        try {
                                            val constructor =
                                                android.service.notification.ZenDeviceEffects.Builder::class.java.getConstructor(
                                                    android.service.notification.ZenDeviceEffects::class.java
                                                )
                                            constructor.newInstance(null)
                                        } catch (refE: Exception) {
                                            null
                                        }
                                    } ?: return@withContext

                                    effectsBuilder.setShouldDisplayGrayscale(action.grayscale)
                                        .setShouldSuppressAmbientDisplay(action.suppressAmbient)
                                        .setShouldDimWallpaper(action.dimWallpaper)
                                        .setShouldUseNightMode(action.nightMode)

                                    val effects = effectsBuilder.build()

                                    "essentials_focus_mode"
                                    val existingRule =
                                        nm.automaticZenRules.values.find { it.name == "Essentials Focus" }
                                    val ruleKey =
                                        existingRule?.let { nm.automaticZenRules.entries.find { entry -> entry.value == it }?.key }

                                    val componentName = android.content.ComponentName(
                                        context,
                                        com.sameerasw.essentials.services.EssentialsConditionProvider::class.java
                                    )
                                    val conditionUri =
                                        com.sameerasw.essentials.services.EssentialsConditionProvider.CONDITION_URI

                                    val ruleBuilder = android.app.AutomaticZenRule.Builder(
                                        "Essentials Focus",
                                        conditionUri
                                    )
                                        .setOwner(componentName)
                                        .setDeviceEffects(effects)
                                        .setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                                        .setZenPolicy(
                                            android.service.notification.ZenPolicy.Builder()
                                                .allowAlarms(true).build()
                                        )
                                        .setConditionId(conditionUri)
                                        .setConfigurationActivity(
                                            android.content.ComponentName(
                                                context,
                                                com.sameerasw.essentials.MainActivity::class.java
                                            )
                                        )

                                    if (ruleKey != null) {
                                        nm.updateAutomaticZenRule(ruleKey, ruleBuilder.build())
                                    } else {
                                        nm.addAutomaticZenRule(ruleBuilder.build())
                                    }

                                    // Trigger the condition to be TRUE
                                    com.sameerasw.essentials.services.EssentialsConditionProvider.setConditionState(
                                        context,
                                        true
                                    )

                                    android.util.Log.d(
                                        "DeviceEffects",
                                        "Updated ZenRule for Device Effects"
                                    )

                                } else {
                                    // DISABLE EFFECTS
                                    val existingRuleEntry =
                                        nm.automaticZenRules.entries.find { it.value.name == "Essentials Focus" }
                                    existingRuleEntry?.let { entry ->
                                        val rule = entry.value
                                        rule.isEnabled = false
                                        nm.updateAutomaticZenRule(entry.key, rule)
                                    }
                                    // Also notify condition false just in case
                                    com.sameerasw.essentials.services.EssentialsConditionProvider.setConditionState(
                                        context,
                                        false
                                    )

                                    android.util.Log.d(
                                        "DeviceEffects",
                                        "Disabled ZenRule for Device Effects"
                                    )
                                }

                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                is Action.SoundMode -> {
                    val audioManager =
                        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val ringerMode = when (action.mode) {
                        Action.SoundModeType.SOUND -> AudioManager.RINGER_MODE_NORMAL
                        Action.SoundModeType.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
                        Action.SoundModeType.SILENT -> AudioManager.RINGER_MODE_SILENT
                    }
                    try {
                        audioManager.ringerMode = ringerMode
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                is Action.ScreenOff -> {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val manager =
                            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                        manager.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                    }
                    if (action.haptic != HapticFeedbackType.NONE) {
                        performHapticFeedback(vibrator, action.haptic)
                    }

                    when (action.method) {
                        ScreenOffMethod.ACCESSIBILITY -> {
                            val enabledServices = Settings.Secure.getString(
                                context.contentResolver,
                                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                            )
                            val hasAccess = enabledServices?.contains("com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService") == true
                            if (hasAccess) {
                                val serviceIntent = Intent(context, ScreenOffAccessibilityService::class.java).apply {
                                    this.action = "LOCK_SCREEN"
                                }
                                context.startService(serviceIntent)
                            } else {
                                Toast.makeText(context, "Missing Accessibility permission", Toast.LENGTH_SHORT).show()
                            }
                        }
                        ScreenOffMethod.INPUT -> {
                            if (ShellUtils.hasPermission(context)) {
                                ShellUtils.runCommand(context, "input keyevent ${KeyEvent.KEYCODE_POWER}")
                            } else {
                                Toast.makeText(context, "Missing Shizuku/Root permission", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                is Action.MediaPlayPause -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                }

                is Action.MediaNext -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT))
                    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT))
                }

                is Action.MediaPrevious -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                    am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                }

                is Action.AIAssistant -> {
                    try {
                        val intent = Intent(Intent.ACTION_ASSIST).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                is Action.TakeScreenshot -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val serviceInst = ScreenOffAccessibilityService.instance
                        if (serviceInst != null) {
                            serviceInst.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                        } else {
                            Toast.makeText(context, "Accessibility service is not running", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                is Action.ToggleMediaVolume -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)

                    if (currentVolume > 0) {
                        prefs.edit().putInt("last_media_volume", currentVolume).apply()
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                    } else {
                        val lastVolume = prefs.getInt(
                            "last_media_volume",
                            am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2
                        )
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, lastVolume, AudioManager.FLAG_SHOW_UI)
                    }
                }

                is Action.LikeCurrentSong -> {
                    context.sendBroadcast(
                        Intent("com.sameerasw.essentials.ACTION_LIKE_CURRENT_SONG").setPackage(
                            context.packageName
                        )
                    )
                }

                is Action.CircleToSearch -> {
                    com.sameerasw.essentials.utils.OmniTriggerUtil.trigger(context)
                }

                is Action.PinApp -> {
                    try {
                        val useActivityTask = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        val serviceName = if (useActivityTask) "activity_task" else Context.ACTIVITY_SERVICE
                        val stubClassName = if (useActivityTask) "android.app.IActivityTaskManager\$Stub" else "android.app.IActivityManager\$Stub"
                        val interfaceClassName = if (useActivityTask) "android.app.IActivityTaskManager" else "android.app.IActivityManager"

                        val binder = SystemServiceHelper.getSystemService(serviceName)
                        val stubClass = Class.forName(stubClassName)
                        val interfaceClass = Class.forName(interfaceClassName)

                        val serviceInstance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            org.lsposed.hiddenapibypass.HiddenApiBypass.invoke(stubClass, null, "asInterface", ShizukuBinderWrapper(binder))
                        } else {
                            val asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder::class.java)
                            asInterfaceMethod.invoke(null, ShizukuBinderWrapper(binder))
                        }

                        val tasks = if (useActivityTask) {
                            org.lsposed.hiddenapibypass.HiddenApiBypass.invoke(
                                interfaceClass,
                                serviceInstance,
                                "getTasks",
                                5,
                                false,
                                false,
                                0
                            ) as List<*>
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                org.lsposed.hiddenapibypass.HiddenApiBypass.invoke(
                                    interfaceClass,
                                    serviceInstance,
                                    "getTasks",
                                    5,
                                    0
                                ) as List<*>
                            } else {
                                val getTasksMethod = interfaceClass.getMethod("getTasks", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                                getTasksMethod.invoke(serviceInstance, 5, 0) as List<*>
                            }
                        }
                        
                        var targetTaskId = -1
                        for (task in tasks) {
                            val taskInfo = task as? ActivityManager.RunningTaskInfo ?: continue
                            val topActivity = taskInfo.topActivity
                            if (topActivity != null && topActivity.packageName != context.packageName) {
                                targetTaskId = taskInfo.id
                                break
                            }
                        }
                        
                        if (targetTaskId == -1 && tasks.isNotEmpty()) {
                            val firstTask = tasks.firstOrNull() as? ActivityManager.RunningTaskInfo
                            if (firstTask != null) {
                                targetTaskId = firstTask.id
                            }
                        }

                        if (targetTaskId != -1) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                org.lsposed.hiddenapibypass.HiddenApiBypass.invoke(
                                    interfaceClass,
                                    serviceInstance,
                                    "startSystemLockTaskMode",
                                    targetTaskId
                                )
                            } else {
                                val startLockTaskMethod = interfaceClass.getMethod("startSystemLockTaskMode", Int::class.javaPrimitiveType)
                                startLockTaskMethod.invoke(serviceInstance, targetTaskId)
                            }
                        } else {
                            Toast.makeText(context, "No active foreground task found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Failed to pin app: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun setLowPowerMode(context: Context, on: Boolean) {
        val value = if (on) 1 else 0
        try {
            android.provider.Settings.Global.putInt(context.contentResolver, "low_power", value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
