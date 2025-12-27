package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.pickers.HapticFeedbackPicker
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.utils.HapticFeedbackType
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonRemapSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var showFlashlightOptions by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Toggle
        RoundedCardContainer(spacing = 0.dp) {
            IconToggleItem(
                iconRes = R.drawable.rounded_switch_access_3_24,
                title = "Enable Button Remap",
                description = "Remap volume buttons when screen is off",
                isChecked = viewModel.isButtonRemapEnabled.value,
                onCheckedChange = { viewModel.setButtonRemapEnabled(it, context) }
            )
        }

        AnimatedVisibility(
            visible = viewModel.isButtonRemapEnabled.value,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Haptic Feedback (Common)
                Text(
                    text = "Haptic Feedback",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RoundedCardContainer(spacing = 0.dp) {
                    HapticFeedbackPicker(
                        selectedFeedback = viewModel.remapHapticType.value,
                        onFeedbackSelected = { viewModel.setRemapHapticType(it, context) },
                    options = listOf(
                        "None" to HapticFeedbackType.NONE,
                        "Tick" to HapticFeedbackType.TICK,
                        "Double" to HapticFeedbackType.DOUBLE
                    )
                    )
                }

                Text(
                    text = "Buttons",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Tab Picker
                RoundedCardContainer(spacing = 2.dp) {
                    SegmentedPicker(
                        items = listOf("Volume Up", "Volume Down"),
                        selectedItem = if (selectedTab == 0) "Volume Up" else "Volume Down",
                        onItemSelected = { selectedTab = if (it == "Volume Up") 0 else 1 },
                        labelProvider = { it }
                    )

                    val currentAction = if (selectedTab == 0) viewModel.volumeUpAction.value else viewModel.volumeDownAction.value
                    val onActionSelected: (String) -> Unit = { action ->
                        if (selectedTab == 0) viewModel.setVolumeUpAction(action, context)
                        else viewModel.setVolumeDownAction(action, context)
                    }

                        RemapActionItem(
                            title = "None",
                            isSelected = currentAction == "None",
                            onClick = { onActionSelected("None") },
                            iconRes = R.drawable.rounded_do_not_disturb_on_24,
                            modifier = Modifier,
                        )
                        RemapActionItem(
                            title = "Toggle flashlight",
                            isSelected = currentAction == "Toggle flashlight",
                            onClick = { onActionSelected("Toggle flashlight") },
                            hasSettings = true,
                            onSettingsClick = { showFlashlightOptions = true },
                            iconRes = R.drawable.rounded_flashlight_on_24,
                            modifier = Modifier
                        )
                        RemapActionItem(
                            title = "Media play/pause",
                            isSelected = currentAction == "Media play/pause",
                            onClick = { onActionSelected("Media play/pause") },
                            iconRes = R.drawable.rounded_play_pause_24,
                            modifier = Modifier
                        )
                        RemapActionItem(
                            title = "Media next",
                            isSelected = currentAction == "Media next",
                            onClick = { onActionSelected("Media next") },
                            iconRes = R.drawable.rounded_skip_next_24,
                            modifier = Modifier
                        )
                        RemapActionItem(
                            title = "Media previous",
                            isSelected = currentAction == "Media previous",
                            onClick = { onActionSelected("Media previous") },
                            iconRes = R.drawable.rounded_skip_previous_24,
                            modifier = Modifier
                        )
                        RemapActionItem(
                            title = "Toggle vibrate",
                            isSelected = currentAction == "Toggle vibrate",
                            onClick = { onActionSelected("Toggle vibrate") },
                            iconRes = R.drawable.rounded_mobile_vibrate_24,
                            modifier = Modifier
                        )
                        RemapActionItem(
                            title = "Toggle mute",
                            isSelected = currentAction == "Toggle mute",
                            onClick = { onActionSelected("Toggle mute") },
                            iconRes = R.drawable.rounded_volume_off_24,
                            modifier = Modifier
                        )
                        RemapActionItem(
                            title = "AI assistant",
                            isSelected = currentAction == "AI assistant",
                            onClick = { onActionSelected("AI assistant") },
                            iconRes = R.drawable.rounded_bubble_chart_24,
                            modifier = Modifier
                        )
                }

            }
        }

        // Hint
        RoundedCardContainer {
            Text(
                text = "When the screen is off, long-press the selected button to trigger its assigned action.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Flashlight Options Bottom Sheet
    if (showFlashlightOptions) {
        ModalBottomSheet(
            onDismissRequest = { showFlashlightOptions = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Flashlight Options",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                RoundedCardContainer(spacing = 0.dp) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_flashlight_on_24,
                        title = "Always turn off flashlight",
                        description = "Even while display is on",
                        isChecked = viewModel.isFlashlightAlwaysTurnOffEnabled.value,
                        onCheckedChange = { viewModel.setFlashlightAlwaysTurnOffEnabled(it, context) }
                    )
                }
                
                Button(
                    onClick = { showFlashlightOptions = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
fun RemapActionItem(
    title: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasSettings: Boolean = false,
    onSettingsClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceBright,
                shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )

        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hasSettings && isSelected) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_settings_24),
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
