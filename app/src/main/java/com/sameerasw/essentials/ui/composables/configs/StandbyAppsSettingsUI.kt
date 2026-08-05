package com.sameerasw.essentials.ui.composables.configs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.AppStandbyInfo
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.sheets.PermissionItem
import com.sameerasw.essentials.ui.components.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StandbyAppsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var requestingPermissionFor by remember { mutableStateOf(false) }

    val isShizukuGranted = viewModel.isShizukuAvailable.value && viewModel.isShizukuPermissionGranted.value
    val isRootGranted = viewModel.isRootAvailable.value && viewModel.isRootPermissionGranted.value
    val isShellGranted = isShizukuGranted || isRootGranted

    LaunchedEffect(Unit) {
        viewModel.loadStandbyApps(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadStandbyApps(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (requestingPermissionFor) {
        val shizukuPermission = PermissionItem(
            iconRes = R.drawable.rounded_adb_24,
            title = if (!viewModel.isShizukuAvailable.value) R.string.perm_shizuku_title else R.string.perm_shizuku_grant_title,
            description = if (!viewModel.isShizukuAvailable.value) R.string.perm_shizuku_desc else R.string.perm_shizuku_grant_desc,
            dependentFeatures = listOf(R.string.feat_standby_apps_title),
            actionLabel = if (!viewModel.isShizukuAvailable.value) R.string.perm_shizuku_install_action else if (isShellGranted) R.string.perm_action_granted else R.string.perm_action_grant,
            action = {
                if (!viewModel.isShizukuAvailable.value) {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } else {
                    viewModel.requestShizukuPermission()
                }
            },
            isGranted = isShellGranted
        )

        PermissionsBottomSheet(
            onDismissRequest = { requestingPermissionFor = false },
            featureTitle = R.string.feat_standby_apps_title,
            permissions = listOf(shizukuPermission)
        )
    }

    val appsList = viewModel.standbyAppsList.value
    val isLoading = viewModel.isStandbyAppsLoading.value

    if (isLoading) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            LoadingIndicator()
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val buckets = listOf(
                10 to R.string.standby_bucket_active,
                20 to R.string.standby_bucket_working_set,
                30 to R.string.standby_bucket_frequent,
                40 to R.string.standby_bucket_rare,
                45 to R.string.standby_bucket_restricted
            )

            buckets.forEach { (bucketCode, titleRes) ->
                val bucketApps = appsList.filter { it.bucket == bucketCode }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(titleRes) + " (${bucketApps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                    )

                    RoundedCardContainer{
                        if (bucketApps.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.standby_apps_empty_bucket),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            bucketApps.forEach { app ->
                                AppStandbyCardItem(
                                    app = app,
                                    currentBucket = bucketCode,
                                    isShellGranted = isShellGranted,
                                    onMoveBucket = { targetBucket ->
                                        if (isShellGranted) {
                                            viewModel.setAppStandbyBucket(app.packageName, targetBucket, context)
                                        } else {
                                            requestingPermissionFor = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppStandbyCardItem(
    app: AppStandbyInfo,
    currentBucket: Int,
    isShellGranted: Boolean,
    onMoveBucket: (Int) -> Unit
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        content = {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            AsyncImage(
                model = app.icon,
                contentDescription = app.label,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingContent = {
            Box {
                IconButton(
                    onClick = {
                        HapticUtil.performUIHaptic(view)
                        if (isShellGranted) {
                            showMenu = true
                        } else {
                            onMoveBucket(currentBucket)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_mobiledata_arrows_24),
                        contentDescription = "Move bucket",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                SegmentedDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    val menuOptions = listOf(
                        10 to R.string.standby_bucket_active,
                        20 to R.string.standby_bucket_working_set,
                        30 to R.string.standby_bucket_frequent,
                        40 to R.string.standby_bucket_rare,
                        45 to R.string.standby_bucket_restricted
                    )

                    menuOptions.forEach { (targetBucket, titleRes) ->
                        val isCurrent = targetBucket == currentBucket
                        SegmentedDropdownMenuItem(
                            text = {
                                Text(text = stringResource(titleRes))
                            },
                            enabled = !isCurrent,
                            onClick = {
                                HapticUtil.performUIHaptic(view)
                                showMenu = false
                                onMoveBucket(targetBucket)
                            }
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
