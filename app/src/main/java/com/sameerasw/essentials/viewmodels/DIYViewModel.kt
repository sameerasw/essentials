/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Automation & DIY Rules
 * File: DIYViewModel.kt
 * Description: ViewModel managing custom triggers, system actions, AI automation suggestions,
 * and rule persistent storage.
 */

package com.sameerasw.essentials.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.domain.diy.State
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.domain.genai.AutomationSuggestion
import com.sameerasw.essentials.domain.genai.GenAIAutomationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class GenAIState {
    object Idle : GenAIState()
    object Loading : GenAIState()
    data class Success(val suggestion: AutomationSuggestion) : GenAIState()
    data class Error(val message: String) : GenAIState()
}

class DIYViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DIYRepository

    private val _genAIState = MutableStateFlow<GenAIState>(GenAIState.Idle)
    val genAIState: StateFlow<GenAIState> = _genAIState.asStateFlow()

    init {
        repository.init(application)
    }

    val automations: StateFlow<List<Automation>> = repository.automations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Executes the delete automation operation.
     *
     * @param id [String] Target id.
     */
    fun deleteAutomation(id: String) {
        repository.removeAutomation(id)
    }

    /**
     * Executes the toggle automation operation.
     *
     * @param id [String] Target id.
     */
    fun toggleAutomation(id: String) {
        repository.getAutomation(id)?.let { automation ->
            repository.updateAutomation(automation.copy(isEnabled = !automation.isEnabled))
        }
    }

    /**
     * Runs the automation's action(s) immediately, without waiting for its trigger/state.
     *
     * @param automation [Automation] Target automation.
     */
    fun testAutomation(automation: Automation) {
        val context = getApplication<Application>().applicationContext
        val actionsToTest = if (automation.type == Automation.Type.STATE || automation.type == Automation.Type.APP) {
            listOfNotNull(automation.entryAction)
        } else {
            automation.actions
        }
        viewModelScope.launch {
            actionsToTest.forEach { action ->
                com.sameerasw.essentials.services.automation.executors.CombinedActionExecutor.execute(
                    context,
                    action
                )
            }
        }
    }

    /**
     * Executes the request gen ai suggestion operation.
     *
     * @param description [String] Target description.
     * @param context [Context?] Target context.
     */
    fun requestGenAISuggestion(description: String, context: Context? = null) {
        viewModelScope.launch {
            _genAIState.value = GenAIState.Loading
            val appContext = context ?: getApplication<Application>().applicationContext
            val result = GenAIAutomationService.suggestAutomation(description, appContext)
            _genAIState.value = result.fold(
                onSuccess = { GenAIState.Success(it) },
                onFailure = { GenAIState.Error(it.message ?: "Failed to generate suggestion") }
            )
        }
    }

    /**
     * Executes the dismiss gen ai suggestion operation.
     */
    fun dismissGenAISuggestion() {
        _genAIState.value = GenAIState.Idle
    }

    /**
     * Executes the confirm gen ai suggestion operation.
     *
     * @param suggestion [AutomationSuggestion] Target suggestion.
     */
    fun confirmGenAISuggestion(suggestion: AutomationSuggestion) {
        val automation = mapSuggestionToAutomation(suggestion)
        repository.addAutomation(automation)
        _genAIState.value = GenAIState.Idle
    }

    private fun mapSuggestionToAutomation(suggestion: AutomationSuggestion): Automation {
        val id = java.util.UUID.randomUUID().toString()
        val type = when (suggestion.type.uppercase()) {
            "STATE" -> Automation.Type.STATE
            "APP" -> Automation.Type.APP
            else -> Automation.Type.TRIGGER
        }

        val trigger = when (suggestion.triggerType) {
            "ScreenOff" -> Trigger.ScreenOff
            "ScreenOn" -> Trigger.ScreenOn
            "DeviceUnlock" -> Trigger.DeviceUnlock
            "ChargerConnected" -> Trigger.ChargerConnected
            "ChargerDisconnected" -> Trigger.ChargerDisconnected
            "PowerSavingOn" -> Trigger.PowerSavingOn
            "PowerSavingOff" -> Trigger.PowerSavingOff
            "Schedule" -> Trigger.Schedule(
                hour = suggestion.hour ?: 0,
                minute = suggestion.minute ?: 0
            )

            "BluetoothConnected" -> Trigger.BluetoothConnected()
            "BluetoothDisconnected" -> Trigger.BluetoothDisconnected()
            "WifiConnected" -> Trigger.WifiConnected()
            "WifiDisconnected" -> Trigger.WifiDisconnected()
            else -> if (type == Automation.Type.TRIGGER) Trigger.ScreenOff else null
        }

        val state = when (suggestion.stateType) {
            "Charging" -> State.Charging
            "ScreenOn" -> State.ScreenOn
            "PowerSaving" -> State.PowerSaving
            "TimePeriod" -> State.TimePeriod(
                startHour = suggestion.hour ?: 0,
                startMinute = suggestion.minute ?: 0,
                endHour = suggestion.endHour ?: 0,
                endMinute = suggestion.endMinute ?: 0
            )

            else -> if (type == Automation.Type.STATE) State.Charging else null
        }

        val actions = suggestion.actionTypes.mapNotNull { actionName ->
            when (actionName) {
                "HapticVibration" -> Action.HapticVibration
                "ShowNotification" -> Action.ShowNotification
                "RemoveNotification" -> Action.RemoveNotification
                "TurnOnFlashlight" -> Action.TurnOnFlashlight
                "TurnOffFlashlight" -> Action.TurnOffFlashlight
                "ToggleFlashlight" -> Action.ToggleFlashlight
                "DimWallpaper" -> Action.DimWallpaper(
                    dimAmount = suggestion.dimWallpaperAmount ?: 0.5f
                )

                "DeviceEffects" -> Action.DeviceEffects()
                "SoundMode" -> {
                    val mode = when (suggestion.soundMode?.uppercase()) {
                        "VIBRATE" -> Action.SoundModeType.VIBRATE
                        "SILENT" -> Action.SoundModeType.SILENT
                        else -> Action.SoundModeType.SOUND
                    }
                    Action.SoundMode(mode = mode)
                }

                "TurnOnLowPower" -> Action.TurnOnLowPower
                "TurnOffLowPower" -> Action.TurnOffLowPower
                "ScreenOff" -> Action.ScreenOff()
                "MediaPlayPause" -> Action.MediaPlayPause
                "MediaNext" -> Action.MediaNext
                "MediaPrevious" -> Action.MediaPrevious
                "AIAssistant" -> Action.AIAssistant
                "TakeScreenshot" -> Action.TakeScreenshot
                "ToggleMediaVolume" -> Action.ToggleMediaVolume
                "LikeCurrentSong" -> Action.LikeCurrentSong
                "CircleToSearch" -> Action.CircleToSearch
                "PinApp" -> Action.PinApp
                "SometimesEssentials" -> Action.SometimesEssentials(
                    changeNotificationLighting = suggestion.notificationLightingEnabled != null,
                    notificationLightingEnabled = suggestion.notificationLightingEnabled ?: true,
                    changeFlashlightPulse = suggestion.flashlightPulseEnabled != null,
                    flashlightPulseEnabled = suggestion.flashlightPulseEnabled ?: true,
                    changeEssentialsOnDisplay = suggestion.essentialsOnDisplayMode != null,
                    essentialsOnDisplayMode = suggestion.essentialsOnDisplayMode ?: "On",
                    changeAlwaysOnDisplay = suggestion.alwaysOnDisplayMode != null,
                    alwaysOnDisplayMode = suggestion.alwaysOnDisplayMode ?: "On",
                    changeLockScreenClock = suggestion.lockScreenClockStyle != null,
                    lockScreenClockStyle = suggestion.lockScreenClockStyle ?: "DEFAULT",
                    changeSmartPixels = suggestion.smartPixelsEnabled != null,
                    smartPixelsEnabled = suggestion.smartPixelsEnabled ?: true
                )

                "FreezeTag" -> Action.FreezeTag(
                    mode = suggestion.freezeTagMode ?: "Freeze",
                    tagIds = suggestion.freezeTagIds
                )

                "Keyboard" -> Action.Keyboard(
                    packageName = suggestion.keyboard
                )

                else -> null
            }
        }

        return Automation(
            id = id,
            type = type,
            trigger = trigger,
            state = state,
            selectedApps = suggestion.selectedApps,
            actions = if (type == Automation.Type.TRIGGER) actions else emptyList(),
            entryAction = if (type == Automation.Type.STATE || type == Automation.Type.APP) actions.firstOrNull() else null,
            exitAction = if ((type == Automation.Type.STATE || type == Automation.Type.APP) && actions.size > 1) actions[1] else null,
            isEnabled = true
        )
    }
}
