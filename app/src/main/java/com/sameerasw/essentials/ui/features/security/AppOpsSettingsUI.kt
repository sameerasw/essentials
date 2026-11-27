/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Security
 * File: AppOpsSettingsUI.kt
 * Description: Two page App Ops browser, by app and by operation.
 */

package com.sameerasw.essentials.ui.features.security

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.appops.AppOpEntry
import com.sameerasw.essentials.domain.appops.AppOpInfo
import com.sameerasw.essentials.domain.appops.AppOpMode
import com.sameerasw.essentials.domain.appops.isEffectivelyAllowed
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.components.CategoryExpandableSection
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.core.sheets.SingleAppSelectionSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.AppOpsViewModel
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppOpsSettingsUI(
    mainViewModel: MainViewModel,
    query: String,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val viewModel: AppOpsViewModel = viewModel()

    val hasShell =
        remember(
            mainViewModel.isShizukuPermissionGranted.value,
            mainViewModel.isRootPermissionGranted.value,
            mainViewModel.isShizukuAvailable.value,
        ) { ShellUtils.isAvailable(context) && ShellUtils.hasPermission(context) }

    LaunchedEffect(hasShell) { viewModel.load(context) }

    val apps = viewModel.apps.value
    val ops = viewModel.ops.value
    val entries = viewModel.entries.value
    val isScanning = viewModel.isScanning.value
    val showSystemApps = viewModel.showSystemApps.value

    var expandedOp by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    var pinnedOp by remember { mutableStateOf<String?>(null) }
    var addAppForOp by remember { mutableStateOf<String?>(null) }
    var showPermissionSheet by remember { mutableStateOf(false) }

    val carouselState = rememberCarouselState { 2 }
    val currentOnPageChange by rememberUpdatedState(onPageChange)

    LaunchedEffect(carouselState) {
        var isFirst = true
        snapshotFlow { carouselState.currentItem }.collect {
            currentOnPageChange(it)
            if (isFirst) isFirst = false else HapticUtil.performHeavyHaptic(view)
        }
    }

    val appsQuery = if (carouselState.currentItem == 0) query else ""
    val opsQuery = if (carouselState.currentItem == 1) query else ""

    val visibleApps =
        remember(apps, showSystemApps) {
            apps.orEmpty().filter { showSystemApps || !it.isSystemApp }
        }
    val appsByPackage = remember(visibleApps) { visibleApps.associateBy { it.packageName } }

    val filteredApps =
        visibleApps.filter {
            appsQuery.isBlank() ||
                it.appName.contains(appsQuery, ignoreCase = true) ||
                it.packageName.contains(appsQuery, ignoreCase = true)
        }

    val appsForOp =
        remember(entries, appsByPackage) {
            val map = mutableMapOf<String, MutableList<Pair<NotificationApp, AppOpEntry>>>()
            entries.forEach { (pkg, opEntries) ->
                val app = appsByPackage[pkg] ?: return@forEach
                opEntries.values.forEach { entry -> map.getOrPut(entry.op) { mutableListOf() }.add(app to entry) }
            }
            map.mapValues { (_, list) -> list.sortedBy { it.first.appName.lowercase() } }
        }

    val filteredOps =
        ops.filter {
            opsQuery.isBlank() ||
                it.name.contains(opsQuery, ignoreCase = true) ||
                it.label.contains(opsQuery, ignoreCase = true) ||
                it.permission?.contains(opsQuery, ignoreCase = true) == true
        }

    val defaultModes = remember(ops) { ops.associate { it.name to it.defaultMode } }

    fun requireShell(action: () -> Unit) {
        if (hasShell) action() else showPermissionSheet = true
    }

    fun changeMode(
        packageName: String,
        op: AppOpInfo,
        mode: AppOpMode,
    ) {
        requireShell {
            viewModel.setMode(context, packageName, op.name, mode) { ok ->
                if (!ok) Toast.makeText(context, context.getString(R.string.app_ops_set_failed, op.label), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    HorizontalMultiBrowseCarousel(
        state = carouselState,
        preferredItemWidth = screenWidth,
        itemSpacing = 4.dp,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) { page ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = statusBarHeight + 20.dp, bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(if (page == 1) 8.dp else 2.dp),
            ) {
                item(key = "title") {
                    TranslatableText(
                        stringResId = if (page == 0) R.string.app_ops_apps_page else R.string.app_ops_ops_page,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }

                item(key = "filters") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = showSystemApps,
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                viewModel.showSystemApps.value = !showSystemApps
                            },
                            label = { TranslatableText(R.string.app_ops_system_apps) },
                        )
                        if (isScanning) {
                            LoadingIndicator(modifier = Modifier.size(32.dp))
                            TranslatableText(
                                stringResId = R.string.app_ops_scanning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (!hasShell) {
                    item(key = "shell") {
                        ShellRequiredCard(onGrant = { showPermissionSheet = true })
                    }
                }

                if (page == 0) {
                    appsPage(
                        apps = apps,
                        filteredApps = filteredApps,
                        query = appsQuery,
                        entries = entries,
                        defaultModes = defaultModes,
                        showCounts = viewModel.hasScanned.value,
                        onClick = { app ->
                            HapticUtil.performVirtualKeyHaptic(view)
                            pinnedOp = null
                            selectedPackage = app.packageName
                        },
                    )
                } else {
                    opsPage(
                        ops = filteredOps,
                        appsForOp = appsForOp,
                        expandedOp = expandedOp,
                        query = opsQuery,
                        onToggleExpand = { op -> expandedOp = if (expandedOp == op.name) null else op.name },
                        onModeChange = { pkg, op, mode -> changeMode(pkg, op, mode) },
                        onOpenApp = { pkg, op ->
                            pinnedOp = op.name
                            selectedPackage = pkg
                        },
                        onAddApp = { op ->
                            HapticUtil.performVirtualKeyHaptic(view)
                            requireShell { addAppForOp = op.name }
                        },
                    )
                }
            }
        }
    }

    selectedPackage?.let { packageName ->
        val app = apps?.find { it.packageName == packageName }
        if (app != null) {
            AppOpsAppSheet(
                app = app,
                ops = ops,
                entries = entries[packageName].orEmpty(),
                pinnedOp = pinnedOp,
                hasShell = hasShell,
                onModeChange = { op, mode -> changeMode(packageName, op, mode) },
                onReset = {
                    requireShell {
                        viewModel.resetApp(context, packageName) { ok ->
                            Toast
                                .makeText(
                                    context,
                                    if (ok) context.getString(R.string.app_ops_reset_done) else context.getString(R.string.app_ops_reset_failed),
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }
                    }
                },
                onRequestShell = { showPermissionSheet = true },
                onDismiss = {
                    selectedPackage = null
                    pinnedOp = null
                },
            )
        }
    }

    addAppForOp?.let { opName ->
        SingleAppSelectionSheet(
            onDismissRequest = { addAppForOp = null },
            includeSelf = true,
            includeSystemApps = showSystemApps,
            onAppSelected = { app ->
                addAppForOp = null
                pinnedOp = opName
                selectedPackage = app.packageName
            },
        )
    }

    if (showPermissionSheet) {
        PermissionsBottomSheet(
            onDismissRequest = {
                showPermissionSheet = false
                mainViewModel.check(context)
                viewModel.rescan(context)
            },
            featureTitle = R.string.feat_app_ops_title,
            permissions =
                PermissionUIHelper.getPermissionItems(
                    listOf(if (ShellUtils.isRootEnabled(context)) "ROOT" else "SHIZUKU"),
                    context,
                    mainViewModel,
                ),
        )
    }
}

private fun LazyListScope.appsPage(
    apps: List<NotificationApp>?,
    filteredApps: List<NotificationApp>,
    query: String,
    entries: Map<String, Map<String, AppOpEntry>>,
    defaultModes: Map<String, AppOpMode>,
    showCounts: Boolean,
    onClick: (NotificationApp) -> Unit,
) {
    when {
        apps == null -> item(key = "loading") { CenteredLoading() }
        filteredApps.isEmpty() -> item(key = "empty") { EmptyText(query) }
        else ->
            itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                val appEntries = entries[app.packageName].orEmpty()
                val allowed =
                    appEntries.values.count { it.mode.isEffectivelyAllowed(defaultModes[it.op] ?: AppOpMode.DEFAULT) }
                AppOpsRowSurface(shape = rowShape(index, filteredApps.lastIndex), onClick = { onClick(app) }) {
                    AppIcon(app.icon, 40)
                    Column(modifier = Modifier.weight(1f)) {
                        SingleLine(app.appName, MaterialTheme.typography.bodyLarge, MaterialTheme.colorScheme.onSurface)
                        SingleLine(app.packageName, MaterialTheme.typography.bodySmall, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (showCounts) {
                        CountPill(text = "$allowed/${appEntries.size}")
                    }
                }
            }
    }
}

private fun LazyListScope.opsPage(
    ops: List<AppOpInfo>,
    appsForOp: Map<String, List<Pair<NotificationApp, AppOpEntry>>>,
    expandedOp: String?,
    query: String,
    onToggleExpand: (AppOpInfo) -> Unit,
    onModeChange: (String, AppOpInfo, AppOpMode) -> Unit,
    onOpenApp: (String, AppOpInfo) -> Unit,
    onAddApp: (AppOpInfo) -> Unit,
) {
    if (ops.isEmpty()) {
        item(key = "ops_empty") { if (query.isBlank()) CenteredLoading() else EmptyText(query) }
        return
    }
    items(ops, key = { it.name }) { op ->
        val opApps = appsForOp[op.name].orEmpty()
        CategoryExpandableSection(
            title = op.label,
            itemCount = opApps.size,
            isExpanded = expandedOp == op.name,
            onToggleExpand = { onToggleExpand(op) },
            iconRes = op.iconRes,
            subtitle = listOfNotNull(op.name, op.permission?.substringAfterLast('.')).joinToString(" · "),
        ) {
            val lastIndex = opApps.size
            opApps.forEachIndexed { index, (app, entry) ->
                OpModeRow(
                    title = app.appName,
                    op = op,
                    entry = entry,
                    shape = rowShape(index, lastIndex),
                    leading = { AppIcon(app.icon, 32) },
                    onModeChange = { onModeChange(app.packageName, op, it) },
                    onRowClick = { onOpenApp(app.packageName, op) },
                )
            }
            AppOpsRowSurface(
                shape = rowShape(lastIndex, lastIndex),
                onClick = { onAddApp(op) },
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_add_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp).padding(4.dp),
                )
                TranslatableText(
                    stringResId = R.string.app_ops_add_app,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CountPill(text: String) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OpModeRow(
    title: String,
    op: AppOpInfo,
    entry: AppOpEntry?,
    shape: RoundedCornerShape,
    enabled: Boolean = true,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceBright,
    leading: (@Composable () -> Unit)? = null,
    onModeChange: (AppOpMode) -> Unit,
    onRowClick: (() -> Unit)? = null,
) {
    val view = LocalView.current
    var menuOpen by remember { mutableStateOf(false) }
    val mode = entry?.mode ?: AppOpMode.DEFAULT
    val details =
        buildList {
            if (entry?.isUidMode == true) add(stringResource(R.string.app_ops_uid_mode))
            entry?.lastUsed?.let(::formatLastUsed)?.let { add(stringResource(R.string.app_ops_last_used, it)) }
        }.joinToString(" · ")

    AppOpsRowSurface(
        shape = shape,
        color = color,
        onClick = {
            HapticUtil.performVirtualKeyHaptic(view)
            if (onRowClick != null) onRowClick() else menuOpen = true
        },
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            SingleLine(title, MaterialTheme.typography.bodyLarge, MaterialTheme.colorScheme.onSurface)
            if (details.isNotEmpty()) {
                SingleLine(details, MaterialTheme.typography.bodySmall, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box {
            Surface(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    menuOpen = true
                },
                shape = RoundedCornerShape(12.dp),
                color =
                    if (mode.isRestricted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor =
                    if (mode.isRestricted) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                enabled = enabled,
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(text = stringResource(mode.label), style = MaterialTheme.typography.labelLarge)
                    Icon(
                        painter = painterResource(R.drawable.rounded_keyboard_arrow_down_24),
                        contentDescription = stringResource(R.string.app_ops_change_mode),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            SegmentedDropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                AppOpMode.entries.forEach { option ->
                    SegmentedDropdownMenuItem(
                        text = { Text(stringResource(option.label)) },
                        onClick = {
                            menuOpen = false
                            HapticUtil.performVirtualKeyHaptic(view)
                            if (option != mode) onModeChange(option)
                        },
                        trailingIcon =
                            if (option == mode) {
                                { Icon(painterResource(R.drawable.rounded_check_24), contentDescription = null) }
                            } else {
                                null
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppOpsRowSurface(
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceBright,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = color,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun AppIcon(
    icon: ImageBitmap,
    sizeDp: Int,
) {
    Image(
        bitmap = icon,
        contentDescription = null,
        modifier = Modifier.size(sizeDp.dp).clip(RoundedCornerShape((sizeDp / 4).dp)),
    )
}

@Composable
private fun SingleLine(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
) {
    Text(text = text, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun ShellRequiredCard(onGrant: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TranslatableText(
                stringResId = R.string.app_ops_shell_required,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Button(onClick = onGrant) { TranslatableText(R.string.perm_action_grant) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CenteredLoading() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalArrangement = Arrangement.Center,
    ) { LoadingIndicator() }
}

@Composable
private fun EmptyText(query: String) {
    Text(
        text = stringResource(R.string.search_no_results, query),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}

private val durationPartRegex = Regex("(\\d+)(d|h|ms|m|s)")

private fun formatLastUsed(raw: String): String? {
    if (raw.firstOrNull()?.isDigit() != true) return null
    val parts = durationPartRegex.findAll(raw).map { it.groupValues[1] to it.groupValues[2] }.toList()
    if (parts.isEmpty()) return null
    val visible = parts.filter { it.second != "ms" }.ifEmpty { listOf("0" to "s") }
    return visible.take(2).joinToString(" ") { (value, unit) -> "$value$unit" }
}

private fun rowShape(
    index: Int,
    lastIndex: Int,
): RoundedCornerShape {
    val outer = 24.dp
    val inner = 4.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == lastIndex) outer else inner,
        bottomEnd = if (index == lastIndex) outer else inner,
    )
}

@Composable
private fun AppOpsAppSheet(
    app: NotificationApp,
    ops: List<AppOpInfo>,
    entries: Map<String, AppOpEntry>,
    pinnedOp: String?,
    hasShell: Boolean,
    onModeChange: (AppOpInfo, AppOpMode) -> Unit,
    onReset: () -> Unit,
    onRequestShell: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val used =
        ops
            .filter { it.name in entries || it.name == pinnedOp }
            .sortedWith(compareByDescending<AppOpInfo> { it.name == pinnedOp }.thenBy { it.name })
    val other = ops.filter { it.name !in entries && it.name != pinnedOp }
    val restricted = entries.values.count { it.mode.isRestricted }
    var otherExpanded by remember { mutableStateOf(false) }

    com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        AppIcon(app.icon, 56)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            SingleLine(app.packageName, MaterialTheme.typography.bodySmall, MaterialTheme.colorScheme.onSurfaceVariant)
                            SingleLine(
                                listOf(
                                    stringResource(if (app.isSystemApp) R.string.app_ops_system_app else R.string.app_ops_user_app),
                                    stringResource(R.string.app_ops_changed_count, entries.size),
                                    stringResource(R.string.app_ops_restricted_count, restricted),
                                ).joinToString(" · "),
                                MaterialTheme.typography.labelMedium,
                                MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                runCatching {
                                    context.startActivity(
                                        android.content
                                            .Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            .setData(android.net.Uri.fromParts("package", app.packageName, null))
                                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { TranslatableText(R.string.action_app_info) }
                        androidx.compose.material3.FilledTonalButton(
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onReset()
                            },
                            modifier = Modifier.weight(1f),
                            colors =
                                androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                        ) { TranslatableText(R.string.app_ops_reset_app) }
                    }
                    if (!hasShell) ShellRequiredCard(onGrant = onRequestShell)
                }
            }

            sheetSection("used", R.string.app_ops_section_used, used, entries, hasShell, onModeChange)

            if (other.isNotEmpty()) {
                item(key = "other") {
                    CategoryExpandableSection(
                        title = stringResource(R.string.app_ops_section_other),
                        itemCount = other.size,
                        isExpanded = otherExpanded,
                        onToggleExpand = { otherExpanded = !otherExpanded },
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        other.forEachIndexed { index, op ->
                            SheetOpRow(op, entries[op.name], rowShape(index, other.lastIndex), hasShell, onModeChange)
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.sheetSection(
    key: String,
    title: Int,
    ops: List<AppOpInfo>,
    entries: Map<String, AppOpEntry>,
    hasShell: Boolean,
    onModeChange: (AppOpInfo, AppOpMode) -> Unit,
) {
    if (ops.isEmpty()) return
    item(key = "title_$key") {
        TranslatableText(
            stringResId = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        )
    }
    itemsIndexed(ops, key = { _, op -> "${key}_${op.name}" }) { index, op ->
        SheetOpRow(op, entries[op.name], rowShape(index, ops.lastIndex), hasShell, onModeChange)
    }
}

@Composable
private fun SheetOpRow(
    op: AppOpInfo,
    entry: AppOpEntry?,
    shape: RoundedCornerShape,
    hasShell: Boolean,
    onModeChange: (AppOpInfo, AppOpMode) -> Unit,
) {
    OpModeRow(
        title = op.label,
        op = op,
        entry = entry,
        shape = shape,
        enabled = hasShell,
        leading = {
            Icon(
                painter = painterResource(op.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        },
        onModeChange = { onModeChange(op, it) },
    )
}
