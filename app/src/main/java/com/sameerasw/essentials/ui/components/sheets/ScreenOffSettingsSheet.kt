package com.sameerasw.essentials.ui.components.sheets

import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.ScreenOffMethod
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.HapticFeedbackPicker
import com.sameerasw.essentials.ui.components.pickers.ScreenOffMethodPicker
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.utils.performHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenOffSettingsSheet(
    initialAction: Action.ScreenOff,
    onDismiss: () -> Unit,
    onSave: (Action.ScreenOff) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedMethod by remember { mutableStateOf(initialAction.method) }
    var selectedHaptic by remember { mutableStateOf(initialAction.haptic) }

    EssentialsBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.diy_action_screen_off),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Screen Off Method
            Text(
                text = stringResource(R.string.screen_off_method_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RoundedCardContainer(spacing = 8.dp, cornerRadius = 24.dp) {
                ScreenOffMethodPicker(
                    selectedMethod = selectedMethod,
                    onMethodSelected = { type ->
                        HapticUtil.performUIHaptic(view)
                        if (type == ScreenOffMethod.INPUT && !ShellUtils.hasPermission(context)) {
                            android.widget.Toast.makeText(
                                context,
                                "Shizuku/Root permission required for Input method",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            selectedMethod = type
                        }
                    }
                )
            }

            // Haptic Feedback
            Text(
                text = stringResource(R.string.settings_section_haptic),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RoundedCardContainer(spacing = 8.dp, cornerRadius = 24.dp) {
                HapticFeedbackPicker(
                    selectedFeedback = selectedHaptic,
                    onFeedbackSelected = { type ->
                        HapticUtil.performUIHaptic(view)
                        selectedHaptic = type
                        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                        }
                        if (vibrator != null) {
                            performHapticFeedback(vibrator, type)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        HapticUtil.performHeavyHaptic(view)
                        onSave(Action.ScreenOff(selectedMethod, selectedHaptic))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
