/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: SnippetExpansionHandler.kt
 * Description: Accessibility event handler for expanding text snippets across any input method or app.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ime.EssentialsInputMethodService
import com.sameerasw.essentials.ime.SnippetExpansionEngine

class SnippetExpansionHandler(
    private val service: AccessibilityService,
) {
    private val settings = SettingsRepository(service)
    @Volatile private var expectedSelfText: String? = null

    private val snippetEngine =
        SnippetExpansionEngine(
            getSnippets = { settings.getTextSnippets() },
            getPrefix = { settings.getSnippetPrefix() },
            isEnabled = { settings.isTextSnippetsEnabled() },
        )

    fun onTextChanged(event: AccessibilityEvent) {
        if (!settings.isTextSnippetsEnabled()) return
        if (EssentialsInputMethodService.isInputActive) return
        if (event.isPassword) return

        val source = event.source ?: return
        if (source.isPassword || !source.isEditable) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && source.isShowingHintText) return

        val inputType = source.inputType
        val isPasswordType =
            (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                ((inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER &&
                    (inputType and InputType.TYPE_MASK_VARIATION) == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        if (isPasswordType) return

        val text = source.text?.toString() ?: return
        if (text.isEmpty()) return

        if (expectedSelfText != null) {
            if (text == expectedSelfText) {
                expectedSelfText = null
                return
            }
        }

        var cursor = source.textSelectionEnd
        if (cursor < 0 || cursor > text.length) {
            cursor = text.length
        }

        val textBeforeCursor = text.substring(0, cursor)

        snippetEngine.checkAndExpand(textBeforeCursor) { deleteCount, replacement ->
            val start = cursor - deleteCount
            if (start < 0) return@checkAndExpand

            val newText = text.substring(0, start) + replacement + text.substring(cursor)
            val newCursor = start + replacement.length

            val supportsSetText = source.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }
            if (!supportsSetText) return@checkAndExpand

            expectedSelfText = newText
            val args =
                Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        newText,
                    )
                }
            val setSuccess = source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (setSuccess) {
                val selArgs =
                    Bundle().apply {
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursor)
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursor)
                    }
                source.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs)
            } else {
                expectedSelfText = null
            }
        }
    }
}
