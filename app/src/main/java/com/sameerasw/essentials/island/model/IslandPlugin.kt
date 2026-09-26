package com.sameerasw.essentials.island.model

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import com.sameerasw.essentials.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

class IslandPluginContext(
    val service: AccessibilityService,
    val settings: SettingsRepository,
    val scope: CoroutineScope,
    val mainHandler: Handler,
    val request: (PluginRequest) -> Unit,
    val isContentSuppressed: () -> Boolean,
    val currentStage: () -> IslandStage,
    val focusedKey: () -> String?,
)

interface IslandPlugin {
    val id: String
    val items: StateFlow<List<IslandItem>>

    val settingKeys: Set<String> get() = emptySet()

    fun start(context: IslandPluginContext)
    fun stop()
    fun refresh() {}
    fun onScreenStateChanged() = refresh()
    fun onUserInteraction(focusedKey: String?) {}
    fun onFocusChanged(stage: IslandStage, focusedKey: String?) {}
}
