package com.sameerasw.essentials.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerGestureTrackerTest {
    @Test
    fun doublePressWithinWindow() {
        val tracker = PowerGestureTracker(doublePressWindowMs = 300)
        assertFalse(tracker.onDown(1000))
        assertTrue(tracker.onDown(1200))
    }

    @Test
    fun doublePressOutsideWindow() {
        val tracker = PowerGestureTracker(doublePressWindowMs = 300)
        assertFalse(tracker.onDown(1000))
        assertFalse(tracker.onDown(1400))
    }

    @Test
    fun threeQuickDownsOnlySecondIsDouble() {
        val tracker = PowerGestureTracker(doublePressWindowMs = 300)
        assertFalse(tracker.onDown(1000))
        assertTrue(tracker.onDown(1150))
        assertFalse(tracker.onDown(1300))
    }

    @Test
    fun holdBetweenTwoQuickDownsPreventsDouble() {
        val tracker = PowerGestureTracker(doublePressWindowMs = 300)
        assertFalse(tracker.onDown(1000))
        tracker.onHold()
        assertFalse(tracker.onDown(1150))
    }

    @Test
    fun gestureStartFirstPressAndSecondPress() {
        val tracker = PowerGestureTracker(doublePressWindowMs = 300)
        assertTrue(tracker.isGestureStart(1000))
        assertFalse(tracker.onDown(1000))
        assertFalse(tracker.isGestureStart(1200))
        assertTrue(tracker.onDown(1200))
        assertTrue(tracker.isGestureStart(1400))
    }

    @Test
    fun powerPressEventHasFirstPressAt() {
        val event = PowerPressEvent(PowerGesture.DOUBLE_PRESS, screenOnBefore = true, firstPressAt = 12345L)
        assertEquals(12345L, event.firstPressAt)
    }
}
