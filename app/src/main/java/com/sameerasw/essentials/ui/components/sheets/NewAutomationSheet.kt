package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import androidx.compose.ui.platform.LocalContext
import com.sameerasw.essentials.data.repository.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAutomationSheet(
    onDismiss: () -> Unit,
    onOptionSelected: (Automation.Type) -> Unit
) {
    val context = LocalContext.current
    val isPixelSearchbarEnabled = remember(context) {
        SettingsRepository(context).getBoolean(SettingsRepository.KEY_PIXEL_SEARCHBAR, false)
    }

    val hasActionShortcut = remember {
        DIYRepository.automations.value.any {
            it.type == Automation.Type.ACTION_SHORTCUT
        }
    }

    val hasPixelSearchbar = remember {
        DIYRepository.automations.value.any {
            it.type == Automation.Type.PIXEL_SEARCHBAR
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.diy_editor_new_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                OutlinedIconButton(
                    onClick = { /* TODO: Implement import */ },
                    enabled = false
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_download_24),
                        contentDescription = "Import Automation"
                    )
                }
            }

            RoundedCardContainer {
                // Trigger Option
                AutomationTypeOption(
                    title = stringResource(R.string.diy_create_trigger_title),
                    description = stringResource(R.string.diy_create_trigger_desc),
                    iconRes = R.drawable.rounded_bolt_24,
                    onClick = { onOptionSelected(Automation.Type.TRIGGER) }
                )

                // State Option
                AutomationTypeOption(
                    title = stringResource(R.string.diy_create_state_title),
                    description = stringResource(R.string.diy_create_state_desc),
                    iconRes = R.drawable.rounded_toggle_on_24,
                    onClick = { onOptionSelected(Automation.Type.STATE) }
                )

                // App Option
                AutomationTypeOption(
                    title = stringResource(R.string.diy_create_app_title),
                    description = stringResource(R.string.diy_create_app_desc),
                    iconRes = R.drawable.rounded_apps_24,
                    onClick = { onOptionSelected(Automation.Type.APP) }
                )

                // Action Shortcut Option
                AutomationTypeOption(
                    title = stringResource(R.string.diy_create_action_shortcut_title),
                    description = stringResource(R.string.diy_create_action_shortcut_desc),
                    iconRes = R.drawable.rounded_rocket_launch_24,
                    enabled = !hasActionShortcut,
                    onClick = { onOptionSelected(Automation.Type.ACTION_SHORTCUT) }
                )

                // Pixel Searchbar Tap Option
                if (isPixelSearchbarEnabled) {
                    AutomationTypeOption(
                        title = stringResource(R.string.diy_create_pixel_searchbar_title),
                        description = stringResource(R.string.diy_create_pixel_searchbar_desc),
                        iconRes = R.drawable.rounded_search_24,
                        enabled = !hasPixelSearchbar,
                        onClick = { onOptionSelected(Automation.Type.PIXEL_SEARCHBAR) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AutomationTypeOption(
    title: String,
    description: String,
    iconRes: Int,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    Surface(
        color = if (enabled) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.clickable {
                    HapticUtil.performUIHaptic(view)
                    onClick()
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.rounded_chevron_right_24),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(24.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
