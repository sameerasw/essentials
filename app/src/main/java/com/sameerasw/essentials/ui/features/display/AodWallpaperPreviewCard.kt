/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Display
 * File: AodWallpaperPreviewCard.kt
 * Description: Live preview of the AOD wallpaper (clock, wallpaper, blur, vignette) shown in the feature header.
 */

package com.sameerasw.essentials.ui.features.display

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalTextApi::class)
@Composable
fun AodWallpaperPreviewCard(
    viewModel: MainViewModel,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current

    val isStoragePermissionGranted = viewModel.isStoragePermissionGranted.value
    val wallpaperBitmap = viewModel.currentWallpaperBitmap.value
    val isWallpaperEnabled = viewModel.isAodWallpaperEnabled.value
    val opacity = viewModel.aodWallpaperOpacity.floatValue
    val blurRadius = viewModel.aodWallpaperBlur.floatValue
    val vignetteIntensity = viewModel.aodWallpaperVignette.floatValue
    val blackThreshold = viewModel.aodWallpaperBlackThreshold.floatValue
    val hasCustomImage = viewModel.hasAodWallpaperCustomImage.value

    LaunchedEffect(isStoragePermissionGranted) {
        if (isStoragePermissionGranted) {
            viewModel.loadCurrentWallpaperBitmap(context)
        }
    }

    val animatedPreviewAlpha by animateFloatAsState(
        targetValue = if (isWallpaperEnabled) opacity else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "aodWallpaperPreviewAlpha",
    )

    val aodClockFont =
        remember {
            FontFamily(
                Font(
                    R.font.google_sans_flex,
                    weight = FontWeight.Thin,
                    variationSettings =
                        FontVariation.Settings(
                            FontVariation.Setting("wght", 100f),
                            FontVariation.Setting("ROND", 100f),
                            FontVariation.Setting("wdth", 150f),
                        ),
                ),
            )
        }

    val timeText =
        remember {
            val cal = java.util.Calendar.getInstance()
            val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
            val pattern = if (is24Hour) "HH mm" else "hh mm"
            java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(cal.time)
        }

    var isPreviewMenuExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { viewModel.setCustomAodWallpaper(context, it) }
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height.coerceAtLeast(0.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .clickable {
                    HapticUtil.performVirtualKeyHaptic(view)
                    isPreviewMenuExpanded = true
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        if (vignetteIntensity > 0f && isWallpaperEnabled) {
                            val edgeAlpha = (1f - vignetteIntensity / 100f).coerceIn(0f, 1f)
                            drawRect(
                                brush =
                                    Brush.radialGradient(
                                        colorStops =
                                            arrayOf(
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
            Text(
                text = timeText,
                style =
                    TextStyle(
                        fontFamily = aodClockFont,
                        fontWeight = FontWeight.Thin,
                        fontSize = 52.sp,
                        letterSpacing = 4.sp,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                textAlign = TextAlign.Center,
            )

            if (wallpaperBitmap != null) {
                val luminanceFilter =
                    remember(blackThreshold) {
                        androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                            androidx.compose.ui.graphics.ColorMatrix(
                                floatArrayOf(
                                    1.2f, 0f, 0f, 0f, 0f,
                                    0f, 1.2f, 0f, 0f, 0f,
                                    0f, 0f, 1.2f, 0f, 0f,
                                    0.5f, 1.5f, 0.2f, 0f, -blackThreshold,
                                ),
                            ),
                        )
                    }

                Image(
                    bitmap = wallpaperBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = luminanceFilter,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .alpha(animatedPreviewAlpha)
                            .then(if (blurRadius > 0f) Modifier.blur(blurRadius.dp) else Modifier),
                )
            }
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
