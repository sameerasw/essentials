package com.sameerasw.essentials.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DuoBatteryLevelsTest {
    @Test
    fun lowAboveCriticalIsKept() {
        assertEquals(30 to 10, DuoBatteryLevels.withLow(30, 10))
    }

    @Test
    fun lowBelowCriticalPullsCriticalDown() {
        assertEquals(8 to 7, DuoBatteryLevels.withLow(8, 10))
    }

    @Test
    fun lowIsClampedToRange() {
        assertEquals(99 to 10, DuoBatteryLevels.withLow(150, 10))
        assertEquals(2 to 1, DuoBatteryLevels.withLow(-5, 10))
    }

    @Test
    fun criticalAboveLowPushesLowUp() {
        assertEquals(26 to 25, DuoBatteryLevels.withCritical(25, 20))
    }

    @Test
    fun criticalBelowLowIsKept() {
        assertEquals(20 to 15, DuoBatteryLevels.withCritical(15, 20))
    }

    @Test
    fun criticalIsClampedToRange() {
        assertEquals(99 to 98, DuoBatteryLevels.withCritical(500, 20))
        assertEquals(20 to 1, DuoBatteryLevels.withCritical(0, 20))
    }
}
