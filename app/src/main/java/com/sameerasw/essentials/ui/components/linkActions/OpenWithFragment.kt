/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: OpenWithFragment.kt
 * Description: UI layout element for OpenWithFragment.kt.
 */

package com.sameerasw.essentials.ui.components.linkActions

import com.sameerasw.essentials.R
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer

@Composable
fun OpenWithContent(
    resolveInfos: List<ResolvedAppInfo>,
    uri: Uri,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    togglePin: (String) -> Unit,
    pinnedPackages: Set<String>,
    demo: Boolean = false,
) {
    Log.d("LinkPicker", "OpenWithContent: ${resolveInfos.size} apps found")
    val context = LocalContext.current

    if (resolveInfos.isEmpty()) {
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            TranslatableText(
                stringResId = R.string.link_no_apps_open,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
        ) {
            RoundedCardContainer(
                modifier = Modifier.fillMaxWidth(),
            ) {
                resolveInfos.forEach { info ->
                    AppPickerItem(
                        info = info,
                        togglePin = togglePin,
                        pinnedPackages = pinnedPackages,
                        demo = demo,
                        onTapAction = {
                            val intent = Intent(Intent.ACTION_VIEW, uri)
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
