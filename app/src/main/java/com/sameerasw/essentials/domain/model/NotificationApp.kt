package com.sameerasw.essentials.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class NotificationApp(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean,
    val icon: ImageBitmap,
    val isSystemApp: Boolean,
    val lastUpdated: Long
)
