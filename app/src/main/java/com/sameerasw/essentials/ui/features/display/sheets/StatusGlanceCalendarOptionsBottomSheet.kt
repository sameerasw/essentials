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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.island.plugins.calendar.CalendarIcons
import com.sameerasw.essentials.island.plugins.calendar.rememberCalendarIconRes
import com.sameerasw.essentials.ui.core.pickers.IconGridPicker
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusGlanceCalendarOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
    allowIconEdit: Boolean = false,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()
    var editingCalendar by remember { mutableStateOf<MainViewModel.CalendarAccount?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchStatusGlanceCalendars(context)
        if (allowIconEdit) viewModel.loadIslandCalendarIcons()
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
    val timeframeMinutes = when (currentCode) {
        "15m" -> 15
        "30m" -> 30
        "1h" -> 60
        "2h" -> 120
        "6h" -> 360
        "24h" -> 1440
        else -> Int.MAX_VALUE
    }
    val settings = remember { SettingsRepository(context) }
    var priorityMinutes by remember { mutableIntStateOf(settings.getIslandCalendarPriorityMinutes()) }
    val priorityOptions = listOf(0, 5, 10, 15, 30, 60, 120).filter { it < timeframeMinutes }
    val disabledLabel = stringResource(R.string.island_calendar_priority_disabled)
    fun priorityLabel(minutes: Int): String = when {
        minutes <= 0 -> disabledLabel
        minutes < 60 -> context.getString(R.string.island_calendar_priority_minutes, minutes)
        else -> context.getString(R.string.island_calendar_priority_hours, minutes / 60)
    }
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

            if (allowIconEdit) {
                RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                    IslandPrefToggle(
                        settingKey = SettingsRepository.KEY_ISLAND_CALENDAR_COMPACT_TIME,
                        iconRes = R.drawable.rounded_schedule_24,
                        title = stringResource(R.string.island_calendar_compact_time_title),
                    )
                }
                IslandLauncherOnlyToggle(SettingsRepository.KEY_ISLAND_CALENDAR_LAUNCHER_ONLY)
                if (rememberIslandShowsWhileLocked()) {
                    RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                        IslandPrefToggle(
                            settingKey = SettingsRepository.KEY_ISLAND_CALENDAR_HIDE_LOCKED,
                            iconRes = R.drawable.rounded_mobile_lock_portrait_24,
                            title = stringResource(R.string.island_calendar_hide_locked_title),
                        )
                    }
                }
            }

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

                if (allowIconEdit) {
                    ConfigPickerItem(
                        title = stringResource(R.string.island_calendar_priority_title),
                        selectedValue = priorityLabel(priorityMinutes),
                        iconRes = R.drawable.rounded_timer_24,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        priorityOptions.forEach { minutes ->
                            SegmentedDropdownMenuItem(
                                text = { Text(priorityLabel(minutes)) },
                                onClick = {
                                    priorityMinutes = minutes
                                    settings.setIslandCalendarPriorityMinutes(minutes)
                                },
                            )
                        }
                    }
                }

                IconToggleItem(
                    title = stringResource(R.string.status_glance_calendar_show_all_day_title),
                    iconRes = R.drawable.rounded_calendar_today_24,
                    isChecked = viewModel.statusGlanceCalendarShowAllDay.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setStatusGlanceCalendarShowAllDay(checked)
                    },
                )

                if (allowIconEdit) {
                    IslandPrefToggle(
                        settingKey = SettingsRepository.KEY_ISLAND_CALENDAR_SHOW_ONGOING,
                        iconRes = R.drawable.rounded_calendar_today_24,
                        title = stringResource(R.string.island_calendar_show_ongoing_title),
                    )
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
                            val icon = if (allowIconEdit) viewModel.islandCalendarIcons.value[calendar.id] else null
                            IconToggleItem(
                                title = calendar.name,
                                iconRes = rememberCalendarIconRes(icon),
                                isChecked = calendar.isSelected,
                                onCheckedChange = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.toggleStatusGlanceCalendarSelection(calendar.id)
                                },
                                onSettingsClick = if (allowIconEdit) ({ editingCalendar = calendar }) else null,
                                settingsIconRes = R.drawable.rounded_edit_24,
                            )
                        }
                    }
                }
            }
        }
    }

    editingCalendar?.let { calendar ->
        CalendarIconSheet(
            calendarName = calendar.name,
            initial = viewModel.islandCalendarIcons.value[calendar.id],
            onSave = { icon -> viewModel.setIslandCalendarIcon(calendar.id, icon) },
            onDismissRequest = { editingCalendar = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarIconSheet(
    calendarName: String,
    initial: String?,
    onSave: (String?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var selected by remember { mutableStateOf(initial) }

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = calendarName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
            IconGridPicker(
                categories = CalendarIcons.categories,
                selectedIconName = selected ?: CalendarIcons.default,
                onIconSelected = { icon ->
                    val value = icon.takeIf { it != CalendarIcons.default }
                    selected = value
                    onSave(value)
                },
                contentPadding = PaddingValues(bottom = 32.dp),
            )
        }
    }
}
