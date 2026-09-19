/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: AodWallpaperSettingsUI.kt
 * Description: UI settings composable for the AOD wallpaper feature.
 */

package com.sameerasw.essentials.ui.features.display

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.PermissionUIHelper
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AodWallpaperSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var showMediaAppSelectionSheet by remember { mutableStateOf(false) }
    var requestingPermissionsFor by remember { mutableStateOf<Pair<Int, List<String>>?>(null) }

    val isAccessibilityEnabled = viewModel.isAccessibilityEnabled.value
    val isStoragePermissionGranted = viewModel.isStoragePermissionGranted.value

    LaunchedEffect(Unit) {
        viewModel.check(context)
    }

    if (requestingPermissionsFor != null) {
        val (featureTitle, permKeys) = requestingPermissionsFor!!
        val permissionItems = PermissionUIHelper.getPermissionItems(permKeys, context, viewModel)
        PermissionsBottomSheet(
            onDismissRequest = {
                requestingPermissionsFor = null
                viewModel.check(context)
            },
            featureTitle = featureTitle,
            permissions = permissionItems,
        )
    }

    val isWallpaperEnabled = viewModel.isAodWallpaperEnabled.value
    val opacity = viewModel.aodWallpaperOpacity.floatValue
    val blurRadius = viewModel.aodWallpaperBlur.floatValue
    val vignetteIntensity = viewModel.aodWallpaperVignette.floatValue
    val blackThreshold = viewModel.aodWallpaperBlackThreshold.floatValue

    fun requestMainPermissions() {
        val missing = mutableListOf<String>()
        if (!isAccessibilityEnabled) missing.add("ACCESSIBILITY")
        if (!isStoragePermissionGranted) missing.add("STORAGE")
        if (missing.isNotEmpty()) {
            requestingPermissionsFor = Pair(R.string.feat_aod_wallpaper_title, missing)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_wallpaper_24,
                title = stringResource(R.string.feat_aod_wallpaper_title),
                isChecked = isWallpaperEnabled,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked) {
                        if (!isAccessibilityEnabled || !isStoragePermissionGranted) {
                            requestMainPermissions()
                        } else {
                            viewModel.toggleAodWallpaperEnabled(true)
                            viewModel.loadCurrentWallpaperBitmap(context)
                        }
                    } else {
                        viewModel.toggleAodWallpaperEnabled(false)
                    }
                },
                enabled = true,
                onDisabledClick = { requestMainPermissions() },
                modifier = Modifier.highlight(highlightSetting == "aod_wallpaper"),
            )

            AnimatedVisibility(
                visible = isWallpaperEnabled,
                enter = expandVertically(animationSpec = tween(durationMillis = 300)) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) + fadeOut(animationSpec = tween(durationMillis = 300)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ConfigSliderItem(
                        title = stringResource(R.string.feat_aod_wallpaper_opacity),
                        value = (opacity * 100f).coerceIn(10f, 75f),
                        onValueChange = { viewModel.setAodWallpaperOpacity(it / 100f) },
                        valueRange = 10f..75f,
                        increment = 5f,
                        valueFormatter = { "${it.toInt()}%" },
                        iconRes = R.drawable.rounded_visibility_24,
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.feat_aod_wallpaper_blur),
                        value = blurRadius,
                        onValueChange = { viewModel.setAodWallpaperBlur(it) },
                        valueRange = 0f..25f,
                        increment = 1f,
                        valueFormatter = { if (it == 0f) "Off" else "${it.toInt()}" },
                        iconRes = R.drawable.rounded_blur_on_24,
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.feat_aod_wallpaper_vignette),
                        value = vignetteIntensity,
                        onValueChange = { viewModel.setAodWallpaperVignette(it) },
                        valueRange = 0f..100f,
                        increment = 5f,
                        valueFormatter = { if (it == 0f) "Off" else "${it.toInt()}%" },
                        iconRes = R.drawable.rounded_grain_24,
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.feat_aod_wallpaper_black_threshold),
                        value = blackThreshold,
                        onValueChange = { viewModel.setAodWallpaperBlackThreshold(it) },
                        valueRange = 0f..50f,
                        increment = 1f,
                        valueFormatter = { if (it == 0f) "Off" else "${it.toInt()}" },
                        iconRes = R.drawable.rounded_invert_colors_24,
                    )

                    val timeoutOptions =
                        listOf(
                            0 to stringResource(R.string.feat_aod_wallpaper_timeout_never),
                            1 to stringResource(R.string.feat_aod_wallpaper_timeout_1m),
                            3 to stringResource(R.string.feat_aod_wallpaper_timeout_3m),
                            5 to stringResource(R.string.feat_aod_wallpaper_timeout_5m),
                            10 to stringResource(R.string.feat_aod_wallpaper_timeout_10m),
                        )
                    val currentTimeout = viewModel.aodWallpaperTimeout.intValue
                    val selectedLabel =
                        timeoutOptions.firstOrNull { it.first == currentTimeout }?.second
                            ?: stringResource(R.string.feat_aod_wallpaper_timeout_3m)
                    ConfigPickerItem(
                        title = stringResource(R.string.feat_aod_wallpaper_timeout),
                        selectedValue = selectedLabel,
                        iconRes = R.drawable.rounded_timer_24,
                    ) {
                        timeoutOptions.forEach { (minutes, label) ->
                            SegmentedDropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.setAodWallpaperTimeout(minutes)
                                },
                            )
                        }
                    }
                }
            }
        }

        val isNotificationListenerGranted = viewModel.isNotificationListenerEnabled.value
        val isAlbumArtEnabled = viewModel.isAodWallpaperUseAlbumArt.value
        val isKeepOnMedia = viewModel.isAodWallpaperKeepOnMedia.value
        val isTimeoutNever = viewModel.aodWallpaperTimeout.intValue == 0

        Text(
            text = stringResource(R.string.feat_aod_wallpaper_media_section_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_music_note_24,
                title = stringResource(R.string.feat_aod_wallpaper_use_album_art),
                isChecked = isAlbumArtEnabled,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked) {
                        if (!isNotificationListenerGranted) {
                            requestingPermissionsFor =
                                Pair(R.string.feat_aod_wallpaper_use_album_art, listOf("NOTIFICATION_LISTENER"))
                        } else {
                            viewModel.setAodWallpaperUseAlbumArt(true)
                        }
                    } else {
                        viewModel.setAodWallpaperUseAlbumArt(false)
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!isNotificationListenerGranted) {
                        requestingPermissionsFor =
                            Pair(R.string.feat_aod_wallpaper_use_album_art, listOf("NOTIFICATION_LISTENER"))
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "aod_wallpaper_album_art"),
            )

            AnimatedVisibility(
                visible = isAlbumArtEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_timer_24,
                    title = stringResource(R.string.feat_aod_wallpaper_keep_on_media),
                    isChecked = if (isTimeoutNever) true else isKeepOnMedia,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setAodWallpaperKeepOnMedia(checked)
                    },
                    enabled = !isTimeoutNever,
                    modifier = Modifier.highlight(highlightSetting == "aod_wallpaper_keep_on_media"),
                )
            }

            AnimatedVisibility(
                visible = isAlbumArtEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_apps_24,
                    title = stringResource(R.string.feat_aod_wallpaper_media_apps),
                    showToggle = false,
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showMediaAppSelectionSheet = true
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))

        if (showMediaAppSelectionSheet) {
            AppSelectionSheet(
                onDismissRequest = { showMediaAppSelectionSheet = false },
                onLoadApps = { viewModel.loadAodWallpaperMediaApps(it) },
                onSaveApps = { ctx, apps -> viewModel.saveAodWallpaperMediaApps(ctx, apps) },
                onAppToggle = { ctx, pkg, enabled ->
                    viewModel.updateAodWallpaperMediaAppEnabled(ctx, pkg, enabled)
                },
                context = context,
            )
        }
    }
}
