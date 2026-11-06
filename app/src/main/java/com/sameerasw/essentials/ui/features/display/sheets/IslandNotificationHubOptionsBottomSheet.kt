package com.sameerasw.essentials.ui.features.display.sheets

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
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandNotificationHubOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val view = LocalView.current

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.island_notif_hub_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                val autoLabel = stringResource(R.string.island_notif_hub_icons_auto)
                val dynamicLabel = stringResource(R.string.island_notif_hub_order_dynamic)
                val priorityLabel = stringResource(R.string.island_notif_hub_order_priority)
                val recentLabel = stringResource(R.string.island_notif_hub_order_recent)
                SegmentedPicker(
                    items = listOf(
                        SettingsRepository.ISLAND_NOTIF_HUB_ORDER_DYNAMIC,
                        SettingsRepository.ISLAND_NOTIF_HUB_ORDER_PRIORITY,
                        SettingsRepository.ISLAND_NOTIF_HUB_ORDER_RECENT,
                    ),
                    selectedItem = viewModel.islandNotifHubOrder.value,
                    onItemSelected = { viewModel.setIslandNotifHubOrder(it) },
                    labelProvider = {
                        when (it) {
                            SettingsRepository.ISLAND_NOTIF_HUB_ORDER_PRIORITY -> priorityLabel
                            SettingsRepository.ISLAND_NOTIF_HUB_ORDER_RECENT -> recentLabel
                            else -> dynamicLabel
                        }
                    },
                    title = R.string.island_notif_hub_order_title,
                    modifier = Modifier.fillMaxWidth(),
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_notifications_off_24,
                    title = stringResource(R.string.island_notif_hub_include_silent),
                    isChecked = viewModel.isIslandNotifHubIncludeSilent.value,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandNotifHubIncludeSilent(it)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_apps_24,
                    title = stringResource(R.string.island_notif_hub_group_apps),
                    isChecked = viewModel.isIslandNotifHubGroupByApps.value,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandNotifHubGroupByApps(it)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_person_24,
                    title = stringResource(R.string.island_notif_hub_chat_pictures),
                    isChecked = viewModel.isIslandNotifHubPreferChatPictures.value,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandNotifHubPreferChatPictures(it)
                    },
                )
                ConfigSliderItem(
                    title = stringResource(R.string.island_hide_icon_notifications),
                    value = viewModel.islandNotifHubIcons.intValue.toFloat(),
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandNotifHubIcons(it.toInt())
                    },
                    valueRange = 0f..8f,
                    increment = 1f,
                    iconRes = R.drawable.rounded_notifications_unread_24,
                    valueFormatter = { if (it.toInt() == 0) autoLabel else it.toInt().toString() },
                )
            }

            Text(
                text = stringResource(R.string.island_notif_hub_overflow_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                val numberLabel = stringResource(R.string.island_notif_hub_overflow_number)
                val dotLabel = stringResource(R.string.island_notif_hub_overflow_dot)
                val noneLabel = stringResource(R.string.island_notif_hub_overflow_none)
                SegmentedPicker(
                    items = com.sameerasw.essentials.island.model.QueueOverflow.entries.toList(),
                    selectedItem = viewModel.islandNotifHubOverflow.value,
                    onItemSelected = { viewModel.setIslandNotifHubOverflow(it) },
                    labelProvider = {
                        when (it) {
                            com.sameerasw.essentials.island.model.QueueOverflow.Dot -> dotLabel
                            com.sameerasw.essentials.island.model.QueueOverflow.None -> noneLabel
                            else -> numberLabel
                        }
                    },
                    title = R.string.island_notif_hub_overflow_title,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
