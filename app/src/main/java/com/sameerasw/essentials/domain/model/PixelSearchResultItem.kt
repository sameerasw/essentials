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
        val isFrozen: Boolean = false,
    ) : PixelSearchResultItem()

    data class ContactItem(
        val id: String,
        val name: String,
        val phoneNumber: String?,
        val photoUri: String?,
    ) : PixelSearchResultItem()

    data class SystemSettingItem(
        val title: String,
        val subtitle: String,
        @DrawableRes val iconRes: Int,
        val intent: Intent,
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

    data class FileItem(
        val id: Long,
        val uri: android.net.Uri,
        val displayName: String,
        val mimeType: String,
        val sizeBytes: Long,
        val dateModified: Long,
        val isMedia: Boolean,
        val isImage: Boolean = mimeType.startsWith("image/", ignoreCase = true),
        val isGif: Boolean = mimeType.equals("image/gif", ignoreCase = true),
        val isVideo: Boolean = mimeType.startsWith("video/", ignoreCase = true),
        val durationMs: Long = 0L,
        val path: String? = null,
        @DrawableRes val iconRes: Int = com.sameerasw.essentials.R.drawable.rounded_description_24,
    ) : PixelSearchResultItem()
}
