/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Security
 * File: ScreenshotsSettingsUI.kt
 * Description: Settings for cleaning up shared and old screenshots.
 */

package com.sameerasw.essentials.ui.features.security

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.MainViewModel

private val SHARE_DELAY_MINUTES = listOf(0, 60, 24 * 60)
private val AUTO_CLEAN_DAYS = listOf(1, 7, 30)

@Composable
fun ScreenshotsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val showAfterSharing = DeviceUtils.isGoogleDevice() || viewModel.isEnableUnsupportedFeatures.value
    var requestingPermissionsFor by remember { mutableStateOf<List<String>?>(null) }

    fun missingPermissions(needsNotifications: Boolean): List<String> =
        buildList {
            if (!viewModel.isAccessibilityEnabled.value) add("ACCESSIBILITY")
            if (!viewModel.isStoragePermissionGranted.value) add("STORAGE")
            if (needsNotifications && !viewModel.isPostNotificationsEnabled.value) add("POST_NOTIFICATIONS")
        }

    requestingPermissionsFor?.let { permKeys ->
        PermissionsBottomSheet(
            onDismissRequest = {
                requestingPermissionsFor = null
                viewModel.check(context)
            },
            featureTitle = stringResource(R.string.feat_screenshots_title),
            permissions = PermissionUIHelper.getPermissionItems(permKeys, context, viewModel),
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
    ) {
        if (showAfterSharing) {
            SectionTitle(R.string.screenshots_section_after_sharing)
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_share_24,
                    title = stringResource(R.string.feat_screenshot_share_cleanup_title),
                    description = stringResource(R.string.feat_screenshot_share_cleanup_desc),
                    isChecked = viewModel.isScreenshotShareCleanupEnabled.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        val missing = missingPermissions(needsNotifications = false)
                        if (checked && missing.isNotEmpty()) {
                            requestingPermissionsFor = missing
                        } else {
                            viewModel.setScreenshotShareCleanupEnabled(checked)
                        }
                    },
                    modifier = Modifier.highlight(highlightSetting == "screenshot_share_cleanup_enabled"),
                )

                AnimatedVisibility(
                    visible = viewModel.isScreenshotShareCleanupEnabled.value,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    PickerRow(
                        title = R.string.screenshot_share_cleanup_delay_title,
                        highlighted = highlightSetting == "screenshot_share_cleanup_delay",
                    ) {
                        SegmentedPicker(
                            items = SHARE_DELAY_MINUTES,
                            selectedItem = viewModel.screenshotShareCleanupDelayMinutes.intValue,
                            onItemSelected = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.setScreenshotShareCleanupDelayMinutes(it)
                            },
                            labelProvider = { minutes ->
                                when (minutes) {
                                    0 -> context.getString(R.string.screenshot_delay_immediately)
                                    60 -> context.getString(R.string.screenshot_delay_1_hour)
                                    else -> context.getString(R.string.screenshot_delay_1_day)
                                }
                            },
                            title = R.string.screenshot_share_cleanup_delay_title,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        SectionTitle(R.string.screenshots_section_auto_clean)
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_timer_24,
                title = stringResource(R.string.screenshot_auto_clean_title),
                description = stringResource(R.string.screenshot_auto_clean_note),
                isChecked = viewModel.isScreenshotAutoCleanEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    val missing = missingPermissions(needsNotifications = true)
                    if (checked && missing.isNotEmpty()) {
                        requestingPermissionsFor = missing
                    } else {
                        viewModel.setScreenshotAutoCleanEnabled(checked)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "screenshot_auto_clean_enabled"),
            )

            AnimatedVisibility(
                visible = viewModel.isScreenshotAutoCleanEnabled.value,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                PickerRow(
                    title = R.string.screenshot_auto_clean_days_title,
                    highlighted = highlightSetting == "screenshot_auto_clean_days",
                ) {
                    SegmentedPicker(
                        items = AUTO_CLEAN_DAYS,
                        selectedItem = viewModel.screenshotAutoCleanDays.intValue,
                        onItemSelected = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.setScreenshotAutoCleanDays(it)
                        },
                        labelProvider = { days -> context.resources.getQuantityString(R.plurals.screenshot_days, days, days) },
                        title = R.string.screenshot_auto_clean_days_title,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        SectionTitle(R.string.screenshots_section_stats)
        RoundedCardContainer(
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_delete_24,
                title = stringResource(R.string.screenshots_cleaned_count_title),
                description = viewModel.screenshotsCleanedCount.intValue.toString(),
                showToggle = false,
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_save_24,
                title = stringResource(R.string.screenshots_storage_saved_title),
                description = Formatter.formatShortFileSize(context, viewModel.screenshotsCleanedBytes.longValue),
                showToggle = false,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun PickerRow(
    title: Int,
    highlighted: Boolean,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                .padding(top = 12.dp)
                .highlight(highlighted),
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        content()
    }
}
