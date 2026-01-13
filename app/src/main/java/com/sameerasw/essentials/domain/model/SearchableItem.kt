package com.sameerasw.essentials.domain.model

import androidx.annotation.DrawableRes

data class SearchableItem(
    val title: String,
    val description: String,
    val category: String,
    @DrawableRes val icon: Int? = null,
    val featureKey: String,
    val keywords: List<String> = emptyList(),
    val parentFeature: String? = null,
    val targetSettingHighlightKey: String? = null,
    val titleRes: Int? = null,
    val descriptionRes: Int? = null,
    val isBeta: Boolean = false
)
