/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Audio
 * File: AppVolumeSettingsUI.kt
 * Description: Settings page for per-app volume levels.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.SingleAppSelectionSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.media.AppVolumeController
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlin.math.roundToInt

@Composable
fun AppVolumeSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val playing by AppVolumeController.playingPackages.collectAsState()
    val levels = viewModel.appVolumeLevels.value
    val isEnabled = viewModel.isAppVolumeEnabled.value
    var showAppPicker by remember { mutableStateOf(false) }
    var editingPackage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_speaker_group_24,
                title = stringResource(R.string.app_volume_enable_title),
                isChecked = isEnabled,
                onCheckedChange = { viewModel.setAppVolumeEnabled(it) },
                enabled = viewModel.isAccessibilityEnabled.value && viewModel.isShizukuPermissionGranted.value,
                modifier = Modifier.highlight(highlightSetting == "enable_app_volume"),
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_volume_up_24,
                title = stringResource(R.string.app_volume_overlay_title),
                isChecked = viewModel.isAppVolumeOverlayEnabled.value,
                onCheckedChange = { viewModel.setAppVolumeOverlayEnabled(it) },
                enabled = isEnabled && viewModel.isAccessibilityEnabled.value && viewModel.isShizukuPermissionGranted.value,
                modifier = Modifier.highlight(highlightSetting == "app_volume_overlay"),
            )
        }

        Text(
            text = stringResource(R.string.app_volume_apps_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
            modifier = Modifier.highlight(highlightSetting == "app_volume_apps"),
        ) {
            (playing + levels.keys).distinct().forEach { packageName ->
                val appIcon =
                    remember(packageName) {
                        runCatching {
                            BitmapPainter(
                                AppUtil
                                    .drawableToBitmap(context.packageManager.getApplicationIcon(packageName))
                                    .asImageBitmap(),
                            )
                        }.getOrNull()
                    }
                val percent = formatLevel(levels[packageName] ?: 1f)
                FeatureCard(
                    title = appLabel(packageName),
                    descriptionOverride =
                        if (packageName in playing) {
                            stringResource(R.string.app_volume_playing_summary, percent)
                        } else {
                            percent
                        },
                    isEnabled = true,
                    onToggle = {},
                    onClick = { editingPackage = packageName },
                    iconPainter = appIcon,
                    showToggle = false,
                    hasMoreSettings = true,
                    customTrailingContent =
                        if (packageName in levels) {
                            {
                                IconButton(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        viewModel.removeAppVolumeLevel(packageName)
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_delete_24),
                                        contentDescription = stringResource(R.string.action_remove),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        } else {
                            null
                        },
                )
            }

            FeatureCard(
                title = R.string.app_volume_add_app,
                description = R.string.app_volume_add_app_desc,
                iconRes = R.drawable.rounded_add_24,
                isEnabled = true,
                onToggle = {},
                onClick = { showAppPicker = true },
                showToggle = false,
                hasMoreSettings = false,
            )
        }

        Text(
            text = stringResource(R.string.app_volume_hint),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showAppPicker) {
        SingleAppSelectionSheet(
            onDismissRequest = { showAppPicker = false },
            includeSystemApps = true,
            onAppSelected = { app ->
                showAppPicker = false
                editingPackage = app.packageName
            },
        )
    }

    editingPackage?.let { packageName ->
        AppVolumeSheet(
            title = appLabel(packageName),
            level = levels[packageName] ?: 1f,
            onLevelChange = { AppVolumeController.preview(packageName, it) },
            onLevelChangeFinished = { viewModel.setAppVolumeLevel(packageName, it) },
            onDismiss = {
                editingPackage = null
                AppVolumeController.refresh()
            },
        )
    }
}

@Composable
private fun AppVolumeSheet(
    title: String,
    level: Float,
    onLevelChange: (Float) -> Unit,
    onLevelChangeFinished: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableFloatStateOf(level) }

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                ConfigSliderItem(
                    title = stringResource(R.string.app_volume_level_title),
                    value = current,
                    onValueChange = {
                        current = it
                        onLevelChange(it)
                    },
                    onValueChangeFinished = { onLevelChangeFinished(current) },
                    valueRange = 0f..1f,
                    steps = 19,
                    increment = 0.05f,
                    valueFormatter = { formatLevel(it) },
                    iconRes = R.drawable.rounded_volume_up_24,
                )
            }
            Text(
                text = stringResource(R.string.app_volume_sheet_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

private fun formatLevel(level: Float): String = "${(level * 100).roundToInt()}%"

@Composable
private fun appLabel(packageName: String): String {
    val context = LocalContext.current
    return remember(packageName) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }
}
