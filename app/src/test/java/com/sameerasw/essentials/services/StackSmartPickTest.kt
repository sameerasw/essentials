/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Unit Tests - Services
 * File: StackSmartPickTest.kt
 * Description: Unit tests for StackSmartPick logic.
 */

package com.sameerasw.essentials.services

import com.sameerasw.essentials.services.widgets.StackSmartPick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StackSmartPickTest {
    @Test
    fun picksPlayingAppWidget() {
        val hosted = listOf("com.google.android.deskclock/.ClockWidget", "com.spotify.music/.NowPlayingWidget")
        assertEquals(1, StackSmartPick.pick(hosted, "com.spotify.music", emptySet(), false))
    }

    @Test
    fun picksCalendarAppWidgetWhenInCalendarPackages() {
        val hosted = listOf("com.google.android.deskclock/.ClockWidget", "com.google.android.calendar/.MonthWidget")
        assertEquals(1, StackSmartPick.pick(hosted, null, setOf("com.google.android.calendar"), false))
    }

    @Test
    fun doesNotPickCalendarWidgetWhenNotInCalendarPackages() {
        val hosted = listOf("com.google.android.deskclock/.ClockWidget", "com.google.android.calendar/.MonthWidget")
        assertNull(StackSmartPick.pick(hosted, null, emptySet(), false))
    }

    @Test
    fun picksBatteryWidgetWhenCharging() {
        val hosted = listOf("com.google.android.deskclock/.ClockWidget", "com.sameerasw.essentials/.BatteryWidget")
        assertEquals(1, StackSmartPick.pick(hosted, null, emptySet(), true))
    }

    @Test
    fun picksBatteriesWidgetWhenCharging() {
        val hosted = listOf("com.google.android.deskclock/.ClockWidget", "com.sameerasw.essentials/.BatteriesWidget")
        assertEquals(1, StackSmartPick.pick(hosted, null, emptySet(), true))
    }

    @Test
    fun doesNotPickBatteryWidgetWhenNotCharging() {
        val hosted = listOf("com.google.android.deskclock/.ClockWidget", "com.sameerasw.essentials/.BatteryWidget")
        assertNull(StackSmartPick.pick(hosted, null, emptySet(), false))
    }

    @Test
    fun doesNotPickWidgetWhenPackageContainsBatteryButClassDoesNot() {
        val hosted = listOf("com.battery.saver/.ClockWidget")
        assertNull(StackSmartPick.pick(hosted, null, emptySet(), true))
    }

    @Test
    fun priorityMusicBeatsCalendarAndChargingEvenWhenLowerPriorityWidgetComesFirst() {
        val hosted =
            listOf(
                "com.sameerasw.essentials/.BatteryWidget",
                "com.google.android.calendar/.MonthWidget",
                "com.spotify.music/.NowPlayingWidget",
            )
        assertEquals(2, StackSmartPick.pick(hosted, "com.spotify.music", setOf("com.google.android.calendar"), true))
    }

    @Test
    fun priorityCalendarBeatsChargingEvenWhenLowerPriorityWidgetComesFirst() {
        val hosted =
            listOf(
                "com.sameerasw.essentials/.BatteryWidget",
                "com.google.android.calendar/.MonthWidget",
            )
        assertEquals(1, StackSmartPick.pick(hosted, null, setOf("com.google.android.calendar"), true))
    }

    @Test
    fun firstWinsWhenTwoWidgetsMatchSameRule() {
        val hostedMusic =
            listOf(
                "com.spotify.music/.WidgetOne",
                "com.google.android.deskclock/.Clock",
                "com.spotify.music/.WidgetTwo",
            )
        assertEquals(0, StackSmartPick.pick(hostedMusic, "com.spotify.music", emptySet(), false))

        val hostedCalendar =
            listOf(
                "com.google.android.calendar/.WidgetOne",
                "com.google.android.calendar/.WidgetTwo",
            )
        assertEquals(0, StackSmartPick.pick(hostedCalendar, null, setOf("com.google.android.calendar"), false))

        val hostedBattery =
            listOf(
                "com.example/.BatterySmall",
                "com.example/.BatteryLarge",
            )
        assertEquals(0, StackSmartPick.pick(hostedBattery, null, emptySet(), true))
    }

    @Test
    fun nullComponentsNeverMatch() {
        val hosted = listOf(null, null)
        assertNull(StackSmartPick.pick(hosted, "com.spotify.music", setOf("com.google.android.calendar"), true))
    }

    @Test
    fun nothingMatchingReturnsNull() {
        val hosted = listOf("com.google.android.deskclock/.ClockWidget", "com.example.notes/.NotesWidget")
        assertNull(StackSmartPick.pick(hosted, "com.spotify.music", setOf("com.google.android.calendar"), false))
    }

    @Test
    fun emptyHostedListReturnsNull() {
        assertNull(StackSmartPick.pick(emptyList(), "com.spotify.music", setOf("com.google.android.calendar"), true))
    }

    @Test
    fun skipsNullHostedComponentsWhenFindingMatch() {
        val hosted = listOf(null, "com.spotify.music/.NowPlayingWidget")
        assertEquals(1, StackSmartPick.pick(hosted, "com.spotify.music", emptySet(), false))
    }
}
