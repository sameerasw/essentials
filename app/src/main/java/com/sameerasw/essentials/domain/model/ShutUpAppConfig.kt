/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: ShutUpAppConfig.kt
 * Description: Domain model and business logic entry for ShutUpAppConfig.kt.
 */

package com.sameerasw.essentials.domain.model

import androidx.annotation.Keep
import com.sameerasw.essentials.domain.diy.Action

data class ShutUpAppConfig(
    val packageName: String,
    val isEnabled: Boolean = true,
    val disableDevOptions: Boolean = true,
    val disableUsbDebugging: Boolean = true,
    val disableWirelessDebugging: Boolean = true,
    val disableAccessibility: Boolean = false,
    val autoArchive: Boolean = false,
    val restoreKeyboard: Boolean = false,
    // Nullable since Gson leaves it null for configs saved before custom settings existed
    val customSettings: List<ShutUpCustomSetting>? = null,
) {
    val activeCustomSettings: List<ShutUpCustomSetting>
        get() = customSettings.orEmpty().filter { it.isEnabled && it.key.isNotBlank() }
}

@Keep
data class ShutUpCustomSetting(
    val table: Action.SettingsTable = Action.SettingsTable.GLOBAL,
    val key: String = "",
    val valueOnLaunch: String = "",
    val valueOnRestore: String = "",
    val isEnabled: Boolean = true,
)
