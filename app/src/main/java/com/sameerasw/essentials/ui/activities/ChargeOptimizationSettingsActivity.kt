package com.sameerasw.essentials.ui.activities

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.EssentialsTheme
import com.sameerasw.essentials.utils.HapticUtil

class ChargeOptimizationSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: com.sameerasw.essentials.viewmodels.MainViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel()
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.check(context)
            }
            val isPitchBlackThemeEnabled by viewModel.isPitchBlackThemeEnabled
            EssentialsTheme(pitchBlackTheme = isPitchBlackThemeEnabled) {
                ChargeOptimizationSettingsOverlay(onDismiss = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargeOptimizationSettingsOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val settingsRepository = remember {
        com.sameerasw.essentials.data.repository.SettingsRepository(context)
    }

    var enableAdaptive by remember {
        mutableStateOf(settingsRepository.getBoolean("charge_opt_toggle_adaptive", true))
    }
    var enableLimit by remember {
        mutableStateOf(settingsRepository.getBoolean("charge_opt_toggle_limit", true))
    }
    var enableDeactivated by remember {
        mutableStateOf(settingsRepository.getBoolean("charge_opt_toggle_deactivated", true))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_battery_android_frame_shield_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.tile_charge_optimization),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = stringResource(R.string.charge_opt_long_press_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Text(
                text = stringResource(R.string.charge_opt_long_press_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            val items = listOf("deactivated", "adaptive", "limit")
            val selectedItems = remember(enableDeactivated, enableAdaptive, enableLimit) {
                mutableSetOf<String>().apply {
                    if (enableDeactivated) add("deactivated")
                    if (enableAdaptive) add("adaptive")
                    if (enableLimit) add("limit")
                }.toSet()
            }

            RoundedCardContainer {
                com.sameerasw.essentials.ui.components.pickers.MultiSegmentedPicker(
                    items = items,
                    selectedItems = selectedItems,
                    onItemsSelected = { newSelection ->
                        if (newSelection.size >= 2) {
                            enableDeactivated = newSelection.contains("deactivated")
                            enableAdaptive = newSelection.contains("adaptive")
                            enableLimit = newSelection.contains("limit")

                            settingsRepository.putBoolean("charge_opt_toggle_deactivated", enableDeactivated)
                            settingsRepository.putBoolean("charge_opt_toggle_adaptive", enableAdaptive)
                            settingsRepository.putBoolean("charge_opt_toggle_limit", enableLimit)
                        }
                    },
                    labelProvider = { item ->
                        when (item) {
                            "deactivated" -> context.getString(R.string.deactivated)
                            "adaptive" -> context.getString(R.string.adaptive_charging)
                            "limit" -> context.getString(R.string.limit_to_80)
                            else -> ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    allowEmpty = false
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
