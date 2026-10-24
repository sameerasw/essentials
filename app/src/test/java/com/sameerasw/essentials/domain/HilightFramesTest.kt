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
        val action = Action.Hilight(HilightPattern.SWEEP, red, 5_000, brightness = 40, repeats = 4)
        assertEquals(action, ActionGsonAdapter.fromJson(ActionGsonAdapter.toJson(action)))
    }

    @Test
    fun pulseBeatsTwiceThenRests() {
        assertArrayEquals(IntArray(8) { red }, frame(HilightPattern.PULSE, 0))
        assertArrayEquals(IntArray(8) { red }, frame(HilightPattern.PULSE, 280))
        assertEquals(HilightFrames.scale(red, 0.25f), frame(HilightPattern.PULSE, 150)[0])
        assertArrayEquals(IntArray(8) { HilightFrames.OFF }, frame(HilightPattern.PULSE, 800))
    }

    @Test
    fun rippleStartsInTheMiddle() {
        val colors = frame(HilightPattern.RIPPLE, 0)
        assertEquals(HilightFrames.scale(red, 0.5f), colors[3])
        assertEquals(HilightFrames.scale(red, 0.5f), colors[4])
        assertEquals(HilightFrames.OFF, colors[0])
        assertEquals(HilightFrames.OFF, colors[7])
        assertTrue(((colors[3] shr 16) and 0xFF) > ((colors[0] shr 16) and 0xFF))
        assertTrue(((colors[4] shr 16) and 0xFF) > ((colors[7] shr 16) and 0xFF))
    }

    @Test
    fun sparkleIsDeterministicAndChanges() {
        val frame0a = frame(HilightPattern.SPARKLE, 0)
        val frame0b = frame(HilightPattern.SPARKLE, 0)
        assertArrayEquals(frame0a, frame0b)

        val frame120 = frame(HilightPattern.SPARKLE, 120)
        assertTrue(!frame0a.contentEquals(frame120))

        val allLeds =
            (0 until 10).flatMap { step ->
                frame(HilightPattern.SPARKLE, step * 120L).toList()
            }
        assertTrue(allLeds.any { it != HilightFrames.OFF })
        assertTrue(allLeds.any { it == HilightFrames.OFF })
    }

    @Test
    fun batteryFillsToLevelThenHolds() {
        val effect = HilightEffect(HilightPattern.BATTERY, red)
        val frameStart = HilightFrames.frame(effect, 0, 8, batteryLevel = 50)
        assertEquals(1, frameStart.count { it == red })
        assertEquals(7, frameStart.count { it == HilightFrames.OFF })

        val frameEnd = HilightFrames.frame(effect, 8 * 150L, 8, batteryLevel = 50)
        assertEquals(4, frameEnd.count { it == red })
        assertEquals(4, frameEnd.count { it == HilightFrames.OFF })
        for (i in 0 until 4) {
            assertEquals(red, frameEnd[i])
        }
        for (i in 4 until 8) {
            assertEquals(HilightFrames.OFF, frameEnd[i])
        }
    }

    @Test
    fun brightnessScalesEveryLed() {
        val effect50 = HilightEffect(HilightPattern.SOLID, red, brightness = 50)
        assertArrayEquals(IntArray(8) { HilightFrames.scale(red, 0.5f) }, HilightFrames.frame(effect50, 0, 8))

        val effect100 = HilightEffect(HilightPattern.SOLID, red, brightness = 100)
        assertArrayEquals(IntArray(8) { red }, HilightFrames.frame(effect100, 0, 8))
    }

    @Test
    fun durationUsesWholeCycles() {
        val glow = HilightEffect(HilightPattern.GLOW, red, durationMs = 3000, repeats = 3)
        assertEquals(6_000L, HilightFrames.durationMs(glow, 8))

        val rainbow = HilightEffect(HilightPattern.RAINBOW, red, durationMs = 3000, repeats = 10)
        assertEquals(30_000L, HilightFrames.durationMs(rainbow, 8))

        val solid = HilightEffect(HilightPattern.SOLID, red, durationMs = 5000, repeats = 3)
        assertEquals(5_000L, HilightFrames.durationMs(solid, 8))

        val repeatsZero = HilightEffect(HilightPattern.GLOW, red, durationMs = 5000, repeats = 0)
        assertEquals(5_000L, HilightFrames.durationMs(repeatsZero, 8))
    }

    @Test
    fun repeatsForDerivesFromLegacyDuration() {
        val effect = HilightEffect(HilightPattern.GLOW, red, durationMs = 6000, repeats = 0)
        assertEquals(3, HilightFrames.repeatsFor(effect))
    }

    @Test
    fun oldHilightJsonLoadsWithDefaults() {
        val json = """{"type":"Hilight","pattern":"GLOW","color":-48060,"durationMs":5000}"""
        val action = ActionGsonAdapter.fromJson(json) as Action.Hilight
        assertEquals(100, action.brightness)
        assertEquals(0, action.repeats)
        val effect = action.toEffect()
        assertEquals(100, effect.brightness)
        assertEquals(0, effect.repeats)
    }

    @Test
    fun unknownPatternNamesFallBackToGlow() {
        val gson = Gson()
        val effect = gson.fromJson("""{"pattern":"BREATHE","color":-1,"durationMs":3000}""", HilightEffect::class.java)
        assertEquals(HilightPattern.GLOW, effect.withValidPattern().pattern)
        val action = gson.fromJson("""{"pattern":"CHASE","color":-1,"durationMs":3000}""", Action.Hilight::class.java)
        assertEquals(HilightPattern.GLOW, action.toEffect().pattern)
    }

    @Test
    fun appColorOrFallsBackWhenTransparent() {
        assertEquals(red, HilightEffect.appColorOr(0x00000000, red))
        assertEquals(red, HilightEffect.appColorOr(0x00123456, red))
        assertEquals(0xFF00FF00.toInt(), HilightEffect.appColorOr(0xFF00FF00.toInt(), red))
    }

    @Test
    fun autoColorRoundTripsAndDefaultsOff() {
        val gson = Gson()
        val loaded = gson.fromJson(gson.toJson(HilightEffect(autoColor = true)), HilightEffect::class.java)
        assertEquals(true, loaded.autoColor)

        val legacy = gson.fromJson("""{"pattern":"GLOW","color":-48060,"durationMs":3000}""", HilightEffect::class.java)
        assertEquals(false, legacy.autoColor)
    }

    @Test
    fun withShownRepeatsFillsTheShownCount() {
        val glowDefault = HilightEffect(HilightPattern.GLOW, red, durationMs = 3000, repeats = 0)
        assertEquals(2, HilightFrames.withShownRepeats(glowDefault).repeats)

        val solid = HilightEffect(HilightPattern.SOLID, red, repeats = 0)
        assertEquals(0, HilightFrames.withShownRepeats(solid).repeats)

        val glowExplicit = HilightEffect(HilightPattern.GLOW, red, repeats = 4)
        assertEquals(4, HilightFrames.withShownRepeats(glowExplicit).repeats)
    }
}
