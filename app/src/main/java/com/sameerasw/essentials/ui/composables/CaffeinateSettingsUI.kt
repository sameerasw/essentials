package com.sameerasw.essentials.ui.composables

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.CaffeinateViewModel
import com.sameerasw.essentials.R

@Composable
fun CaffeinateSettingsUI(
    viewModel: CaffeinateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isActive = viewModel.isActive.value

    var showPermissionSheet by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.postNotificationsGranted.value = isGranted
    }

    // Refresh state when composable is shown
    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    if (showPermissionSheet) {
        PermissionsBottomSheet(
            onDismissRequest = { showPermissionSheet = false },
            featureTitle = "Show Notification",
            permissions = listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_notifications_unread_24,
                    title = "Post Notifications",
                    description = "Allows the app to show notifications",
                    dependentFeatures = listOf("Show notification"),
                    actionLabel = "Grant Permission",
                    action = {
                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        showPermissionSheet = false
                    },
                    isGranted = viewModel.postNotificationsGranted.value
                )
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsCard(title = "Notification") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Show notification",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    Switch(
                        checked = viewModel.showNotification.value,
                        onCheckedChange = { isChecked ->
                            viewModel.setShowNotification(isChecked, context)
                        },
                        enabled = viewModel.postNotificationsGranted.value
                    )

                    // Invisible overlay catches taps on disabled Switch
                    if (!viewModel.postNotificationsGranted.value) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    showPermissionSheet = true
                                }
                        )
                    }
                }
            }
        }
    }
}
