/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Automation
 * File: SendNotificationSettingsSheet.kt
 * Description: UI bottom sheet for configuring the Send Notification automation action.
 */

package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendNotificationSettingsSheet(
    initialAction: Action.SendNotification,
    onDismiss: () -> Unit,
    onSave: (Action.SendNotification) -> Unit,
) {
    val view = LocalView.current
    var title by remember { mutableStateOf(initialAction.notificationTitle) }
    var body by remember { mutableStateOf(initialAction.body) }
    var label by remember { mutableStateOf(initialAction.label) }

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.diy_action_send_notification),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                NotificationField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.diy_send_notification_title),
                    singleLine = true,
                )
                NotificationField(
                    value = body,
                    onValueChange = { body = it },
                    label = stringResource(R.string.diy_send_notification_body),
                    minLines = 3,
                    maxLines = 6,
                )
                NotificationField(
                    value = label,
                    onValueChange = { label = it },
                    label = stringResource(R.string.diy_send_notification_label),
                    singleLine = true,
                )
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
                        onSave(
                            Action.SendNotification(
                                notificationTitle = title.trim(),
                                body = body.trim(),
                                label = label.trim(),
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank() || body.isNotBlank(),
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

@Composable
private fun NotificationField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                .padding(12.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = singleLine,
            minLines = minLines,
            maxLines = if (singleLine) 1 else maxLines,
            shape = MaterialTheme.shapes.medium,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
