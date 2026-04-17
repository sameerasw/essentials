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
        PermissionsBottomSheet(
            onDismissRequest = { showPermissionSheet = false },
            featureTitle = R.string.feat_other_customizations_title,
            permissions = listOf(
                PermissionItem(
                    iconRes = R.drawable.rounded_adb_24,
                    title = R.string.perm_shizuku_title,
                    description = R.string.perm_shizuku_desc,
                    dependentFeatures = listOf(R.string.feat_hide_gesture_bar_title),
                    actionLabel = R.string.perm_shizuku_install_action,
                    action = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/thedjchi/Shizuku"))
                        context.startActivity(intent)
                    },
                    isGranted = viewModel.isShizukuAvailable.value || viewModel.isRootAvailable.value
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
            val isShellAvailable = viewModel.isShizukuAvailable.value || viewModel.isRootAvailable.value
            
            IconToggleItem(
                title = stringResource(R.string.feat_hide_gesture_bar_title),
                description = stringResource(R.string.feat_hide_gesture_bar_desc),
                isChecked = viewModel.isHideGestureBarEnabled.value,
                onCheckedChange = { enabled ->
                    if (isShellAvailable) {
                        viewModel.setHideGestureBarEnabled(enabled, context)
                    } else {
                        showPermissionSheet = true
                    }
                },
                enabled = true,
                onDisabledClick = {
                    showPermissionSheet = true
                },
                iconRes = R.drawable.rounded_home_24,
                modifier = Modifier.highlight(highlightSetting == "hide_gesture_bar_toggle")
            )
        }
    }
}
