package com.sameerasw.essentials.domain

import com.sameerasw.essentials.domain.model.HilightFrames
import com.sameerasw.essentials.domain.model.HilightProgressColorMode
import com.sameerasw.essentials.domain.model.HilightProgressFrames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HilightProgressFramesTest {
    private val blue = 0xFF4285F4.toInt()

    @Test
    fun eachEighthLightsOneMoreLed() {
        assertEquals(1, HilightProgressFrames.litCount(0f, 8))
        assertEquals(1, HilightProgressFrames.litCount(12.5f, 8))
        assertEquals(2, HilightProgressFrames.litCount(12.6f, 8))
        assertEquals(3, HilightProgressFrames.litCount(37f, 8))
        assertEquals(8, HilightProgressFrames.litCount(99.9f, 8))
        assertEquals(8, HilightProgressFrames.litCount(150f, 8))
    }

    @Test
    fun progressLightsWholeLedsOnly() {
        val frame = HilightProgressFrames.progress(37f, HilightProgressColorMode.MONOCHROME, blue, 8)
        assertTrue(frame.take(3).all { it == HilightFrames.scale(blue, 1f) })
        assertTrue(frame.drop(3).all { it == HilightFrames.OFF })
    }

    @Test
    fun sequentialAndVibgyorGiveEveryLedItsOwnColour() {
        for (mode in listOf(HilightProgressColorMode.SEQUENTIAL, HilightProgressColorMode.VIBGYOR)) {
            val colors = (0 until 8).map { HilightProgressFrames.ledColor(mode, blue, it, 8) }
            assertEquals(mode.name, 8, colors.toSet().size)
        }
        val mono = (0 until 8).map { HilightProgressFrames.ledColor(HilightProgressColorMode.MONOCHROME, blue, it, 8) }
        assertEquals(1, mono.toSet().size)
    }

    @Test
    fun sequentialGetsBrighterAlongTheArray() {
        val blues = (0 until 8).map { HilightProgressFrames.ledColor(HilightProgressColorMode.SEQUENTIAL, blue, it, 8) and 0xFF }
        assertEquals(blues.sorted(), blues)
        assertEquals(0xF4, blues.last())
    }

    @Test
    fun vibgyorRunsFromVioletToRed() {
        val first = HilightProgressFrames.ledColor(HilightProgressColorMode.VIBGYOR, blue, 0, 8)
        val last = HilightProgressFrames.ledColor(HilightProgressColorMode.VIBGYOR, blue, 7, 8)
        val (r0, g0, b0) = channels(first)
        assertTrue(b0 > r0 && r0 > g0)
        assertEquals(Triple(255, 0, 0), channels(last))
        val yellow = HilightProgressFrames.ledColor(HilightProgressColorMode.VIBGYOR, blue, 5, 8)
        assertEquals(Triple(255, 255, 0), channels(yellow))
    }

    @Test
    fun completionBlinksTwiceThenStaysOff() {
        val litAt = listOf(0L, 100L, 300L, 500L, 700L).map { t ->
            HilightProgressFrames.completion(t, HilightProgressColorMode.VIBGYOR, blue, 8).any { it != HilightFrames.OFF }
        }
        assertEquals(listOf(true, true, false, true, false), litAt)
    }

    @Test
    fun indeterminateSweepsOneLedBackAndForth() {
        val positions = (0 until 15).map { step ->
            val frame = HilightProgressFrames.indeterminate(step * 120L, HilightProgressColorMode.MONOCHROME, blue, 8)
            assertEquals(1, frame.count { it != HilightFrames.OFF })
            frame.indexOfFirst { it != HilightFrames.OFF }
        }
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1, 0), positions)
    }

    @Test
    fun demoFillsOneLedAtATimeThenCompletes() {
        val step = HilightProgressFrames.DEMO_STEP_MS
        val lit = (0 until 8).map { i ->
            HilightProgressFrames.demo(i * step, HilightProgressColorMode.MONOCHROME, blue, 8).count { it != HilightFrames.OFF }
        }
        assertEquals((1..8).toList(), lit)
        assertEquals(8 * step + HilightProgressFrames.COMPLETION_DURATION_MS, HilightProgressFrames.demoDurationMs(8))
    }

    private fun channels(color: Int) = Triple((color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF)
}
