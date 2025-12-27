package com.sameerasw.essentials.ui.composables.configs

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.StatusBarIconRegistry
import com.sameerasw.essentials.viewmodels.StatusBarIconViewModel
import com.sameerasw.essentials.ui.components.pickers.NetworkTypePicker
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun StatusBarIconSettingsUI(
    viewModel: StatusBarIconViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isPermissionGranted = viewModel.isWriteSecureSettingsEnabled.value

    var showPermissionSheet by remember { mutableStateOf(false) }

    // Refresh permission state when composable is shown
    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    // Permission sheet for Smart Data
    if (showPermissionSheet) {
        PermissionsBottomSheet(
            onDismissRequest = { showPermissionSheet = false },
            featureTitle = "Smart Data",
            permissions = listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_android_cell_dual_4_bar_24,
                    title = "Read Phone State",
                    description = "Required to detect network type for Smart Data feature",
                    dependentFeatures = listOf("Smart Data"),
                    actionLabel = "Grant Permission",
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
        categories.forEach { categoryName ->
            val iconsInCat = StatusBarIconRegistry.ALL_ICONS.filter { it.category == categoryName }
            if (iconsInCat.isNotEmpty()) {
                Text(
                    text = categoryName,
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
                        val isChecked = viewModel.getIconVisibility(icon.id)?.value ?: icon.defaultVisible
                        IconToggleItem(
                            iconRes = icon.iconRes ?: R.drawable.rounded_info_24,
                            title = icon.displayName,
                            isChecked = isChecked,
                            onCheckedChange = { checked ->
                                viewModel.setIconVisibility(icon.id, checked, context)
                            },
                            enabled = isPermissionGranted
                        )
                    }
                }
            }
        }

        // Smart Visibility Category
        Text(
            text = "Smart Visibility",
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
                title = "Smart WiFi",
                description = "Hide mobile data when WiFi is connected",
                isChecked = viewModel.isSmartWiFiEnabled.value,
                onCheckedChange = { isChecked ->
                    viewModel.setSmartWiFiEnabled(isChecked, context)
                },
                enabled = isPermissionGranted && viewModel.isMobileDataVisible.value
            )

            Box(
                modifier = Modifier.clickable {
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
                    title = "Smart Data",
                    description = "Hide mobile data in certain modes",
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
                    Box(modifier = Modifier.matchParentSize().clickable {
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
                Column() {
                    NetworkTypePicker(
                        selectedTypes = viewModel.selectedNetworkTypes.value,
                        onTypesSelected = { selectedTypes ->
                            viewModel.selectedNetworkTypes.value = selectedTypes
                            // Save to preferences
                            val prefs = context.getSharedPreferences(
                                "essentials_prefs",
                                Context.MODE_PRIVATE
                            )
                            prefs.edit().putStringSet(
                                "selected_network_types",
                                selectedTypes.map { it.name }.toSet()
                            ).apply()
                        }
                    )
                }
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
                .padding(vertical = 16.dp),
            enabled = isPermissionGranted
        ) {
            Text("Reset All Icons")
        }

        Text("Please note that the implementation of these options may depend on the OEM and some may not be functional at all.")
    }
}
