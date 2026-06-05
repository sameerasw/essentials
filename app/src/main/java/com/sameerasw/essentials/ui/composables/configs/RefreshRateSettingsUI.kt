package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.RefreshRateUtils
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlin.math.roundToInt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import com.sameerasw.essentials.domain.model.AppRefreshRateConfig
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.sheets.PerAppRefreshRateSettingsSheet
import com.sameerasw.essentials.ui.components.sheets.SingleAppSelectionSheet
import com.sameerasw.essentials.utils.AppUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefreshRateSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isEnabled = viewModel.isShizukuPermissionGranted.value
    val isFixedMode = viewModel.refreshRateMode.value == RefreshRateUtils.MODE_FIXED
    val systemLabel = stringResource(R.string.refresh_rate_system_default)

    var isAppSelectionSheetOpen by remember { mutableStateOf(false) }
    var isEditSheetOpen by remember { mutableStateOf(false) }
    var editingPackageName by remember { mutableStateOf("") }
    var editingCurrentRate by remember { mutableStateOf(0f) }
    var editingIsFixed by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.refresh_rate_section_mode),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(spacing = 2.dp) {
            SegmentedPicker(
                items = listOf(RefreshRateUtils.MODE_FIXED, RefreshRateUtils.MODE_RANGE),
                selectedItem = viewModel.refreshRateMode.value,
                onItemSelected = { viewModel.setRefreshRateMode(it) },
                labelProvider = {
                    when (it) {
                        RefreshRateUtils.MODE_RANGE -> context.getString(R.string.refresh_rate_mode_range)
                        else -> context.getString(R.string.refresh_rate_mode_fixed)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = stringResource(R.string.refresh_rate_section_values),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(spacing = 2.dp) {
            if (isFixedMode) {
                ConfigSliderItem(
                    title = stringResource(R.string.refresh_rate_fixed_title),
                    description = stringResource(R.string.refresh_rate_fixed_desc),
                    value = viewModel.fixedRefreshRate.floatValue,
                    onValueChange = {
                        viewModel.updateFixedRefreshRate(it.roundToInt().toFloat())
                        HapticUtil.performSliderHaptic(view)
                    },
                    onValueChangeFinished = {
                        viewModel.applyFixedRefreshRate(context)
                    },
                    valueRange = 0f..120f,
                    steps = 11,
                    increment = 10f,
                    valueFormatter = { formatRefreshRateLabel(it, systemLabel) },
                    icon = R.drawable.rounded_shutter_speed_24,
                    enabled = isEnabled
                )
            } else {
                ConfigSliderItem(
                    title = stringResource(R.string.refresh_rate_min_title),
                    description = stringResource(R.string.refresh_rate_min_desc),
                    value = viewModel.minRefreshRate.floatValue,
                    onValueChange = {
                        viewModel.updateMinRefreshRate(it.roundToInt().toFloat())
                        HapticUtil.performSliderHaptic(view)
                    },
                    onValueChangeFinished = {
                        viewModel.applyRefreshRateRange(context)
                    },
                    valueRange = 0f..120f,
                    steps = 11,
                    increment = 10f,
                    valueFormatter = { formatRefreshRateLabel(it, systemLabel) },
                    icon = R.drawable.rounded_keyboard_arrow_down_24,
                    enabled = isEnabled
                )

                ConfigSliderItem(
                    title = stringResource(R.string.refresh_rate_peak_title),
                    description = stringResource(R.string.refresh_rate_peak_desc),
                    value = viewModel.peakRefreshRate.floatValue,
                    onValueChange = {
                        viewModel.updatePeakRefreshRate(it.roundToInt().toFloat())
                        HapticUtil.performSliderHaptic(view)
                    },
                    onValueChangeFinished = {
                        viewModel.applyRefreshRateRange(context)
                    },
                    valueRange = 0f..120f,
                    steps = 11,
                    increment = 10f,
                    valueFormatter = { formatRefreshRateLabel(it, systemLabel) },
                    icon = R.drawable.rounded_keyboard_arrow_up_24,
                    enabled = isEnabled
                )
            }

            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                    )
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = if (isEnabled) Arrangement.SpaceBetween else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEnabled) {
                    Text(
                        text = stringResource(R.string.refresh_rate_reset_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Button(
                        onClick = {
                            viewModel.resetRefreshRate(context)
                            HapticUtil.performSliderHaptic(view)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_reset_default),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.msg_refresh_rate_permission_required),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Button(
                        onClick = {
                            viewModel.requestShizukuPermission()
                            HapticUtil.performSliderHaptic(view)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_grant_permission),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.refresh_rate_section_per_app),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val onTogglePerAppRefreshRate: (Boolean) -> Unit = { enabled ->
            if (enabled) {
                val isUseUsageAccessVal = viewModel.isUseUsageAccess.value
                val hasPermission = if (isUseUsageAccessVal) {
                    viewModel.isUsageStatsPermissionGranted.value
                } else {
                    viewModel.isAccessibilityEnabled.value
                }

                if (!hasPermission) {
                    if (isUseUsageAccessVal) {
                        com.sameerasw.essentials.utils.PermissionUtils.openUsageStatsSettings(context)
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.refresh_rate_per_app_usage_access_required),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    } else {
                        com.sameerasw.essentials.utils.PermissionUtils.openAccessibilitySettings(context)
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.refresh_rate_per_app_accessibility_required),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    viewModel.setPerAppRefreshRateEnabled(true, context)
                }
            } else {
                viewModel.setPerAppRefreshRateEnabled(false, context)
            }
        }

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            FeatureCard(
                title = stringResource(R.string.refresh_rate_per_app_enable_title),
                description = stringResource(R.string.refresh_rate_per_app_enable_desc),
                iconRes = R.drawable.rounded_shutter_speed_24,
                isEnabled = viewModel.isPerAppRefreshRateEnabled.value,
                showToggle = true,
                hasMoreSettings = false,
                onToggle = onTogglePerAppRefreshRate,
                onClick = { onTogglePerAppRefreshRate(!viewModel.isPerAppRefreshRateEnabled.value) }
            )
        }

        if (viewModel.isPerAppRefreshRateEnabled.value) {
            RoundedCardContainer(
                modifier = Modifier,
                spacing = 2.dp,
                cornerRadius = 24.dp
            ) {
                FeatureCard(
                    title = stringResource(R.string.refresh_rate_per_app_add_app),
                    description = stringResource(R.string.refresh_rate_per_app_add_app_desc),
                    iconRes = R.drawable.rounded_add_24,
                    isEnabled = true,
                    showToggle = false,
                    hasMoreSettings = false,
                    onToggle = {},
                    onClick = { isAppSelectionSheetOpen = true }
                )
            }

            val configs by viewModel.perAppRefreshRateConfigs
            if (configs.isNotEmpty()) {
                RoundedCardContainer(
                    modifier = Modifier,
                    spacing = 2.dp,
                    cornerRadius = 24.dp
                ) {
                    configs.forEach { config ->
                        val appName = remember(config.packageName) {
                            try {
                                val appInfo = context.packageManager.getApplicationInfo(config.packageName, 0)
                                context.packageManager.getApplicationLabel(appInfo).toString()
                            } catch (e: Exception) {
                                config.packageName
                            }
                        }

                        val appIconPainter = remember(config.packageName) {
                            try {
                                val drawable = context.packageManager.getApplicationIcon(config.packageName)
                                androidx.compose.ui.graphics.painter.BitmapPainter(
                                    AppUtil.drawableToBitmap(drawable).asImageBitmap()
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }

                        FeatureCard(
                            title = appName,
                            description = "${config.refreshRate.toInt()} Hz (${if (config.isFixed) stringResource(R.string.refresh_rate_per_app_mode_fixed) else stringResource(R.string.refresh_rate_per_app_mode_dynamic)})",
                            isEnabled = config.isEnabled,
                            showToggle = true,
                            onToggle = { isChecked ->
                                viewModel.updatePerAppRefreshRateConfig(config.copy(isEnabled = isChecked))
                            },
                            onClick = {
                                editingPackageName = config.packageName
                                editingCurrentRate = config.refreshRate
                                editingIsFixed = config.isFixed
                                isEditSheetOpen = true
                            },
                            iconPainter = appIconPainter,
                            hasMoreSettings = true,
                            additionalMenuItems = { onDismiss ->
                                SegmentedDropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_remove)) },
                                    onClick = {
                                        onDismiss()
                                        viewModel.removePerAppRefreshRateConfig(config.packageName)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_delete_24),
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        if (isAppSelectionSheetOpen) {
            SingleAppSelectionSheet(
                onDismissRequest = { isAppSelectionSheetOpen = false },
                onAppSelected = { app ->
                    isAppSelectionSheetOpen = false
                    editingPackageName = app.packageName
                    editingCurrentRate = 0f
                    editingIsFixed = false
                    isEditSheetOpen = true
                }
            )
        }

        if (isEditSheetOpen) {
            PerAppRefreshRateSettingsSheet(
                packageName = editingPackageName,
                currentRate = editingCurrentRate,
                isFixed = editingIsFixed,
                onSave = { rate, isFixed ->
                    viewModel.updatePerAppRefreshRateConfig(
                        AppRefreshRateConfig(
                            packageName = editingPackageName,
                            refreshRate = rate,
                            isFixed = isFixed,
                            isEnabled = true
                        )
                    )
                },
                onDelete = {
                    viewModel.removePerAppRefreshRateConfig(editingPackageName)
                },
                onDismissRequest = { isEditSheetOpen = false }
            )
        }
    }
}

private fun formatRefreshRateLabel(value: Float, systemLabel: String): String {
    return if (value <= 0f) {
        systemLabel
    } else {
        "${value.roundToInt()} Hz"
    }
}
