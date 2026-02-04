package com.sameerasw.essentials.domain

import com.sameerasw.essentials.R

/**
 * Data class representing a status bar icon with all its variants across different OEMs
 * @param id Unique identifier for this icon type
 * @param displayName Display name for UI
 * @param blacklistNames List of blacklist names this icon can have across different ROMs/OEMs
 * @param defaultVisible Whether this icon should be visible by default
 * @param preferencesKey Key to store preference in SharedPreferences
 * @param category Category for UI grouping
 * @param iconRes Optional icon resource for UI
 */
data class StatusBarIcon(
    val id: String,
    val displayNameRes: Int,
    val blacklistNames: List<String>,
    val defaultVisible: Boolean = true,
    val preferencesKey: String = "icon_${id}_visible",
    val categoryRes: Int = R.string.status_bar_category_oem_specific,
    val iconRes: Int? = null
)

/**
 * Defines all supported status bar icons with their variants
 * This centralizes all icon configurations and reduces code duplication
 */
object StatusBarIconRegistry {

    val CAT_CONNECTIVITY = R.string.status_bar_category_connectivity
    val CAT_PHONE_NETWORK = R.string.status_bar_category_phone_network
    val CAT_AUDIO_MEDIA = R.string.status_bar_category_audio_media
    val CAT_SYSTEM_STATUS = R.string.status_bar_category_system_status
    val CAT_OEM_SPECIFIC = R.string.status_bar_category_oem_specific

