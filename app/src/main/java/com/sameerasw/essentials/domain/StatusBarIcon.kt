package com.sameerasw.essentials.domain

/**
 * Data class representing a status bar icon with all its variants across different OEMs
 * @param id Unique identifier for this icon type
 * @param displayName Display name for UI
 * @param blacklistNames List of blacklist names this icon can have across different ROMs/OEMs
 * @param defaultVisible Whether this icon should be visible by default
 * @param preferencesKey Key to store preference in SharedPreferences
 */
data class StatusBarIcon(
    val id: String,
    val displayName: String,
    val blacklistNames: List<String>,
    val defaultVisible: Boolean = true,
    val preferencesKey: String = "icon_${id}_visible"
)

/**
 * Defines all supported status bar icons with their variants
 * This centralizes all icon configurations and reduces code duplication
 */
object StatusBarIconRegistry {

    // Build the complete list of all supported icons
    val ALL_ICONS = listOf(
        // Network Related
        StatusBarIcon(
            id = "mobile_data",
            displayName = "Mobile Data",
            blacklistNames = listOf("mobile", "data_connection"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "wifi",
            displayName = "WiFi",
            blacklistNames = listOf("wifi", "wifi_oxygen"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "vpn",
            displayName = "VPN",
            blacklistNames = listOf("vpn"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "airplane_mode",
            displayName = "Airplane Mode",
            blacklistNames = listOf("airplane", "airplane_mode"),
            defaultVisible = true
        ),

        // VoLTE / WiFi Calling
        StatusBarIcon(
            id = "volte",
            displayName = "VoLTE",
            blacklistNames = listOf("volte", "ims_volte", "vowifi", "wifi_calling", "volte_call"),
            defaultVisible = true
        ),

        // Audio
        StatusBarIcon(
            id = "headset",
            displayName = "Headset",
            blacklistNames = listOf("headset", "earphone"),
            defaultVisible = false
        ),
        StatusBarIcon(
            id = "volume",
            displayName = "Volume",
            blacklistNames = listOf("volume", "mute"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "speakerphone",
            displayName = "Speakerphone",
            blacklistNames = listOf("speakerphone"),
            defaultVisible = true
        ),

        // Connectivity & Sync
        StatusBarIcon(
            id = "bluetooth",
            displayName = "Bluetooth",
            blacklistNames = listOf("bluetooth", "bluetooth_handsfree_battery"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "hotspot",
            displayName = "Hotspot",
            blacklistNames = listOf("hotspot", "wifi_ap"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "nfc",
            displayName = "NFC",
            blacklistNames = listOf("nfc", "nfc_on", "nfclock"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "cast",
            displayName = "Cast",
            blacklistNames = listOf("cast"),
            defaultVisible = true
        ),

        // System Status
        StatusBarIcon(
            id = "battery",
            displayName = "Battery",
            blacklistNames = listOf("battery"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "clock",
            displayName = "Clock",
            blacklistNames = listOf("clock"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "alarm",
            displayName = "Alarm",
            blacklistNames = listOf("alarm", "alarm_clock"),
            defaultVisible = false
        ),
        StatusBarIcon(
            id = "rotate",
            displayName = "Rotation Lock",
            blacklistNames = listOf("rotate"),
            defaultVisible = false
        ),

        // Do Not Disturb & Zen
        StatusBarIcon(
            id = "dnd",
            displayName = "Do Not Disturb",
            blacklistNames = listOf("do_not_disturb", "dnd", "zen"),
            defaultVisible = true
        ),

        // Power & Data Saving
        StatusBarIcon(
            id = "data_saver",
            displayName = "Data Saver",
            blacklistNames = listOf("data_saver"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "power_saver",
            displayName = "Power Saver",
            blacklistNames = listOf("power_saver", "powersavingmode"),
            defaultVisible = true
        ),

        // Other
        StatusBarIcon(
            id = "location",
            displayName = "Location",
            blacklistNames = listOf("location", "gps", "lbs"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "managed_profile",
            displayName = "Managed Profile",
            blacklistNames = listOf("managed_profile"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "sync",
            displayName = "Sync",
            blacklistNames = listOf("sync_active", "sync_failing"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "secure",
            displayName = "Secure",
            blacklistNames = listOf("secure", "su"),
            defaultVisible = true
        ),

        // Samsung
        StatusBarIcon(
            id = "knox_container",
            displayName = "(Samsung) KNOX Container",
            blacklistNames = listOf("knox_container"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "smart_network",
            displayName = "(Samsung) Smart Network",
            blacklistNames = listOf("smart_network"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "smart_bonding",
            displayName = "(Samsung) Smart Bonding",
            blacklistNames = listOf("smart_bonding"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "private_mode",
            displayName = "(Samsung) Private Mode",
            blacklistNames = listOf("private_mode"),
            defaultVisible = true
        ),
        StatusBarIcon(
            id = "wifi_p2p",
            displayName = "(Samsung) WiFi P2P",
            blacklistNames = listOf("wifi_p2p"),
            defaultVisible = true
        ),
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

