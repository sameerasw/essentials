/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: PixelSearchResultItem.kt
 * Description: Data classes representing categorized results for Pixel search.
 */

package com.sameerasw.essentials.domain.model

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap

sealed class PixelSearchResultItem {
    data class AppItem(
        val appName: String,
        val packageName: String,
        val icon: ImageBitmap?,
        val isSystemApp: Boolean = false,
    ) : PixelSearchResultItem()

    data class ContactItem(
        val id: String,
        val name: String,
        val phoneNumber: String?,
        val photoUri: String?,
    ) : PixelSearchResultItem()

    data class SettingItem(
        val searchableItem: SearchableItem,
    ) : PixelSearchResultItem()

    data class ShortcutItem(
        val id: String,
        val label: String,
        val subtitle: String,
        @DrawableRes val iconRes: Int,
        val intent: Intent,
    ) : PixelSearchResultItem()

    data class WebItem(
        val query: String,
    ) : PixelSearchResultItem()
}
