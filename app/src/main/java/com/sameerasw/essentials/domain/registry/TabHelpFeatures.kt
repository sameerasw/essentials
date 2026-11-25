/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: TabHelpFeatures.kt
 * Description: Help entries for toolbar tabs, kept out of the feature registry so they never show in lists or search.
 */

package com.sameerasw.essentials.domain.registry

import android.content.Context
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.DIYTabs
import com.sameerasw.essentials.domain.model.Feature
import com.sameerasw.essentials.viewmodels.MainViewModel

object TabHelpFeatures {
    private class TabHelp(
        id: String,
        title: Int,
        iconRes: Int,
        description: Int,
        aboutDescription: Int,
        howToUse: Int? = null,
        permissionKeys: List<String> = emptyList(),
    ) : Feature(
            id = id,
            title = title,
            iconRes = iconRes,
            category = R.string.cat_tools,
            description = description,
            permissionKeys = permissionKeys,
            showToggle = false,
            hasMoreSettings = false,
            isVisibleInMain = false,
            aboutDescription = aboutDescription,
            howToUse = howToUse,
        ) {
        override fun isEnabled(viewModel: MainViewModel): Boolean = true

        override fun onToggle(
            viewModel: MainViewModel,
            context: Context,
            enabled: Boolean,
        ) {}
    }

    private val yourAndroid: Feature =
        TabHelp(
            id = "tab_your_android",
            title = R.string.tab_your_android,
            iconRes = R.drawable.rounded_mobile_24,
            description = R.string.toolbar_tab_your_android_desc,
            aboutDescription = R.string.about_desc_tab_your_android,
        )

    private val diy: Feature =
        TabHelp(
            id = "tab_diy",
            title = R.string.tab_diy,
            iconRes = R.drawable.rounded_experiment_24,
            description = R.string.tab_diy_subtitle,
            aboutDescription = R.string.about_desc_tab_diy,
            howToUse = R.string.how_to_use_tab_diy,
        )

    private val modes: Feature =
        TabHelp(
            id = "tab_modes",
            title = R.string.tab_modes,
            iconRes = R.drawable.rounded_photo_filter_24,
            description = R.string.tab_modes_desc,
            aboutDescription = R.string.about_desc_tab_modes,
            howToUse = R.string.how_to_use_tab_modes,
            permissionKeys = listOf("NOTIFICATION_POLICY", "SHIZUKU"),
        )

    fun forTab(tab: DIYTabs): Feature? =
        when (tab) {
            DIYTabs.FREEZE -> FeatureRegistry.ALL_FEATURES.find { it.id == "Freeze" }
            DIYTabs.YOUR_ANDROID -> yourAndroid
            DIYTabs.DIY -> diy
            DIYTabs.MODES -> modes
            else -> null
        }
}
