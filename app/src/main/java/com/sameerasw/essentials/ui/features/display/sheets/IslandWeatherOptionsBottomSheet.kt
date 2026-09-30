package com.sameerasw.essentials.ui.features.display.sheets

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandWeatherOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings = remember { SettingsRepository(context) }
    var mode by remember { mutableStateOf(settings.getIslandWeatherMode()) }
    var peekAlerts by remember { mutableStateOf(settings.isIslandWeatherPeekAlertsEnabled()) }

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.island_weather_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                SegmentedPicker(
                    items = listOf(
                        SettingsRepository.ISLAND_WEATHER_MODE_BRIEF,
                        SettingsRepository.ISLAND_WEATHER_MODE_COMPACT,
                        SettingsRepository.ISLAND_WEATHER_MODE_ALERTS,
                    ),
                    selectedItem = mode,
                    onItemSelected = {
                        mode = it
                        settings.setIslandWeatherMode(it)
                    },
                    labelProvider = {
                        context.getString(
                            when (it) {
                                SettingsRepository.ISLAND_WEATHER_MODE_COMPACT -> R.string.island_weather_mode_always
                                SettingsRepository.ISLAND_WEATHER_MODE_ALERTS -> R.string.island_weather_mode_alerts
                                else -> R.string.island_weather_mode_brief
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    title = R.string.island_weather_mode_title,
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_warning_24,
                    title = stringResource(R.string.island_weather_peek_alerts_title),
                    isChecked = peekAlerts,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        peekAlerts = it
                        settings.setIslandWeatherPeekAlertsEnabled(it)
                    },
                )
            }

            IslandLauncherOnlyToggle(SettingsRepository.KEY_ISLAND_WEATHER_LAUNCHER_ONLY)

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            HapticUtil.performVirtualKeyHaptic(view)
                            context.startActivity(Intent(context, FeatureSettingsActivity::class.java).putExtra("feature", "Weather").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.rounded_cloud_24), null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            stringResource(R.string.island_weather_open_settings),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(painterResource(R.drawable.rounded_arrow_forward_24), null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
