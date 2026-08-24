/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: ShareWithFragment.kt
 * Description: UI layout element for ShareWithFragment.kt.
 */

package com.sameerasw.essentials.ui.components.linkActions

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer

@Composable
fun ShareWithContent(
    resolveInfos: List<ResolvedAppInfo>,
    uri: Uri,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    togglePin: (String) -> Unit,
    pinnedPackages: Set<String>,
    isGridView: Boolean = false,
    demo: Boolean = false,
) {
    Log.d("LinkPicker", "ShareWithContent: ${resolveInfos.size} apps found, isGridView = $isGridView")
    val context = LocalContext.current

    if (resolveInfos.isEmpty()) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No apps found to share with",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        if (isGridView) {
            Column(
                modifier = modifier.fillMaxWidth().padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val rows = resolveInfos.chunked(4)
                rows.forEach { rowApps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        rowApps.forEach { info ->
                            Box(modifier = Modifier.weight(1f)) {
                                AppPickerItem(
                                    info = info,
                                    togglePin = togglePin,
                                    pinnedPackages = pinnedPackages,
                                    isGrid = true,
                                    demo = demo,
                                    onTapAction = {
                                        val intent =
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, uri.toString())
                                            }
                                        intent.setClassName(
                                            info.resolveInfo.activityInfo.packageName,
                                            info.resolveInfo.activityInfo.name,
                                        )
                                        context.startActivity(intent)
                                        onFinish()
                                    },
                                )
                            }
                        }
                        repeat(4 - rowApps.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            RoundedCardContainer(
                modifier = modifier.fillMaxWidth(),
            ) {
                resolveInfos.forEach { info ->
                    AppPickerItem(
                        info = info,
                        togglePin = togglePin,
                        pinnedPackages = pinnedPackages,
                        isGrid = false,
                        demo = demo,
                        onTapAction = {
                            val intent =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, uri.toString())
                                }
                            intent.setClassName(
                                info.resolveInfo.activityInfo.packageName,
                                info.resolveInfo.activityInfo.name,
                            )
                            context.startActivity(intent)
                            onFinish()
                        },
                    )
                }
            }
        }
    }
}
