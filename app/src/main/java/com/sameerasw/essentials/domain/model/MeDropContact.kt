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
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val includePhones: Boolean = true,
    val includeEmails: Boolean = true
) {
    fun toVCard(): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCARD")
        sb.appendLine("VERSION:3.0")
        sb.appendLine("FN:$displayName")
        sb.appendLine("N:${buildNField(displayName)}")

        if (includePhones) {
            phones.forEach { phone ->
                sb.appendLine("TEL;TYPE=CELL:$phone")
            }
        }
        if (includeEmails) {
            emails.forEach { email ->
                sb.appendLine("EMAIL;TYPE=INTERNET:$email")
            }
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
