/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Tiles
 * File: QuickSettingsTilesSettingsUI.kt
 * Description: UI component and settings composable for Tiles feature domain.
 */

package com.sameerasw.essentials.ui.features.system

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.tiles.AdaptiveBrightnessTileService
import com.sameerasw.essentials.services.tiles.AlwaysOnDisplayTileService
import com.sameerasw.essentials.services.tiles.AppFreezingTileService
import com.sameerasw.essentials.services.tiles.AppLockTileService
import com.sameerasw.essentials.services.tiles.BubblesTileService
import com.sameerasw.essentials.services.tiles.CaffeinateTileService
import com.sameerasw.essentials.services.tiles.ChargeQuickTileService
import com.sameerasw.essentials.services.tiles.DeveloperOptionsTileService
import com.sameerasw.essentials.services.tiles.DynamicNightLightTileService
import com.sameerasw.essentials.services.tiles.EssentialsOnDisplayTileService
import com.sameerasw.essentials.services.tiles.FlashlightPulseTileService
import com.sameerasw.essentials.services.tiles.FlashlightTileService
import com.sameerasw.essentials.services.tiles.LockdownTileService
import com.sameerasw.essentials.services.tiles.MapsPowerSavingTileService
import com.sameerasw.essentials.services.tiles.MonoAudioTileService
import com.sameerasw.essentials.services.tiles.NfcTileService
import com.sameerasw.essentials.services.tiles.NotificationLightingTileService
import com.sameerasw.essentials.services.tiles.PrivateDnsTileService
import com.sameerasw.essentials.services.tiles.PrivateNotificationsTileService
import com.sameerasw.essentials.services.tiles.RefreshRateTileService
import com.sameerasw.essentials.services.tiles.RestartSystemUiTileService
import com.sameerasw.essentials.services.tiles.ScaleAnimationsTileService
import com.sameerasw.essentials.services.tiles.ScreenLockedSecurityTileService
import com.sameerasw.essentials.services.tiles.SmartPixelsTileService
import com.sameerasw.essentials.services.tiles.SoundModeTileService
import com.sameerasw.essentials.services.tiles.StayAwakeTileService
import com.sameerasw.essentials.services.tiles.TapToWakeTileService
import com.sameerasw.essentials.services.tiles.UiBlurTileService
import com.sameerasw.essentials.services.tiles.UrlShortenerTileService
import com.sameerasw.essentials.services.tiles.UsbDebuggingTileService
import com.sameerasw.essentials.domain.registry.QSTileInfo
import com.sameerasw.essentials.domain.registry.QSTileRegistry
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.ui.modifiers.scrollMotionBlur
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun QuickSettingsTilesSettingsUI(
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    val resources = context.resources
    LocalView.current
    val viewModel: MainViewModel = viewModel()
    val includeUnsupportedFeatures by viewModel.isEnableUnsupportedFeatures

    var showPermissionSheet by remember { mutableStateOf(false) }
    var selectedTileForPermissions by remember { mutableStateOf<QSTileInfo?>(null) }

    var showHelpSheet by remember { mutableStateOf(false) }
    var selectedHelpTile by remember { mutableStateOf<QSTileInfo?>(null) }

    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    val isUseUsageStats by viewModel.isUseUsageAccess
    val allTiles = remember(isUseUsageStats) {
        QSTileRegistry.getAllTiles(context, isUseUsageStats)
    }

    val tiles = allTiles.filter { tile -> tile.isSupported(context) || includeUnsupportedFeatures }

    if (showPermissionSheet && selectedTileForPermissions != null) {
        val permissionItems =
            PermissionUIHelper.getPermissionItems(
                selectedTileForPermissions!!.permissionKeys,
                context,
                viewModel,
                context as? Activity,
            )
        if (permissionItems.isNotEmpty()) {
            PermissionsBottomSheet(
                onDismissRequest = {
                    showPermissionSheet = false
                    selectedTileForPermissions = null
                },
                featureTitle = selectedTileForPermissions!!.titleRes,
                permissions = permissionItems,
            )
        }
    }

    if (showHelpSheet && selectedHelpTile != null) {
        val tileId = stringResource(selectedHelpTile!!.titleRes)
        val tempFeature =
            object : com.sameerasw.essentials.domain.model.Feature(
                id = tileId,
                title = selectedHelpTile!!.titleRes,
                iconRes = selectedHelpTile!!.iconRes,
                category = R.string.cat_system,
                description = 0,
                aboutDescription = selectedHelpTile!!.aboutDescription,
                permissionKeys = selectedHelpTile!!.permissionKeys,
                showToggle = false,
            ) {
                override fun isEnabled(viewModel: MainViewModel) = true

                override fun onToggle(
                    viewModel: MainViewModel,
                    context: android.content.Context,
                    enabled: Boolean,
                ) {
                }
            }

        com.sameerasw.essentials.ui.core.sheets.FeatureHelpBottomSheet(
            onDismissRequest = {
                showHelpSheet = false
                selectedHelpTile = null
            },
            feature = tempFeature,
        )
    }

    val categoryOrder =
        listOf(
            R.string.cat_utils,
            R.string.cat_visuals,
            R.string.cat_connectivity,
            R.string.cat_privacy,
            R.string.cat_accessibility,
        )

    val categorizedTiles =
        tiles
            .groupBy { it.categoryRes }
            .toList()
            .sortedBy { (category, _) ->
                val index = categoryOrder.indexOf(category)
                if (index != -1) index else Int.MAX_VALUE
            }

    val scrollState = rememberScrollState()
    val isMotionBlurEnabled by viewModel.isMotionBlurEnabled

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .scrollMotionBlur(scrollState, enabled = isMotionBlurEnabled)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(contentPadding.calculateTopPadding()))
        Spacer(modifier = Modifier.height(16.dp))

        val isSecureSensitiveTilesEnabled by viewModel.isSecureSensitiveTilesEnabled
        RoundedCardContainer(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .highlight(
                        highlightSetting.equals(
                            resources.getString(R.string.qs_secure_sensitive_tiles_title),
                            ignoreCase = true,
                        ),
                    ),
        ) {
            IconToggleItem(
                title = stringResource(R.string.qs_secure_sensitive_tiles_title),
                description = stringResource(R.string.qs_secure_sensitive_tiles_desc),
                iconRes = R.drawable.rounded_shield_lock_24,
                isChecked = isSecureSensitiveTilesEnabled,
                onCheckedChange = { viewModel.setSecureSensitiveTilesEnabled(it) },
            )
        }

        categorizedTiles.forEachIndexed { index, (categoryRes, tilesInSection) ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            val categoryIcon =
                when (categoryRes) {
                    R.string.cat_utils -> R.drawable.rounded_settings_24
                    R.string.cat_visuals -> R.drawable.rounded_brightness_6_24
                    R.string.cat_connectivity -> R.drawable.rounded_android_wifi_3_bar_24
                    R.string.cat_privacy -> R.drawable.rounded_shield_24
                    R.string.cat_accessibility -> R.drawable.rounded_accessibility_new_24
                    else -> null
                }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp, start = 12.dp),
            ) {
                if (categoryIcon != null) {
                    Icon(
                        painter = painterResource(id = categoryIcon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    text = stringResource(categoryRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            tilesInSection.chunked(2).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowTiles.forEach { tile ->
                        // Map permission keys to actual granted state
                        val allPermissionsGranted =
                            tile.permissionKeys.all { key ->
                                PermissionUIHelper
                                    .getPermissionItem(
                                        key,
                                        context,
                                        viewModel,
                                    )?.isGranted == true
                            }

                        val addedTiles by viewModel.addedQSTiles
                        val componentName = ComponentName(context, tile.serviceClass)
                        val isAdded =
                            addedTiles.any {
                                it.contains(componentName.flattenToString()) ||
                                    it.contains(componentName.flattenToShortString()) ||
                                    it.contains(tile.serviceClass.name)
                            }

                        val pinnedQsTiles by viewModel.pinnedQsTileKeys
                        val isPinned = pinnedQsTiles.contains(tile.serviceClass.name)

                        QSTileCard(
                            tile = tile,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .highlight(
                                        highlightSetting.equals(
                                            resources.getString(tile.titleRes),
                                            ignoreCase = true,
                                        ),
                                    ),
                            isMissingPermissions = !allPermissionsGranted,
                            isAdded = isAdded,
                            isPinned = isPinned,
                            onClick = {
                                if (!allPermissionsGranted) {
                                    selectedTileForPermissions = tile
                                    showPermissionSheet = true
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val statusBarManager =
                                            context.getSystemService(StatusBarManager::class.java)
                                        val componentName =
                                            ComponentName(context, tile.serviceClass)

                                        statusBarManager.requestAddTileService(
                                            componentName,
                                            resources.getString(tile.titleRes),
                                            Icon.createWithResource(context, tile.iconRes),
                                            context.mainExecutor,
                                        ) { result ->
                                            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                                context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                                                    .edit()
                                                    .putBoolean("${tile.serviceClass.name}_is_added", true)
                                                    .apply()
                                                viewModel.addedQSTiles.value = viewModel.addedQSTiles.value + tile.serviceClass.name
                                                Toast
                                                    .makeText(
                                                        context,
                                                        resources.getString(R.string.qs_tile_already_added),
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                            } else if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                                context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                                                    .edit()
                                                    .putBoolean("${tile.serviceClass.name}_is_added", true)
                                                    .apply()
                                                viewModel.addedQSTiles.value = viewModel.addedQSTiles.value + tile.serviceClass.name
                                            }
                                        }
                                    } else {
                                        Toast
                                            .makeText(
                                                context,
                                                resources.getString(R.string.qs_tile_requires_android_13),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    }
                                }
                            },
                            onPinToggle = {
                                viewModel.togglePinQsTile(tile.serviceClass.name)
                            },
                            onHelpClick =
                                if (tile.aboutDescription != null) {
                                    {
                                        selectedHelpTile = tile
                                        showHelpSheet = true
                                    }
                                } else {
                                    null
                                },
                        )
                    }
                    // Determine if we need a spacer for the last odd item
                    if (rowTiles.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Text(
            text = "Long press a tile to see what it does",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Favorites Widget Section
        val pinnedQsTiles by viewModel.pinnedQsTileKeys
        val pinnedTileInfos =
            remember(pinnedQsTiles, tiles) {
                pinnedQsTiles.mapNotNull { className -> tiles.find { it.serviceClass.name == className } }
            }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_widgets_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = stringResource(R.string.qs_tiles_widget_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = "Long press tiles to add",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )

            if (pinnedTileInfos.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.qs_tiles_widget_empty_state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                pinnedTileInfos.chunked(2).forEach { rowTiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowTiles.forEach { tile ->
                            val allPermissionsGranted =
                                tile.permissionKeys.all { key ->
                                    PermissionUIHelper
                                        .getPermissionItem(
                                            key,
                                            context,
                                            viewModel,
                                        )?.isGranted == true
                                }
                            val addedTiles by viewModel.addedQSTiles
                            val componentName = ComponentName(context, tile.serviceClass)
                            val isAdded =
                                addedTiles.any {
                                    it.contains(componentName.flattenToString()) ||
                                        it.contains(componentName.flattenToShortString()) ||
                                        it.contains(tile.serviceClass.name)
                                }

                            QSTileCard(
                                tile = tile,
                                modifier = Modifier.weight(1f),
                                isMissingPermissions = !allPermissionsGranted,
                                isAdded = isAdded,
                                isPinned = true,
                                onClick = {
                                    viewModel.togglePinQsTile(tile.serviceClass.name)
                                },
                                onPinToggle = {
                                    viewModel.togglePinQsTile(tile.serviceClass.name)
                                },
                            )
                        }
                        if (rowTiles.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding()))
    }
}

@Composable
fun QSTileCard(
    tile: QSTileInfo,
    isMissingPermissions: Boolean,
    isAdded: Boolean,
    isPinned: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPinToggle: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null,
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }

    val menuState = com.sameerasw.essentials.ui.state.LocalMenuStateManager.current
    androidx.compose.runtime.DisposableEffect(showMenu) {
        if (showMenu) {
            menuState.activeId = tile.titleRes
        } else {
            if (menuState.activeId == tile.titleRes) {
                menuState.activeId = null
            }
        }
        onDispose {
            if (menuState.activeId == tile.titleRes) {
                menuState.activeId = null
            }
        }
    }

    val isBlurred = menuState.activeId != null && menuState.activeId != tile.titleRes
    val blurRadius by animateDpAsState(
        targetValue = if (isBlurred) 10.dp else 0.dp,
        animationSpec = tween(durationMillis = 500),
        label = "blur",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isBlurred) 0.5f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "alpha",
    )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(alpha)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (isMissingPermissions) {
                        MaterialTheme.colorScheme.errorContainer
                    } else if (isAdded) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                ).combinedClickable(
                    onClick = {
                        com.sameerasw.essentials.utils.HapticUtil
                            .performVirtualKeyHaptic(view)
                        onClick()
                    },
                    onLongClick = {
                        if (onHelpClick != null || onPinToggle != null) {
                            com.sameerasw.essentials.utils.HapticUtil
                                .performVirtualKeyHaptic(view)
                            showMenu = true
                        }
                    },
                ).padding(16.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .blur(blurRadius),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val contentColor =
                if (isMissingPermissions) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else if (isAdded) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }

            Icon(
                painter = painterResource(id = tile.iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.padding(8.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(tile.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
                Text(
                    text =
                        if (isMissingPermissions) {
                            "Grant permission"
                        } else if (isAdded) {
                            stringResource(R.string.action_added)
                        } else {
                            stringResource(R.string.action_add)
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }

            if (isPinned) {
                Icon(
                    painter = painterResource(id = R.drawable.round_star_24),
                    contentDescription = null,
                    tint = contentColor,
                    modifier =
                        Modifier
                            .size(16.dp)
                            .padding(top = 2.dp),
                )
            }
        }

        if (onHelpClick != null || onPinToggle != null) {
            com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                if (onPinToggle != null) {
                    com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem(
                        text = {
                            Text(
                                if (isPinned) {
                                    stringResource(R.string.action_unpin)
                                } else {
                                    stringResource(R.string.action_pin)
                                },
                            )
                        },
                        onClick = {
                            showMenu = false
                            onPinToggle()
                        },
                        leadingIcon = {
                            Icon(
                                painter =
                                    painterResource(
                                        id =
                                            if (isPinned) {
                                                R.drawable.rounded_bookmark_remove_24
                                            } else {
                                                R.drawable.rounded_bookmark_24
                                            },
                                    ),
                                contentDescription = null,
                            )
                        },
                    )
                }

                if (onHelpClick != null) {
                    com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.action_what_is_this))
                        },
                        onClick = {
                            showMenu = false
                            onHelpClick()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_help_24),
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }
    }
}
