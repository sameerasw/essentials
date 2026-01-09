package com.sameerasw.essentials.domain.model

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.runtime.Immutable

@Immutable
data class NotificationApp(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean,
    val icon: ImageBitmap,
    val isSystemApp: Boolean,
    val lastUpdated: Long
)
