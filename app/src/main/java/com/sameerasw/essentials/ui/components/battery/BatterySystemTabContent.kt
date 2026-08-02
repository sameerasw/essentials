package com.sameerasw.essentials.ui.components.battery

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.CpuWakeupItem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sameerasw.essentials.ui.components.buttons.ListExpandToggleButton
import java.util.Locale

@Composable
fun BatterySystemTabContent(
    isLoadingAdvanced: Boolean,
    powerProfile: Map<String, String>?,
    wakeupsList: List<CpuWakeupItem>,
    showPercentage: Boolean = false,
    onToggleUnit: () -> Unit
) {
    if (isLoadingAdvanced) {
        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            BatteryLoadingIndicatorCard()
        }
    } else {
        if (!powerProfile.isNullOrEmpty()) {
            val activeKeys = listOf(
                "screen.on", "ambient.on", "audio", "video",
                "camera.avg", "camera.flashlight", "cpu.active", "cpu.idle", "cpu.suspend"
            )
            val totalMa = activeKeys.mapNotNull { powerProfile[it]?.toDoubleOrNull() }.sum().coerceAtLeast(0.0001)

            fun formatProfileValue(raw: String): String {
                val num = raw.toDoubleOrNull() ?: return "$raw mA"
                return if (showPercentage) {
                    val pct = (num / totalMa) * 100.0
                    String.format(Locale.getDefault(), "%.1f %%", pct)
                } else {
                    "$raw mA"
                }
            }

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                powerProfile["screen.on"]?.let {
                    InfoDetailRow(title = stringResource(R.string.label_screen_on_drain), value = formatProfileValue(it), iconRes = R.drawable.rounded_mobile_24, onClick = onToggleUnit)
                }
                powerProfile["screen.full"]?.let {
                    InfoDetailRow(title = stringResource(R.string.label_screen_max_drain), value = formatProfileValue(it), iconRes = R.drawable.rounded_mobile_charge_24, onClick = onToggleUnit)
                }
                powerProfile["ambient.on"]?.let {
                    InfoDetailRow(title = stringResource(R.string.label_ambient_aod_drain), value = formatProfileValue(it), iconRes = R.drawable.rounded_mobile_screensaver_24, onClick = onToggleUnit)
                }
                powerProfile["audio"]?.let {
                    InfoDetailRow(title = stringResource(R.string.label_audio_drain), value = formatProfileValue(it), iconRes = R.drawable.rounded_sound_detection_loud_sound_24, onClick = onToggleUnit)
                }
                powerProfile["video"]?.let {
                    InfoDetailRow(title = stringResource(R.string.label_video_drain), value = formatProfileValue(it), iconRes = R.drawable.rounded_slow_motion_video_24, onClick = onToggleUnit)
                }
                powerProfile["camera.avg"]?.let {
                    InfoDetailRow(title = stringResource(R.string.label_camera_drain), value = formatProfileValue(it), iconRes = R.drawable.rounded_camera_24, onClick = onToggleUnit)
                }
                powerProfile["camera.flashlight"]?.let {
                    InfoDetailRow(title = stringResource(R.string.label_flashlight_drain), value = formatProfileValue(it), iconRes = R.drawable.rounded_flashlight_on_24, onClick = onToggleUnit)
                }
                powerProfile["cpu.active"]?.let {
                    InfoDetailRow(title = stringResource(R.string.label_cpu_active_drain), value = formatProfileValue(it), iconRes = R.drawable.rounded_motion_play_24, onClick = onToggleUnit)
                }
                powerProfile["cpu.idle"]?.let {
                    InfoDetailRow(title = stringResource(R.string.label_cpu_idle_drain), value = formatProfileValue(it), iconRes = R.drawable.rounded_motion_photos_paused_24, onClick = onToggleUnit)
                }
                powerProfile["cpu.suspend"]?.let {
                    InfoDetailRow(title = stringResource(R.string.label_cpu_suspend_drain), value = formatProfileValue(it), iconRes = R.drawable.rounded_stop_circle_24, onClick = onToggleUnit)
                }
            }
        }

        if (wakeupsList.isNotEmpty()) {
            var showWakeups by remember { mutableStateOf(false) }

            ListExpandToggleButton(
                isExpanded = showWakeups,
                onToggle = { showWakeups = !showWakeups },
                title = R.string.action_hide_wakeups,
                description = R.string.action_show_wakeups
            )

            if (showWakeups) {
                RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                    wakeupsList.take(20).forEach { item ->
                        InfoDetailRow(
                            title = "${item.subsystem} (${item.timeAgo})",
                            value = item.attribution,
                            iconRes = item.iconRes
                        )
                    }
                }
            }
        }
    }
}
