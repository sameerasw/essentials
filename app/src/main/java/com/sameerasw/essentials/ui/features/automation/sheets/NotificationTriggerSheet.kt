package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTriggerSheet(
    initialTrigger: Trigger.NotificationReceived?,
    onDismiss: () -> Unit,
    onSave: (Trigger.NotificationReceived) -> Unit,
) {
    val view = LocalView.current
    var packageName by remember { mutableStateOf(initialTrigger?.packageName.orEmpty()) }
    var appName by remember { mutableStateOf(initialTrigger?.appName.orEmpty()) }
    var textContains by remember { mutableStateOf(initialTrigger?.textContains.orEmpty()) }
    var showAppPicker by remember { mutableStateOf(false) }
    val canSave = packageName.isNotBlank() || textContains.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.diy_trigger_notification_received),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.diy_notification_trigger_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RoundedCardContainer {
                IconToggleItem(
                    iconRes = R.drawable.rounded_apps_24,
                    title = stringResource(R.string.diy_notification_trigger_app),
                    description = appName.ifBlank { stringResource(R.string.diy_notification_trigger_any_app) },
                    showToggle = false,
                    onClick = { showAppPicker = true },
                    trailingContent =
                        if (packageName.isNotBlank()) {
                            {
                                IconButton(
                                    onClick = {
                                        HapticUtil.performUIHaptic(view)
                                        packageName = ""
                                        appName = ""
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_close_24),
                                        contentDescription = stringResource(R.string.diy_notification_trigger_any_app),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                )
            }

            OutlinedTextField(
                value = textContains,
                onValueChange = { textContains = it },
                label = { Text(stringResource(R.string.diy_notification_trigger_text)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                        onSave(Trigger.NotificationReceived(packageName, appName, textContains.trim()))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canSave,
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

    if (showAppPicker) {
        SingleAppSelectionSheet(
            onDismissRequest = { showAppPicker = false },
            onAppSelected = { app ->
                packageName = app.packageName
                appName = app.appName
                showAppPicker = false
            },
        )
    }
}
