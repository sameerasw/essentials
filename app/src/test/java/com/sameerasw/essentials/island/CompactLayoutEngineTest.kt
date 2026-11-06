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

    private fun mirrored(mirror: Boolean): String {
        val a = CompactLayoutEngine.arrange(listOf(time, battery, media), leftExtra = -3, balance = true, mirror = mirror)
        return (a.before.reversed() + "|" + a.after).joinToString(" ")
    }

    @Test fun heavyRightBubbleLeavesLayoutWithoutMirror() =
        assertEquals("battery time | media.eq media.art", mirrored(false))

    @Test fun mirrorPullsItemsLeftOfHeavyRightBubble() =
        assertEquals("battery time media.art media.eq |", mirrored(true))

    @Test fun maxItemsCountsDynamicOnly() {
        val a = CompactLayoutEngine.arrange(listOf(time, battery, catchUp, flash), maxItems = 1)
        assertEquals(setOf("time", "battery", "notif"), a.visibleItems)
    }

    @Test fun rightBubblePullsCellsLeft() {
        val a = CompactLayoutEngine.arrange(listOf(time, battery, catchUp, flash), leftExtra = -2)
        assertEquals("flash.icon notif.icon time | battery", (a.before.reversed() + "|" + a.after).joinToString(" "))
    }

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

    private fun budgeted(maxWidth: Float, widths: Map<String, Float> = emptyMap()): Set<String> =
        CompactLayoutEngine.arrange(
            listOf(time, battery, catchUp, flash),
            maxCells = 8,
            maxWidth = maxWidth,
            widthOf = { widths[it.itemKey] ?: it.cellKeys.size.toFloat() },
        ).visibleItems

    @Test fun widthBudgetDropsLowestPriorityFirst() =
        assertEquals(setOf("time", "battery", "notif"), budgeted(maxWidth = 3f))

    @Test fun widthBudgetKeepsPinnedEvenWhenTight() =
        assertEquals(setOf("time", "battery"), budgeted(maxWidth = 1f))

    @Test fun measuredWidthsCountInsteadOfCellCount() =
        assertEquals(setOf("time", "battery", "flash"), budgeted(maxWidth = 3.5f, widths = mapOf("notif" to 2f)))

    private fun shown(a: com.sameerasw.essentials.island.state.CompactArrangement) =
        (a.before.reversed() + "|" + a.after).joinToString(" ")

    @Test fun hubStaysOnScreenOverAChip() {
        val chipMedia = media.copy(keepTogether = true)
        val chipFlash = flash.copy(keepTogether = true)
        val wifi = CompactEntry("wifi", 40, false, listOf("wifi.icon"))
        val a = CompactLayoutEngine.arrange(listOf(wifi, chipFlash, chipMedia, catchUp), maxCells = 8, leftExtra = 1, sideMax = 3.5f)
        // Hub (1) + both chips (3) is 4 > 3.5; media, the lowest-priority chip, fits on neither side, so it drops
        assertEquals("flash.icon | notif.icon wifi.icon", shown(a))
    }

    @Test fun mediaStaysRightOtherChipsLeft() {
        val chipMedia = media.copy(keepTogether = true, endSide = true)
        val chipFlash = flash.copy(keepTogether = true)
        val wifi = CompactEntry("wifi", 40, false, listOf("wifi.icon"))
        for (balance in listOf(false, true)) {
            val a = CompactLayoutEngine.arrange(listOf(chipMedia, chipFlash, wifi), maxCells = 8, leftExtra = 1, balance = balance)
            assertEquals("flash.icon | wifi.icon media.eq media.art", shown(a))
        }
    }

    @Test fun chipJoinsPinnedChipPastPinnedPlainItem() {
        val chipTime = time.copy(keepTogether = true)
        val sound = CompactEntry("sound", 15, false, listOf("sound.icon"), keepTogether = true)
        val chipMedia = media.copy(keepTogether = true, endSide = true)
        val wifi = CompactEntry("wifi", 40, false, listOf("wifi.icon"))
        val a = CompactLayoutEngine.arrange(listOf(chipTime, battery, sound, chipMedia, wifi), maxCells = 8, leftExtra = 1)
        assertEquals("battery sound.icon time | wifi.icon media.eq media.art", shown(a))
    }

    @Test fun pinnedPlainItemCrossesToEvenWidths() {
        val chipTime = time.copy(keepTogether = true)
        val sound = CompactEntry("sound", 15, false, listOf("sound.icon"), keepTogether = true)
        val chipMedia = media.copy(keepTogether = true, endSide = true)
        val a = CompactLayoutEngine.arrange(
            listOf(chipTime, battery, sound, chipMedia),
            maxCells = 8,
            widthOf = { if (it.itemKey == "time") 2f else it.cellKeys.size.toFloat() },
        )
        assertEquals("sound.icon time | battery media.eq media.art", shown(a))
    }

    @Test fun chipsGatherOnOneSide() {
        val chipFlash = flash.copy(keepTogether = true)
        val sound = CompactEntry("sound", 25, false, listOf("sound.icon"), keepTogether = true)
        val a = CompactLayoutEngine.arrange(listOf(time, chipFlash, sound, catchUp), maxCells = 8, balance = true)
        val sides = listOf(a.before, a.after).map { side -> side.count { it == "flash.icon" || it == "sound.icon" } }
        assert(2 in sides) { shown(a) }
    }
}
