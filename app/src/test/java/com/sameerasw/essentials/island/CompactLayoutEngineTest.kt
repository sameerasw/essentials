package com.sameerasw.essentials.island

import com.sameerasw.essentials.island.state.CameraAnchor
import com.sameerasw.essentials.island.state.CompactEntry
import com.sameerasw.essentials.island.state.CompactLayoutEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class CompactLayoutEngineTest {
    private val time = CompactEntry("time", 0, true, listOf("time"))
    private val battery = CompactEntry("battery", 1, true, listOf("battery"))
    private val catchUp = CompactEntry("notif", 10, false, listOf("notif.icon"))
    private val chatCatchUp = CompactEntry("notif", 10, false, listOf("notif.icon"), listOf("notif.app"))
    private val flash = CompactEntry("flash", 20, false, listOf("flash.icon"))
    private val media = CompactEntry("media", 30, false, listOf("media.art", "media.eq"))
    private val calendar = CompactEntry("cal", 50, false, listOf("cal.icon", "cal.time"))

    private fun visual(entries: List<CompactEntry>, anchor: CameraAnchor = CameraAnchor.Center): String {
        val a = CompactLayoutEngine.arrange(entries, anchor)
        return (a.before.reversed() + "|" + a.after).joinToString(" ")
    }

    @Test fun pinnedOnly() = assertEquals("time | battery", visual(listOf(time, battery)))

    @Test fun mediaPushesPinnedLeft() =
        assertEquals("battery time | media.eq media.art", visual(listOf(time, battery, media)))

    @Test fun calendarHiddenWhenOverCap() =
        assertEquals("battery time | media.eq media.art", visual(listOf(calendar, time, battery, media)))

    @Test fun calendarAloneWithPinned() =
        assertEquals("battery time | cal.time cal.icon", visual(listOf(time, battery, calendar)))

    @Test fun singleCellItemsGoToOuterEnds() =
        assertEquals("flash.icon time | battery notif.icon", visual(listOf(time, battery, catchUp, flash)))

    @Test fun mediaAloneSplitsAroundCamera() =
        assertEquals("media.art | media.eq", visual(listOf(media)))

    @Test fun lowerPriorityHiddenFirst() {
        val a = CompactLayoutEngine.arrange(listOf(time, battery, catchUp, flash, media))
        assertEquals(setOf("time", "battery", "notif", "flash"), a.visibleItems)
    }

    @Test fun startAnchorPutsEverythingAfter() =
        assertEquals("| time battery media.eq media.art", visual(listOf(time, battery, media), CameraAnchor.Start))

    @Test fun endAnchorPutsEverythingBefore() =
        assertEquals("media.art media.eq battery time |", visual(listOf(time, battery, media), CameraAnchor.End))

    @Test fun empty() = assertEquals("|", visual(emptyList()))

    @Test fun soloChatNotificationBalancesAroundCamera() =
        assertEquals("notif.app | notif.icon", visual(listOf(chatCatchUp)))

    @Test fun soloCellHiddenWithPinned() =
        assertEquals("time | battery notif.icon", visual(listOf(time, battery, chatCatchUp)))

    @Test fun soloCellHiddenWithOtherDynamic() =
        assertEquals("flash.icon | notif.icon", visual(listOf(chatCatchUp, flash)))

    @Test fun soloCellIgnoredWhenAnchoredToSide() =
        assertEquals("| notif.icon", visual(listOf(chatCatchUp), CameraAnchor.Start))

    private fun withBubble(entries: List<CompactEntry>, balance: Boolean): String {
        val a = CompactLayoutEngine.arrange(entries, CameraAnchor.Center, leftExtra = 1, balance = balance)
        return (a.before.reversed() + "|" + a.after).joinToString(" ")
    }

    @Test fun bubbleWithoutBalanceKeepsLeftItems() =
        assertEquals("flash.icon notif.icon | media.eq media.art", withBubble(listOf(catchUp, flash, media), balance = false))

    @Test fun bubbleBalanceMovesLowestPriorityRight() =
        assertEquals("notif.icon | flash.icon media.eq media.art", withBubble(listOf(catchUp, flash, media), balance = true))

    @Test fun balanceLeavesPinnedOnLeft() =
        assertEquals("time | flash.icon media.eq media.art", withBubble(listOf(time, flash, media), balance = true))
}
