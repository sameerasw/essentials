package com.sameerasw.essentials.island

import com.sameerasw.essentials.island.plugins.notifications.removeKeepingCurrent
import org.junit.Assert.assertEquals
import org.junit.Test

class OtpQueueTest {
    @Test
    fun removingBeforeCurrentShiftsIndexDown() {
        val list = mutableListOf("a", "b", "c")
        assertEquals(1, list.removeKeepingCurrent(0, 2))
        assertEquals(listOf("b", "c"), list)
    }

    @Test
    fun removingCurrentKeepsIndexInRange() {
        val list = mutableListOf("a", "b", "c")
        assertEquals(1, list.removeKeepingCurrent(2, 2))
        assertEquals(listOf("a", "b"), list)
    }

    @Test
    fun removingAfterCurrentKeepsIndex() {
        val list = mutableListOf("a", "b", "c")
        assertEquals(0, list.removeKeepingCurrent(2, 0))
    }

    @Test
    fun removingLastItemGivesZero() {
        val list = mutableListOf("a")
        assertEquals(0, list.removeKeepingCurrent(0, 0))
        assertEquals(emptyList<String>(), list)
    }

    @Test
    fun outOfRangeIndexChangesNothing() {
        val list = mutableListOf("a", "b")
        assertEquals(1, list.removeKeepingCurrent(5, 1))
        assertEquals(listOf("a", "b"), list)
    }
}
