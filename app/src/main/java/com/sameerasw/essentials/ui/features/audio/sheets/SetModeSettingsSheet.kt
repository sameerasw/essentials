/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Audio
 * File: SetModeSettingsSheet.kt
 * Description: Config sheet for the "Set Mode" DIY automation action. Pick one of the system Modes
 *              and whether to turn it on or off.
 */

package com.sameerasw.essentials.ui.features.audio.sheets

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.controller.ZenModeController
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetModeSettingsSheet(
    initialAction: Action.SetMode,
    onDismiss: () -> Unit,
    onSave: (Action.SetMode) -> Unit,
) {
    val view = LocalView.current

    var active by remember { mutableStateOf(initialAction.active) }
    var selectedId by remember { mutableStateOf(initialAction.modeId) }
    var modes by remember { mutableStateOf<List<ZenModeController.Mode>?>(null) }

    LaunchedEffect(Unit) {
        modes =
            withContext(Dispatchers.IO) {
                runCatching { ZenModeController.getModes() }
                    .onFailure { Log.e("SetModeSettingsSheet", "Failed to load modes", it) }
                    .getOrDefault(emptyList())
            }
    }

    EssentialsBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.diy_action_set_mode),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = stringResource(R.string.diy_set_mode_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val turnOn = stringResource(R.string.diy_set_mode_turn_on)
            val turnOff = stringResource(R.string.diy_set_mode_turn_off)
            RoundedCardContainer(containerColor = MaterialTheme.colorScheme.surfaceBright) {
                SegmentedPicker(
                    items = listOf(true, false),
                    selectedItem = active,
                    onItemSelected = { active = it },
                    labelProvider = { if (it) turnOn else turnOff },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val loadedModes = modes
            if (loadedModes != null && loadedModes.isEmpty()) {
                Text(
                    text = stringResource(R.string.diy_set_mode_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (loadedModes != null) {
                RoundedCardContainer(
                    spacing = 2.dp,
                    modifier =
                        Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                ) {
                    loadedModes.forEach { mode ->
                        val isSelected = selectedId == mode.id
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        HapticUtil.performUIHaptic(view)
                                        selectedId = mode.id
                                    }.background(MaterialTheme.colorScheme.surfaceBright)
                                    .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    HapticUtil.performUIHaptic(view)
                                    selectedId = mode.id
                                },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (!mode.isEnabled) {
                                    Text(
                                        text = stringResource(R.string.diy_set_mode_disabled),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        painter = painterResource(R.drawable.rounded_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.action_cancel))
                }

                val selectedMode = modes?.firstOrNull { it.id == selectedId }
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        selectedMode?.let {
                            onSave(initialAction.copy(modeId = it.id, modeName = it.name, active = active))
                        }
                    },
                    enabled = selectedMode != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
