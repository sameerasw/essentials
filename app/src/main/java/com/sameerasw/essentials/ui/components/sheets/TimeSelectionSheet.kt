package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.ui.components.pickers.MultiSegmentedPicker
import com.sameerasw.essentials.utils.HapticUtil
import java.util.Calendar
import com.sameerasw.essentials.domain.diy.State as DIYState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSelectionSheet(
    initialTrigger: Trigger.Schedule? = null,
    initialState: DIYState.TimePeriod? = null,
    onDismiss: () -> Unit,
    onSaveTrigger: (Trigger.Schedule) -> Unit = {},
    onSaveState: (DIYState.TimePeriod) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    val isRange = initialState != null
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)

    val startPickerState = rememberTimePickerState(
        initialHour = initialTrigger?.hour ?: initialState?.startHour ?: 0,
        initialMinute = initialTrigger?.minute ?: initialState?.startMinute ?: 0,
        is24Hour = is24Hour
    )
    val endPickerState = rememberTimePickerState(
        initialHour = initialState?.endHour ?: 0,
        initialMinute = initialState?.endMinute ?: 0,
        is24Hour = is24Hour
    )
    var selectedDays by remember {
        mutableStateOf(
            initialTrigger?.days ?: initialState?.days ?: emptySet<Int>()
        )
    }

    var showingEndPicker by remember { mutableStateOf(false) }

    LaunchedEffect(startPickerState.hour, startPickerState.minute) {
        HapticUtil.performSliderHaptic(view)
    }
    LaunchedEffect(endPickerState.hour, endPickerState.minute) {
        if (showingEndPicker) HapticUtil.performSliderHaptic(view)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(if (isRange) R.string.diy_time_range_selection_title else R.string.diy_time_selection_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )

            if (isRange) {
                // Range display with Start/End tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeDisplayCard(
                        label = stringResource(R.string.diy_start_time_label),
                        hour = startPickerState.hour,
                        minute = startPickerState.minute,
                        isSelected = !showingEndPicker,
                        modifier = Modifier.weight(1f),
                        onClick = { showingEndPicker = false }
                    )
                    TimeDisplayCard(
                        label = stringResource(R.string.diy_end_time_label),
                        hour = endPickerState.hour,
                        minute = endPickerState.minute,
                        isSelected = showingEndPicker,
                        modifier = Modifier.weight(1f),
                        onClick = { showingEndPicker = true }
                    )
                }
            }

            // Time Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(
                    state = if (showingEndPicker) endPickerState else startPickerState
                )
            }

            // Days Selection
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.diy_repeat_days_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val days = listOf(
                    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                    Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
                )
                val dayLabels = mapOf(
                    Calendar.SUNDAY to "S",
                    Calendar.MONDAY to "M",
                    Calendar.TUESDAY to "T",
                    Calendar.WEDNESDAY to "W",
                    Calendar.THURSDAY to "T",
                    Calendar.FRIDAY to "F",
                    Calendar.SATURDAY to "S"
                )

                MultiSegmentedPicker(
                    items = days,
                    selectedItems = selectedDays,
                    onItemsSelected = { selectedDays = it },
                    labelProvider = { dayLabels[it]!! },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        if (isRange) {
                            onSaveState(
                                DIYState.TimePeriod(
                                    startHour = startPickerState.hour,
                                    startMinute = startPickerState.minute,
                                    endHour = endPickerState.hour,
                                    endMinute = endPickerState.minute,
                                    days = selectedDays
                                )
                            )
                        } else {
                            onSaveTrigger(
                                Trigger.Schedule(
                                    hour = startPickerState.hour,
                                    minute = startPickerState.minute,
                                    days = selectedDays
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun TimeDisplayCard(
    label: String,
    hour: Int,
    minute: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current

    val formattedTime = remember(hour, minute) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        android.text.format.DateFormat.getTimeFormat(context).format(calendar.time)
    }

    Surface(
        onClick = {
            HapticUtil.performUIHaptic(view)
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceBright,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
