package com.sameerasw.essentials.domain

import com.google.gson.Gson
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.ActionGsonAdapter
import com.sameerasw.essentials.domain.model.HilightEffect
import com.sameerasw.essentials.domain.model.HilightFrames
import com.sameerasw.essentials.domain.model.HilightPattern
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HilightFramesTest {
    private val red = 0xFFFF0000.toInt()

    private fun frame(
        pattern: HilightPattern,
        t: Long,
        leds: Int = 8,
    ) = HilightFrames.frame(HilightEffect(pattern, red), t, leds)

    @Test
    fun solidLightsEveryLed() {
        assertArrayEquals(IntArray(8) { red }, frame(HilightPattern.SOLID, 1234))
    }

    @Test
    fun blinkAlternatesEveryHalfSecond() {
        assertArrayEquals(IntArray(8) { red }, frame(HilightPattern.BLINK, 100))
        assertArrayEquals(IntArray(8) { HilightFrames.OFF }, frame(HilightPattern.BLINK, 600))
    }

    @Test
    fun glowStartsDarkAndPeaksMidCycle() {
        assertArrayEquals(IntArray(8) { HilightFrames.OFF }, frame(HilightPattern.GLOW, 0))
        assertArrayEquals(IntArray(8) { red }, frame(HilightPattern.GLOW, 1000))
    }

    @Test
    fun flashBlinksTwiceThenRests() {
        assertEquals(red, frame(HilightPattern.FLASH, 50)[0])
        assertEquals(HilightFrames.OFF, frame(HilightPattern.FLASH, 180)[0])
        assertEquals(red, frame(HilightPattern.FLASH, 300)[0])
        assertEquals(HilightFrames.OFF, frame(HilightPattern.FLASH, 800)[0])
    }

    @Test
    fun sweepHasOneHeadAndADimTail() {
        val colors = frame(HilightPattern.SWEEP, 3 * 90L)
        assertEquals(red, colors[3])
        assertEquals(HilightFrames.scale(red, 0.3f), colors[2])
        assertEquals(6, colors.count { it == HilightFrames.OFF })
    }

    @Test
    fun rainbowGivesEachLedADifferentOpaqueColour() {
        val colors = frame(HilightPattern.RAINBOW, 0)
        assertEquals(8, colors.toSet().size)
        assertTrue(colors.all { it ushr 24 == 0xFF })
    }

    @Test
    fun scaleDimsChannelsAndZeroIsOff() {
        assertEquals(0xFF7F0000.toInt(), HilightFrames.scale(red, 0.5f))
        assertEquals(HilightFrames.OFF, HilightFrames.scale(red, 0f))
    }

    @Test
    fun hueToColorMatchesPrimaries() {
        assertEquals(0xFFFF0000.toInt(), HilightFrames.hueToColor(0f))
        assertEquals(0xFF00FF00.toInt(), HilightFrames.hueToColor(120f))
        assertEquals(0xFF0000FF.toInt(), HilightFrames.hueToColor(240f))
    }

    @Test
    fun hilightActionRoundTrips() {
        val action = Action.Hilight(HilightPattern.SWEEP, red, 5_000)
        assertEquals(action, ActionGsonAdapter.fromJson(ActionGsonAdapter.toJson(action)))
    }

    @Test
    fun unknownPatternNamesFallBackToGlow() {
        val gson = Gson()
        val effect = gson.fromJson("""{"pattern":"BREATHE","color":-1,"durationMs":3000}""", HilightEffect::class.java)
        assertEquals(HilightPattern.GLOW, effect.withValidPattern().pattern)
        val action = gson.fromJson("""{"pattern":"CHASE","color":-1,"durationMs":3000}""", Action.Hilight::class.java)
        assertEquals(HilightPattern.GLOW, action.toEffect().pattern)
    }
}
