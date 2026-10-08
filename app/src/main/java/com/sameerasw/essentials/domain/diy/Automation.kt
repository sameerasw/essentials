/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: Automation.kt
 * Description: Domain model and business logic entry for Automation.kt.
 */

package com.sameerasw.essentials.domain.diy

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Automation(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: Type,
    @SerializedName("trigger") val trigger: Trigger? = null,
    @SerializedName("state") val state: State? = null,
    @SerializedName("actions") val actions: List<Action> = emptyList(),
    // Single actions saved before sequences were supported; new saves use entryActions and exitActions
    @SerializedName("entryAction") val entryAction: Action? = null,
    @SerializedName("exitAction") val exitAction: Action? = null,
    @SerializedName("isEnabled") val isEnabled: Boolean = true,
    @SerializedName("selectedApps") val selectedApps: List<String> = emptyList(),
    @SerializedName("entryActions") val entryActions: List<Action>? = null,
    @SerializedName("exitActions") val exitActions: List<Action>? = null,
) {
    // Gson leaves missing lists null despite the defaults, so older automations come back as null
    @Suppress("USELESS_ELVIS")
    val actionList: List<Action> get() = actions ?: emptyList()

    val entryActionList: List<Action> get() = entryActions ?: listOfNotNull(entryAction)

    val exitActionList: List<Action> get() = exitActions ?: listOfNotNull(exitAction)

    @Keep
    enum class Type {
        @SerializedName("TRIGGER")
        TRIGGER,

        @SerializedName("STATE")
        STATE,

        @SerializedName("APP")
        APP,

        @SerializedName("ACTION_SHORTCUT")
        ACTION_SHORTCUT,

        @SerializedName("ACCESSIBILITY_SHORTCUT")
        ACCESSIBILITY_SHORTCUT,

        @SerializedName("ACCESSIBILITY_SHORTCUT_1")
        ACCESSIBILITY_SHORTCUT_1,

        @SerializedName("ACCESSIBILITY_SHORTCUT_2")
        ACCESSIBILITY_SHORTCUT_2,

        @SerializedName("ACCESSIBILITY_SHORTCUT_3")
        ACCESSIBILITY_SHORTCUT_3,

        @SerializedName("PIXEL_SEARCHBAR")
        PIXEL_SEARCHBAR,
    }
}
