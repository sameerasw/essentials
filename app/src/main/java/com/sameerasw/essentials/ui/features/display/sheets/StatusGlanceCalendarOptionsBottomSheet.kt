/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: StatusGlanceCalendarOptionsBottomSheet.kt
 * Description: Bottom sheet for configuring Status Glance upcoming calendar event timeframe and selected calendars.
 */

package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusGlanceCalendarOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.fetchStatusGlanceCalendars(context)
    }

    val timeframes = listOf(
        "15m" to stringResource(R.string.status_glance_calendar_timeframe_15_mins),
        "30m" to stringResource(R.string.status_glance_calendar_timeframe_30_mins),
        "1h" to stringResource(R.string.status_glance_calendar_timeframe_1_hour),
        "2h" to stringResource(R.string.status_glance_calendar_timeframe_2_hours),
        "6h" to stringResource(R.string.status_glance_calendar_timeframe_6_hours),
        "24h" to stringResource(R.string.status_glance_calendar_timeframe_24_hours),
        "today" to stringResource(R.string.status_glance_calendar_timeframe_today),
        "next" to stringResource(R.string.status_glance_calendar_timeframe_next),
    )

    val currentCode = viewModel.statusGlanceCalendarTimeframe.value
    val currentLabel = timeframes.firstOrNull { it.first == currentCode }?.second
        ?: stringResource(R.string.status_glance_calendar_timeframe_today)

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.status_glance_calendar_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer {
                ConfigPickerItem(
                    title = stringResource(R.string.status_glance_calendar_timeframe_title),
                    selectedValue = currentLabel,
                    iconRes = R.drawable.rounded_schedule_24,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    timeframes.forEach { (code, label) ->
                        SegmentedDropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setStatusGlanceCalendarTimeframe(code)
                            },
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.status_glance_calendar_select_calendars_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )

            val calendars = viewModel.statusGlanceAvailableCalendars
            if (calendars.isEmpty()) {
                Text(
                    text = stringResource(R.string.status_glance_calendar_no_calendars),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val grouped = calendars.groupBy { it.accountName }
                grouped.forEach { (accountName, accountCalendars) ->
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )

                    RoundedCardContainer(
                        spacing = 2.dp,
                        cornerRadius = 24.dp,
                    ) {
                        accountCalendars.forEach { calendar ->
                            IconToggleItem(
                                title = calendar.name,
                                iconRes = R.drawable.rounded_calendar_today_24,
                                isChecked = calendar.isSelected,
                                onCheckedChange = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.toggleStatusGlanceCalendarSelection(calendar.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
