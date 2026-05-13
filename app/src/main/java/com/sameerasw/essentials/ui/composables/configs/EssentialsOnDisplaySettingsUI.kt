package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.text.Text
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EssentialsOnDisplaySettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    var showPermissionSheet by remember { androidx.compose.runtime.mutableStateOf(false) }

    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    val isNotificationListenerEnabled by viewModel.isNotificationListenerEnabled

    val isPermissionGranted = isAccessibilityEnabled && isNotificationListenerEnabled

    if (showPermissionSheet) {
        com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet(
            onDismissRequest = { showPermissionSheet = false },
            featureTitle = R.string.feat_essentials_on_display_title,
            permissions = listOf(
                com.sameerasw.essentials.ui.components.sheets.PermissionItem(
                    iconRes = R.drawable.rounded_settings_accessibility_24,
                    title = R.string.perm_accessibility_title,
                    description = R.string.perm_accessibility_desc_essentials_on_display,
                    dependentFeatures = listOf(R.string.feat_essentials_on_display_title),
                    actionLabel = R.string.perm_action_enable,
                    action = {
                        val intent =
                            android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    },
                    isGranted = isAccessibilityEnabled
                ),
                com.sameerasw.essentials.ui.components.sheets.PermissionItem(
                    iconRes = R.drawable.rounded_notifications_unread_24,
                    title = R.string.perm_notif_listener_title,
                    description = R.string.perm_notif_listener_desc_lighting,
                    dependentFeatures = listOf(R.string.feat_essentials_on_display_title),
                    actionLabel = R.string.perm_action_grant,
                    action = { viewModel.requestNotificationListenerPermission(context) },
                    isGranted = isNotificationListenerEnabled
                )
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_blur_on_24,
                title = stringResource(R.string.feat_essentials_on_display_title),
                description = stringResource(R.string.feat_essentials_on_display_desc),
                isChecked = viewModel.isAmbientMusicGlanceEnabled.value,
                onCheckedChange = { viewModel.setAmbientMusicGlanceEnabled(it) },
                enabled = isPermissionGranted,
                onDisabledClick = { showPermissionSheet = true },
                modifier = Modifier.highlight(highlightSetting == "enable_essentials_on_display")
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_charge_24,
                title = stringResource(R.string.essentials_on_display_docked_mode_title),
                description = stringResource(R.string.essentials_on_display_docked_mode_desc),
                isChecked = viewModel.isAmbientMusicGlanceDockedModeEnabled.value,
                onCheckedChange = { viewModel.setAmbientMusicGlanceDockedModeEnabled(it) },
                enabled = isPermissionGranted && viewModel.isAmbientMusicGlanceEnabled.value,
                onDisabledClick = { if (!isPermissionGranted) showPermissionSheet = true },
                modifier = Modifier.highlight(highlightSetting == "essentials_on_display_docked_mode")
            )

            androidx.compose.animation.AnimatedVisibility(visible = viewModel.isAmbientMusicGlanceDockedModeEnabled.value) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_notification_sound_24,
                    title = stringResource(R.string.essentials_on_display_respect_notifications_title),
                    description = stringResource(R.string.essentials_on_display_respect_notifications_desc),
                    isChecked = viewModel.isAmbientMusicGlanceRespectNotificationsEnabled.value,
                    onCheckedChange = { viewModel.setAmbientMusicGlanceRespectNotificationsEnabled(it) }
                )
            }
        }

        Text(
            text = stringResource(R.string.essentials_on_display_album_art_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        com.sameerasw.essentials.ui.components.pickers.AlbumArtModePicker(
            selectedMode = viewModel.ambientMusicGlanceAlbumArtMode.value,
            onModeSelected = { viewModel.setAmbientMusicGlanceAlbumArtMode(it) },
            modifier = Modifier.highlight(highlightSetting == "essentials_on_display_album_art")
        )

        androidx.compose.animation.AnimatedVisibility(visible = viewModel.ambientMusicGlanceAlbumArtMode.value == "fill") {
            Column {
                Text(
                    text = stringResource(R.string.essentials_on_display_clock_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RoundedCardContainer {
                    com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem(
                        title = stringResource(R.string.label_size),
                        value = viewModel.ambientMusicGlanceClockSize.intValue.toFloat(),
                        onValueChange = { viewModel.setAmbientMusicGlanceClockSize(it.toInt()) },
                        valueRange = 40f..150f,
                        increment = 5f,
                        valueFormatter = { it.toInt().toString() },
                        iconRes = R.drawable.rounded_mobile_text_2_24
                    )

                    com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem(
                        title = stringResource(R.string.label_weight),
                        value = viewModel.ambientMusicGlanceClockWeight.intValue.toFloat(),
                        onValueChange = { viewModel.setAmbientMusicGlanceClockWeight(it.toInt()) },
                        valueRange = 100f..1000f,
                        increment = 10f,
                        valueFormatter = { it.toInt().toString() },
                        iconRes = R.drawable.rounded_line_weight_24
                    )

                    com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem(
                        title = stringResource(R.string.label_width),
                        value = viewModel.ambientMusicGlanceClockWidth.intValue.toFloat(),
                        onValueChange = { viewModel.setAmbientMusicGlanceClockWidth(it.toInt()) },
                        valueRange = 25f..200f,
                        increment = 5f,
                        valueFormatter = { it.toInt().toString() },
                        iconRes = R.drawable.rounded_arrows_outward_24
                    )

                    com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem(
                        title = stringResource(R.string.label_roundness),
                        value = viewModel.ambientMusicGlanceClockRoundness.intValue.toFloat(),
                        onValueChange = { viewModel.setAmbientMusicGlanceClockRoundness(it.toInt()) },
                        valueRange = 0f..100f,
                        increment = 5f,
                        valueFormatter = { it.toInt().toString() },
                        iconRes = R.drawable.rounded_rounded_corner_24
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                RoundedCardContainer {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_battery_android_frame_plus_24,
                        title = stringResource(R.string.essentials_on_display_force_fill_while_charging_title),
                        description = stringResource(R.string.essentials_on_display_force_fill_while_charging_desc),
                        isChecked = viewModel.isAmbientMusicGlanceForceFillWhileChargingEnabled.value,
                        onCheckedChange = { viewModel.setAmbientMusicGlanceForceFillWhileChargingEnabled(it) }
                    )
                }
            }
        }
    }
}
