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
import com.sameerasw.essentials.island.service.IslandPreviewRequests
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandNotificationQueueOptionsBottomSheet(
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
                text = stringResource(R.string.island_notif_queue_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                val queueStyle = viewModel.islandNotifQueueStyle.value
                val stackLabel = stringResource(R.string.island_notif_queue_style_stack)
                val overlapLabel = stringResource(R.string.island_notif_queue_style_overlap)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceBright, MaterialTheme.shapes.extraSmall)
                        .padding(top = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.island_notif_queue_style_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Text(
                        text = stringResource(
                            if (queueStyle == SettingsRepository.ISLAND_NOTIF_QUEUE_STYLE_OVERLAP) R.string.island_notif_queue_style_overlap_desc
                            else R.string.island_notif_queue_style_stack_desc,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    SegmentedPicker(
                        items = listOf(
                            SettingsRepository.ISLAND_NOTIF_QUEUE_STYLE_STACK,
                            SettingsRepository.ISLAND_NOTIF_QUEUE_STYLE_OVERLAP,
                        ),
                        selectedItem = queueStyle,
                        onItemSelected = {
                            viewModel.setIslandNotifQueueStyle(it)
                            IslandPreviewRequests.showQueue()
                        },
                        labelProvider = { if (it == SettingsRepository.ISLAND_NOTIF_QUEUE_STYLE_OVERLAP) overlapLabel else stackLabel },
                        title = R.string.island_notif_queue_style_title,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                IconToggleItem(
                    iconRes = R.drawable.rounded_fullscreen_portrait_24,
                    title = stringResource(R.string.island_notif_queue_inline_title),
                    description = stringResource(R.string.island_notif_queue_inline_desc),
                    isChecked = viewModel.isIslandNotifQueueInline.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandNotifQueueInline(checked)
                        IslandPreviewRequests.showQueue()
                    },
                )
            }
        }
    }
}
