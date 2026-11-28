/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Freeze
 * File: FreezeGridUI.kt
 * Description: UI component and settings composable for Freeze feature domain.
 */

package com.sameerasw.essentials.ui.composables

import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.WhatIsThisButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.components.rememberPullTitleHeaderState
import com.sameerasw.essentials.ui.components.PullTitleHeader
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.modifiers.scrollMotionBlur
import com.sameerasw.essentials.ui.state.LocalMenuStateManager
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.FreezeManager
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.utils.ShortcutUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FreezeGridUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onGetStartedClick: (() -> Unit)? = null,
    onAppLaunched: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    searchQuery: String = "",
    searchSubmitSignal: Int = 0,
    onHelpClick: (() -> Unit)? = null,
    tabNameInToolbar: Boolean = false,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val menuState = LocalMenuStateManager.current
    val scope = rememberCoroutineScope()
    val pickedApps by viewModel.freezePickedApps
    val isPickedAppsLoading by viewModel.isFreezePickedAppsLoading

    val frozenStates = remember { mutableStateMapOf<String, Boolean>() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val focusManager = LocalFocusManager.current

    val freezeTags by viewModel.freezeTags
    val freezeAppTagMap by viewModel.freezeAppTagMap
    val isTagColorCodedEnabled by viewModel.isFreezeTagColorCodedEnabled

    var selectedTagId by rememberSaveable { mutableStateOf<String?>(null) }

    val filteredApps =
        remember(pickedApps, searchQuery, selectedTagId, freezeAppTagMap) {
            var list = pickedApps
            val filterTagId = selectedTagId
            if (!filterTagId.isNullOrEmpty()) {
                list =
                    list.filter { app ->
                        val assignedTagIds = freezeAppTagMap[app.packageName] ?: emptyList()
                        assignedTagIds.contains(filterTagId)
                    }
            }
            if (searchQuery.isNotBlank()) {
                list =
                    list.filter { app ->
                        app.appName.contains(searchQuery, ignoreCase = true) ||
                            app.packageName.contains(searchQuery, ignoreCase = true)
                    }
            }
            list
        }

    val tagFilterPackages =
        remember(pickedApps, selectedTagId, freezeAppTagMap) {
            val filterTagId = selectedTagId
            if (filterTagId.isNullOrEmpty()) {
                null
            } else {
                pickedApps
                    .filter { freezeAppTagMap[it.packageName]?.contains(filterTagId) == true }
                    .map { it.packageName }
                    .toSet()
            }
        }

    val bestMatch =
        remember(searchQuery, filteredApps) {
            if (searchQuery.isNotBlank() && filteredApps.isNotEmpty()) filteredApps.first() else null
        }

    var showShellPermissionSheet by remember { mutableStateOf(false) }

    fun ensureShellPermission(): Boolean {
        if (ShellUtils.isAvailable(context) && !ShellUtils.hasPermission(context)) {
            showShellPermissionSheet = true
            return false
        }
        return true
    }

    LaunchedEffect(searchSubmitSignal) {
        if (searchSubmitSignal > 0) {
            bestMatch?.let { app ->
                HapticUtil.performVirtualKeyHaptic(view)
                if (frozenStates[app.packageName] != true || ensureShellPermission()) {
                    viewModel.launchAndUnfreezeApp(context, app.packageName)
                    onAppLaunched?.invoke()
                }
            }
        }
    }

    var resumeCount by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    resumeCount++
                    viewModel.check(context)
                    viewModel.refreshFreezePickedApps(context)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(pickedApps, resumeCount) {
        val states =
            withContext(Dispatchers.IO) {
                pickedApps.associate { app ->
                    app.packageName to FreezeManager.isAppFrozen(context, app.packageName)
                }
            }
        states.forEach { (pkg, frozen) ->
            if (frozenStates[pkg] != frozen) frozenStates[pkg] = frozen
        }
    }

    val runBulk: ((Set<String>?) -> Unit) -> Unit = { action ->
        if (ensureShellPermission()) scope.launch(Dispatchers.IO) {
            action(tagFilterPackages)
            val states =
                pickedApps.associate { app ->
                    app.packageName to FreezeManager.isAppFrozen(context, app.packageName)
                }
            withContext(Dispatchers.Main) { frozenStates.putAll(states) }
        }
    }

    var appForTagAssignment by remember { mutableStateOf<NotificationApp?>(null) }
    var isAppSelectionSheetOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (isPickedAppsLoading && pickedApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        } else if (pickedApps.isEmpty()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_mode_cool_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                )
                Spacer(modifier = Modifier.height(16.dp))
                TranslatableText(
                    stringResId = R.string.msg_no_apps_frozen,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onGetStartedClick != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onGetStartedClick()
                        },
                    ) {
                        TranslatableText(R.string.action_get_started)
                    }
                }
            }
        } else {
            val listState = rememberLazyListState()
            val isMotionBlurEnabled by viewModel.isMotionBlurEnabled
            val headerState = rememberPullTitleHeaderState()
            val frozenCount by remember(pickedApps) {
                derivedStateOf { pickedApps.count { frozenStates[it.packageName] == true } }
            }
            val neverAutoFreezeTagIds =
                remember(freezeTags) { freezeTags.filter { it.neverAutoFreeze }.mapTo(HashSet()) { it.id } }
            val tagColors = remember(freezeTags) { freezeTags.associate { it.id to parseTagColor(it.colorHex) } }
            val gridRows =
                remember(filteredApps, searchQuery) {
                    val entries: List<NotificationApp?> =
                        if (searchQuery.isEmpty()) filteredApps + null else filteredApps
                    entries.chunked(4)
                }

            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(headerState.nestedScrollConnection)
                        .scrollMotionBlur(listState, enabled = isMotionBlurEnabled)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        },
                contentPadding =
                    PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding(),
                    ),
            ) {
                item(key = "header") {
                    PullTitleHeader(
                        title =
                            if (tabNameInToolbar) {
                                stringResource(R.string.freeze_header_caption, frozenCount, pickedApps.size)
                            } else {
                                stringResource(R.string.tab_freeze)
                            },
                        state = headerState,
                        caption = if (tabNameInToolbar) null else stringResource(R.string.freeze_header_caption, frozenCount, pickedApps.size),
                    )
                }

                item(key = "actions") {
                    SubcomposeLayout(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { constraints ->
                        val actions: @Composable (Boolean) -> Unit = { showLabels ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FreezeSplitButton(
                                    iconRes = R.drawable.rounded_mode_cool_24,
                                    text = stringResource(R.string.action_freeze),
                                    menuText = stringResource(R.string.action_freeze_all),
                                    onClick = { runBulk { FreezeManager.freezeAll(context, it) } },
                                    onMenuClick = { runBulk { FreezeManager.freezeAllManual(context, it) } },
                                    showLabel = showLabels,
                                )
                                FreezeSplitButton(
                                    iconRes = R.drawable.rounded_mode_cool_off_24,
                                    text = stringResource(R.string.action_unfreeze),
                                    menuText = stringResource(R.string.action_unfreeze_all),
                                    onClick = { runBulk { FreezeManager.unfreezeAll(context, it) } },
                                    onMenuClick = { runBulk { FreezeManager.unfreezeAllManual(context, it) } },
                                    showLabel = showLabels,
                                )
                                if (onSettingsClick != null) {
                                    FilledTonalIconButton(
                                        onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            onSettingsClick()
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.rounded_settings_24),
                                            contentDescription = stringResource(R.string.label_settings),
                                        )
                                    }
                                }
                            }
                        }
                        val loose = constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity)
                        val full = subcompose("full") { actions(true) }.first().measure(loose)
                        val row = if (full.width <= constraints.maxWidth) full
                        else subcompose("compact") { actions(false) }.first().measure(loose)
                        val width = constraints.maxWidth
                        layout(width, row.height) {
                            row.place(((width - row.width) / 2).coerceAtLeast(0), 0)
                        }
                    }
                }

                if (freezeTags.isNotEmpty()) {
                    item(key = "tags") {
                        LazyRow(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            items(freezeTags) { tag ->
                                val isSelected = selectedTagId == tag.id
                                val color = tagColors[tag.id] ?: MaterialTheme.colorScheme.primary
                                val iconResId =
                                    remember(tag.iconName) {
                                        context.resources.getIdentifier(tag.iconName, "drawable", context.packageName)
                                    }
                                val richColor = remember(color) { ColorUtil.toRichColor(color) }

                                androidx.compose.material3.FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        selectedTagId = if (isSelected) null else tag.id
                                    },
                                    label = { Text(tag.name) },
                                    shape = if (isSelected) CircleShape else androidx.compose.material3.FilterChipDefaults.shape,
                                    leadingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.rounded_check_24),
                                                contentDescription = null,
                                                modifier = Modifier.size(androidx.compose.material3.FilterChipDefaults.IconSize),
                                            )
                                        } else {
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
                                )
                            }
                        }
                    }
                }

                item(key = "grid_gap") { Spacer(modifier = Modifier.height(16.dp)) }

                if (filteredApps.isEmpty() && searchQuery.isNotEmpty()) {
                    item(key = "no_results") {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(vertical = 32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "¯\\_(ツ)_/¯",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TranslatableText(
                                stringResId = R.string.search_no_results,
                                formatArgs = listOf(searchQuery),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                } else {
                    itemsIndexed(
                        items = gridRows,
                        contentType = { _, _ -> "grid_row" },
                    ) { index, rowApps ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = if (index > 0) 4.dp else 0.dp)
                                    .then(if (null in rowApps) Modifier.height(IntrinsicSize.Min) else Modifier),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            rowApps.forEach { app ->
                                if (app == null) {
                                    AddAppsGridItem(
                                        onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            isAppSelectionSheetOpen = true
                                        },
                                        modifier =
                                            Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                    )
                                    return@forEach
                                }
                                val appTagIds = freezeAppTagMap[app.packageName] ?: emptyList()
                                val isLockedByTag = appTagIds.any { it in neverAutoFreezeTagIds }
                                val tagColor =
                                    if (isTagColorCodedEnabled) appTagIds.firstOrNull()?.let { tagColors[it] } else null

                                Box(modifier = Modifier.weight(1f)) {
                                    AppGridItem(
                                        app = app,
                                        isFrozen = frozenStates[app.packageName] ?: false,
                                        isAutoFreezeEnabled = app.isEnabled,
                                        isLockedByTag = isLockedByTag,
                                        tagColor = tagColor,
                                        isHighlighted = (app == bestMatch && searchQuery.isNotEmpty()),
                                        menuState = menuState,
                                        onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            if (frozenStates[app.packageName] != true || ensureShellPermission()) {
                                                viewModel.launchAndUnfreezeApp(
                                                    context,
                                                    app.packageName,
                                                )
                                                onAppLaunched?.invoke()
                                            }
                                        },
                                        onToggleFreeze = {
                                            if (ensureShellPermission()) scope.launch(Dispatchers.IO) {
                                                val isCurrentlyFrozen =
                                                    frozenStates[app.packageName] ?: false
                                                if (isCurrentlyFrozen) {
                                                    FreezeManager.unfreezeApp(
                                                        context,
                                                        app.packageName,
                                                    )
                                                } else {
                                                    FreezeManager.freezeApp(
                                                        context,
                                                        app.packageName,
                                                    )
                                                }
                                                withContext(Dispatchers.Main) {
                                                    frozenStates[app.packageName] =
                                                        !isCurrentlyFrozen
                                                }
                                            }
                                        },
                                        onToggleAutoFreeze = { isAutoFreeze ->
                                            viewModel.updateFreezeAppAutoFreeze(
                                                context,
                                                app.packageName,
                                                isAutoFreeze,
                                            )
                                        },
                                        onAssignTags = {
                                            appForTagAssignment = app
                                        },
                                        onRemove = {
                                            viewModel.updateFreezeAppEnabled(
                                                context,
                                                app.packageName,
                                                false,
                                            )
                                        },
                                    )
                                }
                            }
                            repeat(4 - rowApps.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                onHelpClick?.let { onHelp ->
                    item(key = "help") {
                        WhatIsThisButton(onClick = onHelp, modifier = Modifier.padding(top = 24.dp))
                    }
                }
            }
        }

        if (isAppSelectionSheetOpen) {
            AppSelectionSheet(
                restrictSystemApps = !viewModel.isEnableUnsupportedFeatures.value,
                showInvertSelection = false,
                hideSystemAppsByDefault = true,
                onDismissRequest = { isAppSelectionSheetOpen = false },
                onLoadApps = { viewModel.loadFreezeSelectedApps(it) },
                onSaveApps = { ctx, apps -> viewModel.saveFreezeSelectedApps(ctx, apps) },
                onAppToggle = { ctx, pkg, enabled -> viewModel.updateFreezeAppEnabled(ctx, pkg, enabled) },
            )
        }

        if (showShellPermissionSheet) {
            PermissionsBottomSheet(
                onDismissRequest = {
                    showShellPermissionSheet = false
                    viewModel.check(context)
                },
                featureTitle = R.string.feat_freeze_title,
                permissions =
                    PermissionUIHelper.getPermissionItems(
                        listOf(if (ShellUtils.isRootEnabled(context)) "ROOT" else "SHIZUKU"),
                        context,
                        viewModel,
                    ),
            )
        }

        appForTagAssignment?.let { targetApp ->
            com.sameerasw.essentials.ui.core.sheets.AssignTagsSheet(
                appName = targetApp.appName,
                availableTags = freezeTags,
                assignedTagIds = freezeAppTagMap[targetApp.packageName] ?: emptyList(),
                onDismissRequest = { appForTagAssignment = null },
                onSave = { selectedTagIds ->
                    viewModel.setAppTags(context, targetApp.packageName, selectedTagIds)
                },
            )
        }
    }
}

