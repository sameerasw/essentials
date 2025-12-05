package com.sameerasw.essentials.domain.model

import android.graphics.drawable.Drawable

data class NotificationApp(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean,
    val icon: Drawable,
    val isSystemApp: Boolean,
    val lastUpdated: Long
)
