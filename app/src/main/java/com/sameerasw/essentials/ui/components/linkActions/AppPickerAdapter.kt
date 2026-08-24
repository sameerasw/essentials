/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: AppPickerAdapter.kt
 * Description: UI layout element for AppPickerAdapter.kt.
 */

package com.sameerasw.essentials.ui.components.linkActions

import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sameerasw.essentials.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ResolvedAppInfo(
    val resolveInfo: ResolveInfo,
    val label: String,
)

@Composable
fun AppPickerItem(
    info: ResolvedAppInfo,
    modifier: Modifier = Modifier,
    togglePin: (String) -> Unit,
    pinnedPackages: Set<String>,
    isGrid: Boolean = false,
    demo: Boolean = false,
    onTapAction: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val packageName = info.resolveInfo.activityInfo.packageName
    val isPinned = pinnedPackages.contains(packageName)
    val isDarkTheme = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current

    Log.d("LinkPicker", "AppPickerItem, demo = $demo")

    // Load icon asynchronously
    var icon by remember { mutableStateOf<Drawable?>(null) }
    LaunchedEffect(info.resolveInfo) {
        withContext(Dispatchers.IO) {
            icon = info.resolveInfo.loadIcon(context.packageManager)
        }
    }

    if (isGrid) {
        val gridBgColor =
            if (isPinned) {
                if (isDarkTheme) Color.Black else Color.White
            } else {
                MaterialTheme.colorScheme.surfaceBright
            }

        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            if (demo) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else {
                                onTapAction?.invoke()
                            }
                        },
                        onLongClick = {
                            togglePin(packageName)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    ).background(
                        color = gridBgColor,
                        shape = RoundedCornerShape(16.dp),
                    ).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AsyncImage(
                model = icon,
                contentDescription = "App icon",
                modifier =
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp)),
            )

            Text(
                text = info.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .basicMarquee(),
            )
        }
    } else {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            if (demo) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else {
                                onTapAction?.invoke()
                            }
                        },
                        onLongClick = {
                            togglePin(packageName)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    ).background(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd),
                    ).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = icon,
                contentDescription = "App icon",
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.label,
                    modifier =
                        Modifier
                            .padding(start = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isPinned) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_bookmark_24),
                    contentDescription = "Pinned",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.padding())
            }
        }
    }
}