    // Build the complete list of all supported icons
    val ALL_ICONS = listOf(
        // --- Connectivity ---
        StatusBarIcon(
            id = "wifi",
            displayNameRes = R.string.icon_wifi,
            blacklistNames = listOf("wifi", "wifi_oxygen", "wifi_p2p"),
            categoryRes = CAT_CONNECTIVITY,
            iconRes = R.drawable.rounded_android_wifi_3_bar_24
        ),
        StatusBarIcon(
            id = "bluetooth",
            displayNameRes = R.string.icon_bluetooth,
            blacklistNames = listOf("bluetooth", "bluetooth_handsfree_battery", "ble_unlock_mode"),
            categoryRes = CAT_CONNECTIVITY,
            iconRes = R.drawable.rounded_bluetooth_24
        ),
        StatusBarIcon(
            id = "nfc",
            displayNameRes = R.string.icon_nfc,
            blacklistNames = listOf("nfc", "nfc_on", "nfclock", "felica_lock"),
            categoryRes = CAT_CONNECTIVITY,
            iconRes = R.drawable.rounded_nfc_24
        ),
        StatusBarIcon(
            id = "vpn",
            displayNameRes = R.string.icon_vpn,
            blacklistNames = listOf("vpn"),
            categoryRes = CAT_CONNECTIVITY,
            iconRes = R.drawable.rounded_vpn_key_24
        ),
        StatusBarIcon(
            id = "airplane_mode",
            displayNameRes = R.string.icon_airplane_mode,
            blacklistNames = listOf("airplane", "airplane_mode"),
            categoryRes = CAT_CONNECTIVITY,
            iconRes = R.drawable.rounded_flight_24
        ),
        StatusBarIcon(
            id = "hotspot",
            displayNameRes = R.string.icon_hotspot,
            blacklistNames = listOf("hotspot", "wifi_ap"),
            categoryRes = CAT_CONNECTIVITY,
            iconRes = R.drawable.rounded_wifi_tethering_24
        ),
        StatusBarIcon(
            id = "cast",
            displayNameRes = R.string.icon_cast,
            blacklistNames = listOf("cast"),
            categoryRes = CAT_CONNECTIVITY,
            iconRes = R.drawable.rounded_cast_24
        ),
        StatusBarIcon(
            id = "ethernet",
            displayNameRes = R.string.icon_ethernet,
            blacklistNames = listOf("ethernet"),
            categoryRes = CAT_CONNECTIVITY,
            iconRes = R.drawable.rounded_lan_24
        ),

        // --- Phone & Network ---
        StatusBarIcon(
            id = "mobile_data",
            displayNameRes = R.string.icon_mobile_data,
            blacklistNames = listOf("mobile", "data_connection"),
            categoryRes = CAT_PHONE_NETWORK,
            iconRes = R.drawable.rounded_android_cell_dual_4_bar_24
        ),
        StatusBarIcon(
            id = "phone_signal",
            displayNameRes = R.string.icon_phone_signal,
            blacklistNames = listOf("phone_signal", "phone_signal_second_stub", "phone_evdo_signal", "cdma_eri", "wimax"),
            categoryRes = CAT_PHONE_NETWORK,
            iconRes = R.drawable.rounded_signal_cellular_alt_24
        ),
        StatusBarIcon(
            id = "volte",
            displayNameRes = R.string.icon_volte,
            blacklistNames = listOf("volte", "ims_volte", "volte_call", "unicom_call"),
            categoryRes = CAT_PHONE_NETWORK,
            iconRes = R.drawable.rounded_wifi_calling_bar_3_24
        ),
        StatusBarIcon(
            id = "wifi_calling",
            displayNameRes = R.string.icon_wifi_calling,
            blacklistNames = listOf("wifi_calling", "vowifi"),
            categoryRes = CAT_PHONE_NETWORK,
            iconRes = R.drawable.rounded_wifi_calling_bar_3_24
        ),
        StatusBarIcon(
            id = "remote_call",
            displayNameRes = R.string.icon_call_status,
            blacklistNames = listOf("remote_call", "call_record", "answering_memo", "missed_call"),
            categoryRes = CAT_PHONE_NETWORK,
            iconRes = R.drawable.rounded_call_log_24
        ),
        StatusBarIcon(
            id = "tty",
            displayNameRes = R.string.icon_tty,
            blacklistNames = listOf("tty"),
            categoryRes = CAT_PHONE_NETWORK,
            iconRes = R.drawable.rounded_settings_accessibility_24
        ),

        // --- Audio & Media ---
        StatusBarIcon(
            id = "volume",
            displayNameRes = R.string.icon_volume,
            blacklistNames = listOf("volume", "mute", "quiet"),
            categoryRes = CAT_AUDIO_MEDIA,
            iconRes = R.drawable.rounded_volume_up_24
        ),
        StatusBarIcon(
            id = "headset",
            displayNameRes = R.string.icon_headset,
            blacklistNames = listOf("headset", "earphone"),
            defaultVisible = false,
            categoryRes = CAT_AUDIO_MEDIA,
            iconRes = R.drawable.rounded_headset_mic_24
        ),
        StatusBarIcon(
            id = "speakerphone",
            displayNameRes = R.string.icon_speakerphone,
            blacklistNames = listOf("speakerphone"),
            categoryRes = CAT_AUDIO_MEDIA,
            iconRes = R.drawable.rounded_volume_up_24
        ),
        StatusBarIcon(
            id = "dmb",
            displayNameRes = R.string.icon_dmb,
            blacklistNames = listOf("dmb"),
            categoryRes = CAT_AUDIO_MEDIA,
            iconRes = R.drawable.rounded_play_arrow_24
        ),

        // --- System Status ---
        StatusBarIcon(
            id = "clock",
            displayNameRes = R.string.icon_clock,
            blacklistNames = listOf("clock"),
            categoryRes = CAT_SYSTEM_STATUS,
            iconRes = R.drawable.rounded_nest_clock_farsight_analog_24
        ),
        StatusBarIcon(
            id = "ime",
            displayNameRes = R.string.icon_ime,
            blacklistNames = listOf("ime"),
            categoryRes = CAT_SYSTEM_STATUS,
            iconRes = R.drawable.rounded_settings_accessibility_24
        ),
        StatusBarIcon(
            id = "alarm",
            displayNameRes = R.string.icon_alarm,
            blacklistNames = listOf("alarm", "alarm_clock"),
            defaultVisible = false,
            categoryRes = CAT_SYSTEM_STATUS,
            iconRes = R.drawable.rounded_alarm_24
        ),
        StatusBarIcon(
            id = "battery",
            displayNameRes = R.string.icon_battery,
            blacklistNames = listOf("battery"),
            categoryRes = CAT_SYSTEM_STATUS,
            iconRes = R.drawable.rounded_battery_android_frame_6_24
        ),
        StatusBarIcon(
            id = "power_saver",
            displayNameRes = R.string.icon_power_saving,
            blacklistNames = listOf("power_saver", "powersavingmode"),
            categoryRes = CAT_SYSTEM_STATUS,
            iconRes = R.drawable.rounded_battery_android_frame_plus_24
        ),
        StatusBarIcon(
            id = "data_saver",
            displayNameRes = R.string.icon_data_saver,
            blacklistNames = listOf("data_saver"),
            categoryRes = CAT_SYSTEM_STATUS,
            iconRes = R.drawable.rounded_data_saver_on_24
        ),
        StatusBarIcon(
            id = "rotate",
            displayNameRes = R.string.icon_rotation_lock,
            blacklistNames = listOf("rotate"),
            defaultVisible = false,
            categoryRes = CAT_SYSTEM_STATUS,
            iconRes = R.drawable.rounded_mobile_rotate_24
        ),
        StatusBarIcon(
            id = "location",
            displayNameRes = R.string.icon_location,
            blacklistNames = listOf("location", "gps", "lbs"),
            categoryRes = CAT_SYSTEM_STATUS,
            iconRes = R.drawable.rounded_navigation_24
        ),
        StatusBarIcon(
            id = "sync",
            displayNameRes = R.string.icon_sync,
            blacklistNames = listOf("sync_active", "sync_failing"),
            categoryRes = CAT_SYSTEM_STATUS,
            iconRes = R.drawable.rounded_sync_24
        ),
        StatusBarIcon(
            id = "managed_profile",
            displayNameRes = R.string.icon_managed_profile,
            blacklistNames = listOf("managed_profile"),
            categoryRes = CAT_SYSTEM_STATUS,
            iconRes = R.drawable.rounded_security_24
        ),
        StatusBarIcon(
            id = "dnd",
            displayNameRes = R.string.icon_dnd,
            blacklistNames = listOf("do_not_disturb", "dnd", "zen"),
            categoryRes = CAT_SYSTEM_STATUS,
            iconRes = R.drawable.rounded_do_not_disturb_on_24
        ),
        StatusBarIcon(
            id = "privacy",
            displayNameRes = R.string.icon_privacy,
            blacklistNames = listOf("privacy_mode", "private_mode", "knox_container"),
            categoryRes = CAT_SYSTEM_STATUS,
            iconRes = R.drawable.rounded_security_24
        ),
        StatusBarIcon(
            id = "secure",
            displayNameRes = R.string.icon_security_status,
            blacklistNames = listOf("secure", "su"),
            categoryRes = CAT_SYSTEM_STATUS,
            iconRes = R.drawable.rounded_security_24
        ),

        // --- OEM Specific ---
        StatusBarIcon(
            id = "otg",
            displayNameRes = R.string.icon_otg,
            blacklistNames = listOf("otg_mouse", "otg_keyboard"),
            categoryRes = CAT_OEM_SPECIFIC,
            iconRes = R.drawable.rounded_settings_accessibility_24
        ),
        StatusBarIcon(
            id = "samsung_smart",
            displayNameRes = R.string.icon_samsung_smart,
            blacklistNames = listOf("glove", "gesture", "smart_scroll", "face", "smart_network", "smart_bonding"),
            categoryRes = CAT_OEM_SPECIFIC,
            iconRes = R.drawable.rounded_fiber_smart_record_24
        ),
        StatusBarIcon(
            id = "samsung_services",
            displayNameRes = R.string.icon_samsung_services,
            blacklistNames = listOf("wearable_gear", "femtoicon", "com.samsung.rcs", "toddler", "keyguard_wakeup", "safezone"),
            categoryRes = CAT_OEM_SPECIFIC,
            iconRes = R.drawable.rounded_interests_24
        )
    )

