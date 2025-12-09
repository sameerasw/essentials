package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EdgeLightingSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    // App selection state
    var selectedApps by remember { mutableStateOf<List<NotificationApp>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }


    // Load apps when composable is first shown
    LaunchedEffect(Unit) {
        isLoadingApps = true
        try {
            // Load saved selections first (fast operation)
            val savedSelections = viewModel.loadEdgeLightingSelectedApps(context)

            // Load all installed apps (heavy operation on background thread)
            val allApps = AppUtil.getInstalledApps(context)

            // If no apps are saved yet, initialize with all apps enabled
            val finalSelections = if (savedSelections.isEmpty()) {
                val initialSelections = allApps.map { AppSelection(it.packageName, true) }
                // Save in background to avoid blocking UI
                withContext(Dispatchers.IO) {
                    viewModel.saveEdgeLightingSelectedApps(context, allApps)
                }
                initialSelections
            } else {
                savedSelections
            }

            // Merge saved preferences with installed apps
            selectedApps = AppUtil.mergeWithSavedApps(allApps, finalSelections)
        } catch (e: Exception) {
            android.util.Log.e("EdgeLightingSettingsUI", "Error loading apps: ${e.message}")
            // Handle error - maybe show empty state or error message
        } finally {
            isLoadingApps = false
        }
    }

    // Filter to only show downloaded apps
    val filteredApps = selectedApps.filter { !it.isSystemApp }

    // Corner radius state (default: 20 DP to match OverlayHelper.CORNER_RADIUS_DP)
    var cornerRadiusDp by remember { mutableStateOf(viewModel.loadEdgeLightingCornerRadius(context).toFloat()) }
    var strokeThicknessDp by remember { mutableStateOf(viewModel.loadEdgeLightingStrokeThickness(context).toFloat()) }
    val coroutineScope = rememberCoroutineScope()

    // Cleanup overlay when composable is destroyed (activity paused/closed/destroyed)
    DisposableEffect(Unit) {
        onDispose {
            // Remove any ongoing preview overlay when the composable is disposed
            viewModel.removePreviewOverlay(context)
        }
    }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {

        Button(onClick = {
            HapticUtil.performVirtualKeyHaptic(view)
            viewModel.triggerEdgeLighting(context)
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(painter = painterResource(id = R.drawable.rounded_play_arrow_24), contentDescription = null)
            Text("Preview")
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {

            Text(
                text = "Corner radius",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = cornerRadiusDp,
                onValueChange = { newValue ->
                    cornerRadiusDp = newValue
                    HapticUtil.performSliderHaptic(view)
                    // Show preview overlay while dragging
                    viewModel.triggerEdgeLightingWithRadiusAndThickness(context, newValue.toInt(), strokeThicknessDp.toInt())
                },
                onValueChangeFinished = {
                    // Save the corner radius
                    viewModel.saveEdgeLightingCornerRadius(context, cornerRadiusDp.toInt())
                    // Wait 5 seconds then remove preview overlay
                    coroutineScope.launch {
                        delay(5000)
                        viewModel.removePreviewOverlay(context)
                    }
                },
                valueRange = 0f..50f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Stroke Thickness Slider Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Stroke thickness",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = strokeThicknessDp,
                onValueChange = { newValue ->
                    strokeThicknessDp = newValue
                    HapticUtil.performSliderHaptic(view)
                    // Show preview overlay while dragging
                    viewModel.triggerEdgeLightingWithRadiusAndThickness(context, cornerRadiusDp.toInt(), newValue.toInt())
                },
                onValueChangeFinished = {
                    // Save the stroke thickness
                    viewModel.saveEdgeLightingStrokeThickness(context, strokeThicknessDp.toInt())
                    // Wait 5 seconds then remove preview overlay
                    coroutineScope.launch {
                        delay(5000)
                        viewModel.removePreviewOverlay(context)
                    }
                },
                valueRange = 1f..20f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Downloaded Apps Section
        Text(
            text = "Downloaded Apps",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {

            if (isLoadingApps) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    LoadingIndicator()
                }
            } else {
                    filteredApps.sortedBy { it.appName.lowercase() }.forEach { app ->
                        AppToggleItem(
                            app = app,
                            isChecked = app.isEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.updateEdgeLightingAppEnabled(context, app.packageName, isChecked)
                                // Update local state
                                selectedApps = selectedApps.map {
                                    if (it.packageName == app.packageName) it.copy(isEnabled = isChecked) else it
                                }
                            }
                        )
                    }
            }
        }

        // Invert Selection Button
        OutlinedButton(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                // Invert selection for all downloaded apps
                filteredApps.forEach { app ->
                    val newEnabled = !app.isEnabled
                    viewModel.updateEdgeLightingAppEnabled(context, app.packageName, newEnabled)
                }
                // Update local state
                selectedApps = selectedApps.map { app ->
                    if (!app.isSystemApp) app.copy(isEnabled = !app.isEnabled) else app
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Invert Selection")
        }
    }
}

@Composable
fun AppToggleItem(
    app: NotificationApp,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceBright,
                shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.size(2.dp))
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = app.appName,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.size(2.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = { checked ->
                HapticUtil.performVirtualKeyHaptic(view)
                onCheckedChange(checked)
            }
        )
    }
}