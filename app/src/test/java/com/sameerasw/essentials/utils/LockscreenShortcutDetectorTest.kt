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
}
