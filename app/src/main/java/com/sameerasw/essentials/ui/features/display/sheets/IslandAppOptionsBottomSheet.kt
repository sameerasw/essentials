package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.remote.ProviderTrustStore
import com.sameerasw.essentials.island.remote.RemoteProviderOptions
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.ui.features.display.ProviderAppIcon
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandAppOptionsBottomSheet(
    packageName: String,
    label: String,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val store = remember { ProviderTrustStore(SettingsRepository(context)) }
    var launcherOnly by remember { mutableStateOf(packageName in store.launcherOnly()) }
    var hideWhenLocked by remember { mutableStateOf(packageName in store.hideWhenLocked()) }
    var showOnUpdate by remember { mutableStateOf(packageName in store.showOnUpdate()) }
    var chip by remember { mutableStateOf(packageName in store.chipped()) }
    val chipsAvailable = remember { SettingsRepository(context).getBoolean(SettingsRepository.KEY_ISLAND_CHIPS, false) }
    val providerOptions by RemoteProviderOptions.options.collectAsState()
    val ownOptions = providerOptions[packageName].orEmpty()

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_home_24,
                    title = stringResource(R.string.island_launcher_only_title),
                    isChecked = launcherOnly,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        launcherOnly = it
                        store.setLauncherOnly(packageName, it)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_lock_24,
                    title = stringResource(R.string.island_hide_when_locked_title),
                    isChecked = hideWhenLocked,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        hideWhenLocked = it
                        store.setHideWhenLocked(packageName, it)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_sync_24,
                    title = stringResource(R.string.island_show_on_update_title),
                    isChecked = showOnUpdate,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showOnUpdate = it
                        store.setShowOnUpdate(packageName, it)
                    },
                )
                if (chipsAvailable) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_palette_24,
                        title = stringResource(R.string.island_provider_chip_title),
                        isChecked = chip,
                        onCheckedChange = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            chip = it
                            store.setChip(packageName, it)
                        },
                    )
                }
            }

            if (ownOptions.isNotEmpty()) {
                RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                    ownOptions.forEach { option ->
                        IconToggleItem(
                            leadingIcon = ProviderAppIcon(packageName),
                            title = option.title,
                            isChecked = option.checked,
                            onCheckedChange = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                RemoteProviderOptions.change(packageName, option.key, it)
                            },
                        )
                    }
                }
            }
        }
    }
}
