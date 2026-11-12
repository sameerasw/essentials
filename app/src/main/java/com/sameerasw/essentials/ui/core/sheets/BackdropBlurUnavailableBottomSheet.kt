package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.island.ui.IslandBackdropBlurSupport
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.utils.HapticUtil

@Composable
internal fun BackdropBlurUnavailableBottomSheet(
    status: IslandBackdropBlurSupport.Status,
    onDismissRequest: () -> Unit,
) {
    val view = LocalView.current
    val reason =
        when (status) {
            IslandBackdropBlurSupport.Status.Available -> R.string.island_backdrop_blur_available_note
            IslandBackdropBlurSupport.Status.UnsupportedOs -> R.string.island_backdrop_blur_unavailable_os
            IslandBackdropBlurSupport.Status.WindowBlurDisabled -> R.string.island_backdrop_blur_unavailable_disabled
            else -> R.string.island_backdrop_blur_unavailable_api
        }
    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TranslatableText(
                stringResId = R.string.island_backdrop_blur_title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (status == IslandBackdropBlurSupport.Status.Available) {
                TranslatableText(
                    stringResId = R.string.island_backdrop_blur_available_title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TranslatableText(
                stringResId = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                TranslatableText(R.string.action_got_it)
            }
        }
    }
}
