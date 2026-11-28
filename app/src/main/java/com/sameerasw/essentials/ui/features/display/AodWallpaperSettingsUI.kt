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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.services.LiveWallpaperService
import com.sameerasw.essentials.services.DailyWallpaperWorker
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import com.sameerasw.essentials.ui.activities.WallpaperActivity
import android.content.Intent
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.AodCompatibilityInfoBottomSheet
import com.sameerasw.essentials.ui.core.sheets.AppSelectionSheet
import com.sameerasw.essentials.ui.core.sheets.PermissionsBottomSheet
import com.sameerasw.essentials.ui.modifiers.highlight
import com.sameerasw.essentials.ui.modifiers.whatsNewSetting
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
    var showCompatInfoSheet by remember { mutableStateOf(false) }
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

    var isDailyLiveActive by remember { mutableStateOf(false) }
    var dailyPreview by remember { mutableStateOf<ImageBitmap?>(null) }
    LifecycleResumeEffect(Unit) {
        val active = DailyWallpaperWorker.isDailyLiveWallpaperActive(context, SettingsRepository(context))
        if (active != isDailyLiveActive) {
            isDailyLiveActive = active
            viewModel.loadCurrentWallpaperBitmap(context)
        }
        onPauseOrDispose { }
    }
    LaunchedEffect(isDailyLiveActive) {
        dailyPreview =
            if (!isDailyLiveActive) {
                null
            } else {
                withContext(Dispatchers.IO) {
                    LiveWallpaperService.latestDailyImageFile(context)?.let {
                        AppUtil.decodeSampledBitmapFromFile(it.absolutePath, reqWidth = 160, reqHeight = 320)?.asImageBitmap()
                    }
                }
            }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnimatedVisibility(
            visible = isDailyLiveActive,
            enter = expandVertically(animationSpec = tween(durationMillis = 300)) + fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) + fadeOut(animationSpec = tween(durationMillis = 300)),
        ) {
            RoundedCardContainer {
                IconToggleItem(
                    title = stringResource(R.string.feat_aod_wallpaper_use_daily_live),
                    isChecked = viewModel.isAodWallpaperUseDailyLive.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setAodWallpaperUseDailyLive(checked)
                        viewModel.loadCurrentWallpaperBitmap(context)
                    },
                    modifier = Modifier.whatsNewSetting(SettingsRepository.KEY_AOD_WALLPAPER_USE_DAILY_LIVE, "AOD wallpaper"),
                    leadingIcon = {
                        Box(
                            modifier =
                                Modifier
                                    .size(width = 32.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            dailyPreview?.let {
                                Image(
                                    bitmap = it,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    },
                )
            }
        }

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
                modifier = Modifier.highlight("aod_wallpaper", highlightSetting),
            )

            AnimatedVisibility(
                visible = isWallpaperEnabled,
                enter = expandVertically(animationSpec = tween(durationMillis = 300)) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) + fadeOut(animationSpec = tween(durationMillis = 300)),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_do_not_disturb_on_24,
                    title = stringResource(R.string.feat_aod_wallpaper_disable_on_dnd),
                    isChecked = viewModel.isAodWallpaperDisableOnDnd.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setAodWallpaperDisableOnDnd(checked)
                    },
                    modifier = Modifier.highlight("aod_wallpaper_disable_on_dnd", highlightSetting),
                )
            }

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
                        valueFormatter = { if (it == 0f) context.getString(R.string.off) else "${it.toInt()}" },
                        iconRes = R.drawable.rounded_blur_on_24,
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.feat_aod_wallpaper_vignette),
                        value = vignetteIntensity,
                        onValueChange = { viewModel.setAodWallpaperVignette(it) },
                        valueRange = 0f..100f,
                        increment = 5f,
                        valueFormatter = { if (it == 0f) context.getString(R.string.off) else "${it.toInt()}%" },
                        iconRes = R.drawable.rounded_grain_24,
                    )

                    ConfigSliderItem(
                        title = stringResource(R.string.feat_aod_wallpaper_black_threshold),
                        value = blackThreshold,
                        onValueChange = { viewModel.setAodWallpaperBlackThreshold(it) },
                        valueRange = 0f..50f,
                        increment = 1f,
                        valueFormatter = { if (it == 0f) context.getString(R.string.off) else "${it.toInt()}" },
                        iconRes = R.drawable.rounded_invert_colors_24,
                    )

                    val timeoutOptions =
                        listOf(
                            "never" to stringResource(R.string.feat_aod_wallpaper_timeout_never),
                            "5s" to stringResource(R.string.feat_aod_wallpaper_timeout_5s),
                            "15s" to stringResource(R.string.feat_aod_wallpaper_timeout_15s),
                            "30s" to stringResource(R.string.feat_aod_wallpaper_timeout_30s),
                            "1m" to stringResource(R.string.feat_aod_wallpaper_timeout_1m),
                            "3m" to stringResource(R.string.feat_aod_wallpaper_timeout_3m),
                            "5m" to stringResource(R.string.feat_aod_wallpaper_timeout_5m),
                            "10m" to stringResource(R.string.feat_aod_wallpaper_timeout_10m),
                            "custom" to stringResource(R.string.feat_aod_wallpaper_timeout_custom),
                        )
                    val currentTimeout = viewModel.aodWallpaperTimeout.value
                    val selectedLabel =
                        timeoutOptions.firstOrNull { it.first == currentTimeout }?.second
                            ?: stringResource(R.string.feat_aod_wallpaper_timeout_3m)
                    ConfigPickerItem(
                        title = stringResource(R.string.feat_aod_wallpaper_timeout),
                        selectedValue = selectedLabel,
                        iconRes = R.drawable.rounded_timer_24,
                    ) {
                        timeoutOptions.forEach { (optionValue, label) ->
                            SegmentedDropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    viewModel.setAodWallpaperTimeout(optionValue)
                                },
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = currentTimeout == "custom",
                        enter = expandVertically(animationSpec = tween(durationMillis = 300)) + fadeIn(animationSpec = tween(durationMillis = 300)),
                        exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) + fadeOut(animationSpec = tween(durationMillis = 300)),
                    ) {
                        val customTimeout = viewModel.aodWallpaperCustomTimeout.floatValue
                        ConfigSliderItem(
                            title = stringResource(R.string.feat_aod_wallpaper_custom_timeout_slider),
                            value = customTimeout,
                            onValueChange = { viewModel.setAodWallpaperCustomTimeout(it) },
                            valueRange = 1f..180f,
                            increment = 1f,
                            valueFormatter = {
                                val secs = it.toInt()
                                if (secs < 60) "${secs}s" else if (secs % 60 == 0) "${secs / 60}m" else "${secs / 60}m ${secs % 60}s"
                            },
                            iconRes = R.drawable.rounded_timer_24,
                        )
                    }
                }
            }
        }

        val isNotificationListenerGranted = viewModel.isNotificationListenerEnabled.value
        val isAlbumArtEnabled = viewModel.isAodWallpaperUseAlbumArt.value
        val isKeepOnMedia = viewModel.isAodWallpaperKeepOnMedia.value
        val isTimeoutNever = viewModel.aodWallpaperTimeout.value == "never" || viewModel.aodWallpaperTimeout.value == "0"

        TranslatableText(
            stringResId = R.string.feat_aod_wallpaper_media_section_title,
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
                modifier = Modifier.highlight("aod_wallpaper_album_art", highlightSetting),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_wallpaper_24,
                title = stringResource(R.string.wallpaper_album_art_link),
                showToggle = false,
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    context.startActivity(Intent(context, WallpaperActivity::class.java).putExtra("tab", "music"))
                },
                modifier = Modifier.whatsNewSetting("aod_wallpaper_album_art_link", "AOD wallpaper"),
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
                    modifier = Modifier.highlight("aod_wallpaper_keep_on_media", highlightSetting),
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

        val isExtendedMedia = viewModel.isAodWallpaperExtendedMedia.value

        TranslatableText(
            stringResId = R.string.feat_aod_wallpaper_extended_section_title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_music_note_24,
                title = stringResource(R.string.feat_aod_wallpaper_extended_media),
                isChecked = isExtendedMedia,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    val missing =
                        buildList {
                            if (!isNotificationListenerGranted) add("NOTIFICATION_LISTENER")
                            if (!viewModel.isWriteSecureSettingsEnabled.value) add("WRITE_SECURE_SETTINGS")
                        }
                    if (checked && missing.isNotEmpty()) {
                        requestingPermissionsFor =
                            Pair(R.string.feat_aod_wallpaper_extended_media, missing)
                    } else {
                        viewModel.setAodWallpaperExtendedMedia(checked)
                    }
                },
                modifier = Modifier.highlight("aod_wallpaper_extended_media", highlightSetting),
            )

            AnimatedVisibility(
                visible = isExtendedMedia,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_apps_24,
                    title = stringResource(R.string.feat_aod_wallpaper_extended_app_icon),
                    isChecked = viewModel.isAodWallpaperExtendedAppIcon.value,
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setAodWallpaperExtendedAppIcon(checked)
                    },
                    modifier = Modifier.highlight("aod_wallpaper_extended_app_icon", highlightSetting),
                )
            }

            AnimatedVisibility(
                visible = isExtendedMedia,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                ConfigSliderItem(
                    title = stringResource(R.string.status_glance_font_size_title),
                    value = (viewModel.aodWallpaperExtendedTextScale.floatValue * 100f).coerceIn(70f, 200f),
                    onValueChange = { viewModel.setAodWallpaperExtendedTextScale(it / 100f) },
                    valueRange = 70f..200f,
                    increment = 5f,
                    valueFormatter = { "${it.toInt()}%" },
                    iconRes = R.drawable.rounded_format_size_24,
                )
            }
        }

        val compatSettings = remember { SettingsRepository(context) }
        var compatMode by remember { mutableStateOf(compatSettings.getBoolean(SettingsRepository.KEY_AOD_WALLPAPER_COMPAT, false)) }
        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_bug_report_24,
                title = stringResource(R.string.feat_aod_wallpaper_compat_title),
                isChecked = compatMode,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    compatMode = checked
                    compatSettings.putBoolean(SettingsRepository.KEY_AOD_WALLPAPER_COMPAT, checked)
                },
                onInfoClick = { showCompatInfoSheet = true },
            )
        }

        Spacer(modifier = Modifier.height(80.dp))

        if (showCompatInfoSheet) {
            AodCompatibilityInfoBottomSheet(onDismissRequest = { showCompatInfoSheet = false })
        }

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
