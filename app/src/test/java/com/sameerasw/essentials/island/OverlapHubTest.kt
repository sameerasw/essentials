package com.sameerasw.essentials.island

import com.sameerasw.essentials.island.model.StackIcon
import com.sameerasw.essentials.island.ui.overlapMinFactor
import com.sameerasw.essentials.island.ui.overlapSlots
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlapHubTest {
    private fun icon(key: String, width: Float = 1f) = StackIcon(key, widthFactor = width) {}
    private val hub = listOf(icon("media", 2.2f), icon("n1"), icon("n2"), icon("n3"))

    // A wide media lead with room for one slot still shows media + counter, never the counter alone
    @Test fun counterNeverReplacesTheLead() = assertEquals(2, overlapSlots(hub, 1))

    @Test fun singleIconNeedsOneSlot() = assertEquals(1, overlapSlots(listOf(icon("media", 2.2f)), 1))

    @Test fun minWidthIsLeadPlusCounter() = assertEquals(3.05f, overlapMinFactor(hub), 0.001f)
}
