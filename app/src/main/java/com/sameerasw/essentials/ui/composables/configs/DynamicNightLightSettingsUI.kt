package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DynamicNightLightSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    // App selection state
    // App selection state
    var selectedApps by remember { mutableStateOf<List<NotificationApp>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    
    // Load apps when composable is first shown
    LaunchedEffect(Unit) {
        isLoadingApps = true
        withContext(Dispatchers.IO) {
            try {
                val savedSelections = viewModel.loadDynamicNightLightSelectedApps(context)
                val allApps = AppUtil.getInstalledApps(context)

                val finalSelections = if (savedSelections.isEmpty()) {
                    // For Dynamic Night Light, keep all apps off by default
                    val initialSelections = allApps.map { AppSelection(it.packageName, false) }
                    // Save is already on IO here since we are wrapped
                    viewModel.saveDynamicNightLightSelectedApps(context, allApps.map { it.copy(isEnabled = false) })
                    initialSelections
                } else {
                    savedSelections
                }

                val merged = AppUtil.mergeWithSavedApps(allApps, finalSelections)
                withContext(Dispatchers.Main) {
                  selectedApps = merged
                }
            } catch (e: Exception) {
                android.util.Log.e("DynamicNightLightSettingsUI", "Error loading apps: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    isLoadingApps = false
                }
            }
        }
    }

    val filteredApps = selectedApps.filter { 
        !it.isSystemApp && (searchQuery.isEmpty() || it.appName.contains(searchQuery, ignoreCase = true))
    }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {

        RoundedCardContainer(
            modifier = Modifier.padding(top = 8.dp),
            spacing = 2.dp,
            cornerRadius = 24.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceBright
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_nightlight_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Dynamic Night Light",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Switch(
                    checked = viewModel.isDynamicNightLightEnabled.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setDynamicNightLightEnabled(checked, context)
                    }
                )
            }
        }

        Text(
            text = "Apps that toggle off night light",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Search Bar
        androidx.compose.material3.OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            placeholder = { Text("Search apps") },
            leadingIcon = { 
                Icon(
                    painter = painterResource(id = R.drawable.rounded_search_24),
                    contentDescription = "Search"
                ) 
            },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
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
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    LoadingIndicator()
                }
            } else {
                if (filteredApps.isEmpty()) {
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No apps found" else "No apps available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    filteredApps.sortedBy { it.appName.lowercase() }.forEach { app ->
                        AppToggleItem(
                            app = app,
                            isChecked = app.isEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.updateDynamicNightLightAppEnabled(context, app.packageName, isChecked)
                                selectedApps = selectedApps.map {
                                    if (it.packageName == app.packageName) it.copy(isEnabled = isChecked) else it
                                }
                            }
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                filteredApps.forEach { app ->
                    val newEnabled = !app.isEnabled
                    viewModel.updateDynamicNightLightAppEnabled(context, app.packageName, newEnabled)
                }
                selectedApps = selectedApps.map { app ->
                    // Only toggle if it's in the filtered list (i.e. visible)
                    if (filteredApps.any { it.packageName == app.packageName }) {
                         app.copy(isEnabled = !app.isEnabled)
                    } else {
                        app
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Invert Selection")
        }
    }
}
