package com.sameerasw.essentials.domain.registry

object FeatureResetRegistry {
    private val prefixes: Map<String, List<String>> =
        mapOf(
            "Island" to listOf("island_"),
            "Duo" to listOf("duo_"),
            "Notification lighting" to listOf("edge_lighting_"),
            "Status glance" to listOf("status_glance_"),
            "Pixel Searchbar" to listOf("pixel_search"),
            "AOD wallpaper" to listOf("aod_wallpaper_"),
            "Daily Wallpaper" to listOf("daily_wallpaper_"),
            "Button remap" to listOf("button_remap_"),
            "Flashlight pulse" to listOf("flashlight_pulse_"),
            "Conscious gate" to listOf("conscious_gate_"),
            "Shut-Up!" to listOf("shut_up_"),
            "Pocket mode" to listOf("pocket_mode_"),
            "Hilight" to listOf("hilight_"),
            "Hilight notifications" to listOf("hilight_notifications_"),
            "Hilight live progress" to listOf("hilight_progress_"),
            "Freeze" to listOf("freeze_"),
            "System Keyboard" to listOf("keyboard_"),
            "App lock" to listOf("app_lock_"),
            "Screen refresh rate" to listOf("refresh_rate_"),
            "Smart pixels" to listOf("smart_pixels_"),
            "Face unlock brightness" to listOf("face_unlock_"),
            "Screenshots" to listOf("screenshot_", "screenshots_"),
            "Back gesture" to listOf("back_gesture_"),
            "Dynamic night light" to listOf("dynamic_night_"),
            "Calendar Sync" to listOf("calendar_sync_"),
            "Volume steps" to listOf("volume_steps_"),
            "App volume" to listOf("app_volume_"),
            "Lock screen shortcuts" to listOf("lockscreen_shortcut"),
        )

    fun prefixesFor(featureId: String): List<String>? = prefixes[featureId]
}
