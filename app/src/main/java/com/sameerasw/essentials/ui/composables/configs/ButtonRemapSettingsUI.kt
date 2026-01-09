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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.HapticFeedbackPicker
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.shizuku.ShizukuPermissionHelper
import com.sameerasw.essentials.services.InputEventListenerService
import com.sameerasw.essentials.shizuku.ShizukuStatus
import com.sameerasw.essentials.utils.ShizukuUtils
import android.content.Intent
import androidx.compose.foundation.layout.height
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonRemapSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    var selectedScreenTab by remember { mutableIntStateOf(0) } // 0: Off, 1: On
    var selectedButtonTab by remember { mutableIntStateOf(0) } // 0: Up, 1: Down
    var showFlashlightOptions by remember { mutableStateOf(false) }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    var shizukuStatus by remember { mutableStateOf(ShizukuStatus.NOT_RUNNING) }
    val shizukuHelper = remember { ShizukuPermissionHelper(context) }
    
    // Check Shizuku status on resume
     androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                 shizukuStatus = shizukuHelper.getStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Toggle
        AnimatedVisibility(
            visible = viewModel.isButtonRemapEnabled.value,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                iconRes = R.drawable.rounded_switch_access_3_24,
                title = "Enable Button Remap",
                isChecked = viewModel.isButtonRemapEnabled.value,
                onCheckedChange = { viewModel.setButtonRemapEnabled(it, context) },
                modifier = Modifier.highlight(highlightSetting == "enable_remap")
            )

                IconToggleItem(
                    iconRes = R.drawable.rounded_adb_24,
                    title = "Use Shizuku",
                    description = "Works with screen off (Recommended)",
                    isChecked = viewModel.isButtonRemapUseShizuku.value,
                    onCheckedChange = {  enabled ->
                        if (enabled) {
                            if (shizukuHelper.getStatus() == ShizukuStatus.READY) {
                                viewModel.setButtonRemapUseShizuku(true, context)
                                ContextCompat.startForegroundService(context, Intent(context, InputEventListenerService::class.java))
                            } else if (shizukuHelper.getStatus() == ShizukuStatus.PERMISSION_NEEDED) {
                                shizukuHelper.requestPermission { requestCode, grantResult ->
                                    if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        viewModel.setButtonRemapUseShizuku(true, context)
                                        ContextCompat.startForegroundService(context, Intent(context, InputEventListenerService::class.java))
                                    }
                                }
                            } else {
                                // Shizuku not running
                                viewModel.setButtonRemapUseShizuku(true, context)
                                android.widget.Toast.makeText(context, "Shizuku is not running", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            viewModel.setButtonRemapUseShizuku(false, context)
                            context.stopService(Intent(context, InputEventListenerService::class.java))
                        }
                    },
                    modifier = Modifier.highlight(highlightSetting == "shizuku_remap")
                )
                
                if (viewModel.isButtonRemapUseShizuku.value) {
                     // Status indicator
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val statusText = if (viewModel.shizukuDetectedDevicePath.value != null && shizukuStatus == ShizukuStatus.READY) {
                            "Detected ${viewModel.shizukuDetectedDevicePath.value}"
                        } else {
                            "Status: ${shizukuStatus.name}"
                        }

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (shizukuStatus == ShizukuStatus.READY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (shizukuStatus != ShizukuStatus.READY && shizukuStatus != ShizukuStatus.PERMISSION_NEEDED) {
                             Button(
                                onClick = { 
                                    try {
                                        val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                        if (intent != null) context.startActivity(intent)
                                    } catch(_: Exception) {}
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("Open Shizuku", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = viewModel.isButtonRemapEnabled.value,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Flashlight Options
                Text(
                    text = "Flashlight",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                RoundedCardContainer(spacing = 0.dp) {
                    FeatureCard(
                        title = "Flashlight options",
                        description = "Adjust fading and other settings",
                        iconRes = R.drawable.rounded_flashlight_on_24,
                        isEnabled = true,
                        showToggle = false,
                        hasMoreSettings = true,
                        onToggle = {},
                        onClick = { 
                            HapticUtil.performUIHaptic(view)
                            showFlashlightOptions = true 
                        },
                        modifier = Modifier.highlight(highlightSetting == "flashlight_options")
                    )
                }

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
                        ),
                        modifier = Modifier.highlight(highlightSetting == "remap_haptic")
                    )
                }

                Text(
                    text = "Remap Long Press",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Button Picker & Actions
                RoundedCardContainer(spacing = 2.dp) {
                    SegmentedPicker(
                        items = listOf("Screen Off", "Screen On"),
                        selectedItem = if (selectedScreenTab == 0) "Screen Off" else "Screen On",
                        onItemSelected = { 
                            HapticUtil.performUIHaptic(view)
                            selectedScreenTab = if (it == "Screen Off") 0 else 1 
                        },
                        labelProvider = { it }
                    )
                    SegmentedPicker(
                        items = listOf("Volume Up", "Volume Down"),
                        selectedItem = if (selectedButtonTab == 0) "Volume Up" else "Volume Down",
                        onItemSelected = { 
                            HapticUtil.performUIHaptic(view)
                            selectedButtonTab = if (it == "Volume Up") 0 else 1 
                        },
                        labelProvider = { it }
                    )

                    val currentAction = when (selectedScreenTab) {
                        0 if selectedButtonTab == 0 -> viewModel.volumeUpActionOff.value
                        0 if selectedButtonTab == 1 -> viewModel.volumeDownActionOff.value
                        1 if selectedButtonTab == 0 -> viewModel.volumeUpActionOn.value
                        else -> viewModel.volumeDownActionOn.value
                    }

                    val onActionSelected: (String) -> Unit = { action ->
                        when (selectedScreenTab) {
                            0 if selectedButtonTab == 0 -> viewModel.setVolumeUpActionOff(action, context)
                            0 if selectedButtonTab == 1 -> viewModel.setVolumeDownActionOff(action, context)
                            1 if selectedButtonTab == 0 -> viewModel.setVolumeUpActionOn(action, context)
                            else -> viewModel.setVolumeDownActionOn(action, context)
                        }
                    }

                    RemapActionItem(
                        title = "None",
                        isSelected = currentAction == "None",
                        onClick = { onActionSelected("None") },
                        iconRes = R.drawable.rounded_do_not_disturb_on_24,
                    )
                    RemapActionItem(
                        title = "Toggle flashlight",
                        isSelected = currentAction == "Toggle flashlight",
                        onClick = { onActionSelected("Toggle flashlight") },
                        hasSettings = true,
                        onSettingsClick = { showFlashlightOptions = true },
                        iconRes = R.drawable.rounded_flashlight_on_24,
                        modifier = Modifier.highlight(highlightSetting == "flashlight_toggle")
                    )
                    RemapActionItem(
                        title = "Media play/pause",
                        isSelected = currentAction == "Media play/pause",
                        onClick = { onActionSelected("Media play/pause") },
                        iconRes = R.drawable.rounded_play_pause_24,
                    )
                    RemapActionItem(
                        title = "Media next",
                        isSelected = currentAction == "Media next",
                        onClick = { onActionSelected("Media next") },
                        iconRes = R.drawable.rounded_skip_next_24,
                    )
                    RemapActionItem(
                        title = "Media previous",
                        isSelected = currentAction == "Media previous",
                        onClick = { onActionSelected("Media previous") },
                        iconRes = R.drawable.rounded_skip_previous_24,
                    )
                    RemapActionItem(
                        title = "Toggle vibrate",
                        isSelected = currentAction == "Toggle vibrate",
                        onClick = { onActionSelected("Toggle vibrate") },
                        iconRes = R.drawable.rounded_mobile_vibrate_24,
                    )
                    RemapActionItem(
                        title = "Toggle mute",
                        isSelected = currentAction == "Toggle mute",
                        onClick = { onActionSelected("Toggle mute") },
                        iconRes = R.drawable.rounded_volume_off_24,
                    )
                    RemapActionItem(
                        title = "AI assistant",
                        isSelected = currentAction == "AI assistant",
                        onClick = { onActionSelected("AI assistant") },
                        iconRes = R.drawable.rounded_bubble_chart_24,
                    )
                    if (selectedScreenTab == 1) {
                        RemapActionItem(
                            title = "Take screenshot",
                            isSelected = currentAction == "Take screenshot",
                            onClick = { onActionSelected("Take screenshot") },
                            iconRes = R.drawable.rounded_screenshot_region_24,
                        )
                    }
                }
            }
        }

        // Hint
        RoundedCardContainer {
            Text(
                text = if (selectedScreenTab == 0) 
                    "When the screen is off, long-press the selected button to trigger its assigned action. On Pixel devices, this action only gets triggered if the AOD is on due to system limitations."
                    else "When the screen is on, long-press the selected button to trigger its assigned action.",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Flashlight Intensity",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                RoundedCardContainer(spacing = 2.dp) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_blur_on_24,
                        title = "Fade in and out",
                        description = "Smoothly toggle flashlight",
                        isChecked = viewModel.isFlashlightFadeEnabled.value,
                        onCheckedChange = { viewModel.setFlashlightFadeEnabled(it, context) }
                    )
                    IconToggleItem(
                        iconRes = R.drawable.rounded_globe_24,
                        title = "Global controls",
                        description = "Fade-in flashlight globally",
                        isChecked = viewModel.isFlashlightGlobalEnabled.value,
                        onCheckedChange = { viewModel.setFlashlightGlobalEnabled(it, context) }
                    )

                    IconToggleItem(
                        iconRes = R.drawable.rounded_upcoming_24,
                        title = "Adjust intensity",
                        description = "Volume + - adjusts flashlight intensity",
                        isChecked = viewModel.isFlashlightAdjustEnabled.value,
                        onCheckedChange = { viewModel.setFlashlightAdjustEnabled(it, context) }
                    )
                    IconToggleItem(
                        iconRes = R.drawable.rounded_flashlight_on_24,
                        title = "Live update",
                        description = "Show brightness in status bar",
                        isChecked = viewModel.isFlashlightLiveUpdateEnabled.value,
                        onCheckedChange = { viewModel.setFlashlightLiveUpdateEnabled(it, context) }
                    )
                }


                Text(
                    text = "Other",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                
                RoundedCardContainer(spacing = 2.dp) {

                    IconToggleItem(
                        iconRes = R.drawable.rounded_flashlight_on_24,
                        title = "Always turn off flashlight",
                        description = "Even while display is on",
                        isChecked = viewModel.isFlashlightAlwaysTurnOffEnabled.value,
                        onCheckedChange = { viewModel.setFlashlightAlwaysTurnOffEnabled(it, context) }
                    )
                }
                
                Button(
                    onClick = { 
                        HapticUtil.performVirtualKeyHaptic(view)
                        showFlashlightOptions = false
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
    val view = LocalView.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { 
                HapticUtil.performUIHaptic(view)
                onClick() 
            }
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
