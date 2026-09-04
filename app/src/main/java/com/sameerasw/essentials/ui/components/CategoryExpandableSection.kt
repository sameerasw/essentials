/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core UI Components
 * File: CategoryExpandableSection.kt
 * Description: Material 3 Expressive expandable section container with header, item count badge, chevron animation, and haptic feedback.
 */

package com.sameerasw.essentials.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun CategoryExpandableSection(
    modifier: Modifier = Modifier,
    title: String,
    isSettingsSection: Boolean = false,
    itemCount: Int? = null,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val view = LocalView.current
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "ChevronRotation",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        HapticUtil.performUIHaptic(view)
                        onToggleExpand()
                    }
                    .padding(if (isSettingsSection) 16.dp else 12.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (!isSettingsSection) FontWeight.Bold else null,
                    color = if (isSettingsSection) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )

                if (itemCount != null) {
                    Box(
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = itemCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Icon(
                painter = painterResource(id = R.drawable.rounded_keyboard_arrow_down_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .size(24.dp)
                        .rotate(rotationAngle),
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = Modifier.clip(RoundedCornerShape(24.dp))
        ) {
            RoundedCardContainer(modifier = Modifier.padding(top = 8.dp)) {
                content()
            }
        }
    }
}
