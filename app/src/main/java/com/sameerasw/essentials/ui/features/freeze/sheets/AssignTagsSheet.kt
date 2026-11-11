/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Freeze
 * File: AssignTagsSheet.kt
 * Description: UI component and settings composable for Freeze feature domain.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.AppTag
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignTagsSheet(
    appName: String,
    availableTags: List<AppTag>,
    assignedTagIds: List<String>,
    onDismissRequest: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current
    val context = LocalContext.current

    var selectedIds by remember { mutableStateOf(assignedTagIds.toSet()) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.freeze_tag_assign_title) + " ($appName)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (availableTags.isEmpty()) {
                TranslatableText(
                    stringResId = R.string.freeze_tags_empty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                RoundedCardContainer(spacing = 2.dp) {
                    availableTags.forEach { tag ->
                        val isChecked = selectedIds.contains(tag.id)
                        val color =
                            try {
                                Color(android.graphics.Color.parseColor(tag.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                        val iconResId =
                            context.resources.getIdentifier(
                                tag.iconName,
                                "drawable",
                                context.packageName,
                            )

                        androidx.compose.material3.ListItem(
                            leadingContent = {
                                val richColor =
                                    remember(color) {
                                        com.sameerasw.essentials.utils.ColorUtil
                                            .toRichColor(color)
                                    }
                                Box(
                                    modifier =
                                        Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(richColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (iconResId != 0) iconResId else R.drawable.rounded_interests_24,
                                            ),
                                        contentDescription = null,
                                        tint = richColor,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        selectedIds =
                                            if (checked) {
                                                selectedIds + tag.id
                                            } else {
                                                selectedIds - tag.id
                                            }
                                    },
                                )
                            },
                            colors =
                                ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                                ),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .clickable {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        selectedIds =
                                            if (isChecked) {
                                                selectedIds - tag.id
                                            } else {
                                                selectedIds + tag.id
                                            }
                                    },
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onDismissRequest()
                    },
                ) {
                    TranslatableText(R.string.action_cancel)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onSave(selectedIds.toList())
                        onDismissRequest()
                    },
                ) {
                    TranslatableText(R.string.action_save)
                }
            }
        }
    }
}
