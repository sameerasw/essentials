package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

private val EXPAND_DISTANCE_OPTIONS = listOf(0, 50, 100, 200, 500)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandNavigationOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val view = LocalView.current

    @Composable
    fun distanceLabel(meters: Int) =
        if (meters == 0) stringResource(R.string.island_navigation_expand_off) else stringResource(R.string.island_navigation_expand_meters, meters)

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.island_navigation_options_title),
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
                    Text(
                        text = stringResource(R.string.island_navigation_on_new_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Text(
                        text = stringResource(R.string.island_navigation_on_new_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    val labels = mapOf(
                        SettingsRepository.ISLAND_NAVIGATION_ON_NEW_EXPAND to stringResource(R.string.island_navigation_on_new_expand),
                        SettingsRepository.ISLAND_NAVIGATION_ON_NEW_PEEK to stringResource(R.string.island_navigation_on_new_peek),
                        SettingsRepository.ISLAND_NAVIGATION_ON_NEW_OFF to stringResource(R.string.island_navigation_expand_off),
                    )
                    SegmentedPicker(
                        items = labels.keys.toList(),
                        selectedItem = viewModel.islandNavigationOnNew.value,
                        onItemSelected = { viewModel.setIslandNavigationOnNew(it) },
                        labelProvider = { labels[it].orEmpty() },
                        title = R.string.island_navigation_on_new_title,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                ConfigPickerItem(
                    title = stringResource(R.string.island_navigation_expand_title),
                    selectedValue = distanceLabel(viewModel.islandNavigationExpandDistance.intValue),
                    iconRes = R.drawable.rounded_magnify_fullscreen_24,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    EXPAND_DISTANCE_OPTIONS.forEach { meters ->
                        SegmentedDropdownMenuItem(
                            text = { Text(distanceLabel(meters)) },
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.setIslandNavigationExpandDistance(meters)
                            },
                        )
                    }
                }

                IconToggleItem(
                    iconRes = R.drawable.round_navigation_24,
                    title = stringResource(R.string.island_navigation_peek_after_expand_title),
                    description = stringResource(R.string.island_navigation_peek_after_expand_desc),
                    isChecked = viewModel.isIslandNavigationPeekAfterExpand.value,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandNavigationPeekAfterExpand(it)
                    },
                )
            }
        }
    }
}
