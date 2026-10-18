package com.sameerasw.essentials.domain.controller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZenRuleParserTest {
    private class FakeAzrWithId(
        @JvmField val mId: String,
        @JvmField val mRule: String,
    )

    private class FakeParceledListSlice(
        private val items: List<Any>,
    ) {
        fun getList(): List<Any> = items
    }

    @Test
    fun mapResultIsReturnedAsIs() {
        val rules = parseZenRules<String>(mapOf("a" to "Bedtime", "b" to "Driving"))
        assertEquals(mapOf("a" to "Bedtime", "b" to "Driving"), rules)
    }

    @Test
    fun listSliceResultIsKeyedById() {
        val slice = FakeParceledListSlice(listOf(FakeAzrWithId("a", "Bedtime"), FakeAzrWithId("b", "Driving")))
        assertEquals(mapOf("a" to "Bedtime", "b" to "Driving"), parseZenRules<String>(slice))
    }

    @Test
    fun nullResultIsEmpty() {
        assertTrue(parseZenRules<String>(null).isEmpty())
    }
}
