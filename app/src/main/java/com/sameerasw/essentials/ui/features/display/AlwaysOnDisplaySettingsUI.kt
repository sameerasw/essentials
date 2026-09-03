/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: AlwaysOnDisplaySettingsUI.kt
 * Description: UI component and settings composable for Display feature domain.
 */

package com.sameerasw.essentials.ui.features.system

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
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
fun AlwaysOnDisplaySettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null,
) {
    val context = LocalContext.current
    val view = LocalView.current

    var showAppSelectionSheet by remember { mutableStateOf(false) }
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

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RoundedCardContainer(
            modifier = Modifier,
            spacing = 2.dp,
            cornerRadius = 24.dp,
        ) {
            IconToggleItem(
                iconRes = R.drawable.rounded_mobile_text_2_24,
                title = stringResource(R.string.feat_always_on_display_title),
                isChecked = viewModel.isAodEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setAodEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "aod_toggle"),
            )
        }

        Text(
            text = stringResource(R.string.feat_notification_glance_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        RoundedCardContainer {
            IconToggleItem(
                iconRes = R.drawable.rounded_notification_settings_24,
                title = stringResource(R.string.feat_notification_glance_title),
                isChecked = viewModel.isNotificationGlanceEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.toggleNotificationGlanceEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "notification_glance_enabled"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_apps_24,
                title = stringResource(R.string.notification_glance_same_as_lighting_title),
                isChecked = viewModel.isNotificationGlanceSameAsLightingEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    viewModel.setNotificationGlanceSameAsLightingEnabled(checked)
                },
                modifier = Modifier.highlight(highlightSetting == "notification_glance_same_apps"),
            )

            IconToggleItem(
                iconRes = R.drawable.rounded_power_settings_new_24,
                title = stringResource(R.string.feat_aod_force_turn_off_title),
                isChecked = viewModel.isAodForceTurnOffEnabled.value,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked && !isAccessibilityEnabled) {
                        requestingPermissionsFor =
                            Pair(R.string.feat_aod_force_turn_off_title, listOf("ACCESSIBILITY"))
                    } else {
                        viewModel.toggleAodForceTurnOffEnabled(checked)
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!isAccessibilityEnabled) {
                        requestingPermissionsFor =
                            Pair(R.string.feat_aod_force_turn_off_title, listOf("ACCESSIBILITY"))
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "aod_force_turn_off"),
            )
        }

        Text(
            text = stringResource(R.string.notification_glance_desc),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.feat_aod_force_turn_off_desc),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!viewModel.isNotificationGlanceSameAsLightingEnabled.value) {
            Button(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    showAppSelectionSheet = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.isNotificationGlanceEnabled.value,
            ) {
                Text(stringResource(R.string.action_select_apps))
            }
        }

        Text(
            text = stringResource(R.string.feat_aod_wallpaper_section_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val wallpaperBitmap = viewModel.currentWallpaperBitmap.value
        val opacity = viewModel.aodWallpaperOpacity.floatValue

        LaunchedEffect(isStoragePermissionGranted) {
            if (isStoragePermissionGranted) {
                viewModel.loadCurrentWallpaperBitmap(context)
            }
        }

        val isWallpaperEnabled = viewModel.isAodWallpaperEnabled.value
        val blurRadius = viewModel.aodWallpaperBlur.floatValue
        val vignetteIntensity = viewModel.aodWallpaperVignette.floatValue

        val animatedPreviewAlpha by animateFloatAsState(
            targetValue = if (isWallpaperEnabled) opacity else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "aodWallpaperPreviewAlpha",
        )

        @OptIn(ExperimentalTextApi::class)
        val aodClockFont = remember {
            FontFamily(
                Font(
                    R.font.google_sans_flex,
                    weight = FontWeight.Thin,
                    variationSettings = FontVariation.Settings(
                        FontVariation.Setting("wght", 100f),
                        FontVariation.Setting("ROND", 100f),
                        FontVariation.Setting("wdth", 150f),
                    ),
                )
            )
        }

        val timeText = remember {
            val cal = java.util.Calendar.getInstance()
            val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
            val pattern = if (is24Hour) "HH mm" else "hh mm"
            java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(cal.time)
        }

        var isPreviewMenuExpanded by remember { mutableStateOf(false) }
        val hasCustomImage = viewModel.hasAodWallpaperCustomImage.value

        val photoPickerLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
            ) { uri ->
                uri?.let {
                    viewModel.setCustomAodWallpaper(context, it)
                }
            }

        RoundedCardContainer {
            AnimatedVisibility(
                visible = isWallpaperEnabled,
                enter = expandVertically(animationSpec = tween(durationMillis = 300)) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) + fadeOut(animationSpec = tween(durationMillis = 300)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Black)
                        .clickable {
                            HapticUtil.performVirtualKeyHaptic(view)
                            isPreviewMenuExpanded = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                if (vignetteIntensity > 0f && isWallpaperEnabled) {
                                    val edgeAlpha = (1f - vignetteIntensity / 100f).coerceIn(0f, 1f)
                                    drawRect(
                                        brush = Brush.radialGradient(
                                            colorStops = arrayOf(
                                                0.0f to Color.Black,
                                                0.45f to Color.Black,
                                                1.0f to Color.Black.copy(alpha = edgeAlpha),
                                            ),
                                            center = center,
                                            radius = maxOf(size.width, size.height) * 0.75f,
                                        ),
                                        blendMode = BlendMode.DstIn,
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (wallpaperBitmap != null) {
                            val luminanceFilter = remember {
                                androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                    androidx.compose.ui.graphics.ColorMatrix(
                                        floatArrayOf(
                                            1.2f, 0f, 0f, 0f, 0f,
                                            0f, 1.2f, 0f, 0f, 0f,
                                            0f, 0f, 1.2f, 0f, 0f,
                                            0.5f, 1.5f, 0.2f, 0f, -15f,
                                        )
                                    )
                                )
                            }

                            Image(
                                bitmap = wallpaperBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                colorFilter = luminanceFilter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(animatedPreviewAlpha)
                                    .then(
                                        if (blurRadius > 0f) Modifier.blur(blurRadius.dp) else Modifier
                                    ),
                            )
                        }

                        Text(
                            text = timeText,
                            style = TextStyle(
                                fontFamily = aodClockFont,
                                fontWeight = FontWeight.Thin,
                                fontSize = 52.sp,
                                letterSpacing = 4.sp,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = if (isWallpaperEnabled) (animatedPreviewAlpha * 1.4f).coerceIn(0f, 1f) else 0f,
                                ),
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }

                    SegmentedDropdownMenu(
                        expanded = isPreviewMenuExpanded,
                        onDismissRequest = { isPreviewMenuExpanded = false },
                    ) {
                        SegmentedDropdownMenuItem(
                            text = { Text(stringResource(R.string.feat_aod_wallpaper_pick_image)) },
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                isPreviewMenuExpanded = false
                                photoPickerLauncher.launch("image/*")
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rounded_image_24),
                                    contentDescription = null,
                                )
                            },
                        )

                        if (hasCustomImage) {
                            SegmentedDropdownMenuItem(
                                text = { Text(stringResource(R.string.feat_aod_wallpaper_remove_custom_image)) },
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    isPreviewMenuExpanded = false
                                    viewModel.removeCustomAodWallpaper(context)
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_delete_24),
                                        contentDescription = null,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            IconToggleItem(
                iconRes = R.drawable.rounded_wallpaper_24,
                title = stringResource(R.string.feat_aod_wallpaper_title),
                isChecked = isWallpaperEnabled,
                onCheckedChange = { checked ->
                    HapticUtil.performVirtualKeyHaptic(view)
                    if (checked) {
                        if (!isAccessibilityEnabled || !isStoragePermissionGranted) {
                            val missing = mutableListOf<String>()
                            if (!isAccessibilityEnabled) missing.add("ACCESSIBILITY")
                            if (!isStoragePermissionGranted) missing.add("STORAGE")
                            requestingPermissionsFor = Pair(R.string.feat_aod_wallpaper_title, missing)
                        } else {
                            viewModel.toggleAodWallpaperEnabled(true)
                            viewModel.loadCurrentWallpaperBitmap(context)
                        }
                    } else {
                        viewModel.toggleAodWallpaperEnabled(false)
                    }
                },
                enabled = true,
                onDisabledClick = {
                    if (!isAccessibilityEnabled || !isStoragePermissionGranted) {
                        val missing = mutableListOf<String>()
                        if (!isAccessibilityEnabled) missing.add("ACCESSIBILITY")
                        if (!isStoragePermissionGranted) missing.add("STORAGE")
                        requestingPermissionsFor = Pair(R.string.feat_aod_wallpaper_title, missing)
                    }
                },
                modifier = Modifier.highlight(highlightSetting == "aod_wallpaper"),
            )

            AnimatedVisibility(
                visible = isWallpaperEnabled,
                enter = expandVertically(animationSpec = tween(durationMillis = 300)) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) + fadeOut(animationSpec = tween(durationMillis = 300)),
            ) {
                ConfigSliderItem(
                    title = stringResource(R.string.feat_aod_wallpaper_opacity),
                    value = (opacity * 100f).coerceIn(10f, 75f),
                    onValueChange = {
                        viewModel.setAodWallpaperOpacity(it / 100f)
                    },
                    valueRange = 10f..75f,
                    increment = 5f,
                    valueFormatter = { "${it.toInt()}%" },
                    iconRes = R.drawable.rounded_visibility_24,
                )
            }

            AnimatedVisibility(
                visible = isWallpaperEnabled,
                enter = expandVertically(animationSpec = tween(durationMillis = 300)) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) + fadeOut(animationSpec = tween(durationMillis = 300)),
            ) {
                ConfigSliderItem(
                    title = stringResource(R.string.feat_aod_wallpaper_blur),
                    value = blurRadius,
                    onValueChange = { viewModel.setAodWallpaperBlur(it) },
                    valueRange = 0f..25f,
                    increment = 1f,
                    valueFormatter = { if (it == 0f) "Off" else "${it.toInt()}" },
                    iconRes = R.drawable.rounded_blur_on_24,
                )
            }

            AnimatedVisibility(
                visible = isWallpaperEnabled,
                enter = expandVertically(animationSpec = tween(durationMillis = 300)) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) + fadeOut(animationSpec = tween(durationMillis = 300)),
            ) {
                ConfigSliderItem(
                    title = stringResource(R.string.feat_aod_wallpaper_vignette),
                    value = vignetteIntensity,
                    onValueChange = { viewModel.setAodWallpaperVignette(it) },
                    valueRange = 0f..100f,
                    increment = 5f,
                    valueFormatter = { if (it == 0f) "Off" else "${it.toInt()}%" },
                    iconRes = R.drawable.rounded_grain_24,
                )
            }

            AnimatedVisibility(
                visible = isWallpaperEnabled,
                enter = expandVertically(animationSpec = tween(durationMillis = 300)) + fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) + fadeOut(animationSpec = tween(durationMillis = 300)),
            ) {
                val timeoutOptions = listOf(
                    0 to stringResource(R.string.feat_aod_wallpaper_timeout_never),
                    1 to stringResource(R.string.feat_aod_wallpaper_timeout_1m),
                    3 to stringResource(R.string.feat_aod_wallpaper_timeout_3m),
                    5 to stringResource(R.string.feat_aod_wallpaper_timeout_5m),
                    10 to stringResource(R.string.feat_aod_wallpaper_timeout_10m),
                )
                val currentTimeout = viewModel.aodWallpaperTimeout.intValue
                val selectedLabel = timeoutOptions.firstOrNull { it.first == currentTimeout }?.second
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

        Text(
            text = stringResource(R.string.feat_aod_wallpaper_desc),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(80.dp))

        if (showAppSelectionSheet) {
            AppSelectionSheet(
                onDismissRequest = { showAppSelectionSheet = false },
                onLoadApps = { viewModel.loadNotificationGlanceSelectedApps(it) },
                onSaveApps = { ctx, apps ->
                    viewModel.saveNotificationGlanceSelectedApps(
                        ctx,
                        apps,
                    )
                },
                onAppToggle = { ctx, pkg, enabled ->
                    viewModel.updateNotificationGlanceAppEnabled(
                        ctx,
                        pkg,
                        enabled,
                    )
                },
                context = context,
            )
        }
    }
}
