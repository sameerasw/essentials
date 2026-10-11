/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Freeze
 * File: FreezeSettingsUI.kt
 * Description: UI component and settings composable for Freeze feature domain.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.FreezeMode
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.cards.AppToggleItem
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FreezeSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightKey: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var isAppSelectionSheetOpen by remember { mutableStateOf(false) }
    var showPermissionSheet by remember { mutableStateOf(false) }
    var showModeWarningResult by remember { mutableStateOf(false) }
    var permissionsToRequest by remember { mutableStateOf<List<String>>(emptyList()) }

    val isShizukuAvailable by viewModel.isShizukuAvailable
    val isShizukuPermissionGranted by viewModel.isShizukuPermissionGranted
    val pickedApps by viewModel.freezePickedApps

    var tagToEdit by remember { mutableStateOf<com.sameerasw.essentials.domain.model.AppTag?>(null) }
    var isTagEditorSheetOpen by remember { mutableStateOf(false) }

    var isMenuExpanded by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 2 })
    LaunchedEffect(viewModel.freezeMode.intValue) {
        pagerState.animateScrollToPage(viewModel.freezeMode.intValue)
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            uri?.let {
                try {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        viewModel.exportFreezeApps(stream)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri?.let {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        viewModel.importFreezeApps(context, stream)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    remember { MutableInteractionSource() }
    remember { MutableInteractionSource() }
    remember { MutableInteractionSource() }

    val isAccessibilityEnabled by viewModel.isAccessibilityEnabled
    var initialEnabledPackageNames by remember { mutableStateOf<Set<String>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshFreezePickedApps(context)
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_section_app_control),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .highlight(highlightKey == "freeze_all_manual"),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceBright,
                                shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                            ).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Freeze Button
                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.freezeAllAuto(context)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isShizukuAvailable && isShizukuPermissionGranted,
                        shape = ButtonDefaults.shape, // Keep default look
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_mode_cool_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            stringResource(R.string.action_freeze),
                            fontSize = dimensionResource(R.dimen.font_small).value.sp,
                        )
                    }

                    // Unfreeze Button
                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.unfreezeAllAuto(context)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isShizukuAvailable && isShizukuPermissionGranted,
                        shape = ButtonDefaults.shape,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_mode_cool_off_24),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            stringResource(R.string.action_unfreeze),
                            fontSize = dimensionResource(R.dimen.font_small).value.sp,
                        )
                    }

                    // More Menu Button
                    IconButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            isMenuExpanded = true
                        },
                        enabled = isShizukuAvailable && isShizukuPermissionGranted,
                        modifier = Modifier.size(dimensionResource(R.dimen.button_normal)),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_more_vert_24),
                            contentDescription = stringResource(R.string.content_desc_more_options),
                        )

                        SegmentedDropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                        ) {
                            SegmentedDropdownMenuItem(
                                text = { Text(stringResource(R.string.action_freeze_all)) },
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.freezeAllManual(context)
                                    isMenuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_mode_cool_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                            SegmentedDropdownMenuItem(
                                text = { Text(stringResource(R.string.action_unfreeze_all)) },
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.unfreezeAllManual(context)
                                    isMenuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_mode_cool_off_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                            SegmentedDropdownMenuItem(
                                text = { Text(stringResource(R.string.action_export_freeze)) },
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    exportLauncher.launch("freeze_apps_backup.json")
                                    isMenuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_arrow_warm_up_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                            SegmentedDropdownMenuItem(
                                text = { Text(stringResource(R.string.action_import_freeze)) },
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    importLauncher.launch(arrayOf("application/json"))
                                    isMenuExpanded = false
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_arrow_cool_down_24),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            FeatureCard(
                title = R.string.freeze_pick_apps_title,
                description = R.string.freeze_pick_apps_desc,
                iconRes = R.drawable.rounded_app_registration_24,
                isEnabled = true,
                showToggle = false,
                hasMoreSettings = true,
                onToggle = {},
                onClick = { isAppSelectionSheetOpen = true },
                modifier = Modifier.highlight(highlightKey == "freeze_selected_apps"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_mode_cool_24,
                title = stringResource(R.string.freeze_show_in_launcher_title),
                subtitle = stringResource(R.string.freeze_show_in_launcher_desc),
                isChecked = viewModel.isFreezeShowInLauncherEnabled.value,
                onCheckedChange = { enabled ->
                    viewModel.setFreezeShowInLauncherEnabled(enabled, context)
                },
                enabled = true,
            )
        }

        Text(
            text = stringResource(R.string.freeze_mode_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer {
            SegmentedPicker(
                items = FreezeMode.entries,
                selectedItem = FreezeMode.fromInt(viewModel.freezeMode.intValue),
                onItemSelected = { mode ->
                    if (viewModel.freezeMode.intValue != mode.value) {
                        if (viewModel.anyAppsCurrentlyFrozen(context)) {
                            showModeWarningResult = true
                        } else {
                            HapticUtil.performVirtualKeyHaptic(view)
                            viewModel.setFreezeMode(mode.value, context)
                        }
                    }
                },
                labelProvider = { mode ->
                    when (mode) {
                        FreezeMode.FREEZE -> context.getString(R.string.freeze_mode_freeze)
                        FreezeMode.SUSPEND -> context.getString(R.string.freeze_mode_suspend)
                    }
                },
                iconProvider = { mode ->
                    Icon(
                        painter =
                            painterResource(
                                id =
                                    when (mode) {
                                        FreezeMode.FREEZE -> R.drawable.rounded_mode_cool_24
                                        FreezeMode.SUSPEND -> R.drawable.rounded_pause_24
                                    },
                            ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceBright,
                        ).padding(8.dp),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(20.dp),
                                ).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (page == 0) {
                            Text(
                                text = stringResource(R.string.freeze_mode_description_freeze_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.freeze_mode_description_freeze_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.freeze_mode_description_freeze_warning),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.freeze_mode_description_suspend_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(R.string.freeze_mode_description_suspend_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.freeze_mode_description_suspend_footer),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                // Pagination Indicators
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(2) { iteration ->
                        val isActive = pagerState.currentPage == iteration
                        val color by animateColorAsState(
                            targetValue =
                                if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                },
                            label = "dotColor",
                        )

                        Box(
                            modifier =
                                Modifier
                                    .padding(4.dp)
                                    .size(if (isActive) 8.dp else 6.dp)
                                    .background(color, CircleShape),
                        )
                    }
                }
            }
        }

        // Tags
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.freeze_tags_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    tagToEdit = null
                    isTagEditorSheetOpen = true
                },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_add_24),
                    contentDescription = stringResource(R.string.action_add_tag),
                )
            }
        }

        val tags by viewModel.freezeTags
        RoundedCardContainer(spacing = 2.dp) {
            if (tags.isEmpty()) {
                Text(
                    text = stringResource(R.string.freeze_tags_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                tags.forEach { tag ->
                    val color =
                        try {
                            androidx.compose.ui.graphics
                                .Color(android.graphics.Color.parseColor(tag.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                    val iconResId =
                        context.resources.getIdentifier(
                            tag.iconName,
                            "drawable",
                            context.packageName,
                        )

                    androidx.compose.material3.ListItem(
                        leadingContent = {
                            val richColor =
                                androidx.compose.runtime.remember(color) {
                                    com.sameerasw.essentials.utils.ColorUtil
                                        .toRichColor(color)
                                }
                            Box(
                                modifier =
                                    Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(richColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter =
                                        painterResource(
                                            id = if (iconResId != 0) iconResId else R.drawable.rounded_interests_24,
                                        ),
                                    contentDescription = null,
                                    tint = richColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    tagToEdit = tag
                                    isTagEditorSheetOpen = true
                                },
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_edit_24),
                                    contentDescription = stringResource(R.string.action_update_tag),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        colors =
                            androidx.compose.material3.ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.extraSmall),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            if (tag.neverAutoFreeze) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_lock_clock_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            IconToggleItem(
                iconRes = R.drawable.rounded_palette_24,
                title = stringResource(R.string.freeze_tag_color_coded_title),
                description = stringResource(R.string.freeze_tag_color_coded_desc),
                isChecked = viewModel.isFreezeTagColorCodedEnabled.value,
                onCheckedChange = { isChecked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setFreezeTagColorCoded(isChecked)
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .highlight(
                            enabled = highlightKey == "freeze_tag_color_coded_enabled",
                        ),
            )
        }

        Text(
            text = stringResource(R.string.settings_section_automation),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_lock_clock_24,
                title = stringResource(R.string.freeze_when_locked_title),
                isChecked = viewModel.isFreezeWhenLockedEnabled.value,
                onCheckedChange = { enabled ->
                    if (enabled && !isAccessibilityEnabled) {
                        permissionsToRequest = listOf("ACCESSIBILITY")
                        showPermissionSheet = true
                    } else {
                        viewModel.setFreezeWhenLockedEnabled(enabled, context)
                    }
                },
                enabled = true,
                modifier = Modifier.highlight(highlightKey == "freeze_when_locked_enabled"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_app_badging_24,
                title = stringResource(R.string.freeze_dont_freeze_active_apps_title),
                isChecked = viewModel.isFreezeDontFreezeActiveAppsEnabled.value,
                onCheckedChange = { enabled ->
                    val missing = mutableListOf<String>()
                    if (!PermissionUtils.hasUsageStatsPermission(context)) missing.add("USAGE_STATS")
                    if (!PermissionUtils.hasNotificationListenerPermission(context)) missing.add("NOTIFICATION_LISTENER")

                    if (enabled && missing.isNotEmpty()) {
                        permissionsToRequest = missing
                        showPermissionSheet = true
                    } else {
                        viewModel.setFreezeDontFreezeActiveAppsEnabled(enabled, context)
                    }
                },
                enabled = true,
                modifier = Modifier.highlight(highlightKey == "freeze_dont_freeze_active_apps"),
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceBright,
                            shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                        ).padding(horizontal = 32.dp, vertical = 18.dp)
                        .highlight(highlightKey == "freeze_lock_delay_index"),
            ) {
                Text(
                    text = stringResource(R.string.freeze_delay_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                        if (viewModel.isFreezeWhenLockedEnabled.value) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.38f,
                            )
                        },
                )

                val labels =
                    listOf(
                        stringResource(R.string.delay_immediate),
                        stringResource(R.string.delay_1m),
                        stringResource(R.string.delay_5m),
                        stringResource(R.string.delay_15m),
                        stringResource(R.string.delay_manual),
                    )
                Slider(
                    value = viewModel.freezeLockDelayIndex.intValue.toFloat(),
                    onValueChange = { viewModel.setFreezeLockDelayIndex(it.toInt(), context) },
                    valueRange = 0f..4f,
                    steps = 3,
                    enabled = viewModel.isFreezeWhenLockedEnabled.value,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    labels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        val isFreezePickedAppsLoading by viewModel.isFreezePickedAppsLoading

        if (isFreezePickedAppsLoading) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                LoadingIndicator()
            }
        } else if (pickedApps.isNotEmpty()) {
            if (initialEnabledPackageNames == null) {
                initialEnabledPackageNames =
                    pickedApps.filter { it.isEnabled }.map { it.packageName }.toSet()
            }

            val sortedApps =
                remember(pickedApps, initialEnabledPackageNames) {
                    val allowed = initialEnabledPackageNames ?: emptySet()
                    pickedApps.sortedWith(
                        compareByDescending<com.sameerasw.essentials.domain.model.NotificationApp> {
                            allowed.contains(
                                it.packageName,
                            )
                        }.thenBy { it.appName.lowercase() },
                    )
                }

            Text(
                text = stringResource(R.string.freeze_auto_freeze_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RoundedCardContainer(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val appTagMap by viewModel.freezeAppTagMap
                val neverAutoFreezeTagIds = tags.filter { it.neverAutoFreeze }.map { it.id }.toSet()

                sortedApps.forEach { app ->
                    val appTagIds = appTagMap[app.packageName] ?: emptyList()
                    val isLockedByTag = appTagIds.any { neverAutoFreezeTagIds.contains(it) }

                    AppToggleItem(
                        icon = app.icon,
                        title = app.appName,
                        isChecked = app.isEnabled && !isLockedByTag,
                        enabled = !isLockedByTag,
                        onCheckedChange = { isChecked ->
                            if (!isLockedByTag) {
                                viewModel.updateFreezeAppAutoFreeze(
                                    context,
                                    app.packageName,
                                    isChecked,
                                )
                            }
                        },
                    )
                }
            }
        }

        if (isTagEditorSheetOpen) {
            com.sameerasw.essentials.ui.core.sheets.FreezeTagEditorSheet(
                tagToEdit = tagToEdit,
                onDismissRequest = { isTagEditorSheetOpen = false },
                onSave = { tag ->
                    if (tagToEdit == null) {
                        viewModel.addFreezeTag(context, tag)
                    } else {
                        viewModel.updateFreezeTag(context, tag)
                    }
                },
                onDelete = { tagId ->
                    viewModel.deleteFreezeTag(context, tagId)
                },
            )
        }

        Text(
            text = stringResource(R.string.freeze_automation_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.freeze_warning),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Import & Export Buttons
        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceBright,
                            shape = MaterialTheme.shapes.extraSmall,
                        ).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        exportLauncher.launch("freeze_apps_backup.json")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_arrow_warm_up_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.action_export_freeze),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.size(ButtonGroupDefaults.ConnectedSpaceBetween))

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        importLauncher.launch(arrayOf("application/json"))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_arrow_cool_down_24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.action_import_freeze),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (isAppSelectionSheetOpen) {
            AppSelectionSheet(
                restrictSystemApps = !viewModel.isEnableUnsupportedFeatures.value,
                showInvertSelection = false,
                onDismissRequest = { isAppSelectionSheetOpen = false },
                onLoadApps = { viewModel.loadFreezeSelectedApps(it) },
                onSaveApps = { ctx, apps -> viewModel.saveFreezeSelectedApps(ctx, apps) },
                onAppToggle = { ctx, pkg, enabled ->
                    viewModel.updateFreezeAppEnabled(
                        ctx,
                        pkg,
                        enabled,
                    )
                },
            )
        }

        if (showPermissionSheet) {
            val missingPermissions =
                permissionsToRequest.filter { key ->
                    when (key) {
                        "ACCESSIBILITY" -> !isAccessibilityEnabled
                        "USAGE_STATS" -> !PermissionUtils.hasUsageStatsPermission(context)
                        "NOTIFICATION_LISTENER" ->
                            !PermissionUtils.hasNotificationListenerPermission(
                                context,
                            )

                        else -> false
                    }
                }

            if (missingPermissions.isNotEmpty()) {
                PermissionsBottomSheet(
                    onDismissRequest = { showPermissionSheet = false },
                    featureTitle = R.string.feat_freeze_title,
                    permissions =
                        PermissionUIHelper.getPermissionItems(
                            missingPermissions,
                            context,
                            viewModel,
                            context as? android.app.Activity,
                        ),
                )
            } else {
                showPermissionSheet = false
            }
        }

        if (showModeWarningResult) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showModeWarningResult = false },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showModeWarningResult = false
                    }) {
                        Text(stringResource(id = R.string.action_ok))
                    }
                },
                title = { Text(stringResource(id = R.string.warning_title)) },
                text = { Text(stringResource(id = R.string.freeze_mode_warning_desc)) },
            )
        }
    }
}
