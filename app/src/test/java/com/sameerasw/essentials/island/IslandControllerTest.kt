package com.sameerasw.essentials.island

import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandStage
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.state.Cancellable
import com.sameerasw.essentials.island.state.DelayScheduler
import com.sameerasw.essentials.island.state.IslandController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IslandControllerTest {
    private class FakeScheduler : DelayScheduler {
        val pending = mutableListOf<() -> Unit>()
        override fun schedule(delayMs: Long, action: () -> Unit): Cancellable {
            pending += action
            return Cancellable { pending.remove(action) }
        }
        fun runAll() = pending.toList().forEach { it() }.also { pending.clear() }
    }

    private val scheduler = FakeScheduler()
    private val controller = IslandController(scheduler)

    private fun item(key: String, priority: Int = 30, dismissed: () -> Unit = {}) = IslandItem(
        key = key,
        priority = priority,
        placement = CompactPlacement.Dynamic,
        compact = listOf(CompactCell("$key.icon") {}),
        line = LineContent(icon = {}, start = "a", end = "b"),
        expanded = ExpandedContent { },
        dismissible = true,
        onDismiss = dismissed,
    )

    private val stage get() = controller.state.value.stage

    @Test fun hiddenWithoutItems() = assertEquals(IslandStage.Hidden, stage)

    @Test fun tapTogglesCompactAndExpanded() {
        controller.setItems("media", listOf(item("media")))
        assertEquals(IslandStage.Compact, stage)
        controller.onTap("media")
        assertEquals(IslandStage.Expanded, stage)
        controller.onTap(null)
        assertEquals(IslandStage.Compact, stage)
    }

    @Test fun peekReturnsToCompact() {
        controller.setItems("media", listOf(item("media")))
        controller.peek("media", 3000)
        assertEquals(IslandStage.Line, stage)
        scheduler.runAll()
        assertEquals(IslandStage.Compact, stage)
    }

    @Test fun tapInLineExpands() {
        controller.setItems("media", listOf(item("media")))
        controller.peek("media", 3000)
        controller.onTap(null)
        assertEquals(IslandStage.Expanded, stage)
        assertEquals("media", controller.state.value.focusedKey)
    }

    @Test fun peekNeverInterruptsExpanded() {
        controller.setItems("media", listOf(item("media")))
        controller.setItems("notif", listOf(item("notif", 10)))
        controller.expand("media")
        controller.peek("notif", 3000)
        assertEquals(IslandStage.Expanded, stage)
        assertEquals("media", controller.state.value.focusedKey)
    }

    @Test fun focusedItemRemovedFallsBack() {
        controller.setItems("media", listOf(item("media")))
        controller.setItems("notif", listOf(item("notif", 10)))
        controller.expand("notif")
        controller.setItems("notif", emptyList())
        assertEquals(IslandStage.Compact, stage)
        assertNull(controller.state.value.focusedKey)
    }

    @Test fun lineDisabledSkipsPeek() {
        controller.lineStageEnabled = false
        controller.setItems("media", listOf(item("media")))
        controller.peek("media", 3000)
        assertEquals(IslandStage.Compact, stage)
    }

    @Test fun dismissInvokesPlugin() {
        var dismissed = false
        controller.setItems("notif", listOf(item("notif", dismissed = { dismissed = true })))
        controller.expand("notif")
        controller.dismissFocused()
        assertEquals(true, dismissed)
    }

    @Test fun suppressionHides() {
        controller.setItems("media", listOf(item("media")))
        controller.setSuppressed(true)
        assertEquals(IslandStage.Hidden, stage)
    }

    @Test fun interactionRestartsPeekTimer() {
        controller.setItems("media", listOf(item("media")))
        controller.peek("media", 3000)
        controller.onUserInteraction()
        assertEquals(1, scheduler.pending.size)
        scheduler.runAll()
        assertEquals(IslandStage.Compact, stage)
    }

    @Test fun interactionRestartsExpandedTimer() {
        controller.expandedTimeoutMs = 5000
        controller.setItems("media", listOf(item("media")))
        controller.expand("media")
        controller.onUserInteraction()
        assertEquals(1, scheduler.pending.size)
        scheduler.runAll()
        assertEquals(IslandStage.Compact, stage)
    }
}
