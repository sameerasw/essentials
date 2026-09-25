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
import com.sameerasw.essentials.island.service.IslandCoordinator
import com.sameerasw.essentials.island.service.OverlayLifecycleOwner
import com.sameerasw.essentials.ui.ime.snippets.SnippetFloatingPillView
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class SnippetAccessibilityHandler(
    private val service: AccessibilityService,
    private val scope: CoroutineScope,
    var islandCoordinator: IslandCoordinator? = null,
    var duoOverlayHandler: DuoOverlayHandler? = null,
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
    private var lastInputBounds: android.graphics.Rect? = null
    private var lastTypedWord: String = ""

    private var overlayView: FrameLayout? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    private val dismissRunnable = Runnable { hideFloatingPill() }

    fun updateSettings() {
        isUniversalEnabled = settingsRepository.isSnippetsUniversalEnabled()
        isAutoExpandEnabled = settingsRepository.isSnippetsUniversalAutoExpandEnabled()
        isFloatingPillEnabled = settingsRepository.isSnippetsUniversalFloatingPillEnabled()
        if (!isUniversalEnabled) {
            dismissAllSuggestions()
        }
    }

    fun onTextChanged(event: AccessibilityEvent) {
        if (!isUniversalEnabled) return

        val sourceNode = event.source ?: return
        if (!sourceNode.isEditable) return

        activeNode = sourceNode
        val bounds = android.graphics.Rect()
        sourceNode.getBoundsInScreen(bounds)
        if (bounds.height() > 0 && bounds.top > 0) {
            lastInputBounds = bounds
        }

        val textList = event.text
        val fullText =
            if (textList.isNotEmpty()) {
                textList.joinToString("")
            } else {
                sourceNode.text?.toString().orEmpty()
            }

        if (fullText.isEmpty()) {
            dismissAllSuggestions()
            return
        }

        // 1. Check for Option A: Space-based instant auto-expand
        if (fullText.endsWith(" ") && isAutoExpandEnabled) {
            val textBeforeSpace = fullText.dropLast(1)
            val candidateWord = textBeforeSpace.takeLastWhile { !it.isWhitespace() }

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
                    dismissAllSuggestions()
                    return
                }
            }
        }

        // 2. Check for Option B: Suggestion trigger (Pill, Island, Duo, or Both)
        val currentWord = fullText.takeLastWhile { !it.isWhitespace() }
        lastTypedWord = currentWord

        if (currentWord.isNotBlank()) {
            val matches = snippetRepository.findMatching(currentWord)
            if (matches.isNotEmpty()) {
                showSuggestion(matches.first())
            } else {
                dismissAllSuggestions()
            }
        } else {
            dismissAllSuggestions()
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

    private fun calculateImeOffset(): Int {
        val density = service.resources.displayMetrics.density
        val displayHeight = service.resources.displayMetrics.heightPixels
        var keyboardHeight = (290 * density).toInt()

        try {
            val currentWindows = service.windows
            if (!currentWindows.isNullOrEmpty()) {
                val imeWindow =
                    currentWindows.firstOrNull {
                        it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
                    }
                if (imeWindow != null) {
                    val imeBounds = android.graphics.Rect()
                    imeWindow.getBoundsInScreen(imeBounds)
                    if (imeBounds.height() > 0 && imeBounds.top < displayHeight) {
                        keyboardHeight = (displayHeight - imeBounds.top).coerceAtLeast(0)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating IME window bounds", e)
        }

        // Dynamically measure active input field to ensure the pill floats comfortably above it
        val bounds = lastInputBounds
        if (bounds != null && bounds.height() > 0 && bounds.top > 0 && bounds.top < displayHeight) {
            val nodeTopFromBottom = displayHeight - bounds.top
            if (nodeTopFromBottom >= keyboardHeight && bounds.top > (80 * density)) {
                return nodeTopFromBottom + (12 * density).toInt()
            }
        }

        // Fallback: place comfortably above standard multi-line input bar
        return keyboardHeight + (72 * density).toInt()
    }

    private fun expandSnippet(snippet: Snippet) {
        overlayView?.let { HapticUtil.performUIHaptic(it) }
        val targetNode = activeNode?.takeIf {
            try { it.refresh() } catch (_: Exception) { false }
        } ?: service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (targetNode != null) {
            val text = targetNode.text?.toString().orEmpty()
            val expanded = SnippetExpander.expand(snippet.content, service)
            replaceKeywordInNode(
                node = targetNode,
                fullText = text,
                targetWord = lastTypedWord,
                replacement = "$expanded ",
            )
        }
        dismissAllSuggestions()
    }

    private fun showSuggestion(snippet: Snippet) {
        val mode = settingsRepository.getSnippetsSuggestionDisplayMode()
        val onExpand = { expandSnippet(snippet) }

        when (mode) {
            SettingsRepository.SNIPPETS_DISPLAY_FLOATING_PILL -> {
                islandCoordinator?.hideSnippet()
                duoOverlayHandler?.hideSnippetSuggestion()
                showFloatingPill(snippet, onExpand)
            }
            SettingsRepository.SNIPPETS_DISPLAY_DYNAMIC_ISLAND -> {
                hideFloatingPill()
                duoOverlayHandler?.hideSnippetSuggestion()
                if (settingsRepository.isIslandEnabled()) {
                    islandCoordinator?.showSnippet(snippet, onExpand)
                } else {
                    showFloatingPill(snippet, onExpand)
                }
            }
            SettingsRepository.SNIPPETS_DISPLAY_DUO -> {
                hideFloatingPill()
                islandCoordinator?.hideSnippet()
                if (settingsRepository.isDuoEnabled()) {
                    duoOverlayHandler?.showSnippetSuggestion(snippet, onExpand)
                } else {
                    showFloatingPill(snippet, onExpand)
                }
            }
            SettingsRepository.SNIPPETS_DISPLAY_BOTH -> {
                hideFloatingPill()
                val islandOn = settingsRepository.isIslandEnabled()
                val duoOn = settingsRepository.isDuoEnabled()
                if (islandOn) {
                    islandCoordinator?.showSnippet(snippet, onExpand)
                }
                if (duoOn) {
                    duoOverlayHandler?.showSnippetSuggestion(snippet, onExpand)
                }
                if (!islandOn && !duoOn) {
                    showFloatingPill(snippet, onExpand)
                }
            }
            else -> {
                islandCoordinator?.hideSnippet()
                duoOverlayHandler?.hideSnippetSuggestion()
                showFloatingPill(snippet, onExpand)
            }
        }
    }

    fun dismissAllSuggestions() {
        hideFloatingPill()
        islandCoordinator?.hideSnippet()
        duoOverlayHandler?.hideSnippetSuggestion()
    }

    private fun showFloatingPill(snippet: Snippet, onExpand: () -> Unit) {
        mainHandler.post {
            currentMatchedSnippet.value = snippet
            mainHandler.removeCallbacks(dismissRunnable)
            mainHandler.postDelayed(dismissRunnable, AUTO_DISMISS_TIMEOUT_MS)

            if (overlayView != null) {
                // Overlay already visible; update positioning dynamically if keyboard height changed
                try {
                    val frame = overlayView ?: return@post
                    val lp = frame.layoutParams as? WindowManager.LayoutParams
                    if (lp != null) {
                        val newY = calculateImeOffset()
                        if (lp.y != newY) {
                            lp.y = newY
                            windowManager.updateViewLayout(frame, lp)
                        }
                    }
                } catch (_: Exception) {
                }
                return@post
            }

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
                                    onExpand = onExpand,
                                    onDismiss = { dismissAllSuggestions() },
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
                    y = calculateImeOffset()
                }

            val density = service.resources.displayMetrics.density
            var initialY = 0
            var initialTouchY = 0f

            frame.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_OUTSIDE -> {
                        dismissAllSuggestions()
                        true
                    }
                    MotionEvent.ACTION_DOWN -> {
                        val lp = frame.layoutParams as? WindowManager.LayoutParams
                        initialY = lp?.y ?: 0
                        initialTouchY = event.rawY
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dy = (initialTouchY - event.rawY).toInt()
                        if (kotlin.math.abs(dy) > (8 * density).toInt()) {
                            val lp = frame.layoutParams as? WindowManager.LayoutParams
                            if (lp != null) {
                                lp.y = (initialY + dy).coerceAtLeast(0)
                                try {
                                    windowManager.updateViewLayout(frame, lp)
                                } catch (_: Exception) {}
                            }
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
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

    fun onNonImeWindowChanged() {
        if (!isImePresent()) {
            dismissAllSuggestions()
        }
    }

    private fun isImePresent(): Boolean {
        return try {
            val currentWindows = service.windows
            if (currentWindows.isNullOrEmpty()) return false
            val displayHeight = service.resources.displayMetrics.heightPixels
            val imeWindow =
                currentWindows.firstOrNull {
                    it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
                }
            if (imeWindow != null) {
                val bounds = android.graphics.Rect()
                imeWindow.getBoundsInScreen(bounds)
                bounds.height() > 0 && bounds.top < displayHeight
            } else {
                false
            }
        } catch (_: Exception) {
            false
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
        dismissAllSuggestions()
        activeNode = null
    }
}
