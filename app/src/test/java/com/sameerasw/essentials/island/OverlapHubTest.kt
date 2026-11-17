package com.sameerasw.essentials.island

import com.sameerasw.essentials.island.model.StackIcon
import com.sameerasw.essentials.island.model.QueueOverflow
import com.sameerasw.essentials.island.ui.overlapMinFactor
import com.sameerasw.essentials.island.ui.overlapOverflow
import com.sameerasw.essentials.island.ui.overlapSlots
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlapHubTest {
    private fun icon(key: String, width: Float = 1f) = StackIcon(key, widthFactor = width) {}
    private val hub = listOf(icon("media", 2.2f), icon("n1"), icon("n2"), icon("n3"))

    @Test fun counterNeverReplacesTheLead() = assertEquals(2, overlapSlots(hub, 1))

    @Test fun singleIconNeedsOneSlot() = assertEquals(1, overlapSlots(listOf(icon("media", 2.2f)), 1))

    @Test fun minWidthIsLeadPlusCounter() = assertEquals(3.05f, overlapMinFactor(hub), 0.001f)

    @Test fun noRoomShowsLeadOnly() {
        assertEquals(1, overlapSlots(hub, 0))
        assertEquals(QueueOverflow.None, overlapOverflow(0, QueueOverflow.Number))
    }
}
