/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core UI Components
 * File: ActivityIconSheet.kt
 * Description: Chooses the icon of an Open Activity shortcut: the activity's, the app's, or an image from storage.
 */

package com.sameerasw.essentials.ui.core.sheets

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.ActivityIconSource
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.utils.ActivityLauncherUtil
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ActivityIconSheet(
    action: Action.OpenActivity,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (ActivityIconSource, Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sizePx = ActivityLauncherUtil.CUSTOM_ICON_SIZE_PX
    var source by remember { mutableStateOf(ActivityLauncherUtil.iconSourceOf(action)) }
    var customImage by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        if (action.customIconPath.isNotBlank()) {
            customImage = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(action.customIconPath) }
        }
        // A saved image that has gone missing falls back to the activity's icon
        if (source == ActivityIconSource.CUSTOM && customImage == null) source = ActivityIconSource.ACTIVITY
    }

    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val image = withContext(Dispatchers.IO) { ActivityLauncherUtil.decodeSquareImage(context, uri, sizePx) }
                if (image == null) {
                    Toast.makeText(context, R.string.activity_icon_load_failed, Toast.LENGTH_SHORT).show()
                } else {
                    customImage = image
                    source = ActivityIconSource.CUSTOM
                }
            }
        }

    val preview by produceState<Bitmap?>(null, source, customImage) {
        value =
            if (source == ActivityIconSource.CUSTOM) {
                customImage
            } else {
                withContext(Dispatchers.IO) {
                    ActivityLauncherUtil.loadShortcutIcon(context, action.copy(iconSource = source), sizePx)
                }
            }
    }

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.activity_icon_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                preview?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(PREVIEW_SIZE.dp)
                                .clip(RoundedCornerShape(24.dp)),
                    )
                } ?: Spacer(modifier = Modifier.size(PREVIEW_SIZE.dp))
            }

            RoundedCardContainer {
                SegmentedPicker(
                    items = ActivityIconSource.entries,
                    selectedItem = source,
                    onItemSelected = {
                        HapticUtil.performUIHaptic(view)
                        // Picking Image with nothing chosen yet goes straight to the file picker
                        if (it == ActivityIconSource.CUSTOM && customImage == null) {
                            imagePicker.launch(arrayOf("image/*"))
                        } else {
                            source = it
                        }
                    },
                    labelProvider = { context.getString(it.title) },
                )
            }

            if (source == ActivityIconSource.CUSTOM) {
                OutlinedButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        imagePicker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_image_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.activity_icon_choose_image))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        preview?.let { onConfirm(source, it) }
                    },
                    enabled = preview != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_check_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(confirmLabel)
                }
            }
        }
    }
}

private const val PREVIEW_SIZE = 96
