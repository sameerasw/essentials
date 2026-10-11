/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core UI Components
 * File: ActivityPickerSheet.kt
 * Description: App then activity picker for the Open activity action, with pin to home screen.
 */

package com.sameerasw.essentials.ui.core.sheets

import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.ActivityIconSource
import com.sameerasw.essentials.utils.ActivityLauncherUtil
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.utils.ShortcutUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// System apps are included since hidden settings screens are a common target
@Composable
fun OpenActivityPicker(
    onDismiss: () -> Unit,
    onActivitySelected: (Action.OpenActivity) -> Unit,
    closeOnSelect: Boolean = true,
) {
    var packageName by remember { mutableStateOf<String?>(null) }
    val selectedPackage = packageName
    if (selectedPackage == null) {
        SingleAppSelectionSheet(
            includeSelf = true,
            includeSystemApps = true,
            // The app sheet also dismisses itself after a selection
            onDismissRequest = { if (packageName == null) onDismiss() },
            onAppSelected = { packageName = it.packageName },
        )
    } else {
        ActivityPickerSheet(
            packageName = selectedPackage,
            onDismiss = onDismiss,
            onActivitySelected = {
                onActivitySelected(it)
                if (closeOnSelect) onDismiss()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActivityPickerSheet(
    packageName: String,
    onDismiss: () -> Unit,
    onActivitySelected: (Action.OpenActivity) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val canUseRoot = remember { ShellUtils.isRootEnabled(context) }
    val appLabel =
        remember(packageName) {
            runCatching {
                context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString()
            }.getOrDefault(packageName)
        }
    val canPin =
        remember {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                context.getSystemService(ShortcutManager::class.java)?.isRequestPinShortcutSupported == true
        }
    val iconSizePx = with(LocalDensity.current) { ICON_SIZE.dp.roundToPx() }
    var query by remember { mutableStateOf("") }
    var activities by remember { mutableStateOf<List<ActivityLauncherUtil.LaunchableActivity>?>(null) }
    var pinTarget by remember { mutableStateOf<Action.OpenActivity?>(null) }

    LaunchedEffect(packageName) {
        activities = withContext(Dispatchers.IO) { ActivityLauncherUtil.getActivities(context, packageName) }
    }

    val filtered =
        activities.orEmpty().filter {
            query.isBlank() || it.label.contains(query, ignoreCase = true) || it.className.contains(query, ignoreCase = true)
        }

    fun toAction(activity: ActivityLauncherUtil.LaunchableActivity) =
        Action.OpenActivity(
            packageName = packageName,
            className = activity.className,
            label = activity.label,
            requiresRoot = activity.requiresRoot,
        )

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.activity_picker_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = appLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.activity_picker_search)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_search_24),
                        contentDescription = stringResource(R.string.action_search),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            when {
                activities == null ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        LoadingIndicator()
                    }

                filtered.isEmpty() ->
                    Text(
                        text = stringResource(R.string.activity_picker_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )

                else ->
                    LazyColumn(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp)),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(filtered, key = { it.className }) { activity ->
                            val isAvailable = !activity.requiresRoot || canUseRoot
                            ActivityRow(
                                packageName = packageName,
                                activity = activity,
                                iconSizePx = iconSizePx,
                                isAvailable = isAvailable,
                                canPin = canPin,
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    if (isAvailable) {
                                        onActivitySelected(toAction(activity))
                                    } else {
                                        Toast.makeText(context, R.string.activity_picker_root_required_toast, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onPin = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    if (isAvailable) {
                                        pinTarget = toAction(activity)
                                    } else {
                                        Toast.makeText(context, R.string.activity_picker_root_required_toast, Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                        }
                    }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    pinTarget?.let { target ->
        ActivityIconSheet(
            action = target,
            confirmLabel = stringResource(R.string.action_create_shortcut),
            onDismiss = { pinTarget = null },
            onConfirm = { source, icon ->
                ShortcutUtil.pinActionShortcut(
                    context,
                    target,
                    target.label,
                    icon,
                    adaptive = source == ActivityIconSource.CUSTOM,
                )
                pinTarget = null
            },
        )
    }
}

@Composable
private fun ActivityRow(
    packageName: String,
    activity: ActivityLauncherUtil.LaunchableActivity,
    iconSizePx: Int,
    isAvailable: Boolean,
    canPin: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit,
) {
    val context = LocalContext.current
    val icon by produceState<Bitmap?>(null, packageName, activity.className) {
        value =
            withContext(Dispatchers.IO) {
                ActivityLauncherUtil.loadIcon(context, packageName, activity.className, iconSizePx)
                    ?: runCatching {
                        context.packageManager.getApplicationIcon(packageName).toBitmap(iconSizePx, iconSizePx)
                    }.getOrNull()
            }
    }
    val image: ImageBitmap? = remember(icon) { icon?.asImageBitmap() }

    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(if (isAvailable) 1f else 0.5f),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(ICON_SIZE.dp)
                            .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Spacer(modifier = Modifier.size(ICON_SIZE.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = ActivityLauncherUtil.shortClassName(packageName, activity.className),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (activity.requiresRoot) {
                    Text(
                        text = stringResource(R.string.activity_picker_root_only),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            if (canPin) {
                IconButton(onClick = onPin) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_add_24),
                        contentDescription = stringResource(R.string.action_create_shortcut),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private const val ICON_SIZE = 40
