/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Freeze
 * File: FreezeTagSettingsSheet.kt
 * Description: UI component and settings composable for Freeze feature domain.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.AppTag
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreezeTagSettingsSheet(
    initialAction: Action.FreezeTag,
    availableTags: List<AppTag>,
    onDismiss: () -> Unit,
    onSave: (Action.FreezeTag) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current

    val modeOptions = listOf("Freeze", "Unfreeze")
    var selectedMode by remember { mutableStateOf(initialAction.mode) }
    var selectedTagIds by remember { mutableStateOf(initialAction.tagIds.toSet()) }

    EssentialsBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TranslatableText(
                stringResId = R.string.diy_action_freeze_tag,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            RoundedCardContainer {
                SegmentedPicker(
                    items = modeOptions,
                    selectedItem = selectedMode,
                    onItemSelected = { mode ->
                        HapticUtil.performUIHaptic(view)
                        selectedMode = mode
                    },
                    labelProvider = { mode ->
                        if (mode == "Freeze") {
                            context.getString(R.string.action_freeze)
                        } else {
                            context.getString(R.string.action_unfreeze)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            TranslatableText(
                stringResId = R.string.freeze_tags_section_title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (availableTags.isEmpty()) {
                TranslatableText(
                    stringResId = R.string.freeze_tags_empty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                RoundedCardContainer(spacing = 2.dp) {
                    availableTags.forEach { tag ->
                        val isChecked = selectedTagIds.contains(tag.id)
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
                                val richColor = remember(color) { ColorUtil.toRichColor(color) }
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
                                        HapticUtil.performUIHaptic(view)
                                        selectedTagIds =
                                            if (checked) {
                                                selectedTagIds + tag.id
                                            } else {
                                                selectedTagIds - tag.id
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
                                        HapticUtil.performUIHaptic(view)
                                        selectedTagIds =
                                            if (isChecked) {
                                                selectedTagIds - tag.id
                                            } else {
                                                selectedTagIds + tag.id
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

            Spacer(modifier = Modifier.padding(top = 8.dp))

            Button(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    onSave(
                        Action.FreezeTag(
                            mode = selectedMode,
                            tagIds = selectedTagIds.toList(),
                        ),
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                TranslatableText(stringResId = R.string.action_save)
            }
        }
    }
}
