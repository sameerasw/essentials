package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.LocationIconPicker
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.LocationReachedViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LocationReachedBottomSheet(
    viewModel: LocationReachedViewModel,
    onDismissRequest: () -> Unit
) {
    val tempAlarm by viewModel.tempAlarm
    val currentAlarm = tempAlarm
    val view = androidx.compose.ui.platform.LocalView.current

    val isProcessing by viewModel.isProcessingCoordinates

    if (currentAlarm == null && !isProcessing) return

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (currentAlarm != null && viewModel.savedAlarms.value.any { it.id == currentAlarm.id })
                    stringResource(R.string.location_reached_edit_title)
                else stringResource(R.string.location_reached_add_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isProcessing && currentAlarm == null) {
                RoundedCardContainer(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LoadingIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.location_reached_resolving),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (currentAlarm != null) {
                RoundedCardContainer(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = currentAlarm.name,
                            onValueChange = { viewModel.setTempAlarm(currentAlarm.copy(name = it)) },
                            label = { Text(stringResource(R.string.location_reached_name_label)) },
                            placeholder = { Text(stringResource(R.string.location_reached_name_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.large
                        )

                        LocationIconPicker(
                            selectedIconName = currentAlarm.iconResName,
                            onIconSelected = {
                                viewModel.setTempAlarm(currentAlarm.copy(iconResName = it))
                            }
                        )

                        // Coordinates Display
                        Column {
                            Text(
                                text = "Coordinates",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%.5f, %.5f".format(
                                    currentAlarm.latitude,
                                    currentAlarm.longitude
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Radius Slider
                        Column {
                            Text(
                                text = stringResource(
                                    R.string.location_reached_radius_label,
                                    currentAlarm.radius
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = currentAlarm.radius.toFloat(),
                                onValueChange = {
                                    if (it.toInt() != currentAlarm.radius) {
                                        com.sameerasw.essentials.utils.HapticUtil.performSliderHaptic(
                                            view
                                        )
                                    }
                                    viewModel.setTempAlarm(currentAlarm.copy(radius = it.toInt()))
                                },
                                valueRange = 100f..5000f,
                                steps = 49
                            )
                        }
                    }
                }
            }

            val isEditing =
                currentAlarm != null && viewModel.savedAlarms.value.any { it.id == currentAlarm.id }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEditing) {
                    IconButton(
                        onClick = {
                            com.sameerasw.essentials.utils.HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.deleteAlarm(currentAlarm.id)
                            onDismissRequest()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.size(56.dp) // Slightly larger for better touch target
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_delete_24),
                            contentDescription = stringResource(R.string.action_delete)
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        com.sameerasw.essentials.utils.HapticUtil.performVirtualKeyHaptic(view)
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text(stringResource(R.string.location_reached_cancel_btn))
                }

                Button(
                    onClick = {
                        val alarm = tempAlarm
                        if (alarm != null) {
                            com.sameerasw.essentials.utils.HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.saveAlarm(alarm)
                        }
                    },
                    enabled = currentAlarm != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text(stringResource(R.string.location_reached_save_btn))
                }
            }
        }
    }
}
