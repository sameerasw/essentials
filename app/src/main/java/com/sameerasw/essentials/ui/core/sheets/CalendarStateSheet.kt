/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core UI Sheets
 * File: CalendarStateSheet.kt
 * Description: Configures the calendar event DIY state: calendars and event filters.
 */

package com.sameerasw.essentials.ui.core.sheets

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.CalendarInfo
import com.sameerasw.essentials.data.repository.CalendarRepository
import com.sameerasw.essentials.domain.model.AppPermission
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sameerasw.essentials.domain.diy.State as DIYState


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarStateSheet(
    initialState: DIYState.CalendarEvent?,
    onDismiss: () -> Unit,
    onStateChange: (DIYState.CalendarEvent) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var state by remember { mutableStateOf(initialState ?: DIYState.CalendarEvent()) }
    var hasPermission by remember {
        mutableStateOf(CalendarRepository.hasPermission(context))
    }
    var calendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
    var askedOnce by remember { mutableStateOf(false) }
    var workingLocations by remember { mutableStateOf<List<String>>(emptyList()) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            askedOnce = true
            hasPermission = granted
            if (granted) CalendarRepository.onPermissionChanged(context)
        }

    fun requestPermission() {
        // Once Android stops showing the prompt, the app settings page is the only way left to grant it
        if (askedOnce) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    LifecycleResumeEffect(Unit) {
        val granted = CalendarRepository.hasPermission(context)
        if (granted && !hasPermission) CalendarRepository.onPermissionChanged(context)
        hasPermission = granted
        onPauseOrDispose {}
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) requestPermission()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) calendars = withContext(Dispatchers.IO) { CalendarRepository.queryCalendars(context) }
    }

    LaunchedEffect(hasPermission, state.workingLocationOnly) {
        if (hasPermission && state.workingLocationOnly && workingLocations.isEmpty()) {
            workingLocations = withContext(Dispatchers.IO) { CalendarRepository.workingLocationNames(context) }
        }
    }

    fun update(next: DIYState.CalendarEvent) {
        HapticUtil.performVirtualKeyHaptic(view)
        state = next
        onStateChange(next)
    }

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TranslatableText(
                stringResId = R.string.diy_calendar_state_title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
            TranslatableText(
                stringResId = R.string.diy_calendar_state_desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )

            SectionLabel(stringResource(R.string.diy_calendar_match))
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                            .padding(12.dp),
                ) {
                    OutlinedTextField(
                        value = state.titleContains,
                        onValueChange = {
                            state = state.copy(titleContains = it)
                            onStateChange(state)
                        },
                        placeholder = {
                            TranslatableText(
                                if (state.workingLocationOnly) R.string.diy_calendar_working_location_name else R.string.diy_calendar_title_contains,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ),
                    )
                }
                IconToggleItem(
                    iconRes = R.drawable.rounded_work_history_24,
                    title = stringResource(R.string.diy_calendar_working_location),
                    isChecked = state.workingLocationOnly,
                    onCheckedChange = { update(state.copy(workingLocationOnly = it)) },
                )
                if (state.workingLocationOnly && workingLocations.isNotEmpty()) {
                    FlowRow(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        workingLocations.forEach { location ->
                            FilterChip(
                                selected = state.titleContains.trim().equals(location, ignoreCase = true),
                                onClick = { update(state.copy(titleContains = location)) },
                                label = { Text(location) },
                            )
                        }
                    }
                }
            }

            SectionLabel(stringResource(R.string.diy_calendar_filters))
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_calendar_today_24,
                    title = stringResource(R.string.diy_calendar_include_all_day),
                    isChecked = state.includeAllDay,
                    onCheckedChange = { update(state.copy(includeAllDay = it)) },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_close_24,
                    title = stringResource(R.string.diy_calendar_include_declined),
                    isChecked = state.includeDeclined,
                    onCheckedChange = { update(state.copy(includeDeclined = it)) },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_help_24,
                    title = stringResource(R.string.diy_calendar_include_tentative),
                    isChecked = state.includeTentative,
                    onCheckedChange = { update(state.copy(includeTentative = it)) },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_check_circle_24,
                    title = stringResource(R.string.diy_calendar_include_free),
                    isChecked = state.includeFree,
                    onCheckedChange = { update(state.copy(includeFree = it)) },
                )
            }

            SectionLabel(stringResource(R.string.diy_calendar_calendars))
            if (!hasPermission) {
                RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                    IconToggleItem(
                        iconRes = AppPermission.READ_CALENDAR.iconRes,
                        title = stringResource(AppPermission.READ_CALENDAR.titleRes),
                        showToggle = false,
                        onClick = { requestPermission() },
                        trailingContent = {
                            FilledTonalButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    requestPermission()
                                },
                            ) {
                                Text(stringResource(R.string.widget_stack_grant))
                            }
                        },
                    )
                }
            } else if (calendars.isEmpty()) {
                TranslatableText(
                    stringResId = R.string.status_glance_calendar_no_calendars,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            } else {
                calendars.groupBy { it.accountName }.forEach { (account, entries) ->
                    Text(
                        text = account,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                    RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                        entries.forEach { calendar ->
                            IconToggleItem(
                                iconRes = R.drawable.rounded_calendar_today_24,
                                title = calendar.name,
                                isChecked = calendar.id in state.calendarIds,
                                onCheckedChange = { checked ->
                                    val ids = if (checked) state.calendarIds + calendar.id else state.calendarIds - calendar.id
                                    update(state.copy(calendarIds = ids))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp),
    )
}

