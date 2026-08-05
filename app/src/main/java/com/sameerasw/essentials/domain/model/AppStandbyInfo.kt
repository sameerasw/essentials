package com.sameerasw.essentials.domain.model

import android.graphics.drawable.Drawable

data class AppStandbyInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val bucket: Int
)
