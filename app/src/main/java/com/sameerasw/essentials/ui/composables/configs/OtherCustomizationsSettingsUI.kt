package com.sameerasw.essentials.ui.composables.configs

import android.content.Intent
import android.net.Uri
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
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun OtherCustomizationsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    var showPermissionSheet by remember { mutableStateOf(false) }

    if (showPermissionSheet) {
        val isShizukuAvailable = viewModel.isShizukuAvailable.value
        val isShizukuGranted = viewModel.isShizukuPermissionGranted.value
        val isRootAvailable = viewModel.isRootAvailable.value
        val isRootGranted = viewModel.isRootPermissionGranted.value
        val isShellGranted = (isShizukuAvailable && isShizukuGranted) || (isRootAvailable && isRootGranted)

        PermissionsBottomSheet(
            onDismissRequest = { showPermissionSheet = false },
            featureTitle = R.string.feat_other_customizations_title,
            permissions = listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_adb_24,
                    title = if (!isShizukuAvailable) R.string.perm_shizuku_title else R.string.perm_shizuku_grant_title,
                    description = if (!isShizukuAvailable) R.string.perm_shizuku_desc else R.string.perm_shizuku_grant_desc,
                    dependentFeatures = listOf(R.string.feat_hide_gesture_bar_title),
                    actionLabel = if (!isShizukuAvailable) R.string.perm_shizuku_install_action else R.string.perm_action_grant,
                    action = {
                        if (!isShizukuAvailable) {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")
                            )
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } else {
                            viewModel.requestShizukuPermission()
                        }
                    },
                    isGranted = isShellGranted
                )
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            val isShizukuGranted = viewModel.isShizukuAvailable.value && viewModel.isShizukuPermissionGranted.value
            val isRootGranted = viewModel.isRootAvailable.value && viewModel.isRootPermissionGranted.value
            val isShellGranted = isShizukuGranted || isRootGranted
            
            IconToggleItem(
                title = stringResource(R.string.feat_hide_gesture_bar_title),
                description = stringResource(R.string.feat_hide_gesture_bar_desc),
                isChecked = viewModel.isHideGestureBarEnabled.value,
                onCheckedChange = { enabled ->
                    if (isShellGranted) {
                        viewModel.setHideGestureBarEnabled(enabled, context)
                    } else {
                        showPermissionSheet = true
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!isShellGranted) {
                        showPermissionSheet = true
                    }
                },
                iconRes = R.drawable.rounded_home_24,
                modifier = Modifier.highlight(highlightSetting == "hide_gesture_bar_toggle")
            )
        }
    }
}
