package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEffectsSettingsSheet(
    initialAction: Action.DeviceEffects,
    onDismiss: () -> Unit,
    onSave: (Action.DeviceEffects) -> Unit
) {
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var enabled by remember { mutableStateOf(initialAction.enabled) }
    var grayscale by remember { mutableStateOf(initialAction.grayscale) }
    var suppressAmbient by remember { mutableStateOf(initialAction.suppressAmbient) }
    var dimWallpaper by remember { mutableStateOf(initialAction.dimWallpaper) }
    var nightMode by remember { mutableStateOf(initialAction.nightMode) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.diy_action_device_effects),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val infoTitle = "Info" // Key for color generation
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = ColorUtil.getPastelColorFor(infoTitle),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_info_24),
                            contentDescription = null,
                            tint = ColorUtil.getVibrantColorFor(infoTitle),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.diy_device_effects_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (android.os.Build.VERSION.SDK_INT < 35) {
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = stringResource(R.string.diy_device_effects_android_15_warning),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Settings Container
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Master Toggle (Enable/Disable)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                HapticUtil.performVirtualKeyHaptic(view)
                                enabled = !enabled
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val toggleTitle = stringResource(R.string.diy_device_effects_enabled)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (enabled) ColorUtil.getPastelColorFor(toggleTitle) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = if (enabled) R.drawable.rounded_check_circle_24 else R.drawable.rounded_cancel_24),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = if (enabled) ColorUtil.getVibrantColorFor(toggleTitle) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = stringResource(if (enabled) R.string.diy_device_effects_enabled else R.string.diy_device_effects_disabled),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                enabled = it
                            }
                        )
                    }
                }

                if (enabled) {
                    // Effect Toggles in a Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            EffectToggleItem(
                                title = stringResource(R.string.diy_effect_grayscale),
                                isChecked = grayscale,
                                onCheckedChange = { grayscale = it }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.5f
                                ), modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            EffectToggleItem(
                                title = stringResource(R.string.diy_effect_suppress_ambient),
                                isChecked = suppressAmbient,
                                onCheckedChange = { suppressAmbient = it }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.5f
                                ), modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            EffectToggleItem(
                                title = stringResource(R.string.diy_effect_dim_wallpaper),
                                isChecked = dimWallpaper,
                                onCheckedChange = { dimWallpaper = it }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                    alpha = 0.5f
                                ), modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            EffectToggleItem(
                                title = stringResource(R.string.diy_effect_night_mode),
                                isChecked = nightMode,
                                onCheckedChange = { nightMode = it }
                            )
                        }
                    }
                }
            }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onSave(
                            initialAction.copy(
                                enabled = enabled,
                                grayscale = grayscale,
                                suppressAmbient = suppressAmbient,
                                dimWallpaper = dimWallpaper,
                                nightMode = nightMode
                            )
                        )
                    },
                    enabled = android.os.Build.VERSION.SDK_INT >= 35,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun EffectToggleItem(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                HapticUtil.performVirtualKeyHaptic(view)
                onCheckedChange(!isChecked)
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = {
                HapticUtil.performVirtualKeyHaptic(view)
                onCheckedChange(it)
            }
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
