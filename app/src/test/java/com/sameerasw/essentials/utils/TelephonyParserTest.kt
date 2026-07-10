package com.sameerasw.essentials.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TelephonyParserTest {

    @Test
    fun parseActiveSims_filtersDisabledESIMsAndTraces() {
        val sampleDumpsysIsub = """
Active modem count=2
activeDataSubId=5
Active subscriptions:
  [SubscriptionInfoInternal: id=3 iccId=899186104[****] simSlotIndex=1 portIndex=0 isEmbedded=1 carrierId=2018 displayName=Jio carrierName=Jio True5G]
  [SubscriptionInfoInternal: id=4 iccId=899100090[****] simSlotIndex=-1 portIndex=-1 isEmbedded=0 carrierId=1961 displayName=Airtel carrierName=airtel]
  [SubscriptionInfoInternal: id=5 iccId=899145227[****] simSlotIndex=0 portIndex=1 isEmbedded=1 carrierId=1961 displayName=Airtel carrierName=airtel Wi-Fi Calling]
  [SubscriptionInfoInternal: id=6 iccId=899145227[****] simSlotIndex=-1 portIndex=-1 isEmbedded=0 carrierId=1961 displayName=CARD 5 carrierName=]

Embedded subscriptions: [1, 3, 5]
All Subscription Info List:
  [SubscriptionInfoInternal: id=3 iccId=899186104[****] simSlotIndex=1 portIndex=0 isEmbedded=1 carrierId=2018 displayName=Jio carrierName=Jio True5G]
  [SubscriptionInfoInternal: id=4 iccId=899100090[****] simSlotIndex=-1 portIndex=-1 isEmbedded=0 carrierId=1961 displayName=Airtel carrierName=airtel]
  [SubscriptionInfoInternal: id=5 iccId=899145227[****] simSlotIndex=0 portIndex=1 isEmbedded=1 carrierId=1961 displayName=Airtel carrierName=airtel Wi-Fi Calling]
  [SubscriptionInfoInternal: id=6 iccId=899145227[****] simSlotIndex=-1 portIndex=-1 isEmbedded=0 carrierId=1961 displayName=CARD 5 carrierName=]
        """.trimIndent()

        val parsedSims = TelephonyParser.parseActiveSims(sampleDumpsysIsub)

        // There should be exactly 2 active SIMs parsed
        assertEquals(2, parsedSims.size)

        // The first parsed SIM should be Airtel (slot 0)
        assertEquals(5, parsedSims[0].id)
        assertEquals(0, parsedSims[0].slotIndex)
        assertEquals("Airtel", parsedSims[0].displayName)

        // The second parsed SIM should be Jio (slot 1)
        assertEquals(3, parsedSims[1].id)
        assertEquals(1, parsedSims[1].slotIndex)
        assertEquals("Jio", parsedSims[1].displayName)
    }

    @Test
    fun parseActiveSims_returnsEmptyOnEmptyOutput() {
        val parsedSims = TelephonyParser.parseActiveSims("")
        assertEquals(0, parsedSims.size)
    }

    @Test
    fun parseActiveSims_ignoresNonActiveSectionSubscriptions() {
        val sampleDumpsysIsub = """
All Subscription Info List:
  [SubscriptionInfoInternal: id=3 iccId=899186104[****] simSlotIndex=1 portIndex=0 isEmbedded=1 carrierId=2018 displayName=Jio carrierName=Jio True5G]
  [SubscriptionInfoInternal: id=5 iccId=899145227[****] simSlotIndex=0 portIndex=1 isEmbedded=1 carrierId=1961 displayName=Airtel carrierName=airtel Wi-Fi Calling]
        """.trimIndent()

        val parsedSims = TelephonyParser.parseActiveSims(sampleDumpsysIsub)
        assertEquals(0, parsedSims.size) // No Active subscriptions header, so it should return empty list
    }
}
