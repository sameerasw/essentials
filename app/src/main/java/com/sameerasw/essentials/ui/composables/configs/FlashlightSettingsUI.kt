package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.ui.components.pickers.HapticFeedbackPicker
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.utils.HapticFeedbackType

@Composable
fun FlashlightSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Trigger Button",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 0.dp
        ) {
             SegmentedPicker(
                items = listOf("Volume Up", "Volume Down"),
                selectedItem = viewModel.flashlightTriggerButton.value,
                onItemSelected = { viewModel.setFlashlightTriggerButton(it, context) },
                labelProvider = { it }
            )
        }

        Text(
            text = "Haptic Feedback",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 0.dp
        ) {
            HapticFeedbackPicker(
                selectedFeedback = viewModel.flashlightHapticType.value,
                onFeedbackSelected = { viewModel.setFlashlightHapticType(it, context) },
                options = listOf(
                    "None" to HapticFeedbackType.NONE,
                    "Tick" to HapticFeedbackType.TICK,
                    "Long" to HapticFeedbackType.LONG
                )
            )
        }

        RoundedCardContainer {
            Text(
                text = "When the screen is off, long-press the selected Volume button to toggle the flashlight.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
