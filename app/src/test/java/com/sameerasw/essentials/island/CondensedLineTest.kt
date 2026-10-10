package com.sameerasw.essentials.island

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.ui.condensedTitleWidth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CondensedLineTest {
    private val lineWidth = 360.dp
    private val cameraSlot = 40.dp
    private val cellSize = 28.dp
    private val edgePad = 12.dp

    private fun title(natural: Dp, endInset: Dp = 0.dp) =
        condensedTitleWidth(natural, lineWidth, cameraSlot, cellSize, edgePad, endInset)

    private fun pillWidth(natural: Dp, endInset: Dp = 0.dp) =
        edgePad * 2 + cellSize + cameraSlot + title(natural, endInset)

    private fun leadIn() = edgePad + cellSize

    @Test fun shortTitleHugsTheText() {
        assertEquals(60.dp, title(natural = 60.dp))
        assertEquals(152.dp, pillWidth(natural = 60.dp))
    }

    @Test fun longTitleStopsAtTheRoomBesideTheCamera() = assertEquals(148.dp, title(natural = 900.dp))

    @Test fun titleNeverNarrowerThanTheIconBlock() = assertEquals(cellSize, title(natural = 4.dp))

    @Test fun iconSideNeverGrowsWithTheTitle() {
        val lead = leadIn()
        listOf(0.dp, 60.dp, 900.dp).forEach { natural ->
            assertEquals("natural=$natural", lead, pillWidth(natural) - title(natural) - cameraSlot - edgePad)
        }
    }

    @Test fun rightEdgeStaysInsideHalfTheMaxWidth() {
        listOf(0.dp, 60.dp, 900.dp).forEach { natural ->
            listOf(0.dp, 46.dp).forEach { endInset ->
                val fromCentre = cameraSlot / 2 + edgePad + title(natural, endInset)
                val budget = (lineWidth - endInset) / 2
                assertTrue("natural=$natural endInset=$endInset gave $fromCentre", fromCentre <= budget)
            }
        }
    }

    @Test fun titleGrowsMonotonicallyWithItsText() {
        val widths = listOf(0.dp, 30.dp, 60.dp, 140.dp, 900.dp).map { title(it) }
        assertEquals(widths.sortedBy { it.value }, widths)
    }
}
