package com.sameerasw.essentials.ui.components.sheets

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.utils.RefreshRateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerAppRefreshRateSettingsSheet(
    packageName: String,
    currentRate: Float,
    isFixed: Boolean,
    landscapeRate: Float?,
    onlyOnMediaPlaying: Boolean,
    onSave: (Float, Boolean, Float?, Boolean) -> Unit,
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var appInfo by remember { mutableStateOf<NotificationApp?>(null) }
    
    val rates = remember { RefreshRateUtils.getSupportedRefreshRates(context) }
    var selectedRate by remember { mutableStateOf(if (currentRate <= 0f) (rates.lastOrNull() ?: 120f) else currentRate) }
    var selectedIsFixed by remember { mutableStateOf(isFixed) }

    var useLandscapeRate by remember { mutableStateOf(landscapeRate != null) }
    var selectedLandscapeRate by remember { mutableStateOf(landscapeRate ?: rates.lastOrNull() ?: 120f) }
    var selectedOnlyOnMediaPlaying by remember { mutableStateOf(onlyOnMediaPlaying) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            val app = AppUtil.getAppsByPackageNames(context, listOf(packageName)).firstOrNull()
            withContext(Dispatchers.Main) {
                appInfo = app
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Title
            Text(
                text = stringResource(R.string.refresh_rate_per_app_select_rate),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // App Header
            appInfo?.let { app ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } ?: Spacer(modifier = Modifier.height(110.dp))

            // Refresh Rate Selection, Fixed Mode & Landscape option
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RoundedCardContainer(
                    spacing = 0.dp,
                    cornerRadius = 24.dp
                ) {
                    SegmentedPicker(
                        items = rates,
                        selectedItem = selectedRate,
                        onItemSelected = { selectedRate = it },
                        labelProvider = { "${it.toInt()} Hz" },
                        modifier = Modifier.fillMaxWidth(),
                        cornerShape = CornerSize(24.dp)
                    )
                }

                RoundedCardContainer(
                    spacing = 0.dp,
                    cornerRadius = 24.dp
                ) {
                    IconToggleItem(
                        iconRes = R.drawable.rounded_shutter_speed_24,
                        title = stringResource(R.string.refresh_rate_per_app_fixed_toggle),
                        description = stringResource(R.string.refresh_rate_per_app_fixed_toggle_desc),
                        isChecked = selectedIsFixed,
                        onCheckedChange = { selectedIsFixed = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                RoundedCardContainer(
                    spacing = 0.dp,
                    cornerRadius = 24.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        IconToggleItem(
                            iconRes = R.drawable.rounded_mobile_rotate_24,
                            title = stringResource(R.string.refresh_rate_per_app_landscape_toggle),
                            description = stringResource(R.string.refresh_rate_per_app_landscape_toggle_desc),
                            isChecked = useLandscapeRate,
                            onCheckedChange = { useLandscapeRate = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (useLandscapeRate) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SegmentedPicker(
                                    items = rates,
                                    selectedItem = selectedLandscapeRate,
                                    onItemSelected = { selectedLandscapeRate = it },
                                    labelProvider = { "${it.toInt()} Hz" },
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerShape = CornerSize(18.dp)
                                )
                                IconToggleItem(
                                    iconRes = R.drawable.round_play_arrow_24,
                                    title = stringResource(R.string.refresh_rate_per_app_only_media_toggle),
                                    description = stringResource(R.string.refresh_rate_per_app_only_media_toggle_desc),
                                    isChecked = selectedOnlyOnMediaPlaying,
                                    onCheckedChange = { selectedOnlyOnMediaPlaying = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Delete Button (only if there was an existing config, i.e., currentRate > 0)
                if (currentRate > 0f) {
                    OutlinedButton(
                        onClick = {
                            onDelete()
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Save Button
                Button(
                    onClick = {
                        onSave(
                            selectedRate,
                            selectedIsFixed,
                            if (useLandscapeRate) selectedLandscapeRate else null,
                            if (useLandscapeRate) selectedOnlyOnMediaPlaying else false
                        )
                        onDismissRequest()
                    },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
