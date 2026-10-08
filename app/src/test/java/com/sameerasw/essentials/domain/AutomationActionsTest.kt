package com.sameerasw.essentials.domain

import com.google.gson.Gson
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.Automation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationActionsTest {
    @Test
    fun singleActionsSavedBeforeSequencesStillRun() {
        val old = Automation(id = "a", type = Automation.Type.STATE, entryAction = Action.TurnOnWifi, exitAction = Action.TurnOffWifi)
        assertEquals(listOf(Action.TurnOnWifi), old.entryActionList)
        assertEquals(listOf(Action.TurnOffWifi), old.exitActionList)
    }

    @Test
    fun sequencesWinOverTheOldSingleActions() {
        val automation =
            Automation(
                id = "a",
                type = Automation.Type.APP,
                entryAction = Action.TurnOnWifi,
                entryActions = listOf(Action.TurnOffWifi, Action.ToggleFlashlight),
                exitActions = emptyList(),
                exitAction = Action.TurnOffWifi,
            )
        assertEquals(listOf(Action.TurnOffWifi, Action.ToggleFlashlight), automation.entryActionList)
        // An emptied list means the user removed the out actions, so the old single one must not come back
        assertTrue(automation.exitActionList.isEmpty())
    }

    @Test
    fun missingFieldsInSavedJsonGiveEmptyLists() {
        val automation = Gson().fromJson("""{"id":"a","type":"TRIGGER"}""", Automation::class.java)
        assertTrue(automation.actionList.isEmpty())
        assertTrue(automation.entryActionList.isEmpty())
        assertTrue(automation.exitActionList.isEmpty())
    }
}
