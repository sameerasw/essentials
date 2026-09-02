/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: WearOS Companion
 * File: ComplicationsSettingsUI.kt
 * Description: Composable settings screen for configuring WearOS complication preferences.
 */

package com.sameerasw.essentials.ui.features.watch

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.DeviceInfoSyncManager
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplicationsSettingsUI(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
    }

    var glanceBatteryAlerts by remember {
        mutableStateOf(prefs.getBoolean("watchface_glance_battery_alerts", true))
    }
    var glanceFlashlight by remember {
        mutableStateOf(prefs.getBoolean("watchface_glance_flashlight", true))
    }
    var glanceTravel by remember {
        mutableStateOf(prefs.getBoolean("watchface_glance_travel", true))
    }
    var glanceEvents by remember {
        mutableStateOf(prefs.getBoolean("watchface_glance_events", true))
    }
    var glanceAlarm by remember {
        mutableStateOf(prefs.getBoolean("watchface_glance_alarm", true))
    }
    var showGlow by remember {
        mutableStateOf(prefs.getBoolean("watchface_show_glow", true))
    }
    var showGlanceComplicationsSheet by remember {
        mutableStateOf(false)
    }

    if (showGlanceComplicationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showGlanceComplicationsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.watchface_glance_complications_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
                RoundedCardContainer(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                ) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_battery_alert_24,
                        title = stringResource(R.string.watchface_glance_comp_battery_alerts_title),
                        description = stringResource(R.string.watchface_glance_comp_battery_alerts_desc),
                        isChecked = glanceBatteryAlerts,
                        onCheckedChange = {
                            glanceBatteryAlerts = it
                            prefs.edit().putBoolean("watchface_glance_battery_alerts", it).apply()
                            DeviceInfoSyncManager.forceSync(context)
                        },
                    )
                    IconToggleItem(
                        iconRes = R.drawable.rounded_flashlight_on_24,
                        title = stringResource(R.string.watchface_glance_comp_flashlight_title),
                        description = stringResource(R.string.watchface_glance_comp_flashlight_desc),
                        isChecked = glanceFlashlight,
                        onCheckedChange = {
                            glanceFlashlight = it
                            prefs.edit().putBoolean("watchface_glance_flashlight", it).apply()
                            DeviceInfoSyncManager.forceSync(context)
                        },
                    )
                    IconToggleItem(
                        iconRes = R.drawable.rounded_distance_24,
                        title = stringResource(R.string.watchface_glance_comp_travel_title),
                        description = stringResource(R.string.watchface_glance_comp_travel_desc),
                        isChecked = glanceTravel,
                        onCheckedChange = {
                            glanceTravel = it
                            prefs.edit().putBoolean("watchface_glance_travel", it).apply()
                            DeviceInfoSyncManager.forceSync(context)
                        },
                    )
                    IconToggleItem(
                        iconRes = R.drawable.rounded_calendar_today_24,
                        title = stringResource(R.string.watchface_glance_comp_events_title),
                        description = stringResource(R.string.watchface_glance_comp_events_desc),
                        isChecked = glanceEvents,
                        onCheckedChange = {
                            glanceEvents = it
                            prefs.edit().putBoolean("watchface_glance_events", it).apply()
                            DeviceInfoSyncManager.forceSync(context)
                        },
                    )
                    IconToggleItem(
                        iconRes = R.drawable.rounded_alarm_24,
                        title = stringResource(R.string.watchface_glance_comp_alarm_title),
                        description = stringResource(R.string.watchface_glance_comp_alarm_desc),
                        isChecked = glanceAlarm,
                        onCheckedChange = {
                            glanceAlarm = it
                            prefs.edit().putBoolean("watchface_glance_alarm", it).apply()
                            DeviceInfoSyncManager.forceSync(context)
                        },
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.glance_complication_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_widgets_24,
                title = stringResource(R.string.watchface_glance_complications_title),
                description = stringResource(R.string.watchface_glance_complications_desc),
                showToggle = false,
                onClick = {
                    showGlanceComplicationsSheet = true
                },
            )
            IconToggleItem(
                iconRes = R.drawable.rounded_blur_on_24,
                title = stringResource(R.string.watchface_show_glow_title),
                description = stringResource(R.string.watchface_show_glow_desc),
                isChecked = showGlow,
                onCheckedChange = {
                    showGlow = it
                    prefs.edit().putBoolean("watchface_show_glow", it).apply()
                    DeviceInfoSyncManager.forceSync(context)
                },
            )
        }
    }
}
