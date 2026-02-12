package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.watermark.WatermarkOptions
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil.performUIHaptic
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWatermarkSheet(
    options: WatermarkOptions,
    currentBrand: String?,
    currentCustom: String,
    currentDate: String?,
    formatDate: (String) -> String,
    onDismissRequest: () -> Unit,
    onSaveClick: (showBrand: Boolean, brand: String, custom: String, date: String?) -> Unit
) {
    val view = LocalView.current
    var draftBrand by remember { mutableStateOf(currentBrand ?: "") }
    var draftCustom by remember { mutableStateOf(currentCustom) }
    var draftDate by remember { mutableStateOf(currentDate) }
    var showBrandToggle by remember { mutableStateOf(options.showDeviceBrand) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val displayDate =
        draftDate?.let { formatDate(it) } ?: stringResource(R.string.watermark_no_date)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.watermark_edit_texts),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            RoundedCardContainer {
                Column(
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(4.dp)
                    )
                ) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_mobile_text_2_24,
                        title = stringResource(R.string.watermark_show_brand),
                        isChecked = showBrandToggle,
                        onCheckedChange = {
                            performUIHaptic(view)
                            showBrandToggle = it
                        }
                    )

                    androidx.compose.animation.AnimatedVisibility(visible = showBrandToggle) {
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            OutlinedTextField(
                                value = draftBrand,
                                onValueChange = { draftBrand = it },
                                label = { Text(stringResource(R.string.watermark_device_brand)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                singleLine = true
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceBright,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = draftCustom,
                        onValueChange = { draftCustom = it },
                        label = { Text(stringResource(R.string.watermark_custom_text)) },
                        placeholder = { Text(stringResource(R.string.watermark_custom_text_hint)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceBright,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable {
                            performUIHaptic(view)
                            showDatePicker = true
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.size(2.dp))
                    Icon(
                        painter = painterResource(R.drawable.rounded_date_range_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.watermark_date_time),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.rounded_chevron_right_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        performUIHaptic(view)
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        performUIHaptic(view)
                        onSaveClick(showBrandToggle, draftBrand, draftCustom, draftDate)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.action_save_changes))
                }
            }
        }
    }

    if (showDatePicker) {
        val initialDateMillis = remember(draftDate) {
            try {
                val sdf = SimpleDateFormat("yyyy:MM:dd", Locale.US)
                sdf.parse(draftDate?.split(" ")?.getOrNull(0) ?: "")?.time
            } catch (e: Exception) {
                null
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
                        val sdf = SimpleDateFormat("yyyy:MM:dd", Locale.US)
                        val datePart = sdf.format(calendar.time)
                        val timePart = draftDate?.split(" ")?.getOrNull(1) ?: "00:00:00"
                        draftDate = "$datePart $timePart"
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text(stringResource(R.string.action_next)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val (initialHour, initialMinute) = remember(draftDate) {
            try {
                val timePart = draftDate?.split(" ")?.getOrNull(1) ?: "00:00:00"
                val parts = timePart.split(":")
                parts.getOrNull(0)?.toInt()!! to parts.getOrNull(1)?.toInt()!!
            } catch (e: Exception) {
                0 to 0
            }
        }
        val timePickerState =
            rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val timePart =
                        String.format("%02d:%02d:00", timePickerState.hour, timePickerState.minute)
                    val datePart = draftDate?.split(" ")?.getOrNull(0) ?: "2024:01:01"
                    draftDate = "$datePart $timePart"
                    showTimePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}
