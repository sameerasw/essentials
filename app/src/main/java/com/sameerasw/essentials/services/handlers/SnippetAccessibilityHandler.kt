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
import android.provider.Settings
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
        private const val REPLACEMENT_DEBOUNCE_MS = 300L
        private const val DEFAULT_KEYBOARD_HEIGHT_DP = 290
        private const val FALLBACK_IME_PADDING_DP = 72
        private const val MIN_INPUT_BOUNDS_TOP_DP = 80
        private const val FLOATING_PILL_OFFSET_DP = 12
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

    @Volatile
    private var isPerformingReplacement = false

    fun updateSettings() {
        isUniversalEnabled = settingsRepository.isSnippetsUniversalEnabled()
        isAutoExpandEnabled = settingsRepository.isSnippetsUniversalAutoExpandEnabled()
        isFloatingPillEnabled = settingsRepository.isSnippetsUniversalFloatingPillEnabled()
        if (!isUniversalEnabled) {
            dismissAllSuggestions()
        }
    }

    private var lastImeCheckTime = 0L
    private var cachedIsEssentialsIme = false

    private fun isEssentialsImeActive(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastImeCheckTime < 2000L) {
            return cachedIsEssentialsIme
        }
        lastImeCheckTime = now
        cachedIsEssentialsIme = try {
            val currentIme = Settings.Secure.getString(
                service.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD,
            ).orEmpty()
            currentIme.startsWith(service.packageName)
        } catch (_: Exception) {
            false
        }
        return cachedIsEssentialsIme
    }

    fun onTextChanged(event: AccessibilityEvent) {
        if (!isUniversalEnabled || isPerformingReplacement) return

        val eventPkg = event.packageName?.toString().orEmpty()
        if (eventPkg == service.packageName) return

        val sourceNode = event.source ?: return
        if (event.isPassword || sourceNode.isPassword || isPasswordField(sourceNode)) return
        if (!sourceNode.isEditable) return

        activeNode?.recycle()
        activeNode = AccessibilityNodeInfo.obtain(sourceNode)
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

        // 1. Space-based instant auto-expand (skip if Essentials IME is typing, to prevent double expansion)
        if (fullText.endsWith(" ") && isAutoExpandEnabled && !isEssentialsImeActive()) {
            val textBeforeSpace = fullText.dropLast(1)
            val candidateWord = textBeforeSpace.takeLastWhile { !it.isWhitespace() }

            if (candidateWord.isNotBlank()) {
                val matchingSnippet =
                    snippetRepository.snippets.value.firstOrNull { snippet ->
                        snippet.keyword.equals(candidateWord, ignoreCase = true)
                    }

                if (matchingSnippet != null && matchingSnippet.autoExpandOnSpace) {
                    val expanded = SnippetExpander.expand(matchingSnippet.content, service)
                    val success = replaceKeywordInNode(
                        node = sourceNode,
                        targetWord = "$candidateWord ",
                        replacement = "$expanded ",
                    )
                    if (success) {
                        dismissAllSuggestions()
                        return
                    }
                }
            }
        }

        // 2. Suggestion trigger (Pill, Island, Duo, or Both)
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
        targetWord: String,
        replacement: String,
    ): Boolean {
        isPerformingReplacement = true
        try {
            val fullText = node.text?.toString().orEmpty()
            if (fullText.isEmpty()) return false

            val cursor = node.textSelectionEnd
            // Verify cursor is at the end of targetWord to prevent replacing arbitrary occurrences
            val targetIndex =
                if (cursor in targetWord.length..fullText.length &&
                    fullText.substring(cursor - targetWord.length, cursor).equals(targetWord, ignoreCase = true)
                ) {
                    cursor - targetWord.length
                } else if (fullText.endsWith(targetWord, ignoreCase = true)) {
                    fullText.length - targetWord.length
                } else {
                    return false
                }

            val newText =
                fullText.substring(0, targetIndex) +
                    replacement +
                    fullText.substring(targetIndex + targetWord.length)

            val args =
                Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                }
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (!success) return false

            val newCursorPos = targetIndex + replacement.length
            val cursorArgs =
                Bundle().apply {
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursorPos)
                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursorPos)
                }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, cursorArgs)
            return true
        } finally {
            mainHandler.postDelayed({ isPerformingReplacement = false }, REPLACEMENT_DEBOUNCE_MS)
        }
    }

    private fun isPasswordField(node: AccessibilityNodeInfo): Boolean {
        val inputType = node.inputType
        val variation = inputType and android.text.InputType.TYPE_MASK_VARIATION
        return variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            (inputType and android.text.InputType.TYPE_MASK_CLASS == android.text.InputType.TYPE_CLASS_NUMBER &&
                variation == android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD)
    }

    private fun calculateImeOffset(): Int {
        val density = service.resources.displayMetrics.density
        val displayHeight = service.resources.displayMetrics.heightPixels
        var keyboardHeight = (DEFAULT_KEYBOARD_HEIGHT_DP * density).toInt()

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

        val bounds = lastInputBounds
        if (bounds != null && bounds.height() > 0 && bounds.top > 0 && bounds.top < displayHeight) {
            val nodeTopFromBottom = displayHeight - bounds.top
            if (nodeTopFromBottom >= keyboardHeight && bounds.top > (MIN_INPUT_BOUNDS_TOP_DP * density)) {
                return nodeTopFromBottom + (FLOATING_PILL_OFFSET_DP * density).toInt()
            }
        }

        return keyboardHeight + (FALLBACK_IME_PADDING_DP * density).toInt()
    }

    private fun expandSnippet(snippet: Snippet) {
        overlayView?.let { HapticUtil.performUIHaptic(it) }
        val targetNode = activeNode?.takeIf {
            try { it.refresh() } catch (_: Exception) { false }
        } ?: service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (targetNode != null) {
            val fullText = targetNode.text?.toString().orEmpty()
            val cursor = targetNode.textSelectionEnd
            val textBeforeCursor = if (cursor in 0..fullText.length) fullText.substring(0, cursor) else fullText
            val candidateWord = textBeforeCursor.takeLastWhile { !it.isWhitespace() }
            val targetWord = if (candidateWord.isNotBlank() && snippet.keyword.startsWith(candidateWord, ignoreCase = true)) {
                candidateWord
            } else {
                lastTypedWord.ifBlank { snippet.keyword }
            }

            val expanded = SnippetExpander.expand(snippet.content, service)
            replaceKeywordInNode(
                node = targetNode,
                targetWord = targetWord,
                replacement = "$expanded ",
            )
        }
        dismissAllSuggestions()
    }

    private fun showSuggestion(snippet: Snippet) {
        val mode = settingsRepository.getSnippetsSuggestionDisplayMode()
        val onExpand = { expandSnippet(snippet) }

        when (mode) {
            SettingsRepository.SNIPPETS_DISPLAY_NONE -> {
                hideFloatingPill()
                islandCoordinator?.hideSnippet()
                duoOverlayHandler?.hideSnippetSuggestion()
            }
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
        activeNode?.recycle()
        activeNode = null
        lastTypedWord = ""
    }

    private fun showFloatingPill(snippet: Snippet, onExpand: () -> Unit) {
        mainHandler.post {
            currentMatchedSnippet.value = snippet
            mainHandler.removeCallbacks(dismissRunnable)
            mainHandler.postDelayed(dismissRunnable, AUTO_DISMISS_TIMEOUT_MS)

            if (overlayView != null) {
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
        activeNode?.recycle()
        activeNode = null
    }
}
