/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Audio
 * File: AutoDismissNotificationsSettingsUI.kt
 * Description: UI component and settings composable for Auto-dismiss notifications feature.
 */

package com.sameerasw.essentials.ui.features.system

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.AutoDismissAppConfig
import com.sameerasw.essentials.services.AUTO_DISMISS_MINUTES
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutoDismissNotificationsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    var isAppSelectionSheetOpen by remember { mutableStateOf(false) }
    var selectedAppForEditing by remember { mutableStateOf<AutoDismissAppConfig?>(null) }

    val configs by viewModel.autoDismissConfigs
    val channels by viewModel.autoDismissChannels
    val stats by viewModel.autoDismissStats

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            FeatureCard(
                title = stringResource(R.string.auto_dismiss_select_apps_title),
                description = stringResource(R.string.auto_dismiss_select_apps_desc),
                iconRes = R.drawable.rounded_app_registration_24,
                isEnabled = true,
                showToggle = false,
                hasMoreSettings = true,
                onToggle = {},
                onClick = { isAppSelectionSheetOpen = true },
            )
        }

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            configs.forEach { config ->
                val appName =
                    remember(config.packageName) {
                        try {
                            val appInfo =
                                context.packageManager.getApplicationInfo(config.packageName, 0)
                            context.packageManager.getApplicationLabel(appInfo).toString()
                        } catch (e: Exception) {
                            config.packageName
                        }
                    }

                val appIconPainter =
                    remember(config.packageName) {
                        try {
                            val drawable = context.packageManager.getApplicationIcon(config.packageName)
                            BitmapPainter(
                                AppUtil.drawableToBitmap(drawable).asImageBitmap(),
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                val timeDescription = formatAutoDismissTime(context, config.minutes)
                val baseDescription =
                    if (!config.excludedChannels.isNullOrEmpty()) {
                        val count = config.excludedChannels.size
                        val offText =
                            context.resources.getQuantityString(
                                R.plurals.auto_dismiss_categories_off,
                                count,
                                count,
                            )
                        "$timeDescription · $offText"
                    } else {
                        timeDescription
                    }
                val stat = stats[config.packageName]
                val clearedCount = stat?.count ?: 0
                val finalDescription =
                    if (clearedCount > 0) {
                        val clearedText =
                            context.resources.getQuantityString(
                                R.plurals.auto_dismiss_cleared_count,
                                clearedCount,
                                clearedCount,
                            )
                        "$baseDescription · $clearedText"
                    } else {
                        baseDescription
                    }

                FeatureCard(
                    title = appName,
                    description = finalDescription,
                    isEnabled = true,
                    onToggle = {},
                    onClick = { selectedAppForEditing = config },
                    iconPainter = appIconPainter,
                    showToggle = false,
                    hasMoreSettings = true,
                    customTrailingContent = {
                        IconButton(
                            onClick = {
                                if (selectedAppForEditing?.packageName == config.packageName) {
                                    selectedAppForEditing = null
                                }
                                viewModel.removeAutoDismissApp(config.packageName)
                            },
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_delete_24),
                                contentDescription = stringResource(R.string.action_remove),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    additionalMenuItems = { onDismiss ->
                        SegmentedDropdownMenuItem(
                            text = { Text(stringResource(R.string.action_remove)) },
                            onClick = {
                                onDismiss()
                                viewModel.removeAutoDismissApp(config.packageName)
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_delete_24),
                                    contentDescription = null,
                                )
                            },
                        )
                    },
                )
            }
        }

        Text(
            text = stringResource(R.string.auto_dismiss_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isAppSelectionSheetOpen) {
            AppSelectionSheet(
                onDismissRequest = { isAppSelectionSheetOpen = false },
                onLoadApps = { _ ->
                    viewModel.autoDismissConfigs.value.map { AppSelection(it.packageName, true) }
                },
                onSaveApps = { ctx, apps -> viewModel.saveAutoDismissSelectedApps(ctx, apps) },
            )
        }

        val editing = selectedAppForEditing
        if (editing != null) {
            val targetPkg = editing.packageName
            val currentMinutes =
                configs.find { it.packageName == targetPkg }?.minutes
                    ?: editing.minutes
            var sliderIndex by remember(targetPkg) {
                val idx = AUTO_DISMISS_MINUTES.indexOf(currentMinutes)
                mutableFloatStateOf(if (idx >= 0) idx.toFloat() else 3f)
            }

            ModalBottomSheet(
                onDismissRequest = { selectedAppForEditing = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ConfigSliderItem(
                        title = stringResource(R.string.auto_dismiss_time_title),
                        value = sliderIndex,
                        onValueChange = { sliderIndex = it },
                        valueRange = 0f..10f,
                        steps = 9,
                        increment = 1f,
                        valueFormatter = { idx ->
                            val mins =
                                AUTO_DISMISS_MINUTES.getOrElse(
                                    idx.toInt().coerceIn(0, AUTO_DISMISS_MINUTES.size - 1),
                                ) { 15 }
                            formatAutoDismissTime(context, mins)
                        },
                        onValueChangeFinished = {
                            val idx = sliderIndex.toInt().coerceIn(0, AUTO_DISMISS_MINUTES.size - 1)
                            val minutes = AUTO_DISMISS_MINUTES[idx]
                            viewModel.setAutoDismissMinutes(targetPkg, minutes)
                        },
                        iconRes = R.drawable.rounded_timer_24,
                    )

                    val appStat = stats[targetPkg]
                    val count = appStat?.count ?: 0
                    val sinceMillis = appStat?.since ?: System.currentTimeMillis()
                    val formattedDate = formatAutoDismissDate(sinceMillis)
                    Text(
                        text =
                            context.resources.getQuantityString(
                                R.plurals.auto_dismiss_cleared_since,
                                count,
                                count,
                                formattedDate,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )

                    Text(
                        text = stringResource(R.string.auto_dismiss_categories_title),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 8.dp),
                    )

                    val appChannels =
                        remember(channels, targetPkg) {
                            channels.filter { it.packageName == targetPkg }.sortedBy { it.name }
                        }
                    val currentConfig = configs.find { it.packageName == targetPkg } ?: editing
                    val excludedChannels = currentConfig.excludedChannels.orEmpty()

                    if (appChannels.isNotEmpty()) {
                        RoundedCardContainer(
                            spacing = 2.dp,
                            cornerRadius = 24.dp,
                        ) {
                            appChannels.forEach { channel ->
                                IconToggleItem(
                                    title = channel.name,
                                    isChecked = channel.id !in excludedChannels,
                                    onCheckedChange = { isChecked ->
                                        viewModel.setAutoDismissChannelIncluded(
                                            targetPkg,
                                            channel.id,
                                            isChecked,
                                        )
                                    },
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.auto_dismiss_categories_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun formatAutoDismissTime(
    context: Context,
    minutes: Int,
): String =
    if (minutes >= 60) {
        context.getString(R.string.auto_dismiss_time_hours, minutes / 60)
    } else {
        context.getString(R.string.auto_dismiss_time_minutes, minutes)
    }

private fun formatAutoDismissDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
