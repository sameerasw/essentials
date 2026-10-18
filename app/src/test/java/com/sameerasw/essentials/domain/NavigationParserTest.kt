package com.sameerasw.essentials.domain

import com.sameerasw.essentials.domain.model.NavigationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationParserTest {
    @Test
    fun distanceInTitle() {
        val p = NavigationParser.parse("300 m", "Turn right onto Main St", "12 min · 4.5 km", null)
        assertEquals("300 m", p.distance)
        assertEquals("Turn right onto Main St", p.instruction)
        assertEquals("12 min · 4.5 km", p.eta)
    }

    @Test
    fun distanceInText() {
        val p = NavigationParser.parse("Gira a la derecha hacia Av. Juárez", "1,2 km", null, null)
        assertEquals("1,2 km", p.distance)
        assertEquals("Gira a la derecha hacia Av. Juárez", p.instruction)
        assertNull(p.eta)
    }

    @Test
    fun chipWinsForDistance() {
        val p = NavigationParser.parse("Turn left", "Toward downtown", "3:45 PM", "0.2 mi")
        assertEquals("0.2 mi", p.distance)
        assertEquals("Turn left", p.instruction)
        assertEquals("3:45 PM", p.eta)
    }

    @Test
    fun noDistanceKeepsTextAsDetail() {
        val p = NavigationParser.parse("Head north", "Toward Main St", null, null)
        assertNull(p.distance)
        assertEquals("Head north", p.instruction)
        assertEquals("Toward Main St", p.eta)
    }

    @Test
    fun nonLatinDistanceInTitle() {
        val p = NavigationParser.parse("300 м", "Поверните направо", null, null)
        assertEquals("300 м", p.distance)
        assertEquals("Поверните направо", p.instruction)
    }

    @Test
    fun longTextIsNotADistance() {
        val p = NavigationParser.parse("Continue on Highway 15D for 25 km", null, null, null)
        assertNull(p.distance)
    }

    @Test
    fun exitAction() {
        assertEquals(1, NavigationParser.exitActionIndex(listOf("Mute", "Exit navigation")))
        assertEquals(0, NavigationParser.exitActionIndex(listOf("Salir")))
        assertEquals(-1, NavigationParser.exitActionIndex(listOf("Something")))
        assertEquals(-1, NavigationParser.exitActionIndex(listOf("Mute")))
        assertEquals(-1, NavigationParser.exitActionIndex(listOf("Mute", "Send")))
        assertEquals(-1, NavigationParser.exitActionIndex(emptyList()))
    }

    @Test
    fun meters() {
        assertEquals(240 * 0.9144, NavigationParser.meters("240 yd")!!, 0.01)
        assertEquals(1200.0, NavigationParser.meters("1,2 km")!!, 0.01)
        assertEquals(1200 * 0.3048, NavigationParser.meters("1,200 ft")!!, 0.01)
        assertEquals(0.2 * 1609.344, NavigationParser.meters("0.2 mi")!!, 0.01)
        assertEquals(300.0, NavigationParser.meters("300 m")!!, 0.01)
        assertEquals(50.0, NavigationParser.meters("In 50m")!!, 0.01)
        assertEquals(300.0, NavigationParser.meters("300 м")!!, 0.01)
        assertEquals(1500.0, NavigationParser.meters("1,5 км")!!, 0.01)
        assertEquals(300.0, NavigationParser.meters("300米")!!, 0.01)
        assertEquals(1200.0, NavigationParser.meters("1.2公里")!!, 0.01)
        assertEquals(1200.0, NavigationParser.meters("1.2千米")!!, 0.01)
        assertNull(NavigationParser.meters("5 мин"))
        assertNull(NavigationParser.meters("Western Road"))
        assertNull(NavigationParser.meters(null))
    }
}
