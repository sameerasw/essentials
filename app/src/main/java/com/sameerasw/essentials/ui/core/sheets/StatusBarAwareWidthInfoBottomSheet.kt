package com.sameerasw.essentials.ui.core.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusBarAwareWidthInfoBottomSheet(onDismissRequest: () -> Unit) {
    val view = LocalView.current
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
                stringResId = R.string.island_statusbar_aware_width_title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TranslatableText(
                stringResId = R.string.island_statusbar_aware_width_info,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TranslatableText(
                stringResId = R.string.island_statusbar_aware_width_info_tip,
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
