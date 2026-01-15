package com.sameerasw.essentials.ui.components.sheets

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.AppToggleItem
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppSelectionSheet(
    onDismissRequest: () -> Unit,
    onLoadApps: suspend (Context) -> List<AppSelection>,
    onSaveApps: suspend (Context, List<AppSelection>) -> Unit,
    onAppToggle: ((Context, String, Boolean) -> Unit)? = null,
    context: Context = LocalContext.current
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedApps by remember { mutableStateOf<List<NotificationApp>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var showSystemApps by remember { mutableStateOf(false) }
    var initialEnabledPackageNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    // Load apps when sheet opens
    LaunchedEffect(Unit) {
        isLoadingApps = true
        withContext(Dispatchers.IO) {
            try {
                // Load saved selections first (fast operation)
                val savedSelections = onLoadApps(context)

                // Load all installed apps (heavy operation on background thread)
                val allApps = AppUtil.getInstalledApps(context)

                val selectionsToMerge = savedSelections.ifEmpty {
                    // Default to all disabled if no preferences found
                    allApps.map { AppSelection(it.packageName, false) }
                }

                val merged = AppUtil.mergeWithSavedApps(allApps, selectionsToMerge)

                withContext(Dispatchers.Main) {
                    selectedApps = merged
                    initialEnabledPackageNames = merged.filter { it.isEnabled }.map { it.packageName }.toSet()
                }
            } catch (e: Exception) {
                android.util.Log.e("AppSelectionSheet", context.getString(R.string.error_loading_apps, e.message ?: ""))
            } finally {
                withContext(Dispatchers.Main) {
                    isLoadingApps = false
                }
            }
        }
    }

    val filteredApps = selectedApps.filter {
        val matchesSearch = searchQuery.isEmpty() || it.appName.contains(searchQuery, ignoreCase = true)
        val isVisible = !it.isSystemApp || showSystemApps || it.isEnabled // Always show if enabled, or if system toggle checks out
        matchesSearch && isVisible
    }.sortedWith(compareByDescending<NotificationApp> { initialEnabledPackageNames.contains(it.packageName) }.thenBy { it.appName.lowercase() })

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.action_select_apps),
                    style = MaterialTheme.typography.headlineSmall
                )
                
                androidx.compose.material3.IconButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        val updatedList = selectedApps.map { app ->
                            val isVisible = !app.isSystemApp || showSystemApps || app.isEnabled
                            if (isVisible) app.copy(isEnabled = !app.isEnabled) else app
                        }
                        selectedApps = updatedList
                        scope.launch(Dispatchers.IO) {
                            onSaveApps(context, updatedList.map { AppSelection(it.packageName, it.isEnabled) })
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_invert_colors_24),
                        contentDescription = stringResource(R.string.action_invert_selection),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.label_search)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_search_24),
                        contentDescription = stringResource(R.string.action_search)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // System Apps Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { 
                        HapticUtil.performVirtualKeyHaptic(view)
                        showSystemApps = !showSystemApps 
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_settings_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.toggle_show_system_apps),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = showSystemApps,
                    onCheckedChange = { 
                        HapticUtil.performVirtualKeyHaptic(view)
                        showSystemApps = it 
                    }
                )
            }

            if (isLoadingApps) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    LoadingIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppToggleItem(
                            icon = app.icon,
                            title = app.appName,
                            isChecked = app.isEnabled,
                            onCheckedChange = { isChecked ->
                                val updatedList = selectedApps.map {
                                    if (it.packageName == app.packageName) it.copy(isEnabled = isChecked) else it
                                }
                                
                                // If toggled via switch, update specific app then save all
                                updatedList.find { it.packageName == app.packageName }?.let {
                                    onAppToggle?.invoke(context, it.packageName, it.isEnabled)
                                }
                                
                                selectedApps = updatedList
                                scope.launch(Dispatchers.IO) {
                                    // Use updatedList here to ensure we save the new state
                                    onSaveApps(context, updatedList.map { AppSelection(it.packageName, it.isEnabled) })
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
