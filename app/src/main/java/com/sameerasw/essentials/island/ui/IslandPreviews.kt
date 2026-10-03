package com.sameerasw.essentials.island.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.IslandStage
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.StackIcon
import androidx.compose.ui.unit.Dp
import com.sameerasw.essentials.island.state.CompactEntry
import com.sameerasw.essentials.island.state.CompactLayoutEngine
import com.sameerasw.essentials.island.state.IslandUiState
import com.sameerasw.essentials.island.ui.components.EqualizerBars
import com.sameerasw.essentials.island.ui.components.IslandIcon

@Composable
private fun PreviewContainer(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color.DarkGray),
            color = Color.DarkGray,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                content()
            }
        }
    }
}

private fun createSampleMusicItem(): IslandItem {
    return IslandItem(
        key = "media_preview",
        priority = IslandPriority.MEDIA,
        placement = CompactPlacement.Dynamic,
        compact = listOf(
            CompactCell("icon") {
                IslandIcon(res = R.drawable.rounded_music_note_24, size = 16.dp)
            },
            CompactCell("eq") {
                EqualizerBars(playing = true, color = Color.Green, size = 16.dp)
            },
        ),
        line = LineContent(
            icon = { IslandIcon(res = R.drawable.rounded_music_note_24, size = 16.dp) },
            start = "Playing",
            end = "Dynamic Island Song",
        ),
        expanded = ExpandedContent {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Expanded Media Player Preview", color = Color.White)
            }
        },
        accent = Color.Green,
    )
}

private fun createSampleNotificationItem(): IslandItem {
    return IslandItem(
        key = "notification_preview",
        priority = IslandPriority.NOTIFICATION,
        placement = CompactPlacement.Dynamic,
        compact = listOf(
            CompactCell("notif.icon") {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "W",
                        color = Color.White,
                        style = IslandTextStyles.compact.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    )
                }
            },
        ),
        line = LineContent(
            icon = {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "W",
                        color = Color.White,
                        style = IslandTextStyles.compact.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    )
                }
            },
            start = "WhatsApp",
            end = "New message from friend",
        ),
        expanded = ExpandedContent {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Expanded Notification Preview", color = Color.White)
            }
        },
        accent = Color(0xFF25D366),
    )
}

@Preview(name = "1. Hidden State", showBackground = true, backgroundColor = 0xFF222222)
@Composable
private fun IslandHiddenPreview() {
    PreviewContainer {
        IslandRoot(
            state = IslandUiState(
                stage = IslandStage.Hidden,
                items = emptyMap(),
                arrangement = CompactLayoutEngine.arrange(emptyList()),
                focusedKey = null,
            ),
            spec = IslandLayoutSpec(),
            actions = object : IslandActions {
                override fun onTap(itemKey: String?) = false
                override fun onLongPress(itemKey: String?) {}
                override fun onCollapse() {}
                override fun onDismiss() = false
                override fun onOpenFocused() {}
                override fun onInteraction() {}
                override fun onTextInputChanged(active: Boolean) {}
                override fun onAdvance() = false
            },
            onTargetBoundsChanged = {},
        )
    }
}

@Preview(name = "2. Compact State", showBackground = true, backgroundColor = 0xFF222222)
@Composable
private fun IslandCompactPreview() {
    val musicItem = remember { createSampleMusicItem() }
    val arrangement = remember(musicItem) {
        CompactLayoutEngine.arrange(
            listOf(
                CompactEntry(
                    musicItem.key,
                    musicItem.effectivePriority,
                    musicItem.placement == CompactPlacement.Pinned,
                    musicItem.compact.filterNot { it.soloOnly }.map { it.key },
                    musicItem.compact.filter { it.soloOnly }.map { it.key },
                ),
            ),
        )
    }
    PreviewContainer {
        IslandRoot(
            state = IslandUiState(
                stage = IslandStage.Compact,
                items = mapOf(musicItem.key to musicItem),
                arrangement = arrangement,
                focusedKey = null,
            ),
            spec = IslandLayoutSpec(),
            actions = object : IslandActions {
                override fun onTap(itemKey: String?) = false
                override fun onLongPress(itemKey: String?) {}
                override fun onCollapse() {}
                override fun onDismiss() = false
                override fun onOpenFocused() {}
                override fun onInteraction() {}
                override fun onTextInputChanged(active: Boolean) {}
                override fun onAdvance() = false
            },
            onTargetBoundsChanged = {},
        )
    }
}

@Preview(name = "5. Compact Notification State (WhatsApp W)", showBackground = true, backgroundColor = 0xFF222222)
@Composable
private fun IslandCompactNotificationPreview() {
    val notifItem = remember { createSampleNotificationItem() }
    val arrangement = remember(notifItem) {
        CompactLayoutEngine.arrange(
            listOf(
                CompactEntry(
                    notifItem.key,
                    notifItem.effectivePriority,
                    notifItem.placement == CompactPlacement.Pinned,
                    notifItem.compact.filterNot { it.soloOnly }.map { it.key },
                    notifItem.compact.filter { it.soloOnly }.map { it.key },
                ),
            ),
        )
    }
    PreviewContainer {
        IslandRoot(
            state = IslandUiState(
                stage = IslandStage.Compact,
                items = mapOf(notifItem.key to notifItem),
                arrangement = arrangement,
                focusedKey = null,
            ),
            spec = IslandLayoutSpec(),
            actions = object : IslandActions {
                override fun onTap(itemKey: String?) = false
                override fun onLongPress(itemKey: String?) {}
                override fun onCollapse() {}
                override fun onDismiss() = false
                override fun onOpenFocused() {}
                override fun onInteraction() {}
                override fun onTextInputChanged(active: Boolean) {}
                override fun onAdvance() = false
            },
            onTargetBoundsChanged = {},
        )
    }
}

