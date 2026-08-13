/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: GenAIAutomationService.kt
 * Description: Domain model and business logic entry for GenAIAutomationService.kt.
 */

package com.sameerasw.essentials.domain.genai

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.sameerasw.essentials.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GenAIAutomationService {
    private const val TAG = "GenAIAutomationService"
    private val gson = Gson()

    suspend fun isSupported(): Boolean = withContext(Dispatchers.IO) {
        try {
            val generativeModel = Generation.getClient()
            val status = generativeModel.checkStatus()
            status == FeatureStatus.AVAILABLE || status == FeatureStatus.DOWNLOADABLE || status == FeatureStatus.DOWNLOADING
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check GenAI status", e)
            false
        }
    }

    suspend fun suggestAutomation(
        userPrompt: String,
        context: Context? = null
    ): Result<AutomationSuggestion> = withContext(Dispatchers.IO) {
        try {
            val generativeModel = Generation.getClient()
            when (generativeModel.checkStatus()) {
                FeatureStatus.UNAVAILABLE -> {
                    return@withContext Result.failure(IllegalStateException("GenAI feature is UNAVAILABLE on this device"))
                }

                FeatureStatus.DOWNLOADABLE -> {
                    Log.d(TAG, "Gemini Nano model is downloadable. Triggering download...")
                    var downloadFailedReason: String? = null
                    generativeModel.download().collect { downloadStatus ->
                        when (downloadStatus) {
                            is com.google.mlkit.genai.common.DownloadStatus.DownloadFailed -> {
                                downloadFailedReason = downloadStatus.e.message
                            }

                            else -> {}
                        }
                    }
                    if (downloadFailedReason != null) {
                        return@withContext Result.failure(IllegalStateException("Gemini Nano download failed: $downloadFailedReason"))
                    }
                }

                FeatureStatus.DOWNLOADING -> {
                    Log.d(TAG, "Gemini Nano model is currently downloading...")
                    generativeModel.download().collect {}
                }

                FeatureStatus.AVAILABLE -> {
                    // Ready to proceed
                }
            }

            // Gather context data if available
            val availableTagsInfo = if (context != null) {
                val repository = SettingsRepository(context)
                val tags = repository.getFreezeTags()
                if (tags.isNotEmpty()) {
                    "Available freeze app tags on device: " + tags.joinToString { "${it.name} (id: ${it.id})" }
                } else {
                    "Available freeze app tags: None created yet."
                }
            } else ""

            val systemInstructionText = """
                You are an automation assistant for the Essentials Android app.
                Map the user's description into a single JSON object matching the following structure:
                {
                  "type": "TRIGGER" or "STATE" or "APP",
                  "title": "Short descriptive title",
                  "triggerType": "ScreenOff" or "ScreenOn" or "DeviceUnlock" or "ChargerConnected" or "ChargerDisconnected" or "PowerSavingOn" or "PowerSavingOff" or "Schedule" or "BluetoothConnected" or "BluetoothDisconnected" or "WifiConnected" or "WifiDisconnected",
                  "stateType": "Charging" or "ScreenOn" or "PowerSaving" or "TimePeriod",
                  "actionTypes": ["HapticVibration", "ShowNotification", "RemoveNotification", "TurnOnFlashlight", "TurnOffFlashlight", "ToggleFlashlight", "DimWallpaper", "DeviceEffects", "SoundMode", "TurnOnLowPower", "TurnOffLowPower", "ScreenOff", "MediaPlayPause", "MediaNext", "MediaPrevious", "AIAssistant", "TakeScreenshot", "ToggleMediaVolume", "LikeCurrentSong", "CircleToSearch", "PinApp", "SometimesEssentials", "FreezeTag"],
                  "explanation": "Brief 1-2 sentence description of what this automation does",
                  "hour": null or integer (0-23 for Schedule trigger or start time of TimePeriod state),
                  "minute": null or integer (0-59 for Schedule trigger or start time of TimePeriod state),
                  "endHour": null or integer (0-23 for end time of TimePeriod state),
                  "endMinute": null or integer (0-59 for end time of TimePeriod state),
                  "soundMode": null or "SOUND" or "VIBRATE" or "SILENT" (used for SoundMode action, distinguish VIBRATE mode from HapticVibration action),
                  "freezeTagMode": null or "Freeze" or "Unfreeze" (used for FreezeTag action),
                  "freezeTagIds": array of tag IDs or names (used for FreezeTag action),
                  "lockScreenClockStyle": null or "DEFAULT" or "METRO" or "EXPRESSIVE" or "PRIDE" or "MONOSPACE" or "BUBBLE" (used when configuring lock screen clock in SometimesEssentials),
                  "alwaysOnDisplayMode": null or "Off" or "Dynamic" or "On" (used for SometimesEssentials action),
                  "essentialsOnDisplayMode": null or "Off" or "On" or "Docked" (used for SometimesEssentials action),
                  "flashlightPulseEnabled": null or boolean (used for SometimesEssentials action),
                  "notificationLightingEnabled": null or boolean (used for SometimesEssentials action),
                  "smartPixelsEnabled": null or boolean (used to turn Smart Pixels on or off in SometimesEssentials action),
                  "dimWallpaperAmount": null or float 0.0 to 1.0 (used for DimWallpaper action),
                  "selectedApps": array of package names (e.g. ["com.instagram.android"] when user specifies apps for APP type automation)
                }

                Rules:
                1. "type" MUST be:
                   - "TRIGGER" if event-driven (e.g. "when screen turns off", "when charger connected", "at 8:30 AM", "when wifi connected").
                   - "STATE" if condition-based (e.g. "while charging", "while screen is on", "between 10 PM and 7 AM").
                   - "APP" if triggered by opening/closing specific apps (e.g. "when Instagram or YouTube is opened").
                2. Do NOT confuse "SoundMode" action with "HapticVibration" action:
                   - "HapticVibration" = trigger a quick tactile vibration.
                   - "SoundMode" = change device ringer/sound mode ("SOUND", "VIBRATE", or "SILENT").
                3. For exact time schedules (e.g. "at 8:30 AM", "at 22:15"):
                   - Set type="TRIGGER", triggerType="Schedule", hour=8, minute=30.
                4. For time periods (e.g. "from 10 PM to 7 AM", "between 13:00 and 15:00"):
                   - Set type="STATE", stateType="TimePeriod", hour=22, minute=0, endHour=7, endMinute=0.
                5. For freezing/unfreezing apps or app tags:
                   - Use action "FreezeTag".
                   - Set freezeTagMode="Freeze" or "Unfreeze".
                   - Set freezeTagIds with matching tag IDs/names from available tags: $availableTagsInfo.
                6. For deep settings like Metro lock screen clock, AOD mode, flashlight pulse, or Smart Pixels:
                   - Use action "SometimesEssentials".
                   - Set lockScreenClockStyle="METRO", "EXPRESSIVE", etc.
                   - Set smartPixelsEnabled=true or false when user mentions smart pixels or blacking out pixels.

                Respond ONLY with valid raw JSON without any markdown formatting or code blocks.
            """.trimIndent()

            val contextPrompt = """
                $systemInstructionText

                User request: "$userPrompt"
            """.trimIndent()

            val request = generateContentRequest(TextPart(contextPrompt)) {
                temperature = 0.2f
            }

            val response = generativeModel.generateContent(request)
            val candidateText = response.candidates.firstOrNull()?.text ?: ""

            val jsonText = candidateText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            if (jsonText.isNotEmpty()) {
                val suggestion = gson.fromJson(jsonText, AutomationSuggestion::class.java)
                if (suggestion != null) {
                    Result.success(suggestion)
                } else {
                    Result.failure(Exception("Failed to parse JSON response into AutomationSuggestion"))
                }
            } else {
                val finishReason = response.candidates.firstOrNull()?.finishReason
                Result.failure(Exception("AI response was empty. Finish reason: $finishReason"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating automation suggestion", e)
            Result.failure(e)
        }
    }
}
