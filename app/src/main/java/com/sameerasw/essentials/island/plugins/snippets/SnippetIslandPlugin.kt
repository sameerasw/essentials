/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Dynamic Island - Plugins
 * File: SnippetIslandPlugin.kt
 * Description: Dynamic Island plugin showing snippet suggestions and peek banners for external keyboards.
 */

package com.sameerasw.essentials.island.plugins.snippets

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ime.snippets.Snippet
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.InteractionOverrides
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.RollingText

class SnippetIslandPlugin : BaseIslandPlugin() {
    companion object {
        const val ITEM_KEY = "snippet_suggestion"
        private const val PEEK_DURATION_MS = 4500L
    }

    override val id = "snippets"

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val autoDismissRunnable = Runnable { hideSnippet() }

    private var activeSnippet: Snippet? = null
    private var onExpandAction: (() -> Unit)? = null

    fun showSnippet(snippet: Snippet, onExpand: () -> Unit) {
        activeSnippet = snippet
        onExpandAction = onExpand
        render()
        ctx?.request(PluginRequest.Peek(ITEM_KEY, PEEK_DURATION_MS))
        mainHandler.removeCallbacks(autoDismissRunnable)
        mainHandler.postDelayed(autoDismissRunnable, PEEK_DURATION_MS)
    }

    fun hideSnippet() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        if (activeSnippet == null) return
        activeSnippet = null
        onExpandAction = null
        publish(emptyList())
        ctx?.request(PluginRequest.Collapse(ITEM_KEY))
    }

    override fun onStop() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        activeSnippet = null
        onExpandAction = null
    }

    private fun render() {
        val snippet = activeSnippet
        if (snippet == null) {
            publish(emptyList())
            return
        }

        val item = IslandItem(
            key = ITEM_KEY,
            priority = IslandPriority.NOTIFICATION - 1,
            placement = CompactPlacement.Dynamic,
            compact = listOf(
                CompactCell("snippet.icon") {
                    IslandIcon(
                        res = R.drawable.rounded_text_snippet_24,
                        size = 18.dp,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                CompactCell("snippet.keyword") {
                    RollingText(snippet.keyword)
                },
            ),
            line = LineContent(
                icon = {
                    IslandIcon(
                        res = R.drawable.rounded_text_snippet_24,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                start = snippet.keyword,
                end = snippet.title.ifEmpty { snippet.content.take(24) },
            ),
            interactions = InteractionOverrides(
                onTap = {
                    onExpandAction?.invoke()
                    hideSnippet()
                    true
                },
            ),
            dismissible = true,
            onDismiss = { hideSnippet() },
        )
        publish(listOf(item))
    }
}
