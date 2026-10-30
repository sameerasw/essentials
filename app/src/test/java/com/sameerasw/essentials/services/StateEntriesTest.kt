/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Unit Tests - Services
 * File: StateEntriesTest.kt
 * Description: Unit tests for StateEntries.
 */

package com.sameerasw.essentials.services

import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.State
import com.sameerasw.essentials.services.automation.modules.StateEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StateEntriesTest {
    @Test
    fun activeOnFirstSyncReturnsEntryListOnceThenNothing() {
        val entries = StateEntries()
        val automation =
            Automation(
                id = "1",
                type = Automation.Type.STATE,
                state = State.Dnd,
                entryActions = listOf(Action.TurnOnWifi),
                exitActions = listOf(Action.TurnOffWifi),
            )
        val first = entries.sync(listOf(automation)) { true }
        assertEquals(listOf(listOf(Action.TurnOnWifi)), first)

        val second = entries.sync(listOf(automation)) { true }
        assertTrue(second.isEmpty())
    }

    @Test
    fun activeThenInactiveReturnsExitListOnce() {
        val entries = StateEntries()
        val automation =
            Automation(
                id = "1",
                type = Automation.Type.STATE,
                state = State.Dnd,
                entryActions = listOf(Action.TurnOnWifi),
                exitActions = listOf(Action.TurnOffWifi),
            )
        entries.sync(listOf(automation)) { true }

        val inactive = entries.sync(listOf(automation)) { false }
        assertEquals(listOf(listOf(Action.TurnOffWifi)), inactive)

        val secondInactive = entries.sync(listOf(automation)) { false }
        assertTrue(secondInactive.isEmpty())
    }

    @Test
    fun enteredThenRemovedOrDisabledReturnsExitList() {
        val entries = StateEntries()
        val automation =
            Automation(
                id = "1",
                type = Automation.Type.STATE,
                state = State.Dnd,
                entryActions = listOf(Action.TurnOnWifi),
                exitActions = listOf(Action.TurnOffWifi),
            )
        entries.sync(listOf(automation)) { true }

        val removed = entries.sync(emptyList()) { true }
        assertEquals(listOf(listOf(Action.TurnOffWifi)), removed)

        entries.sync(listOf(automation)) { true }
        val disabledAuto = automation.copy(isEnabled = false)
        val disabled = entries.sync(listOf(disabledAuto)) { true }
        assertEquals(listOf(listOf(Action.TurnOffWifi)), disabled)
    }

    @Test
    fun enteredThenEditedWhileActiveReplacesStoredCopy() {
        val entries = StateEntries()
        val initial =
            Automation(
                id = "1",
                type = Automation.Type.STATE,
                state = State.Dnd,
                entryActions = listOf(Action.TurnOnWifi),
                exitActions = listOf(Action.TurnOffWifi),
            )
        entries.sync(listOf(initial)) { true }

        val edited = initial.copy(exitActions = listOf(Action.TurnOnWifi))
        val syncEdited = entries.sync(listOf(edited)) { true }
        assertTrue(syncEdited.isEmpty())

        val inactive = entries.sync(listOf(edited)) { false }
        assertEquals(listOf(listOf(Action.TurnOnWifi)), inactive)
    }

    @Test
    fun enteredThenInactiveWithNewExitActionsUsesStoredExitList() {
        val entries = StateEntries()
        val initial =
            Automation(
                id = "1",
                type = Automation.Type.STATE,
                state = State.Dnd,
                entryActions = listOf(Action.TurnOnWifi),
                exitActions = listOf(Action.TurnOffWifi),
            )
        entries.sync(listOf(initial)) { true }

        val edited = initial.copy(exitActions = listOf(Action.TurnOnWifi))
        val inactive = entries.sync(listOf(edited)) { false }
        assertEquals(listOf(listOf(Action.TurnOffWifi)), inactive)
    }

    @Test
    fun neverEnteredAndRemovedReturnsNothing() {
        val entries = StateEntries()
        val automation =
            Automation(
                id = "1",
                type = Automation.Type.STATE,
                state = State.Dnd,
                entryActions = listOf(Action.TurnOnWifi),
                exitActions = listOf(Action.TurnOffWifi),
            )
        val syncInactive = entries.sync(listOf(automation)) { false }
        assertTrue(syncInactive.isEmpty())

        val syncRemoved = entries.sync(emptyList()) { false }
        assertTrue(syncRemoved.isEmpty())
    }

    @Test
    fun closeAllReturnsExitsAndAllowsReentry() {
        val entries = StateEntries()
        val automation =
            Automation(
                id = "1",
                type = Automation.Type.STATE,
                state = State.Dnd,
                entryActions = listOf(Action.TurnOnWifi),
                exitActions = listOf(Action.TurnOffWifi),
            )
        entries.sync(listOf(automation)) { true }

        val closed = entries.closeAll()
        assertEquals(listOf(listOf(Action.TurnOffWifi)), closed)

        val reenter = entries.sync(listOf(automation)) { true }
        assertEquals(listOf(listOf(Action.TurnOnWifi)), reenter)
    }

    @Test
    fun triggerAutomationsAreIgnored() {
        val entries = StateEntries()
        val triggerAutomation =
            Automation(
                id = "1",
                type = Automation.Type.TRIGGER,
                entryActions = listOf(Action.TurnOnWifi),
                exitActions = listOf(Action.TurnOffWifi),
            )
        val result = entries.sync(listOf(triggerAutomation)) { true }
        assertTrue(result.isEmpty())
    }
}
