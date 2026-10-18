package com.sameerasw.essentials.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapsLinkParserTest {
    @Test
    fun placeLinkUsesThePinNotTheViewport() {
        val url = "https://www.google.com/maps/place/Cubbon+Park/@12.9700000,77.5900000,15z/data=!4m6!3m5!1s0x0:0x0!8m2!3d12.9763472!4d77.5929284"
        val place = MapsLinkParser.parse(url)!!
        assertEquals(12.9763472, place.latitude, 1e-7)
        assertEquals(77.5929284, place.longitude, 1e-7)
        assertEquals("Cubbon Park", place.name)
    }

    @Test
    fun sharedTextTakesTheNameFromItsFirstLine() {
        val text = "Lalbagh Botanical Garden\nhttps://www.google.com/maps/search/?api=1&query=12.9507,77.5848"
        val place = MapsLinkParser.parse(text)!!
        assertEquals(12.9507, place.latitude, 1e-7)
        assertEquals(77.5848, place.longitude, 1e-7)
        assertEquals("Lalbagh Botanical Garden", place.name)
    }

    @Test
    fun geoUrisParse() {
        assertEquals(51.5007, MapsLinkParser.parse("geo:51.5007,-0.1246?q=Big+Ben")!!.latitude, 1e-7)
    }

    @Test
    fun viewportCentreIsNotTakenAsThePlace() {
        assertNull(MapsLinkParser.parse("https://www.google.com/maps/@-33.8568,151.2153,17z"))
        assertNull(MapsLinkParser.parse("https://www.google.com/maps/place/Opera+House/@-33.8568,151.2153,17z"))
    }

    @Test
    fun bareCoordinatesAreNotUsedAsTheName() {
        val place = MapsLinkParser.parse("12.9507, 77.5848")!!
        assertNull(place.name)
    }

    @Test
    fun linksWithoutValidCoordinatesAreRejected() {
        assertNull(MapsLinkParser.parse("https://maps.app.goo.gl/abc123"))
        assertNull(MapsLinkParser.parse("0.0, 0.0"))
        assertNull(MapsLinkParser.parse("95.1, 10.2"))
    }

    @Test
    fun shortLinksAreRecognised() {
        assertTrue(MapsLinkParser.isShortLink("https://maps.app.goo.gl/abc123"))
        assertFalse(MapsLinkParser.isShortLink("https://www.google.com/maps/place/x"))
        assertEquals("https://maps.app.goo.gl/abc123", MapsLinkParser.findUrl("Home\nhttps://maps.app.goo.gl/abc123"))
    }
}
