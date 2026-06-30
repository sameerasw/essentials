package com.sameerasw.essentials.ui.composables.configs

import android.content.SharedPreferences
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.ScreenOffMethod
import com.sameerasw.essentials.ui.components.cards.FeatureCard
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.HapticFeedbackPicker
import com.sameerasw.essentials.ui.components.pickers.ScreenOffMethodPicker
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.performHapticFeedback
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun ScreenOffWidgetSettingsUI(
    viewModel: MainViewModel,
    selectedHaptic: HapticFeedbackType,
    onHapticSelected: (HapticFeedbackType) -> Unit,
    vibrator: Vibrator?,
    prefs: SharedPreferences,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
    onShowPermissionSheet: (Boolean) -> Unit,
    onSetChildFeatureForPermissions: (String?) -> Unit
) {
    val context = LocalContext.current
    val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted

    var selectedScreenOffMethod by remember {
        val name =
            prefs.getString("screen_off_method", ScreenOffMethod.ACCESSIBILITY.name)
        mutableStateOf(
            try {
                ScreenOffMethod.valueOf(name ?: ScreenOffMethod.ACCESSIBILITY.name)
            } catch (@Suppress("UNUSED_PARAMETER") e: Exception) {
                ScreenOffMethod.ACCESSIBILITY
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Screen Off Method Category
        Text(
            text = stringResource(R.string.screen_off_method_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 8.dp,
            cornerRadius = 24.dp
        ) {
            ScreenOffMethodPicker(
                selectedMethod = selectedScreenOffMethod,
                onMethodSelected = { type ->
                    if (type == ScreenOffMethod.INPUT && !isShizukuPermissionGranted) {
                        onSetChildFeatureForPermissions(context.getString(R.string.screen_off_widget_input_permission_id))
                        onShowPermissionSheet(true)
                    } else {
                        prefs.edit {
                            putString("screen_off_method", type.name)
                        }
                        selectedScreenOffMethod = type
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "screen_off_method_picker")
            )
        }

        // Haptic Feedback Category
        Text(
            text = stringResource(R.string.settings_section_haptic),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 8.dp,
            cornerRadius = 24.dp
        ) {
            HapticFeedbackPicker(
                selectedFeedback = selectedHaptic,
                onFeedbackSelected = { type ->
                    prefs.edit {
                        putString("haptic_feedback_type", type.name)
                    }
                    onHapticSelected(type)
                    viewModel.setHapticFeedback(type, context)
                    if (vibrator != null) {
                        performHapticFeedback(vibrator, type)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "haptic_picker")
            )
        }
    }
}
