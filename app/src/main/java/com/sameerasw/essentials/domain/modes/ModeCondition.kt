/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeCondition.kt
 * Description: Conditions that turn a mode on while they hold, built on the DIY automation modules.
 */

package com.sameerasw.essentials.domain.modes

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.SerializedName
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.Trigger
import java.lang.reflect.Type
import com.sameerasw.essentials.domain.diy.State as DIYState

@Keep
sealed interface ModeCondition {
    @get:StringRes
    val title: Int

    @get:DrawableRes
    val icon: Int

    val permissions: List<String>
        get() = emptyList()

    @Keep
    data class Schedule(
        @SerializedName("period") val period: DIYState.TimePeriod = DIYState.TimePeriod(),
    ) : ModeCondition {
        override val title: Int get() = R.string.diy_state_time_period
        override val icon: Int get() = R.drawable.rounded_timelapse_24
    }

    @Keep
    data class Calendar(
        @SerializedName("event") val event: DIYState.CalendarEvent = DIYState.CalendarEvent(),
    ) : ModeCondition {
        override val title: Int get() = R.string.diy_state_calendar_event
        override val icon: Int get() = R.drawable.rounded_calendar_today_24
        override val permissions: List<String> get() = listOf("READ_CALENDAR")
    }

    @Keep
    data class Wifi(
        @SerializedName("ssid") val ssid: String = "",
    ) : ModeCondition {
        override val title: Int get() = R.string.diy_trigger_wifi_connected
        override val icon: Int get() = R.drawable.rounded_android_wifi_4_bar_plus_24
        override val permissions: List<String> get() = listOf("LOCATION")
    }

    @Keep
    data class Bluetooth(
        @SerializedName("deviceAddress") val deviceAddress: String = "",
        @SerializedName("deviceName") val deviceName: String = "",
    ) : ModeCondition {
        override val title: Int get() = R.string.diy_trigger_bluetooth_connected
        override val icon: Int get() = R.drawable.rounded_bluetooth_24
        override val permissions: List<String> get() = listOf("BLUETOOTH")
    }

    @Keep
    data class Place(
        @SerializedName("latitude") val latitude: Double = 0.0,
        @SerializedName("longitude") val longitude: Double = 0.0,
        @SerializedName("radiusMeters") val radiusMeters: Int = Trigger.DEFAULT_LOCATION_RADIUS_METERS,
        @SerializedName("placeName") val placeName: String = "",
    ) : ModeCondition {
        override val title: Int get() = R.string.mode_condition_place
        override val icon: Int get() = R.drawable.rounded_location_on_24
        override val permissions: List<String> get() = listOf("LOCATION", "BACKGROUND_LOCATION")
    }

    @Keep
    data object Charging : ModeCondition {
        override val title: Int = R.string.diy_state_charging
        override val icon: Int = R.drawable.rounded_charger_24
    }

    @Keep
    data object AndroidAuto : ModeCondition {
        override val title: Int = R.string.diy_trigger_android_auto_connected
        override val icon: Int = R.drawable.rounded_directions_car_24
    }

    // Stable while the condition is unchanged, so editing one condition doesn't disturb the others
    fun key(index: Int): String = "$index:${hashCode()}"

    fun toAutomations(
        modeId: String,
        key: String,
    ): List<Automation> {
        val on = Action.ModeConditionChanged(modeId, key, true)
        val off = Action.ModeConditionChanged(modeId, key, false)
        val prefix = "mode_condition:$modeId:$key"

        fun state(state: DIYState) =
            listOf(Automation(id = prefix, type = Automation.Type.STATE, state = state, entryActions = listOf(on), exitActions = listOf(off)))

        fun triggers(
            enter: Trigger,
            leave: Trigger,
        ) = listOf(
            Automation(id = "$prefix:on", type = Automation.Type.TRIGGER, trigger = enter, actions = listOf(on)),
            Automation(id = "$prefix:off", type = Automation.Type.TRIGGER, trigger = leave, actions = listOf(off)),
        )

        return when (this) {
            is Schedule -> state(period)
            is Calendar -> state(event)
            is Charging -> state(DIYState.Charging)
            is Wifi -> triggers(Trigger.WifiConnected(ssid), Trigger.WifiDisconnected(ssid))
            is Bluetooth -> triggers(Trigger.BluetoothConnected(deviceAddress, deviceName), Trigger.BluetoothDisconnected(deviceAddress, deviceName))
            is Place ->
                triggers(
                    Trigger.ArriveAtLocation(latitude, longitude, radiusMeters, placeName),
                    Trigger.LeaveLocation(latitude, longitude, radiusMeters, placeName),
                )
            is AndroidAuto -> triggers(Trigger.AndroidAutoConnected, Trigger.AndroidAutoDisconnected)
        }
    }

    object GsonAdapter : JsonSerializer<ModeCondition>, JsonDeserializer<ModeCondition> {
        override fun serialize(
            src: ModeCondition,
            typeOfSrc: Type,
            context: JsonSerializationContext,
        ): JsonElement =
            context.serialize(src, src::class.java).also {
                if (it.isJsonObject) it.asJsonObject.addProperty("type", src::class.simpleName)
            }

        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext,
        ): ModeCondition? {
            val typeName = json.asJsonObject.get("type")?.asString ?: return null
            val subClass = ModeCondition::class.sealedSubclasses.firstOrNull { it.simpleName == typeName } ?: return null
            return subClass.objectInstance ?: context.deserialize(json, subClass.java)
        }
    }
}
