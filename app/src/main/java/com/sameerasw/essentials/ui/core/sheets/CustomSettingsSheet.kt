/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - System
 * File: CustomSettingsSheet.kt
 * Description: Bottom sheet allowing recording, editing, and saving of custom Android settings entries.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.SettingsRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class RecordingState {
    IDLE,
    RECORDING,
}

private const val MAX_ENTRIES_LIMIT = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSettingsSheet(
    initialAction: Action.CustomSettings,
    onDismiss: () -> Unit,
    onSave: (Action.CustomSettings) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var recordingState by remember { mutableStateOf(RecordingState.IDLE) }
    var beforeSnapshot by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var showHowItWorksExpanded by remember { mutableStateOf(false) }

    val entries =
        remember {
            mutableStateListOf<Action.SettingsEntry>().apply {
                addAll(initialAction.entries)
            }
        }

    LaunchedEffect(recordingState) {
        if (recordingState == RecordingState.RECORDING) {
            elapsedSeconds = 0
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    EssentialsBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Title Header
            TranslatableText(
                stringResId = R.string.diy_action_custom_settings,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(24.dp))
                        .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                        ),
                    shape = RoundedCornerShape(24.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable {
                                HapticUtil.performUIHaptic(view)
                                showHowItWorksExpanded = !showHowItWorksExpanded
                            },
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val infoTitle = stringResource(R.string.diy_custom_settings_how_it_works)
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .background(
                                            color = ColorUtil.getPastelColorFor(infoTitle),
                                            shape = CircleShape,
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_info_24),
                                    contentDescription = null,
                                    tint = ColorUtil.getVibrantColorFor(infoTitle),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            TranslatableText(
                                stringResId = R.string.diy_custom_settings_how_it_works,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                painter =
                                    painterResource(
                                        if (showHowItWorksExpanded) {
                                            R.drawable.rounded_keyboard_arrow_up_24
                                        } else {
                                            R.drawable.rounded_keyboard_arrow_down_24
                                        },
                                    ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        AnimatedVisibility(
                            visible = showHowItWorksExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                TranslatableText(
                                    stringResId = R.string.diy_custom_settings_how_to,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                            .padding(12.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_release_alert_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TranslatableText(
                                            stringResId = R.string.diy_custom_settings_warning_title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                        TranslatableText(
                                            stringResId = R.string.diy_custom_settings_warning_desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (recordingState == RecordingState.IDLE) {
                    Button(
                        onClick = {
                            HapticUtil.performHeavyHaptic(view)
                            scope.launch {
                                val snapshot =
                                    withContext(Dispatchers.IO) {
                                        SettingsRecorder.snapshot(context)
                                    }
                                beforeSnapshot = snapshot
                                recordingState = RecordingState.RECORDING
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_play_arrow_24),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        TranslatableText(
                            stringResId = R.string.diy_custom_settings_start_recording,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                } else {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1f,
                        animationSpec =
                            infiniteRepeatable(
                                animation = tween(600),
                                repeatMode = RepeatMode.Reverse,
                            ),
                        label = "pulseAlpha",
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = {
                                HapticUtil.performHeavyHaptic(view)
                                scope.launch {
                                    val afterSnapshot =
                                        withContext(Dispatchers.IO) {
                                            SettingsRecorder.snapshot(context)
                                        }
                                    val newChanges =
                                        withContext(Dispatchers.IO) {
                                            SettingsRecorder.diff(beforeSnapshot, afterSnapshot)
                                        }

                                    for (newEntry in newChanges) {
                                        if (entries.size >= MAX_ENTRIES_LIMIT) {
                                            val existingIndex =
                                                entries.indexOfFirst {
                                                    it.table == newEntry.table && it.key == newEntry.key
                                                }
                                            if (existingIndex != -1) {
                                                entries[existingIndex] = newEntry
                                            }
                                            continue
                                        }

                                        val existingIndex =
                                            entries.indexOfFirst {
                                                it.table == newEntry.table && it.key == newEntry.key
                                            }
                                        if (existingIndex != -1) {
                                            entries[existingIndex] = newEntry
                                        } else {
                                            entries.add(newEntry)
                                        }
                                    }
                                    recordingState = RecordingState.IDLE
                                }
                            },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(56.dp),
                            shape = ButtonGroupDefaults.connectedLeadingButtonShapes().shape,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.rounded_stop_circle_24),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TranslatableText(
                                stringResId = R.string.diy_custom_settings_stop_recording,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                            )
                        }

                        Button(
                            onClick = {},
                            modifier =
                                Modifier
                                    .width(96.dp)
                                    .height(56.dp)
                                    .alpha(pulseAlpha),
                            shape = ButtonGroupDefaults.connectedTrailingButtonShapes().shape,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.error, CircleShape),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${elapsedSeconds}s",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                if (entries.isEmpty()) {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                            ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TranslatableText(
                            stringResId = R.string.diy_custom_settings_no_entries,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${stringResource(R.string.diy_action_custom_settings)} (${entries.size}/$MAX_ENTRIES_LIMIT)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    RoundedCardContainer(
                        spacing = 2.dp,
                        cornerRadius = 24.dp,
                    ) {
                        val count = entries.size
                        entries.forEachIndexed { index, entry ->
                            CustomSettingEntryRow(
                                entry = entry,
                                index = index,
                                count = count,
                                onValueChange = { newValue ->
                                    entries[index] = entry.copy(value = newValue)
                                },
                                onDelete = {
                                    HapticUtil.performLightHaptic(view)
                                    entries.removeAt(index)
                                },
                            )
                        }
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    TranslatableText(R.string.action_cancel)
                }

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onSave(Action.CustomSettings(entries = entries.toList()))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = entries.isNotEmpty(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    TranslatableText(R.string.action_save)
                }
            }
        }
    }
}

@Composable
private fun CustomSettingEntryRow(
    entry: Action.SettingsEntry,
    index: Int,
    count: Int,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val tableTitle =
        when (entry.table) {
            Action.SettingsTable.SYSTEM -> stringResource(R.string.diy_custom_settings_table_system)
            Action.SettingsTable.SECURE -> stringResource(R.string.diy_custom_settings_table_secure)
            Action.SettingsTable.GLOBAL -> stringResource(R.string.diy_custom_settings_table_global)
        }

    val chipContainerColor =
        when (entry.table) {
            Action.SettingsTable.SYSTEM -> MaterialTheme.colorScheme.primaryContainer
            Action.SettingsTable.SECURE -> MaterialTheme.colorScheme.secondaryContainer
            Action.SettingsTable.GLOBAL -> MaterialTheme.colorScheme.tertiaryContainer
        }

    val chipContentColor =
        when (entry.table) {
            Action.SettingsTable.SYSTEM -> MaterialTheme.colorScheme.onPrimaryContainer
            Action.SettingsTable.SECURE -> MaterialTheme.colorScheme.onSecondaryContainer
            Action.SettingsTable.GLOBAL -> MaterialTheme.colorScheme.onTertiaryContainer
        }

    val shape =
        when {
            count == 1 -> RoundedCornerShape(24.dp)
            index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
            index == count - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
            else -> RoundedCornerShape(4.dp)
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SuggestionChip(
                onClick = {},
                label = { Text(tableTitle, style = MaterialTheme.typography.labelSmall) },
                colors =
                    SuggestionChipDefaults.suggestionChipColors(
                        containerColor = chipContainerColor,
                        labelColor = chipContentColor,
                    ),
                border = null,
                modifier = Modifier.height(26.dp),
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_delete_24),
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Text(
            text = entry.key,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        OutlinedTextField(
            value = entry.value,
            onValueChange = onValueChange,
            label = { TranslatableText(R.string.diy_custom_settings_value_label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
    }
}
