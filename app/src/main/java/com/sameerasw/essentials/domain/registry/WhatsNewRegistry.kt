/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Registries
 * File: WhatsNewRegistry.kt
 * Description: Per-release list of new features and settings, resolved up the feature hierarchy.
 */

package com.sameerasw.essentials.domain.registry

import com.sameerasw.essentials.BuildConfig
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.DIYTabs
import com.sameerasw.essentials.ui.features.display.ISLAND_DURATION_FEATURE_ID
import com.sameerasw.essentials.ui.features.display.ISLAND_PLACEMENT_FEATURE_ID
import com.sameerasw.essentials.ui.features.display.ISLAND_VISUALS_FEATURE_ID

data class WhatsNewItem(
    val featureId: String,
    val settingKey: String? = null,
)

object WhatsNewRegistry {
    const val SETTINGS = "Settings"
    const val MODES = "Modes"
    const val DIY = "DIY"
    const val CONFIG_SHARE = "config_share"

    fun tabId(tab: DIYTabs) = "tab:${tab.name}"

    private val pseudoParents =
        mapOf(
            SETTINGS to tabId(DIYTabs.SETTINGS),
            MODES to tabId(DIYTabs.MODES),
            DIY to tabId(DIYTabs.DIY),
        )

    val releases: Map<String, List<WhatsNewItem>> =
        mapOf(
            "22.0" to
                listOf(
                    WhatsNewItem(ISLAND_PLACEMENT_FEATURE_ID, "island_camera_gap_enabled"),
                    WhatsNewItem(ISLAND_PLACEMENT_FEATURE_ID, "island_bond_scale"),
                    WhatsNewItem(ISLAND_PLACEMENT_FEATURE_ID, "island_always_bond"),
                    WhatsNewItem(ISLAND_PLACEMENT_FEATURE_ID, "island_bond_keep_gap"),
                    WhatsNewItem(ISLAND_VISUALS_FEATURE_ID, "island_persistent_order"),
                    WhatsNewItem(ISLAND_VISUALS_FEATURE_ID, "island_backdrop_blur"),
                    WhatsNewItem(ISLAND_DURATION_FEATURE_ID, "island_timeout_after_marquee"),
                    WhatsNewItem("Island", "island_max_items"),
                    WhatsNewItem("Island", "island_show_calls"),
                    WhatsNewItem("Island", "island_show_time_battery"),
                    WhatsNewItem("Island", "island_show_battery"),
                    WhatsNewItem("Island", "island_show_calendar"),
                    WhatsNewItem("Island", "island_show_sound_mode"),
                    WhatsNewItem("Island", "island_show_network"),
                    WhatsNewItem("Island", "island_show_starlink"),
                    WhatsNewItem("Island", SettingsRepository.KEY_ISLAND_CALL_RING_SHAKE),
                    WhatsNewItem("Island", SettingsRepository.KEY_ISLAND_BATTERY_STATUS_ICON),
                    WhatsNewItem("Island", SettingsRepository.KEY_ISLAND_BATTERY_CHARGING_ANIM),
                    WhatsNewItem("Island", SettingsRepository.KEY_ISLAND_BATTERY_CRITICAL_ANIM),
                    WhatsNewItem("Island", SettingsRepository.KEY_ISLAND_CALENDAR_ICON_ONLY),
                    WhatsNewItem("Island", SettingsRepository.KEY_ISLAND_SOUND_MODE_VIBRATE_SHAKE),
                    WhatsNewItem("Island", SettingsRepository.KEY_ISLAND_NETWORK_VPN),
                    WhatsNewItem("Island", SettingsRepository.KEY_ISLAND_STARLINK_STATUS_COLOR),
                    WhatsNewItem("App Ops"),
                    WhatsNewItem("App volume", "app_volume_ignore_focus"),
                    WhatsNewItem("Shut-Up!"),
                    WhatsNewItem("Widget stack"),
                    WhatsNewItem(tabId(DIYTabs.FREEZE)),
                    WhatsNewItem(MODES),
                    WhatsNewItem(DIY),
                    WhatsNewItem(SETTINGS, "app_icon"),
                    WhatsNewItem(SETTINGS, "app_color"),
                    WhatsNewItem(SETTINGS, "ui_design"),
                    WhatsNewItem(SETTINGS, "system_font"),
                    WhatsNewItem(SETTINGS, "toolbar"),
                ),
        )

    val currentRelease: String
        get() = BuildConfig.VERSION_NAME.substringBefore('-')

    val currentItems: List<WhatsNewItem>
        get() = releases[currentRelease].orEmpty()

    val newFeatures: Set<String> by lazy {
        val parents = FeatureRegistry.ALL_FEATURES.associate { it.id to it.parentFeatureId }
        buildSet {
            currentItems.forEach { item ->
                var id: String? = item.featureId
                while (id != null && add(id)) {
                    id = parents[id] ?: pseudoParents[id]
                }
            }
        }
    }

    val newSettings: Set<Pair<String, String>> by lazy {
        currentItems.mapNotNull { item -> item.settingKey?.let { item.featureId to it } }.toSet()
    }
}
