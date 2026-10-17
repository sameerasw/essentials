package com.sameerasw.essentials.island.plugins.navigation

import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.plugins.navigation.NavigationTrigger.Action
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationTriggerTest {
    private val trigger = NavigationTrigger()

    private fun decide(
        instruction: String = "Turn right",
        turned: Boolean = false,
        meters: Double? = null,
        onNew: String = SettingsRepository.ISLAND_NAVIGATION_ON_NEW_PEEK,
        canExpand: Boolean = true,
        now: Long = 100_000L,
    ) = trigger.decide(instruction, turned, meters, 200, onNew, canPeek = true, canExpand = canExpand, now = now)

    @Test
    fun newInstructionPeeks() {
        assertEquals(Action.Peek, decide(turned = true, meters = 1000.0))
    }

    @Test
    fun newInstructionExpandsWhenSetToExpand() {
        assertEquals(Action.Expand, decide(turned = true, meters = 1000.0, onNew = SettingsRepository.ISLAND_NAVIGATION_ON_NEW_EXPAND))
    }

    @Test
    fun offDoesNothingOnNewInstruction() {
        assertEquals(Action.None, decide(turned = true, meters = 1000.0, onNew = SettingsRepository.ISLAND_NAVIGATION_ON_NEW_OFF))
    }

    @Test
    fun announceSkippedWhenTurnIsClose() {
        assertEquals(Action.None, decide(turned = true, meters = 250.0))
    }

    @Test
    fun approachingExpandsOncePerManeuver() {
        assertEquals(Action.Expand, decide(meters = 150.0))
        assertEquals(Action.None, decide(meters = 100.0))
    }

    @Test
    fun approachingRearmsAfterDistanceJumpsBack() {
        assertEquals(Action.Expand, decide(meters = 150.0))
        decide(meters = 1000.0)
        assertEquals(Action.Expand, decide(meters = 150.0))
    }

    @Test
    fun approachingWaitsWhileOtherItemIsFocused() {
        assertEquals(Action.None, decide(turned = true, meters = 150.0, canExpand = false))
        assertEquals(Action.Expand, decide(meters = 140.0))
    }

    @Test
    fun surfacedInstructionNotAnnouncedAgain() {
        assertEquals(Action.Peek, decide(turned = true))
        assertEquals(Action.None, decide(turned = true, now = 200_000L))
    }

    @Test
    fun cooldownBlocksBackToBackAnnouncements() {
        assertEquals(Action.Peek, decide(instruction = "A", turned = true, meters = 1000.0))
        assertEquals(Action.None, decide(instruction = "B", turned = true, meters = 1000.0, now = 110_000L))
        assertEquals(Action.Peek, decide(instruction = "C", turned = true, meters = 1000.0, now = 140_000L))
    }

    @Test
    fun resetForgetsSurfaced() {
        decide(turned = true, meters = 1000.0)
        trigger.reset()
        assertEquals(Action.Peek, decide(turned = true, meters = 1000.0))
    }
}
