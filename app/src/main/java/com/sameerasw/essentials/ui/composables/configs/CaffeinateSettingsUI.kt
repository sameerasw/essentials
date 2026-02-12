package com.sameerasw.essentials.ui.composables.configs

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.MultiSegmentedPicker
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.viewmodels.CaffeinateViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaffeinateSettingsUI(
    viewModel: CaffeinateViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
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
            featureTitle = R.string.permission_show_notification_title,
            permissions = listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_notifications_unread_24,
                    title = R.string.permission_post_notifications_title,
                    description = R.string.permission_post_notifications_desc,
                    dependentFeatures = listOf(R.string.permission_show_notification_title),
                    actionLabel = R.string.permission_grant_action,
                    action = {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    isGranted = viewModel.postNotificationsGranted.value
                ),
                PermissionItem(
                    iconRes = R.drawable.rounded_battery_android_frame_alert_24,
                    title = R.string.perm_battery_optimization_title,
                    description = R.string.perm_battery_optimization_desc,
                    dependentFeatures = listOf(R.string.feat_caffeinate_title),
                    actionLabel = R.string.permission_grant_action,
                    action = {
                        val intent =
                            Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                        context.startActivity(intent)
                    },
                    isGranted = viewModel.batteryOptimizationGranted.value
                )
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RoundedCardContainer {

            IconToggleItem(
                title = stringResource(R.string.caffeinate_battery_optimization_title),
                isChecked = viewModel.batteryOptimizationGranted.value,
                onCheckedChange = { _ ->
                    val intent =
                        Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                    context.startActivity(intent)
                },
                iconRes = R.drawable.rounded_battery_android_frame_alert_24,
            )
        }

        RoundedCardContainer {
            IconToggleItem(
                title = stringResource(R.string.caffeinate_abort_screen_off_title),
                isChecked = viewModel.abortWithScreenOff.value,
                onCheckedChange = { viewModel.setAbortWithScreenOff(it, context) },
                iconRes = R.drawable.rounded_power_settings_new_24,
            )

            IconToggleItem(
                title = stringResource(R.string.caffeinate_skip_countdown_title),
                description = stringResource(R.string.caffeinate_skip_countdown_desc),
                isChecked = viewModel.skipCountdown.value,
                onCheckedChange = { viewModel.setSkipCountdown(it, context) },
                iconRes = R.drawable.rounded_timer_off_24,
            )
        }

        RoundedCardContainer {
            IconToggleItem(
                title = stringResource(R.string.caffeinate_timeout_presets_title),
                description = stringResource(R.string.caffeinate_timeout_presets_desc),
                isChecked = true,
                onCheckedChange = { },
                iconRes = R.drawable.rounded_timer_24,
                showToggle = false
            )

            MultiSegmentedPicker(
                items = viewModel.timeoutPresets,
                selectedItems = viewModel.enabledPresets.value,
                onItemsSelected = { newSelection ->
                    // Correctly update presets via viewModel
                    // Since MultiSegmentedPicker manages the set, we can just sync it
                    val prefs =
                        context.getSharedPreferences("caffeinate_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putStringSet("enabled_presets", newSelection.map { it.toString() }.toSet())
                        .apply()
                    viewModel.enabledPresets.value = newSelection
                },
                labelProvider = { preset ->
                    when (preset) {
                        5 -> context.getString(R.string.caffeinate_timeout_5m)
                        10 -> context.getString(R.string.caffeinate_timeout_10m)
                        30 -> context.getString(R.string.caffeinate_timeout_30m)
                        60 -> context.getString(R.string.caffeinate_timeout_1h)
                        else -> context.getString(R.string.caffeinate_timeout_infinity)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
