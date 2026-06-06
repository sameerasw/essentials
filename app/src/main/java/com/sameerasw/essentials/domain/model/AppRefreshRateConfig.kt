package com.sameerasw.essentials.domain.model

data class AppRefreshRateConfig(
    val packageName: String,
    val refreshRate: Float,
    val isFixed: Boolean = false,
    val isEnabled: Boolean = true,
    val landscapeRefreshRate: Float? = null,
    val onlyOnMediaPlaying: Boolean = false
)
