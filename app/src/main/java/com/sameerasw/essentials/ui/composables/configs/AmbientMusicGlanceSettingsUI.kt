package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientMusicGlanceSettingsUI(
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
            featureTitle = R.string.feat_ambient_music_glance_title,
            permissions = listOf(
                com.sameerasw.essentials.ui.components.sheets.PermissionItem(
                    iconRes = R.drawable.rounded_settings_accessibility_24,
                    title = R.string.perm_accessibility_title,
                    description = R.string.perm_accessibility_desc_ambient_music_glance,
                    dependentFeatures = listOf(R.string.feat_ambient_music_glance_title),
                    actionLabel = R.string.perm_action_enable,
                    action = {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    },
                    isGranted = isAccessibilityEnabled
                ),
                com.sameerasw.essentials.ui.components.sheets.PermissionItem(
                    iconRes = R.drawable.rounded_notifications_unread_24,
                    title = R.string.perm_notif_listener_title,
                    description = R.string.perm_notif_listener_desc_lighting,
                    dependentFeatures = listOf(R.string.feat_ambient_music_glance_title),
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
                title = stringResource(R.string.feat_ambient_music_glance_title),
                description = stringResource(R.string.feat_ambient_music_glance_desc),
                isChecked = viewModel.isAmbientMusicGlanceEnabled.value,
                onCheckedChange = { viewModel.setAmbientMusicGlanceEnabled(it) },
                enabled = isPermissionGranted,
                onDisabledClick = { showPermissionSheet = true },
                modifier = Modifier.highlight(highlightSetting == "enable_ambient_glance")
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_charge_24,
                title = stringResource(R.string.ambient_glance_docked_mode_title),
                description = stringResource(R.string.ambient_glance_docked_mode_desc),
                isChecked = viewModel.isAmbientMusicGlanceDockedModeEnabled.value,
                onCheckedChange = { viewModel.setAmbientMusicGlanceDockedModeEnabled(it) },
                enabled = isPermissionGranted && viewModel.isAmbientMusicGlanceEnabled.value,
                onDisabledClick = { if (!isPermissionGranted) showPermissionSheet = true },
                modifier = Modifier.highlight(highlightSetting == "ambient_glance_docked_mode")
            )
        }
    }
}
