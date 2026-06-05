package com.sameerasw.essentials.domain.model

data class ShutUpAppConfig(
    val packageName: String,
    val isEnabled: Boolean = true,
    val settings: List<AppSetting> = emptyList(),
    val attemptShizukuRestart: Boolean = false,
    val autoArchive: Boolean = false
)

val ShutUpAppConfig.disableDevOptions: Boolean
    get() = settings.any { it.key == "development_settings_enabled" && it.enabled }

val ShutUpAppConfig.disableUsbDebugging: Boolean
    get() = settings.any { it.key == "adb_enabled" && it.enabled }

val ShutUpAppConfig.disableWirelessDebugging: Boolean
    get() = settings.any { it.key == "adb_wifi_enabled" && it.enabled }

val ShutUpAppConfig.disableAccessibility: Boolean
    get() = settings.any { it.key == "accessibility_enabled" && it.enabled }

fun ShutUpAppConfig.copy(
    packageName: String = this.packageName,
    isEnabled: Boolean = this.isEnabled,
    attemptShizukuRestart: Boolean = this.attemptShizukuRestart,
    autoArchive: Boolean = this.autoArchive,
    disableDevOptions: Boolean = this.disableDevOptions,
    disableUsbDebugging: Boolean = this.disableUsbDebugging,
    disableWirelessDebugging: Boolean = this.disableWirelessDebugging,
    disableAccessibility: Boolean = this.disableAccessibility
): ShutUpAppConfig {
    val newList = settings.toMutableList()

    fun updateKey(key: String, label: String, enabled: Boolean) {
        val existing = newList.find { it.key == key }
        if (existing != null) {
            newList[newList.indexOf(existing)] = existing.copy(enabled = enabled)
        } else if (enabled) {
            val type = if (key == "accessibility_enabled") "SECURE" else "GLOBAL"
            newList.add(
                AppSetting(
                    label = label,
                    settingType = type,
                    key = key,
                    valueOnLaunch = "0",
                    valueOnRevert = "1",
                    enabled = true
                )
            )
        }
    }

    updateKey("development_settings_enabled", "Hide Developer Options", disableDevOptions)
    updateKey("adb_enabled", "Hide USB Debugging", disableUsbDebugging)
    updateKey("adb_wifi_enabled", "Hide Wireless Debugging", disableWirelessDebugging)
    updateKey("accessibility_enabled", "Hide Accessibility Services", disableAccessibility)

    return ShutUpAppConfig(
        packageName = packageName,
        isEnabled = isEnabled,
        settings = newList,
        attemptShizukuRestart = attemptShizukuRestart,
        autoArchive = autoArchive
    )
}

