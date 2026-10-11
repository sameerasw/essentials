/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: ShutUpAppConfig.kt
 * Description: Domain model and business logic entry for ShutUpAppConfig.kt.
 */

package com.sameerasw.essentials.domain.model

data class ShutUpAppConfig(
    val packageName: String,
    val isEnabled: Boolean = true,
    val disableDevOptions: Boolean = true,
    val disableUsbDebugging: Boolean = true,
    val disableWirelessDebugging: Boolean = true,
    val disableAccessibility: Boolean = false,
    val autoArchive: Boolean = false,
    val restoreKeyboard: Boolean = false,
)
