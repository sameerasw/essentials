/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Network
 * File: SimNamesBottomSheet.kt
 * Description: Bottom sheet allowing users to view, customize, and reset carrier names per active SIM card.
 */

package com.sameerasw.essentials.ui.features.network.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.SimCarrierInfo
import com.sameerasw.essentials.utils.SimCarrierUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimNamesBottomSheet(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsRepository = remember { SettingsRepository(context) }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var simList by remember { mutableStateOf<List<SimCarrierInfo>>(emptyList()) }
    val carrierNameInputs = remember { mutableStateMapOf<Int, String>() }
    var isApplyAfterRebootEnabled by remember {
        mutableStateOf(settingsRepository.isSimNamesApplyOnBootEnabled())
    }

    LaunchedEffect(Unit) {
        val sims =
            withContext(Dispatchers.IO) {
                SimCarrierUtil.getSimCarrierInfoList(context)
            }
        simList = sims
        sims.forEach { sim ->
            carrierNameInputs[sim.subId] = sim.currentCarrierName
        }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.feat_sim_names_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (simList.isEmpty()) {
                Text(
                    text = stringResource(R.string.sim_names_no_sim),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp),
                )
            } else {
                RoundedCardContainer {
                    val count = simList.size
                    simList.forEachIndexed { index, sim ->
                        val currentText = carrierNameInputs[sim.subId] ?: ""
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(MaterialTheme.colorScheme.surfaceBright)
                                    .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text =
                                    stringResource(
                                        R.string.sim_names_slot_format,
                                        sim.simSlotIndex + 1,
                                        sim.displayName.ifBlank { sim.defaultCarrierName },
                                    ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = currentText,
                                    onValueChange = { carrierNameInputs[sim.subId] = it },
                                    label = { Text(stringResource(R.string.sim_names_carrier_label)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                )

                                IconButton(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        carrierNameInputs[sim.subId] = sim.defaultCarrierName
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_refresh_24),
                                        contentDescription = stringResource(R.string.sim_names_reset_tooltip),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            RoundedCardContainer {
                IconToggleItem(
                    title = stringResource(R.string.label_apply_after_reboot),
                    isChecked = isApplyAfterRebootEnabled,
                    onCheckedChange = {
                        isApplyAfterRebootEnabled = it
                        settingsRepository.setSimNamesApplyOnBootEnabled(it)
                        HapticUtil.performUIHaptic(view)
                    },
                    iconRes = R.drawable.rounded_cycle_24,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onDismissRequest()
                    },
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    enabled = !isLoading && !isSaving && simList.isNotEmpty(),
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        isSaving = true
                        scope.launch {
                            val savedMap = mutableMapOf<Int, String>()
                            simList.forEach { sim ->
                                val newName = carrierNameInputs[sim.subId]?.trim()
                                if (newName.isNullOrBlank() || newName == sim.defaultCarrierName) {
                                    SimCarrierUtil.resetCarrierName(context, sim.subId)
                                } else {
                                    SimCarrierUtil.overrideCarrierName(context, sim.subId, newName)
                                    savedMap[sim.subId] = newName
                                }
                            }
                            SimCarrierUtil.saveCustomCarrierNames(context, savedMap)
                            isSaving = false
                            onDismissRequest()
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
