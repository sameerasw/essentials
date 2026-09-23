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
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sameerasw.essentials.domain.diy.State as DIYState

private class CalendarEntry(val id: Long, val name: String, val account: String)

@OptIn(ExperimentalMaterial3Api::class)
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
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }
    var calendars by remember { mutableStateOf<List<CalendarEntry>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        } else {
            calendars = withContext(Dispatchers.IO) { loadCalendars(context) }
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
            Text(
                text = stringResource(R.string.diy_calendar_state_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
            Text(
                text = stringResource(R.string.diy_calendar_state_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )

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
                Text(
                    text = stringResource(R.string.diy_calendar_permission),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            } else if (calendars.isEmpty()) {
                Text(
                    text = stringResource(R.string.status_glance_calendar_no_calendars),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            } else {
                calendars.groupBy { it.account }.forEach { (account, entries) ->
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

private fun loadCalendars(context: Context): List<CalendarEntry> {
    val projection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.ACCOUNT_NAME,
    )
    val result = mutableListOf<CalendarEntry>()
    try {
        context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                result += CalendarEntry(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty())
            }
        }
    } catch (_: Exception) {
    }
    return result
}
