package com.sameerasw.essentials.domain.configshare

import com.sameerasw.essentials.R

object ConfigShareRegistry {
    const val SECTION_PLACEMENT = "placement"
    const val SECTION_VISUALS = "visuals"
    const val SECTION_PLUGINS = "plugins"
    const val SECTION_INTERACTIONS = "interactions"
    const val SECTION_TIMING = "timing"
    const val SECTION_DEVICE = "device"

    private val islandBlocked =
        setOf(
            "enabled",
            "expanded_dark_notice_dismissed",
            "providers",
            "providers_hidden",
            "media_excluded_apps",
            "timers_selected_apps",
            "notif_filter_apps_list",
            "devices_battery_order",
            "calendar_hidden_events",
            "calendar_icons",
        )

    private val islandPlacement =
        setOf(
            "use_auto_detect", "camera_offset_x", "camera_offset_y", "camera_size", "max_width",
            "cutout_gap", "padding_around", "landscape_top_spacing", "camera_position",
            "orientation_profiles", "hide_portrait", "hide_landscape", "keep_on_landscape",
            "bond_edge", "balance_compact", "secondary_bubble", "secondary_bubble_side",
            "statusbar_aware_width", "statusbar_ignore_notifications", "max_items",
            "hide_status_bar_insets", "persistent_order",
        )

    private val islandInteractions =
        setOf(
            "long_press_action", "slide_mode", "slide_track", "slide_invert_direction",
            "always_gestures", "dismiss_on_outside", "gesture_hints", "dismiss_direction",
            "notif_tap_to_open", "tap_to_open_expanded", "time_tap_opens_clock",
            "otp_tap_to_copy", "otp_dismiss_after_copy", "disable_interactions_in_fullscreen",
            "hide_in_owner_app", "hide_media_in_owner_app", "hide_on_shade", "keep_in_fullscreen",
        )

    private val islandVisualPrefixes =
        listOf(
            "show_glow", "glow_", "border_outline_", "pulse_shadow_", "transparency", "backdrop_blur",
            "progressive_blur", "themed_", "icon_color_", "icon_material_you", "neural_expressive",
            "battery_idle_color", "expanded_roundness", "expanded_scale",
        )

    private val islandVisuals =
        setOf(
            "notif_themed_bubble", "font_scale", "animation_speed", "prefer_still", "chips",
            "time_chip", "line_stage_enabled", "preview_ring", "preview_stage",
        )

    private fun islandSectionOf(key: String): String? {
        if (!key.startsWith("island_")) return null
        if (key.contains("@")) {
            val base = key.removePrefix("island_").substringBefore('@')
            return if (base in islandBlocked || base.endsWith("_reset_v1")) null else SECTION_DEVICE
        }
        val base = key.removePrefix("island_")
        if (base in islandBlocked || base.endsWith("_reset_v1")) return null
        if (base.endsWith("_apps_list") || base.endsWith("_selected_apps") || base.endsWith("_excluded_apps")) return null
        return when {
            base.endsWith("timeout_ms") || base == "timeout_after_marquee" ||
                base.contains("peek_duration") || base == "alarm_window_hours" ||
                base == "calendar_priority_minutes" || base.startsWith("catch_up_") -> SECTION_TIMING
            base in islandPlacement ||
                (base.startsWith("expanded_") && base != "expanded_roundness" && base != "expanded_scale") -> SECTION_PLACEMENT
            base in islandInteractions -> SECTION_INTERACTIONS
            base in islandVisuals || islandVisualPrefixes.any { base.startsWith(it) } -> SECTION_VISUALS
            else -> SECTION_PLUGINS
        }
    }

    private val descriptors: Map<String, ConfigShareDescriptor> =
        mapOf(
            "Island" to
                ConfigShareDescriptor(
                    featureId = "Island",
                    prefixes = listOf("island_"),
                    sections =
                        listOf(
                            ConfigSection(SECTION_PLACEMENT, R.string.config_share_section_placement),
                            ConfigSection(SECTION_VISUALS, R.string.config_share_section_visuals),
                            ConfigSection(SECTION_PLUGINS, R.string.config_share_section_plugins),
                            ConfigSection(SECTION_INTERACTIONS, R.string.config_share_section_interactions),
                            ConfigSection(SECTION_TIMING, R.string.config_share_section_timing),
                            ConfigSection(SECTION_DEVICE, R.string.config_share_section_device, defaultSelected = false),
                        ),
                    sectionOf = ::islandSectionOf,
                ),
        )

    fun descriptorFor(featureId: String): ConfigShareDescriptor? = descriptors[featureId]
}
