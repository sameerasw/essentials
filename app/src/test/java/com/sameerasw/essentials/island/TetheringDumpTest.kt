package com.sameerasw.essentials.island

import com.sameerasw.essentials.island.plugins.hotspot.TetheringDump
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TetheringDumpTest {
    private fun dump(clients: String) =
        """
        Tethering:
          Device map:
            <empty>
          Client Information:
        $clients
          IPv4 Upstream Indices:
            <empty>
        """.trimIndent()

    @Test fun emptyClientListIsZero() = assertEquals(0, TetheringDump.clientCount(dump("    <empty>")))

    @Test fun countsEachClientAcrossInterfaces() {
        val clients =
            """
            |    {wlan1=[ClientInfo{client: /192.168.43.2, hwAddr: aa}, ClientInfo{client: /192.168.43.3, hwAddr: bb}]}
            |    {rndis0=[ClientInfo{client: /192.168.42.2, hwAddr: cc}]}
            """.trimMargin()
        assertEquals(3, TetheringDump.clientCount(dump(clients)))
    }

    @Test fun missingSectionIsUnknown() = assertNull(TetheringDump.clientCount("Tethering:\n  Device map:\n    <empty>"))
}
