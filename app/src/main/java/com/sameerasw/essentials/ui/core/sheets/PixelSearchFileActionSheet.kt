/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Sheets
 * File: PixelSearchFileActionSheet.kt
 * Description: Contextual bottom sheet providing file actions (Open, Open with, Show in folder, Share, Copy path).
 */

package com.sameerasw.essentials.ui.core.sheets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.PixelSearchResultItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.utils.ColorUtil
import com.sameerasw.essentials.utils.FileSearchUtil
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun PixelSearchFileActionSheet(
    fileItem: PixelSearchResultItem.FileItem,
    onDismissRequest: () -> Unit,
    onActionCompleted: () -> Unit = onDismissRequest,
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
                if (fileItem.isImage || fileItem.isVideo || fileItem.isGif) {
                    AsyncImage(
                        model = fileItem.uri,
                        contentDescription = fileItem.displayName,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp)),
                    )
                } else {
                    Box(
                        modifier =
                            Modifier
                                .size(52.dp)
                                .background(
                                    color = ColorUtil.iconContainerColor(fileItem.displayName),
                                    shape = CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(fileItem.iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileItem.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val details = buildString {
                        append(FileSearchUtil.formatFileSize(fileItem.sizeBytes))
                        fileItem.path?.let { p ->
                            append(" • ")
                            append(p)
                        }
                    }
                    Text(
                        text = details,
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
                        launchFile(context, fileItem)
                        onActionCompleted()
                    },
                    showToggle = false,
                    hasMoreSettings = false,
                    iconRes = R.drawable.rounded_open_in_new_24,
                    hasIconBackground = true,
                )
                FeatureCard(
                    title = stringResource(R.string.pixel_search_action_open_with),
                    isEnabled = true,
                    onToggle = {},
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        openWithFile(context, fileItem)
                        onActionCompleted()
                    },
                    showToggle = false,
                    hasMoreSettings = false,
                    iconRes = R.drawable.rounded_apps_24,
                    hasIconBackground = true,
                )
                FeatureCard(
                    title = stringResource(R.string.pixel_search_action_show_in_folder),
                    isEnabled = true,
                    onToggle = {},
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        openFileFolder(context, fileItem)
                        onActionCompleted()
                    },
                    showToggle = false,
                    hasMoreSettings = false,
                    iconRes = R.drawable.rounded_folder_24,
                    hasIconBackground = true,
                )
                FeatureCard(
                    title = stringResource(R.string.pixel_search_action_share),
                    isEnabled = true,
                    onToggle = {},
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        shareFile(context, fileItem)
                        onDismissRequest()
                    },
                    showToggle = false,
                    hasMoreSettings = false,
                    iconRes = R.drawable.rounded_share_24,
                    hasIconBackground = true,
                )
                FeatureCard(
                    title = stringResource(R.string.pixel_search_action_copy_path),
                    isEnabled = true,
                    onToggle = {},
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        copyFilePath(context, fileItem)
                        onDismissRequest()
                    },
                    showToggle = false,
                    hasMoreSettings = false,
                    iconRes = R.drawable.rounded_content_copy_24,
                    hasIconBackground = true,
                )
            }
        }
    }
}

private fun launchFile(context: Context, item: PixelSearchResultItem.FileItem) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(item.uri, item.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(item.uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {
            Toast.makeText(context, item.displayName, Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openWithFile(context: Context, item: PixelSearchResultItem.FileItem) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(item.uri, item.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.pixel_search_action_open_with)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        launchFile(context, item)
    }
}

private fun shareFile(context: Context, item: PixelSearchResultItem.FileItem) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.pixel_search_action_share)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        Toast.makeText(context, item.displayName, Toast.LENGTH_SHORT).show()
    }
}

private fun openFileFolder(context: Context, item: PixelSearchResultItem.FileItem) {
    try {
        val parentPath = item.path?.let { java.io.File(it).parent }
        if (parentPath != null) {
            val relativePath = parentPath.removePrefix("/storage/emulated/0/").removePrefix("/")
            val folderUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3A" + Uri.encode(relativePath))
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(folderUri, "vnd.android.document/directory")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }
    } catch (_: Exception) {
    }

    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = Uri.parse("content://com.android.externalstorage.documents/root/primary")
            setDataAndType(uri, "vnd.android.document/root")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        copyFilePath(context, item)
    }
}

private fun copyFilePath(context: Context, item: PixelSearchResultItem.FileItem) {
    val path = item.path ?: item.displayName
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("File Path", path)
    clipboard.setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, context.getString(R.string.pixel_search_copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }
}
