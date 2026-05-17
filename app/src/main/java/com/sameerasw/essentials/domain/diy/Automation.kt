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
    @SerializedName("entryAction") val entryAction: Action? = null,
    @SerializedName("exitAction") val exitAction: Action? = null,
    @SerializedName("isEnabled") val isEnabled: Boolean = true,
    @SerializedName("selectedApps") val selectedApps: List<String> = emptyList()
) {
    @Keep
    enum class Type {
        @SerializedName("TRIGGER")
        TRIGGER,

        @SerializedName("STATE")
        STATE,

        @SerializedName("APP")
        APP
    }
}
