package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandLiveUpdatesOptionsBottomSheet(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings = remember { SettingsRepository(context) }
    var count by remember { mutableIntStateOf(settings.getIslandLiveUpdatesCount()) }
    var expanded by remember { mutableStateOf(settings.isIslandLiveUpdatesExpandedEnabled()) }

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TranslatableText(
                stringResId = R.string.island_live_updates_title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                        .padding(top = 12.dp),
                ) {
                    TranslatableText(
                        stringResId = R.string.island_live_updates_count_title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    SegmentedPicker(
                        items = listOf(1, 2, 3),
                        selectedItem = count,
                        onItemSelected = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            count = it
                            settings.setIslandLiveUpdatesCount(it)
                        },
                        labelProvider = { it.toString() },
                        title = R.string.island_live_updates_count_title,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                IconToggleItem(
                    iconRes = R.drawable.rounded_downloading_24,
                    title = stringResource(R.string.island_live_updates_expanded_title),
                    isChecked = expanded,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        expanded = it
                        settings.setIslandLiveUpdatesExpandedEnabled(it)
                    },
                )
            }
        }
    }
}
