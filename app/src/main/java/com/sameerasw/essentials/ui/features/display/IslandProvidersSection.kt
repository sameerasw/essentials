package com.sameerasw.essentials.ui.features.display

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.remote.OvercastLink
import com.sameerasw.essentials.island.remote.ProviderDiscovery
import com.sameerasw.essentials.island.remote.ProviderInfo
import com.sameerasw.essentials.island.remote.ProviderPairing
import com.sameerasw.essentials.island.remote.ProviderTrustStore
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.features.display.sheets.IslandProviderPairingSheet
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun IslandProvidersSection(onChanged: () -> Unit = {}) {
    val context = LocalContext.current
    val view = LocalView.current
    val trust = remember { ProviderTrustStore(SettingsRepository(context)) }
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose {}
    }
    val providers = remember(refresh) { ProviderDiscovery.query(context) }
    val showOvercast = providers.none { it.packageName == OvercastLink.PACKAGE }
    var pending by remember { mutableStateOf<ProviderInfo?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Text(
        text = stringResource(R.string.island_providers_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp),
    )
    RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
        providers.forEach { provider ->
            IconToggleItem(
                leadingIcon = ProviderAppIcon(provider.packageName),
                title = provider.label,
                isChecked = remember(refresh, provider.packageName) { trust.isEnabled(provider.packageName) },
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked) {
                        pending = provider
                    } else {
                        trust.disable(provider.packageName)
                        scope.launch { ProviderPairing.revoke(context, provider) }
                    }
                    refresh++
                    onChanged()
                },
                onSettingsClick = { ProviderDiscovery.openSettings(context, provider) },
            )
        }
        if (showOvercast) {
            IconToggleItem(
                iconRes = R.drawable.rounded_partly_cloudy_day_24,
                title = stringResource(R.string.app_overcast),
                showToggle = false,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (OvercastLink.isInstalled(context)) OvercastLink.openApp(context) else OvercastLink.openInstallPage(context)
                },
            )
        }
    }

    pending?.let { provider ->
        IslandProviderPairingSheet(
            provider = provider,
            busy = busy,
            onAllow = {
                busy = true
                scope.launch {
                    if (ProviderPairing.grant(context, provider) && trust.enable(context, provider.packageName)) {
                        pending = null
                    } else {
                        Toast.makeText(context, R.string.island_provider_pair_failed, Toast.LENGTH_SHORT).show()
                    }
                    busy = false
                    refresh++
                    onChanged()
                }
            },
            onDismissRequest = { if (!busy) pending = null },
        )
    }
}
