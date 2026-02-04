package com.sameerasw.essentials.ui.composables.configs

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.loadingindicator.LoadingIndicator
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.viewmodels.LocationReachedViewModel
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.domain.model.LocationAlarm

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LocationReachedSettingsUI(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val locationViewModel: LocationReachedViewModel = viewModel()
    val alarm by locationViewModel.alarm
    val distance by locationViewModel.currentDistance
    val isProcessing by locationViewModel.isProcessingCoordinates
    val startDistance by locationViewModel.startDistance
    
    DisposableEffect(locationViewModel) {
        locationViewModel.startUiTracking()
        onDispose {
            locationViewModel.stopUiTracking()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isProcessing) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoadingIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.location_reached_processing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (alarm.latitude != 0.0 && alarm.longitude != 0.0) {
            // Destination Set State
            RoundedCardContainer(
                modifier = Modifier,
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceBright,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    if (alarm.isEnabled) {
                        // TRACKING STATE
                        val distanceText = distance?.let {
                            if (it < 1000) stringResource(R.string.location_reached_dist_m, it.toInt()) 
                            else stringResource(R.string.location_reached_dist_km, it / 1000f)
                        } ?: stringResource(R.string.location_reached_calculating)

                        Text(
                            text = stringResource(R.string.location_reached_dist_remaining),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = distanceText,
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (distance != null && startDistance > 0) {
                            val progress = (1.0f - (distance!! / startDistance)).coerceIn(0.0f, 1.0f)
                            Spacer(modifier = Modifier.height(24.dp))

                            LinearWavyProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer,
                                wavelength = 20.dp,
                                amplitude = { 1.0f } // Normalized amplitude
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { locationViewModel.stopTracking() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ) {
                            Icon(painterResource(R.drawable.rounded_pause_24), contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.location_reached_stop_tracking))
                        }

                    } else {
                        // READY STATE (Not Tracking)
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_my_location_24),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.location_reached_dest_ready),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${alarm.latitude}, ${alarm.longitude}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { locationViewModel.startTracking() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ) {
                            Icon(painterResource(R.drawable.rounded_play_arrow_24), contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.location_reached_start_tracking))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Secondary Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val gmmIntentUri = Uri.parse("geo:${alarm.latitude},${alarm.longitude}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Icon(painterResource(R.drawable.rounded_map_24), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.location_reached_view_map))
                        }

                        Button(
                            onClick = { locationViewModel.clearAlarm() },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Icon(painterResource(R.drawable.rounded_delete_24), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.location_reached_clear))
                        }
                    }
                }
            }
        } else {
            // Empty State
            RoundedCardContainer(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_add_location_alt_24),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.location_reached_no_dest),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.location_reached_how_to),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val gmmIntentUri = Uri.parse("geo:0,0?q=")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text(stringResource(R.string.location_reached_open_maps))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.location_reached_radius_title, alarm.radius),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            cornerRadius = 24.dp
        ) {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceBright).padding(16.dp)) {
                Slider(
                    value = alarm.radius.toFloat(),
                    onValueChange = { newVal ->
                        locationViewModel.updateAlarm(alarm.copy(radius = newVal.toInt()))
                    },
                    valueRange = 100f..5000f,
                    steps = 49
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        val isFSIGranted by mainViewModel.isFullScreenIntentPermissionGranted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !isFSIGranted) {
            RoundedCardContainer(
                modifier = Modifier.background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                ),
                cornerRadius = 24.dp
            ) {
                IconToggleItem(
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
}

