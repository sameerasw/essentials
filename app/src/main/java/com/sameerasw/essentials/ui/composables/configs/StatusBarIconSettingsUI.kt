package com.sameerasw.essentials.ui.composables.configs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.StatusBarIconRegistry
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.NetworkTypePicker
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.StatusBarIconViewModel

@Composable
fun StatusBarIconSettingsUI(
    viewModel: StatusBarIconViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isPermissionGranted =
        viewModel.isWriteSecureSettingsEnabled.value || viewModel.isShizukuAvailable.value || viewModel.isRootAvailable.value
    val hasWriteSettings = viewModel.isWriteSettingsEnabled.value
    val batteryPermissionGranted =
        isPermissionGranted || hasWriteSettings || viewModel.isShizukuAvailable.value || viewModel.isRootAvailable.value

    var showPermissionSheet by remember { mutableStateOf(false) }

    // Refresh permission state when composable is shown
    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    // Permission sheet for Smart Data
    if (showPermissionSheet) {
        PermissionsBottomSheet(
            onDismissRequest = { showPermissionSheet = false },
            featureTitle = R.string.smart_data_title,
            permissions = listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_android_cell_dual_4_bar_24,
                    title = R.string.permission_read_phone_state_title,
                    description = R.string.permission_read_phone_state_desc,
                    dependentFeatures = listOf(R.string.smart_data_title),
                    actionLabel = R.string.permission_grant_action,
                    action = {
                        ActivityCompat.requestPermissions(
                            context as ComponentActivity,
                            arrayOf(Manifest.permission.READ_PHONE_STATE),
                            1001
                        )
                    },
                    isGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED
                )
            )
        )
    }

    // Categorized icons
    val categories = listOf(
        StatusBarIconRegistry.CAT_CONNECTIVITY,
        StatusBarIconRegistry.CAT_PHONE_NETWORK,
        StatusBarIconRegistry.CAT_AUDIO_MEDIA,
        StatusBarIconRegistry.CAT_SYSTEM_STATUS,
        StatusBarIconRegistry.CAT_OEM_SPECIFIC
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Iterate through categories
        categories.forEach { categoryRes ->
            val iconsInCat =
                StatusBarIconRegistry.ALL_ICONS.filter { it.categoryRes == categoryRes }
            if (iconsInCat.isNotEmpty()) {
                Text(
                    text = stringResource(categoryRes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RoundedCardContainer(
                    modifier = Modifier,
                    spacing = 2.dp,
                    cornerRadius = 24.dp
                ) {
                    iconsInCat.forEach { icon ->
                        val isChecked =
                            viewModel.getIconVisibility(icon.id)?.value ?: icon.defaultVisible
                        IconToggleItem(
                            iconRes = icon.iconRes ?: R.drawable.rounded_info_24,
                            title = stringResource(icon.displayNameRes),
                            isChecked = isChecked,
                            onCheckedChange = { checked ->
                                viewModel.setIconVisibility(icon.id, checked, context)
                            },
                            enabled = isPermissionGranted,
                            modifier = Modifier.highlight(highlightSetting == context.getString(icon.displayNameRes))
                        )
                    }
                }
            }
        }

        // Smart Visibility Category
        Text(
            text = stringResource(R.string.settings_section_smart_visibility),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_cell_wifi_24,
                title = stringResource(R.string.smart_wifi_title),
                description = stringResource(R.string.smart_wifi_desc),
                isChecked = viewModel.isSmartWiFiEnabled.value,
                onCheckedChange = { isChecked ->
                    viewModel.setSmartWiFiEnabled(isChecked, context)
                },
                enabled = isPermissionGranted && viewModel.isMobileDataVisible.value,
                modifier = Modifier.highlight(highlightSetting == "smart_wifi")
            )

            Box(
                modifier = Modifier
                    .highlight(highlightSetting == "smart_data")
                    .clickable {
                        HapticUtil.performUIHaptic(view)
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_PHONE_STATE
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            showPermissionSheet = true
                        }
                    }
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_android_cell_dual_5_bar_alert_24,
                    title = stringResource(R.string.smart_data_title),
                    description = stringResource(R.string.smart_data_desc),
                    isChecked = viewModel.isSmartDataEnabled.value,
                    onCheckedChange = { isChecked ->
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_PHONE_STATE
                        ) == PackageManager.PERMISSION_GRANTED

                        if (isChecked && !hasPermission) {
                            showPermissionSheet = true
                        } else {
                            viewModel.setSmartDataEnabled(isChecked, context)
                        }
                    },
                    enabled = isPermissionGranted && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED && viewModel.isMobileDataVisible.value,
                    onDisabledClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_PHONE_STATE
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            showPermissionSheet = true
                        }
                    }
                )

                val isSwitchDisabled =
                    !(isPermissionGranted && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_PHONE_STATE
                    ) == PackageManager.PERMISSION_GRANTED)

                if (isSwitchDisabled) {
                    Box(modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            HapticUtil.performUIHaptic(view)
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_PHONE_STATE
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasPermission) {
                                showPermissionSheet = true
                            }
                        })
                }
            }

            // Network Type Picker (only show when Smart Data is enabled)
            if (viewModel.isSmartDataEnabled.value) {
                Column {
                    NetworkTypePicker(
                        selectedTypes = viewModel.selectedNetworkTypes.value,
                        onTypesSelected = { selectedTypes ->
                            viewModel.selectedNetworkTypes.value = selectedTypes
                            // Save to preferences
                            val prefs = context.getSharedPreferences(
                                "essentials_prefs",
                                Context.MODE_PRIVATE
                            )
                            prefs.edit {
                                putStringSet(
                                    "selected_network_types",
                                    selectedTypes.map { it.name }.toSet()
                                )
                            }
                        }
                    )
                }
            }
        }

        // Advanced Status Bar Settings
        Text(
            text = stringResource(R.string.status_bar_section_advanced),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_nest_clock_farsight_analog_24,
                title = stringResource(R.string.stb_clock_seconds),
                isChecked = viewModel.isClockSecondsEnabled.value,
                onCheckedChange = { checked ->
                    viewModel.setClockSecondsEnabled(checked, context)
                },
                enabled = isPermissionGranted,
                modifier = Modifier.highlight(highlightSetting == "clock_seconds")
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_security_24,
                title = stringResource(R.string.stb_privacy_chip),
                isChecked = viewModel.isPrivacyChipEnabled.value,
                onCheckedChange = { checked ->
                    viewModel.setPrivacyChipEnabled(checked, context)
                },
                enabled = isPermissionGranted,
                modifier = Modifier.highlight(highlightSetting == "privacy_chip")
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                    )
                    .padding(16.dp)
                    .highlight(highlightSetting == "battery_percentage")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.stb_battery_percentage),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (batteryPermissionGranted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f
                        )
                    )
                    if (!viewModel.isShizukuAvailable.value && !viewModel.isRootAvailable.value) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.req_shizuku),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                SegmentedPicker(
                    items = listOf(0, 1, 2), // 0: Never, 1: Always, 2: Charging
                    selectedItem = viewModel.batteryPercentageMode.value,
                    onItemSelected = { mode ->
                        viewModel.setBatteryPercentageMode(mode, context)
                    },
                    labelProvider = { mode ->
                        when (mode) {
                            0 -> context.getString(R.string.stb_battery_percentage_never)
                            1 -> context.getString(R.string.stb_battery_percentage_always)
                            2 -> context.getString(R.string.stb_battery_percentage_charging)
                            else -> ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (!batteryPermissionGranted) Modifier.alpha(0.5f) else Modifier),
                )
            }
        }

        // Reset All Icons Button
        Button(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                viewModel.resetAllIcons(context)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .highlight(highlightSetting == "reset_icons"),
            enabled = isPermissionGranted
        ) {
            Text(stringResource(R.string.action_reset_all_icons))
        }

        Text(stringResource(R.string.status_bar_icons_disclaimer))
    }
}
