package com.sameerasw.essentials.domain.diy

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object DIYRepository {
    private val _automations = MutableStateFlow<List<Automation>>(emptyList())
    val automations: Flow<List<Automation>> = _automations.asStateFlow()

    init {
        // Add mock data
        _automations.value = listOf(
            Automation(
                id = "1",
                type = Automation.Type.TRIGGER,
                trigger = Trigger.ScreenOff,
                actions = listOf(Action.HapticVibration)
            ),
            Automation(
                id = "2",
                type = Automation.Type.STATE,
                state = State.Charging,
                entryAction = Action.ShowNotification,
                exitAction = Action.RemoveNotification
            )
        )
    }
}