@Preview(name = "3. Line State", showBackground = true, backgroundColor = 0xFF222222)
@Composable
private fun IslandLinePreview() {
    val musicItem = remember { createSampleMusicItem() }
    val arrangement = remember(musicItem) {
        CompactLayoutEngine.arrange(
            listOf(
                CompactEntry(
                    musicItem.key,
                    musicItem.effectivePriority,
                    musicItem.placement == CompactPlacement.Pinned,
                    musicItem.compact.filterNot { it.soloOnly }.map { it.key },
                    musicItem.compact.filter { it.soloOnly }.map { it.key },
                ),
            ),
        )
    }
    PreviewContainer {
        IslandRoot(
            state = IslandUiState(
                stage = IslandStage.Line,
                items = mapOf(musicItem.key to musicItem),
                arrangement = arrangement,
                focusedKey = musicItem.key,
            ),
            spec = IslandLayoutSpec(),
            actions = object : IslandActions {
                override fun onTap(itemKey: String?) = false
                override fun onLongPress(itemKey: String?) {}
                override fun onCollapse() {}
                override fun onDismiss() = false
                override fun onOpenFocused() {}
                override fun onInteraction() {}
                override fun onTextInputChanged(active: Boolean) {}
                override fun onAdvance() = false
            },
            onTargetBoundsChanged = {},
        )
    }
}

@Preview(name = "4. Expanded State", showBackground = true, backgroundColor = 0xFF222222)
@Composable
private fun IslandExpandedPreview() {
    val musicItem = remember { createSampleMusicItem() }
    val arrangement = remember(musicItem) {
        CompactLayoutEngine.arrange(
            listOf(
                CompactEntry(
                    musicItem.key,
                    musicItem.effectivePriority,
                    musicItem.placement == CompactPlacement.Pinned,
                    musicItem.compact.filterNot { it.soloOnly }.map { it.key },
                    musicItem.compact.filter { it.soloOnly }.map { it.key },
                ),
            ),
        )
    }
    PreviewContainer {
        IslandRoot(
            state = IslandUiState(
                stage = IslandStage.Expanded,
                items = mapOf(musicItem.key to musicItem),
                arrangement = arrangement,
                focusedKey = musicItem.key,
            ),
            spec = IslandLayoutSpec(),
            actions = object : IslandActions {
                override fun onTap(itemKey: String?) = false
                override fun onLongPress(itemKey: String?) {}
                override fun onCollapse() {}
                override fun onDismiss() = false
                override fun onOpenFocused() {}
                override fun onInteraction() {}
                override fun onTextInputChanged(active: Boolean) {}
                override fun onAdvance() = false
            },
            onTargetBoundsChanged = {},
        )
    }
}

private fun createSampleMultipleNotificationItem(): IslandItem {
    val notifItem = createSampleNotificationItem()
    val stackIcons = listOf(
        StackIcon(
            key = "notif_1",
            current = true,
            onSelect = {},
            content = { size ->
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("W", color = Color.White, style = IslandTextStyles.compact.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                }
            },
        ),
        StackIcon(
            key = "notif_2",
            current = false,
            onSelect = {},
            content = { size ->
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(Color(0xFF4285F4)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("G", color = Color.White, style = IslandTextStyles.compact.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                }
            },
        ),
    )
    return IslandItem(
        key = notifItem.key,
        priority = notifItem.priority,
        placement = notifItem.placement,
        compact = notifItem.compact,
        line = notifItem.line,
        expanded = notifItem.expanded,
        accent = notifItem.accent,
        dismissible = notifItem.dismissible,
        onDismiss = notifItem.onDismiss,
        onOpen = notifItem.onOpen,
        interactions = notifItem.interactions,
        queue = notifItem.queue,
        sourcePackage = notifItem.sourcePackage,
        stack = stackIcons,
    )
}

@Preview(name = "6. Compact State (Multiple Notifications with Stack Bubble)", showBackground = true, backgroundColor = 0xFF222222)
@Composable
private fun IslandCompactMultipleNotificationsPreview() {
    val notifItem = remember { createSampleMultipleNotificationItem() }
    val arrangement = remember(notifItem) {
        CompactLayoutEngine.arrange(
            listOf(
                CompactEntry(
                    notifItem.key,
                    notifItem.effectivePriority,
                    notifItem.placement == CompactPlacement.Pinned,
                    notifItem.compact.filterNot { it.soloOnly }.map { it.key },
                    notifItem.compact.filter { it.soloOnly }.map { it.key },
                ),
            ),
        )
    }
    PreviewContainer {
        IslandRoot(
            state = IslandUiState(
                stage = IslandStage.Compact,
                items = mapOf(notifItem.key to notifItem),
                arrangement = arrangement,
                focusedKey = null,
            ),
            spec = IslandLayoutSpec(),
            actions = object : IslandActions {
                override fun onTap(itemKey: String?) = false
                override fun onLongPress(itemKey: String?) {}
                override fun onCollapse() {}
                override fun onDismiss() = false
                override fun onOpenFocused() {}
                override fun onInteraction() {}
                override fun onTextInputChanged(active: Boolean) {}
                override fun onAdvance() = false
            },
            onTargetBoundsChanged = {},
        )
    }
}