    // Create a map for quick lookup
    private val iconMap = ALL_ICONS.associateBy { it.id }
    private val blacklistNameMap = ALL_ICONS.flatMap { icon ->
        icon.blacklistNames.map { name -> name to icon }
    }.toMap()

    fun getIconById(id: String): StatusBarIcon? = iconMap[id]

    fun getIconByBlacklistName(blacklistName: String): StatusBarIcon? = blacklistNameMap[blacklistName]

    /**
     * Get all blacklist names that should be hidden
     * @param iconVisibilities Map of icon IDs to their visibility state
     * @return Set of blacklist names that should be in the blacklist
     */
    fun getBlacklistNames(iconVisibilities: Map<String, Boolean>): Set<String> {
        val blacklistNames = mutableSetOf<String>()

        for (icon in ALL_ICONS) {
            val isVisible = iconVisibilities[icon.id] ?: icon.defaultVisible
            // If icon is not visible, add all its blacklist names
            if (!isVisible) {
                blacklistNames.addAll(icon.blacklistNames)
            }
        }

        return blacklistNames
    }

    /**
     * Get visibility state for all icons based on current blacklist
     * @param blacklist Comma-separated blacklist string from settings
     * @return Map of icon ID to visibility state
     */
    fun getVisibilityState(blacklist: String?): Map<String, Boolean> {
        val blacklistSet = blacklist?.split(",")?.toSet() ?: emptySet()
        val visibilities = mutableMapOf<String, Boolean>()

        for (icon in ALL_ICONS) {
            // Check if any of this icon's blacklist names are in the blacklist
            val isHidden = icon.blacklistNames.any { it in blacklistSet }
            visibilities[icon.id] = !isHidden
        }

        return visibilities
    }
}

