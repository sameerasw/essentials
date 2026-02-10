package com.sameerasw.essentials.domain.registry

import com.sameerasw.essentials.R

object PermissionRegistry {
    private val registry = mutableMapOf<String, MutableList<Int>>()

    fun register(permissionKey: String, featureTitleRes: Int) {
        val list = registry.getOrPut(permissionKey) { mutableListOf() }
        if (!list.contains(featureTitleRes)) list.add(featureTitleRes)
    }

    fun getFeatures(permissionKey: String): List<Int> = registry[permissionKey]?.toList() ?: emptyList()
}

// Register existing dependencies
fun initPermissionRegistry() {
    // Accessibility permission
    PermissionRegistry.register("ACCESSIBILITY", R.string.feat_screen_off_widget_title)
    PermissionRegistry.register("ACCESSIBILITY", R.string.feat_notification_lighting_title)
    PermissionRegistry.register("ACCESSIBILITY", R.string.feat_dynamic_night_light_title)
    PermissionRegistry.register("ACCESSIBILITY", R.string.feat_screen_locked_security_title)
    PermissionRegistry.register("ACCESSIBILITY", R.string.feat_app_lock_title)
    PermissionRegistry.register("ACCESSIBILITY", R.string.feat_ambient_music_glance_title)

    // Write secure settings permission
    PermissionRegistry.register("WRITE_SECURE_SETTINGS", R.string.feat_statusbar_icons_title)
    PermissionRegistry.register("WRITE_SECURE_SETTINGS", R.string.feat_sound_mode_tile_title)
    PermissionRegistry.register("WRITE_SECURE_SETTINGS", R.string.feat_dynamic_night_light_title)
    PermissionRegistry.register("WRITE_SECURE_SETTINGS", R.string.feat_screen_locked_security_title)
    PermissionRegistry.register("WRITE_SECURE_SETTINGS", R.string.tile_developer_options)


    // Shizuku permission
    PermissionRegistry.register("SHIZUKU", R.string.feat_freeze_title)
    PermissionRegistry.register("SHIZUKU", R.string.feat_maps_power_saving_title)
    PermissionRegistry.register("USAGE_STATS", R.string.feat_freeze_title)
    PermissionRegistry.register("NOTIFICATION_LISTENER", R.string.feat_freeze_title)
    
    // Root permission
    PermissionRegistry.register("ROOT", R.string.feat_maps_power_saving_title)
    PermissionRegistry.register("ROOT", R.string.feat_freeze_title)
    PermissionRegistry.register("ROOT", R.string.feat_button_remap_title)

    // Notification listener permission
    PermissionRegistry.register("NOTIFICATION_LISTENER", R.string.feat_maps_power_saving_title)
    PermissionRegistry.register("NOTIFICATION_LISTENER", R.string.feat_notification_lighting_title)
    PermissionRegistry.register("NOTIFICATION_LISTENER", R.string.feat_call_vibrations_title)
    PermissionRegistry.register("NOTIFICATION_LISTENER", R.string.feat_ambient_music_glance_title)

    // Bluetooth permissions
    PermissionRegistry.register("BLUETOOTH_CONNECT", R.string.feat_batteries_title)
    PermissionRegistry.register("BLUETOOTH_SCAN", R.string.feat_batteries_title)

    // Draw over other apps permission
    PermissionRegistry.register("DRAW_OVER_OTHER_APPS", R.string.feat_notification_lighting_title)

    // Post notifications permission
    PermissionRegistry.register("POST_NOTIFICATIONS", R.string.feat_caffeinate_title)

    // Read phone state permission
    PermissionRegistry.register("READ_PHONE_STATE", R.string.search_smart_data_title)
    PermissionRegistry.register("READ_PHONE_STATE", R.string.feat_call_vibrations_title)

    // Device Admin permission
    PermissionRegistry.register("DEVICE_ADMIN", R.string.feat_screen_locked_security_title)

    // Location permission
    PermissionRegistry.register("LOCATION", R.string.feat_location_reached_title)
    PermissionRegistry.register("BACKGROUND_LOCATION", R.string.feat_location_reached_title)
    PermissionRegistry.register("USE_FULL_SCREEN_INTENT", R.string.feat_location_reached_title)

    // Battery optimization permission
    PermissionRegistry.register("BATTERY_OPTIMIZATION", R.string.feat_caffeinate_title)

    // Modify system settings permission
    PermissionRegistry.register("WRITE_SETTINGS", R.string.feat_qs_tiles_title)
    
    // Calendar sync permission
    PermissionRegistry.register("READ_CALENDAR", R.string.feat_calendar_sync_title)

    // Notification policy permission
    PermissionRegistry.register("NOTIFICATION_POLICY", R.string.feat_sound_modes_title)

    // Default browser permission
    PermissionRegistry.register("DEFAULT_BROWSER", R.string.feat_link_actions_title)
}