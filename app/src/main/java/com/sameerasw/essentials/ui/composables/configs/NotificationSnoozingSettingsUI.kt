package com.sameerasw.essentials.ui.composables.configs

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun NotificationSnoozingSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    val isSnoozeEnabled = viewModel.isShowNotificationSnoozeEnabled.value
    val snoozeDefault = viewModel.notificationSnoozeDefault.intValue
    val snoozeOptions = viewModel.notificationSnoozeOptions.value

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                title = stringResource(R.string.feat_notification_snoozing_title),
                description = stringResource(R.string.feat_notification_snoozing_desc),
                isChecked = isSnoozeEnabled,
                onCheckedChange = {
                    viewModel.setShowNotificationSnoozeEnabled(context, it)
                    HapticUtil.performUIHaptic(view)
                },
                iconRes = R.drawable.rounded_notifications_paused_24,
                modifier = Modifier.highlight(highlightSetting == "snooze_enabled")
            )
        }

        AnimatedVisibility(
            visible = isSnoozeEnabled,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.title_snooze_durations),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RoundedCardContainer(spacing = 2.dp) {
                    ConfigSliderItem(
                        title = stringResource(R.string.label_snooze_default),
                        value = snoozeDefault.toFloat(),
                        onValueChange = {
                            val options = snoozeOptions.toMutableList()
                            viewModel.saveNotificationSnoozeOptions(context, it.toInt(), options)
                            HapticUtil.performSliderHaptic(view)
                        },
                        valueRange = 10f..1440f,
                        steps = 285,
                        increment = 5f,
                        valueFormatter = { f ->
                            val mins = f.toInt()
                            if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
                        },
                        iconRes = R.drawable.rounded_notifications_paused_24,
                        enabled = isSnoozeEnabled
                    )

                    snoozeOptions.forEachIndexed { index, optionVal ->
                        val labelRes = when (index) {
                            0 -> R.string.label_snooze_option_1
                            1 -> R.string.label_snooze_option_2
                            2 -> R.string.label_snooze_option_3
                            else -> R.string.label_snooze_option_4
                        }
                        ConfigSliderItem(
                            title = stringResource(labelRes),
                            value = optionVal.toFloat(),
                            onValueChange = { newVal ->
                                val options = snoozeOptions.toMutableList()
                                if (index < options.size) {
                                    options[index] = newVal.toInt()
                                }
                                viewModel.saveNotificationSnoozeOptions(context, snoozeDefault, options)
                                HapticUtil.performSliderHaptic(view)
                            },
                            valueRange = 10f..1440f,
                            steps = 285,
                            increment = 5f,
                            valueFormatter = { f ->
                                val mins = f.toInt()
                                if (mins >= 60) "${mins / 60}h ${mins % 60}m" else "${mins}m"
                            },
                            iconRes = R.drawable.rounded_notifications_paused_24,
                            enabled = isSnoozeEnabled
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.resetNotificationSnoozeOptions(context)
                        HapticUtil.performUIHaptic(view)
                        Toast.makeText(context, context.getString(R.string.msg_snooze_reset_success), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = stringResource(R.string.action_reset_snooze))
                }
            }
        }
    }
}
