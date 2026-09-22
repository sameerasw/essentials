package com.sameerasw.essentials.island.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.model.IslandItem

@Composable
fun ExpandedHost(
    item: IslandItem,
    spec: IslandLayoutSpec,
    onCollapse: () -> Unit,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
) {
    val content = item.expanded ?: return
    val scope = remember(item, spec, onCollapse, onDismiss, onOpen) {
        object : IslandExpandedScope {
            override val spec: IslandLayoutSpec = spec
            override val accent: Color = item.accent ?: Color.White
            override fun collapse() = onCollapse()
            override fun dismiss() = onDismiss()
            override fun openApp() = onOpen()
        }
    }
    Box(Modifier.width(spec.expandedWidth)) { content.content(scope) }
}
