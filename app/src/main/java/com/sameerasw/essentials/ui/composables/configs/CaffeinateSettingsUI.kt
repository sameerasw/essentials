package com.sameerasw.essentials.ui.composables.configs

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.viewmodels.CaffeinateViewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.cards.SimpleToggleItem

@Composable
fun CaffeinateSettingsUI(
    viewModel: CaffeinateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        showPermissionSheet = false
                    },
                    isGranted = viewModel.postNotificationsGranted.value
                )
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Notification Category
        Text(
            text = "Notification",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            IconToggleItem(
                title = "Show notification",
                isChecked = viewModel.showNotification.value,
                onCheckedChange = { isChecked ->
                    viewModel.setShowNotification(isChecked, context)
                },
                enabled = viewModel.postNotificationsGranted.value,
                onDisabledClick = {
                    showPermissionSheet = true
                },
                iconRes = R.drawable.rounded_notifications_unread_24,
            )
        }
    }
}
