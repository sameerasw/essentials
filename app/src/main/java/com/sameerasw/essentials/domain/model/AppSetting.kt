package com.sameerasw.essentials.domain.model

data class AppSetting(
    val enabled: Boolean = true,
    val settingType: String, // "GLOBAL", "SECURE", "SYSTEM"
    val key: String,
    val valueOnLaunch: String,
    val valueOnRevert: String,
    val label: String
)
