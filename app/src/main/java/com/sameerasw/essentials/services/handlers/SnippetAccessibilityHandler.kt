/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers - Handlers
 * File: SnippetAccessibilityHandler.kt
 * Description: Accessibility service handler providing universal snippet expansion and floating pills across external keyboards.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ime.snippets.Snippet
import com.sameerasw.essentials.ime.snippets.SnippetExpander
import com.sameerasw.essentials.ime.snippets.SnippetRepository
import com.sameerasw.essentials.island.service.OverlayLifecycleOwner
import com.sameerasw.essentials.ui.ime.snippets.SnippetFloatingPillView
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class SnippetAccessibilityHandler(
    private val service: AccessibilityService,
    private val scope: CoroutineScope,
) {
    companion object {
        private const val TAG = "SnippetA11yHandler"
        private const val AUTO_DISMISS_TIMEOUT_MS = 6000L
    }

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val settingsRepository = SettingsRepository(service)
    private val snippetRepository = SnippetRepository.getInstance(service)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isUniversalEnabled = settingsRepository.isSnippetsUniversalEnabled()
    private var isAutoExpandEnabled = settingsRepository.isSnippetsUniversalAutoExpandEnabled()
    private var isFloatingPillEnabled = settingsRepository.isSnippetsUniversalFloatingPillEnabled()

    private val currentMatchedSnippet = MutableStateFlow<Snippet?>(null)
    private var activeNode: AccessibilityNodeInfo? = null
    private var lastTypedWord: String = ""

    private var overlayView: FrameLayout? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private val dismissRunnable = Runnable { hideFloatingPill() }

    fun updateSettings() {
        isUniversalEnabled = settingsRepository.isSnippetsUniversalEnabled()
        isAutoExpandEnabled = settingsRepository.isSnippetsUniversalAutoExpandEnabled()
        isFloatingPillEnabled = settingsRepository.isSnippetsUniversalFloatingPillEnabled()
        if (!isUniversalEnabled) {
            hideFloatingPill()
        }
    }

    fun onTextChanged(event: AccessibilityEvent) {
        if (!isUniversalEnabled) return

        val sourceNode = event.source ?: return
        if (!sourceNode.isEditable) return

        activeNode = sourceNode

        val textList = event.text
        val fullText =
            if (textList.isNotEmpty()) {
                textList.joinToString("")
            } else {
                sourceNode.text?.toString().orEmpty()
            }

        if (fullText.isEmpty()) {
            hideFloatingPill()
            return
        }

        // 1. Check for Option A: Space-based instant auto-expand
        if (fullText.endsWith(" ") && isAutoExpandEnabled) {
            val textBeforeSpace = fullText.dropLast(1)
            val candidateWord = textBeforeSpace.substringAfterLast(' ', "").ifBlank { textBeforeSpace }

            if (candidateWord.isNotBlank()) {
                val matchingSnippet =
                    snippetRepository.snippets.value.firstOrNull { snippet ->
                        snippet.keyword.equals(candidateWord, ignoreCase = false)
                    }

                if (matchingSnippet != null && matchingSnippet.autoExpandOnSpace) {
                    val expanded = SnippetExpander.expand(matchingSnippet.content, service)
                    replaceKeywordInNode(
                        node = sourceNode,
                        fullText = fullText,
                        targetWord = "$candidateWord ",
                        replacement = "$expanded ",
                    )
                    hideFloatingPill()
                    return
                }
            }
        }

        // 2. Check for Option B: Floating pill trigger
        val currentWord = fullText.substringAfterLast(' ', "").ifBlank { fullText }
        lastTypedWord = currentWord

        if (currentWord.isNotBlank() && isFloatingPillEnabled) {
            val matches = snippetRepository.findMatching(currentWord)
            if (matches.isNotEmpty()) {
                showFloatingPill(matches.first())
            } else {
                hideFloatingPill()
            }
        } else {
            hideFloatingPill()
        }
    }

    private fun replaceKeywordInNode(
        node: AccessibilityNodeInfo,
        fullText: String,
        targetWord: String,
        replacement: String,
    ) {
        val targetIndex = fullText.lastIndexOf(targetWord)
        if (targetIndex < 0) return

        val newText =
            fullText.substring(0, targetIndex) +
                replacement +
                fullText.substring(targetIndex + targetWord.length)

        val args =
            Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
            }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        // Set cursor position at the end of replacement
        val newCursorPos = targetIndex + replacement.length
        val cursorArgs =
            Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursorPos)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursorPos)
            }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, cursorArgs)
    }

    private fun showFloatingPill(snippet: Snippet) {
        mainHandler.post {
            currentMatchedSnippet.value = snippet
            mainHandler.removeCallbacks(dismissRunnable)
            mainHandler.postDelayed(dismissRunnable, AUTO_DISMISS_TIMEOUT_MS)

            if (overlayView == null) {
                val lifecycle = OverlayLifecycleOwner().also { it.onCreate() }
                lifecycleOwner = lifecycle

                val composeView =
                    ComposeView(service).apply {
                        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                        setContent {
                            EssentialsTheme {
                                val currentSnippet = currentMatchedSnippet.collectAsState().value
                                if (currentSnippet != null) {
                                    SnippetFloatingPillView(
                                        snippet = currentSnippet,
                                        onExpand = {
                                            overlayView?.let { HapticUtil.performUIHaptic(it) }
                                            activeNode?.let { node ->
                                                val text = node.text?.toString().orEmpty()
                                                val expanded =
                                                    SnippetExpander.expand(currentSnippet.content, service)
                                                replaceKeywordInNode(
                                                    node = node,
                                                    fullText = text,
                                                    targetWord = lastTypedWord,
                                                    replacement = "$expanded ",
                                                )
                                            }
                                            hideFloatingPill()
                                        },
                                        onDismiss = { hideFloatingPill() },
                                    )
                                }
                            }
                        }
                    }

                val frame =
                    FrameLayout(service).apply {
                        setViewTreeLifecycleOwner(lifecycle)
                        setViewTreeSavedStateRegistryOwner(lifecycle)
                        setViewTreeViewModelStoreOwner(lifecycle)
                        addView(composeView)
                    }

                val density = service.resources.displayMetrics.density
                val params =
                    WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                        PixelFormat.TRANSLUCENT,
                    ).apply {
                        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        y = (290 * density).toInt()
                    }

                frame.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_OUTSIDE) {
                        hideFloatingPill()
                        true
                    } else {
                        false
                    }
                }

                overlayView = frame
                try {
                    windowManager.addView(frame, params)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to display snippet floating pill overlay", e)
                    overlayView = null
                }
            }
        }
    }

    fun hideFloatingPill() {
        mainHandler.post {
            mainHandler.removeCallbacks(dismissRunnable)
            currentMatchedSnippet.value = null
            val view = overlayView ?: return@post
            overlayView = null
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
            lifecycleOwner?.onDestroy()
            lifecycleOwner = null
        }
    }

    fun destroy() {
        hideFloatingPill()
        activeNode = null
    }
}
