package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

private val WAIT_STEPS_SECONDS = intArrayOf(1, 2, 3, 5, 10, 15, 30, 45, 60, 120, 300, 600)

fun formatWait(seconds: Int): String = if (seconds < 60) "${seconds}s" else "${seconds / 60}m"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitSettingsSheet(
    initialAction: Action.Wait,
    onDismiss: () -> Unit,
    onSave: (Action.Wait) -> Unit,
) {
    val view = LocalView.current
    var stepIndex by remember {
        mutableIntStateOf(WAIT_STEPS_SECONDS.indices.minBy { kotlin.math.abs(WAIT_STEPS_SECONDS[it] - initialAction.seconds) })
    }

    EssentialsBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.diy_action_wait),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            RoundedCardContainer {
                ConfigSliderItem(
                    title = stringResource(R.string.diy_wait_duration),
                    description = stringResource(R.string.diy_wait_desc),
                    value = stepIndex.toFloat(),
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        stepIndex = it.toInt().coerceIn(WAIT_STEPS_SECONDS.indices)
                    },
                    valueRange = 0f..WAIT_STEPS_SECONDS.lastIndex.toFloat(),
                    increment = 1f,
                    iconRes = R.drawable.rounded_timer_24,
                    valueFormatter = { formatWait(WAIT_STEPS_SECONDS[it.toInt().coerceIn(WAIT_STEPS_SECONDS.indices)]) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onSave(Action.Wait(seconds = WAIT_STEPS_SECONDS[stepIndex]))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
