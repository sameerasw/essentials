/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Dynamic Island - Plugins
 * File: SnippetExpanded.kt
 * Description: Expanded card view for snippet suggestions in Dynamic Island.
 */

package com.sameerasw.essentials.island.plugins.snippets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ime.snippets.Snippet
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.ConnectedTextLabel
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.MarqueeText
import com.sameerasw.essentials.island.ui.components.accentGlow
import com.sameerasw.essentials.island.ui.components.cameraClearance

@Composable
fun SnippetExpanded(
    snippet: Snippet,
    scope: IslandExpandedScope,
    onInsert: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spec = scope.spec
    val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
    val accent = MaterialTheme.colorScheme.primary

    Box(propagateMinConstraints = true) {
        Box(Modifier.matchParentSize().accentGlow(accent, true, scope.cameraClearance))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spec.expandedOutset),
        ) {
            Spacer(Modifier.height(spec.expandedTopPadding))
            scope.CameraRow(
                horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                start = {
                    Box(Modifier.padding(horizontal = 4.dp)) {
                        IslandIcon(
                            res = R.drawable.rounded_text_snippet_24,
                            size = 20.dp,
                            tint = accent,
                        )
                    }
                    val displayTitle = snippet.title.ifBlank { snippet.keyword }
                    MarqueeText(
                        text = displayTitle,
                        style = IslandTextStyles.title,
                        modifier = Modifier.weight(1f),
                    )
                },
                end = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = snippet.keyword,
                            style = IslandTextStyles.compact.copy(
                                fontSize = 11.sp,
                                color = accent,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            // Body preview of snippet content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sidePadding)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = snippet.content,
                    style = IslandTextStyles.body.copy(
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                    ),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(14.dp))

            // Action row: Insert, Copy, Dismiss
            ConnectedButtonRow(
                height = 36.dp,
                modifier = Modifier.padding(horizontal = sidePadding),
                container = Color.White.copy(alpha = 0.2f),
                items = listOf(
                    ConnectedItem(onInsert) {
                        ConnectedTextLabel(stringResource(R.string.snippets_action_insert))
                    },
                    ConnectedItem(onCopy) {
                        ConnectedTextLabel(stringResource(R.string.snippets_action_copy))
                    },
                    ConnectedItem(onDismiss) {
                        ConnectedTextLabel(stringResource(R.string.action_dismiss))
                    },
                ),
            )

            Spacer(Modifier.height(spec.expandedBottomPadding))
        }
    }
}
