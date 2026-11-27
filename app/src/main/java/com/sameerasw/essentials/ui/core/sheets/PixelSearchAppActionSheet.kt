/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Sheets
 * File: PixelSearchAppActionSheet.kt
 * Description: Contextual bottom sheet providing app actions (Open, Unfreeze, App info, Play Store, Share).
 */

package com.sameerasw.essentials.ui.core.sheets

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.PixelSearchResultItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun PixelSearchAppActionSheet(
    appItem: PixelSearchResultItem.AppItem,
    onDismissRequest: () -> Unit,
    onOpenApp: (String) -> Unit,
    onUnfreezeApp: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current

    EssentialsBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                if (appItem.icon != null) {
                    Image(
                        painter = BitmapPainter(appItem.icon),
                        contentDescription = appItem.appName,
                        modifier = Modifier.size(52.dp),
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .size(52.dp)
                                .background(
                                    color = ColorUtil.iconContainerColor(appItem.appName),
                                    shape = CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_apps_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appItem.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = appItem.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                FeatureCard(
                    title = stringResource(R.string.pixel_search_action_open),
                    isEnabled = true,
                    onToggle = {},
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onOpenApp(appItem.packageName)
                        onDismissRequest()
                    },
                    showToggle = false,
                    hasMoreSettings = false,
                    iconRes = R.drawable.rounded_open_in_new_24,
                    hasIconBackground = true,
                )
                if (appItem.isFrozen && onUnfreezeApp != null) {
                    FeatureCard(
                        title = stringResource(R.string.action_unfreeze),
                        isEnabled = true,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onUnfreezeApp(appItem.packageName)
                            onDismissRequest()
                        },
                        showToggle = false,
                        hasMoreSettings = false,
                        iconRes = R.drawable.rounded_mode_cool_off_24,
                        hasIconBackground = true,
                    )
                }
                FeatureCard(
                    title = stringResource(R.string.action_app_info),
                    isEnabled = true,
                    onToggle = {},
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        val infoIntent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", appItem.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        context.startActivity(infoIntent)
                        onDismissRequest()
                    },
                    showToggle = false,
                    hasMoreSettings = false,
                    iconRes = R.drawable.rounded_info_24,
                    hasIconBackground = true,
                )
                FeatureCard(
                    title = stringResource(R.string.pixel_search_action_play_store),
                    isEnabled = true,
                    onToggle = {},
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        openAppInPlayStore(context, appItem.packageName)
                        onDismissRequest()
                    },
                    showToggle = false,
                    hasMoreSettings = false,
                    iconRes = R.drawable.rounded_storefront_24,
                    hasIconBackground = true,
                )
                FeatureCard(
                    title = stringResource(R.string.pixel_search_action_share_app),
                    isEnabled = true,
                    onToggle = {},
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        shareApp(context, appItem.appName, appItem.packageName)
                        onDismissRequest()
                    },
                    showToggle = false,
                    hasMoreSettings = false,
                    iconRes = R.drawable.rounded_share_24,
                    hasIconBackground = true,
                )
            }
        }
    }
}

private fun openAppInPlayStore(context: Context, packageName: String) {
    try {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    } catch (_: Exception) {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }
}

private fun shareApp(context: Context, appName: String, packageName: String) {
    try {
        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, appName)
                putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        val chooser =
            Intent.createChooser(shareIntent, context.getString(R.string.pixel_search_action_share_app)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(chooser)
    } catch (_: Exception) {
    }
}
