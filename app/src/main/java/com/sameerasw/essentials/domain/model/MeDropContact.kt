/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer
 * File: MeDropContact.kt
 */

package com.sameerasw.essentials.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MeDropContact(
    val lookupKey: String,
    val displayName: String,
    val photoUri: String? = null,
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val organization: String? = null,
    val jobTitle: String? = null,
    val addresses: List<String> = emptyList(),
    val urls: List<String> = emptyList(),
    val note: String? = null,
    val selectedEntryIds: Set<String>? = null
) {
    @Suppress("SENSELESS_COMPARISON")
    fun getSafePhones(): List<String> = if (phones != null) phones else emptyList()
    @Suppress("SENSELESS_COMPARISON")
    fun getSafeEmails(): List<String> = if (emails != null) emails else emptyList()
    @Suppress("SENSELESS_COMPARISON")
    fun getSafeAddresses(): List<String> = if (addresses != null) addresses else emptyList()
    @Suppress("SENSELESS_COMPARISON")
    fun getSafeUrls(): List<String> = if (urls != null) urls else emptyList()

    @Suppress("SENSELESS_COMPARISON")
    fun getActiveEntryIds(): Set<String> {
        if (selectedEntryIds != null) return selectedEntryIds
        val all = mutableSetOf<String>()
        getSafePhones().forEachIndexed { i, _ -> all.add("phone_$i") }
        getSafeEmails().forEachIndexed { i, _ -> all.add("email_$i") }
        if (!organization.isNullOrBlank()) all.add("organization")
        if (!jobTitle.isNullOrBlank()) all.add("jobTitle")
        getSafeAddresses().forEachIndexed { i, _ -> all.add("address_$i") }
        getSafeUrls().forEachIndexed { i, _ -> all.add("url_$i") }
        if (!note.isNullOrBlank()) all.add("note")
        return all
    }

    fun isEntrySelected(id: String): Boolean = getActiveEntryIds().contains(id)

    fun toVCard(): String {
        val active = getActiveEntryIds()
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCARD")
        sb.appendLine("VERSION:3.0")
        sb.appendLine("FN:$displayName")
        sb.appendLine("N:${buildNField(displayName)}")

        if (active.contains("organization") && !organization.isNullOrBlank()) {
            sb.appendLine("ORG:$organization")
        }
        if (active.contains("jobTitle") && !jobTitle.isNullOrBlank()) {
            sb.appendLine("TITLE:$jobTitle")
        }

        getSafePhones().forEachIndexed { i, phone ->
            if (active.contains("phone_$i")) {
                sb.appendLine("TEL;TYPE=CELL:$phone")
            }
        }
        getSafeEmails().forEachIndexed { i, email ->
            if (active.contains("email_$i")) {
                sb.appendLine("EMAIL;TYPE=INTERNET:$email")
            }
        }
        getSafeAddresses().forEachIndexed { i, addr ->
            if (active.contains("address_$i")) {
                sb.appendLine("ADR;TYPE=HOME:;;${addr.replace("\n", ";")};;;")
            }
        }
        getSafeUrls().forEachIndexed { i, url ->
            if (active.contains("url_$i")) {
                sb.appendLine("URL:$url")
            }
        }
        if (active.contains("note") && !note.isNullOrBlank()) {
            sb.appendLine("NOTE:${note.replace("\n", " ") }")
        }

        val rev = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).format(Date())
        sb.appendLine("REV:$rev")
        sb.append("END:VCARD")
        return sb.toString()
    }

    private fun buildNField(displayName: String): String {
        val parts = displayName.trim().split(" ")
        val last = if (parts.size > 1) parts.last() else ""
        val first = if (parts.size > 1) parts.dropLast(1).joinToString(" ") else displayName
        return "$last;$first;;;"
    }
}
