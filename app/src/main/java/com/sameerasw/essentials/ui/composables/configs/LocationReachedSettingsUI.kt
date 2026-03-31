package com.sameerasw.essentials.ui.composables.configs

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.LocationAlarmCard
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.sheets.LocationReachedBottomSheet
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.LocationReachedViewModel
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LocationReachedSettingsUI(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val locationViewModel: LocationReachedViewModel = viewModel()
    val savedAlarms by locationViewModel.savedAlarms
    val activeAlarmId by locationViewModel.activeAlarmId
    val lastTrip by locationViewModel.lastTrip
    val distance by locationViewModel.currentDistance
    val startDistance by locationViewModel.startDistance
    val showBottomSheet by locationViewModel.showBottomSheet
    val isProcessing by locationViewModel.isProcessingCoordinates
    val remainingTimeMinutes by locationViewModel.remainingTimeMinutes
    val view = androidx.compose.ui.platform.LocalView.current

    DisposableEffect(locationViewModel) {
        locationViewModel.startUiTracking()
        onDispose {
            locationViewModel.stopUiTracking()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarHeight + 8.dp,
                bottom = 150.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Top Progress / Last Trip Card
            item {
                val activeAlarm = savedAlarms.find { it.id == activeAlarmId }
                TopStatusCard(
                    activeAlarm = activeAlarm,
                    lastTrip = lastTrip,
                    distance = distance,
                    remainingTimeMinutes = remainingTimeMinutes,
                    startDistance = startDistance,
                    onStop = { locationViewModel.stopTracking() },
                    onStart = { 
                        HapticUtil.performVirtualKeyHaptic(view)
                        locationViewModel.startTracking(it) 
                    }
                )
            }

            // List Header
            if (savedAlarms.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.location_reached_saved_destinations),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Destinations List
            if (savedAlarms.isNotEmpty()) {
                item {
                    RoundedCardContainer(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                            savedAlarms.forEachIndexed { index, alarm ->
                                LocationAlarmCard(
                                    alarm = alarm,
                                    isActive = activeAlarmId == alarm.id,
                                    isAnyTracking = activeAlarmId != null,
                                    onStart = { 
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        locationViewModel.startTracking(alarm.id) 
                                    },
                                    onStop = { 
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        locationViewModel.stopTracking() 
                                    },
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        locationViewModel.setTempAlarm(alarm)
                                        locationViewModel.setShowBottomSheet(true)
                                    }
                                )
                            }
                    }
                }
            }

            if (savedAlarms.isEmpty() && !isProcessing) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.location_reached_no_saved_dest),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Instructional Description
            item {
                Text(
                    text = stringResource(R.string.location_reached_instructional_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Permission Warning
            item {
                val isFSIGranted by mainViewModel.isFullScreenIntentPermissionGranted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !isFSIGranted) {
                    RoundedCardContainer(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        com.sameerasw.essentials.ui.components.cards.IconToggleItem(
                            title = stringResource(R.string.location_reached_fsi_title),
                            description = stringResource(R.string.location_reached_fsi_desc),
                            isChecked = false,
                            onCheckedChange = { mainViewModel.requestFullScreenIntentPermission(context) },
                            iconRes = R.drawable.rounded_info_24,
                            showToggle = false
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showBottomSheet) {
        LocationReachedBottomSheet(
            viewModel = locationViewModel,
            onDismissRequest = { locationViewModel.setShowBottomSheet(false) }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopStatusCard(
    activeAlarm: com.sameerasw.essentials.domain.model.LocationAlarm?,
    lastTrip: com.sameerasw.essentials.domain.model.LocationAlarm?,
    distance: Float?,
    remainingTimeMinutes: Int?,
    startDistance: Float,
    onStop: () -> Unit,
    onStart: (String) -> Unit
) {
    val isTracking = activeAlarm != null
    val displayAlarm = activeAlarm ?: lastTrip

    RoundedCardContainer(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 32.dp,
        containerColor = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isTracking) {
                val context = LocalContext.current
                val iconResId = context.resources.getIdentifier(displayAlarm?.iconResName ?: "round_navigation_24", "drawable", context.packageName)

                Icon(
                    painter = painterResource(id = if (iconResId != 0) iconResId else R.drawable.round_navigation_24),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.location_reached_tracking_now),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayAlarm?.name?.ifEmpty { "Destination" } ?: "Destination",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (distance == null) {
                    LoadingIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val distanceText = distance?.let {
                    if (it < 1000) "${it.toInt()} m" else "%.1f km".format(it / 1000f)
                } ?: stringResource(R.string.location_reached_calculating)

                Text(
                    text = distanceText,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                remainingTimeMinutes?.let { mins ->
                    val etaText = if (mins >= 60) {
                        stringResource(R.string.location_reached_eta_hr_min, mins / 60, mins % 60)
                    } else {
                        stringResource(R.string.location_reached_eta_min, mins)
                    }

                    Text(
                        text = etaText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.location_reached_to_go).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (distance != null && startDistance > 0) {
                    val progress = (1.0f - (distance / startDistance)).coerceIn(0.0f, 1.0f)
                    Spacer(modifier = Modifier.height(24.dp))
                    LinearWavyProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        wavelength = 20.dp,
                        amplitude = { 1.0f }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(painterResource(R.drawable.rounded_close_24), contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.action_stop))
                }
            } else if (lastTrip != null) {
                val context = LocalContext.current
                val iconResId = context.resources.getIdentifier(lastTrip.iconResName, "drawable", context.packageName)

                Icon(
                    painter = painterResource(id = if (iconResId != 0) iconResId else R.drawable.round_navigation_24),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.location_reached_last_trip),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastTrip.name.ifEmpty { "Destination" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onStart(lastTrip.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text(stringResource(R.string.location_reached_restart_btn))
                }
            } else {
                // Completely empty state for top card
                Icon(
                    painter = painterResource(R.drawable.round_navigation_24),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.feat_location_reached_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
