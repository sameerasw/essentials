/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Unit Tests - IME
 * File: SnippetExpanderTest.kt
 * Description: Unit tests for SnippetExpander dynamic placeholder evaluation.
 */

package com.sameerasw.essentials.ime

import com.sameerasw.essentials.ime.snippets.SnippetExpander
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class SnippetExpanderTest {
    @Test
    fun expand_staticText_returnsUnchanged() {
        val template = "Hello, world!"
        val result = SnippetExpander.expand(template, null)
        assertEquals("Hello, world!", result)
    }

    @Test
    fun expand_datePlaceholder_replacesWithFormattedDate() {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()))
        val template = "Today is {date}."
        val result = SnippetExpander.expand(template, null)
        assertEquals("Today is $today.", result)
    }

    @Test
    fun expand_timePlaceholder_replacesWithTime() {
        val template = "Current time is {time}."
        val result = SnippetExpander.expand(template, null)
        assertTrue(result.contains("AM") || result.contains("PM"))
    }

    @Test
    fun expand_nullContextClipboard_replacesWithEmptyString() {
        val template = "Quoted: {clipboard}"
        val result = SnippetExpander.expand(template, null)
        assertEquals("Quoted: ", result)
    }
}
