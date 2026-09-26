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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SnippetExpander {
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val TIME_FORMAT = SimpleDateFormat("h:mm a", Locale.getDefault())

    /**
     * Replaces dynamic placeholders such as {date}, {time}, and {clipboard} with actual values.
     */
    fun expand(template: String, context: Context?): String {
        var result = template
        if (result.contains("{date}", ignoreCase = true)) {
            result = result.replace("{date}", DATE_FORMAT.format(Date()), ignoreCase = true)
        }
        if (result.contains("{time}", ignoreCase = true)) {
            result = result.replace("{time}", TIME_FORMAT.format(Date()), ignoreCase = true)
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
            cm?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
        } catch (_: Exception) {
            ""
        }
    }
}
