package com.sameerasw.essentials.ui.features.display.sheets

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.essentials.R
import com.sameerasw.essentials.island.remote.ProviderInfo
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.island.sdk.IslandIdentity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandProviderPairingSheet(
    provider: ProviderInfo,
    busy: Boolean,
    onAllow: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val hostIcon = remember { appIcon(context.packageManager.getApplicationIcon(context.packageName)) }
    val providerIcon = remember(provider.packageName) {
        runCatching { appIcon(context.packageManager.getApplicationIcon(provider.packageName)) }.getOrNull()
    }
    val fingerprint = remember(provider.packageName) {
        IslandIdentity.fingerprint(context, provider.packageName)?.let(IslandIdentity::shortFingerprint).orEmpty()
    }

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                AppBadge(hostIcon)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_link_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                providerIcon?.let { AppBadge(it) }
            }
            Text(
                text = stringResource(R.string.island_provider_pair_title, provider.label),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = provider.description ?: stringResource(R.string.island_provider_pair_subtitle, provider.label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                CapabilityRow(R.drawable.rounded_visibility_24, stringResource(R.string.island_provider_cap_show, provider.label), top = true)
                CapabilityRow(R.drawable.rounded_widgets_24, stringResource(R.string.island_provider_cap_views))
                CapabilityRow(R.drawable.rounded_open_in_new_24, stringResource(R.string.island_provider_cap_open, provider.label), bottom = true)
            }
            if (fingerprint.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_shield_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.island_provider_detail_key),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = fingerprint,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(stringResource(R.string.island_provider_deny)) }
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onAllow()
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) { Text(stringResource(R.string.island_provider_allow)) }
            }
        }
    }
}

private fun appIcon(drawable: Drawable): ImageBitmap = drawable.toBitmap(192, 192).asImageBitmap()

@Composable
private fun AppBadge(icon: ImageBitmap) {
    Image(
        bitmap = icon,
        contentDescription = null,
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(20.dp)),
    )
}

@Composable
private fun CapabilityRow(icon: Int, text: String, top: Boolean = false, bottom: Boolean = false) {
    val large = 20.dp
    val small = 6.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = if (top) large else small,
                    topEnd = if (top) large else small,
                    bottomStart = if (bottom) large else small,
                    bottomEnd = if (bottom) large else small,
                ),
            )
            .background(MaterialTheme.colorScheme.surfaceBright)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
