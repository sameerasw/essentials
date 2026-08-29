/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Core Components
 * File: PreReleaseConfirmationSheet.kt
 * Description: Reusable confirmation bottom sheet for enabling or disabling pre-releases.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreReleaseConfirmationSheet(
    isEnabling: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmRestart: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text =
                    stringResource(
                        if (isEnabling) {
                            R.string.pre_release_enable_sheet_title
                        } else {
                            R.string.pre_release_disable_sheet_title
                        },
                    ),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text =
                    stringResource(
                        if (isEnabling) {
                            R.string.pre_release_enable_sheet_desc
                        } else {
                            R.string.pre_release_disable_sheet_desc
                        },
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.pre_release_restart_notice),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_abort))
                }

                Button(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        onConfirmRestart()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_restart_app))
                }
            }
        }
    }
}
