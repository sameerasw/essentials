/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Tiles
 * File: QsTileRestoreCard.kt
 * Description: Offers to put Quick Settings tiles from an imported backup back into Quick Settings.
 */

package com.sameerasw.essentials.ui.features.tiles

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon as M3Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.registry.QSTileInfo
import com.sameerasw.essentials.services.tiles.QsTileLayoutRestorer
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
fun QsTileRestoreCard(
    viewModel: MainViewModel,
    tiles: List<QSTileInfo>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pending by viewModel.qsTilesPendingRestore
    val pendingTiles = tiles.filter { it.serviceClass.name in pending }
    if (pendingTiles.isEmpty()) return

    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val names = pendingTiles.joinToString(", ") { context.getString(it.titleRes) }

    RoundedCardContainer(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                M3Icon(
                    painter = painterResource(R.drawable.rounded_history_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.qs_tiles_restore_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = stringResource(R.string.qs_tiles_restore_desc, names),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = { viewModel.dismissQsTileRestore(context) }, enabled = !busy) {
                    Text(stringResource(R.string.qs_tiles_restore_dismiss))
                }
                Button(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            try {
                                restore(context, viewModel, pendingTiles)
                            } finally {
                                busy = false
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.qs_tiles_restore_action))
                }
            }
        }
    }
}

// Shizuku/root first (keeps the old positions), then the system add-tile dialog for whatever is left.
private suspend fun restore(
    context: Context,
    viewModel: MainViewModel,
    pendingTiles: List<QSTileInfo>,
) {
    var restored = 0
    if (ShellUtils.hasPermission(context)) restored += viewModel.restoreQsTilesWithShell(context).restored.size
    val left = pendingTiles.filter { it.serviceClass.name in viewModel.qsTilesPendingRestore.value }
    if (left.isNotEmpty() && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.qs_tiles_restore_needs_android_13, Toast.LENGTH_LONG).show()
        return
    }
    val addedByDialog = mutableSetOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
        for (tile in left) {
            val result = suspendCancellableCoroutine { cont ->
                statusBarManager.requestAddTileService(
                    ComponentName(context, tile.serviceClass),
                    context.getString(tile.titleRes),
                    Icon.createWithResource(context, tile.iconRes),
                    context.mainExecutor,
                ) { if (cont.isActive) cont.resume(it) }
            }
            when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED,
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED,
                -> {
                    viewModel.markQsTileRestored(context, tile.serviceClass.name)
                    addedByDialog += tile.serviceClass.name
                    restored++
                }
                // The user said no, or the system refused: stop asking, the card stays for another try.
                else -> break
            }
        }
    }
    // The system dialog always appends; move those tiles to their old spots when Shizuku/root can write the layout.
    if (addedByDialog.isNotEmpty() && !QsTileLayoutRestorer.repositionWithShell(context, addedByDialog)) {
        Toast.makeText(context, R.string.qs_tiles_restore_no_positions, Toast.LENGTH_LONG).show()
        return
    }
    if (restored > 0) {
        Toast.makeText(
            context,
            context.resources.getQuantityString(R.plurals.qs_tiles_restored_toast, restored, restored),
            Toast.LENGTH_SHORT,
        ).show()
    }
}
