/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Tiles
 * File: QSTilesSearchResultCard.kt
 * Description: Grouped Quick Settings tiles card for in-app search results with smooth 2x2 grid expansion.
 */

package com.sameerasw.essentials.ui.features.tiles

import android.app.Activity
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.Feature
import com.sameerasw.essentials.domain.registry.QSTileInfo
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.FeatureHelpBottomSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.features.system.QSTileCard
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.MainViewModel

private sealed interface GridItem {
    data class Tile(val info: QSTileInfo) : GridItem
    data class ToggleButton(val isExpanded: Boolean) : GridItem
}

@Composable
fun QSTilesSearchResultCard(
    tiles: List<QSTileInfo>,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    if (tiles.isEmpty()) return

    val context = LocalContext.current
    val view = LocalView.current
    var isExpanded by remember { mutableStateOf(false) }

    var showPermissionSheet by remember { mutableStateOf(false) }
    var selectedTileForPermissions by remember { mutableStateOf<QSTileInfo?>(null) }

    var showHelpSheet by remember { mutableStateOf(false) }
    var selectedHelpTile by remember { mutableStateOf<QSTileInfo?>(null) }

    if (showPermissionSheet && selectedTileForPermissions != null) {
        val permissionItems =
            PermissionUIHelper.getPermissionItems(
                selectedTileForPermissions!!.permissionKeys,
                context,
                viewModel,
                context as? Activity ?: (view.context as? Activity),
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
            object : Feature(
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
                    context: Context,
                    enabled: Boolean,
                ) {}
            }

        FeatureHelpBottomSheet(
            onDismissRequest = {
                showHelpSheet = false
                selectedHelpTile = null
            },
            feature = tempFeature,
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.feat_qs_tiles_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AssistChip(
                onClick = {
                    HapticUtil.performUIHaptic(view)
                    val intent =
                        Intent(context, FeatureSettingsActivity::class.java).apply {
                            putExtra("feature", "Quick settings tiles")
                        }
                    context.startActivity(intent)
                },
                label = {
                    Text(
                        text = stringResource(R.string.action_view_all),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.rounded_chevron_right_24),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors =
                    AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        trailingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                border = AssistChipDefaults.assistChipBorder(true),
            )
        }

        RoundedCardContainer(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceBright,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .animateContentSize(animationSpec = tween(durationMillis = 300)),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {

            val showExpandButton = tiles.size > 3

            val visibleItems: List<GridItem> =
                if (!showExpandButton) {
                    tiles.map { GridItem.Tile(it) }
                } else if (!isExpanded) {
                    tiles.take(3).map { GridItem.Tile(it) } + GridItem.ToggleButton(isExpanded = false)
                } else {
                    tiles.map { GridItem.Tile(it) } + GridItem.ToggleButton(isExpanded = true)
                }

            visibleItems.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEach { item ->
                        when (item) {
                            is GridItem.Tile -> {
                                val tile = item.info
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
                                    modifier = Modifier.weight(1f),
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
                                                val targetComponent =
                                                    ComponentName(context, tile.serviceClass)

                                                statusBarManager.requestAddTileService(
                                                    targetComponent,
                                                    context.getString(tile.titleRes),
                                                    Icon.createWithResource(context, tile.iconRes),
                                                    context.mainExecutor,
                                                ) { result ->
                                                    if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                                        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                                                            .edit()
                                                            .putBoolean("${tile.serviceClass.name}_is_added", true)
                                                            .apply()
                                                        viewModel.addedQSTiles.value = viewModel.addedQSTiles.value + tile.serviceClass.name
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(R.string.qs_tile_already_added),
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
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.qs_tile_requires_android_13),
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
                            is GridItem.ToggleButton -> {
                                Box(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(24.dp))
                                            .border(
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                shape = RoundedCornerShape(24.dp),
                                            )
                                            .clickable {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                isExpanded = !isExpanded
                                            }
                                            .padding(16.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    if (item.isExpanded) {
                                                        R.drawable.rounded_keyboard_arrow_up_24
                                                    } else {
                                                        R.drawable.rounded_keyboard_arrow_down_24
                                                    },
                                                ),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(8.dp),
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text =
                                                    stringResource(
                                                        if (item.isExpanded) {
                                                            R.string.action_show_less
                                                        } else {
                                                            R.string.action_show_more
                                                        },
                                                    ),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                            )
                                            Text(
                                                text = stringResource(R.string.feat_qs_tiles_title),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
}
