package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sameerasw.essentials.ui.components.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.modifiers.highlight

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DynamicNightLightSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current


    // App selection state
    var showAppSelectionSheet by remember { mutableStateOf(false) }


    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {

        RoundedCardContainer(
            modifier = Modifier.padding(top = 8.dp),
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_nightlight_24,
                title = "Enable Dynamic Night Light",
                isChecked = viewModel.isDynamicNightLightEnabled.value,
                onCheckedChange = { checked ->
                    viewModel.setDynamicNightLightEnabled(checked, context)
                },
                modifier = Modifier.highlight(highlightSetting == "dynamic_night_light_toggle")
            )
        }

        Text(
            text = "Apps that toggle off night light",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // App Selection Sheet Button
        Button(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                showAppSelectionSheet = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select apps")
        }

        if (showAppSelectionSheet) {
            AppSelectionSheet(
                onDismissRequest = { showAppSelectionSheet = false },
                onLoadApps = { viewModel.loadDynamicNightLightSelectedApps(it) },
                onSaveApps = { ctx, apps -> viewModel.saveDynamicNightLightSelectedApps(ctx, apps) },
                onAppToggle = { ctx, pkg, enabled -> viewModel.updateDynamicNightLightAppEnabled(ctx, pkg, enabled) },
                context = context
            )
        }
    }
}