private fun parseTagColor(colorHex: String?): Color? =
    try {
        colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
    } catch (e: Exception) {
        null
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddAppsGridItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .drawBehind {
                    val stroke = 1.5.dp.toPx()
                    drawRoundRect(
                        color = outline,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(16.dp.toPx() - stroke / 2),
                        style =
                            Stroke(
                                width = stroke,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                            ),
                    )
                }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.rounded_add_24),
            contentDescription = stringResource(R.string.action_add),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AppGridItem(
    app: NotificationApp,
    isFrozen: Boolean,
    isAutoFreezeEnabled: Boolean,
    isLockedByTag: Boolean = false,
    tagColor: Color? = null,
    isHighlighted: Boolean = false,
    menuState: com.sameerasw.essentials.ui.state.MenuStateManager,
    onClick: () -> Unit,
    onToggleFreeze: () -> Unit,
    onToggleAutoFreeze: (Boolean) -> Unit,
    onAssignTags: () -> Unit,
    onRemove: () -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val isBlurred = menuState.activeId != null && menuState.activeId != app.packageName
    val blurRadius = animateDpAsState(
        targetValue = if (isBlurred) 10.dp else 0.dp,
        animationSpec = tween(durationMillis = 500),
        label = "blur",
    )
    val dimAlpha = animateFloatAsState(
        targetValue = if (isBlurred) 0.5f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "alpha",
    )

    DisposableEffect(showMenu) {
        if (showMenu) {
            menuState.activeId = app.packageName
        } else {
            if (menuState.activeId == app.packageName) {
                menuState.activeId = null
            }
        }
        onDispose {
            if (menuState.activeId == app.packageName) {
                menuState.activeId = null
            }
        }
    }

    val frozenColorFilter = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.4f) }) }

    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "borderColorAnimation",
    )

    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val containerColor =
        remember(tagColor, isDarkTheme) {
            if (tagColor == null) {
                null
            } else {
                val rich = ColorUtil.toRichColor(tagColor)
                if (isDarkTheme) {
                    rich.copy(alpha = 0.35f)
                } else {
                    rich.copy(alpha = 0.2f)
                }
            }
        }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor ?: MaterialTheme.colorScheme.surfaceBright,
        border = if (isHighlighted) BorderStroke(2.dp, borderColor) else null,
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = dimAlpha.value
                    val radius = blurRadius.value.toPx()
                    renderEffect = if (radius > 0f) BlurEffect(radius, radius, TileMode.Clamp) else null
                    shape = RectangleShape
                    clip = true
                }
                .combinedClickable(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onClick()
                    },
                    onLongClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showMenu = true
                    },
                ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp),
            ) {
                // App Icon
                Image(
                    bitmap = app.icon,
                    contentDescription = app.appName,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Fit,
                    colorFilter = if (isFrozen) frozenColorFilter else null,
                    alpha = if (isFrozen) 0.6f else 1f,
                )

                // Status Badges (Top Right)
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp),
                    horizontalArrangement = Arrangement.spacedBy((-4).dp),
                ) {
                    // Auto-freeze Exclusion Badge (Lock)
                    if (!isAutoFreezeEnabled || isLockedByTag) {
                        Box(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                    .padding(4.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_lock_clock_24),
                                contentDescription = stringResource(R.string.freeze_auto_freeze_excluded),
                                modifier = Modifier.fillMaxSize(),
                                tint = MaterialTheme.colorScheme.onError,
                            )
                        }
                    }
                }
            }

            Text(
                text = app.appName,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )

            SegmentedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                SegmentedDropdownMenuItem(
                    text = {
                        Text(
                            if (isFrozen) {
                                stringResource(R.string.action_unfreeze)
                            } else {
                                stringResource(
                                    R.string.action_freeze,
                                )
                            },
                        )
                    },
                    onClick = {
                        showMenu = false
                        onToggleFreeze()
                    },
                    leadingIcon = {
                        Icon(
                            painter =
                                painterResource(
                                    id = if (isFrozen) R.drawable.rounded_mode_cool_off_24 else R.drawable.rounded_mode_cool_24,
                                ),
                            contentDescription = null,
                        )
                    },
                )

                SegmentedDropdownMenuItem(
                    text = {
                        Text(
                            if (isAutoFreezeEnabled && !isLockedByTag) {
                                stringResource(R.string.action_lock_auto_freeze)
                            } else {
                                stringResource(
                                    R.string.action_unlock_auto_freeze,
                                )
                            },
                        )
                    },
                    enabled = !isLockedByTag,
                    onClick = {
                        showMenu = false
                        onToggleAutoFreeze(!isAutoFreezeEnabled)
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_lock_clock_24),
                            contentDescription = null,
                        )
                    },
                )

                SegmentedDropdownMenuItem(
                    text = {
                        TranslatableText(R.string.action_assign_tags)
                    },
                    onClick = {
                        showMenu = false
                        onAssignTags()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_interests_24),
                            contentDescription = null,
                        )
                    },
                )

                SegmentedDropdownMenuItem(
                    text = {
                        TranslatableText(R.string.action_remove)
                    },
                    onClick = {
                        showMenu = false
                        onRemove()
                    },
                    enabled = !isFrozen,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_delete_24),
                            contentDescription = null,
                        )
                    },
                )

                SegmentedDropdownMenuItem(
                    text = {
                        TranslatableText(R.string.action_create_shortcut)
                    },
                    onClick = {
                        showMenu = false
                        ShortcutUtil.pinAppShortcut(context, app)
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_home_health_24),
                            contentDescription = null,
                        )
                    },
                )

                SegmentedDropdownMenuItem(
                    text = {
                        TranslatableText(R.string.action_app_info)
                    },
                    onClick = {
                        showMenu = false
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        context.startActivity(intent)
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.rounded_info_24),
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FreezeSplitButton(
    iconRes: Int,
    text: String,
    menuText: String,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    showLabel: Boolean = true,
) {
    val view = LocalView.current
    var isMenuExpanded by remember { mutableStateOf(false) }

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onClick()
                },
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = if (showLabel) null else text,
                    modifier = Modifier.size(18.dp),
                )
                if (showLabel) {
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text(text, maxLines = 1, softWrap = false)
                }
            }
        },
        trailingButton = {
            Box {
                SplitButtonDefaults.TrailingButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        isMenuExpanded = true
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_keyboard_arrow_down_24),
                        contentDescription = menuText,
                    )
                }
                SegmentedDropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false },
                ) {
                    SegmentedDropdownMenuItem(
                        text = { Text(menuText) },
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            isMenuExpanded = false
                            onMenuClick()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        },
    )
}
