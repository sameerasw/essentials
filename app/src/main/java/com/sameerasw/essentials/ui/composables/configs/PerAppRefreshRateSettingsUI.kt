package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.AppRefreshRateConfig
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.sheets.PerAppRefreshRateSettingsSheet
import com.sameerasw.essentials.ui.components.sheets.SingleAppSelectionSheet
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PerAppRefreshRateSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    var isAppSelectionSheetOpen by remember { mutableStateOf(false) }
    var isEditSheetOpen by remember { mutableStateOf(false) }
    var editingPackageName by remember { mutableStateOf("") }
    var editingCurrentRate by remember { mutableStateOf(0f) }
    var editingIsFixed by remember { mutableStateOf(false) }
    var editingLandscapeRate by remember { mutableStateOf<Float?>(null) }
    var editingOnlyOnMediaPlaying by remember { mutableStateOf(false) }

    val configs by viewModel.perAppRefreshRateConfigs

    val checkPermissionAndRun: (onGranted: () -> Unit) -> Unit = { onGranted ->
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
        } else if (!viewModel.isShizukuPermissionGranted.value) {
            viewModel.requestShizukuPermission()
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.msg_refresh_rate_permission_required),
                android.widget.Toast.LENGTH_LONG
            ).show()
        } else {
            onGranted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                onClick = {
                    checkPermissionAndRun {
                        isAppSelectionSheetOpen = true
                    }
                }
            )
        }

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

                    val suffix = if (config.onlyOnMediaPlaying) " (Media Only)" else ""
                    val cardDesc = if (config.landscapeRefreshRate != null) {
                        "${config.refreshRate.toInt()} Hz (${if (config.isFixed) stringResource(R.string.refresh_rate_per_app_mode_fixed) else stringResource(R.string.refresh_rate_per_app_mode_dynamic)}) | ${config.landscapeRefreshRate.toInt()} Hz in landscape$suffix"
                    } else {
                        "${config.refreshRate.toInt()} Hz (${if (config.isFixed) stringResource(R.string.refresh_rate_per_app_mode_fixed) else stringResource(R.string.refresh_rate_per_app_mode_dynamic)})"
                    }

                    FeatureCard(
                        title = appName,
                        description = cardDesc,
                        isEnabled = config.isEnabled,
                        showToggle = true,
                        onToggle = { isChecked ->
                            if (isChecked) {
                                checkPermissionAndRun {
                                    viewModel.updatePerAppRefreshRateConfig(config.copy(isEnabled = true))
                                    val anyEnabled = configs.any { it.packageName != config.packageName && it.isEnabled } || true
                                    viewModel.setPerAppRefreshRateEnabled(anyEnabled, context)
                                }
                            } else {
                                viewModel.updatePerAppRefreshRateConfig(config.copy(isEnabled = false))
                                val anyEnabled = configs.any { it.packageName != config.packageName && it.isEnabled }
                                viewModel.setPerAppRefreshRateEnabled(anyEnabled, context)
                            }
                        },
                        onClick = {
                            editingPackageName = config.packageName
                            editingCurrentRate = config.refreshRate
                            editingIsFixed = config.isFixed
                            editingLandscapeRate = config.landscapeRefreshRate
                            editingOnlyOnMediaPlaying = config.onlyOnMediaPlaying
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
                                    val anyEnabled = configs.filter { it.packageName != config.packageName }.any { it.isEnabled }
                                    viewModel.setPerAppRefreshRateEnabled(anyEnabled, context)
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

        if (isAppSelectionSheetOpen) {
            SingleAppSelectionSheet(
                onDismissRequest = { isAppSelectionSheetOpen = false },
                onAppSelected = { app ->
                    isAppSelectionSheetOpen = false
                    editingPackageName = app.packageName
                    editingCurrentRate = 0f
                    editingIsFixed = false
                    editingLandscapeRate = null
                    editingOnlyOnMediaPlaying = false
                    isEditSheetOpen = true
                }
            )
        }

        if (isEditSheetOpen) {
            PerAppRefreshRateSettingsSheet(
                packageName = editingPackageName,
                currentRate = editingCurrentRate,
                isFixed = editingIsFixed,
                landscapeRate = editingLandscapeRate,
                onlyOnMediaPlaying = editingOnlyOnMediaPlaying,
                onSave = { rate, isFixed, landscapeRate, onlyOnMedia ->
                    viewModel.updatePerAppRefreshRateConfig(
                        AppRefreshRateConfig(
                            packageName = editingPackageName,
                            refreshRate = rate,
                            isFixed = isFixed,
                            landscapeRefreshRate = landscapeRate,
                            onlyOnMediaPlaying = onlyOnMedia,
                            isEnabled = true
                        )
                    )
                    viewModel.setPerAppRefreshRateEnabled(true, context)
                },
                onDelete = {
                    viewModel.removePerAppRefreshRateConfig(editingPackageName)
                    val anyEnabled = configs.filter { it.packageName != editingPackageName }.any { it.isEnabled }
                    viewModel.setPerAppRefreshRateEnabled(anyEnabled, context)
                },
                onDismissRequest = { isEditSheetOpen = false }
            )
        }
    }
}
