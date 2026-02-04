package com.sameerasw.essentials.ui.composables.configs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSyncSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightKey: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val calendars = viewModel.availableCalendars
    val isPeriodicEnabled by viewModel.isCalendarSyncPeriodicEnabled
    val isEnabled by viewModel.isCalendarSyncEnabled

    LaunchedEffect(Unit) {
        viewModel.fetchCalendars(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.calendar_sync_settings_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_sync_24,
                title = stringResource(R.string.calendar_sync_periodic_title),
                description = stringResource(R.string.calendar_sync_periodic_desc),
                isChecked = isPeriodicEnabled,
                onCheckedChange = { viewModel.setCalendarSyncPeriodicEnabled(it, context) },
                enabled = isEnabled,
                modifier = Modifier.highlight(highlightKey == "periodic_sync")
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_sync_24,
                title = stringResource(R.string.calendar_sync_sync_now),
                description = stringResource(R.string.calendar_sync_manual_sync_desc),
                isChecked = false,
                onCheckedChange = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.triggerCalendarSyncNow(context)
                    Toast.makeText(context, R.string.calendar_sync_sync_started, Toast.LENGTH_SHORT).show()
                },
                enabled = isEnabled,
                showToggle = false,
                modifier = Modifier.highlight(highlightKey == "sync_now")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.calendar_sync_select_calendars),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (calendars.isEmpty()) {
            Text(
                text = stringResource(R.string.calendar_sync_no_calendars),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val groupedCalendars = calendars.groupBy { it.accountName }
            
            groupedCalendars.forEach { (accountName, accountCalendars) ->
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                
                RoundedCardContainer {
                    accountCalendars.forEach { calendar ->
                        CalendarSelectionItem(
                            calendar = calendar,
                            isEnabled = isEnabled,
                            onToggle = { viewModel.toggleCalendarSelection(calendar.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarSelectionItem(
    calendar: MainViewModel.CalendarAccount,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceBright)
            .clickable(enabled = isEnabled) {
                HapticUtil.performVirtualKeyHaptic(view)
                onToggle()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = calendar.isSelected,
            onCheckedChange = { 
                HapticUtil.performVirtualKeyHaptic(view)
                onToggle() 
            },
            enabled = isEnabled
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = calendar.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
            
        }
    }
}
