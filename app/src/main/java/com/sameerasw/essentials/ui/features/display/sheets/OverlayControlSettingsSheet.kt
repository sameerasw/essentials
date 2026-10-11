/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: OverlayControlSettingsSheet.kt
 * Description: Config sheet for the "Overlays" DIY automation action.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayControlSettingsSheet(
    initialAction: Action.OverlayControl,
    onDismiss: () -> Unit,
    onSave: (Action.OverlayControl) -> Unit,
) {
    val view = LocalView.current
    var draft by remember { mutableStateOf(initialAction) }
    var tab by remember { mutableIntStateOf(0) }

    val tabs =
        listOf(
            R.string.duo_title to draft.duo,
            R.string.island_title to draft.island,
            R.string.feat_status_glance_title to draft.statusGlance,
        )
    val modes =
        listOf(
            Action.OverlayMode.SKIP to R.string.overlay_mode_skip,
            Action.OverlayMode.OFF to R.string.overlay_mode_off,
            Action.OverlayMode.ON to R.string.overlay_mode_on,
            Action.OverlayMode.TOGGLE to R.string.overlay_mode_toggle,
        )

    fun select(mode: Action.OverlayMode) {
        draft =
            when (tab) {
                0 -> draft.copy(duo = mode)
                1 -> draft.copy(island = mode)
                else -> draft.copy(statusGlance = mode)
            }
    }

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.diy_action_overlay_control),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            PrimaryTabRow(
                selectedTabIndex = tab,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
            ) {
                tabs.forEachIndexed { index, (titleRes, mode) ->
                    Tab(
                        selected = tab == index,
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            tab = index
                        },
                        text = {
                            Text(
                                text = stringResource(titleRes) + if (mode != Action.OverlayMode.SKIP) " •" else "",
                            )
                        },
                    )
                }
            }

            RoundedCardContainer(spacing = 2.dp) {
                val current = tabs[tab].second
                modes.forEach { (mode, labelRes) ->
                    val isSelected = current == mode
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    HapticUtil.performUIHaptic(view)
                                    select(mode)
                                }.background(MaterialTheme.colorScheme.surfaceBright)
                                .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                select(mode)
                            },
                        )
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                        painter = painterResource(id = R.drawable.rounded_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onSave(draft)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_check_24),
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
