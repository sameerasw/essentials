package com.sameerasw.essentials.island.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * A shape drawn below an item's expanded view, bonded to it like the queue pill. Content draws its own
 * black background; the slot clips it and owns position, motion and the liquid neck.
 *
 * @param content Gets the slot width in px and whether the attachment is settled (not animating or being
 *  dragged), which is when a live surface may be shown.
 */
class IslandAttachment(
    val key: String,
    val anchor: String,
    val matchTitle: String? = null,
    val beat: IslandBeat? = null,
    val content: @Composable (widthPx: Int, settled: Boolean) -> Unit,
) {
    fun matches(title: String?): Boolean {
        val wanted = matchTitle?.normalized()?.takeIf { it.isNotEmpty() } ?: return true
        val actual = title?.normalized()?.takeIf { it.isNotEmpty() } ?: return false
        return actual.contains(wanted) || wanted.contains(actual)
    }

    private fun String.normalized() = trim().lowercase()

    companion object {
        val CORNER = 28.dp
    }
}

data class IslandBeat(val periodMs: Float, val offsetMs: Float)

object IslandAttachments {
    private val byOwner = MutableStateFlow<Map<String, List<IslandAttachment>>>(emptyMap())

    val all: StateFlow<Map<String, List<IslandAttachment>>> = byOwner

    fun set(owner: String, attachments: List<IslandAttachment>) {
        byOwner.update { if (attachments.isEmpty()) it - owner else it + (owner to attachments) }
    }

    fun resolve(all: Map<String, List<IslandAttachment>>, anchor: String, title: String?): IslandAttachment? =
        all.entries
            .sortedBy { it.key }
            .firstNotNullOfOrNull { (_, list) -> list.firstOrNull { it.anchor == anchor && it.matches(title) } }
}
