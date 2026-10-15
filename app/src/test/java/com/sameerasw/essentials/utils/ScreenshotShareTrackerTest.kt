package com.sameerasw.essentials.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotShareTrackerTest {
    private val sheet = "com.android.intentresolver"
    private val editor = "com.google.android.markup"
    private val origin = "com.android.settings"
    private val target = "com.google.android.gm"
    private val launcher = "com.google.android.apps.nexuslauncher"

    private fun sharedToTarget(): ScreenshotShareTracker =
        ScreenshotShareTracker(sheet, editor).apply {
            onShareTapped(0, origin)
            onForegroundPackage(sheet, 1_000)
            onForegroundPackage(target, 2_000)
        }

    @Test
    fun leavingTargetTrashes() {
        val tracker = sharedToTarget()
        assertEquals(ScreenshotShareTracker.Stage.IN_TARGET, tracker.stage)
        assertFalse(tracker.onForegroundPackage(target, 3_000))
        assertTrue(tracker.onForegroundPackage(launcher, 600_000))
        assertNull(tracker.stage)
    }

    @Test
    fun returningToOriginFromSheetIsDismissal() {
        val tracker = ScreenshotShareTracker(sheet, editor)
        tracker.onShareTapped(0, origin)
        tracker.onForegroundPackage(sheet, 1_000)
        assertFalse(tracker.onForegroundPackage(origin, 2_000))
        assertNull(tracker.stage)
    }

    @Test
    fun editFromSheetKeepsScreenshot() {
        val tracker = ScreenshotShareTracker(sheet, editor)
        tracker.onShareTapped(0, origin)
        tracker.onForegroundPackage(sheet, 1_000)
        assertFalse(tracker.onForegroundPackage(editor, 2_000))
        assertFalse(tracker.onForegroundPackage(launcher, 3_000))
        assertNull(tracker.stage)
    }

    @Test
    fun originEventsBeforeSheetOpensAreIgnored() {
        val tracker = ScreenshotShareTracker(sheet, editor)
        tracker.onShareTapped(0, origin)
        assertFalse(tracker.onForegroundPackage(origin, 500))
        assertEquals(ScreenshotShareTracker.Stage.SHARE_TAPPED, tracker.stage)
    }

    @Test
    fun staleShareExpires() {
        val tracker = ScreenshotShareTracker(sheet, editor)
        tracker.onShareTapped(0, origin)
        assertFalse(tracker.onForegroundPackage(sheet, ScreenshotShareTracker.SHARE_TIMEOUT_MS + 1))
        assertNull(tracker.stage)
    }

    @Test
    fun screenOffDropsPendingShareButNotOneInTarget() {
        val pending = ScreenshotShareTracker(sheet, editor)
        pending.onShareTapped(0, origin)
        pending.onScreenOff()
        assertNull(pending.stage)

        val inTarget = sharedToTarget()
        inTarget.onScreenOff()
        assertEquals(ScreenshotShareTracker.Stage.IN_TARGET, inTarget.stage)
    }
}
