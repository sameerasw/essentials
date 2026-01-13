package com.sameerasw.essentials.domain.diy

data class Automation(
    val id: String,
    val type: Type,
    val trigger: Trigger? = null,
    val state: State? = null,
    val actions: List<Action> = emptyList(),
    val entryAction: Action? = null,
    val exitAction: Action? = null
) {
    enum class Type {
        TRIGGER,
        STATE
    }
}
