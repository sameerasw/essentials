package com.sameerasw.essentials.ui.components

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.model.DeviceSpecs
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.modifiers.shimmer
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.DeviceImageMapper
import com.sameerasw.essentials.utils.DeviceInfo
import com.sameerasw.essentials.utils.DeviceUtils
import com.sameerasw.essentials.utils.HapticUtil

@Composable
fun DeviceHeroCard(
    deviceInfo: DeviceInfo,
    deviceSpecs: DeviceSpecs? = null,
    imageOffset: () -> Dp = { 0.dp },
    contentAlpha: () -> Float = { 1f },
    contentOffset: () -> Dp = { 0.dp },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val imageUrls = deviceSpecs?.imageUrls ?: emptyList()
    val isPixel = deviceInfo.manufacturer.contains("Google", ignoreCase = true)

    var showFlashbangDialog by remember { mutableStateOf(false) }

    val launchIntent = { packageName: String, className: String ->
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(packageName, className)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {

        }
    }

    if (showFlashbangDialog) {
        FlashbangDialog(
            onDismiss = { showFlashbangDialog = false },
            onContinue = {
                showFlashbangDialog = false
                launchIntent(
                    "com.google.android.apps.diagnosticstool",
                    "com.google.android.apps.diagnosticstool.login.EndUserLoginActivity"
                )
            }
        )
    }

    // Only show the illustration page if it's a Pixel AND we have a mapping
    val illustrationRes = DeviceImageMapper.getDeviceDrawable(deviceInfo.model)
    val showIllustration = isPixel && illustrationRes != 0
    val pageCount = (if (showIllustration) 1 else 0) + imageUrls.size
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (pageCount > 0) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = imageOffset().toPx()
                    }
                    .fillMaxWidth()
                    .height(480.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showIllustration && page == 0) {
                            // stylized vector
                            Icon(
                                painter = painterResource(id = illustrationRes),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxHeight(0.85f)
                                    .fillMaxWidth(0.85f)
                            )
                        } else {
                            // real image from gsmarena (or local cache)
                            val imageIndex = if (showIllustration) page - 1 else page
                            val imageModel =
                                if (deviceSpecs?.localImagePaths?.isNotEmpty() == true &&
                                    deviceSpecs.localImagePaths.size > imageIndex
                                ) {
                                    deviceSpecs.localImagePaths[imageIndex]
                                } else {
                                    imageUrls[imageIndex]
                                }

                            AsyncImage(
                                model = imageModel,
                                contentDescription = "Device Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxHeight(0.85f)
                                    .fillMaxWidth(0.85f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .shimmer()
                            )
                        }
                    }
                }

                // Page Indicator dots
                if (pageCount > 1) {
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(pageCount) { iteration ->
                            val color = if (pagerState.currentPage == iteration)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(color)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // User-set Device Name
        Text(
            text = deviceInfo.deviceName,
            modifier = Modifier.graphicsLayer {
                alpha = contentAlpha()
                translationY = contentOffset().toPx()
            },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = null
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Manufacturer Model
        Text(
            text = "${deviceInfo.manufacturer.replaceFirstChar { it.uppercase() }} ${deviceInfo.model} (${deviceInfo.hardware})",
            modifier = Modifier.graphicsLayer {
                alpha = contentAlpha()
                translationY = contentOffset().toPx()
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

//            Spacer(modifier = Modifier.height(24.dp))

    }

    RoundedCardContainer(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = contentAlpha()
                translationY = contentOffset().toPx()
            },
    ) {

        val androidLogoRes = DeviceImageMapper.getAndroidLogo(deviceInfo)
        if (isPixel && androidLogoRes != 0) {
            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = Shapes.extraSmall
                    )
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = androidLogoRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(56.dp)
                    )
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Android ${deviceInfo.androidVersion} (${
                                    DeviceUtils.getOSName(
                                        deviceInfo.sdkInt,
                                        deviceInfo.osCodename
                                    )
                                })",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "API ${deviceInfo.sdkInt} • Patch: ${
                                DeviceUtils.formatSecurityPatch(
                                    deviceInfo.securityPatch
                                )
                            }",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Build: ${deviceInfo.display}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = Shapes.extraSmall
                )
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Storage and Memory Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Storage Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_dns_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = stringResource(R.string.label_device_storage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DeviceUtils.formatHardwareSize(deviceInfo.totalStorage),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Memory Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_memory_alt_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = stringResource(R.string.label_device_ram),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DeviceUtils.formatHardwareSize(deviceInfo.totalRam),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

        }

        if (isPixel) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceBright,
                        shape = Shapes.extraSmall
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PixelToolButton(
                    iconRes = R.drawable.rounded_diagnosis_24,
                    label = stringResource(id = R.string.label_diagnostics),
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        launchIntent(
                            "com.android.devicediagnostics",
                            "com.android.devicediagnostics.MainActivity"
                        )
                    }
                )
                PixelToolButton(
                    iconRes = R.drawable.rounded_search_check_2_24,
                    label = stringResource(id = R.string.label_device_check),
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showFlashbangDialog = true
                    }
                )
            }
        }
    }
}

@Composable
private fun PixelToolButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FlashbangDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.label_device_check),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.msg_flashbang),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(R.drawable.flashbang)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onContinue()
                }
            ) {
                Text(text = stringResource(id = R.string.action_continue))
            }
        },
        dismissButton = {
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.action_abort))
            }
        }
    )
}
