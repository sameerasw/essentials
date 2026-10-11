/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: IME - Snippets
 * File: SnippetExpander.kt
 * Description: Evaluates dynamic placeholder tags inside snippets.
 */

package com.sameerasw.essentials.ime.snippets

import android.content.ClipboardManager
import android.content.Context
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object SnippetExpander {
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    fun expand(template: String, context: Context?): String {
        var result = template
        if (result.contains("{date}", ignoreCase = true)) {
            result = result.replace("{date}", LocalDate.now().format(DATE_FORMAT), ignoreCase = true)
        }
        if (result.contains("{time}", ignoreCase = true)) {
            result = result.replace("{time}", LocalTime.now().format(TIME_FORMAT), ignoreCase = true)
        }
        if (result.contains("{clipboard}", ignoreCase = true)) {
            val clipText = context?.let { getClipboardText(it) } ?: ""
            result = result.replace("{clipboard}", clipText, ignoreCase = true)
        }
        return result
    }

    private fun getClipboardText(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0)?.text?.toString().orEmpty()
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }
}
