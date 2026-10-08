package com.sameerasw.essentials.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockscreenShortcutDetectorTest {
    private val width = 1280
    private val height = 2856

    private fun candidate(
        id: String = "",
        clickable: Boolean = true,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) = LockscreenShortcutDetector.isShortcutCandidate(id, clickable, left, top, right, bottom, width, height)

    @Test
    fun bottomLeftButtonIsShortcut() {
        // Do Not Disturb shortcut as laid out on a Pixel emulator (Android 17)
        assertTrue(candidate(left = 48, top = 2616, right = 192, bottom = 2760))
    }

    @Test
    fun bottomRightButtonIsShortcut() {
        assertTrue(candidate(left = 1088, top = 2616, right = 1232, bottom = 2760))
    }

    @Test
    fun knownAffordanceIdIsShortcutAnywhere() {
        assertTrue(candidate(id = "start_button", clickable = false, left = 0, top = 0, right = 10, bottom = 10))
    }

    @Test
    fun deviceEntryIconIsNotShortcut() {
        assertFalse(candidate(id = "device_entry_icon_view", left = 48, top = 2616, right = 192, bottom = 2760))
    }

    @Test
    fun centeredOrFullWidthNodesAreNotShortcuts() {
        assertFalse(candidate(left = 532, top = 2418, right = 748, bottom = 2634))
        assertFalse(candidate(left = 0, top = 2625, right = 1280, bottom = 2769))
    }

    @Test
    fun nonClickableOrHighNodesAreNotShortcuts() {
        assertFalse(candidate(clickable = false, left = 48, top = 2616, right = 192, bottom = 2760))
        assertFalse(candidate(left = 48, top = 1000, right = 192, bottom = 1144))
    }

    // Every System UI view id on a Pixel 11 Pro lock screen with the shade closed (Android 17)
    private val pixel11LockscreenIds =
        listOf(
                "accessibility_actions_view",
                "alarm_text_view",
                "ambient_indication",
                "ambient_indication_container",
                "backgroundDimmed",
                "backgroundNormal",
                "bc_smartspace_view",
                "burn_in_layer",
                "burn_in_layer_empty_view",
                "compose_view",
                "content",
                "cutout_space_view",
                "date",
                "date_smartspace_view",
                "device_entry_icon_bg",
                "device_entry_icon_fg",
                "device_entry_icon_view",
                "expandableNotificationRow",
                "expanded",
                "fake_shadow",
                "keyguard_carrier_text",
                "keyguard_header",
                "keyguard_indication_area",
                "keyguard_indication_text_bottom",
                "keyguard_long_press",
                "keyguard_message_area_container",
                "keyguard_root_view",
                "legacy_window_root",
                "light_reveal_scrim",
                "low_light_animation_container",
                "notificationShelf",
                "notification_container_parent",
                "notification_panel",
                "notification_stack_scroller",
                "nssl_placeholder",
                "qs_frame",
                "quick_settings_container",
                "scrim_behind",
                "scrim_in_front",
                "scrim_notifications",
                "shared_notification_container",
                "smartspace_card_pager",
                "smartspace_subtitle_group",
                "statusIcons",
                "status_icon_area",
                "subtitle_text",
                "system_icons",
                "system_icons_container",
                "text_group",
                "title_text",
                "weather_smartspace_view",
                "weather_text_view",
                "wifi_combo",
                "wifi_group",
                "wifi_signal",
        )

    @Test
    fun pixel11LockscreenIsVisibleAndNotCovered() {
        assertTrue(pixel11LockscreenIds.any(LockscreenShortcutDetector::isLockscreenMarker))
        assertFalse(pixel11LockscreenIds.any(LockscreenShortcutDetector::isOccludingMarker))
    }

    @Test
    fun pixel11QuickSettingsCoversTheLockscreen() {
        assertTrue(LockscreenShortcutDetector.isOccludingMarker("quick_settings_panel"))
        assertTrue(LockscreenShortcutDetector.isOccludingMarker("bouncer_container"))
    }
}
