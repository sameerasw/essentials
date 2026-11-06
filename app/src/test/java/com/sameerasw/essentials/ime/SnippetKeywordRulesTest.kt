/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Unit Tests - IME
 * File: SnippetKeywordRulesTest.kt
 * Description: Unit tests for snippet keyword validation and snippet backup import/export.
 */

package com.sameerasw.essentials.ime

import com.sameerasw.essentials.ime.snippets.Snippet
import com.sameerasw.essentials.ime.snippets.SnippetBackup
import com.sameerasw.essentials.ime.snippets.SnippetKeywordRules
import com.sameerasw.essentials.ime.snippets.SnippetKeywordRules.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnippetKeywordRulesTest {
    private val existing = listOf(Snippet(id = "1", title = "Date", keyword = "!date", content = "{date}"))

    private fun check(
        keyword: String,
        editingId: String? = null,
    ) = SnippetKeywordRules.validate(keyword, existing, editingId)

    @Test
    fun validKeyword_hasNoProblem() {
        assertNull(check("!email"))
        assertNull(check("  /sig  "))
        assertNull(check(":phone"))
        assertNull(check(";meet"))
    }

    @Test
    fun emptyKeyword_isRejected() {
        assertEquals(Problem.Empty, check("   "))
    }

    @Test
    fun whitespaceInside_isRejected() {
        assertEquals(Problem.ContainsWhitespace, check("!my email"))
    }

    @Test
    fun plainWord_requiresTriggerPrefix() {
        assertEquals(Problem.MissingTrigger, check("hi"))
        assertEquals(Problem.MissingTrigger, check("date"))
    }

    @Test
    fun triggerOnly_isRejected() {
        assertEquals(Problem.TriggerOnly, check("!"))
    }

    @Test
    fun tooLong_isRejected() {
        assertEquals(Problem.TooLong, check("!" + "a".repeat(SnippetKeywordRules.MAX_LENGTH)))
    }

    @Test
    fun duplicate_isRejectedCaseInsensitively() {
        assertEquals(Problem.Duplicate, check("!DATE"))
    }

    @Test
    fun editingSameSnippet_isNotADuplicate() {
        assertNull(check("!date", editingId = "1"))
    }

    @Test
    fun backup_roundTripsSnippets() {
        val original =
            listOf(
                Snippet(title = "Email", keyword = "!email", content = "me@example.com", autoExpandOnSpace = false),
                Snippet(title = "Sig", keyword = "/sig", content = "Thanks,\nBalaji"),
            )
        val parsed = SnippetBackup.parse(SnippetBackup.toJson(original), emptyList())
        assertEquals(0, parsed?.skipped)
        assertEquals(original.map { it.keyword }, parsed?.imported?.map { it.keyword })
        assertEquals(original.map { it.content }, parsed?.imported?.map { it.content })
        assertEquals(false, parsed?.imported?.first()?.autoExpandOnSpace)
    }

    @Test
    fun backup_skipsInvalidAndDuplicateEntries() {
        val json =
            """
            {"version":1,"snippets":[
              {"title":"ok","keyword":"!ok","content":"fine"},
              {"title":"dupe of existing","keyword":"!date","content":"x"},
              {"title":"no trigger","keyword":"plain","content":"x"},
              {"title":"empty content","keyword":"!empty","content":"  "},
              {"title":"dupe inside file","keyword":"!OK","content":"y"},
              "not an object"
            ]}
            """.trimIndent()
        val parsed = SnippetBackup.parse(json, existing)
        assertEquals(listOf("!ok"), parsed?.imported?.map { it.keyword })
        assertEquals(5, parsed?.skipped)
    }

    @Test
    fun backup_acceptsBareArray() {
        val parsed = SnippetBackup.parse("""[{"keyword":"!a1","content":"x"}]""", emptyList())
        assertEquals(listOf("!a1"), parsed?.imported?.map { it.keyword })
        assertEquals("!a1", parsed?.imported?.first()?.title)
    }

    @Test
    fun backup_rejectsNonSnippetJson() {
        assertNull(SnippetBackup.parse("""{"hello":"world"}""", emptyList()))
        assertNull(SnippetBackup.parse("not json at all {", emptyList()))
        assertTrue(SnippetBackup.parse("[]", emptyList())?.imported?.isEmpty() == true)
    }
}
