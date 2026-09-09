/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: WearOS Companion
 * File: WatchNotificationSettingsUI.kt
 * Description: UI component for managing watch notification sync settings.
 */

package com.sameerasw.essentials.ui.features.watch

import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.services.WatchNotificationSyncManager
import com.sameerasw.essentials.ui.core.cards.FeatureCard
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun WatchNotificationSettingsUI(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val prefs =
        remember {
            context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        }

    var isSyncEnabled by remember {
        mutableStateOf(prefs.getBoolean("watch_notif_sync_enabled", false))
    }
    var isSilentEnabled by remember {
        mutableStateOf(prefs.getBoolean("watch_notif_silent_enabled", false))
    }
    var isMediaEnabled by remember {
        mutableStateOf(prefs.getBoolean("watch_notif_media_enabled", true))
    }
    var isCallSyncEnabled by remember {
        mutableStateOf(prefs.getBoolean("watch_call_sync_enabled", true))
    }
    var showAppPicker by remember { mutableStateOf(false) }
    var showCallPermissionSheet by remember { mutableStateOf(false) }
    var showNotifPermissionSheet by remember { mutableStateOf(false) }

    val isCallSyncPermissionGranted =
        com.sameerasw.essentials.utils.PermissionUtils
            .hasCallPermissions(context)
    val isNotifSyncPermissionGranted = viewModel.isNotificationListenerEnabled.value

    val callPermissionLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            contract =
                androidx.activity.result.contract.ActivityResultContracts
                    .RequestMultiplePermissions(),
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                isCallSyncEnabled = true
                prefs.edit().putBoolean("watch_call_sync_enabled", true).apply()
                showCallPermissionSheet = false
            }
        }

    val requestCallPermissions = {
        val perms =
            mutableListOf(
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.READ_CALL_LOG,
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            perms.add(android.Manifest.permission.ANSWER_PHONE_CALLS)
        }
        callPermissionLauncher.launch(perms.toTypedArray())
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    ) {
        RoundedCardContainer {
            IconToggleItem(
                title = stringResource(R.string.watch_notif_sync_title),
                description = stringResource(R.string.watch_notif_sync_desc),
                iconRes = R.drawable.rounded_sync_24,
                isChecked = isSyncEnabled && isNotifSyncPermissionGranted,
                onCheckedChange = { checked ->
                    HapticUtil.performUIHaptic(view)
                    if (!isNotifSyncPermissionGranted) {
                        showNotifPermissionSheet = true
                    } else {
                        isSyncEnabled = checked
                        prefs.edit().putBoolean("watch_notif_sync_enabled", checked).apply()
                    }
                },
                enabled = isNotifSyncPermissionGranted,
                onDisabledClick = {
                    if (!isNotifSyncPermissionGranted) {
                        showNotifPermissionSheet = true
                    }
                },
            )

            FeatureCard(
                title = stringResource(R.string.watch_notif_sync_choose_apps),
                description = stringResource(R.string.watch_notif_sync_choose_apps_desc),
                iconRes = R.drawable.rounded_apps_24,
                isEnabled = isSyncEnabled,
                showToggle = false,
                hasMoreSettings = true,
                onToggle = {},
                onClick = {
                    if (isSyncEnabled) {
                        HapticUtil.performUIHaptic(view)
                        showAppPicker = true
                    }
                },
            )
        }

        AnimatedVisibility(visible = isSyncEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RoundedCardContainer(
                    modifier =
                        Modifier
                            .padding(top = 18.dp)
                            .fillMaxWidth(),
                ) {
                    IconToggleItem(
                        title = stringResource(R.string.watch_notif_sync_silent_title),
                        description = stringResource(R.string.watch_notif_sync_silent_desc),
                        iconRes = R.drawable.rounded_notifications_off_24,
                        checked = isSilentEnabled,
                        onCheckedChange = { checked ->
                            HapticUtil.performUIHaptic(view)
                            isSilentEnabled = checked
                            prefs.edit().putBoolean("watch_notif_silent_enabled", checked).apply()
                        },
                    )

                    IconToggleItem(
                        title = stringResource(R.string.watch_notif_sync_media_title),
                        description = stringResource(R.string.watch_notif_sync_media_desc),
                        iconRes = R.drawable.rounded_music_video_24,
                        checked = isMediaEnabled,
                        onCheckedChange = { checked ->
                            HapticUtil.performUIHaptic(view)
                            isMediaEnabled = checked
                            prefs.edit().putBoolean("watch_notif_media_enabled", checked).apply()
                        },
                    )
                }

                RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                    IconToggleItem(
                        title = stringResource(R.string.watch_notif_sound_title),
                        description = stringResource(R.string.watch_notif_sound_desc),
                        iconRes = R.drawable.rounded_volume_up_24,
                        showToggle = false,
                        isChecked = true,
                        onCheckedChange = {},
                    )

                    var selectedSound by remember {
                        mutableStateOf(
                            prefs.getString("watch_notif_sound", "notification") ?: "notification",
                        )
                    }
                    val sounds = listOf("notification", "google", "carmen_nexus", "dock")
                    val soundLabels =
                        mapOf(
                            "notification" to stringResource(R.string.watch_notif_sound_notification),
                            "google" to stringResource(R.string.watch_notif_sound_google),
                            "carmen_nexus" to stringResource(R.string.watch_notif_sound_carmen_nexus),
                            "dock" to stringResource(R.string.watch_notif_sound_dock),
                        )

                    SegmentedPicker(
                        items = sounds,
                        selectedItem = selectedSound,
                        onItemSelected = { sound ->
                            selectedSound = sound
                            prefs.edit().putString("watch_notif_sound", sound).apply()
                            WatchNotificationSyncManager.setWatchNotificationSound(context, sound)
                        },
                        labelProvider = { soundLabels[it] ?: it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
                    FeatureCard(
                        title = stringResource(R.string.watch_notif_sync_now),
                        description = stringResource(R.string.watch_notif_sync_now_desc),
                        iconRes = R.drawable.rounded_sync_24,
                        isEnabled = isSyncEnabled,
                        showToggle = false,
                        hasMoreSettings = false,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            val listener = NotificationListener.instance
                            if (listener != null) {
                                val activeNotifs = listener.activeNotifications
                                val count =
                                    WatchNotificationSyncManager.syncActiveNotifications(
                                        context,
                                        activeNotifs,
                                    )
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.watch_notif_synced_active_count, count),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            } else {
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.watch_notif_listener_not_running),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        },
                    )

                    FeatureCard(
                        title = stringResource(R.string.watch_notif_sync_icons),
                        description = stringResource(R.string.watch_notif_sync_icons_desc),
                        iconRes = R.drawable.rounded_apps_24,
                        isEnabled = isSyncEnabled,
                        showToggle = false,
                        hasMoreSettings = false,
                        onToggle = {},
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            val allowedApps = WatchNotificationSyncManager.getAllowedApps(context)
                            val pkgsToSync =
                                if (allowedApps.isNotEmpty()) {
                                    allowedApps
                                } else {
                                    val listener = NotificationListener.instance
                                    listener?.activeNotifications?.map { it.packageName }?.toSet()
                                        ?: emptySet()
                                }
                            val count = WatchNotificationSyncManager.syncAppIcons(context, pkgsToSync)
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.watch_notif_synced_icons_count, count),
                                    Toast.LENGTH_SHORT,
                                ).show()
                        },
                    )
                }
            }
        }
    }

    if (showAppPicker) {
        AppSelectionSheet(
            onDismissRequest = { showAppPicker = false },
            onLoadApps = { ctx ->
                val json =
                    ctx
                        .getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                        .getString("watch_notif_allowed_apps", null)
                if (json != null) {
                    try {
                        val pkgs = Gson().fromJson(json, Array<String>::class.java).toSet()
                        pkgs.map { AppSelection(it, true) }
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            },
            onSaveApps = { ctx, selections ->
                val enabledPkgs = selections.filter { it.isEnabled }.map { it.packageName }
                val json = Gson().toJson(enabledPkgs)
                ctx
                    .getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("watch_notif_allowed_apps", json)
                    .apply()
                if (enabledPkgs.isNotEmpty()) {
                    WatchNotificationSyncManager.syncAppIcons(ctx, enabledPkgs.toSet())
                }
            },
        )
    }

    if (showCallPermissionSheet) {
        PermissionsBottomSheet(
            onDismissRequest = { showCallPermissionSheet = false },
            featureTitle = stringResource(R.string.watch_call_sync_title),
            permissions =
                listOf(
                    com.sameerasw.essentials.ui.core.sheets.PermissionItem(
                        iconRes = R.drawable.rounded_mobile_sound_24,
                        title = stringResource(R.string.watch_call_sync_title),
                        description = stringResource(R.string.watch_call_sync_desc),
                        dependentFeatures = listOf(stringResource(R.string.watch_call_sync_title)),
                        actionLabel =
                            if (isCallSyncPermissionGranted) {
                                stringResource(
                                    R.string.perm_action_granted,
                                )
                            } else {
                                stringResource(R.string.perm_action_grant)
                            },
                        action = { requestCallPermissions() },
                        isGranted = isCallSyncPermissionGranted,
                    ),
                ),
        )
    }

    if (showNotifPermissionSheet) {
        val permissionItems =
            PermissionUIHelper.getPermissionItems(
                listOf("NOTIFICATION_LISTENER"),
                context,
                viewModel,
                activity,
            )
        if (permissionItems.isNotEmpty()) {
            PermissionsBottomSheet(
                onDismissRequest = { showNotifPermissionSheet = false },
                featureTitle = stringResource(R.string.watch_notif_sync_title),
                permissions = permissionItems,
            )
        }
    }
}
