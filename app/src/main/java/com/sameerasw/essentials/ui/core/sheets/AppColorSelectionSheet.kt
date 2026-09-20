/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core UI Sheets
 * File: AppColorSelectionSheet.kt
 * Description: Shared bottom sheet for assigning a custom color to individual apps.
 */

package com.sameerasw.essentials.ui.core.sheets

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.ui.core.pickers.ColorSwatchPicker
import com.sameerasw.essentials.utils.AppColorUtil
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppColorSelectionSheet(
    onDismissRequest: () -> Unit,
    title: String = stringResource(R.string.app_colors_title),
    onColorsChanged: (() -> Unit)? = null,
    context: Context = LocalContext.current,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current

    var searchQuery by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<NotificationApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showSystemApps by remember { mutableStateOf(false) }
    var expandedPackage by remember { mutableStateOf<String?>(null) }
    var overrides by remember { mutableStateOf(AppColorUtil.getOverrides(context)) }

    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val installed =
                try {
                    AppUtil.getInstalledApps(context)
                } catch (_: Exception) {
                    emptyList()
                }
            withContext(Dispatchers.Main) {
                apps = installed
                isLoading = false
            }
        }
    }

    val filteredApps =
        apps
            .filter {
                val matchesSearch =
                    searchQuery.isEmpty() || it.appName.contains(searchQuery, ignoreCase = true)
                val hasOverride = overrides.containsKey(it.packageName)
                val isVisible = !it.isSystemApp || showSystemApps || hasOverride
                matchesSearch && isVisible
            }.sortedWith(
                compareByDescending<NotificationApp> { overrides.containsKey(it.packageName) }
                    .thenBy { it.appName.lowercase() },
            )

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                )

                IconButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        AppColorUtil.clearAll(context)
                        overrides = AppColorUtil.getOverrides(context)
                        onColorsChanged?.invoke()
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_refresh_24),
                        contentDescription = stringResource(R.string.app_colors_reset_all),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.label_search)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_search_24),
                        contentDescription = stringResource(R.string.action_search),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            HapticUtil.performVirtualKeyHaptic(view)
                            showSystemApps = !showSystemApps
                        }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_settings_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.toggle_show_system_apps),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Switch(
                    checked = showSystemApps,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showSystemApps = it
                    },
                )
            }

            if (isLoading) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    LoadingIndicator()
                }
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppColorRow(
                            app = app,
                            selectedColor = overrides[app.packageName],
                            isExpanded = expandedPackage == app.packageName,
                            onToggleExpanded = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                expandedPackage =
                                    if (expandedPackage == app.packageName) null else app.packageName
                            },
                            onColorSelected = { color ->
                                AppColorUtil.setOverride(context, app.packageName, color)
                                overrides = AppColorUtil.getOverrides(context)
                                onColorsChanged?.invoke()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppColorRow(
    app: NotificationApp,
    selectedColor: Int?,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onColorSelected: (Int?) -> Unit,
) {
    val context = LocalContext.current
    var brandColor by remember(app.packageName) { mutableStateOf<Int?>(null) }

    LaunchedEffect(app.packageName) {
        AppUtil.getAppBrandColor(context, app.packageName) { brandColor = it }
    }

    val effectiveColor = selectedColor ?: brandColor

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                bitmap = app.icon,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        if (selectedColor != null) {
                            stringResource(R.string.app_colors_custom)
                        } else {
                            stringResource(R.string.app_colors_auto)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            effectiveColor?.let { Color(it) }
                                ?: MaterialTheme.colorScheme.surfaceVariant,
                        ),
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                ColorSwatchPicker(
                    selectedColorHex =
                        selectedColor?.let { String.format("#%06X", 0xFFFFFF and it) } ?: "auto",
                    onColorSelected = { hex ->
                        if (hex.equals("auto", ignoreCase = true)) {
                            onColorSelected(null)
                        } else {
                            onColorSelected(android.graphics.Color.parseColor(hex))
                        }
                    },
                    allowAuto = true,
                    autoColor = brandColor?.let { Color(it) },
                )
            }
        }
    }
}
