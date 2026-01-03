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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.essentials.R
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
                }
            } catch (e: Exception) {
                android.util.Log.e("AppSelectionSheet", "Error loading apps: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    isLoadingApps = false
                }
            }
        }
    }

    val filteredApps = selectedApps.filter {
        !it.isSystemApp && (searchQuery.isEmpty() || it.appName.contains(searchQuery, ignoreCase = true))
    }.sortedWith(compareByDescending<NotificationApp> { it.isEnabled }.thenBy { it.appName.lowercase() })

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Select Apps",
                style = MaterialTheme.typography.headlineSmall
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
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

            // Invert Button
            OutlinedButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    val updatedList = selectedApps.map { app ->
                        if (!app.isSystemApp) app.copy(isEnabled = !app.isEnabled) else app
                    }
                    selectedApps = updatedList
                    scope.launch(Dispatchers.IO) {
                        onSaveApps(context, updatedList.map { AppSelection(it.packageName, it.isEnabled) })
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_invert_colors_24),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Invert selection")
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
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    val updatedList = selectedApps.map {
                                        if (it.packageName == app.packageName) it.copy(isEnabled = !it.isEnabled) else it
                                    }
                                    selectedApps = updatedList
                                    scope.launch(Dispatchers.IO) {
                                        onSaveApps(context, updatedList.map { AppSelection(it.packageName, it.isEnabled) })
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = app.icon.toBitmap().asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                contentScale = ContentScale.Fit
                            )
                            
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp)
                            )
                            
                            Switch(
                                checked = app.isEnabled,
                                onCheckedChange = { isChecked ->
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    val updatedList = selectedApps.map {
                                        if (it.packageName == app.packageName) it.copy(isEnabled = isChecked) else it
                                    }
                                    
                                    // If toggled via switch, update specific app then save all
                                    updatedList.find { it.packageName == app.packageName }?.let {
                                        onAppToggle?.invoke(context, it.packageName, it.isEnabled)
                                    }
                                    
                                    selectedApps = updatedList
                                    scope.launch(Dispatchers.IO) {
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
}
