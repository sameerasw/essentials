package com.sameerasw.essentials.ui.features.display

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import com.sameerasw.essentials.translation.TranslatableText
import kotlinx.coroutines.launch
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.remote.KnownProviders
import com.sameerasw.essentials.island.remote.ProviderDiscovery
import com.sameerasw.essentials.island.remote.ProviderInfo
import com.sameerasw.essentials.island.remote.PairingResult
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
    val suggested = KnownProviders.all.filter { known -> providers.none { it.packageName == known.packageName } }
    var pending by remember { mutableStateOf<ProviderInfo?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    TranslatableText(
        stringResId = R.string.island_providers_title,
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
        suggested.forEach { known ->
            IconToggleItem(
                iconRes = known.iconRes,
                title = stringResource(known.nameRes),
                showToggle = false,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    known.open(context)
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
                    val result = ProviderPairing.grant(context, provider)
                    if (result == PairingResult.Granted && trust.enable(context, provider.packageName)) {
                        pending = null
                    } else {
                        val message = if (result == PairingResult.Refused) R.string.island_provider_pair_refused else R.string.island_provider_pair_failed
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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
