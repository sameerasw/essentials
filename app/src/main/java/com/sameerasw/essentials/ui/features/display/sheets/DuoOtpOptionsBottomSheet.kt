/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: DuoOtpOptionsBottomSheet.kt
 * Description: Bottom sheet for configuring Duo OTP quick glance actions, permission, instructions, and timeout.
 */

package com.sameerasw.essentials.ui.features.display.sheets

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuoOtpOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val isPermissionGranted = viewModel.isNotificationListenerEnabled.value
    val isSensitivePermissionGranted = viewModel.isSensitiveNotificationAccessGranted.value
    val isAdbFallbackDialogVisible = viewModel.isAdbFallbackDialogVisible.value
    var showAppSelectionSheet by remember { mutableStateOf(false) }

    if (showAppSelectionSheet) {
        AppSelectionSheet(
            title = stringResource(R.string.duo_otp_filter_select_apps_title),
            onDismissRequest = { showAppSelectionSheet = false },
            onLoadApps = { viewModel.loadDuoOtpSelectedApps(it) },
            onSaveApps = { ctx, apps ->
                viewModel.saveDuoOtpSelectedApps(ctx, apps)
            },
            onAppToggle = { ctx, pkg, enabled ->
                viewModel.updateDuoOtpAppSelection(ctx, pkg, enabled)
            },
            context = context,
        )
    }

    if (isAdbFallbackDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.isAdbFallbackDialogVisible.value = false },
            title = { Text(stringResource(R.string.duo_otp_adb_title)) },
            text = { Text(stringResource(R.string.duo_otp_adb_desc)) },
            confirmButton = {
                TextButton(onClick = { viewModel.isAdbFallbackDialogVisible.value = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ADB Command", "adb shell cmd appops set com.sameerasw.essentials RECEIVE_SENSITIVE_NOTIFICATIONS allow"))
                    Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(android.R.string.copy))
                }
            }
        )
    }

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.duo_otp_options_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = MaterialTheme.shapes.extraSmall,
                ) {
                    Text(
                        text = stringResource(R.string.label_beta),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            // Auto-paste & Auto-dismiss toggles
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_content_paste_24,
                    title = stringResource(R.string.duo_otp_auto_paste_title),
                    description = stringResource(R.string.duo_otp_auto_paste_desc),
                    isChecked = viewModel.isDuoOtpAutoPaste.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setDuoOtpAutoPaste(checked)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_notifications_off_24,
                    title = stringResource(R.string.duo_otp_auto_dismiss_title),
                    description = stringResource(R.string.duo_otp_auto_dismiss_desc),
                    isChecked = viewModel.isDuoOtpAutoDismiss.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setDuoOtpAutoDismiss(checked)
                    },
                )
            }

            // App filter toggle & selection
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_shield_lock_24,
                    title = stringResource(R.string.duo_otp_filter_title),
                    description = stringResource(R.string.duo_otp_filter_desc),
                    isChecked = viewModel.isDuoOtpAppFilterEnabled.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setDuoOtpAppFilterEnabled(checked)
                    },
                    onSettingsClick = {
                        showAppSelectionSheet = true
                    },
                )
                if (viewModel.isDuoOtpAppFilterEnabled.value) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_apps_24,
                        title = stringResource(R.string.duo_otp_filter_select_apps_title),
                        description = stringResource(R.string.duo_otp_filter_select_apps_desc),
                        showToggle = false,
                        onClick = {
                            showAppSelectionSheet = true
                        },
                    )
                }
            }

            // Expiry timeout slider
            RoundedCardContainer(
                spacing = 2.dp,
                cornerRadius = 24.dp,
            ) {
                ConfigSliderItem(
                    title = stringResource(R.string.duo_otp_expiry_title),
                    description = stringResource(R.string.duo_otp_expiry_desc),
                    value = viewModel.duoOtpExpirySeconds.intValue.toFloat(),
                    onValueChange = { seconds ->
                        viewModel.setDuoOtpExpirySeconds(seconds.toInt())
                    },
                    valueRange = 10f..120f,
                    steps = 21,
                    increment = 5f,
                    iconRes = R.drawable.rounded_timer_24,
                    valueFormatter = { "${it.toInt()}s" },
                )
            }

                    }
    }
}
