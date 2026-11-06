package com.sameerasw.essentials.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraFlipButtonTest {
    @Test
    fun isFlipButtonMatches() {
        assertTrue(CameraFlipButton.isFlipButton("com.android.camera2:id/camera_toggle_button", null))
        assertTrue(CameraFlipButton.isFlipButton("com.google.android.GoogleCamera:id/camera_switch_button", null))
        assertTrue(CameraFlipButton.isFlipButton(null, "Switch to front camera"))
        assertFalse(CameraFlipButton.isFlipButton("com.android.camera2:id/shutter_button", "Shutter"))
        assertFalse(CameraFlipButton.isFlipButton("com.android.camera2:id/mode_options_toggle", "Options"))
        assertFalse(CameraFlipButton.isFlipButton("com.android.camera2:id/flash_toggle_button", "Flash auto"))
        assertFalse(CameraFlipButton.isFlipButton(null, null))
    }

    @Test
    fun lensInUseAtCameraOpenAcrossFirstPress() {
        val sessions = listOf(CameraSession(lens = 1, openedAt = 1000L, closedAt = 3000L))
        assertEquals(1, lensInUseAt(sessions, 2000L))

        val unclosedSessions = listOf(CameraSession(lens = 0, openedAt = 1000L, closedAt = null))
        assertEquals(0, lensInUseAt(unclosedSessions, 2000L))
    }

    @Test
    fun lensInUseAtCameraOpenedAfterFirstPressReturnsNull() {
        val sessions = listOf(CameraSession(lens = 1, openedAt = 2500L, closedAt = null))
        assertNull(lensInUseAt(sessions, 2000L))
    }

    @Test
    fun lensInUseAtNoSessionsReturnsNull() {
        assertNull(lensInUseAt(emptyList(), 2000L))
    }

    @Test
    fun lensInUseAtSessionClosedBeforeFirstPressReturnsNull() {
        val sessions = listOf(CameraSession(lens = 1, openedAt = 500L, closedAt = 1500L))
        assertNull(lensInUseAt(sessions, 2000L))
    }

    @Test
    fun lensInUseAtTwoSessionsReturnsOldSessionLens() {
        val sessions =
            listOf(
                CameraSession(lens = 0, openedAt = 1000L, closedAt = 2500L),
                CameraSession(lens = 1, openedAt = 2200L, closedAt = null),
            )
        assertEquals(0, lensInUseAt(sessions, 2100L))
    }

    @Test
    fun openLensReturnsLatestOpenSession() {
        assertNull(openLens(emptyList()))
        assertNull(openLens(listOf(CameraSession(lens = 0, openedAt = 1000L, closedAt = 2000L))))
        assertEquals(
            1,
            openLens(
                listOf(
                    CameraSession(lens = 0, openedAt = 1000L, closedAt = 2000L),
                    CameraSession(lens = 1, openedAt = 2500L, closedAt = null),
                ),
            ),
        )
        assertEquals(
            2,
            openLens(
                listOf(
                    CameraSession(lens = 1, openedAt = 1000L, closedAt = null),
                    CameraSession(lens = 2, openedAt = 2000L, closedAt = null),
                ),
            ),
        )
    }
}
